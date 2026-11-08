# Same as build/cds/Docker/Dockerfile, except that the CDS comes from a local build (the "dist" build
# context, see build.sh) instead of a GitHub release. Build it with build/cds/Docker as the context, so
# that start.sh, accounts.yaml and cdsConfig.xml are shared with the published image.
FROM ubuntu:26.04

ENV DEBIAN_FRONTEND=noninteractive
LABEL org.opencontainers.image.description="ICPC Tools Contest Data Server (local build)"
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

# Install CDS from the local build
RUN --mount=type=bind,from=dist,source=wlp.CDS-${CDS_VERSION}.zip,target=/tmp/wlp.CDS.zip \
    mkdir -p /opt && \
    unzip /tmp/wlp.CDS.zip -d /opt

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
