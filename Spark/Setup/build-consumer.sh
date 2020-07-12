cd ../Spark-Consumer
mvn package
cd ../
rm Setup/Spark-Consumer-0.0.1-SNAPSHOT.jar
cp Spark-Consumer/target/Spark-Consumer-0.0.1-SNAPSHOT.jar Setup/