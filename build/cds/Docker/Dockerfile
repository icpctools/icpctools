FROM ubuntu:focal

ENV DEBIAN_FRONTEND=noninteractive
LABEL org.opencontainers.image.description="ICPC Tools Contest Data Server"
LABEL org.opencontainers.image.source=https://github.com/icpctools/icpctools

ARG CDS_VERSION

# Install dependencies
RUN apt-get update && \ 
    apt-get install -y --no-install-recommends unzip ca-certificates curl zip && \
    rm -rf /var/lib/apt/lists/

# Install SDKMAN!
RUN rm /bin/sh && ln -s /bin/bash /bin/sh
RUN curl -s "https://get.sdkman.io" | bash
RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install java 17.0.1-open

# Install CDS
RUN mkdir -p /opt && \
    curl -L -o /opt/wlp.CDS-${CDS_VERSION}.zip https://github.com/icpctools/icpctools/releases/download/v${CDS_VERSION}/wlp.CDS-${CDS_VERSION}.zip && \
    unzip /opt/wlp.CDS-${CDS_VERSION}.zip -d /opt

COPY accounts.yaml /opt/wlp/usr/servers/cds/config/accounts.yaml
COPY cdsConfig.xml /opt/wlp/usr/servers/cds/config/cdsConfig.xml
COPY start.sh /usr/local/bin/start.sh

RUN mkdir -p /contest && \
    mkdir -p /contest/teams && \
    mkdir -p /contest/organizations && \
    mkdir -p /contest/config && \
    mkdir -p /contest/registration

VOLUME /contest

EXPOSE 8443
EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/start.sh"]
CMD /opt/wlp/bin/server run cds
