FROM azul/zulu-openjdk:11-latest

RUN mkdir -p /opt/c5
COPY corda-combined-worker-5.0.0.0-Fox-SNAPSHOT.jar /opt/c5/corda-combined-worker.jar
COPY jdbcDrivers/postgresql-42.4.1.jar /opt/c5/jdbcDrivers
COPY run-c5.sh /opt/c5/run-c5.sh
WORKDIR /opt/c5
RUN chmod +x run-c5.sh

EXPOSE 8888
ENTRYPOINT ["./run-c5.sh"]
