#!/bin/bash

source ./export-default-env-vars.sh

export JAVA_OPTS=" -Xmx1204m -Xdebug -Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"
export PORT="${PORT:-8082}"


# Restolino configuration
export RESTOLINO_STATIC="src/main/resources/files"
export RESTOLINO_CLASSES="zebedee-cms/target/classes"
export PACKAGE_PREFIX=com.github.onsdigital.zebedee
export audit_db_enabled=false

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

# Dev local defaults for Cognito Pool
if [[ "${ENABLE_JWT_SESSIONS}" == "true"
      && -z "${AWS_COGNITO_SIGNING_KEY_ONE}" && -z "${AWS_COGNITO_SIGNING_KEY_TWO}"
      && -z "${AWS_COGNITO_KEY_ID_ONE}" && -z "${AWS_COGNITO_KEY_ID_TWO}" ]]; then

  if ! command -v curl &> /dev/null; then
      echo "local dev autoconfiguration requires 'curl', either 'brew install jq' or set the 'AWS_COGNITO_*' configs manually"
      exit 1
  fi
  if ! command -v jq &> /dev/null; then
      echo "local dev autoconfiguration requires 'jq', either 'brew install jq' or set the 'AWS_COGNITO_*' configs manually"
      exit 1
  fi

  AWS_REGION=eu-west-1
  LOCAL_USER_POOL_ID=eu-west-1_Rnma9lp2q

  KEYS_JSON=$(curl "https://cognito-idp.${AWS_REGION}.amazonaws.com/$LOCAL_USER_POOL_ID/.well-known/jwks.json")

  export AWS_COGNITO_KEY_ID_ONE=$(echo $KEYS_JSON | jq '.keys[0].kid')
  export AWS_COGNITO_SIGNING_KEY_ONE=$(echo $KEYS_JSON | jq '.keys[1].n')
  export AWS_COGNITO_KEY_ID_TWO=$(echo $KEYS_JSON | jq '.keys[1].kid')
  export AWS_COGNITO_SIGNING_KEY_TWO=$(echo $KEYS_JSON | jq '.keys[1].n')
fi

# Development: reloadable
mvn clean package dependency:copy-dependencies -Dmaven.test.skip=true -Dossindex.skip=true && \
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

