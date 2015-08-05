#!/bin/bash

export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"
export PORT="8082"

# Restolino configuration
export RESTOLINO_STATIC="src/main/resources/files"
export RESTOLINO_CLASSES="zebedee-cms/target/classes"
export PACKAGE_PREFIX=com.github.onsdigital.zebedee

# Development: reloadable
mvn clean package dependency:copy-dependencies && \
java $JAVA_OPTS \
 -Drestolino.files=$RESTOLINO_STATIC \
 -Drestolino.classes=$RESTOLINO_CLASSES \
 -Drestolino.packageprefix=$PACKAGE_PREFIX \
 -cp "zebedee-cms/target/dependency/*" \
 com.github.davidcarboni.restolino.Main

#mvn package && \
#java $JAVA_OPTS \
# -Drestolino.packageprefix=$PACKAGE_PREFIX \
# -jar zebedee-cms/target/*-jar-with-dependencies.jar

