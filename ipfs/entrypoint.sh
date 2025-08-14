#!/usr/bin/env bash
set -e
if [ ! -f /data/ipfs/config ]; then ipfs init; fi
ipfs config --json API.HTTPHeaders.Access-Control-Allow-Origin '["*"]'
ipfs config --json API.HTTPHeaders.Access-Control-Allow-Methods '["PUT", "POST", "GET"]'
exec ipfs daemon --migrate=true --enable-gc=false
