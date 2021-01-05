#!/bin/bash
docker run --rm -v "`pwd`:/data" pandoc/latex "$1"/README.md -f gfm -V linkcolor:blue -V geometry:margin=1in -o "$1"/README.pdf