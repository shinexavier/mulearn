Absolutely—here’s a clean, copy-pasteable setup that’s production-leaning and easy to debug.

I’m giving you:

1. a **Dockerfile** for ACA-Py with the `did:indy` resolver plugin
2. a **docker-compose.yml** for ACA-Py
3. a **one-liner to bring up von-network** on a shared Docker network (using the official repo)
4. **sample usage** (cURL + tiny Java client)
5. an **optional multi-ledger config** pattern you can enable later

---

# 1) Project layout

```
mulearn-ssi/
├─ acapy/
│  └─ Dockerfile
├─ docker-compose.yml
└─ examples/
   └─ ResolveDidIndy.java
```

---

# 2) ACA-Py Dockerfile (with did\:indy plugin)

`acapy/Dockerfile`

```dockerfile
# ACA-Py official image (OpenWallet Foundation / GHCR)
FROM ghcr.io/openwallet-foundation/acapy-agent:py3.12-1.2.0

# Install the did:indy resolver/registrar plugin
# (Indicio’s plugin; widely used with ACA-Py)
RUN pip install --no-cache-dir git+https://github.com/Indicio-tech/acapy-did-indy@main
```

---

# 3) docker-compose (ACA-Py service)

`docker-compose.yml`

```yaml
version: "3.9"

networks:
  ssi-net:
    name: ssi-net    # shared network we’ll also attach von-network to
    external: true

services:
  acapy:
    build:
      context: ./acapy
    container_name: acapy
    networks: [ ssi-net ]
    ports:
      - "8020:8020"   # agent endpoint (HTTP)
      - "8031:8031"   # admin API (secured via X-API-Key)
    environment:
      ACAPY_LOG_LEVEL: info
    command: >
      aca-py start
      --label mulearn-issuer
      --admin 0.0.0.0 8031 --admin-api-key secret
      -it http 0.0.0.0 8020 -ot http
      --endpoint http://acapy:8020
      --wallet-type askar --wallet-name mulearn --wallet-key mulearnkey --auto-provision
      --plugin acapy_did_indy
      --genesis-url http://von-web:9000/genesis   # served by von-network
      --emit-new-didcomm-prefix
      --public-invites
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://localhost:8031/status"]
      interval: 10s
      timeout: 5s
      retries: 30
```

> We point `--genesis-url` at `http://von-web:9000/genesis`—that hostname will be available once we run **von-network** on the **same Docker network** (`ssi-net`).

---

# 4) Bring up **von-network** on the same network

The von-network repo ships its own scripts. We’ll:

* create the shared network,
* clone and run von-network so it joins `ssi-net`,
* then start ACA-Py.

```bash
# 1) Create the shared network (once)
docker network create ssi-net || true

# 2) Clone von-network
git clone https://github.com/bcgov/von-network.git
cd von-network

# 3) Start von-network on the same docker network
#    (the scripts honor DOCKER_NET for the compose network name)
export DOCKER_NET=ssi-net
./manage build
./manage start

# von-network exposes genesis at http://localhost:9000/genesis on your host
# and inside the docker network it will be reachable as http://von-web:9000/genesis

# 4) In a separate shell, start ACA-Py
cd ../mulearn-ssi
docker compose up --build
```

> If you prefer not to use the scripts, you can run von-network’s own docker-compose directly—just ensure its services join `ssi-net` and that the **web** service is named `von-web` (or update `--genesis-url` accordingly).

---

# 5) Smoke test ACA-Py + did\:indy resolution

**Health**

```bash
curl -s -H "X-API-Key: secret" http://localhost:8031/status | jq
```

**Confirm plugin is visible**

```bash
curl -s -H "X-API-Key: secret" http://localhost:8031/plugins | jq
```

**Resolve a DID (replace with one that exists on your von ledger)**

```bash
DID='did:indy:local:von:WgWxqztrNooG92RXvxSTWv'
curl -s -H "X-API-Key: secret" \
  "http://localhost:8031/resolver/resolve/${DID}" | jq
```

You should see a DID Resolution Result with a `didDocument` object.

---

# 6) Tiny Java client (no low-level Indy code)

`examples/ResolveDidIndy.java`

```java
import network.idu.acapy.AriesClient;
import java.util.Optional;
import java.util.Map;

public class ResolveDidIndy {
  public static void main(String[] args) throws Exception {
    String adminUrl = "http://localhost:8031";
    String apiKey   = "secret";
    String did      = "did:indy:local:von:WgWxqztrNooG92RXvxSTWv"; // TODO: replace

    AriesClient ac = AriesClient.builder()
        .url(adminUrl)
        .apiKey(apiKey)
        .build();

    Optional<Map<String, Object>> res = ac.resolverResolve(did);
    System.out.println(res.orElseThrow(() -> new RuntimeException("Resolve failed")));
  }
}
```

**Maven (dependency)**

```xml
<dependency>
  <groupId>network.idu.acapy</groupId>
  <artifactId>aries-client-python</artifactId>
  <version>0.10.0</version>
</dependency>
```

Compile & run (Java 11+):

```bash
mvn -q -DskipTests package
java -cp target/classes:~/.m2/repository/network/idu/acapy/aries-client-python/0.10.0/aries-client-python-0.10.0.jar \
     ResolveDidIndy
```

---

# 7) Optional: Multi-ledger config (enable later)

If you need ACA-Py to read from multiple Indy networks (e.g., `local:von` and a partner’s staging net), switch from `--genesis-url` to a **list**. A common pattern is providing a YAML/JSON and passing its path to `--genesis-transactions-list`:

`ledger-config.yml` (example)

```yaml
ledgers:
  - id: local:von
    genesis_url: http://von-web:9000/genesis
    is_write: true
  - id: partner:stage
    genesis_url: https://partner.example.com/genesis
    is_write: false
```

Update the ACA-Py command:

```yaml
# replace the --genesis-url line with
--genesis-transactions-list /acapy/ledger-config.yml
```

…and **mount** that file into the container:

```yaml
    volumes:
      - ./ledger-config.yml:/acapy/ledger-config.yml:ro
```

> Exact flag names vary slightly across ACA-Py versions; if your image errors on startup, run with `--help` to confirm the multi-ledger flag your tag expects. The pattern (a list of ledgers with IDs + genesis sources) is stable.

---

## Gotchas & tips

* If ACA-Py can’t fetch genesis, confirm `von-web` is on `ssi-net` and healthy: `curl http://localhost:9000/genesis`.
* On Linux, `host.docker.internal` may not resolve; the shared Docker network method above avoids that.
* Keep **Admin API** protected (`--admin-api-key`); use `--admin-insecure-mode` only for local dev.
* Pin ACA-Py to a **specific tag** in your Dockerfile for reproducible builds (e.g., `py3.12-1.2.0` as above).

---

Also these files can be bundled as a zip or drop a GitHub-ready repo layout with a Makefile (`make up`, `make down`, `make resolve DID=...`).
