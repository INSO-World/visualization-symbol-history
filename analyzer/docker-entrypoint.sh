#!/usr/bin/env sh
set -eu
REPO="$(realpath /repo/*/ | head -n 1)"
export REPO
echo "Starting analysis..."
java -Dfile.encoding=UTF-8 -jar analyzer.jar "$REPO"
cp ./result.json /out/
