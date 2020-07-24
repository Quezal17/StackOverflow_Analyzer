cd Kafka/Setup
bash build-connector.sh
cd ../../Spark/Setup
bash build-consumer.sh
cd ../..
docker-compose build