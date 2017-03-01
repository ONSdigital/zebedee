#!/bin/bash -eux

pushd zebedee
  mvn clean package dependency:copy-dependencies -DskipTests=true
popd

cp -r zebedee/zebedee-cms/target zebedee-cms/
cp -r zebedee/zebedee-reader/target zebedee-reader/
