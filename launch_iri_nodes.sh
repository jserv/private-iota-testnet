#!/bin/bash

# set -o xtrace

function run_node() {
	DIR="nodes/$1"
	echo "node: $DIR, port=$2,udp$3,tcp$4, neighbor_port=$5"
	mkdir -p $DIR
	cp iri-testnet.jar "${DIR}/node.jar"
	pushd $DIR
	java -jar node.jar --testnet -p $2 -u $3 -t $4 -n "udp://127.0.0.1:$5" >> ../../nodes.log 2>&1 &
	popd
}

rm -f nodes.log
rm -rf nodes
mkdir -p nodes

run_node 0 15000 15001 15002 14600

trap 'kill $(jobs -p)' EXIT

tail -f nodes.log
