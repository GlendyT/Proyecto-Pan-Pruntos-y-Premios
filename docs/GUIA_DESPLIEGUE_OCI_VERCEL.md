# Guía de traspaso y despliegue OCI + Vercel

**Proyecto:** Pan, Puntos y Premios  
**Última actualización:** 13 de octubre de 2026
**Propósito:** permitir que otro desarrollador entienda el estado actual, prepare su máquina y opere el backend ya desplegado y el frontend publicado sin repetir migraciones ni exponer secretos.

> Esta guía registra el entorno OCI del proyecto. No contiene credenciales, claves SSH privadas, wallet, OCID ni valores privados de EmailJS. Los secretos se solicitan al responsable por un canal privado.

## 1. Estado actual y próximo trabajo

### Ya realizado y verificado

- La Oracle Autonomous Database existente se conserva como fuente de datos. No crear otra ADB ni ejecutar el DDL inicial sobre ella.
- Las tablas, columnas, restricciones e índices de autenticación (`CLIENTE_TOKEN_ACCESO` y campos de verificación de correo) ya fueron creados y verificados por el responsable de la base.
- La cuenta de servicio `APP_PUNTOS_API` tiene los privilegios mínimos que necesita la API sobre objetos del esquema `ADMIN`; el usuario confirmó que la conexión JDBC desde la VM reporta `baseDatos: UP`.
- Las fases locales de API Spring Boot, autenticación por enlace de un solo uso, consulta de puntos y portal Next.js están implementadas. El flujo local se probó con correo real; no se deben publicar las claves de correo usadas.
- VM OCI `pan-puntos-api` en región Querétaro (`mx-queretaro-1`), Oracle Linux 9 AArch64, shape `VM.Standard.A1.Flex` (1 OCPU, 6 GB). SSH funciona con usuario `opc`.
- IP pública reservada actual: **`163.192.144.19`**; dominio DuckDNS: **`panpuntospremios.duckdns.org`**. La IP efímera anterior (`159.54.152.182`) dejó de ser la dirección vigente.
- El JAR backend está desplegado como servicio `pan-puntos-api`; el puerto Java 8080 es interno y no debe exponerse públicamente.
- Nginx está activo como proxy inverso. `https://panpuntospremios.duckdns.org/api/v1/health` respondió `UP` y `baseDatos: UP`.
- HTTPS está activo en `https://panpuntospremios.duckdns.org`; el certificado de producción de Let’s Encrypt está en `/etc/letsencrypt/live/panpuntospremios-duckdns/`. Certbot 5.8.0 está instalado en un entorno virtual con Python 3.12.
- El servicio/timer `certbot-renew.service` y `certbot-renew.timer` revisan renovaciones dos veces al día. Certbot guarda un deploy hook para recargar Nginx; la simulación `certbot renew --dry-run --run-deploy-hooks` terminó con todas las renovaciones simuladas exitosamente.
- Las reglas de OCI Security List permiten TCP 80 y 443 desde Internet. `firewalld` en la VM permite `http`, `https` y `ssh`. SELinux permanece en `Enforcing`; se habilitó el permiso requerido para el proxy Nginx.
- El proveedor activo de correo es EmailJS (`EMAIL_PROVIDER=emailjs`), invocado por el backend. Las variables actuales están en el `.env` privado de la VM.

### Estado operativo

El portal Next.js está publicado en `https://pan-puntos-portal.vercel.app`; `APP_PORTAL_BASE_URL` en OCI apunta a esa URL. El usuario confirmó que el enlace nuevo abre el portal e inicia sesión. La Fase 8 está cerrada. La Fase 9 se documenta como aceptación de la aplicación sin cambios a la infraestructura; el usuario prefiere mantener la VM estable por ahora. Consulta [`FASE9_ACEPTACION_SEGURIDAD.md`](FASE9_ACEPTACION_SEGURIDAD.md).

### Consideraciones registradas para endurecimiento futuro

- **SSH:** la Security List vista anteriormente permitía TCP 22 desde `0.0.0.0/0`. La restricción queda aplazada por decisión del usuario; esta documentación no cambia esa regla.
- **ADB:** se observó endpoint público, ACL deshabilitada y mTLS requerido. No activar ACL ahora; primero sería necesario inventariar clientes Swing y otros orígenes para no cortar conexiones.
- **Cabeceras de proxy:** `AuthController` procesa `X-Forwarded-For`; revisar la confianza del proxy como una tarea de aplicación separada, sin modificar Nginx/VM durante esta fase documental.
- **Aceptación de seguridad de aplicación:** tokens vencidos/reutilizados, CSRF/logout, `no-store`, límites, aislamiento con dos clientes y remitente están descritos en [`FASE9_ACEPTACION_SEGURIDAD.md`](FASE9_ACEPTACION_SEGURIDAD.md); los resultados deben registrarse antes de declarar la fase plenamente aceptada.
- El usuario confirmó que el enlace de producción abre el portal. La aceptación web básica quedó realizada; endurecimiento SSH/ACL y cambios de infraestructura se posponen hasta una decisión futura y una ventana de cambio con plan de reversión.

## 2. Arquitectura y endpoints

```text
Navegador del cliente
  └─ HTTPS → Next.js / Vercel (producción)
               └─ rewrite /api/* → https://panpuntospremios.duckdns.org/api/*
                                      └─ Nginx :443
                                           └─ Spring Boot 127.0.0.1:8080
                                                └─ Autonomous Database existente
```

El cliente se autentica con un enlace de un solo uso enviado al correo registrado. La identidad se toma de la sesión; la API no acepta un ID arbitrario de cliente para elegir qué puntos consultar. Las respuestas personales usan `no-store`.

| Recurso | Uso |
|---|---|
| `GET /api/v1/health` | Verifica API y conexión de base (responde JSON `UP`/`DOWN`). |
| `POST /api/v1/auth/access-requests` | Solicita un enlace, por ejemplo `{ "email": "cliente@ejemplo.com" }`. La respuesta es deliberadamente genérica. |
| `POST /api/v1/auth/consume` | Consume token de acceso enviado por el callback; el navegador recibe sesión. |
| `GET /api/v1/me` | Perfil de la sesión actual. Anónimo devuelve `401`, comportamiento esperado. |
| `GET /api/v1/me/points` | Saldo de la sesión actual. |
| `GET /api/v1/me/points/movements?limit=10` | Historial paginado de la sesión. |
| `POST /api/v1/auth/logout` | Cierra sesión; requiere CSRF. |

La API de salud actualmente responde en `https://panpuntospremios.duckdns.org/api/v1/health`. El navegador debe mostrar `status: UP` y `baseDatos: UP`. No usar HTTP ni publicar el puerto 8080.

### Para qué sirve cada recurso

- **VM OCI `pan-puntos-api`:** mantiene en ejecución la API Java y su conexión a Oracle. Nginx recibe HTTPS y reenvía las solicitudes al puerto local 8080. La VM no aloja Next.js ni la base de datos.
- **Nginx y Certbot:** Nginx funciona como proxy HTTPS; Certbot administra el certificado TLS de DuckDNS.
- **Autonomous Database:** conserva los datos del negocio. El backend web se conecta como `APP_PUNTOS_API`; Swing usa sus credenciales configuradas por separado.
- **Vercel:** construye y publica el portal Next.js y reenvía `/api/*` al backend OCI.
- **EmailJS:** entrega enlaces de acceso a clientes por solicitud del backend. Sus claves privadas se guardan solo en el `.env` de OCI.

### Encender y comprobar la VM sin cambiar su configuración

Desde la consola de Oracle Cloud, abre **Compute → Instances → pan-puntos-api** en la región **Mexico Central (Querétaro)**. Revisa `Lifecycle state`; si está `Stopped`, pulsa **Start** y espera `Running`. Si ya está `Running`, no hace falta reiniciarla. Conserva la IP pública reservada `163.192.144.19`.

Conéctate desde Git Bash con la clave privada guardada localmente:

```bash
ssh -i "/c/Users/<usuario>/Downloads/<archivo-clave>.key" opc@163.192.144.19
```

Ya dentro de la VM, consulta el estado sin cambiar configuración:

```bash
sudo systemctl is-active pan-puntos-api
sudo systemctl is-active nginx
curl -fsS https://panpuntospremios.duckdns.org/api/v1/health
```

La respuesta esperada del health check contiene `"status":"UP"` y `"baseDatos":"UP"`. Si falla, revisa primero:

```bash
sudo systemctl status pan-puntos-api --no-pager
sudo journalctl -u pan-puntos-api -n 80 --no-pager
sudo systemctl status nginx --no-pager
sudo tail -n 50 /var/log/nginx/error.log
```

No uses `kill` para terminar Java. Si un reinicio rutinario del backend resulta necesario, systemd administra el proceso:

```bash
sudo systemctl restart pan-puntos-api
sudo systemctl is-active pan-puntos-api
curl -fsS https://panpuntospremios.duckdns.org/api/v1/health
```

El arranque/apagado de la instancia se hace desde OCI con **Start/Stop**. La configuración del servicio systemd lo inicia con Linux si quedó habilitado. No cambies reglas de red, wallet, `.env`, Nginx, firewall ni ACL como parte de este procedimiento de consulta.

### URLs de producción

| URL/configuración | Función |
|---|---|
| `https://pan-puntos-portal.vercel.app` | Portal público de clientes; valor actual de `APP_PORTAL_BASE_URL` en OCI. |
| `https://panpuntospremios.duckdns.org` | Backend público tras Nginx/TLS; Vercel lo usa como `SPRING_API_ORIGIN`. |
| `https://panpuntospremios.duckdns.org/api/v1/health` | Health check de API y conexión ADB. |
| `http://127.0.0.1:8080` | Spring Boot interno; no es URL pública. |

## 3. Requisitos para una máquina de desarrollo nueva

- Git.
- JDK 17 o superior (la VM usa Java 17; el código compila para Java 17).
- Maven 3.9 o superior.
- Node.js 20.9 o superior para Next.js 16; pnpm 10.19.0 según `frontend/package.json`.
- Acceso privado al wallet de la ADB y a las credenciales de servicio si se requiere probar Oracle localmente. No es necesario que cada desarrollador tenga acceso al tenancy OCI; solicítalo solo si administrará el servidor.

Después de clonar:

```bash
git clone <URL_PRIVADA_DEL_REPOSITORIO>
cd Proyecto-Pan-Pruntos-y-Premios
```

### 3.1 Configurar el backend local

1. Pide el wallet de la ADB existente y descomprímelo fuera del repo, por ejemplo `C:/Users/<usuario>/oracle-wallet/`.
2. Copia `.env.example` a `.env` en la raíz del repositorio.
3. Solicita al responsable el password de `APP_PUNTOS_API` y los valores vigentes de EmailJS por un canal privado. El esquema propietario es `ADMIN`.
4. Completa en `.env` los marcadores; la ruta del wallet se escribe con `/` o con backslashes escapadas según el lector. Ejemplo con marcadores:

```dotenv
DB_URL=jdbc:oracle:thin:@panpuntospremios_low?TNS_ADMIN=C:/Users/<usuario>/oracle-wallet
DB_USER=APP_PUNTOS_API
DB_PASSWORD=<SECRETO_PROVISTO_PRIVADAMENTE>
DB_SCHEMA=ADMIN

# Solo para ejecutar la aplicación Swing local:
SWING_DB_USER=ADMIN
SWING_DB_PASSWORD=<CONTRASENA_ADMIN_PROVISTA_PRIVADAMENTE>
# Opcional si Swing utiliza otro wallet o alias TNS:
# SWING_DB_URL=jdbc:oracle:thin:@panpuntospremios_low?TNS_ADMIN=C:/Users/<usuario>/oracle-wallet

EMAIL_PROVIDER=emailjs
EMAILJS_SERVICE_ID=<VALOR_PRIVADO>
EMAILJS_TEMPLATE_ID=<VALOR_PRIVADO>
EMAILJS_PUBLIC_KEY=<VALOR_PRIVADO>
EMAILJS_PRIVATE_KEY=<VALOR_PRIVADO>

APP_PORTAL_BASE_URL=http://localhost:3000
SESSION_COOKIE_SECURE=false
```

La API usa `DB_USER`/`DB_PASSWORD` con `APP_PUNTOS_API`; Swing usa `SWING_DB_USER`/`SWING_DB_PASSWORD`. `ConexionOracle` mantiene compatibilidad con configuraciones antiguas de escritorio que aún tienen `DB_USER=ADMIN`, pero no toma las credenciales `APP_PUNTOS_API` como fallback para Swing.

Usa el alias TNS realmente presente en el `tnsnames.ora` del wallet; `panpuntospremios_low` es el alias usado en el entorno actual. No incluyas secretos en capturas, tickets, commits ni variables `NEXT_PUBLIC_*`.

No ejecutes `sql/ddl.sql`, `sql/auth_tokens_up.sql` ni `sql/app_puntos_api_grants.sql` en la ADB compartida para configurar una laptop. El esquema ya está migrado; son scripts de administración/migración y repetirlos puede fallar o alterar datos. Para inspeccionar objetos, usa las consultas/verificaciones de solo lectura con autorización del responsable.

### 3.2 Ejecutar localmente

Terminal 1, desde la raíz del repositorio:

```bash
mvn -Pbackend compile exec:java
```

La API queda en `http://localhost:8080`. En otra terminal, desde `frontend/`:

```bash
pnpm install --frozen-lockfile
pnpm dev
```

Next se sirve en `http://localhost:3000`. `frontend/next.config.ts` reescribe `/api/:path*` a `SPRING_API_ORIGIN`, cuyo default local es `http://localhost:8080`. Si se cambia el origen, definirlo antes de arrancar Next:

```bash
# PowerShell
$env:SPRING_API_ORIGIN = "http://localhost:8080"
pnpm dev
```

```bash
# Git Bash / macOS / Linux
SPRING_API_ORIGIN=http://localhost:8080 pnpm dev
```

No confundas `GET /api/v1/me` con una falla del servidor: `401 Unauthorized` sin iniciar sesión es correcto. Para desarrollo de frontend, `pnpm run lint` y `pnpm run build` están definidos en `frontend/package.json`.

## 4. Construir y actualizar la API en OCI

Esta sección es para el administrador/desarrollador autorizado del servidor actual. Mantén wallet y `.env` solo en la VM; nunca los empaquetes en el JAR.

### 4.1 Acceso SSH

La llave privada SSH se distribuye por canal privado y debe estar solo en la máquina del administrador. En Git Bash para Windows, ejemplo:

```bash
ssh -i "/c/Users/<usuario>/Downloads/<llave-privada>.key" opc@163.192.144.19
```

La primera conexión puede pedir confirmar host key. Antes de aceptarla en un entorno formal, valida su fingerprint con el administrador OCI. Nunca copies la llave privada al repo o a la VM.

### 4.2 Generar, transferir y activar una versión

Desde la raíz del repo:

```bash
mvn -Pbackend clean package -DskipTests
```

El artefacto de backend debe ser `target/pan-puntos-premios-1.0-SNAPSHOT-api.jar`. El perfil `backend` usa Spring Boot repackage y evita reemplazar el JAR de escritorio Swing.

Transferir a la carpeta existente del servidor:

```bash
scp -i "/c/Users/<usuario>/Downloads/<llave-privada>.key" \
  target/pan-puntos-premios-1.0-SNAPSHOT-api.jar \
  opc@163.192.144.19:~/pan-puntos-api/
```

En la VM:

```bash
sudo systemctl restart pan-puntos-api
sudo systemctl is-active pan-puntos-api
sudo journalctl -u pan-puntos-api -n 80 --no-pager
curl -i http://127.0.0.1:8080/api/v1/health
curl -i https://panpuntospremios.duckdns.org/api/v1/health
```

Ambos health checks deben mostrar `status: UP` y `baseDatos: UP`. Para revisar la definición real del servicio antes de cambiar rutas o variables:

```bash
sudo systemctl cat pan-puntos-api
```

La VM debe mantener `~/pan-puntos-api/.env` y el wallet en el lugar al que apunta `DB_URL`; archivos secretos con permisos solo para `opc` (por ejemplo `.env` `chmod 600`, wallet `chmod 700`). La unidad systemd y su `WorkingDirectory` deben apuntar a `/home/opc/pan-puntos-api`, y leer el entorno esperado. No sobreescribas esa configuración a ciegas: inspecciónala con `systemctl cat` y `journalctl`.

Variables que deben existir en el entorno de producción, todas con valores obtenidos de forma privada:

```dotenv
DB_URL=jdbc:oracle:thin:@panpuntospremios_low?TNS_ADMIN=/home/opc/pan-puntos-api/Wallet_panpuntospremios
DB_USER=APP_PUNTOS_API
DB_PASSWORD=<SECRETO>
DB_SCHEMA=ADMIN
EMAIL_PROVIDER=emailjs
EMAILJS_SERVICE_ID=<SECRETO/CONFIG>
EMAILJS_TEMPLATE_ID=<SECRETO/CONFIG>
EMAILJS_PUBLIC_KEY=<CONFIG>
EMAILJS_PRIVATE_KEY=<SECRETO>
APP_PORTAL_BASE_URL=<URL_HTTPS_DEL_PORTAL_VERCEL>
SESSION_COOKIE_SECURE=true
```

En el entorno actual, `APP_PORTAL_BASE_URL` debe ser `https://pan-puntos-portal.vercel.app`. El valor activo ya fue corregido por el responsable y el flujo se confirmó. Esta plantilla solo es un ejemplo para una instalación o reconstrucción nueva; valida el dominio antes de usarlo.

### 4.3 Configuración OCI existente

- VM: compartimento `glendytuyuc (root)`, instancia `pan-puntos-api`, subnet pública `public subnet-global-dispatch-vcn`.
- La IP `163.192.144.19` se reservó bajo el nombre `pan-puntos-api-ip` y se asignó a la IP privada primaria. No confiar en la antigua IP efímera.
- La Security List default de `global-dispatch-vcn` actualmente contiene TCP 80 y 443 desde `0.0.0.0/0`; 22 también aparecía abierto a `0.0.0.0/0`. Cerrar/restringir SSH a CIDR de administración en la siguiente tarea de seguridad. No añadir 8080.
- En la VM `firewalld` zona `public` habilita `http`, `https`, `ssh`. SELinux queda `Enforcing`; se configuró `httpd_can_network_connect=on` para el reverse proxy.
- El NSG de la VNIC debe inspeccionarse junto con Security Lists si hay problemas de red.

### Secuencia para reproducir el servidor en una VM nueva

Esta subsección sirve como registro del aprovisionamiento, no como instrucción para modificar la VM que ya está funcionando. Si se reconstruye el servidor, primero coordina una ventana de cambio, conserva su IP reservada o asigna una nueva y ajusta DNS/Vercel/ACL según corresponda.

1. En OCI abre **Compute → Instances → Create instance**. El entorno actual usa Querétaro, Oracle Linux 9 AArch64, `VM.Standard.A1.Flex` (1 OCPU/6 GB), una subnet pública y una llave SSH pública. Guarda la llave privada de forma segura. No crees una nueva base de datos.
2. En la VNIC asigna una IP pública reservada. Si la instancia inicialmente recibió una IP efímera, en **IP administration → Edit** cambia temporalmente a **No public IP**, guarda y vuelve a editar para seleccionar **Reserved public IP**. La reservada se asigna a la IP privada primaria.
3. En la Security List de la subnet agrega ingress stateful TCP 80 y 443 desde `0.0.0.0/0`. SSH TCP 22 debe tener como origen solo el CIDR administrativo. No abras TCP 8080. Revisa también NSG, Internet Gateway, tabla de rutas y firewalld; ambas capas de firewall deben permitir HTTP/HTTPS.
4. Conéctate como `opc` y prepara runtime/proxy:

```bash
sudo dnf update -y
sudo dnf install -y java-17-openjdk nginx python3.12 python3.12-pip policycoreutils-python-utils
java -version
sudo systemctl enable --now nginx
sudo firewall-cmd --permanent --zone=public --add-service=http
sudo firewall-cmd --permanent --zone=public --add-service=https
sudo firewall-cmd --reload
sudo setsebool -P httpd_can_network_connect on
```

5. Crea `/home/opc/pan-puntos-api`, transfiere el JAR, wallet y `.env` por canal seguro (nunca los incluyas en Git). Usa `chmod 600` para `.env`, `chmod 700` para el wallet y ajusta `DB_URL` al wallet local de la VM. Verifica JDBC y `curl http://127.0.0.1:8080/api/v1/health` antes de configurar el proxy.
6. Crea un servicio systemd similar al siguiente (ajusta memoria a la shape y verifica el nombre exacto del artefacto):

```ini
[Unit]
Description=Pan Puntos Premios API
After=network-online.target
Wants=network-online.target

[Service]
User=opc
WorkingDirectory=/home/opc/pan-puntos-api
ExecStart=/usr/bin/java -Xms256m -Xmx768m -jar /home/opc/pan-puntos-api/pan-puntos-premios-1.0-SNAPSHOT-api.jar
Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

Guárdalo como `/etc/systemd/system/pan-puntos-api.service`; luego ejecuta `sudo systemctl daemon-reload`, `sudo systemctl enable --now pan-puntos-api` y revisa `sudo journalctl -u pan-puntos-api -n 80 --no-pager`. `ApiApplication` lee el `.env` de su `WorkingDirectory`.
7. Configura `/etc/nginx/conf.d/pan-puntos-api.conf` como se muestra abajo. Para el desafío HTTP-01, crea `/var/www/certbot/.well-known/acme-challenge`, etiqueta ese contenido `httpd_sys_content_t` con `semanage fcontext`/`restorecon`, y comprueba que un archivo de prueba sea accesible desde fuera antes de solicitar certificado:

```bash
sudo mkdir -p /var/www/certbot/.well-known/acme-challenge
sudo semanage fcontext -a -t httpd_sys_content_t "/var/www/certbot(/.*)?"
sudo restorecon -Rv /var/www/certbot
```
8. Certbot 5.8.0 está instalado en Python 3.12. Como el endpoint vigente usa un dominio, solicita el certificado de dominio con HTTP-01:

```bash
sudo python3.12 -m venv /opt/certbot312
sudo /opt/certbot312/bin/pip install --upgrade pip certbot
sudo ln -sf /opt/certbot312/bin/certbot /usr/local/bin/certbot
/usr/local/bin/certbot --version
```

Después de validar HTTP-01, solicita el certificado con `sudo /usr/local/bin/certbot certonly --webroot --webroot-path /var/www/certbot -d panpuntospremios.duckdns.org --cert-name panpuntospremios-duckdns --email <CORREO_ADMIN> --agree-tos --no-eff-email`. Configura Nginx con `fullchain.pem` y `privkey.pem`, valida con `nginx -t` y recarga. Evita solicitar certificados repetidamente; primero usa staging para probar una configuración nueva.
9. Crea/activa la renovación automática y deploy hook de Nginx. En la VM actual existen `certbot-renew.service` y `certbot-renew.timer`, el timer corre dos veces al día y ejecuta `/usr/local/bin/certbot renew --quiet`; Certbot reconfigura el lineage con `--deploy-hook "/usr/bin/systemctl reload nginx"`. Un timer equivalente:

```ini
[Unit]
Description=Check for Let's Encrypt certificate renewal twice daily

[Timer]
OnCalendar=*-*-* 00,12:00:00
RandomizedDelaySec=30m
Persistent=true

[Install]
WantedBy=timers.target
```

Ejecuta `certbot renew --dry-run --run-deploy-hooks` y confirma resultado exitoso antes de dar HTTPS por listo.

### 4.4 Nginx, SELinux y HTTPS ya configurados

Archivo activo: `/etc/nginx/conf.d/pan-puntos-api.conf`. Esquema equivalente:

```nginx
server {
    listen 80;
    server_name panpuntospremios.duckdns.org;
    location /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 301 https://$host$request_uri; }
}

server {
    listen 443 ssl;
    server_name panpuntospremios.duckdns.org;
    ssl_certificate /etc/letsencrypt/live/panpuntospremios-duckdns/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/panpuntospremios-duckdns/privkey.pem;
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Si se modifica:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

El webroot ACME `/var/www/certbot` usa SELinux type `httpd_sys_content_t`. Nginx necesita conexión upstream habilitada mediante `httpd_can_network_connect`; no desactives SELinux.

El certificado vigente es para `panpuntospremios.duckdns.org`, no para la IP. Mantén habilitada la renovación automática; el certificado de dominio usa el ciclo estándar de ACME.

- `certbot-renew.timer` se ejecuta dos veces al día con retraso aleatorio.
- `/etc/systemd/system/certbot-renew.service` ejecuta `/usr/local/bin/certbot renew --quiet`.
- Certbot renewal lineage `panpuntospremios-duckdns` conserva `--webroot` y el deploy hook `/usr/bin/systemctl reload nginx`.
- Verificación efectuada: `certbot renew --cert-name panpuntospremios-duckdns --dry-run --run-deploy-hooks` reportó “all simulated renewals succeeded”.

Comandos operativos:

```bash
sudo /usr/local/bin/certbot certificates
systemctl list-timers certbot-renew.timer
sudo systemctl status certbot-renew.timer
sudo /usr/local/bin/certbot renew --cert-name panpuntospremios-duckdns --dry-run --run-deploy-hooks
```

Usa `https://panpuntospremios.duckdns.org` como origen HTTPS oficial. La IP reservada identifica la VM, pero el certificado y el `server_name` corresponden al dominio. Si cambia la IP, actualiza el registro DuckDNS; si cambia el dominio, actualiza certificado, Nginx y `SPRING_API_ORIGIN` en Vercel.

## 5. Registro reproducible de publicación Vercel (Fase 8 completada)

El portal ya está publicado y el usuario confirmó el acceso mediante correo. Los pasos siguientes son la receta reproducible usada para este proyecto, no tareas pendientes del despliegue actual.

1. Confirma que los últimos cambios estén en una rama remota de Git accesible por Vercel. No añadas `.env`, wallet, ZIP del wallet, claves SSH ni archivos de export con secretos.
2. En Vercel crea un proyecto desde el repositorio Git.
3. Configura **Root Directory** como `frontend`.
4. Framework preset: **Next.js**. Usa el lockfile `frontend/pnpm-lock.yaml` y pnpm `10.19.0` (`packageManager` del `package.json`). Instala con `pnpm install --frozen-lockfile`; build `pnpm build` (o configuración detectada equivalente).
5. Define versión Node compatible con Next.js 16 (20.9+; usar versión LTS aceptada por Vercel).
6. Añade `SPRING_API_ORIGIN` en Preview y Production con el origen, sin path final: `https://panpuntospremios.duckdns.org`. No usar `NEXT_PUBLIC_`; es una configuración del build de Next, no un secreto.
7. Despliega Preview. Prueba `GET /api/v1/health` a través del rewrite, pide un enlace a un correo de prueba controlado por el equipo y completa callback/login y llamadas autenticadas.
8. Cuando Vercel emita la URL estable `https://<proyecto>.vercel.app`, agrega ese valor a `APP_PORTAL_BASE_URL` del `.env` en OCI, conserva `SESSION_COOKIE_SECURE=true`, reinicia `pan-puntos-api` y repite el flujo de correo. El enlace debe abrir el mismo dominio de Vercel.
9. Revisa `Set-Cookie` de `PANPUNTOS_SESSION`, cookie CSRF, `401` anónimo, logout y que las respuestas `/api/v1/me/**` no se almacenen en caché. Vercel no debe almacenar en caché llamadas personales.
10. Promueve a Production solo tras aceptación E2E; comparte la URL `*.vercel.app`. Dominio personalizado es opcional.

`frontend/next.config.ts` define rewrite externo para `/api/:path*` a `${SPRING_API_ORIGIN}/api/:path*`. El navegador llama rutas relativas `/api/v1/...` para mantener las cookies en el mismo origen del sitio. No pongas credenciales Oracle, wallet o claves EmailJS en Vercel; solo `SPRING_API_ORIGIN` pertenece al frontend.

## 6. Correo de acceso

- Se intentó usar OCI Email Delivery por SMTP. Desde la PC los puertos eran bloqueados; desde VM TCP/587 sí respondió, pero completar autenticación/remitente/dominio no fue la vía elegida.
- Se configuró EmailJS con una plantilla de enlace, usando Gmail como servicio personal. El backend envía al endpoint REST de EmailJS y se confirmó entrega real; la respuesta HTTP 200 del endpoint de acceso por sí sola no demuestra que el email se envió. Corroborar logs `Correo de acceso enviado exitosamente vía EmailJS` y correo recibido.
- Las cuatro variables `EMAILJS_*` se configuran exclusivamente en `.env` de backend, nunca en frontend/Vercel. La clave privada que se expuso durante la configuración fue rotada; no recuperar ni reusar claves previas. Si se sospecha una nueva exposición, rotar el valor de EmailJS.
- El endpoint puede responder mensaje genérico aun si el email no existe, hay cooldown/cuota o envío no concluido. Revisar logs seguros y estado de token en Oracle sin imprimir tokens ni PII.

## 7. Troubleshooting conocido

| Síntoma | Explicación / comprobación |
|---|---|
| `GET /api/v1/me` devuelve 401 recién abierto el portal | Esperado para cliente anónimo. Solicita enlace. |
| `502 Bad Gateway` en Nginx | Spring puede estar en 8080, pero SELinux bloquea Nginx. Ver `getenforce`, `sudo tail /var/log/nginx/error.log`, `sudo getsebool httpd_can_network_connect`; permitir solo el boolean necesario y mantener SELinux activo. |
| Health local `UP`, pero externo timeout | Revisar Security List de la subnet, NSG de VNIC, firewalld y que Nginx escuche 80/443. Mantener 8080 cerrado a Internet. |
| Solicitud da 200, no llega correo | 200 es anti-enumeración. Revisar `EMAIL_PROVIDER=emailjs`, variables de VM y log de `EmailService`; confirmar correo/servicio EmailJS. No imprimir valores ni token. |
| Token insertado pero enlace dice inválido | Confirmar que el callback conserva el fragmento `#token=...`, que se abre el enlace más reciente y no ha expirado (15 min/uso único); no quitar ni editar el fragmento. El callback limpia URL tras capturarlo y protege doble efecto de React StrictMode. |
| Error de SQL `LONG` o identificador inválido al verificar | Algunas vistas de Oracle exponen columnas LONG y los nombres deben coincidir exactamente con el esquema. Usar scripts SQL de verificación actualizados; no asumir que una consulta fallida significa que la migración UP falló. |
| SQLcl `CONNECT` no encuentra wallet | Configurar `TNS_ADMIN` en PowerShell antes de lanzar SQLcl desde esa misma consola; comandos `$env:...` no se ejecutan dentro del prompt SQL (`SQL>`). |

## 8. Referencias de proyecto

- [Plan completo por fases](../PLAN_API_SPRING_BOOT.md)
- [README general y aplicación Swing](../README.md)
- [Plantilla de entorno](../.env.example)
- [Migración de tokens (ya aplicada; no repetir)](../sql/auth_tokens_up.sql)
- Scripts de referencia (no ejecutar sobre la ADB compartida sin autorización): [grants](../sql/app_puntos_api_grants.sql) · [verificación](../sql/app_puntos_api_verificar.sql)

## 9. Documentación oficial de proveedores

- [Certificados Let’s Encrypt para IP y Certbot](https://letsencrypt.org/2026/03/11/shorter-certs-certbot)
- [Guía de Certbot: renovación y `reconfigure`](https://eff-certbot.readthedocs.io/en/stable/using.html)
- [OCI: tipos y ciclo de vida de IP públicas](https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/managingpublicIPs.htm)
- [OCI: asignar una IP reservada a una IP privada](https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/reserved-public-ip-assign.htm)
- [Oracle Linux: configurar `firewalld`](https://docs.oracle.com/en-us/iaas/oracle-linux/firewall/ol-firewall-configuring-the-firewall-with-firewalld.htm)
- [Vercel: rewrites a orígenes externos](https://vercel.com/docs/routing/rewrites)
- [Vercel: framework Next.js](https://vercel.com/docs/frameworks/full-stack/nextjs)
