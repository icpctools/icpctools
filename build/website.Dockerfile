FROM ubuntu:20.04
LABEL maintainer="Tim deBoer"

RUN apt-get update \
   && apt-get install -y wget openssh-client git httpie jq curl python3 python3-requests \
   && rm -rf /var/lib/apt/lists/* \
   && wget -O /root/hugo.deb https://github.com/gohugoio/hugo/releases/download/v0.80.0/hugo_0.80.0_Linux-64bit.deb \
   && dpkg -i /root/hugo.deb \
   && rm /root/hugo.deb
