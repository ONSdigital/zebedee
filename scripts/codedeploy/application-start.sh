#!/bin/bash

CONFIG_BUCKET=
ECR_REPOSITORY_URI=
GIT_COMMIT=

if [[ $DEPLOYMENT_GROUP_NAME =~ ^production-.+ ]]; then
  CONFIG_FILE=production.sh
elif [[ $DEPLOYMENT_GROUP_NAME =~ ^sandpit-.+ ]]; then
  CONFIG_FILE=sandpit.sh
else
  CONFIG_FILE=development.sh
fi

aws s3 cp s3://$CONFIG_BUCKET/zebedee/$CONFIG_FILE . || exit $?

source $CONFIG_FILE && docker run -d                                 \
  --env=audit_db_enabled=$AUDIT_ENABLED                              \
  --env=brian_url=http://brian:8080                                  \
  --env=ELASTIC_SEARCH_CLUSTER=cluster                               \
  --env=ELASTIC_SEARCH_SERVER=elasticsearch                          \
  --env=db_audit_password=$AUDIT_DB_PASSWORD                         \
  --env=db_audit_url=$AUDIT_DB_URI                                   \
  --env=db_audit_username=$AUDIT_DB_USERNAME                         \
  --env=MATHJAX_SERVICE_URL=http://mathjax:8080                      \
  --env=publish_url=$PUBLISH_URL                                     \
  --env=publish_verification_enabled=$VERIFY_PUBLICATIONS            \
  --env=scheduled_publishing_enabled=$SCHEDULED_PUBLICATIONS_ENABLED \
  --env=slack_alarm_channel=$SLACK_ALARM_CHANNEL                     \
  --env=slack_api_token=$SLACK_API_TOKEN                             \
  --env=website_reindex_key=$REINDEXING_KEY                          \
  --env=website_url=$WEBSITE_URL                                     \
  --env=zebedee_root=/content                                        \
  --name=zebedee                                                     \
  --net=publishing                                                   \
  --restart=always                                                   \
  --volume=/var/florence:/content                                    \
  $ECR_REPOSITORY_URI/zebedee:$GIT_COMMIT
