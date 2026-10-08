#!/bin/bash
# Load .env, then start the app
set -a
source .env
set +a
./mvnw spring-boot:run
