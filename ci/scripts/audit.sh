#!/bin/bash -eux

pushd zebedee
    mvn clean install dependency:copy-dependencies -Dmaven.test.skip=true -Dossindex.skip=true

    if [[ "$APPLICATION" == "zebedee" ]]; then
        make audit-cms
    elif [[ "$APPLICATION" == "zebedee-reader" ]]; then
        make audit-reader
    fi
popd
