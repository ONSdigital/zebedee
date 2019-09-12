#!/bin/bash

source ./export-default-env-vars.sh

export JAVA_OPTS=" -Xmx1204m -Xdebug -Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"
export PORT="${PORT:-8082}"


# Restolino configuration
export RESTOLINO_STATIC="src/main/resources/files"
export RESTOLINO_CLASSES="zebedee-cms/target/classes"
export PACKAGE_PREFIX=com.github.onsdigital.zebedee
export audit_db_enabled=false
export enable_splunk_reporting=false

export FORMAT_LOGGING=true

# CMD config (dev local values)
export ENABLE_DATASET_IMPORT=true
export ENABLE_PERMISSIONS_AUTH=true

export DATASET_API_URL="http://localhost:22000"
export DATASET_API_AUTH_TOKEN="FD0108EA-825D-411C-9B1D-41EF7727F465"
export SERVICE_AUTH_TOKEN="fc4089e2e12937861377629b0cd96cf79298a4c5d329a2ebb96664c88df77b67"

# Development: reloadable
mvn clean package dependency:copy-dependencies -Dmaven.test.skip=true && \
java $JAVA_OPTS \
 -Dlogback.configurationFile=zebedee-cms/target/classes/logback.xml \
 -Ddb_audit_url=$db_audit_url \
 -DFORMAT_LOGGING=$FORMAT_LOGGING \
 -Daudit_db_enabled=$audit_db_enabled \
 -Ddb_audit_username=$db_audit_username \
 -Ddb_audit_password=$db_audit_password \
 -Drestolino.files=$RESTOLINO_STATIC \
 -Drestolino.files=$RESTOLINO_STATIC \
 -Drestolino.classes=$RESTOLINO_CLASSES \
 -Drestolino.packageprefix=$PACKAGE_PREFIX \
 -DSTART_EMBEDDED_SERVER=N \
 -cp "zebedee-cms/target/classes:zebedee-cms/target/dependency/*" \
 com.github.davidcarboni.restolino.Main

