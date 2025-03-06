
# thermos-postgis

The Dockerfile in this folder builds the Thermos database image:

```bash
# build the image
docker build -t thermos-postgis .

# run it
docker run --rm -d -p 5432:5432 --name thermos-postgis thermos-postgis

# or interactively
docker run --rm -it -p 5432:5432 --name thermos-postgis thermos-postgis

```
