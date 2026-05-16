FROM quay.io/keycloak/keycloak:26.5.4

COPY realm-export.json /opt/keycloak/data/import/realm-export.json

ENV KC_HTTP_ENABLED=true
ENV KC_PROXY_HEADERS=xforwarded
ENV KC_HOSTNAME_STRICT=false
ENV KC_HEALTH_ENABLED=true
ENV KC_HTTP_MANAGEMENT_HEALTH_ENABLED=false
ENV PORT=8080

EXPOSE 8080

ENTRYPOINT []
CMD ["sh", "-c", "exec /opt/keycloak/bin/kc.sh start-dev --import-realm --http-port=${PORT:-8080}"]
