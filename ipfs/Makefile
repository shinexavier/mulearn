CLUSTER?=docker-compose.cluster.yml
DEV?=docker-compose.dev.yml
ENVFILE?=--env-file secrets/cluster.env

.PHONY: up-dev down-dev up-cluster down-cluster add cluster-auto cluster-stop peerid patch show-peer secret pin fetch status clean-secrets clean

up-dev:
	docker compose -f $(DEV) up -d

down-dev:
	docker compose -f $(DEV) down -v

up-cluster:
	docker compose $(ENVFILE) -f $(CLUSTER) up -d

down-cluster:
	docker compose $(ENVFILE) -f $(CLUSTER) down -v

add:
	@test -n "$(FILE)" || (echo "FILE= required"; exit 1)
	curl -s -F file=@$(FILE) http://localhost:25001/api/v0/add

cluster-auto: secret cluster-stop
	docker compose $(ENVFILE) -f $(CLUSTER) up -d cluster-daemon-a
	@echo "Waiting 8s for clusterA to initialize..."; sleep 8
	@PEERID=$$(docker logs cluster-daemon-a 2>&1 | sed -n 's|.*/p2p/\(.*\)|\1|p' | head -n 1); \
	if [ -z "$$PEERID" ]; then \
	  echo "Could not obtain cluster-daemon-a peer id. Check logs: docker logs cluster-daemon-a"; exit 1; \
	else \
	  echo $$PEERID > .peerid && echo "cluster-daemon-a peer id: $$PEERID"; \
	  python3 scripts/patch_bootstrap.py $(CLUSTER) $$PEERID && \
	  docker compose $(ENVFILE) -f $(CLUSTER) up -d; \
	fi

cluster-stop:
	-@docker compose $(ENVFILE) -f $(CLUSTER) down -v --remove-orphans >/dev/null 2>&1 || true

show-peer:
	@docker exec cluster-daemon-a ipfs-cluster-ctl --enc=json id 2>/dev/null | sed -n 's/.*\"id\":\"\([^\"]*\)\".*/\1/p'

peerid:
	@cat .peerid 2>/dev/null || echo "No .peerid yet. Run: make cluster-auto."

patch:
	@test -n "$(PEERID)" || (echo "PEERID= required"; exit 1)
	python3 scripts/patch_bootstrap.py $(CLUSTER) $(PEERID)

secret:
	@mkdir -p secrets
	@python3 scripts/gen_secret.py
	@if [ ! -s secrets/swarm.key ]; then python3 scripts/create_swarm_key.py && echo "Generated secrets/swarm.key"; else echo "swarm.key exists"; fi

pin:
	@test -n "$(CID)" || (echo "CID= required"; exit 1)
	@NAME=$${NAME:-unnamed}
	curl -s -X POST "http://localhost:9094/pins/$(CID)" \
	  -H "Content-Type: application/json" \
	  -d '{"name":"'$$NAME'","replication_min":2,"replication_max":3}' | jq .

fetch:
	@test -n "$(CID)" || (echo "CID= required"; exit 1)
	@OUTFILE?=downloaded.bin
	curl -s "http://localhost:28080/ipfs/${CID}" -o "$(OUTFILE)" && ls -lh "$(OUTFILE)"

status:
	@test -n "$(CID)" || (echo "CID= required"; exit 1)
	curl -s "http://localhost:9094/pins/${CID}" | jq .

clean-secrets:
	@echo "About to delete secrets/{cluster.secret,cluster.env,swarm.key}..."
	@test "$(CONFIRM)" = "YES" || (echo "Refusing. Run: make clean-secrets CONFIRM=YES"; exit 1)
	@rm -f secrets/cluster.secret secrets/cluster.env secrets/swarm.key
	@echo "Secrets removed. Recreate with: make secret"

clean:
	docker compose $(ENVFILE) -f $(CLUSTER) down -v --remove-orphans
