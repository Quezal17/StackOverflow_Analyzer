FROM maven:latest
ENV PATH /opt/kafka/bin:$PATH
ENV KAFKA_DIR "/opt/kafka"
ENV ZOOKEEPER_SERVER "10.0.100.22:2181"
ENV STACKOFLW_CONNECTOR "Stackoverflow-Connector"
ENV STACKOFLW_SETUP "Setup"
ENV STACKOFLW_CONNECT_DIR "/opt/connector"
ENV KAFKA_PLUGINS_DIR "/usr/local/share/kafka/plugins/"

ARG KAFKA_VERSION="2.12-2.5.0"

# Installing Kafka
ADD ${STACKOFLW_SETUP}/kafka_${KAFKA_VERSION}.tgz /opt

# Create Sym Link 
RUN ln -s /opt/kafka_${KAFKA_VERSION} ${KAFKA_DIR} 

#Copy properties
COPY ${STACKOFLW_SETUP}/source-setup.properties ${KAFKA_DIR}/conf/
COPY ${STACKOFLW_SETUP}/worker.properties ${KAFKA_DIR}/conf/

COPY ${STACKOFLW_SETUP}/Stackoverflow-Connector-0.0.1-SNAPSHOT.jar ${KAFKA_PLUGINS_DIR}

COPY ${STACKOFLW_SETUP}/kafka-starter.sh ${KAFKA_DIR}

WORKDIR ${KAFKA_DIR}
CMD ./kafka-starter.sh