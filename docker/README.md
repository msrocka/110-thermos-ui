# Docker setup

For the Docker setup, we have two images that we need to build:

+ `thermos-postgis` for the Thermos database
+ `thermos-app` for the Thermos application

Use the `db.Dockerfile` to build the database image like this:

```bash

# build the image
docker build -t thermos-postgis . -f db.Dockerfile

# run it
docker run --rm -d -p 5432:5432 --name thermos-postgis thermos-postgis

# or interactively
docker run --rm -it -p 5432:5432 --name thermos-postgis thermos-postgis
````

Running database container is also useful when developing the application.

For the application image, use the `app.Dockerfile`. **Make sure** to run the 
application build first, before building the image:

```bash

# first build the application; this will produce the target/thermos.jar file
clojure -M:client:server:dev pkg

cd docker

# build the image
docker build -t thermos-app . -f app.Dockerfile

# run it
docker run --rm -d -p 8000:8000 --name thermos-app thermos-app

# or interactively
docker run --rm -it -p 8000:8000 --name thermos-app thermos-app
```


## Deployment

The current (stupid) deployment strategy is to build the images locally, export
them to a tarball, and then import them on the server. This is done like this:

```bash
docker save thermos-postgis > thermos-postgis.tar
docker save thermos-app > thermos-app.tar
```

Then copy the tarballs to the server and import them:

```bash
docker load < thermos-postgis.tar
docker load < thermos-app.tar
```

(Note that we plan to use a proper CI/CD pipeline in the future.)