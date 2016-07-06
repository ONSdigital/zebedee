#!/bin/bash

if [[ $(docker inspect --format="{{ .State.Running }}" zebedee) == "false" ]]; then
  exit 1;
fi
