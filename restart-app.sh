#!/bin/bash
# Workaround for Docker embedded DNS and NAT being broken in codespace DinD (no iptables).
# - App container uses mongo IP directly (bypasses DNS)
# - Jenkins uses --network host (bypasses NAT, gets real internet access)
set -e

cd "$(dirname "$0")"

# Ensure dockerd is running (codespace DinD requires --iptables=false)
if ! docker info &>/dev/null; then
  echo "Starting dockerd..."
  sudo dockerd --iptables=false &>/tmp/dockerd.log &
  until docker info &>/dev/null; do sleep 1; done
  echo "dockerd ready"
fi

mkdir -p "${JENKINS_CONFIG_ROOT:-/workspaces/jenkins_config}"

# Start mongo via compose (jenkins and prod-eng started manually below)
docker compose --profile mongo up -d

# Wait for mongo to be healthy
echo "Waiting for mongo..."
until docker exec service-mongo-1 mongosh --quiet --eval "db.adminCommand('ping')" &>/dev/null; do
  sleep 1
done

MONGO_IP=$(docker inspect service-mongo-1 --format '{{.NetworkSettings.Networks.service_default.IPAddress}}')
echo "Mongo IP: $MONGO_IP"

# Start app with mongo IP to bypass broken DNS
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

# Start Jenkins with --network host to bypass broken NAT (needs internet for plugins)
docker rm -f service-jenkins-1 2>/dev/null || true
docker run -d \
  --name service-jenkins-1 \
  --network host \
  --restart on-failure \
  -v "${JENKINS_CONFIG_ROOT:-/workspaces/jenkins_config}":/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /usr/bin/docker:/usr/bin/docker \
  -v "$(pwd)/Dockerfile":/Dockerfile \
  -v /usr/libexec/docker/cli-plugins/docker-compose:/usr/libexec/docker/cli-plugins/docker-compose \
  -u 0:0 \
  jenkins/jenkins --httpPort=8082
echo "Jenkins started at http://localhost:8082"
