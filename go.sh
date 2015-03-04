#!/bin/bash

export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"

# Restolino configuration
export RESTOLINO_STATIC="src/main/resources/files"
export RESTOLINO_CLASSES="target/classes"
export PACKAGE_PREFIX=com.github.onsdigital.zebedee.api

# Development: reloadable
mvn test dependency:copy-dependencies  && \
java $JAVA_OPTS \
 -Drestolino.files=$RESTOLINO_STATIC \
 -Drestolino.classes=$RESTOLINO_CLASSES \
 -Drestolino.packageprefix=$PACKAGE_PREFIX \
 -cp "target/dependency/*" \
 com.github.davidcarboni.restolino.Main
 
#mvn package && \
#java $JAVA_OPTS \
# -Drestolino.packageprefix=$PACKAGE_PREFIX \
# -jar target/*-jar-with-dependencies.jar

