from carboni.io/java-component

# Consul
WORKDIR /etc/consul.d
RUN echo '{"service": {"name": "zebedee-cms", "tags": ["blue"], "port": 8080, "check": {"script": "curl http://localhost:8080 >/dev/null 2>&1", "interval": "10s"}}}' > zebedee.json

# Add the repo source
WORKDIR /usr/src
ADD git_commit_id /usr/src/
ADD ./zebedee-cms/target/*-jar-with-dependencies.jar /usr/src/target/

# Update the entry point script
RUN mv /usr/entrypoint/container.sh /usr/src/
ENV PACKAGE_PREFIX com.github.onsdigital.zebedee.api
RUN echo "java -Drestolino.packageprefix=$PACKAGE_PREFIX -jar zebedee-cms/target/*-jar-with-dependencies.jar" >> container.sh
