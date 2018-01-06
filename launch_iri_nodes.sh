#!/bin/bash

set -o xtrace

function make_node() {
	DIR="node$1"
	PORT=$2
	NEIGHBOR_PORT=$3
	echo "node: $DIR, port=$PORT, neighbor_port=$NEIGHBOR_PORT"
	rm -rf $DIR
	mkdir -p $DIR
	cp iri-testnet.jar "${DIR}/"
	pushd $DIR
	java -jar iri-testnet.jar --testnet -p $PORT -u $PORT -t $PORT -n "udp://127.0.0.1:$3" >> ../out.txt 2>&1 &
	popd
}

make_node 0 14800 14801
make_node 1 14801 14800

trap 'kill $(jobs -p)' EXIT

tail -f out.txt
