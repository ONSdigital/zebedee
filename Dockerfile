FROM openjdk:8-jre

# Add the repo source
WORKDIR /usr/src
ADD ./zebedee-cms/target/dependency /usr/src/target/dependency
ADD ./zebedee-cms/target/classes /usr/src/target/classes

# Temporary: expose Elasticsearch
EXPOSE 9200

# Update the entry point script
ENTRYPOINT java -Xmx2048m \
          -Drestolino.classes=target/classes \
          -Drestolino.packageprefix=com.github.onsdigital.zebedee.api \
          -cp "target/dependency/*:target/classes/" \
          com.github.davidcarboni.restolino.Main
