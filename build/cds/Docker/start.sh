#!/usr/bin/env bash
set -e

if [ -n "${PRESADMIN_PASSWORD}" ]; then
    sed -i "s|PRESADMIN_PASSWORD|${PRESADMIN_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${ADMIN_PASSWORD}" ]; then
    sed -i "s|ADMIN_PASSWORD|${ADMIN_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${BLUE_PASSWORD}" ]; then
    sed -i "s|BLUE_PASSWORD|${BLUE_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${BALLOON_PASSWORD}" ]; then
    sed -i "s|BALLOON_PASSWORD|${BALLOON_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${PUBLIC_PASSWORD}" ]; then
    sed -i "s|PUBLIC_PASSWORD|${PUBLIC_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${PRESENTATION_PASSWORD}" ]; then
    sed -i "s|PRESENTATION_PASSWORD|${PRESENTATION_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${MYICPC_PASSWORD}" ]; then
    sed -i "s|MYICPC_PASSWORD|${MYICPC_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${LIVE_PASSWORD}" ]; then
    sed -i "s|LIVE_PASSWORD|${LIVE_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${CCS_URL}" ]; then
    sed -i "s|CCS_URL|${CCS_URL}|" /opt/wlp/usr/servers/cds/config/cdsConfig.xml
fi
if [ -n "${CCS_USER}" ]; then
    sed -i "s|CCS_USER|${CCS_USER}|" /opt/wlp/usr/servers/cds/config/cdsConfig.xml
fi
if [ -n "${CCS_PASSWORD}" ]; then
    sed -i "s|CCS_PASSWORD|${CCS_PASSWORD}|" /opt/wlp/usr/servers/cds/config/cdsConfig.xml
fi

exec "$@"
