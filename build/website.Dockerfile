FROM ubuntu:22.04
LABEL maintainer="Tim deBoer"
LABEL org.opencontainers.image.description="ICPC Tools website builder"
LABEL org.opencontainers.image.source=https://github.com/icpctools/icpctools

RUN apt-get update \
   && apt-get install -y wget openssh-client git httpie jq curl python3 python3-requests unzip \
   && rm -rf /var/lib/apt/lists/* \
   && wget -O /root/hugo.deb https://github.com/gohugoio/hugo/releases/download/v0.80.0/hugo_0.80.0_Linux-64bit.deb \
   && dpkg -i /root/hugo.deb \
   && rm /root/hugo.deb
