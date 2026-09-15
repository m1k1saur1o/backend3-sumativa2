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
        ├── config/       RestClient hacia ms-backend (URL y timeouts por properties)
        ├── clients/      DataServiceClient — unico punto que llama a ms-backend
        ├── dtos/backend/ espejo del contrato de ms-backend
        ├── exceptions/   GlobalExceptionHandler (404 / 400 / 503 / 500)
        ├── web/          BffWebController + BffWebService + dtos/
        ├── movil/        BffMovilController + BffMovilService + dtos/
        └── cajeros/      BffCajerosController + BffCajerosService + dtos/
```

La carpeta `bff-unificado/` es el microservicio de ejemplo entregado como referencia y **no
forma parte de la solucion**: se mantiene intacto.

## 3. Como ejecutar

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
./mvnw spring-boot:run        # http://localhost:8081
```

**3) Microservicio BFF (terminal 2):**

```bash
cd ms-bff
./mvnw spring-boot:run        # http://localhost:8080
```

El BFF lee la URL del microservicio de datos desde `ms-bff/src/main/resources/application.properties`
(`data-service.url=http://localhost:8081`); no esta hardcodeada en el codigo.

## 4. Pruebas con Postman

Importar `sumativa2.postman_collection.json`, o probar manualmente los 9 endpoints. Todos son
`GET` y responden `application/json`. En los ejemplos se usa la cuenta `110`.

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

Los endpoints internos de `ms-backend` (`/api/cuentas-anuales/{cuentaId}`,
`/api/intereses/{cuentaId}`, `/api/transacciones/{id}` en el puerto 8081) tambien pueden
consultarse desde Postman para verificar el acceso a datos, pero estan pensados para ser
consumidos por el BFF, no por los clientes finales.
