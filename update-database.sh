#!/bin/bash

source ./export-default-env-vars.sh

mvn -f zebedee-cms/pom.xml liquibase:update
