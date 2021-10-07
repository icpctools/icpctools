FROM ubuntu:focal

ENV DEBIAN_FRONTEND=noninteractive

ARG CDS_VERSION

# Install dependencies
RUN apt-get update && \ 
    apt-get install -y --no-install-recommends unzip ca-certificates curl && \
    rm -rf /var/lib/apt/lists/

# Install java
RUN mkdir -p /opt/jdk && \
    curl -L -o /root/openjdk15.tar.gz https://download.java.net/java/GA/jdk15.0.2/0d1cfde4252546c6931946de8db48ee2/7/GPL/openjdk-15.0.2_linux-x64_bin.tar.gz && \
    tar -zxf /root/openjdk15.tar.gz -C /opt/jdk && \
    update-alternatives --install /usr/bin/java java /opt/jdk/jdk-15.0.2/bin/java 100 && \
    update-alternatives --install /usr/bin/javac javac /opt/jdk/jdk-15.0.2/bin/javac 100

# Install CDS
RUN mkdir -p /opt && \
    curl -L -o /opt/wlp.CDS-${CDS_VERSION}.zip https://github.com/icpctools/icpctools/releases/download/v${CDS_VERSION}/wlp.CDS-${CDS_VERSION}.zip && \
    unzip /opt/wlp.CDS-${CDS_VERSION}.zip -d /opt

COPY users.xml /opt/wlp/usr/servers/cds/users.xml
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
