TP SPARK 4/5

Liste des fichier/dossier:
    
    - /JobML_modelFinal.csv         : Modèle final sauvé à la fin du processing
    - /src                          : sources du projet
    - build.sbt                     : build.sbt
    - cleanedDataFrame.parquet.zip  : fichier zip contenant les données néttoyées pendant les TP 2/3
    - CommandeSave.txt              : Liste des commandes pour lancer Spark (master et slave), compiler et executer
    - plugins.sbt                   : plugins.sbt
    - Results_Terminal.txt          : Capture du terminal à la fin de l'execution



Pour lancer le jar compilé, la commande est la suivante:

./spark-submit --conf spark.eventLog.enabled=true --conf spark.eventLog.dir="/tmp" --driver-memory 3G --executor-memory 4G --class com.sparkProject.JobML --master spark://jbk-UbVm:7077 /media/sf_ShareJBKubVM/Spark/TP2-3/tp_spark/target/scala-2.11/tp_spark-assembly-1.0.jar /home/jbk/shareWin/Spark/TP2-3/cleanedDataFrame.parquet

Le nom du fichier d'entrée est passé en argument. Ce fichier est le fichier fournit à la fin du TP 3.
Le dossier contenant le fichier d'entrée est celui dans lequel je sauve le modèle final.


