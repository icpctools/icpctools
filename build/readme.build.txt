docker build . -f builder.Dockerfile -t icpctools/builder
docker push icpctools/builder

docker build . -f website.Dockerfile -t icpctools/website
docker push icpctools/website