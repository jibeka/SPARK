# SPARK

###TP SPARK 4/5

#####Liste des fichiers/dossiers:

    - /JobML_modelFinal             : Modèle final sauvé à la fin du processing
    - /src                          : sources du projet
    - build.sbt                     : build.sbt
    - cleanedDataFrame.parquet.zip  : fichier zip contenant les données néttoyées pendant les TP 2/3
    - CommandeSave.txt              : Liste des commandes pour lancer Spark (master et slave), compiler et executer
    - plugins.sbt                   : plugins.sbt
    - Results_Terminal.txt          : Capture du terminal à la fin de l'execution



**Pour lancer le jar compilé, la commande est la suivante:**

./spark-submit --conf spark.eventLog.enabled=true --conf spark.eventLog.dir="/tmp" --driver-memory 3G --executor-memory 4G --class com.sparkProject.JobML --master spark://jbk-UbVm:7077 /media/sf_ShareJBKubVM/Spark/TP2-3/tp_spark/target/scala-2.11/tp_spark-assembly-1.0.jar /home/jbk/shareWin/Spark/TP2-3/cleanedDataFrame.parquet

> modifier :
> 
> **--master spark://jbk-UbVm:7077** par **--master spark:{master name}**


> **/media/sf_ShareJBKubVM/Spark/TP2-3/tp_spark/target/scala-2.11/tp_spark-assembly-1.0.jar** par le chemin du jar donné par la commande sbt assembly 

> **/home/jbk/shareWin/Spark/TP2-3/cleanedDataFrame.parquet** par son propre chemin d'accès au jeu de données cleanedDataFrame.parquet



**Le flow (verbeux) du job est le suivant :**

 -  Le nom du fichier d'entrée est passé en argument. Ce fichier est le fichier fournit à la fin du TP 3.

 -  Je commence par créer un array ne contenant que les colonnes de features puis j'applique le module VectorAssembler pour une colonne features.

 -  Ensuite, j'applique le module stringIndexer pour transformer les labels STRINGS en label DOUBLE.

 -  Je sépare les données d'entrée en set d'entrainement et test avec randomSplit (90, 10).

 -  Je crée un objet LogisticRegression.

 -  Je crée une grille d'hyperparamètres pour le paramètre de régularisation avec GridParam.

 -  Je construis un objet TrainValidation Split(70/30) pour tester la grille d'hyperparamètres.

 - Je fit le modèles avec le set d'entrainement.

 - J'applique le modèle entrainé sur les données de test.


En utiliant le module trainValidationSplit (70/30) avec la grille de paramètres de régularisation demandée, j'obtiens un optimum pour 1.0E-4.

Une fois le modèle obtenu appliqué aux données de test, je mesure sa précision ainsi que sa matrice de confusion via le module MulticlassMetrics de mllib. La précision obtenue varie autour de 94% en fonction du RandomSplit appliqué aux données d'entrée.

Le meilleur modèle obtenu est enfin extrait et sauvegardé dans le répertoire contenant le fichier d'entrée sous le nom JobML_modelFinal.


La sortie finale du terminal est la suivante :

	Paramètres de régularisations
	+--------------------+------------------+
	|           regParams|     modelAccuracy|
	+--------------------+------------------+
	|              1.0E-6|0.9871129383992222|
	|3.162277660168379E-6|0.9863333669733682|
	|              1.0E-5| 0.986794089298212|
	|3.162277660168379...|0.9880124438906034|
	|              1.0E-4|0.9891971584402128|
	|3.162277660168379...|0.9875356328496421|
	|               0.001|0.9864708523655523|
	|0.003162277660168...|0.9857293088141263|
	|                0.01| 0.983289674408263|
	| 0.03162277660168379|0.9744204040022835|
	|                 0.1|0.9290926402899391|
	| 0.31622776601683794|               0.5|
	|                 1.0|               0.5|
	+--------------------+------------------+

	Paramètres du modèle final
	{
        	logreg_dd9b992831e6-elasticNetParam: 1.0,
        	logreg_dd9b992831e6-featuresCol: features,
        	logreg_dd9b992831e6-fitIntercept: true,
        	logreg_dd9b992831e6-labelCol: label,
        	logreg_dd9b992831e6-maxIter: 300,
        	logreg_dd9b992831e6-predictionCol: prediction,
        	logreg_dd9b992831e6-probabilityCol: probability,
        	logreg_dd9b992831e6-rawPredictionCol: rawPrediction,
        	logreg_dd9b992831e6-regParam: 1.0E-4,
        	logreg_dd9b992831e6-standardization: true,
        	logreg_dd9b992831e6-threshold: 0.5,
        	logreg_dd9b992831e6-tol: 1.0E-5
	}
	File saved: /home/jbk/shareWin/Spark/TP2-3//JobML_modelFinal
	Résultat de TrainValidationSet+-----+----------+-----+
	|label|prediction|count|
	+-----+----------+-----+
	|  1.0|       1.0|  208|
	|  0.0|       1.0|   14|
	|  1.0|       0.0|    8|
	|  0.0|       0.0|  343|
	+-----+----------+-----+

	Matrice de confusion : 
	343.0  14.0   
	8.0    208.0  
	La précision du modèle est : 
	0.9616055846422339





