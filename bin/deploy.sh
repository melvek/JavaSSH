#!/bin/sh

ENV=$1
shift

java -Dfile.encoding=UTF-8 -jar jssh.jar deploy "$@" -i "${ENV}.yaml"