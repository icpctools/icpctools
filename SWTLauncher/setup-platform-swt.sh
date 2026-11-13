#!/bin/sh
# Puts the SWT JAR for this platform in SWTLauncher/lib-platform, for IDEs.
# The released tools pick the JAR at runtime (see SWTLauncher.findSWTJar).
# An IDE runs the tools directly, so its classpath must hold exactly one
# SWT JAR. The IntelliJ modules reference this directory, with gitignored
# .jar files.
set -e

cd "$(dirname "$0")"

case "$(uname -s)" in
  Darwin)
    case "$(uname -m)" in
      x86_64) jar=swt-cocoa-macosx-x86_64.jar ;;
      *) jar=swt-cocoa-macosx-aarch64.jar ;;
    esac ;;
  MINGW*|MSYS*|CYGWIN*|UWIN*|Windows_NT) jar=swt-win32-win32-x86_64.jar ;;
  *) jar=swt-gtk-linux-x86_64.jar ;;
esac

if [ ! -f "lib/$jar" ]; then
  echo "Missing lib/$jar" >&2
  exit 1
fi

mkdir -p lib-platform
cp "lib/$jar" lib-platform/
echo "Copied lib/$jar to SWTLauncher/lib-platform/"
