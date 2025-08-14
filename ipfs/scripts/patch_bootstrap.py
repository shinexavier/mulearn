#!/usr/bin/env python3
import sys, re, pathlib
if len(sys.argv) != 3:
    print("Usage: patch_bootstrap.py <compose-file> <peer_id>")
    sys.exit(1)
compose = pathlib.Path(sys.argv[1])
peer = sys.argv[2].strip()
txt = compose.read_text(encoding="utf-8")
new = re.sub(r"REPLACE_ME", peer, txt)
compose.write_text(new, encoding="utf-8")
print(f"Patched {compose} with peer id {peer}")
