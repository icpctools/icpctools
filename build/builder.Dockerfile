FROM docker.io/openjdk:17-bullseye
LABEL maintainer="ICPC Tools Team"
LABEL org.opencontainers.image.description="ICPC Tools code builder"
LABEL org.opencontainers.image.source=https://github.com/icpctools/icpctools

RUN apt-get update \
   && apt-get install -y ant pandoc texlive \
   && rm -rf /var/lib/apt/lists/*
COPY /cds wlp/usr/servers/cds
RUN wget -O liberty.zip https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/24.0.0.12/openliberty-webProfile10-24.0.0.12.zip \
   && unzip liberty.zip && /wlp/bin/server package cds --archive=../../../../cds.zip --include=minify && rm -f liberty.zip && rm -rf wlp
