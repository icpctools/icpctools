#!/bin/bash
#
# Purpose: to apply awards to an event feed 
#

set -e

export LIBDIR=$( dirname "${BASH_SOURCE}[0]" )/lib
UNAME=$( uname  -s )
vmoptions=
if [ "$UNAME" == "Darwin" ]; then
   vmoptions=-XstartOnFirstThread
fi

java $vmoptions -jar "$LIBDIR/swtLauncher.jar" resolver.jar org.icpc.tools.resolver.awards.Awards "$@"
