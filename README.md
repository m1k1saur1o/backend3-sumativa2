# Patron BFF — ms-backend + ms-bff

## 1. Objetivo

Demostrar el patron **BFF (Backend For Frontend)**: un mismo conjunto de datos se expone de
tres formas distintas, una por cada tipo de cliente (Web, Movil y Cajeros Automaticos).

La solucion son **dos microservicios independientes**:

```
MySQL (Docker) --JDBC/JPA--> ms-backend --HTTP/REST--> ms-bff --> Web / Movil / Cajeros
```

* `ms-backend` es el unico que accede a la base de datos.
* `ms-bff` es el unico que conoce las necesidades de cada canal; nunca se conecta a MySQL.
* Cada canal del BFF tiene su **propio controller, service y DTOs**, sin compartirlos entre si.

| Recurso | Web | Movil | Cajeros |
|---|---|---|---|
| `cuentas_anuales` | todos los registros, todos los campos | 3 mas recientes; solo `fecha`, `monto`, `transaccion` | 1 registro (el ultimo); solo `fecha`, `monto` |
| `intereses` | todos los registros, todos los campos | todos; solo `nombre`, `saldo`, `tipo` | 1 registro (el ultimo); solo `nombre`, `saldo` |
| `transacciones` | todos los registros, todos los campos | todos; solo `fecha`, `monto` | 1 registro (el ultimo); solo `fecha`, `monto` |

El orden es deterministico y lo aplica `ms-backend` en la consulta:
`cuentas_anuales` → `fecha DESC, id DESC`; `intereses` → `id DESC`;
`transacciones` → `fecha DESC, registro_id DESC`. El "ultimo registro" es siempre el primero
de la lista y los "3 mas recientes" son los tres primeros.

### Integracion con sumativa3-backend3 (JWT, Eureka y auditoria en Kafka)

`ms-bff` y `ms-backend` se integran con la plataforma del proyecto hermano `../sumativa3-backend3`:

```
Cliente ─Bearer JWT─▶ ms-bff :18080 ──http://ms-backend──▶ ms-backend x2 (Eureka + LoadBalancer)
                        │
                        └─(asincrono)─▶ producer-service ─▶ Kafka "bff-call-events" ─▶ consumer-service
JWT: POST http://localhost:8080/auth/login (auth-service)      Registro: discovery-service (Eureka)
```

* **Seguridad**: `ms-bff` es OAuth2 Resource Server. Todos los `/api/**` exigen
  `Authorization: Bearer <JWT>` emitido por `auth-service` (HS256 con `JWT_SECRET`, issuer
  `JWT_ISSUER`, audiencia `BFF_JWT_AUDIENCE`=`bff-api`, expiracion) y rol `USER` o `ADMIN`.
  Sin token o token invalido: **401**; token valido sin rol: **403**. `/actuator/health` es publico.
  Los errores de seguridad usan el mismo formato `{"status":...,"message":...}` del BFF.
* **Descubrimiento y balanceo**: `ms-backend` se registra en Eureka (una entrada por replica) y el BFF
  lo llama por nombre logico (`data-service.url=http://ms-backend`) con un `RestClient` `@LoadBalanced`
  (round robin). Se conservan los timeouts (2 s / 3 s) y se agregan: 1 reintento en otra replica
  (solo GET, ante error de conexion/timeout o 502/503/504) y un circuit breaker `msBackend` que solo
  cuenta fallas de disponibilidad. Los mensajes de error 404/400/503 son los mismos de antes.
* **Auditoria**: cada llamada a `/api/**` (incluidas 400/401/403/404/500/503) genera un evento
  `BffCallEvent` v1 que se publica de forma asincrona via `producer-service` en el topico
  `bff-call-events`. Esquema y decisiones: `../sumativa3-backend3/docs/EVENTO_AUDITORIA_BFF.md`.
* **Correlacion**: header `X-Request-Id` (se acepta del cliente o se genera), devuelto en la respuesta,
  propagado a `ms-backend` y presente en los logs (`[requestId]`).

Guia de pruebas paso a paso: `../sumativa3-backend3/INSTRUCCIONES_POSTMAN.txt`.

## 2. Estructura del codigo

```
proyecto/
├── README.md
├── sumativa2.postman_collection.json   (opcional: coleccion lista para importar en Postman)
├── ms-backend/                         acceso a datos — puerto 8081
│   ├── pom.xml
│   └── src/main/java/cl/duoc/sumativa2/backend/
│       ├── controller/   CuentaAnualController, InteresController, TransaccionController
│       ├── service/      logica de consulta y traduccion a DTO
│       ├── repository/   Spring Data JPA (ordenamiento deterministico)
│       ├── entity/       CuentaAnual, Interes, Transaccion
│       ├── dto/          contratos REST + ApiErrorDTO
│       └── exception/    GlobalExceptionHandler (404 / 400 / 500)
└── ms-bff/                             BFF — puerto 8080
    ├── pom.xml
    └── src/main/java/cl/duoc/sumativa2/bff/
        ├── config/       RestClient @LoadBalanced hacia ms-backend, JwtProperties, AuditoriaProperties
        ├── clients/      DataServiceClient — unico punto que llama a ms-backend (circuit breaker)
        ├── security/     SecurityConfig (Resource Server JWT) + JsonSecurityErrorHandler (401/403)
        ├── auditoria/    AuditoriaFilter, BffCallEvent v1, ProducerAuditoriaPublisher, token de servicio
        ├── dtos/backend/ espejo del contrato de ms-backend
        ├── exceptions/   GlobalExceptionHandler (404 / 400 / 503 / 500)
        ├── web/          BffWebController + BffWebService + dtos/
        ├── movil/        BffMovilController + BffMovilService + dtos/
        └── cajeros/      BffCajerosController + BffCajerosService + dtos/
```

La carpeta `bff-unificado/` es el microservicio de ejemplo entregado como referencia y **no
forma parte de la solucion**: se mantiene intacto.

## 3. Como ejecutar

### Opcion A (recomendada): Docker Compose integrado

```bash
cp .env.example .env && chmod 600 .env     # mismos JWT_SECRET, AUTH_CLIENT_*, EUREKA_* que sumativa3
(cd ../sumativa3-backend3 && docker compose up -d --build)   # crea la red sumativa-net, Eureka, Kafka...
docker compose up -d --build                                 # ms-backend x2 (interno) + ms-bff :18080
docker compose ps                                            # todo "healthy"
docker compose up -d --scale ms-backend=3                    # mas replicas, sin tocar el BFF
```

`ms-backend` no publica puertos: solo `ms-bff` lo alcanza. Las credenciales de MySQL, el secreto JWT
y las credenciales de Eureka se leen de `.env` (no versionado). Pruebas Maven:

```bash
(cd ms-backend && ./mvnw clean verify)   # 4 pruebas (H2 en memoria): contrato REST y health
(cd ms-bff && ./mvnw clean verify)       # 46 pruebas: seguridad JWT, auditoria, balanceo, failover, circuit breaker
```

### Opcion B: ejecucion local con Maven (sin Docker)

**Requisitos:** Java 17 o superior y Docker. Maven no es necesario: cada proyecto incluye el
wrapper `./mvnw`.

**1) Base de datos MySQL (si el contenedor aun no existe):**

```bash
docker run --name mysql-dev -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=myapp \
  -e MYSQL_USER=myuser \
  -e MYSQL_PASSWORD=mypassword \
  -d mysql:8.4
```

Si el contenedor ya existe, basta con `docker start mysql-dev`. Las tablas
`cuentas_anuales`, `intereses` y `transacciones` deben estar creadas y con datos.

**2) Microservicio de datos (terminal 1):**

```bash
cd ms-backend
DB_PASSWORD=<clave-mysql> EUREKA_ENABLED=false ./mvnw spring-boot:run        # http://localhost:8081
```

**3) Microservicio BFF (terminal 2):** sin Eureka, la instancia de `ms-backend` se declara en el
`SimpleDiscoveryClient`; la auditoria queda solo en el log. Usa un puerto distinto de 8080 si
`auth-service` esta corriendo.

```bash
cd ms-bff
JWT_SECRET=<mismo-secreto-de-auth-service> EUREKA_ENABLED=false AUDIT_ENABLED=false SERVER_PORT=18080 \
SPRING_APPLICATION_JSON='{"spring.cloud.discovery.client.simple.instances.ms-backend[0].uri":"http://localhost:8081"}' \
./mvnw spring-boot:run
```

El BFF lee la URL del microservicio de datos desde `data-service.url` (`DATA_SERVICE_URL`, por defecto
el nombre logico `http://ms-backend`); no hay IPs ni puertos fijos en el codigo.

## 4. Pruebas con Postman

Importar `sumativa2.postman_collection.json`: incluye el login contra `auth-service` (guarda el token en
la variable `token`) y todos los endpoints con `Authorization: Bearer {{token}}`. Variables:
`bff_url` (`http://localhost:18080` con Docker), `auth_url`, `username` y `password` (completar con
`AUTH_USERNAME`/`AUTH_PASSWORD` del `.env` de sumativa3). Todos los endpoints son `GET`, requieren el
token y responden `application/json`. En los ejemplos se usa la cuenta `110` y el puerto 8080 de la
ejecucion local; con Docker el BFF esta en `http://localhost:18080`.

### BFF Web — informacion completa

| Metodo | URL | Parametro | Codigo |
|---|---|---|---|
| GET | `http://localhost:8080/api/web/cuentas/110` | `cuentaId` (path) | 200 |
| GET | `http://localhost:8080/api/web/intereses/110` | `cuentaId` (path) | 200 |
| GET | `http://localhost:8080/api/web/transacciones/110` | `id` (path) | 200 |

```json
[ { "id": 449, "cuentaId": "110", "fecha": "2024-11-23", "transaccion": "compra",
    "monto": 1500.00, "descripcion": "Ingreso mensual" } ]

[ { "id": 646, "cuentaId": "110", "nombre": "Charlie Green", "saldo": 10000.00,
    "edad": 25, "tipo": "prestamo" } ]

[ { "registroId": 240, "id": "110", "fecha": "2024-07-05", "monto": 800.00,
    "tipo": "debito" } ]
```

### BFF Movil — respuesta reducida

| Metodo | URL | Parametro | Codigo |
|---|---|---|---|
| GET | `http://localhost:8080/api/movil/cuentas/101` | `cuentaId` (path) | 200 |
| GET | `http://localhost:8080/api/movil/intereses/110` | `cuentaId` (path) | 200 |
| GET | `http://localhost:8080/api/movil/transacciones/110` | `id` (path) | 200 |

```json
[ { "fecha": "2024-12-22", "monto": 2500.00, "transaccion": "deposito" },
  { "fecha": "2024-12-22", "monto": 2500.00, "transaccion": "deposito" },
  { "fecha": "2024-11-23", "monto": 1500.00, "transaccion": "compra" } ]

[ { "nombre": "Charlie Green", "saldo": 10000.00, "tipo": "prestamo" } ]

[ { "fecha": "2024-07-05", "monto": 800.00 } ]
```

### BFF Cajeros Automaticos — respuesta minima (un solo objeto)

| Metodo | URL | Parametro | Codigo |
|---|---|---|---|
| GET | `http://localhost:8080/api/cajeros/cuentas/110` | `cuentaId` (path) | 200 |
| GET | `http://localhost:8080/api/cajeros/intereses/110` | `cuentaId` (path) | 200 |
| GET | `http://localhost:8080/api/cajeros/transacciones/110` | `id` (path) | 200 |

```json
{ "fecha": "2024-11-23", "monto": 1500.00 }

{ "nombre": "Charlie Green", "saldo": 10000.00 }

{ "fecha": "2024-07-05", "monto": 800.00 }
```

### Errores

| Caso | Ejemplo | Codigo | Respuesta |
|---|---|---|---|
| Sin registros | `GET /api/web/cuentas/99999` | 404 | `{"status":404,"message":"No se encontraron registros de cuentas anuales para la cuenta 99999"}` |
| Identificador invalido (> 50 caracteres) | `GET /api/web/cuentas/999...9` | 400 | `{"status":400,"message":"El identificador '999...9' no es valido"}` |
| `ms-backend` detenido | `GET /api/movil/intereses/110` | 503 | `{"status":503,"message":"El microservicio de datos no esta disponible en este momento"}` |
| Error inesperado | — | 500 | `{"status":500,"message":"Error interno del BFF"}` |
| Sin token / token invalido | `GET /api/movil/cuentas/110` | 401 | `{"status":401,"message":"Falta el token. Envie Authorization: Bearer <access_token>"}` |
| Token sin rol USER/ADMIN | `GET /api/cajeros/cuentas/110` | 403 | `{"status":403,"message":"El token no tiene los roles necesarios (USER o ADMIN)"}` |
| Ruta inexistente bajo `/api` | `GET /api/movil/otra/110` | 404 | `{"status":404,"message":"El recurso solicitado no existe"}` |

Los endpoints internos de `ms-backend` (`/api/cuentas-anuales/{cuentaId}`,
`/api/intereses/{cuentaId}`, `/api/transacciones/{id}` en el puerto 8081) solo son accesibles en la
ejecucion local; con Docker `ms-backend` no se publica y solo lo consume el BFF.
