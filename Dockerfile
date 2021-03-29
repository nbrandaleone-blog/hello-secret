# Use the official lightweight node image (v15.12.0).
# https://hub.docker.com/_/node
FROM node:current-alpine

# Use TCP port 8080
ENV PORT=8080
EXPOSE 8080

# Create and change to the app directory.
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
# I assume that the Clojurescript code has already been transpiled into JS.
# I could include the [Shadow-CLJS](https://github.com/thheller/shadow-cljs) library 
# and do compilation step as well, if needed.
COPY . ./

# Run the web service on container startup.
CMD ["node", "target/hello-secret.js"]

# Add a .dockerignore file to exclude files from your container image.
