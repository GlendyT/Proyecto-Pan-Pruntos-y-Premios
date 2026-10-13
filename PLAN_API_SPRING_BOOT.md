# Plan de API Spring Boot y portal de puntos

**Proyecto:** Pan, Puntos y Premios
**Fecha:** octubre de 2026
**Estado:** fases 0–8 completadas para el despliegue acordado: API Spring Boot en OCI, portal Next.js en Vercel y flujo de correo/enlace de acceso confirmado por el usuario. La Fase 9 está documentada como aceptación de aplicación; los cambios de infraestructura están aplazados por decisión del usuario. Ver [`docs/FASE9_ACEPTACION_SEGURIDAD.md`](docs/FASE9_ACEPTACION_SEGURIDAD.md).
**Arquitectura acordada:** Next.js en Vercel, API Spring Boot en Oracle Cloud Infrastructure (OCI), conexión a la Oracle Autonomous Database existente.

---

## 1. Objetivo y conclusión de viabilidad

Agregar una página web para que cada cliente consulte su saldo e historial de puntos de forma segura. La página consumirá una API REST en Spring Boot. La aplicación Swing y su entrega académica permanecen independientes.

El proyecto usa Java 17, Maven, Oracle, DAOs y servicios de negocio. El esquema incluye `cliente.saldo_puntos`, `movimiento_puntos` y `cliente.email`. La API, autenticación por correo, consulta personal de puntos y portal Next.js están implementados y desplegados; el usuario confirmó el flujo de enlace, sesión y consulta. La base actual se conserva: no crear otra, no reemplazarla ni volver a cargar el DDL/datos semilla sobre producción.

El alcance anterior de panel de cajero/administrador sin autenticación queda descartado. El sitio no permitirá administrar clientes, catálogo, inventario, pedidos, ventas ni canjes.

La viabilidad depende de identificar al cliente con seguridad. El identificador de cliente y el ID de Oracle no son credenciales. El usuario confirma que todos los clientes tienen correo. Como decisión de Fase 0, el primer enlace enviado al correo ya registrado funcionará como verificación inicial: al consumirlo, la API marcará el correo como verificado y abrirá la sesión; no se confiará solo en que exista un valor de correo en la base. Los enlaces de solicitudes posteriores siguen siendo de un solo uso. Una dirección compartida o desactualizada requiere atención manual antes de habilitar esa cuenta.

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
- **ADB con endpoint público protegido por ACL:** asignar a la VM una IP pública reservada/estable y autorizarla en la ACL de la base. La ACL no debe permitir navegadores ni Vercel. Como Swing actualmente puede conectarse directamente a Oracle, inventariar sus equipos de uso y conservar solo los orígenes autorizados necesarios o migrar Swing a una ruta de red controlada; no restringir a la VM sin evaluar ese impacto.
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
| **0. Datos y acceso** | Confirmar que cada cliente tiene un correo, definir cómo comprobar que controla ese buzón y elegir proveedor transaccional. Inspeccionar en OCI la región, endpoint, tipo de acceso, ACL/VCN y TLS/mTLS de la Autonomous Database existente. | Flujo de identidad aprobado y conectividad objetivo determinada. Bloqueante para publicar a clientes. | 2–4 h | **Completada.** Cada cliente debe tener correo; el primer enlace verifica el buzón. EmailJS/Gmail personal quedó configurado y probado en el backend; OCI Email Delivery/SMTP fue investigado pero no quedó como proveedor activo. ADB pública en `mx-queretaro-1`, ACL actualmente deshabilitada y mTLS requerido; evaluar endurecimiento ACL en fase 9. |
| **1. Diseño y migración aditiva** | Prevalidar el esquema con consulta de solo lectura, confirmar conexión como propietario del esquema `CLIENTE`, comprobar objetos/columnas previos y definir un respaldo/exportación acorde al nivel de servicio ADB. Después aplicar únicamente el script UP aditivo, verificarlo y documentar privilegios mínimos de `app_puntos_api`. | Migración aplicada y verificada en la ADB existente; clientes y movimientos conservados. | 3–5 h | **Completada en ADB**: el usuario confirma que el verificador terminó y ya aparecen tablas, columnas, restricciones e índices. |
| **2. API Spring Boot local** | Añadir starters/BOM sin reemplazar el parent Maven ni romper Shade; crear `ApiApplication`, configuración Oracle/Hikari, health y manejo de errores. Separar el arranque de Swing. | API inicia localmente; Swing sigue usando `Main` y su JAR. | 6–10 h | **Completada (PR 17)** |
| **3. Autenticación** | Implementar solicitud, envío, consumo de token, sesión/logout, CSRF, límites y mensajes anti-enumeración. | Solo correos controlados inician sesión; primer enlace verifica el correo, expira y no se reutiliza. | 10–16 h | **Desplegada y confirmada por el usuario:** recibió el correo de producción; el enlace abre Vercel e inicia sesión. La revisión de controles restantes está registrada en la fase 9. |
| **4. API de puntos** | Implementar `/me`, saldo y movimientos paginados; vincular principal Spring Security con `id_cliente`. | Imposible consultar a otro cliente cambiando ID/parámetros. | 4–6 h | **Desplegada y validada por el usuario desde el portal**; consulta el saldo personal en ADB. |
| **5. Portal Next.js** | Crear `frontend/` con Next.js App Router + React + TypeScript; vistas adaptables y llamadas `/api/v1/...`; estados de espera, error y sesión cerrada. | Portal funcional publicado en Vercel consumiendo la API de OCI. | 10–16 h | **Implementado y desplegado**; el usuario confirmó el enlace por correo, callback, sesión y acceso al portal publicado. |
| **6. Preparar OCI** | Inspeccionar endpoint/VCN de ADB; crear VM Linux en región adecuada, configurar IP reservada, SSH, NSG, rutas y acceso DB; validar el usuario Oracle `APP_PUNTOS_API` con privilegios mínimos y conexión wallet/mTLS desde la VM. | VM administrable por SSH y con camino de red hacia ADB; JDBC probado con identidad de servicio. | 4–8 h | **Completada operativamente.** VM `pan-puntos-api` en `mx-queretaro-1`, Oracle Linux 9 AArch64, `VM.Standard.A1.Flex` (1 OCPU/6 GB). IP reservada `163.192.144.19`; servicio conectado a ADB con `APP_PUNTOS_API`, health `UP`. El usuario decidió aplazar cambios de SSH/ACL para mantener la VM estable; véase [`docs/FASE9_ACEPTACION_SEGURIDAD.md`](docs/FASE9_ACEPTACION_SEGURIDAD.md). Grants y verificación: [`sql/app_puntos_api_grants.sql`](sql/app_puntos_api_grants.sql), [`sql/app_puntos_api_verificar.sql`](sql/app_puntos_api_verificar.sql). |
| **7. Publicar backend OCI** | Construir JAR, desplegar como servicio supervisado, configurar secretos/wallet, Nginx, TLS y health check. | API disponible por HTTPS; Spring 8080 no expuesto; consulta ADB comprobada. | 6–10 h | **Completada y verificada.** Servicio `pan-puntos-api`; Nginx proxy a `127.0.0.1:8080`; HTTPS por DuckDNS con certificado Let’s Encrypt y renovación programada; health `UP` con ADB. `APP_PORTAL_BASE_URL` se actualizó al dominio Vercel y el usuario confirmó el flujo de correo. |
| **8. Publicar frontend Vercel** | Importar repositorio Git; configurar raíz `frontend/`, framework Next.js, Node compatible (mínimo documentado actualmente 20.9), pnpm 10.19.0, `SPRING_API_ORIGIN=https://panpuntospremios.duckdns.org` y rewrite `/api/*` al backend OCI. Después fijar `APP_PORTAL_BASE_URL` en OCI al URL Vercel. | Vercel genera URL de producción; llamadas autenticadas llegan a OCI; enlaces de correo abren ese portal. | 3–5 h | **Completada y validada por el usuario:** portal publicado, `APP_PORTAL_BASE_URL` corregida en OCI y enlace recibido por correo abre el portal e inicia sesión. Guía: [`docs/GUIA_DESPLIEGUE_OCI_VERCEL.md`](docs/GUIA_DESPLIEGUE_OCI_VERCEL.md). |
| **9. Seguridad y aceptación** | Revisar controles de aplicación, sesiones, CSRF, aislamiento, no-cache, límites, logs y compatibilidad Swing. Los cambios SSH/ACL/red quedan fuera mientras el usuario prefiera mantener estable la VM. | Aceptación documentada y riesgos de infraestructura aplazados con claridad. | 6–10 h | **Documentada; no implica cambios en OCI.** El usuario prefiere no tocar la VM. Lista de verificación: [`docs/FASE9_ACEPTACION_SEGURIDAD.md`](docs/FASE9_ACEPTACION_SEGURIDAD.md). |

### Evidencia y entregables y límites de validación
- **Fase 0 (Datos y acceso):** El usuario confirma que todos los clientes tienen correo. La red reportada es endpoint público, ACL deshabilitada y mTLS requerido; `qVQL:MX-QUERETARO-1-AD-1` corresponde a la región `mx-queretaro-1` (Mexico Central/Querétaro). OCI Email Delivery/SMTP se investigó; por el bloqueo SMTP desde la PC y la configuración de remitente pendiente, el proveedor activo elegido fue **EmailJS con Gmail personal**, probado con recepción real desde el backend. La verificación inicial usa el primer enlace enviado al correo registrado; consumirlo verifica buzón y abre la sesión. Cuentas compartidas o con dirección desactualizada requieren revisión manual. No se ha cambiado la red de ADB.
- **Fase 1 (Diseño y migración aditiva):**
  - Prevalidación de solo lectura: [`sql/auth_tokens_precheck.sql`](sql/auth_tokens_precheck.sql). Ejecutar primero desde Database Actions conectado como propietario de `CLIENTE`; confirma usuario/esquema, objetos/columnas esperados y conteos de correos sin mostrar PII.
  - Migración UP: [`sql/auth_tokens_up.sql`](sql/auth_tokens_up.sql).
  - Para ejecutar UP desde SQLcl y detenerse en el primer error: [`sql/auth_tokens_up_sqlcl.sql`](sql/auth_tokens_up_sqlcl.sql).
  - Migración DOWN: [`sql/auth_tokens_down.sql`](sql/auth_tokens_down.sql).
  - Verificación PL/SQL: [`sql/auth_tokens_verificar.sql`](sql/auth_tokens_verificar.sql).
  - Pruebas de compatibilidad: [`ClienteTokenAccesoCompatibilidadTest.java`](src/test/java/com/panpuntospremios/dao/ClienteTokenAccesoCompatibilidadTest.java).
  - La migración añade estado `N` por defecto y tabla de tokens; no carga DDL inicial, semilla ni reescribe saldo/movimientos. Oracle confirma automáticamente cada DDL, por lo que UP no es una transacción que pueda revertirse con `ROLLBACK`. DOWN elimina los objetos/columnas de autenticación y no debe usarse tras empezar a depender de ellos sin evaluar sus datos.
  - Si ADB es Always Free, Oracle no ofrece respaldos manuales ni restauración a un punto en el tiempo para esa modalidad; verificar el nivel/funciones disponibles en la consola. Antes de UP, usar un entorno de prueba si está disponible y acordar exportación lógica/recuperación de los objetos afectados. La migración no altera filas existentes de saldo o movimientos.
  - Ejecutar `auth_tokens_verificar.sql` solo después de que UP termine correctamente. El usuario de la API no será `ADMIN`: `auth_tokens_up.sql` documenta `SELECT` sobre `cliente`, `UPDATE` limitado a las columnas de verificación, CRUD necesario sobre tokens y `SELECT` sobre `movimiento_puntos`.
  - **Estado actual:** el usuario confirma que `auth_tokens_up.sql` quedó aplicado y que `auth_tokens_verificar.sql` validó las tablas, columnas, restricciones e índices. Esta verificación local/remota de esquema no demuestra todavía que la API pueda conectarse por JDBC con wallet/mTLS; esa comprobación corresponde a integración de Fases 4 y 6–7. No reejecutar UP.
- **Fase 3 (primer acceso):** La solicitud emite un enlace a cualquier cliente activo con correo registrado, aunque `email_verificado='N'`; consumirlo verifica el correo y crea la sesión en forma atómica. La respuesta pública es genérica y los tokens son de un solo uso con vigencia de 15 minutos. Se confirmó desde el entorno local conexión a ADB como `APP_PUNTOS_API`, envío exitoso vía EmailJS/Gmail y acceso usando el enlace. La Private Key EmailJS expuesta fue rotada por el responsable y las variables se guardaron en el `.env` de OCI; falta probar la entrega desde la VM. Se corrigió el cálculo de cooldown para usar UTC de Oracle y el callback conserva el token entre ciclos de efecto de React Strict Mode. Pendientes: endurecer confianza en proxy y completar validación pública.
- Para la validación manual de navegador/correo se agregó el perfil Maven `local-demo`: H2 aislado, esquema y cliente ficticio `cliente.demo@example.test` con estado inicial `N`, 125 puntos y movimientos demo. Al activar `local-demo`, `ApiApplication` ignora `.env`; su SMTP apunta a Mailpit local en `127.0.0.1:1025`. Se probó el arranque y `GET /api/v1/health` respondió `UP` con H2. Arranque API: `mvn '-Plocal-demo,backend' '-Dspring.profiles.active=local-demo' '-Dserver.port=8081' compile exec:java`. En el frontend, `SPRING_API_ORIGIN=http://localhost:8081` y `pnpm dev`; Mailpit muestra los mensajes en `http://localhost:8025`. Esto no usa ADB ni entrega correos externos.
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
  - **Servicio de correo seguro:** [`EmailService.java`](src/main/java/com/panpuntospremios/api/servicio/EmailService.java) envía el enlace con fragmento URL (`#token=...`) vía EmailJS REST cuando `EMAIL_PROVIDER=emailjs`; SMTP queda como alternativa. Variables EmailJS solo en el backend. La plantilla requiere `to_email`, `to_name`, `access_link` y `expiration_minutes`. El usuario confirmó correo recibido desde producción y enlace válido al portal.
  - **Configuración proxy y cabeceras:** [`application.properties`](src/main/resources/application.properties) incluye `server.forward-headers-strategy=framework` para soporte detrás de proxies inversos TLS (como Nginx en OCI) y flags de cookie seguros.
  - **Persistencia atómica:** [`AuthDAOImpl.java`](src/main/java/com/panpuntospremios/api/dao/AuthDAOImpl.java) realiza operaciones de consulta, conteo de cuotas y consumo univalente en `cliente_token_acceso` con cierre estricto de conexiones.
  - **DTOs y modelo:** [`SolicitudAccesoDTO.java`](src/main/java/com/panpuntospremios/api/dto/SolicitudAccesoDTO.java), [`ConsumirTokenDTO.java`](src/main/java/com/panpuntospremios/api/dto/ConsumirTokenDTO.java), [`AuthMensajeDTO.java`](src/main/java/com/panpuntospremios/api/dto/AuthMensajeDTO.java), [`ClienteAuth.java`](src/main/java/com/panpuntospremios/api/modelo/ClienteAuth.java) y [`ClientePrincipal.java`](src/main/java/com/panpuntospremios/api/modelo/ClientePrincipal.java).
  - **Pruebas de integración:** [`AuthControllerTest.java`](src/test/java/com/panpuntospremios/api/AuthControllerTest.java) con 10 casos de prueba que cubren solicitud válida, anti-enumeración por correo inexistente/no-verificado, formato inválido, consumo único, rechazo de reúso, tokens vencidos, protección CSRF y sesión en logout, cooldown por cliente y bloqueo HTTP 429 por exceso de IP. En la revisión actual, la suite completa del proyecto ejecutó 87 pruebas, 0 fallos (`BUILD SUCCESS`). Embalaje con Shade validado exitosamente.
  - **Endurecimiento pendiente/aplazado:** `AuthController` lee `X-Forwarded-For`; la verificación de confianza de proxy está listada en Fase 9. El usuario prefiere no modificar ahora la infraestructura.
- **Fase 4 (API de puntos):** [`PuntosController.java`](src/main/java/com/panpuntospremios/api/controller/PuntosController.java), [`PuntosConsultaService.java`](src/main/java/com/panpuntospremios/api/servicio/PuntosConsultaService.java) y DAO JDBC exponen perfil, saldo e historial paginado por cursor. La identidad se obtiene de la sesión; no se acepta ID de cliente del navegador; respuestas personales usan `no-store`. [`PuntosControllerTest.java`](src/test/java/com/panpuntospremios/api/PuntosControllerTest.java) cubre 401 anónimo, aislamiento, paginación, cursor/límite inválido y cliente inactivo. El usuario confirmó consulta de puntos desde el portal publicado.
- **Fase 5 (portal Next.js):** `frontend/` contiene Next.js App Router, React, TypeScript, solicitud/consumo de enlace, dashboard de puntos, historial paginado y logout. `pnpm-lock.yaml` fija dependencias; el proxy `/api/*` usa `SPRING_API_ORIGIN`. El portal está en producción en `https://pan-puntos-portal.vercel.app`; el usuario confirmó el flujo de correo, callback, sesión y consulta. Los detalles del despliegue están en [`docs/GUIA_DESPLIEGUE_OCI_VERCEL.md`](docs/GUIA_DESPLIEGUE_OCI_VERCEL.md).


### Estimación global

- **Implementación funcional completa (fases 0–9): 54–90 horas-persona.** Para planificación, reservar **60–110 horas-persona** incluyendo margen de integración y correcciones.
- **Esfuerzo restante:** la publicación y aceptación funcional inicial están completadas. La Fase 9 queda documentada; la aceptación restante de aplicación y cualquier endurecimiento de infraestructura se decidirán por separado. El usuario pidió no modificar la VM por ahora.
- **Estimación inicial solo del backend/API (fases 0–4): 25–41 horas-persona** según las estimaciones por fase; no incluye construir ni publicar el portal.
- Para una persona que dedique **8–12 horas semanales**, el proyecto completo representa aproximadamente **5–14 semanas calendario**. Los tiempos de aprobación de correo, verificación/actualización manual de emails, capacidad de OCI, DNS y propagación de certificados pueden alargar el calendario sin aumentar las horas de programación.
- Supuestos: una persona con conocimientos básicos/intermedios de Java, SQL, React/Next.js y Git; la Autonomous Database está activa y accesible; no se rediseña la lógica de puntos; se implementa un portal pequeño y se hace validación funcional/seguridad básica.
- Las fases 0–8 están completadas y el portal está publicado. Para el estado de Fase 9 y los cambios de infraestructura aplazados, consulta [`docs/FASE9_ACEPTACION_SEGURIDAD.md`](docs/FASE9_ACEPTACION_SEGURIDAD.md).

La documentación actual de Next.js requiere Node.js 20.9 o posterior; fijar una versión LTS compatible en Vercel y desarrollo local. ([Requisitos Next.js](https://nextjs.org/docs/app/getting-started/installation))

## 5. Registro de aprovisionamiento OCI Compute (fases 6–7)

Los pasos siguientes documentan cómo se preparó la VM y sirven para reconstrucción autorizada. No implican que debas modificar ahora el servidor que ya funciona. Para encenderlo, conectarte y revisar salud sin cambios, consulta la sección 2.2 de [`docs/GUIA_DESPLIEGUE_OCI_VERCEL.md`](docs/GUIA_DESPLIEGUE_OCI_VERCEL.md).

1. La consola ya reporta para ADB **endpoint público, ACL deshabilitada y mTLS requerido**; seleccionar la región de Querétaro (confirmar su nombre en el selector). Para producción, después de asignar una IP pública reservada a la VM, restringir la ACL de ADB a esa IP y a cualquier equipo Swing que deba conservar acceso directo. No dejar el endpoint accesible desde cualquier IP.
2. En **Compute → Instances → Create instance**, seleccionar compartimento, Linux soportado, una shape pequeña compatible con el presupuesto/capacidad disponible y la VCN/subred acordes con el endpoint de la base existente. Esta instancia alojará la API; no crear una instancia de base de datos. Guardar la clave privada SSH al crearla; OCI no la vuelve a mostrar. ([Crear una instancia OCI](https://docs.oracle.com/en-us/iaas/Content/Compute/Tasks/launchinginstance.htm))
3. Asignar IP pública reservada si la API será accesible directamente desde Internet. Configurar NSG: SSH desde IP administrativa conocida; 443 público; 80 solo para redirigir a HTTPS/validación de certificado. No abrir 8080 ni la base de datos a Internet.
4. Instalar actualizaciones, Docker (o Java 17 si se ejecuta JAR), Nginx y certificado TLS. Crear un servicio de arranque/reinicio para la API. Limitar memoria de JVM según la VM.
5. Guardar el wallet correspondiente a la Autonomous Database existente, contraseña, proveedor de correo y secretos en el mecanismo de secretos disponible o archivos de permisos restringidos fuera del repo y de la imagen. Configurar el datasource y HikariCP; la cuenta Oracle de la API no será `ADMIN` ni las credenciales personales de Swing y tendrá permisos mínimos.
6. Para ADB público, permitir en ACL la IP reservada de la VM y únicamente los orígenes Swing que deban seguir conectándose directamente. Antes de cambiarla, confirmar que la aplicación Swing no perderá conectividad. Para ADB privado, verificar ruta, VCN y reglas NSG desde la VM. Probar conexión TLS sin publicar wallet ni credenciales.
7. Configurar Nginx para recibir HTTPS y reenviar a `127.0.0.1:8080`. No devolver información de `/actuator` excepto health mínimo protegido o público de forma controlada.
8. Probar `https://api.tudominio.com/actuator/health` o endpoint de health dedicado y una llamada autenticada de prueba antes de conectar Vercel.

Para construir un JAR ejecutable de la API sin reemplazar el artefacto de escritorio Swing, usar `mvn -Pbackend -DskipTests package`. El perfil backend omite Maven Shade y Spring Boot genera `target/pan-puntos-premios-1.0-SNAPSHOT-api.jar`; validar con `java -jar` en la VM. No transferir `.env` ni incluir secretos en el JAR.

OCI exige que una instancia pública tenga subnet pública, IP pública, Internet Gateway y rutas/reglas apropiadas; una IP reservada persiste y puede usarse para DNS/ACL. ([IP públicas OCI](https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/managingpublicIPs.htm))

## 6. Guía de publicación Vercel + Next.js (fase 8)

1. Crear `frontend/` con Next.js, React, TypeScript y App Router; incluir scripts `dev`, `build` y `start`, y documentar Node compatible.
2. Subir el frontend al repositorio Git y conectar ese repositorio en Vercel. Si el repo también contiene Maven, configurar **Root Directory = `frontend`** para que Vercel construya solo el Next.js.
3. En Vercel elegir Next.js (detección automática), usar pnpm con el `pnpm-lock.yaml` versionado y comando `pnpm build`; fijar pnpm 10.19.0 y una versión LTS de Node compatible.
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
| **Ejecutar toda la suite de pruebas** | `mvn test` | Ejecuta las pruebas automatizadas de escritorio y API (87 pasaron en esta revisión; el total puede cambiar). |

Para el portal, abrir otra terminal y ejecutar desde `frontend/`:

```powershell
pnpm install
pnpm dev
pnpm lint
pnpm build
```

Con la API activa en `http://localhost:8080`, el rewrite local usa ese origen por defecto. Para cambiarlo, definir `SPRING_API_ORIGIN` antes de ejecutar Next.js. El archivo `pnpm-lock.yaml` se debe conservar versionado para que instalaciones locales y Vercel resuelvan las mismas versiones.

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
| Correos existentes no verificados o compartidos | El primer enlace prueba acceso al buzón registrado; no marcar cuentas como verificadas por tener un correo. Corregir manualmente direcciones compartidas o desactualizadas antes de activar acceso. |
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
- Cliente con correo registrado puede solicitar el primer enlace; al consumirlo se verifica el buzón y se abre la sesión. Correo no registrado recibe la misma respuesta pública.
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
- [OCI: regiones y dominios de disponibilidad](https://docs.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm)
- [OCI: Email Delivery, correo transaccional](https://docs.oracle.com/en-us/iaas/Content/Email/Concepts/overview.htm)
- [OCI: endpoint regional de Email Delivery](https://docs.oracle.com/en-us/iaas/api/)
- [OCI: ver información de red de Autonomous AI Database](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/view-network-information.html)
- [OCI: opciones de acceso de red de Autonomous AI Database](https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/network-access-options.html)
- [OCI: endpoints privados Autonomous Database](https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/private-endpoints-autonomous.html)
- [OCI: direcciones IP públicas](https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/managingpublicIPs.htm)
- [Spring Security: One-Time Token Login](https://docs.spring.io/spring-security/reference/servlet/authentication/onetimetoken.html)
