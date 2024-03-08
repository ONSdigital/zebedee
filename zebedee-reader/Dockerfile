FROM openjdk:8-jre

# Add the repo source
WORKDIR /usr/src
ADD ./target/dependency /usr/src/target/dependency
ADD ./target/classes /usr/src/target/classes

# Temporary: expose Elasticsearch
EXPOSE 9200

# Update the entry point script
ENTRYPOINT java -Xmx2048m \
          -Drestolino.classes=target/classes \
          -Drestolino.packageprefix=com.github.onsdigital.zebedee.reader.api \
          -javaagent:target/dependency/aws-opentelemetry-agent-1.32.0.jar \
          -Dotel.propagators=tracecontext,baggage \
          -Dotel.javaagent.enabled=false \
          -Dotel.service.name=zebedee \
          -cp "target/dependency/*:target/classes/" \
          com.github.davidcarboni.restolino.Main
