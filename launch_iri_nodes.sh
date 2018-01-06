#!/bin/bash

set -o xtrace

function run_node() {
	DIR="nodes/$1"
	PORT=$2
	NEIGHBOR_PORT=$3
	echo "node: $DIR, port=$PORT, neighbor_port=$NEIGHBOR_PORT"
	mkdir -p $DIR
	cp iri-testnet.jar "${DIR}/"
	pushd $DIR
	java -jar iri-testnet.jar --testnet -p $PORT -u $PORT -t $PORT -n "udp://127.0.0.1:$3" >> ../../nodes.log 2>&1 &
	popd
}

rm -f nodes.log
rm -rf nodes
mkdir -p nodes

# run_node 0 14800 14801
run_node 1 12345 14800

trap 'kill $(jobs -p)' EXIT

tail -f nodes.log
