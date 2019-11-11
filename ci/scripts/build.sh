#!/bin/bash -eux

pushd zebedee
    mvn -DskipTests=true clean package dependency:copy-dependencies

    if [[ "$APPLICATION" == "zebedee" ]]; then
        cp -r Dockerfile.concourse zebedee-cms/target/* ../build
    elif [[ "$APPLICATION" == "zebedee-reader" ]]; then
        mkdir ../build/zebedee-reader
        cp -r zebedee-reader/Dockerfile.concourse zebedee-reader/target/* ../build
    fi
popd
