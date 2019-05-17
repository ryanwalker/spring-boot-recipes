#!/usr/bin/env bash

STATUS=`curl -s http://localhost:8080/actuator/health | jq .status --raw-output`

if [[ "$STATUS" == "UP" ]]; then
  echo "App is healthy";
  exit 0;
else
  echo "Unavailable";
  exit 1;
fi
