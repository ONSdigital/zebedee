#!/bin/bash

AWS_REGION=
CONFIG_BUCKET=
ECR_REPOSITORY_URI=
GIT_COMMIT=

INSTANCE=$(curl -s http://instance-data/latest/meta-data/instance-id)
INSTANCE_NUMBER=$(aws --region $AWS_REGION ec2 describe-tags --filters "Name=resource-id,Values=$INSTANCE" "Name=key,Values=Name" --output text | awk '{print $6}')
CONFIG=$(aws --region $AWS_REGION ec2 describe-tags --filters "Name=resource-id,Values=$INSTANCE" "Name=key,Values=Configuration" --output text | awk '{print $5}')

(aws s3 cp s3://$CONFIG_BUCKET/zebedee-reader/$CONFIG.asc . && gpg --decrypt $CONFIG.asc > $CONFIG) || exit $?

source $CONFIG

if [[ $INSTANCE_NUMBER == 1 ]]; then
  ELASTICSEARCH_HOST=$ELASTICSEARCH_1
else
  ELASTICSEARCH_HOST=$ELASTICSEARCH_2
fi

docker run -d                                                                           \
  --env=content_dir=/content                                                            \
  --env=enable_splunk_reporting=$ENABLE_SPLUNK_REPORTING                                \
  --env=ELASTIC_SEARCH_CLUSTER=cluster                                                  \
  --env=ELASTIC_SEARCH_SERVER=$ELASTICSEARCH_HOST                                       \
  --env=splunk_http_event_collection_host=$SPLUNK_HTTP_EVENT_COLLECTOR_HOST             \
  --env=splunk_http_event_collection_port=$SPLUNK_HTTP_EVENT_COLLECTOR_PORT             \
  --env=splunk_http_event_collection_uri=$SPLUNK_HTTP_EVENT_COLLECTOR_URI               \
  --env=splunk_http_event_collection_auth_token=$SPLUNK_HTTP_EVENT_COLLECTOR_AUTH_TOKEN \
  --name=zebedee-reader                                                                 \
  --net=website                                                                         \
  --restart=always                                                                      \
  --volume=/var/babbage/site:/content:ro                                                \
  $ECR_REPOSITORY_URI/zebedee-reader:$GIT_COMMIT
