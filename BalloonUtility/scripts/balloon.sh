#!/bin/bash
#
# Purpose: run the balloon utility
#

export ROOTDIR=$( dirname "${BASH_SOURCE}[0]" )
cd $ROOTDIR

UNAME=$( uname  -s )
vmoptions=
if [ "$UNAME" == "Darwin" ]; then
   vmoptions=-XstartOnFirstThread
fi

# workaround java not looking at LC_PAPER
if [[ "x$LC_ALL" != "x" && "x$LC_PAPER" != "x" && $LC_PAPER != $LC_ALL ]]; then 
LC_ALL=$LC_PAPER
fi

while true; do
  java $vmoptions -jar lib/swtLauncher.jar balloonUtil.jar org.icpc.tools.balloon.BalloonUtility "$@"
  result=$?
  if [ $result = 254 ]
  then
    echo Update downloaded, applying
    rm -rf lib
    mv -f update/* .
    continue
  fi
  [[ $result = 255 ]] || break
done