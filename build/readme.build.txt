docker build . -f builder.Dockerfile -t ghcr.io/icpctools/builder
docker push ghcr.io/icpctools/builder

docker build . -f website.Dockerfile -t ghcr.io/icpctools/website
docker push ghcr.io/icpctools/website
