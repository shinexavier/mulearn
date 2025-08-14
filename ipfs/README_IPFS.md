# μLearn Off-Ledger Storage — IPFS Spec (README, Paranoid Mode)

> This variant avoids using `.env` entirely. Docker Compose reads `CLUSTER_SECRET` from `secrets/cluster.env`, which is generated from `secrets/cluster.secret`. Both live under `secrets/` (git-ignored).

---

## Quick Start (mini-cluster, no `.env`)

### Prerequisites
- **Docker** (Desktop/Engine) and **Docker Compose v2** (`docker compose`)
- **make**
- **curl** and **jq** (optional)

### 1) Generate secrets (file-based only)
```bash
make secret
```
This creates:
- `secrets/cluster.secret` — 64-hex cluster secret (primary source of truth)
- `secrets/cluster.env` — one-line env file derived from `cluster.secret` for Compose
- `secrets/swarm.key` — private IPFS swarm key

### 2) Bring up the cluster (auto-bootstrap)
```bash
make cluster-auto
```
Under the hood:
1. Starts `cluster-daemon-a` + `ipfs-node-a` with `--env-file secrets/cluster.env`.
2. Reads `cluster-daemon-a`’s PeerID and patches `docker-compose.cluster.yml` (replaces both `REPLACE_ME`).
3. Starts `cluster-daemon-b`, `cluster-daemon-hub` and their IPFS nodes.

Check health:
```bash
curl -s http://localhost:9094/health | jq
curl -s http://localhost:25001/api/v0/version | jq
```

### 3) Add → Pin → Fetch
```bash
make add FILE=./submission.enc               # prints CID
CID=<paste CID>
make pin CID=$CID NAME=submission-42         # replicate (min=2, max=3)
make status CID=$CID                         # check replication
make fetch CID=$CID OUTFILE=downloaded.enc   # fetch via Hub gateway
```

**Default ports**  
- Kubo APIs: `5001`, `15001`, `25001`  
- Gateways:  `8080`, `18080`, `28080`  
- Cluster REST (A exposed in demo): `9094`

**Troubleshooting**
- Missing `secrets/swarm.key` → run `make secret` again.
- “Port in use” → `make down-cluster` then retry (or edit the compose ports).
- Show peer ID: `make show-peer`; Logs: `docker logs -f cluster-daemon-a`.

---

## Repo layout
```
.
├─ docker-compose.cluster.yml   # 3-node IPFS cluster with coordinating daemons
├─ docker-compose.dev.yml       # single-node Kubo (optional)
├─ Makefile                     # helper targets (secret, cluster-auto, add/pin/fetch/status, clean-secrets)
├─ scripts/
│  ├─ create_swarm_key.py       # generates a valid swarm.key
│  ├─ gen_secret.py             # writes secrets/cluster.secret and secrets/cluster.env
│  └─ patch_bootstrap.py        # patches REPLACE_ME with cluster-daemon-a PeerID
└─ secrets/
   ├─ .gitkeep               # placeholder so the folder exists in Git
   ├─ cluster.secret            # cluster secret (source of truth)
   ├─ cluster.env               # generated env-file for Compose
   └─ swarm.key                 # private IPFS swarm key
```

### Git hygiene
A `.gitignore` is included to keep secrets out of version control (and still keep the folder):
```
# Secrets (never commit)
secrets/cluster.secret
secrets/cluster.env
secrets/swarm.key
!secrets/.gitkeep

# Optional/Runtime
.env
.peerid
```

---

## 0) Scope & goals
Use **IPFS** as the off-ledger, verifiable blob store for large artefacts. **Hyperledger Fabric** anchors integrity (CIDs + hashes, lifecycle). **DB** holds indexes/ACLs/key grants.

**Key goals**
- Verifiable integrity via **content addressing (CID)** anchored on Fabric.
- **Availability** & **decentralization** via **IPFS Cluster** replication.
- **Privacy** via **encryption** (crypto is the ACL).
- Clean integration with the **Protocol Server** and **Provider Nodes**.

---

## 1) What IPFS is used for (and not)

| Use it for | Don’t use it for |
|---|---|
| Large, mostly immutable artefacts: code zips, notebooks, videos, static specs | PII or data you must delete on request (use DB/object storage) |
| Public or **encrypted** private content shared across the consortium | Frequently changing state (revocation lists, mutable records) |
| Anything you want to tamper-evidence with a **CID** on Fabric | Complex queries/joins (use Postgres) |

**Pattern:** store **ciphertext** on IPFS → get CID → anchor `{cid, sha256, size, encAlg, keyRef}` on Fabric → keep indexes/ACLs/key grants in DB.

---

## 2) Architecture

The architecture consists of two main components for each of the three nodes (Provider A, Provider B, and the μLearn Hub):

*   **IPFS Node (`ipfs-node-*`)**: This is a standard Kubo IPFS daemon that stores the data.
*   **IPFS Cluster Daemon (`cluster-daemon-*`)**: This daemon coordinates the IPFS nodes, manages the cluster, and provides a single API for interacting with the cluster.

This setup ensures that data is replicated across all three nodes for high availability and decentralization.

---

## 3) Developer setup (optional variants)

### A) Single-node dev (Kubo only)
```yaml
# docker-compose.dev.yml
version: "3.9"
services:
  ipfs:
    image: ipfs/kubo:latest
    container_name: ipfs-dev
    environment: { IPFS_PROFILE: server }
    ports: [ "5001:5001", "8080:8080", "4001:4001/tcp", "4001:4001/udp" ]
    command: ["daemon","--migrate=true","--enable-gc=false"]
```
Run: `docker compose -f docker-compose.dev.yml up -d`

### B) Manual cluster bootstrap (advanced)
1) `make secret` (ensures `secrets/cluster.env` and `secrets/swarm.key`)
2) `docker compose --env-file secrets/cluster.env -f docker-compose.cluster.yml up -d cluster-daemon-a`
3) Get PeerID: `docker logs cluster-daemon-a 2>&1 | sed -n 's|.*/p2p/\(.*\)|1|p' | head -n 1`
4) Patch both `REPLACE_ME` in compose, then start all services:
   `docker compose --env-file secrets/cluster.env -f docker-compose.cluster.yml up -d`

---

## 4) Protocol-server usage (API examples)

### 4.1 Add (encrypt → add → pin)
```bash
openssl rand -hex 32 > key.hex
openssl enc -aes-256-gcm -salt -in submission.zip -out submission.enc -pass file:./key.hex
CID=$(curl -s -F file=@submission.enc http://localhost:25001/api/v0/add | jq -r .Hash)
curl -s -X POST "http://localhost:9094/pins/${CID}"   -H "Content-Type: application/json"   -d '{"name":"submission-42","replication_min":2,"replication_max":3}'
```

### 4.2 Fetch & verify
```bash
curl -s "http://localhost:28080/ipfs/$CID" -o submission.enc
# verify against on-chain sha256, then decrypt with the wrapped key
```

---

## 5) Production notes
- Keep `secrets/` out of Git; rotate secrets on any exposure.
- Use a pinned, private gateway; monitor replication health.
- Crypto-shred for deletions (unpin + revoke keys).

---

## 6) Testing the Cluster

This section provides a step-by-step guide to testing the IPFS cluster by adding, pinning, fetching, and unpinning a test file.

### 1. Create a Test File

A test file named `test-artifact.txt` has been created for you. You can also use any other file you choose.

### 2. Add the File to IPFS and Get the CID

The `make add` command uploads a file to the IPFS cluster and returns its Content Identifier (CID).

```bash
# Add the test file to the cluster
make add FILE=test-artifact.txt
```

The output will include the CID. Copy this CID for the next steps.

### 3. Pin the File to the Cluster

Pinning ensures the file is replicated across the cluster nodes. The `make pin` command pins a file using its CID.

```bash
# Replace <YOUR_CID> with the CID from the previous step
make pin CID=<YOUR_CID> NAME=test-artifact
```

### 4. Check the Pin Status

You can check the status of a pinned file to ensure it has been replicated.

```bash
# Replace <YOUR_CID> with the CID of your file
make status CID=<YOUR_CID>
```

### 5. Fetch the File from the Gateway

You can fetch the file from the IPFS gateway to verify its contents. You can also view the file directly in your browser by navigating to `http://localhost:8080/ipfs/<YOUR_CID>`.

```bash
# Replace <YOUR_CID> with the CID of your file
make fetch CID=<YOUR_CID> OUTFILE=downloaded-artifact.txt
```

This will download the file and save it as `downloaded-artifact.txt`.

### 6. Unpin the File

To remove a file from the cluster, you can unpin it. This will remove the file from the cluster's storage.

```bash
# Replace <YOUR_CID> with the CID of your file
curl -X DELETE "http://localhost:9094/pins/<YOUR_CID>"
```

### 7. View Pinned Content

You can view a list of all pinned content on the cluster by accessing the `/pins` endpoint of the IPFS Cluster API:

```bash
curl -s http://localhost:9094/pins | jq .
```

## 7) Operations & Maintenance (SOPs)

This section provides Standard Operating Procedures (SOPs) for managing the IPFS cluster.

### Initial Setup

To bring up the cluster for the first time, run:

```bash
# 1. Generate secrets
make secret

# 2. Start the cluster and auto-bootstrap
make cluster-auto
```

### Teardown

To stop and remove all containers, networks, and volumes, run:

```bash
make clean
```

### Secret Rotation

If the `CLUSTER_SECRET` or `swarm.key` is compromised, you must rotate them.

The recommended approach is to use the following one-liner:
```bash
make clean-secrets CONFIRM=YES && make cluster-auto
```

Alternatively, you can perform the steps manually:

1.  **Stop the cluster:**
    ```bash
    make down-cluster
    ```

2.  **Clean old secrets:**
    ```bash
    make clean-secrets CONFIRM=YES
    ```

3.  **Generate new secrets:**
    ```bash
    make secret
    ```

4.  **Restart the cluster:**
    ```bash
    make cluster-auto
    ```

### Full Reset

To perform a full reset of the cluster, including removing all data and secrets, run:
```bash
make clean && make clean-secrets CONFIRM=YES && make cluster-auto
```

### Checking Cluster Status

-   **Check service health:**
    ```bash
    curl -s http://localhost:9094/health | jq
    ```
-   **Check IPFS version:**
    ```bash
    curl -s http://localhost:25001/api/v0/version | jq
    ```
-   **Show the cluster peer ID:**
    ```bash
    make show-peer
    ```
-   **View container logs:**
    ```bash
    docker logs -f cluster-daemon-a
    ```

## 8) Make targets (reference)
```makefile
make secret
make cluster-auto
make add FILE=...
make pin CID=... NAME=...
make status CID=...
make fetch CID=... OUTFILE...
make down-cluster
make clean-secrets CONFIRM=YES
