#!/bin/bash

export JAVA_OPTS=" -Xmx1204m -Xdebug -Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"
export PORT="${PORT:-8082}"

# Restolino configuration
export RESTOLINO_STATIC="src/main/resources/files"
export RESTOLINO_CLASSES="zebedee-cms/target/classes"
export PACKAGE_PREFIX=com.github.onsdigital.zebedee

export FORMAT_LOGGING=true

# CMD config (dev local values)
export ENABLE_DATASET_IMPORT=true
export ENABLE_PERMISSIONS_AUTH=true

# Dev local defaults for Central Keyring (safe to commit).
export ENABLE_CENTRALISED_KEYRING=false
export KEYRING_SECRET_KEY="38c03PzhNuSrYV8J0537XQ=="
export KEYRING_INIT_VECTOR="RkL9MmjfRcPB86alO82gHQ=="

export DATASET_API_URL="http://localhost:22000"
export DATASET_API_AUTH_TOKEN="FD0108EA-825D-411C-9B1D-41EF7727F465"

# Development: reloadable
mvn clean package dependency:copy-dependencies -Dmaven.test.skip=true -Dossindex.skip=true && \
java $JAVA_OPTS \
 -Dlogback.configurationFile=zebedee-cms/target/classes/logback.xml \
 -DFORMAT_LOGGING=$FORMAT_LOGGING \
 -Drestolino.files=$RESTOLINO_STATIC \
 -Drestolino.files=$RESTOLINO_STATIC \
 -Drestolino.classes=$RESTOLINO_CLASSES \
 -Drestolino.packageprefix=$PACKAGE_PREFIX \
 -DSTART_EMBEDDED_SERVER=N \
 -cp "zebedee-cms/target/classes:zebedee-cms/target/dependency/*" \
 com.github.davidcarboni.restolino.Main

