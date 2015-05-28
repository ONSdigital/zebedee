va

# Install git and maven

RUN \
  apt-get update && \
  apt-get -y dist-upgrade && \
  apt-get install -y git maven 

# Consul agent - /usr/local/bin

ADD https://dl.bintray.com/mitchellh/consul/0.5.2_linux_amd64.zip /tmp/0.5.2_linux_amd64.zip
WORKDIR /usr/local/bin
RUN unzip /tmp/0.5.2_linux_amd64.zip
WORKDIR /etc/consul.d
RUN echo '{"service": {"name": "zebedee", "tags": ["blue"], "port": 8080, "check": {"script": "curl http://localhost:8080 >/dev/null 2>&1", "interval": "10s"}}}'  >zebedee.json

# Check out code from Github

WORKDIR /usr/src
RUN git clone https://github.com/Carboni/zebedee.git
WORKDIR zebedee
RUN git checkout develop

# Pne-download dependencies:

RUN mvn install

# Build the entry point script

ENV PACKAGE_PREFIX com.github.onsdigital.zebedee.api
RUN echo "#!/bin/bash" >> zebedee.sh
# Disabled for now: RUN echo "consul agent -data-dir /tmp/consul -config-dir /etc/consul.d -join=192.168.15.7 -join=192.168.15.8 -join=192.168.15.10 -join=192.168.15.9 &" > zebedee.sh
RUN echo "java -Drestolino.packageprefix=$PACKAGE_PREFIX -jar target/*-jar-with-dependencies.jar" >> zebedee.sh
RUN chmod u+x zebedee.sh

CMD ["./zebedee.sh"]
