#!/bin/bash
# Workaround for Docker embedded DNS being broken in codespace DinD (no iptables NAT).
# Starts mongo/jenkins via compose, then manually starts the app container with the
# mongo IP hardcoded so hostname resolution isn't needed.
set -e

cd "$(dirname "$0")"

# Ensure dockerd is running (codespace DinD requires --iptables=false)
if ! docker info &>/dev/null; then
  echo "Starting dockerd..."
  sudo dockerd --iptables=false &>/tmp/dockerd.log &
  until docker info &>/dev/null; do sleep 1; done
  echo "dockerd ready"
fi

mkdir -p /workspaces/jenkins_config

# Start mongo + jenkins (not the app — it has broken DNS in compose)
docker compose --profile mongo --profile prod-eng-service up -d --scale prod-eng=0 2>/dev/null || \
  docker compose --profile mongo up -d

# Wait for mongo to be healthy
echo "Waiting for mongo..."
until docker exec service-mongo-1 mongosh --quiet --eval "db.adminCommand('ping')" &>/dev/null; do
  sleep 1
done

MONGO_IP=$(docker inspect service-mongo-1 --format '{{.NetworkSettings.Networks.service_default.IPAddress}}')
echo "Mongo IP: $MONGO_IP"

# Remove old app container if exists
docker rm -f service-prod-eng-1 2>/dev/null || true

docker run -d \
  --name service-prod-eng-1 \
  --network service_default \
  -p 8080:8080 \
  -p 5005:5005 \
  --add-host mongo:"$MONGO_IP" \
  -e ENVIRONMENT_NAME=local \
  -e MONGODB_CONECTION_URL="mongodb://root:example@${MONGO_IP}:27017/" \
  --restart always \
  prod-eng-img

echo "App started at http://localhost:8080"
