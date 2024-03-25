FROM openjdk:8-jre

# Add the repo source
WORKDIR /usr/src
ADD ./zebedee-cms/target/dependency /usr/src/target/dependency
ADD ./zebedee-cms/target/classes /usr/src/target/classes

# Temporary: expose Elasticsearch
EXPOSE 9200

# Update the entry point script
ENTRYPOINT java -Xmx2048m -agentlib:jdwp=transport=dt_socket,address=8002,server=y,suspend=n \
          -Drestolino.classes=target/classes \
          -Drestolino.packageprefix=com.github.onsdigital.zebedee.api \
          -javaagent:target/dependency/aws-opentelemetry-agent-1.32.0.jar \
          -Dotel.propagators=tracecontext,baggage \
          -Dotel.service.name=zebedee \
          -Dotel.javaagent.enabled=false \
          -cp "target/dependency/*:target/classes/" \
          com.github.davidcarboni.restolino.Main
