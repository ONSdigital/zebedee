#!/bin/bash -eux

pushd zebedee
  mvn clean package dependency:copy-dependencies
popd
