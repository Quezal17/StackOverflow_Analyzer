FROM openjdk:8

ENV SPARK_VERSION "2.4.6"
ENV SPARK_DIR "/opt/spark"
ENV PATH $SPARK_DIR/bin:$PATH
ENV PATH "/root/.local/bin:$PATH"
ENV PATH "/opt/tap/code:$PATH"
ENV SPARK_SETUP "Setup"
ENV PYTHON_DIR "PythonCode"

RUN apt-get update && apt-get -y install python3.7 python3-pip

RUN pip3 install pyspark==2.4.6    

ENV PYSPARK_DRIVER_PYTHON python3.7
ENV PYSPARK_PYTHON python3.7

ADD ${SPARK_SETUP}/spark-${SPARK_VERSION}-bin-hadoop2.7.tgz /opt

# Create Sym Link 
RUN ln -s /opt/spark-${SPARK_VERSION}-bin-hadoop2.7 ${SPARK_DIR} 

COPY ${PYTHON_DIR}/*  /opt/tap/code/

# Add Spark Starter
COPY ${SPARK_SETUP}/spark-starter.sh $SPARK_DIR/bin/

WORKDIR ${SPARK_DIR}/bin
CMD ./spark-starter.sh