FROM onsdigital/java-component

# Add the repo source
WORKDIR /usr/src
ADD git_commit_id /usr/src/
ADD ./zebedee-cms/target/dependency /usr/src/target/dependency
ADD ./zebedee-cms/target/classes /usr/src/target/classes
#ADD ./zebedee-cms/target/*-jar-with-dependencies.jar /usr/src/target/

# Temporary: expose Elasticsearch
EXPOSE 9200

# Update the entry point script
ENTRYPOINT java -Xmx2048m -javaagent:/usr/src/target/dependency/newrelic/newrelic.jar \
          -Drestolino.classes=target/classes \
          -Drestolino.packageprefix=com.github.onsdigital.zebedee.api \
          -cp "target/dependency/*:target/classes/" \
          com.github.davidcarboni.restolino.Main
