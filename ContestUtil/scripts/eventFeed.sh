#!/usr/bin/env bash

libdir=`dirname "$0"`/lib

java -cp "$libdir/*" org.icpc.tools.contest.util.EventFeedUtil "$@"
exit 0
