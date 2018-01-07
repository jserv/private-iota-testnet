#!/bin/bash

# set -o xtrace

function run_node() {
	DIR="nodes/$1"
	echo "node: $DIR, port=$2, udp=$3, tcp=$4, neighbors=$5"
	mkdir -p $DIR
	cp iri-testnet.jar "${DIR}/node.jar"
	pushd $DIR
	java -jar node.jar --debug --testnet -p $2 -u $3 -t $4 -n "$5" >> ../../nodes.log 2>&1 &
	popd
}

rm -f nodes.log
rm -rf nodes
mkdir -p nodes

function start_nodes() {
	N=$1
	for i in $(seq 1 $N); do
		HTTP=$((15000 + $i * 10))
		UDP=$(($HTTP + 1))
		TCP=$(($UDP + 1))
		HOST="udp://127.0.0.1"
		NB=""
		if [ $i -eq 1 ]; then
			NB="$HOST:$(($UDP+10))"
		elif [ $i -eq $N ]; then
			NB="$HOST:$(($UDP-10))"
		else
			NB="$HOST:$(($UDP-10)) $HOST:$(($UDP+10))"
		fi
		run_node $i $HTTP $UDP $TCP $NB
	done
}

start_nodes 5

trap 'kill $(jobs -p)' EXIT

tail -f nodes.log
