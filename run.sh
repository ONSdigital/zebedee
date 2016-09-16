#!/bin/bash

export JAVA_OPTS=" -Xmx512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"
export PORT="8082"

# Restolino configuration
export RESTOLINO_STATIC="src/main/resources/files"
export RESTOLINO_CLASSES="zebedee-cms/target/classes"
export PACKAGE_PREFIX=com.github.onsdigital.zebedee
export audit_db_enabled=false

# Development: reloadable
mvn clean package dependency:copy-dependencies -Dmaven.test.skip=true && \
java $JAVA_OPTS \
 -Dcolour_logging_enabled=$colour_logging_enabled \
 -Ddb_audit_url=$db_audit_url \
 -Daudit_db_enabled=$audit_db_enabled \
 -Ddb_audit_username=$db_audit_username \
 -Ddb_audit_password=$db_audit_password \
 -Drestolino.files=$RESTOLINO_STATIC \
 -Drestolino.files=$RESTOLINO_STATIC \
 -Drestolino.classes=$RESTOLINO_CLASSES \
 -Drestolino.packageprefix=$PACKAGE_PREFIX \
 -DSTART_EMBEDDED_SERVER=Y \
 -cp "zebedee-cms/target/classes:zebedee-cms/target/dependency/*" \
 com.github.davidcarboni.restolino.Main

#mvn package && \
#java $JAVA_OPTS \
# -Drestolino.packageprefix=$PACKAGE_PREFIX \
# -jar zebedee-cms/target/*-jar-with-dependencies.jar

