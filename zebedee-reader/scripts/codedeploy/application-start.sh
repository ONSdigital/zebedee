#!/bin/bash

ECR_REPOSITORY_URI=
GIT_COMMIT=

docker run -d                               \
  --env=content_dir=/content                \
  --env=ELASTIC_SEARCH_CLUSTER=cluster      \
  --env=ELASTIC_SEARCH_SERVER=elasticsearch \
  --name=zebedee-reader                     \
  --net=website                             \
  --restart=always                          \
  --volume=/var/babbage/site:/content:ro    \
  $ECR_REPOSITORY_URI/zebedee-reader:$GIT_COMMIT
