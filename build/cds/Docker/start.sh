#!/usr/bin/env bash
set -e

if [ -n "${ADMIN_PASSWORD}" ]; then
    sed -i "s|__ADMIN_PASSWORD__|${ADMIN_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${PRESADMIN_PASSWORD}" ]; then
    sed -i "s|__PRESADMIN_PASSWORD__|${PRESADMIN_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${BLUE_PASSWORD}" ]; then
    sed -i "s|__BLUE_PASSWORD__|${BLUE_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${BALLOON_PASSWORD}" ]; then
    sed -i "s|__BALLOON_PASSWORD__|${BALLOON_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${PUBLIC_PASSWORD}" ]; then
    sed -i "s|__PUBLIC_PASSWORD__|${PUBLIC_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${PRESENTATION_PASSWORD}" ]; then
    sed -i "s|__PRESENTATION_PASSWORD__|${PRESENTATION_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${MYICPC_PASSWORD}" ]; then
    sed -i "s|__MYICPC_PASSWORD__|${MYICPC_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${LIVE_PASSWORD}" ]; then
    sed -i "s|__LIVE_PASSWORD__|${LIVE_PASSWORD}|" /opt/wlp/usr/servers/cds/users.xml
fi
if [ -n "${CCS_URL}" ]; then
    sed -i "s|__CCS_URL__|${CCS_URL}|" /opt/wlp/usr/servers/cds/config/cdsConfig.xml
fi
if [ -n "${CCS_USER}" ]; then
    sed -i "s|__CCS_USER__|${CCS_USER}|" /opt/wlp/usr/servers/cds/config/cdsConfig.xml
fi
if [ -n "${CCS_PASSWORD}" ]; then
    sed -i "s|__CCS_PASSWORD__|${CCS_PASSWORD}|" /opt/wlp/usr/servers/cds/config/cdsConfig.xml
fi

exec "$@"
