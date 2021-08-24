#!/bin/bash
echo 1 - $1
pandoc "$1"/README.md -f gfm -V linkcolor:blue -V geometry:margin=1in -o "$1"/README.pdf
echo 2
