#!/bin/sh
# Runs once, on the first start of an empty data volume.
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -c "CREATE DATABASE \"${POSTGRES_TEST_DB:-proxy_test}\" OWNER \"$POSTGRES_USER\";"
