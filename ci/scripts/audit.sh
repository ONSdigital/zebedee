#!/bin/bash -eux

pushd zebedee
    if [[ "$APPLICATION" == "zebedee" ]]; then
        make audit-cms
    elif [[ "$APPLICATION" == "zebedee-reader" ]]; then
        make audit-reader
    fi
popd
