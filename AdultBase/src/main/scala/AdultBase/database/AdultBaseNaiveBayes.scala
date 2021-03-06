package AdultBase.database

import AdultBase.database.AdultBaseFileOperation.getAdultIndexSchema
import org.apache.spark.ml.classification.NaiveBayes
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{VectorAssembler, VectorIndexer}
import org.apache.spark.sql.SparkSession

object AdultBaseNaiveBayes {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getRootLogger.setLevel(Level.WARN)

    val spark = SparkSession
      .builder()
      .master("local[*]")
      .appName("AdultBaseDatabase")
      .getOrCreate()

    val training_table = spark.read.format("csv")
      .option("sep", ",")
      .option("header", "true")
      .schema(getAdultIndexSchema)
      .load("./data/indexed_adult_training")

    val test_table = spark.read.format("csv")
      .option("sep", ",")
      .option("header", "true")
      .schema(getAdultIndexSchema)
      .load("./data/indexed_adult_test")

    val featuresArr = Array("workclassIndex", "educationIndex", "occupationIndex",
      "relationshipIndex", "raceIndex", "sexIndex")


    val assembler = new VectorAssembler()
      .setInputCols(featuresArr)
      .setOutputCol("raw_features")

    val featureIndexer = new VectorIndexer()
      .setInputCol("raw_features")
      .setOutputCol("features")
      .setMaxCategories(10)

    val index_pipeline = new Pipeline().setStages(Array(assembler, featureIndexer))

    val assembled_training_data = index_pipeline.fit(training_table).transform(training_table)
    val assembled_test_data = index_pipeline.fit(test_table).transform(training_table)
    assembled_training_data.show()

    val naiveBayes = new NaiveBayes()
      .setFeaturesCol("features")
      .setLabelCol("maritial_statusIndex")
      .setPredictionCol("prediction")
      .setModelType("multinomial")
      .setSmoothing(1.1)

    val nbModel = naiveBayes.fit(assembled_test_data)
    val predictions = nbModel.transform(assembled_test_data)

    val evaluator = new MulticlassClassificationEvaluator()
      .setMetricName("accuracy")
      .setLabelCol("maritial_statusIndex")
      .setPredictionCol("prediction")

    val accuracy = evaluator
      .setPredictionCol("prediction")
      .setLabelCol("maritial_statusIndex")
      .setMetricName("accuracy")
      .evaluate(predictions)


    println(accuracy)
  }
}