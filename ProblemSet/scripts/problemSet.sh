#!/bin/bash
#
# Purpose: to edit problemset.yaml
#

set -e

export LIBDIR=$( dirname "${BASH_SOURCE}[0]" )/lib
UNAME=$( uname  -s )
vmoptions=
if [ "$UNAME" == "Darwin" ]; then
   vmoptions=-XstartOnFirstThread
fi

java $vmoptions -jar "$LIBDIR/swtLauncher.jar" problemSet.jar,snakeyaml-1.18.jar org.icpc.tools.contest.util.problemset.ProblemSetEditor "$@"
