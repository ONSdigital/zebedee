#!/bin/bash -eux

pushd zebedee
  mvn -DskipTests=true clean package dependency:copy-dependencies
  cp -r Dockerfile.concourse zebedee-cms/target/* ../build

  mkdir ../build/zebedee-reader
  cp -r zebedee-reader/Dockerfile.concourse zebedee-reader/target/* ../build/zebedee-reader
popd
