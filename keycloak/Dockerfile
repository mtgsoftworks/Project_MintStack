FROM quay.io/keycloak/keycloak:26.5.4 AS builder

ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.5.4

COPY --from=builder /opt/keycloak/ /opt/keycloak/

USER root
COPY docker-entrypoint.sh /opt/keycloak/bin/mintstack-entrypoint.sh
COPY --chown=1000:0 realm-export.json /opt/keycloak/data/import/realm-export.json
RUN chmod 0755 /opt/keycloak/bin/mintstack-entrypoint.sh
USER 1000

ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_HTTP_ENABLED=true
ENV KC_HTTP_PORT=8080
ENV KC_PROXY_HEADERS=xforwarded

EXPOSE 8080 9000

ENTRYPOINT ["/opt/keycloak/bin/mintstack-entrypoint.sh"]
CMD ["start", "--optimized", "--import-realm"]
