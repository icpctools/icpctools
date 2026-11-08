# The ICPC Tools builder image, plus what ci.yml installs on each run to build the CDS web UI:
# libatomic (needed by pnpm's native executable), Node 24 and pnpm (through corepack, pinned by
# packageManager in CDS/ui/package.json).
ARG BASE=ghcr.io/icpctools/builder:latest
FROM ${BASE}

RUN apt-get update \
   && apt-get install -y --no-install-recommends libatomic1 curl ca-certificates \
   && curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
   && apt-get install -y --no-install-recommends nodejs \
   && corepack enable \
   && rm -rf /var/lib/apt/lists/*

# don't ask before corepack downloads the pinned pnpm, and keep caches on volumes (see build.sh):
# corepack in /cache, and the pnpm store in the node_modules volume, so pnpm can hard link from it
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0 \
    COREPACK_HOME=/cache/corepack \
    pnpm_config_store_dir=/src/CDS/ui/node_modules/.pnpm-store \
    npm_config_store_dir=/src/CDS/ui/node_modules/.pnpm-store
