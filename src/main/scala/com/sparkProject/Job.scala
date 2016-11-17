package com.sparkProject


import org.apache.spark.sql.SparkSession
// less verbose for spark output
// http://stackoverflow.com/questions/27781187/how-to-stop-messages-displaying-on-spark-console
import org.apache.log4j.{Level, Logger}
import scala.collection.mutable.ListBuffer
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._


object Job {

  def main(args: Array[String]): Unit = {

    // SparkSession configuration
    val spark = SparkSession
      .builder
      .master("local")
      .appName("spark session TP_parisTech")
      .getOrCreate()

    val sc = spark.sparkContext

    import spark.implicits._
    import spark.implicits._

    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    /********************************************************************************
      *
      *        TP 1
      *
      *        - Set environment, InteliJ, submit jobs to Spark
      *        - Load local unstructured data
      *        - Word count , Map Reduce
      ********************************************************************************/



    // ----------------- word count ------------------------

    val df_wordCount = sc.textFile("/home/jbk/SPARK/spark-2.0.0-bin-hadoop2.7/README.md")
      .flatMap{case (line: String) => line.split(" ")}
      .map{case (word: String) => (word, 1)}
      .reduceByKey{case (i: Int, j: Int) => i + j}
      .toDF("word", "count")

    df_wordCount.orderBy($"count".desc).show()


    /********************************************************************************
      *
      *        TP 2 : début du projet
      *

      ********************************************************************************/

    val df = spark.read
      .option("comment","#")
      .option("header","true")
      .option("inferSchema","true")
      .csv("/home/jbk/shareWin/Spark/TP2-3/cumulative.csv")

    println("number of columns", df.columns.length)
    println("number of rows",df.count())
// Question e
    val colNames= df.columns
    val colSel=colNames.slice(10,20)
    df.select(colSel.head, colSel.tail: _*).show(5)
// Question f
    df.printSchema()
// Question g peut on faire ca avec un map? et reducebykey?
    val countsByDispo= df.groupBy("koi_disposition").count()
    countsByDispo.show()

    // Partie 4 cleaning
    // a
    val dfFilt=df.filter($"koi_disposition" === "CONFIRMED" || $"koi_disposition" === "FALSE POSITIVE")
    dfFilt.groupBy("koi_disposition").count().show()
    // b
    dfFilt.select("koi_eccen_err1").distinct().groupBy("koi_eccen_err1").count().show()

    // c
    val dfFiltDrT = dfFilt.drop($"koi_eccen_err1")
    dfFiltDrT.printSchema()


    // d
    val dfFiltDr = dfFilt.drop("koi_eccen_err1" , "index","kepid","koi_fpflag_nt", "koi_fpflag_ss", "koi_fpflag_co",
      "koi_fpflag_ec" , "koi_sparprov", "koi_trans_mod", "koi_datalink_dvr", "koi_datalink_dvr", "koi_datalink_dvr",
      "koi_datalink_dvs", "koi_tce_delivname", "koi_parm_prov", "koi_parm_prov","koi_limbdark_mod", "koi_fittype",
      "koi_disp_prov", "koi_comment", "kepoi_name", " kepoi_name", "kepler_name", "koi_vet_date", "koi_pdisposition")


    // e
    for (col <-dfFiltDr.columns){
      if (dfFiltDr.select(col).distinct().count() <= 1){
          dfFiltDr.drop(col)
      }
    }
    dfFiltDr.printSchema()


    // f
    dfFiltDr.describe()


    // g
    val dfrepNa = df.na.fill(0)

    // 6

    val df_labels = dfrepNa.select("rowid", "koi_disposition")
    val df_features = dfrepNa.drop("koi_disposition")

    // a
    val dfJoin = df_features.join(df_labels,usingColumn = "rowid")

    // 7
      // a

    val df7 = dfJoin.withColumn("koi_ror_min",$"koi_ror" - $"koi_ror_err2")
                    .withColumn("koi_ror_max",$"koi_ror" + $"koi_ror_err1")

    val addCol = udf((ror: Double, err:Double) => ror + err)
    val subCol = udf((ror: Double, err:Double) => ror - err)

    val df8 = dfJoin.withColumn("koi_ror_min", subCol($"koi_ror" , $"koi_ror_err2"))
                    .withColumn("koi_ror_max", addCol($"koi_ror" , $"koi_ror_err1"))

    //df7.write.format("com.databricks.spark.csv").save("/media/sf_ShareJBKubVM/Spark" + "/data/")
    df7.show()
    df7
      .coalesce(1) // optional : regroup all data in ONE partition, so that results are printed in ONE file
      // >>>> You should not that in general, only when the data are small enough to fit in the memory of a single machine.
      .write
      .mode("overwrite")
      .option("header", "true")
      .csv("/home/jbk/SPARKdata/clean_DataSet.csv")
    }



}
