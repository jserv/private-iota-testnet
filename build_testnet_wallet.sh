#!/bin/bash

rm -rf wallet

git clone https://github.com/iotaledger/wallet

pushd wallet

git clean -fdx
git checkout tags/v2.5.1

git clone https://github.com/iotaledger/iri

pushd iri

mvn package
cp target/iri-*.jar iri-testnet.jar

popd

cp package.testnet.json package.json

brew update
brew install node@8
brew link node@8

export PATH="/usr/local/opt/node@8/bin:$PATH"

npm install bower
npm install

npm run compile:mac

mv out/mac/*.app ..