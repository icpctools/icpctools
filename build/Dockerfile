FROM openjdk:8
LABEL maintainer="Tim deBoer"

RUN apt-get update \
   && apt-get install -y ant httpie jq curl python3 python3-requests \
   && rm -rf /var/lib/apt/lists/* \
   && wget -O asciidoctor-ant.jar http://repo1.maven.org/maven2/org/asciidoctor/asciidoctor-ant/1.5.8/asciidoctor-ant-1.5.8.jar \
   && wget -O /root/hugo.deb https://github.com/gohugoio/hugo/releases/download/v0.61.0/hugo_0.61.0_Linux-64bit.deb \
   && dpkg -i /root/hugo.deb \
   && rm /root/hugo.deb
COPY /cds wlp/usr/servers/cds
RUN wget -O liberty.zip 
https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/2020-02-04_1746/openliberty-webProfile8-20.0.0.2.zip \
   && unzip liberty.zip && /wlp/bin/server package cds --archive=../../../../cds.zip --include=minify && rm -f liberty.zip && rm -rf wlp
