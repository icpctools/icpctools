# Local CDS Docker build

Build and run a CDS Docker image from your working tree, without going through GitHub Actions or a
release. The image is like `ghcr.io/icpctools/cds:<version>`, but contains your local changes.

```bash
build/cds-local/build.sh                         # CDS image for the Docker host's platform (arm64 on Apple silicon)
PLATFORM=linux/amd64 build/cds-local/build.sh    # CDS image for amd64, like CI (runs emulated on Apple silicon)
build/cds-local/run.sh                           # runs cds-local:latest at https://localhost:8443/
```

## Files

* [build/cds-local/build.sh](build.sh): builds the builder image, runs the ant build of
  ContestModel and CDS in it, and then builds the CDS image `cds-local:<version>-<arch>` (also tagged
  `cds-local:latest`). The version gets a `-dirty` suffix when tracked files have uncommitted changes.
* [build/cds-local/builder.Dockerfile](builder.Dockerfile): `ghcr.io/icpctools/builder`
  plus Node 24, pnpm and libatomic, the extras that [ci.yml](../../.github/workflows/ci.yml) installs per run.
* [build/cds-local/cds.Dockerfile](cds.Dockerfile): a copy of
  [build/cds/Docker/Dockerfile](../cds/Docker/Dockerfile) that takes `wlp.CDS-<version>.zip` from
  `dist/` instead of downloading it from a GitHub release. It is built with `build/cds/Docker` as its
  context, so `start.sh`, `accounts.yaml` and `cdsConfig.xml` are shared with the published image.
* [build/cds-local/run.sh](run.sh): the README's `docker run` command, with defaults that
  can be overridden through `IMAGE`, `CCS_URL`, `CCS_USER`, `CCS_PASSWORD`, `CONTEST_DIR` (mounted at
  `/contest`), `PLATFORM` and `NAME`. Extra arguments go to `docker run`.

## Notes

* The ant build has to run inside the builder image: `CDS/build.xml` only makes `wlp.CDS-*.zip` when the
  packaged Open Liberty at `/cds.zip` exists, and only the builder image has that.
* The ant build always runs natively, on the Docker host's platform (`BUILD_PLATFORM` to override).
  `wlp.CDS-*.zip` is platform-independent, so `PLATFORM` only decides the platform of the CDS image.
  Running the build itself under QEMU emulation doesn't work: pnpm's native binary panics (an epoll
  "Bad file descriptor" error), and pandoc gets killed.
* On Apple silicon with Colima, `PLATFORM=linux/amd64` can fail while the CDS image unpacks the JDK
  (`tar: ... Cannot open: Function not implemented`). Colima's VM may come with an old QEMU (2022) that
  doesn't implement the `openat2` syscall, which tar 1.35 in Ubuntu 26.04 uses. To fix it, register a
  current QEMU (repeat after each VM restart):
  `docker run --privileged --rm tonistiigi/binfmt --uninstall qemu-x86_64 && docker run --privileged --rm tonistiigi/binfmt --install amd64`.
  Alternatively, use Rosetta: `colima start --vm-type vz --vz-rosetta`. Apple is phasing Rosetta out
  from macOS 28, though, and it's unclear whether that includes Rosetta for Linux VMs.
* Only ContestModel and CDS are built. Presentations are left out; develop those in dev mode from the IDE.
* `CDS/ui/node_modules`, which also holds the pnpm store, and the corepack cache are kept in
  per-architecture Docker volumes (`cds-local-node_modules-<arch>`, `cds-local-cache-<arch>`), so they
  don't mix with the host's `node_modules`. Remove them with `docker volume rm` to start clean.
* `build.sh` builds the builder itself from [build/builder.Dockerfile](../builder.Dockerfile), as
  `icpctools-builder:local-<arch>`. This is needed because `ghcr.io/icpctools/builder` can't be pulled
  anonymously, and only exists for amd64. It also means changes to the builder (for example the Open
  Liberty version) are picked up. The first build is slow because of texlive; after that it is cached. If
  you are logged in to ghcr.io (`docker login ghcr.io`), `--published-builder` uses the published image
  instead (amd64 only, so it needs `BUILD_PLATFORM=linux/amd64`, which brings back the emulation problems
  above on Apple silicon).
