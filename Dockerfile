from onsdigital/java-component

# Consul

WORKDIR /etc/consul.d
RUN echo '{"service": {"name": "zebedee", "tags": ["blue"], "port": 8080, "check": {"script": "curl http://localhost:8080 >/dev/null 2>&1", "interval": "10s"}}}' > zebedee.json

# Check out from Github

WORKDIR /usr/src
RUN git clone https://github.com/Carboni/zebedee.git .
RUN git checkout develop

# Build jar-with-dependencies:

RUN mvn clean install -DskipTests

# Update the entry point script

ENV PACKAGE_PREFIX com.github.onsdigital.zebedee.api
RUN echo "java -Drestolino.packageprefix=$PACKAGE_PREFIX -jar target/*-jar-with-dependencies.jar" >> container.sh
