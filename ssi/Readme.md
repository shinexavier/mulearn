# ACA-Py + `did:indy` Resolver — Working Setup (BCovrin Test)

This README captures the key decisions, pitfalls, and the exact fix that made `did:indy` resolution work in our ACA-Py deployment.

## TL;DR — What finally worked
1. **Image:** Use the OWF image `ghcr.io/openwallet-foundation/acapy-agent:py3.12-1.2.0`.
2. **Plugin install:** Build a tiny image that installs `acapy-did-indy` **from GitHub (main)**.
3. **Config:** Pass the plugin config via `--plugin-config-value` (CLI), not a YAML file, to avoid schema/quoting issues.
4. **Namespace mapping (critical):**  
   ```text
   "acapy_did_indy.indy_namespace=bcovrin:test"
   "acapy_did_indy.ledgers.bcovrin:test=http://test.bcovrin.vonx.io/genesis"
   ```
5. **Rebuild/run:** `docker compose build --no-cache && docker compose up -d --force-recreate`

---

## Final Dockerfile
```dockerfile
# Dockerfile
FROM ghcr.io/openwallet-foundation/acapy-agent:py3.12-1.2.0
USER root
# Ensure we have the latest plugin (new schema); avoid older PyPI builds
RUN python -m pip uninstall -y acapy-did-indy || true  && python -m pip install --no-cache-dir git+https://github.com/Indicio-tech/acapy-did-indy@main
USER aries
```

## Final `docker-compose.yml` (service excerpt)
> Note: remove `version:` at the top to silence the Compose warning. Keep the `ssi-net` as an **external** Docker network.

```yaml
services:
  acapy:
    build: .
    image: acapy-with-didindy:1.2.0
    container_name: acapy
    restart: unless-stopped
    networks: [ si-net ]
    ports:
      - "8020:8020"   # inbound http
      - "8031:8031"   # admin api
    environment:
      ACAPY_LOG_LEVEL: info
    command:
      - start
      - --label
      - mulearn-issuer
      - --admin
      - 0.0.0.0
      - "8031"
      - --admin-api-key
      - secret
      - -it
      - http
      - 0.0.0.0
      - "8020"
      - -ot
      - http
      - --endpoint
      - http://acapy:8020
      - --wallet-type
      - askar
      - --wallet-name
      - mulearn
      - --wallet-key
      - mulearnkey
      - --auto-provision
      - --public-invites
      - --plugin
      - acapy_did_indy
      # >>> CRITICAL: pass config via CLI (correct quoting) <<<
      - --plugin-config-value
      - "acapy_did_indy.indy_namespace=bcovrin:test"
      - --plugin-config-value
      - "acapy_did_indy.ledgers.bcovrin:test=http://test.bcovrin.vonx.io/genesis"
      # Standard ledger connect (keep for dev)
      - --genesis-url
      - http://test.bcovrin.vonx.io/genesis
      - --emit-new-didcomm-prefix
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS -H 'X-API-Key: secret' http://localhost:8031/status >/dev/null || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 30
```

---

## Bring-up
```bash
# Ensure external network exists
docker network create ssi-net || true

# Build & run (no cache so the plugin installs cleanly)
docker compose build --no-cache
docker compose up -d --force-recreate

# Health check (remember the API key)
curl -H "X-API-Key: secret" http://localhost:8031/status
```

## Verify the plugin & config
```bash
# Plugin list should include acapy_did_indy
curl -H "X-API-Key: secret" http://localhost:8031/plugins

# Config should show EXACTLY this (no extra quotes in the key)
curl -H "X-API-Key: secret" http://localhost:8031/status/config | jq '.plugin_config.acapy_did_indy'
# {
#   "indy_namespace": "bcovrin:test",
#   "ledgers": { "bcovrin:test": "http://test.bcovrin.vonx.io/genesis" }
# }
```

## Resolve a DID from BCovrin Test
Use a DID that actually exists on **bcovrin:test**. The W3C DID form is:
```
did:indy:bcovrin:test:<INDY_DID_IDENTIFIER>
```
Example command:
```bash
curl -H "X-API-Key: secret"   "http://localhost:8031/resolver/resolve/did:indy:bcovrin:test:2juZ9a3Nc9F7WVK26hzAFr"
```

---

## Troubleshooting Cheatsheet

**Error:** `Module doesn't exist: acapy_did_indy`  
**Cause:** Plugin not installed in the image.  
**Fix:** Build with the Dockerfile above (installs plugin from GitHub).

**Error:** `ResolverError: missing auto flag or ledger map` during startup  
**Cause A:** Plugin loaded but no config provided.  
**Cause B (common):** Old plugin reading the **old** schema.  
**Fix:** Use the GitHub build + pass config via `--plugin-config-value` (new schema).

**Error:** `Unknown DID namespace: bcovrin:test` when resolving  
**Cause:** The ledger map key was quoted **inside** the key (e.g., `""bcovrin:test""`).  
**Fix:** Use **exact** CLI args shown above. In `/status/config`, the key must appear as `"bcovrin:test"` (no inner quotes).

**401 Unauthorized on /status**  
**Cause:** Missing `X-API-Key` header.  
**Fix:** Include `-H "X-API-Key: secret"` in admin API calls. Healthcheck in compose already does this.

**Deprecation block in logs**  
Just informational; keep using DID Exchange v1.0, Issue Credential 2.0, Present Proof 2.0 in flows. `--emit-new-didcomm-prefix` is set.

**General tips**
- Don’t mix `build:` and `image:` unless you intend to override. For the plugin, you **must build**.
- If you prefer a YAML file instead of CLI values, ensure the schema matches the plugin version:
  ```yaml
  # New schema (when using GitHub build)
  acapy-did-indy:
    indy_namespace: "bcovrin:test"
    ledgers:
      "bcovrin:test": "http://test.bcovrin.vonx.io/genesis"
  ```
  Be sure the file is mounted and referenced by `--plugin-config /path/to/file`.

---

## Optional: Webhook + Java Demo
There’s a small bundle with a Flask **webhook** and a **Java** client (OkHttp/Jackson) that:
- checks `/status`
- creates an **Out-of-Band** invitation
- resolves a `did:indy`

Use it to validate E2E quickly.

---

## Known-good Genesis (BCovrin Test)
```
http://test.bcovrin.vonx.io/genesis
```

---

## License
Internal project README; adapt as needed.