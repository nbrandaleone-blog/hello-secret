# Multi-stage Dockerfile
FROM node:latest

# Use TCP port 8080, as default. Container reads ENV "PORT".
# If PORT not set, the container defaults to TCP port 3000.
ENV PORT=8080
EXPOSE 8080

# Download Java for compilation phase
ENV CLOJURE_VER=1.10.3.814
WORKDIR /tmp

# Base image is Debian 9 (Stretch). We need to install Java 11.
RUN apt-get update && apt-get upgrade \
  && wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | apt-key add - \
  && echo "deb https://adoptopenjdk.jfrog.io/adoptopenjdk/deb stretch main" | tee /etc/apt/sources.list.d/adoptopenjdk.list \
  && apt-get update \
  && apt-get install --yes adoptopenjdk-11-hotspot \
  && npm install -g shadow-cljs \
  && curl -s https://download.clojure.org/install/linux-install-$CLOJURE_VER.sh | bash

# Create and change to the app directory
WORKDIR /usr/src/app

# Copy application dependency manifests to the container image.
# A wildcard is used to ensure copying both package.json AND package-lock.json (when available).
# Copying this first prevents re-running npm install on every code change.
COPY package*.json ./

# Install production dependencies.
# If you add a package-lock.json, speed your build by switching to 'npm ci'.
# RUN npm ci --only=production
RUN npm install --only=production

# Copy local code to the container image.
COPY . ./

# Transpile the Clojurescript into Javascript using Shadow-CLJS
RUN npx shadow-cljs release app

#################################################################

# Use the official lightweight node image (v15.12.0), for release.
# https://hub.docker.com/_/node
FROM node:current-alpine

WORKDIR /usr/src/app

# Copy over build artifact and libraries
COPY --from=0 /usr/src/app/target/hello-secret.js /usr/src/app/target/hello-secret.js 
COPY . ./
RUN npm install --only=production

# Run the web server on container startup
CMD ["node", "target/hello-secret.js"]
