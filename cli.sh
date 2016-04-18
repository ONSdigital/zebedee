#!/bin/bash

mvn package -DskipTests

java -jar zebedee-cli/target/zebedee-cli-0.0.1-SNAPSHOT.jar $1
