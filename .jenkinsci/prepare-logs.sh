#!/usr/bin/env bash

mkdir -p build-logs

while read -r LINE
do
  docker logs $(echo $LINE | cut -d ' ' -f1) | \
    gzip -6 > build-logs/`echo $LINE | cut -d ' ' -f2`.log.gz
done < <(docker ps --filter "network=d3-${DOCKER_NETWORK}" --format "{{.ID}} {{.Names}}")

tar -zcvf build-logs/notaryEthIntegrationTest.gz \
  -C notary-eth-integration-test/build/reports/tests \
  integrationTest || true