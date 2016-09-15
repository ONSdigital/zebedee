#!/bin/bash

AWS_REGION=
CONFIG_BUCKET=
ECR_REPOSITORY_URI=
GIT_COMMIT=

INSTANCE=$(curl -s http://instance-data/latest/meta-data/instance-id)
CONFIG=$(aws --region $AWS_REGION ec2 describe-tags --filters "Name=resource-id,Values=$INSTANCE" "Name=key,Values=Configuration" --output text | awk '{print $5}')

(aws s3 cp s3://$CONFIG_BUCKET/zebedee/$CONFIG.asc . && gpg --decrypt $CONFIG.asc > $CONFIG) || exit $?

source $CONFIG && docker run -d                                                         \
  --env=audit_db_enabled=$AUDIT_ENABLED                                                 \
  --env=BABBAGE_URL=$BABBAGE_URL                                                        \
  --env=brian_url=http://brian:8080                                                     \
  --env=enable_splunk_reporting=$ENABLE_SPLUNK_REPORTING                                \
  --env=ELASTIC_SEARCH_CLUSTER=cluster                                                  \
  --env=ELASTIC_SEARCH_SERVER=elasticsearch                                             \
  --env=db_audit_password=$AUDIT_DB_PASSWORD                                            \
  --env=db_audit_url=$AUDIT_DB_URI                                                      \
  --env=db_audit_username=$AUDIT_DB_USERNAME                                            \
  --env=enable_influx_reporting=$ENABLE_INFLUX_REPORTING                                \
  --env=influxdb_url=$INFLUXDB_URL                                                      \
  --env=MATHJAX_SERVICE_URL=http://mathjax:8080                                         \
  --env=publish_url=$PUBLISH_URL                                                        \
  --env=publish_verification_enabled=$VERIFY_PUBLICATIONS                               \
  --env=scheduled_publishing_enabled=$SCHEDULED_PUBLICATIONS_ENABLED                    \
  --env=slack_alarm_channel=$SLACK_ALARM_CHANNEL                                        \
  --env=slack_api_token=$SLACK_API_TOKEN                                                \
  --env=splunk_http_event_collection_host=$SPLUNK_HTTP_EVENT_COLLECTOR_HOST             \
  --env=splunk_http_event_collection_port=$SPLUNK_HTTP_EVENT_COLLECTOR_PORT             \
  --env=splunk_http_event_collection_uri=$SPLUNK_HTTP_EVENT_COLLECTOR_URI               \
  --env=splunk_http_event_collection_auth_token=$SPLUNK_HTTP_EVENT_COLLECTOR_AUTH_TOKEN \
  --env=website_reindex_key=$REINDEXING_KEY                                             \
  --env=website_url=$WEBSITE_URL                                                        \
  --env=zebedee_root=/content                                                           \
  --name=zebedee                                                                        \
  --net=publishing                                                                      \
  --restart=always                                                                      \
  --volume=/var/florence:/content                                                       \
  $ECR_REPOSITORY_URI/zebedee:$GIT_COMMIT
