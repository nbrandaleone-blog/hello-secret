# Makefile
# https://opensource.com/article/18/8/what-how-makefile

compile:
	npx shadow-cljs compile app

watch:
	npx shadow-cljs watch app

release:
	npx shadow-cljs release app
	mv target/main.js target/hello-secret.js

docker:
	docker build -t hello-secret .

push:
	echo "docker push"


