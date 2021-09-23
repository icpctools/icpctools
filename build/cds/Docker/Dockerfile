FROM ubuntu:focal

ENV DEBIAN_FRONTEND=noninteractive

ARG CDS_VERSION

# Install dependencies
RUN apt-get update && \ 
    apt-get install -y --no-install-recommends openjdk-16-jdk-headless unzip ca-certificates curl && \
    rm -rf /var/lib/apt/lists/

# Install CDS
RUN mkdir -p /opt && \
    curl -L -o /opt/wlp.CDS-${CDS_VERSION}.zip https://github.com/icpctools/icpctools/releases/download/v${CDS_VERSION}/wlp.CDS-${CDS_VERSION}.zip && \
    unzip /opt/wlp.CDS-${CDS_VERSION}.zip -d /opt

COPY users.xml /opt/wlp/usr/servers/cds/users.xml
COPY cdsConfig.xml /opt/wlp/usr/servers/cds/config/cdsConfig.xml
COPY start.sh /usr/local/bin/start.sh

RUN mkdir -p /contest && \
    mkdir -p /contest/teams && \
    mkdir -p /contest/organizations

VOLUME /contest

EXPOSE 8443
EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/start.sh"]
CMD /opt/wlp/bin/server run cds
