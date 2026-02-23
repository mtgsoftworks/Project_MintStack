#!/bin/sh
set -e

REDIS_CONF="/etc/redis/redis.conf"

if [ -n "$REDIS_PASSWORD" ]; then
    sed -i "s/\${REDIS_PASSWORD}/$REDIS_PASSWORD/g" "$REDIS_CONF"
fi

exec redis-server "$REDIS_CONF"
