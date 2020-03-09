#!/bin/bash
#
# Purpose: display presentations controlled by a CDS
#

export ROOTDIR=$( dirname "${BASH_SOURCE}[0]" )
cd $ROOTDIR

while true; do
  java -Xmx1024m -cp "lib/*" org.icpc.tools.presentation.contest.internal.ClientLauncher "$@"
  result=$?
  if [ $result = 254 ]
  then
    echo Update downloaded, applying
    rm -rf lib
    mv -f update/* .
    continue
  elif [ $result = 134 ]
  then
    # seg abort, restart
    continue
  elif [ $result = 139 ]
  then
    # seg fault, restart
    continue
  fi
  [[ $result = 255 ]] || break
done
