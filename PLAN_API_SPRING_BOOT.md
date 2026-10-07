# Plan de API Spring Boot y portal de puntos

**Proyecto:** Pan, Puntos y Premios
**Fecha:** octubre de 2026
**Estado:** plan de implementación y despliegue; API y portal aún no implementados
**Arquitectura acordada:** Next.js en Vercel, API Spring Boot en Oracle Cloud Infrastructure (OCI), conexión a la Oracle Autonomous Database existente.

---

## 1. Objetivo y conclusión de viabilidad

Agregar una página web para que cada cliente consulte su saldo e historial de puntos de forma segura. La página consumirá una API REST en Spring Boot. La aplicación Swing y su entrega académica permanecen independientes.

El proyecto actual ya usa Java 17, Maven, Oracle, DAOs y servicios de negocio, y el equipo ya tiene una Oracle Autonomous Database con la información del sistema. El esquema incluye `cliente.saldo_puntos`, `movimiento_puntos` y `cliente.email`; los servicios existentes permiten consultar movimientos. **Aún no existe la API ni el portal web.** Hay que implementar ambos antes de desplegarlos. La base actual se conserva: no crear otra, no reemplazarla ni volver a cargar el DDL/datos semilla sobre producción.

El alcance anterior de panel de cajero/administrador sin autenticación queda descartado. El sitio no permitirá administrar clientes, catálogo, inventario, pedidos, ventas ni canjes.

La viabilidad depende de identificar al cliente con seguridad. El identificador de cliente y el ID de Oracle no son credenciales. Se propone enviar un enlace de acceso de un solo uso al correo previamente verificado. El esquema actual no registra si el correo está verificado, por lo que la verificación y los clientes sin un correo utilizable deben resolverse antes de habilitar acceso real.

## 2. Arquitectura acordada

```text
Cliente
  │ HTTPS
  ▼
Next.js (React + TypeScript) en Vercel
  │ /api/* mediante rewrite/proxy de Vercel
  ▼
Spring Boot en OCI Compute, detrás de Nginx/HTTPS
  ├─ Oracle Autonomous Database (misma región/VCN cuando sea posible)
  └─ Proveedor de correo para enlace de acceso

Swing ──► servicios actuales ──► Oracle (interfaz independiente)
```

Vercel no ejecutará el backend Java. Su documentación enumera runtimes de funciones que no incluyen Java; Next.js sí tiene integración oficial en Vercel. Por eso, el frontend Next.js se despliega allí y Spring Boot corre como aplicación Java/Docker en OCI. ([Runtimes de funciones de Vercel](https://vercel.com/docs/functions/runtimes), [Next.js en Vercel](https://vercel.com/docs/frameworks/full-stack/nextjs))

### 2.1 Frontend

- **Next.js con React, TypeScript y App Router**, dentro de una carpeta `frontend/` del repositorio.
- Usarlo como interfaz web; la API de negocio y el acceso a Oracle permanecen en Spring Boot.
- Las pantallas iniciales serán solicitud de enlace, consumo del enlace, resumen de puntos e historial.
- El navegador llama rutas `/api/v1/...` del mismo origen. Vercel reescribe `/api/*` hacia el origen HTTPS del backend OCI. Así se evita una configuración de CORS entre dominios y se reduce la dependencia de cookies de terceros. Vercel documenta que los rewrites externos actúan como proxy inverso y conservan la URL visible. ([Rewrites externos de Vercel](https://vercel.com/docs/routing/rewrites))
- Los endpoints autenticados deben devolver `Cache-Control: no-store`; no configurar caché CDN para saldo, historial, autenticación ni respuestas personales.
- Si se decide no usar proxy/rewrite, configurar CORS en Spring para el origen exacto de Vercel y usar cookies/credenciales con CSRF. No usar `Access-Control-Allow-Origin: *` con credenciales.

### 2.2 API y base de datos

- Spring Boot 3.x, Java 17, Spring Web, Spring Security, Spring JDBC y Spring Validation.
- OCI Compute ejecuta la API en un contenedor Docker o como JAR bajo un servicio supervisado. Nginx termina TLS y reenvía localmente a Spring Boot.
- Solo Nginx expone HTTP/HTTPS. El puerto de Spring Boot (por ejemplo 8080) escucha en loopback o en red privada y no se abre a Internet.
- La Autonomous Database existente sigue siendo la fuente de verdad; OCI Compute alojará la API, no otra base de datos. El backend se conectará a esa instancia usando su wallet/TLS y credenciales guardadas fuera del repositorio e imagen.
- La API no acepta `idCliente` en rutas o cuerpos para consultar puntos. Obtiene el cliente autenticado desde la sesión.

### 2.3 Red OCI según la configuración actual de Autonomous Database

La base de datos ya existe y contiene la información del sistema. Antes de crear la VM se inspeccionan en OCI la región y el endpoint de esa instancia. No crear una nueva Autonomous Database durante el despliegue de la API.

- **ADB con endpoint privado:** crear Compute en la misma región y VCN con reglas NSG/route adecuadas. La base privada solo admite conexiones desde su red/VCN configurada. ([Oracle: endpoints privados de Autonomous Database](https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/private-endpoints-autonomous.html))
- **ADB con endpoint público protegido por ACL:** asignar a la VM una IP pública reservada/estable y autorizar esa IP en la ACL de la base. La ACL debe permitir el backend, no los navegadores ni Vercel.
- La VM necesita una ruta pública para servir la API, una IP pública reservada y reglas de entrada limitadas: TCP 443 (y 80 solo si se requiere redirección/validación TLS) desde clientes; TCP 22 solo desde la IP administrativa del equipo. El puerto de la base se permite en salida solo según la conexión Oracle configurada.
- La IP reservada se usa también en DNS para un subdominio opcional como `api.tudominio.com`. El dominio no es requisito para iniciar; puede usarse la URL pública temporal de OCI y después reemplazarla en la configuración de Vercel.
- No publicar el wallet, credenciales, OCID, claves SSH privadas ni archivos `.env`.

## 3. Identidad y contrato inicial de API

Se propone autenticación por enlace temporal enviado a un correo verificado. El token irá en el fragmento del enlace (`#token=...`), que el navegador no envía automáticamente al servidor; la página de aterrizaje lo manda una sola vez en un `POST`. Esto reduce exposición en registros y evita que la previsualización de correo consuma un enlace con un `GET`.

| Método y ruta | Acceso | Uso |
|---|---|---|
| `POST /api/v1/auth/access-requests` | Público, con límites | Solicitar enlace al correo. Respuesta genérica para no confirmar si existe una cuenta. |
| `POST /api/v1/auth/consume` | Token temporal | Consumir token de un solo uso y crear sesión segura. |
| `POST /api/v1/auth/logout` | Sesión | Cerrar e invalidar la sesión. |
| `GET /api/v1/me` | Sesión | Datos mínimos del cliente autenticado. |
| `GET /api/v1/me/points` | Sesión | Saldo actual. |
| `GET /api/v1/me/points/movements?limit=20&before=…` | Sesión | Historial paginado del cliente autenticado. |

No exponer operaciones de cajero ni canje en esta versión. Los errores de autenticación no revelan si un correo existe; nunca devolver stack traces, tokens ni información personal innecesaria.

### 3.1 Almacenamiento de acceso

Crear una migración Oracle para una tabla como `cliente_token_acceso` con FK a cliente, hash único del token, expiración, fecha de uso y fecha de creación. Guardar solo el hash; consumir el token transaccionalmente y una sola vez. Expiración corta (por ejemplo 10–15 minutos), límites por IP/destino y limpieza de vencidos. Añadir forma fiable de marcar el correo como verificado; no considerar verificados automáticamente los correos existentes.

Spring Security manejará la sesión con cookie `HttpOnly`, `Secure` en producción y `SameSite=Lax`. Activar protección CSRF, validar origen en operaciones con sesión y no guardar credenciales o tokens en `localStorage`. El token de enlace no se registra y el fragmento se limpia tras consumirlo.

## 4. Fases de implementación

| Fase | Trabajo | Salida esperada | Esfuerzo estimado | Estado |
|---|---|---|:---:|:---:|
| **0. Datos y acceso** | Confirmar correos utilizables, definir proceso de verificación, proveedor de correo y tratamiento de clientes sin email. Inspeccionar la Autonomous Database existente: región, endpoint y ACL/VCN. | Flujo de identidad aprobado y conectividad objetivo determinada. Bloqueante para publicar a clientes. | 2–4 h | **Completada** |
| **1. Diseño y migración aditiva** | Especificar tabla de tokens, verificación de correo, expiración, sesión, límites, privilegios y secretos. Respaldar/validar el esquema actual y preparar solo cambios aditivos compatibles; no ejecutar DDL inicial ni datos semilla sobre la base existente. | Migración revisable y reversible en entorno de prueba, sin reemplazar ni borrar los datos actuales. | 3–5 h | **Completada** |
| **2. API Spring Boot local** | Añadir starters/BOM sin reemplazar el parent Maven ni romper Shade; crear `ApiApplication`, configuración Oracle/Hikari, health y manejo de errores. Separar el arranque de Swing. | API inicia localmente; Swing sigue usando `Main` y su JAR. | 6–10 h | **Completada** |
| **3. Autenticación** | Implementar solicitud, envío, consumo de token, sesión/logout, CSRF, límites y mensajes anti-enumeración. | Solo correos verificados inician sesión; token expira y no se reutiliza. | 10–16 h | **Completada** |
| **4. API de puntos** | Implementar `/me`, saldo y movimientos paginados; vincular principal Spring Security con `id_cliente`. | Imposible consultar a otro cliente cambiando ID/parámetros. | 4–6 h | Pendiente |
| **5. Portal Next.js** | Crear `frontend/` con Next.js App Router + React + TypeScript; vistas adaptables y llamadas `/api/v1/...`; estados de espera, error y sesión cerrada. | Portal funcional en desarrollo consumiendo API local. | 10–16 h | Pendiente |
| **6. Preparar OCI** | Inspeccionar endpoint/VCN de ADB; crear VM Linux en región adecuada, configurar IP reservada, SSH, NSG, rutas y acceso DB; crear usuario Oracle con privilegios mínimos. | VM administrable por SSH y con camino de red hacia ADB, sin abrir la base al público. | 4–8 h | Pendiente |
| **7. Publicar backend OCI** | Construir imagen/JAR, transferir/desplegar, configurar secretos/wallet, servicio supervisado, Nginx, TLS, DNS API opcional y health check. | API disponible por HTTPS; Spring 8080 no está expuesto; consulta ADB comprobada. | 6–10 h | Pendiente |
| **8. Publicar frontend Vercel** | Importar repositorio Git; configurar raíz `frontend/`, framework Next.js, Node compatible (mínimo documentado actualmente 20.9), variables por entorno y rewrite `/api/*` al backend OCI. | Vercel genera URL de vista previa y producción; llamadas autenticadas llegan a OCI. | 3–5 h | Pendiente |
| **9. Seguridad y aceptación** | Validar correo desconocido, token expirado/reutilizado, CSRF, aislamiento entre clientes, cookies, no-cache, rate limits, acceso de red, logs y compatibilidad Swing. | Criterios de aceptación completos y URL lista para compartir. | 6–10 h | Pendiente |

### Evidencia y entregables de las Fases 0, 1, 2 y 3 (Completadas):
- **Fase 0 (Datos y acceso):** Flujo de identidad y acceso acordado. Las 5 decisiones de la fase quedaron resueltas (correos reales de prueba, verificación manual para demo, proveedor Gmail con app password, tratamiento sin correo y endpoint OCI público con mTLS).
- **Fase 1 (Diseño y migración aditiva):**
  - Migración UP: [`sql/auth_tokens_up.sql`](sql/auth_tokens_up.sql).
  - Migración DOWN: [`sql/auth_tokens_down.sql`](sql/auth_tokens_down.sql).
  - Verificación PL/SQL: [`sql/auth_tokens_verificar.sql`](sql/auth_tokens_verificar.sql).
  - Pruebas de compatibilidad: [`ClienteTokenAccesoCompatibilidadTest.java`](src/test/java/com/panpuntospremios/dao/ClienteTokenAccesoCompatibilidadTest.java).
- **Fase 2 (API Spring Boot local):**
  - **Dependencias Maven:** Integración limpia mediante `dependencyManagement` (`spring-boot-dependencies:3.3.4`), starters de Web, Validation, Actuator, JDBC y Test sin alterar parent Maven ni romper `maven-shade-plugin`.
  - **Coexistencia y perfiles de ejecución:** Swing mantiene su ejecución por defecto con `com.panpuntospremios.Main` (`mvn compile exec:java`) y su JAR sombreado (`java -jar target/pan-puntos-premios-1.0-SNAPSHOT.jar`). La API cuenta con su perfil independiente (`mvn compile exec:java -Pbackend`).
  - **Arranque independiente:** [`ApiApplication.java`](src/main/java/com/panpuntospremios/api/ApiApplication.java) carga variables de entorno desde `.env` local en propiedades del sistema e inicializa Spring Boot en puerto 8080.
  - **Configuración de base de datos y pool acotado:** [`application.properties`](src/main/resources/application.properties) configura HikariCP con `maximum-pool-size=5`, `minimum-idle=1`, timeouts acotados y compatibilidad transparente con Oracle y H2.
  - **Endpoint de salud:** [`HealthController.java`](src/main/java/com/panpuntospremios/api/controller/HealthController.java) expone `GET /api/v1/health` retornando estado del servicio y validando conectividad a la base de datos sin filtrar secretos.
  - **Manejo global de errores:** [`GlobalExceptionHandler.java`](src/main/java/com/panpuntospremios/api/excepcion/GlobalExceptionHandler.java) y [`ErrorRespuestaDTO.java`](src/main/java/com/panpuntospremios/api/dto/ErrorRespuestaDTO.java) capturan excepciones del negocio (`ValidacionException`, `StockInsuficienteException`, `PuntosInsuficientesException`, `PagoRechazadoException`), validaciones de request y `SQLException` (ocultando detalles sensibles).
  - **Pruebas automatizadas:** [`ApiApplicationTest.java`](src/test/java/com/panpuntospremios/api/ApiApplicationTest.java) verifica carga del contexto, configuración del pool Hikari, endpoint de salud y respuestas de error.
- **Fase 3 (Autenticación):**
  - **Seguridad y sesiones:** [`SecurityConfig.java`](src/main/java/com/panpuntospremios/api/config/SecurityConfig.java) configura Spring Security con sesiones HTTP (`PANPUNTOS_SESSION`), cookie `HttpOnly`/`SameSite=Lax`/`Secure` condicional, protección CSRF activa con `CookieCsrfTokenRepository` (eximiendo únicamente `/access-requests` y `/consume`), rutas protegidas (`/logout`, `/me/**`) y respuestas 401/403 en JSON sin redirecciones.
  - **Controlador REST:** [`AuthController.java`](src/main/java/com/panpuntospremios/api/controller/AuthController.java) expone `POST /api/v1/auth/access-requests`, `POST /api/v1/auth/consume` y `POST /api/v1/auth/logout`.
  - **Lógica de negocio y tokens:** [`AuthService.java`](src/main/java/com/panpuntospremios/api/servicio/AuthService.java) gestiona generación de tokens con `SecureRandom` (32 bytes), hash SHA-256, expiración a 15 min, mitigación de enumeración (respuesta genérica idéntica) y consumo único atómico.
  - **Control de abusos y Rate Limiting:** Doble capa de protección mediante [`IpRateLimiter.java`](src/main/java/com/panpuntospremios/api/servicio/IpRateLimiter.java) (límite de 10 peticiones cada 15 min por IP arrojando [`LimitePeticionesException`](src/main/java/com/panpuntospremios/api/excepcion/LimitePeticionesException.java) / HTTP 429) y cooldown en BD por cliente identificado (60s cooldown, máx 3 por 10 min).
  - **Servicio de correo seguro:** [`EmailService.java`](src/main/java/com/panpuntospremios/api/servicio/EmailService.java) envía el enlace con fragmento URL (`#token=...`) vía JavaMail/Gmail sin filtrar el token crudo en logs del servidor (auditoría neutral en INFO y enlace confinado exclusivamente a DEBUG).
  - **Configuración proxy y cabeceras:** [`application.properties`](src/main/resources/application.properties) incluye `server.forward-headers-strategy=framework` para soporte detrás de proxies inversos TLS (como Nginx en OCI) y flags de cookie seguros.
  - **Persistencia atómica:** [`AuthDAOImpl.java`](src/main/java/com/panpuntospremios/api/dao/AuthDAOImpl.java) realiza operaciones de consulta, conteo de cuotas y consumo univalente en `cliente_token_acceso` con cierre estricto de conexiones.
  - **DTOs y modelo:** [`SolicitudAccesoDTO.java`](src/main/java/com/panpuntospremios/api/dto/SolicitudAccesoDTO.java), [`ConsumirTokenDTO.java`](src/main/java/com/panpuntospremios/api/dto/ConsumirTokenDTO.java), [`AuthMensajeDTO.java`](src/main/java/com/panpuntospremios/api/dto/AuthMensajeDTO.java), [`ClienteAuth.java`](src/main/java/com/panpuntospremios/api/modelo/ClienteAuth.java) y [`ClientePrincipal.java`](src/main/java/com/panpuntospremios/api/modelo/ClientePrincipal.java).
  - **Pruebas de integración:** [`AuthControllerTest.java`](src/test/java/com/panpuntospremios/api/AuthControllerTest.java) con 10 casos de prueba que cubren solicitud válida, anti-enumeración por correo inexistente/no-verificado, formato inválido, consumo único, rechazo de reúso, tokens vencidos, protección CSRF y sesión en logout, cooldown por cliente y bloqueo HTTP 429 por exceso de IP. Suite completa del proyecto: 83 pruebas ejecutadas, 0 fallos (`BUILD SUCCESS`). Embalaje con Shade validado exitosamente.


### Estimación global

- **Implementación funcional completa (fases 0–9): 54–90 horas-persona.** Para planificación, reservar **60–110 horas-persona** incluyendo margen de integración y correcciones.
- **Solo el backend/API (fases 0–4): 25–41 horas-persona** según las estimaciones por fase; no incluye construir ni publicar el portal.
- Para una persona que dedique **8–12 horas semanales**, el proyecto completo representa aproximadamente **5–14 semanas calendario**. Los tiempos de aprobación de correo, verificación/actualización manual de emails, capacidad de OCI, DNS y propagación de certificados pueden alargar el calendario sin aumentar las horas de programación.
- Supuestos: una persona con conocimientos básicos/intermedios de Java, SQL, React/Next.js y Git; la Autonomous Database está activa y accesible; no se rediseña la lógica de puntos; se implementa un portal pequeño y se hace validación funcional/seguridad básica.
- La incertidumbre principal está en el correo verificado, el acceso de red de la base existente, las transacciones JDBC actuales y la experiencia del equipo con Next.js/OCI. Si alguno requiere cambios inesperados, reestimar después de las fases 0–2.

La documentación actual de Next.js requiere Node.js 20.9 o posterior; fijar una versión LTS compatible en Vercel y desarrollo local. ([Requisitos Next.js](https://nextjs.org/docs/app/getting-started/installation))

## 5. Guía de publicación OCI Compute (fase 6–7)

1. En la consola OCI seleccionar la misma región de ADB; inspeccionar **Autonomous AI Database → Network** para determinar si el endpoint es privado o público/ACL.
2. En **Compute → Instances → Create instance**, seleccionar compartimento, Linux soportado, una shape pequeña compatible con el presupuesto/capacidad disponible y la VCN/subred acordes con el endpoint de la base existente. Esta instancia alojará la API; no crear una instancia de base de datos. Guardar la clave privada SSH al crearla; OCI no la vuelve a mostrar. ([Crear una instancia OCI](https://docs.oracle.com/en-us/iaas/Content/Compute/Tasks/launchinginstance.htm))
3. Asignar IP pública reservada si la API será accesible directamente desde Internet. Configurar NSG: SSH desde IP administrativa conocida; 443 público; 80 solo para redirigir a HTTPS/validación de certificado. No abrir 8080 ni la base de datos a Internet.
4. Instalar actualizaciones, Docker (o Java 17 si se ejecuta JAR), Nginx y certificado TLS. Crear un servicio de arranque/reinicio para la API. Limitar memoria de JVM según la VM.
5. Guardar el wallet correspondiente a la Autonomous Database existente, contraseña, proveedor de correo y secretos en el mecanismo de secretos disponible o archivos de permisos restringidos fuera del repo y de la imagen. Configurar el datasource y HikariCP; la cuenta Oracle de la API no será `ADMIN` ni las credenciales personales de Swing y tendrá permisos mínimos.
6. Para ADB público, permitir en ACL únicamente la IP reservada de la VM. Para ADB privado, verificar ruta, VCN y reglas NSG desde la VM. Probar conexión TLS sin publicar wallet ni credenciales.
7. Configurar Nginx para recibir HTTPS y reenviar a `127.0.0.1:8080`. No devolver información de `/actuator` excepto health mínimo protegido o público de forma controlada.
8. Probar `https://api.tudominio.com/actuator/health` o endpoint de health dedicado y una llamada autenticada de prueba antes de conectar Vercel.

OCI exige que una instancia pública tenga subnet pública, IP pública, Internet Gateway y rutas/reglas apropiadas; una IP reservada persiste y puede usarse para DNS/ACL. ([IP públicas OCI](https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/managingpublicIPs.htm))

## 6. Guía de publicación Vercel + Next.js (fase 8)

1. Crear `frontend/` con Next.js, React, TypeScript y App Router; incluir scripts `dev`, `build` y `start`, y documentar Node compatible.
2. Subir el frontend al repositorio Git y conectar ese repositorio en Vercel. Si el repo también contiene Maven, configurar **Root Directory = `frontend`** para que Vercel construya solo el Next.js.
3. En Vercel elegir Next.js (detección automática), confirmar comando `npm run build` y Node LTS compatible.
4. Configurar variables separadas para Preview y Production. Definir el origen API no secreto (por ejemplo `SPRING_API_ORIGIN=https://api.tudominio.com`) y configurar el rewrite externo: el navegador solicita `/api/:path*` y Vercel lo envía a `${SPRING_API_ORIGIN}/api/:path*` (por ejemplo, `/api/v1/me` llega al backend como `/api/v1/me`). No usar prefijo `NEXT_PUBLIC_` para secretos; la URL de API puede ser pública, pero credenciales de Oracle/correo nunca deben estar en Vercel.
5. En la API, reconocer únicamente las cabeceras de proxy confiables necesarias para HTTPS y validar CSRF/Origin contra el dominio del portal. No confiar en `X-Forwarded-*` enviado directamente por cualquier cliente si llega al origen sin proxy confiable.
6. Crear deployment Preview, probar el flujo completo y confirmar que cookies de sesión y encabezados `Set-Cookie` atraviesen el rewrite como se espera. Verificar que respuestas `/api/v1/me/**` no se almacenen en caché.
7. Promover a Production; compartir la URL `*.vercel.app`. Opcionalmente agregar un dominio propio, por ejemplo `puntos.tudominio.com`, en Vercel y configurar DNS. Un subdominio `api` no es indispensable cuando se usa el rewrite, pero el origen público de OCI debe existir para el destino del proxy.

Vercel soporta despliegues Preview y Production, variables por entorno y dominios propios; cambios en variables se aplican a los nuevos despliegues. ([Desplegar desde CLI](https://vercel.com/docs/projects/deploy-from-cli), [variables Vercel](https://vercel.com/docs/environment-variables))

## 7. Maven, conexiones y coexistencia Swing/API

### 7.1 Comandos de ejecución local

Para que cualquier integrante del equipo o revisor pueda ejecutar ambas aplicaciones sin confusiones:

| Objetivo | Comando | ¿Qué hace? |
|---|---|---|
| **Abrir aplicación de escritorio (Swing)** | `mvn compile exec:java` | Arranca la ventana gráfica de escritorio tradicional (`com.panpuntospremios.Main`). |
| **Iniciar servidor web (API Spring Boot)** | `mvn compile exec:java -Pbackend` | Arranca el backend REST en `http://localhost:8080` (`com.panpuntospremios.api.ApiApplication`). |
| **Verificar salud en el navegador** | Abrir `http://localhost:8080/api/v1/health` | Muestra estado `UP` del servicio y de la base de datos en JSON. |
| **Ejecutar JAR de escritorio empaquetado** | `java -jar target/pan-puntos-premios-1.0-SNAPSHOT.jar` | Corre el JAR sombreado generado por `maven-shade-plugin`. |
| **Ejecutar toda la suite de pruebas** | `mvn test` | Corre las 73 pruebas automatizadas de escritorio y API. |

### 7.2 Reglas de coexistencia y base de datos

- Importar BOM Spring Boot desde `dependencyManagement`; no cambiar el parent Maven existente sin necesidad.
- Preservar `maven-shade-plugin` y la clase principal `com.panpuntospremios.Main`. No vincular `spring-boot:repackage` al ciclo `package` compartido hasta separar perfiles de empaquetado.
- Iniciar `ApiApplication` con el perfil `backend` (`-Pbackend`) o desde el IDE ejecutando su método `main`.
- HikariCP proporciona conexiones acotadas (`maximum-pool-size=5`). Los DAO actuales reciben `Connection` por constructor y algunos servicios controlan transacciones: no compartir una conexión singleton entre solicitudes. Delimitar cada operación y devolver conexiones con autoCommit correcto, rollback y `try-with-resources`.
- No reimplementar reglas de puntos en controladores. Reutilizar servicios existentes cuando su transacción y cierre sean adecuados; añadir método explícito y seguro de saldo si hace falta.
- No conservar pool/URL/wallet/contraseñas dentro de la imagen Docker ni en Git.

## 8. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Consultar puntos con un ID o identificador fácil de adivinar | API `/me` vinculada a una sesión con correo verificado. |
| Correos existentes sin confirmar o compartidos | No auto-verificarlos; procedimiento de verificación y atención para clientes sin correo. |
| Base Oracle expuesta a la web | Solo el backend OCI se conecta; ACL con IP backend o endpoint privado/VCN. |
| Búsqueda abusiva de enlaces | Respuesta genérica, rate limits por IP/destino, enfriamiento y monitoreo. |
| Exposición de secreto de un solo uso | Hash en DB, token corto y de un uso, fragmento de URL, POST de consumo, no logs. |
| Cookie bloqueada o sesión inconsistente entre Vercel/OCI | Proxy Vercel same-origin recomendado; verificar `Set-Cookie`, CSRF, Secure/HttpOnly/SameSite y `no-store` en Preview y Production. |
| API protegida solo por CORS | CORS no es autenticación; cada endpoint valida sesión y autorización aunque se use proxy. |
| Pool filtrado o transacción incompleta | HikariCP limitado; cierre/rollback explícito y revisión del ciclo de vida JDBC. |
| Cambios de API rompen Swing o su JAR | Entradas y empaquetado separados; conservar `Main` y ejecutar validación de compatibilidad. |
| Costos o falta de capacidad Always Free | Revisar estimador, región, shape disponible y consumo antes de crear recursos; no asumir capacidad gratuita garantizada. |

## 9. Criterios de aceptación

- Swing se ejecuta como antes y no comparte el arranque del servidor.
- Cliente con correo verificado puede solicitar y consumir un enlace válido; correo no registrado recibe la misma respuesta pública.
- Token vencido o usado se rechaza; logout invalida la sesión.
- Saldo e historial pertenecen siempre al principal autenticado; no es posible cambiar de cliente modificando URL/cuerpo.
- Next.js en Vercel sirve el portal y proxifica `/api/*` a Spring en OCI con HTTPS y sin cachear datos privados.
- OCI puede consultar Oracle sin exponer wallet ni permitir conexiones desde navegador/Vercel directamente a la base.
- El despliegue se conecta a la Autonomous Database existente y conserva sus datos; cualquier DDL de autenticación se aplica como migración aditiva tras respaldo y validación.
- Solo 22 administrativo restringido, 443 público y 80 opcional están abiertos; 8080 y puerto Oracle no tienen ingress público.
- Sin secretos/tokens/datos personales en Git, imágenes, navegador persistente o logs.
- Las instrucciones de desarrollo, despliegue, dominios y secretos están documentadas.

## 10. Referencias oficiales

- [Next.js App Router: instalación y requisitos](https://nextjs.org/docs/app/getting-started/installation)
- [Next.js en Vercel](https://vercel.com/docs/frameworks/full-stack/nextjs)
- [Vercel: rewrites externos](https://vercel.com/docs/routing/rewrites)
- [Vercel: variables de entorno](https://vercel.com/docs/environment-variables)
- [OCI: crear Compute](https://docs.oracle.com/en-us/iaas/Content/Compute/Tasks/launchinginstance.htm)
- [OCI: endpoints privados Autonomous Database](https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/private-endpoints-autonomous.html)
- [OCI: direcciones IP públicas](https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/managingpublicIPs.htm)
- [Spring Security: One-Time Token Login](https://docs.spring.io/spring-security/reference/servlet/authentication/onetimetoken.html)
