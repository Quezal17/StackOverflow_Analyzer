#FROM openjdk:8
FROM maven:latest

ENV SPARK_VERSION "3.0.0"
ENV SPARK_DIR "/opt/spark"
ENV PATH $SPARK_DIR/bin:$PATH
ENV PATH "/root/.local/bin:$PATH"
ENV PATH "/opt/code:$PATH"
ENV SPARK_SETUP "Setup"
ENV PYTHON_DIR "PythonCode"
ENV CODE_DIR "/opt/code/"

ADD ${SPARK_SETUP}/spark-${SPARK_VERSION}-bin-hadoop2.7.tgz /opt

# Create Sym Link 
RUN ln -s /opt/spark-${SPARK_VERSION}-bin-hadoop2.7 ${SPARK_DIR} 

COPY ${SPARK_SETUP}/Spark-Consumer-0.0.1-SNAPSHOT.jar ${CODE_DIR}
COPY ${SPARK_SETUP}/trainingDataset.csv ${CODE_DIR}

# Add Spark Starter
COPY ${SPARK_SETUP}/spark-starter.sh ${CODE_DIR}

WORKDIR ${CODE_DIR}
CMD ./spark-starter.sh