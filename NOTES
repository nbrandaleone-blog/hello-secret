# create node shadow-cljs project
npx create-cljs-project hello-secret

## Install express and google/secret-manager libraries

## Compile once
npx shadow-cljs compile app

## Watch and reload + REPL
npx shadow-cljs watch app
node hello-secret.js

### Connect to the REPL ???
npx shadow-cljs cljs-repl app

## Compile release version of app
npx shadow-cljs release app
node hello-secret.js

# Create Docker container
docker build -t hello-secret:0.1 .
docker run --rm --env PORT=3000 --env TARGET=Universe \
  -d -p 3000:3000 hello-secret:0.1

## Push to DockerHub
docker tag hello-secret:0.1 nbrand/hello-secret:0.1
docker push nbrand/hello-secret:0.1

## Push to GCR
docker tag hello-secret:0.1 gcr.io/nicks-playground-3141/hello-secret:0.1
docker push gcr.io/nicks-playground-3141/hello-secret:0.1

# Using Cloud Build
gcloud builds submit --tag gcr.io/PROJECT-ID/hello-secret
gcloud builds submit --tag gcr.io/nicks-playground-3141/hello-secret

# Deploy to Cloud Run
gcloud run deploy <service-name> --image gcr.io/PROJECT-ID/helloworld --platform managed
gcloud run deploy hello-secret --image gcr.io/nicks-playground-3141/hello-secret --platform managed

## Set up credentials
export GOOGLE_APPLICATION_CREDENTIALS=/Users/nbrand/gcp/service-accounts/nicks-playground-3141-ce55e71ee2d3.json
