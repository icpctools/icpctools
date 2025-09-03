#!/usr/bin/env bash
#
# Purpose: launch the coach view
#

export ROOTDIR=$( dirname "${BASH_SOURCE}[0]" )
cd $ROOTDIR

while true; do
  java -cp "lib/*" org.icpc.tools.coachview.CoachView "$@"
  result=$?
  if [ $result = 254 ]
  then
    echo Update downloaded, applying
    rm -rf lib
    mv -f update/* .
    continue
  elif [ $result = 253 ]
  then
    echo Clearing cache and restarting
    rm -rf "${TMPDIR:-/tmp}/org.icpc.tools.cache"*
    continue
  fi
  [[ $result = 255 ]] || break
done