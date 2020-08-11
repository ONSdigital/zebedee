#!/bin/bash -eux

pushd zebedee
    if [[ "$APPLICATION" == "zebedee" ]]; then
        make build-cms
        cp -r Dockerfile.concourse zebedee-cms/target/* ../build
    elif [[ "$APPLICATION" == "zebedee-reader" ]]; then
        make build-reader
        mkdir ../build/zebedee-reader
        cp -r zebedee-reader/Dockerfile.concourse zebedee-reader/target/* ../build
    fi
popd
