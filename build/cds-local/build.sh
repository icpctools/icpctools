#!/usr/bin/env bash
# Build a CDS Docker image from the working tree. See build/cds-local/LOCAL.md.
#
#   build/cds-local/build.sh                         # CDS image for the Docker host's platform
#   PLATFORM=linux/amd64 build/cds-local/build.sh    # CDS image for amd64, like CI
#   build/cds-local/build.sh --published-builder     # pull ghcr.io/icpctools/builder (needs docker login ghcr.io)
#
# The ant build always runs natively (BUILD_PLATFORM, default the Docker host's platform): its output
# is platform-independent, and pnpm and pandoc break under QEMU emulation. Only the CDS image is built
# for PLATFORM. Produces cds-local:<version>-<arch> and cds-local:latest (the most recent build).
set -euo pipefail

REPO=$(cd "$(dirname "$0")/../.." && pwd)
HERE="$REPO/build/cds-local"
NATIVE_ARCH=$(docker version --format '{{.Server.Arch}}')
case "$NATIVE_ARCH" in aarch64) NATIVE_ARCH=arm64 ;; x86_64) NATIVE_ARCH=amd64 ;; esac
PLATFORM=${PLATFORM:-linux/$NATIVE_ARCH}
ARCH=${PLATFORM#linux/}
BUILD_PLATFORM=${BUILD_PLATFORM:-linux/$NATIVE_ARCH}
BUILD_ARCH=${BUILD_PLATFORM#linux/}
OWN_BUILDER=1

for arg in "$@"; do
  case "$arg" in
    --published-builder) OWN_BUILDER=0 ;;
    -h|--help) sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

# the published builder only exists for amd64
if [ "$OWN_BUILDER" = 0 ] && [ "$BUILD_ARCH" != amd64 ]; then
  echo "The published builder is amd64 only, building it locally instead" >&2
  OWN_BUILDER=1
fi

cd "$REPO"
VERSION="$(awk -F '=' '{ print $2 }' version.properties).$(git rev-list HEAD --count)"
TAG="$VERSION"
if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  TAG="$VERSION-dirty"
fi

echo "==> Building CDS $VERSION on $BUILD_PLATFORM, image for $PLATFORM"

if [ "$OWN_BUILDER" = 1 ]; then
  BASE="icpctools-builder:local-$BUILD_ARCH"
  echo "==> Builder base image $BASE (from build/builder.Dockerfile)"
  docker build --platform "$BUILD_PLATFORM" -t "$BASE" -f build/builder.Dockerfile build
else
  BASE=ghcr.io/icpctools/builder:latest
  echo "==> Builder base image $BASE"
  docker pull --platform "$BUILD_PLATFORM" "$BASE"
fi

BUILDER="cds-local-builder:$BUILD_ARCH"
docker build --platform "$BUILD_PLATFORM" --build-arg BASE="$BASE" -t "$BUILDER" -f "$HERE/builder.Dockerfile" "$HERE"

echo "==> ant build of ContestModel, presentations and CDS"
# the CDS bundles every dist/presentations-*.zip, so drop stale ones first
rm -f "dist/wlp.CDS-$VERSION.zip" dist/presentations-*.zip
# node_modules and the pnpm/corepack caches live in per-arch volumes, so they never mix with the
# host's (macOS) node_modules or with the other architecture's
docker run --rm --platform "$BUILD_PLATFORM" \
  -v "$REPO":/src -w /src \
  -v "cds-local-node_modules-$BUILD_ARCH":/src/CDS/ui/node_modules \
  -v "cds-local-cache-$BUILD_ARCH":/cache \
  "$BUILDER" \
  bash -c 'git config --global --add safe.directory /src && ant -f ContestModel/build.xml && ant -f PresCore/build.xml && ant -f PresContest/build.xml && ant -f CDS/build.xml dist'

if [ ! -f "dist/wlp.CDS-$VERSION.zip" ]; then
  echo "Build did not produce dist/wlp.CDS-$VERSION.zip" >&2
  ls dist >&2 || true
  exit 1
fi

echo "==> CDS image cds-local:$TAG-$ARCH"
docker build --platform "$PLATFORM" \
  -f "$HERE/cds.Dockerfile" \
  --build-context dist=dist \
  --build-arg CDS_VERSION="$VERSION" \
  -t "cds-local:$TAG-$ARCH" -t cds-local:latest \
  build/cds/Docker

echo "==> Done: cds-local:$TAG-$ARCH (also cds-local:latest). Run it with build/cds-local/run.sh"
