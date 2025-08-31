#!/usr/bin/env bash
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

java $vmoptions -cp lib/swtLauncher.jar org.icpc.tools.contest.SWTLauncher org.icpc.tools.contest.util.problemset.ProblemSetEditor "$@"
