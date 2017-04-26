#!/bin/bash -eux

REV=$(cat revisions/zebedee-reader)

if ! [[ $REV =~ ^[0-9]+\.[0-9]+\.[0-9]+(\-rc[0-9]+)?$ ]]; then
  REV=$REV-1.0.0
fi

tar cvzf reader-deployment/$REV.tar.gz -C artifacts/zebedee-reader .
