#!/usr/bin/env bash
# Run a CDS image like the README does, by default the most recent local build. See build/cds-local/LOCAL.md.
#
#   build/cds-local/run.sh
#   IMAGE=ghcr.io/icpctools/cds:2.7.1444 build/cds-local/run.sh
#   CONTEST_DIR=~/contests/nwerc18 CCS_URL=... CCS_USER=... CCS_PASSWORD=... build/cds-local/run.sh
#
# CDS_HOST sets the address that is printed (default: the Colima VM address if there is one, else localhost).
# Extra arguments are passed to docker run, e.g. -e ADMIN_PASSWORD=secret
set -euo pipefail

IMAGE=${IMAGE:-cds-local:latest}
args=(--name "${NAME:-cds}" --rm -it -p 8080:8080 -p 8443:8443
  -e CCS_URL="${CCS_URL:-https://www.domjudge.org/demoweb/api/contests/nwerc18}"
  -e CCS_USER="${CCS_USER:-admin}"
  -e CCS_PASSWORD="${CCS_PASSWORD:-admin}")
if [ -n "${CONTEST_DIR:-}" ]; then
  args+=(-v "$(cd "$CONTEST_DIR" && pwd)":/contest)
fi
if [ -n "${PLATFORM:-}" ]; then
  args+=(--platform "$PLATFORM")
fi

# Colima with a network address serves ports on the VM's address rather than on localhost
HOST=${CDS_HOST:-}
if [ -z "$HOST" ] && command -v colima >/dev/null 2>&1; then
  HOST=$(colima ls -j 2>/dev/null | sed -n 's/.*"status":"Running".*"address":"\([^"]*\)".*/\1/p' | head -1)
fi
HOST=${HOST:-localhost}

echo "CDS from $IMAGE at https://$HOST:8443/"
exec docker run "${args[@]}" "$@" "$IMAGE"
