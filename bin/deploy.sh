#!/bin/sh
ENV=$1
shift
java -Dfile.encoding=UTF-8 -jar jssh-1.1.0.jar deploy "$@" -i "${ENV}.yaml"