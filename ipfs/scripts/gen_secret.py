#!/usr/bin/env python3
import re, secrets, pathlib

secret_file = pathlib.Path("secrets/cluster.secret")
env_file = pathlib.Path("secrets/cluster.env")
hex_re = re.compile(r"^[0-9a-fA-F]{64}$")

# Prefer existing cluster.secret if valid
secret = secret_file.read_text().strip() if secret_file.exists() else ""
if not hex_re.match(secret or ""):
    secret = secrets.token_hex(32)
    secret_file.parent.mkdir(parents=True, exist_ok=True)
    secret_file.write_text(secret + "\n", encoding="utf-8")
    print("Generated new cluster.secret")

# Write the env-file for Compose
env_file.write_text(f"CLUSTER_SECRET={secret}\n", encoding="utf-8")
print(f"CLUSTER_SECRET={secret}")
print("Wrote secrets/cluster.secret and secrets/cluster.env")
