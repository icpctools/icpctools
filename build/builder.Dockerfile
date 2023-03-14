FROM openjdk:17-bullseye
LABEL maintainer="Tim deBoer"
LABEL org.opencontainers.image.description="ICPC Tools code builder"
LABEL org.opencontainers.image.source=https://github.com/icpctools/icpctools

RUN apt-get update \
   && apt-get install -y ant pandoc texlive \
   && rm -rf /var/lib/apt/lists/*
COPY /cds wlp/usr/servers/cds
RUN wget -O liberty.zip https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/22.0.0.10/openliberty-webProfile8-22.0.0.10.zip \
   && unzip liberty.zip && /wlp/bin/server package cds --archive=../../../../cds.zip --include=minify && rm -f liberty.zip && rm -rf wlp
