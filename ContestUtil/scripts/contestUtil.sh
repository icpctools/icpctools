#!/bin/bash

libdir=`dirname "$0"`/lib

java -jar "$libdir/contestUtil.jar" "$@"
exit 0
