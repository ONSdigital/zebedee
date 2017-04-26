#!/bin/bash -eux

tar zxfv cms-build-bundle/*.tar.gz -C zebedee-cms
tar zxfv reader-build-bundle/*.tar.gz && mv revision target/
