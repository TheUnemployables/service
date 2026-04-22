#!/bin/bash
set -e

cd "$(dirname "$0")"

# Ensure dockerd is running (dev container DinD requires --iptables=false)
if ! docker info &>/dev/null; then
  echo "Starting dockerd..."
  sudo dockerd --iptables=false &>/tmp/dockerd.log &
  until docker info &>/dev/null; do sleep 1; done
  echo "dockerd ready"
fi

mkdir -p "${JENKINS_CONFIG_ROOT:-/workspaces/jenkins_config}"

# Start mongo
docker compose --profile mongo up -d

echo "Waiting for mongo..."
until docker exec service-mongo-1 mongosh --quiet --eval "db.adminCommand('ping')" &>/dev/null; do
  sleep 1
done

# Get mongo container IP — embedded DNS is broken in DinD, extra_hosts used instead
MONGO_IP=$(docker inspect service-mongo-1 --format '{{.NetworkSettings.Networks.service_default.IPAddress}}')
echo "Mongo IP: $MONGO_IP"

# Start app (MONGO_IP passed so extra_hosts in compose resolves 'mongo' correctly)
export MONGO_IP
export DOCKER_IMAGE="${DOCKER_IMAGE:-octavalexandru/service}"
export IMAGE_TAG="${IMAGE_TAG:-latest}"
docker compose --profile prod-eng-service up -d --no-deps --force-recreate prod-eng
echo "App started at http://localhost:8080"

# Start Jenkins (network_mode: host set in compose so it uses dev container's NIC)
docker compose --profile prod-eng-service up -d --no-deps jenkins
echo "Jenkins started at http://localhost:8082"
