FROM docker.io/eclipse-temurin:17-jdk

LABEL maintainer="ICPC Tools Team"
LABEL org.opencontainers.image.description="ICPC Tools code builder"
LABEL org.opencontainers.image.source=https://github.com/icpctools/icpctools

RUN apt-get update \
   && apt-get install -y ant pandoc texlive unzip wget git \
   && rm -rf /var/lib/apt/lists/*
COPY /cds wlp/usr/servers/cds
RUN wget -O liberty.zip https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/26.0.0.10/openliberty-webProfile11-26.0.0.10.zip \
   && unzip liberty.zip && /wlp/bin/server package cds --archive=../../../../cds.zip --include=minify && rm -f liberty.zip && rm -rf wlp
