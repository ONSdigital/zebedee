#!/bin/bash -eux

pushd zebedee
	sed appspec.yml zebedee-reader/appspec.yml scripts/codedeploy/* zebedee-reader/scripts/codedeploy/* -i \
  -e s/\${CODEDEPLOY_USER}/$CODEDEPLOY_USER/g                         \
  -e s/^CONFIG_BUCKET=.*/CONFIG_BUCKET=$CONFIGURATION_BUCKET/         \
  -e s/^ECR_REPOSITORY_URI=.*/ECR_REPOSITORY_URI=$ECR_REPOSITORY_URI/ \
  -e s/^GIT_COMMIT=.*/GIT_COMMIT=$(cat ../target/revision)/           \
  -e s/^AWS_REGION=.*/AWS_REGION=$AWS_REGION/
popd

mv zebedee-cms/revision revisions/zebedee-cms
mv target/revision revisions/zebedee-reader

mkdir -p artifacts/zebedee/scripts/codedeploy
mkdir -p artifacts/zebedee-reader/scripts/codedeploy

cp zebedee/appspec.yml artifacts/zebedee/
cp zebedee/zebedee-reader/appspec.yml artifacts/zebedee-reader/

cp zebedee/scripts/codedeploy/* artifacts/zebedee/scripts/codedeploy
cp zebedee/zebedee-reader/scripts/codedeploy/* artifacts/zebedee-reader/scripts/codedeploy
