.PHONY: build ensure-source up up-all down clean rebuild logs wait test help

## Default target
.DEFAULT_GOAL := help

## Build the shared source image and all compose service images.
## Run this on first setup and whenever the source branch changes.
##
## The GITHUB_BRANCH shell variable selects which mlibrary fork branch to clone
## inside the source image (default: umich).  Override at the command line:
##   GITHUB_BRANCH=my-branch make build
##
## In GitHub Actions CI the same value is carried by SOURCE_BRANCH (because the
## Actions runner reserves all GITHUB_* env var names) and forwarded to Docker
## as --build-arg GITHUB_BRANCH=${SOURCE_BRANCH}.  The Dockerfile is unchanged.
build:
	docker build -t dspace-containerization-source \
	  --build-arg GITHUB_BRANCH=$${GITHUB_BRANCH:-umich} .
	docker compose build

## Build the source image only when it is not already present locally.
## Called automatically by 'up' so you can never accidentally start with a missing source image.
ensure-source:
	@docker image inspect dspace-containerization-source:latest > /dev/null 2>&1 \
	  && echo "Source image already exists; skipping build." \
	  || (echo "Source image not found; building now..." && \
	      docker build -t dspace-containerization-source \
	        --build-arg GITHUB_BRANCH=$${GITHUB_BRANCH:-umich} .)

## Start the core services (db, solr, backend, frontend) in the background.
## Builds the source image first if it is not already present.
up: ensure-source
	docker compose up -d

## Start core + all optional services (apache, express).
up-all: ensure-source
	docker compose --profile optional up -d

## Stop and remove containers (volumes are preserved).
down:
	docker compose down

## Stop and remove containers AND all named volumes (full clean: destroys data).
clean:
	docker compose down -v --rmi local
	docker rmi -f dspace-containerization-source 2>/dev/null || true

## Rebuild all images from scratch and restart core services.
rebuild: clean build up

## Show logs for all running services (Ctrl-C to exit).
logs:
	docker compose logs -f

## Wait for all core services to be healthy (backend, solr, frontend).
## Polls every 5 s; times out after MAX_WAIT seconds (default 300).
wait:
	@bash tests/wait-for-stack.sh

## Run the smoke-test suite against the running local stack.
## Ensures the source image exists, starts the stack, waits for readiness, then runs tests.
test: up wait
	@bash tests/smoke.sh

## Show this help message.
help:
	@echo ""
	@echo "dspace-containerization: local dev Makefile"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "  build          Build source image + all compose service images"
	@echo "  ensure-source  Build source image only if not already present"
	@echo "  up             Start core services (db, solr, backend, frontend)"
	@echo "  up-all         Start core + optional services (apache, express)"
	@echo "  down           Stop containers (volumes preserved)"
	@echo "  clean          Stop containers and delete volumes + images"
	@echo "  rebuild        Full clean, build, and up"
	@echo "  logs           Tail logs for all services"
	@echo "  wait           Wait for all services to be healthy"
	@echo "  test           Ensure source, start stack, wait, run smoke tests"
	@echo "  help           Show this message"
	@echo ""

