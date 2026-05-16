# Railway Deployment

Bu proje Railway icin minimum servis modeliyle calistirilmelidir. Yerel Docker
imajlari Railway'e otomatik gitmez; Railway GitHub reposundaki Dockerfile'lari
build eder ve her push sonrasi otomatik redeploy yapar.

## Minimum servisler

| Servis | Railway root directory | Zorunlu mu | Not |
| --- | --- | --- | --- |
| Postgres | Railway PostgreSQL plugin | Evet | Uygulama veritabani |
| Redis | Railway Redis plugin | Evet | Cache ve rate limit |
| keycloak | `/keycloak` | Evet | OIDC login |
| backend | `/backend` | Evet | Spring Boot API |
| frontend | `/frontend` | Evet | React/Nginx arayuz |

Kafka, OpenSearch, Grafana, Prometheus, Alertmanager, OpenLDAP Railway demo
kurulumunda zorunlu degildir. Bunlar lokal/kurumsal observability ve event
altyapisi icin tutulur.

## Servis adlari

Railway'de su adlari birebir kullanin. Reference variable'lar bu isimlere gore
calisir:

- `Postgres`
- `Redis`
- `keycloak`
- `backend`
- `frontend`

## Build ayarlari

Her uygulama servisinde root directory secin:

- Backend: `/backend`
- Frontend: `/frontend`
- Keycloak: `/keycloak`

Bu dizinlerdeki `railway.toml` dosyalari Dockerfile build ve healthcheck
ayarlarini verir. Railway config-as-code environment variable/secrets set etmez;
secret ve servis baglantilari Railway Variables uzerinden verilmelidir.

Onerilen kurulum sirasi:

1. `Postgres` plugin ekle.
2. `Redis` plugin ekle.
3. `keycloak` servisini GitHub repo + `/keycloak` root directory ile ekle.
4. `keycloak` icin public domain uret.
5. `backend` servisini GitHub repo + `/backend` root directory ile ekle.
6. `backend` icin public domain uret.
7. `frontend` servisini GitHub repo + `/frontend` root directory ile ekle.
8. `frontend` icin public domain uret.

Frontend `VITE_*` degerlerini build sirasinda gomdugu icin Keycloak veya
backend domaini degistiginde frontend'i yeniden deploy edin.

## Keycloak variables

`keycloak` servisinde Variables -> Raw Editor:

```env
KC_BOOTSTRAP_ADMIN_USERNAME=admin
KC_BOOTSTRAP_ADMIN_PASSWORD=<STRONG_PASSWORD>
KEYCLOAK_FINANCE_BACKEND_SECRET=<STRONG_RANDOM_SECRET>
KEYCLOAK_ADMIN_USER_PASSWORD=<STRONG_APP_ADMIN_PASSWORD>
KEYCLOAK_TEST_USER_PASSWORD=<STRONG_TEST_USER_PASSWORD>
KC_HTTP_ENABLED=true
KC_PROXY_HEADERS=xforwarded
KC_HOSTNAME_STRICT=false
KC_HEALTH_ENABLED=true
KC_HTTP_MANAGEMENT_HEALTH_ENABLED=false
KC_DB=postgres
KC_DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
KC_DB_USERNAME=${{Postgres.PGUSER}}
KC_DB_PASSWORD=${{Postgres.PGPASSWORD}}
KC_TRANSACTION_XA_ENABLED=false
KC_HOSTNAME=https://${{keycloak.RAILWAY_PUBLIC_DOMAIN}}
```

Public Networking altindan domain uretin.

## Backend variables

`backend` servisinde Variables -> Raw Editor:

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
SPRING_DATA_REDIS_HOST=${{Redis.REDISHOST}}
SPRING_DATA_REDIS_PORT=${{Redis.REDISPORT}}
SPRING_DATA_REDIS_PASSWORD=${{Redis.REDISPASSWORD}}
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://${{keycloak.RAILWAY_PUBLIC_DOMAIN}}/realms/mintstack-finance
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://${{keycloak.RAILWAY_PUBLIC_DOMAIN}}/realms/mintstack-finance/protocol/openid-connect/certs
APP_MESSAGING_ENABLED=false
APP_MARKET_DATA_CONSUMER_ENABLED=false
APP_EXTERNAL_API_FINTABLES_ENABLED=false
APP_EXTERNAL_API_BIST_DATASTORE_ENABLED=true
APP_NEWS_LLM_ENABLED=false
OTEL_SDK_DISABLED=true
LOGGING_CONFIG=classpath:log4j2-console.xml
```

Public Networking altindan domain uretin.

## Frontend variables

`frontend` servisinde Variables -> Raw Editor:

```env
VITE_API_URL=https://${{backend.RAILWAY_PUBLIC_DOMAIN}}/api/v1
VITE_WS_URL=https://${{backend.RAILWAY_PUBLIC_DOMAIN}}/ws
VITE_KEYCLOAK_URL=https://${{keycloak.RAILWAY_PUBLIC_DOMAIN}}
VITE_KEYCLOAK_REALM=mintstack-finance
VITE_KEYCLOAK_CLIENT_ID=finance-frontend
```

Bu degiskenler build-time oldugu icin degistirildiginde frontend redeploy
edilmelidir.

## Auto deploy

GitHub repo Railway'e baglandiktan sonra Railway GitHub push'larini otomatik
deploy eder. Backend, frontend ve Keycloak icin ayrica image push etmeye gerek
yoktur.

## Dikkat

- Railway'de environment/secrets dosyaya yazilmaz; Variables ekraninda tutulur.
- Keycloak admin sifresi repoya yazilmaz.
- VIOP ve Tahvil/Bono icin dogrulanmis dis veri kaynagi baglanmadikca sahte
  fiyat gosterilmez.
