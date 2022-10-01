#!/bin/bash

mkdir build
cd build
git clone https://github.com/andreypfau/kotlinio.git
cd kotlinio
./gradlew publishToMavenLocal
cd ..
git clone https://github.com/andreypfau/pcap-kotlin.git
cd pcap-kotlin
./gradlew publishToMavenLocal
cd ../../
./gradlew :ton-proxy-client-lib:build :ton-proxy-client-app:packageDmg
