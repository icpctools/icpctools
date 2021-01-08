FROM openjdk:8
LABEL maintainer="Tim deBoer"

RUN apt-get update \
   && apt-get install -y ant pandoc texlive \
   && rm -rf /var/lib/apt/lists/*
COPY /cds wlp/usr/servers/cds
RUN wget -O liberty.zip https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/2020-02-04_1746/openliberty-webProfile8-20.0.0.2.zip \
   && unzip liberty.zip && /wlp/bin/server package cds --archive=../../../../cds.zip --include=minify && rm -f liberty.zip && rm -rf wlp
