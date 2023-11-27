#!/usr/bin/env bash

export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"
export PORT="${PORT:-8082}"

# Restolino configuration
export RESTOLINO_CLASSES="zebedee-reader/target/classes"
export PACKAGE_PREFIX=com.github.onsdigital.zebedee.reader.api
export CONTENT_DIR="content"
export FORMAT_LOGGING=true

# CMD config (dev local values)
export ENABLE_DATASET_IMPORT=true
export DATASET_API_URL="http://localhost:22000"
export DATASET_API_AUTH_TOKEN="FD0108EA-825D-411C-9B1D-41EF7727F465"

export FORMAT_LOGGING=true

# Development: reloadable
mvn clean package dependency:copy-dependencies -Dmaven.test.skip=true -Dossindex.skip=true && \
java $JAVA_OPTS \
 -DFORMAT_LOGGING=$FORMAT_LOGGING \
 -Dlogback.configurationFile=zebedee-reader/target/classes/logback.xml \
 -Drestolino.classes=$RESTOLINO_CLASSES \
 -Dcontent_dir=$CONTENT_DIR \
 -DSTART_EMBEDDED_SERVER=N \
 -Drestolino.packageprefix=$PACKAGE_PREFIX \
 -DFORMAT_LOGGING=$FORMAT_LOGGING \
 -javaagent:zebedee-cms/target/dependency/aws-opentelemetry-agent-1.31.0.jar \
 -Dotel.propagators=tracecontext,baggage \
 -cp "zebedee-reader/target/classes/:zebedee-reader/target/dependency/*" \
 com.github.davidcarboni.restolino.Main

