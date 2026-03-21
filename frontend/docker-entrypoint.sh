#!/usr/bin/env sh
set -eu
cp /out/result.json ./public/
npm run dev -- --mode docker
