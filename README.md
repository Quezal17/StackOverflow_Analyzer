# StackOverflow - Analyzer
Developed by **Simone Torrisi**, Computer Science student at University of Catania

## Project Goal

The goal of this project is to analyze real time questions from <a href="https://stackoverflow.com/">**Stack Overflow**</a> and clustering them based on title, body and tags associated to the question. The results will be then displayed on dashboards.

You can get more information visiting docs, Kafka and Spark directories.

## Technologies used
<ul>
<li> <strong>Centralized service</strong>: <a href="https://zookeeper.apache.org/">Zookeeper</a></li>
<li> <strong>Data Ingestion</strong>: <a href="https://docs.confluent.io/current/connect/index.html">Kafka Connect</a> 2.4.1 with Java 11 </li>
<li><strong>Data Streaming</strong>: <a href="https://www.confluent.io/what-is-apache-kafka/">Apache Kafka</a> and <a href="https://spark.apache.org/streaming/">Spark Streaming</a></li>
<li><strong>Data Processing</strong>: <a href="https://spark.apache.org/">Apache Spark</a> 3.0.0 and <a href="https://spark.apache.org/mllib/">Spark MLlib</a> with Java 11</li>
<li><strong>Data Indexing</strong>: <a href="https://www.elastic.co/what-is/elasticsearch">Elasticsearch</a> 7.8.0</li>
<li><strong>Data Visualization</strong>: <a href="https://www.elastic.co/what-is/kibana">Kibana</a> 7.8.0</li>
</ul>

## Project Structure

<img src="./docs/project-overview/images/project-structure.svg" width="1000" height="400"/>

## How to execute the project

### Downloads
<ul>
<li><strong>Apache Kafka</strong>: download from <a href="https://www.apache.org/dyn/closer.cgi?path=/kafka/2.5.0/kafka_2.12-2.5.0.tgz">here</a> and put the tgz file into Kafka/Setup directory.</li>
<li><strong>Apache Spark</strong>: download from <a href="https://www.apache.org/dyn/closer.lua/spark/spark-3.0.0/spark-3.0.0-bin-hadoop2.7.tgz">here</a> and put the tgz file into Spark/Setup directory.</li>
</ul>
In addition, it is required that <a href="https://docs.docker.com/get-docker/">Docker</a> and <a href="https://maven.apache.org/download.cgi">Apache Maven</a> have been already installed.

### Initial setup
To start the initial setup the following script <code>initial-setup.sh</code> has to be executed in the main directory.

There are two options:
<ul>
<li> Using bash command: <code>bash initial-setup.sh</code>
<li> Making script executable: <code>chmod +x initial-setup.sh</code> and then <code>./initial-setup.sh</code>
</ul>

### Start project
After the previous step is completed, the project can be started by using the code <code>docker-compose up</code>