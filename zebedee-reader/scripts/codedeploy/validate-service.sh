#!/bin/bash

if [[ $(docker inspect --format="{{ .State.Running }}" zebedee-reader) == "false" ]]; then
  exit 1;
fi
