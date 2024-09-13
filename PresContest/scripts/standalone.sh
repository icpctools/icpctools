#!/usr/bin/env bash
#
# Purpose: display presentations in standalone mode
#

set -e

export LIBDIR=$( dirname "${BASH_SOURCE}[0]" )/lib

java -Xmx1024m -cp "$LIBDIR/*" org.icpc.tools.presentation.contest.internal.standalone.StandaloneLauncher "$@"
