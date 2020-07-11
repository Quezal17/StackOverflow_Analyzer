cd ../Stackoverflow-Connector
mvn package
cd ../
rm Setup/Stackoverflow-Connector-0.0.1-SNAPSHOT.jar
cp Stackoverflow-Connector/target/Stackoverflow-Connector-0.0.1-SNAPSHOT.jar Setup/