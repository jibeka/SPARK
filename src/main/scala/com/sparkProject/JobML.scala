package com.sparkProject

// less verbose for spark output
// http://stackoverflow.com/questions/27781187/how-to-stop-messages-displaying-on-spark-console
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionModel}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder, TrainValidationSplit}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.mllib.evaluation.MulticlassMetrics

/**
  * Created by jbk on 25/10/16.
  */
object JobML {

  def main(args: Array[String]): Unit = {

    // SparkSession configuration
    val spark = SparkSession
      .builder
      .master("local")
      .appName("spark session TP_parisTech")
      .getOrCreate()

    val sc = spark.sparkContext
    import spark.implicits._

    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)


    //val df = spark.read.parquet("/home/jbk/shareWin/Spark/TP2-3/cleanedDataFrame.parquet")
    val splitString = args(0).split("/")
    val directory = args(0).replace(splitString(splitString.length-1), "")
    // Load des données nettoyées pendant les tp 2/3(/4)
    val df = spark.read.parquet(args(0))
    df.printSchema()
    //A la base, mon fichier était plein de string, et je voulais caster les string en double...
    // je garde ce bout de code comme exemple pour moi...
    /**
    val colNaType = df.dtypes
    var ArrStr = Array[String]()
    for (col <-colNaType){
      if (col._2 == "StringType"){
        ArrStr +:= col._1
      }
    }

**/
    // On récupère le nom des colonnes du df pour pouvoir filtrer les champs que l'on ne veut pas garder
    val colNames= df.columns

    // On filtre les colonnes koi_disposition et rowid
    // Utiliser 2 fois filter fait une double boucle, ca marche mais ce n'est pas optimal
    // val colNoLabel = colNames.filter(!_.contains("koi_disposition" )).filter(! _.contains("rowid"))
    val colNoLabel = colNames.filter(x => !x.contains("koi_disposition") && !x.contains("rowid"))
    println("colNoLabel")
    println(colNoLabel.mkString(" \n"))

    // Je regroupe les variables explicatives dans un vecteur dense pour que cela soit utilisable par le module de
    // classification
    // Je pourrais piper les différentes étapes de transformation des datas, mais comme je préfère les vérifier avant
    val assembler = new VectorAssembler()
      .setInputCols(colNoLabel).setOutputCol("features")


    val df_Proc = assembler.transform(df).select("features", "koi_disposition")
    df_Proc.printSchema()

    // J'indexe les labels STRING koi_disposition en labels DOUBLE label
    val indexer = new StringIndexer()
      .setInputCol("koi_disposition")
      .setOutputCol("label")
    val indexed = indexer.fit(df_Proc).transform(df_Proc)
    indexed.show(100)

    // Je sépare les données en DF Train pour entrainer le modèle et Test pour vérifier les résultats
    val Array(trainingData, testData) = indexed.randomSplit(Array(0.9, 0.1))
    testData.show(100)


    // Je définie le modèle de classification
    /**
      LogisticRegression
      setElasticNetParam(1.0)     : Paramètre supplementaire de l'elastic-net: 1 = L1-norm = LASSO
      setLabelCol("label")        : Optionnel si on veut changer le nom de la colonne label, mais ca plante si ce n'est
                                    pas le cas...
      setFeaturesCol("features")  : Optionnel si on veut changer le nom de la colonne features, mais ca plante si ce
                                    n'est pas le cas...
      setStandardization(true)    : Normalisation des données, obligatoire avec les régressions pénalisées
      setFitIntercept(true)       : Avec l'intercept, on a une regression affine
      setTol(1.0e-5)              : Paramètre d'arret de la convergence
      setMaxIter(300)             : Budget max d'iterations pour éviter une boucle infinie en cas de non convergence
                                    de l'algorithme
    **/
    val lr = new LogisticRegression()
      .setElasticNetParam(1.0)
      .setLabelCol("label")
      .setFeaturesCol("features")
      .setStandardization(true)
      .setFitIntercept(true)
      .setTol(1.0e-5)
      .setMaxIter(300)

    // Je définie un array de valeurs pour le paramètre de régularisation à tester
    val regParam = (-6.0 to 0 by 0.5 toArray).map(math.pow(10,_))


    // J'utilise la librairie ML.tuning pour définir les meilleurs hyerparamètres du modèle
    // Je construis un objet ParamGrid pour parcourir un set d'hyperparamètres à tester
    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, regParam)
      .build()

    // Une première version avec le trainValidation split
    // Je découpe le set d'entrainement en 70/30
    // 70% ou on fit le modèle
    // 30% ou on mesure la performance du modèle
    // Le module conserve le set d'hyperparamètres le plus efficace.
    val trainValidationSplit = new TrainValidationSplit()
      .setEstimator(lr)
      .setEvaluator(new BinaryClassificationEvaluator)
      .setEstimatorParamMaps(paramGrid)
      .setTrainRatio(0.7)
    val tvsModel = trainValidationSplit.fit(trainingData)

    // Récupération des précisions des differents modèles
    val tvsModelAccuracies = tvsModel.validationMetrics
    val regParAccu = sc.parallelize(regParam  zip tvsModelAccuracies).toDF("regParams" , "modelAccuracy")
    println("Paramètres de régularisations")
    regParAccu.show()


    // Récupération du meilleur modèle
    val bModel = tvsModel.bestModel.asInstanceOf[LogisticRegressionModel]
    println("Paramètres du modèle final")
    println(bModel.extractParamMap.toString())
    bModel.write.overwrite().save(directory + "/JobML_modelFinal.csv")
    println("File saved: " + directory + "/JobML_modelFinal")

    // On applique le meilleur modèle sur le set de test
    val predictionTVS = tvsModel.transform(testData)

    print("Résultat de TrainValidationSet")
    predictionTVS.groupBy("label", "prediction").count.show()

    // Calcul de metrics supplémentaires pour tester
    val predictionAndLabels = predictionTVS.rdd.map({ row => (
      row.getAs[Double]("prediction"),
      row.getAs[Double]("label"))})

    // Instantiation de l'objet MulticlassificationMetrics
    val mMetrics = new MulticlassMetrics(predictionAndLabels)

    println("Matrice de confusion : ")
    println(mMetrics.confusionMatrix)
    println("La précision du modèle est : ")
    println(mMetrics.accuracy)


  /**
    // Une seconde version avec Cross-validator
    // Une autre approche consiste à prendre tout le set de test et de faire une cross-validation dessus
    val cv = new CrossValidator()
      .setEstimator(lr)
      .setEvaluator(new BinaryClassificationEvaluator())
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(6) // Nombre de fold de la cross-validation
    val cvModel = cv.fit(trainingData)

    // On applique le meilleur modèle sur le set de test
    val predictionsCV = cvModel.transform(testData)

    print("Résultat de CrossValidator")
    predictionsCV.groupBy("label", "prediction").count.show()


    // Calcul de metrics supplémentaires pour tester
    val predictionAndLabels = predictionsCV.rdd.map({ row => (
      row.getAs[Double]("prediction"),
      row.getAs[Double]("label"))})

    // Instantiation de l'objet BinaryClassificationMetrics
    val metrics = new BinaryClassificationMetrics(predictionAndLabels)

    // Precision by threshold
    val precision = metrics.precisionByThreshold
    precision.foreach { case (t, p) =>
      println(s"Threshold: $t, Precision: $p")
    }

    // Instantiation de l'objet BinaryClassificationMetrics
    val mMetrics = new MulticlassMetrics(predictionAndLabels)
    println(mMetrics.confusionMatrix)
    println(mMetrics.accuracy)

  **/
  }

}
