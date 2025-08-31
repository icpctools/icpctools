#!/usr/bin/env bash
#
# Purpose: run a Presentation Admin
#

export ROOTDIR=$( dirname "${BASH_SOURCE}[0]" )
cd $ROOTDIR

UNAME=$( uname  -s )
vmoptions=
if [ "$UNAME" == "Darwin" ]; then
   vmoptions=-XstartOnFirstThread
fi

while true; do
  java $vmoptions -cp lib/swtLauncher.jar org.icpc.tools.contest.SWTLauncher org.icpc.tools.presentation.admin.internal.Admin "$@"
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
