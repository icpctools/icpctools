#!/bin/bash
#
# Purpose: to run a standalone resolver
#   or to connect to a CDS and control the resolution
#   or to connect to a CDS and view the resolution
#

set -e

export LIBDIR=$( dirname "${BASH_SOURCE}[0]" )/lib
UNAME=$( uname  -s )

java -Xmx1024m -cp "$LIBDIR/*" org.icpc.tools.resolver.Resolver "$@"
