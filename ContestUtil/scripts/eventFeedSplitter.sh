#!/bin/bash

libdir=`dirname "$0"`/lib

java -cp "$libdir/contestUtil.jar" org.icpc.tools.contest.util.EventFeedSplitter "$@"
exit 0
