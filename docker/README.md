# Docker setup

For the Docker setup, we have two images that we need to build:

+ `thermos-db` for the Thermos database
+ `thermos-app` for the Thermos application


## Building the database image

Use the `db.Dockerfile` to build the database image like this:

```bash

# build the image
docker build -t thermos-db . -f db.Dockerfile

# run it
docker run --rm -d -p 5432:5432 --name thermos-db thermos-db

# or interactively
docker run --rm -it -p 5432:5432 --name thermos-db thermos-db

# and of course, map the data outside of the container ...
docker run --rm -it \
  -v ./data/thermos-db:/app/data/db \
  -e PGDATA=/app/data/db \
  -p 5432:5432 \
  --name thermos-db \
  thermos-db
```

A running database container is also useful when developing the application.


## Building the application image

For the application image, use the `app.Dockerfile`. **Make sure** to run the
application build first and copy it to the `docker` folder, before building the
image:

```bash

# first build the application; this will produce the target/thermos.jar file
clojure -M:client:server:dev pkg
rm docker/thermos.jar
cp target/thermos.jar docker/

cd docker

# build the image
docker build -t thermos-app . -f app.Dockerfile

# run it
docker run --rm -d -p 3000:3000 --name thermos-app thermos-app

# or interactively
docker run --rm -it -p 3000:3000 --name thermos-app thermos-app
```


## Deployment

The current (stupid) deployment strategy is to build the images locally, export
them to a tarball, and then import them on the server. This is done like this:

```bash
docker save thermos-db > thermos-db.tar
docker save thermos-app > thermos-app.tar

# for faster transfer to the server, you want to compress the tarballs
gzip thermos-db.tar  # this will create thermos-db.tar.gz
gzip thermos-app.tar  # this will create thermos-app.tar.gz
```

Then copy the tarballs to the server and import them:

```bash
# decompress the tarballs first
gunzip thermos-db.tar.gz
gunzip thermos-app.tar.gz

# then load them
docker load < thermos-db.tar
docker load < thermos-app.tar
```

(Note that we plan to use a proper CI/CD pipeline in the future.)

There is a `docker-compose.yml` file that can be used to run the application
and the database together.

```bash
docker compose up

# add the -d flag to run it in detached mode
docker compose up -d
```
