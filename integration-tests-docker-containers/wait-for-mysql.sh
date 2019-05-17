#!/bin/bash
set -e

# shift removes the first parameter, which is `--` from docker-compose.yml
shift
# This is the command after the `--` in docker-compose.yml
startAppCommand="$@"

TIMEOUT_SECONDS=20
# The SECONDS variable automatically increments each second (shell black magic)
SECONDS=0

# Ping mysql until it is up
until mysqladmin ping -h testdb -uroot -proot; do
  >&2 echo "MySQL is starting up"
  sleep 1

  if [[ $SECONDS -gt ${TIMEOUT_SECONDS} ]]; then
    >&2 echo "Timed out waiting for MySQL"
    break;
  fi

done

# Star up app
exec $startAppCommand
