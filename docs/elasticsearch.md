# Elasticsearch
<img src="./project-overview/images/elasticsearch-logo.png" width="600" height="300"/>

## What is Elasticsearch?

<a href="https://www.elastic.co/what-is/elasticsearch">Elasticsearch</a> is a distributed, open source search and analytics engine for all types of data, including textual, numerical, geospatial, structured, and unstructured. Elasticsearch is built on Apache Lucene and was first released in 2010 by Elasticsearch N.V. (now known as Elastic). Known for its simple REST APIs, distributed nature, speed, and scalability, Elasticsearch is the central component of the Elastic Stack, a set of open source tools for data ingestion, enrichment, storage, analysis, and visualization.

In this project, Elasticsearch is used to index data in JSON format processed by the Spark application.

## Elasticsearch Volume
It is implemented a Docker volume, named *elasticsearch*, to keep data when the container is deleted or pruned.

## Docker container information

- **Name**: stackoflw-elasticsearch
- **IP Address**: 10.0.100.51
- **Ports**: 9200:9200
