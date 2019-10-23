FROM openjdk:8
LABEL maintainer="Tim deBoer"

RUN apt-get update && apt-get install -y ant && wget -O asciidoctor-ant.jar http://repo1.maven.org/maven2/org/asciidoctor/asciidoctor-ant/1.5.8/asciidoctor-ant-1.5.8.jar
COPY /cds wlp/usr/servers/cds
RUN wget -O liberty.zip https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/2019-10-02_0300/openliberty-webProfile8-19.0.0.10.zip \
   && unzip liberty.zip && /wlp/bin/server package cds --archive=../../../../cds.zip --include=minify && rm -f liberty.zip && rm -rf wlp