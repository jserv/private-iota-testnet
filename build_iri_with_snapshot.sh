#!/bin/bash

if [ ! -d "iri" ]; then
	git clone https://github.com/jserv/iri
fi

pushd iri

git reset --hard testing
git clean -fdx
git apply ../dist/iri-snapshot.patch

cp ../dist/Snapshot.txt src/main/resources

mvn package -Dmaven.test.skip=true

cp target/iri-*.jar ../iri-testnet.jar
