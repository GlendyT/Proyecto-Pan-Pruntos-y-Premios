# Pan, Puntos y Premios

Sistema de gestión de pedidos y fidelización para un restaurante de sándwiches. El repositorio contiene la aplicación de escritorio Java/Swing y un portal web de clientes (Next.js) respaldado por una API Spring Boot y Oracle Autonomous Database.

## Portal web de clientes y despliegue

El portal permite a cada cliente solicitar acceso por correo y consultar su propio saldo e historial de puntos. No ofrece funciones de cajero ni administración.

- **Estado:** API desplegada en OCI y accesible por HTTPS; certificado real y renovación automática configurados. El frontend Next.js todavía no se ha publicado en Vercel.
- **Continuar desde aquí:** consulta [`PLAN_API_SPRING_BOOT.md`](PLAN_API_SPRING_BOOT.md) para el plan por fases y [`docs/GUIA_DESPLIEGUE_OCI_VERCEL.md`](docs/GUIA_DESPLIEGUE_OCI_VERCEL.md) para el registro reproducible de la configuración, el traspaso a otro desarrollador, la preparación local y los siguientes pasos de Vercel.
- **API actual:** `https://163.192.144.19/api/v1/health` (solo health; el acceso a puntos requiere sesión de cliente).
- **Frontend local:** desde `frontend/`, ejecutar `pnpm install --frozen-lockfile` y `pnpm dev`. La guía de despliegue explica cómo configurar el proxy hacia la API.

No reutilices ni solicites por chat claves privadas, contraseñas de base de datos, secretos de EmailJS ni contenido del wallet. Solicita acceso y secretos al responsable por un canal privado; las plantillas del repositorio contienen solo marcadores.

---

## Aplicación de escritorio Swing

La aplicación original de escritorio sigue disponible y es independiente del portal web. Para diseño y funcionalidad consulta [`diseno_pan_puntos_premios.md`](diseno_pan_puntos_premios.md). El enunciado original se encuentra en [`Proyecto_Pan_Puntos_y_Premios.pdf`](Proyecto_Pan_Puntos_y_Premios.pdf).

---

## 1. Requisitos previos

| Herramienta | Versión usada | Notas |
|---|---|---|
| Java (JDK) | 17 o superior | El proyecto genera bytecode compatible con Java 17; `java -version` para verificar |
| Maven | 3.9.16 | `mvn -version` para verificar |
| Node.js | 20.9 o superior | Requerido por Next.js 16 para el portal web |
| pnpm | 10.19.0 | Versión declarada por `frontend/package.json`; instalar con Corepack o `npm install --global pnpm@10.19.0` |

### Si no tienes Maven instalado

1. Descarga el binario desde [maven.apache.org](https://maven.apache.org/download.cgi) (o usa `dlcdn.apache.org/maven/maven-3/<version>/binaries/apache-maven-<version>-bin.zip`).
2. Descomprime en una carpeta, por ejemplo `C:\Users\<tu_usuario>\Tools\apache-maven-<version>`.
3. Agrega la subcarpeta `bin` de esa ruta a la variable de entorno `Path` de tu usuario (Windows: Configuración → Variables de entorno → Path del usuario → Editar → Nuevo).
4. Abre una terminal **nueva** y confirma con `mvn -version`.

Alternativa sin instalar nada aparte: abrir el proyecto con **VS Code** (extensión "Extension Pack for Java") o **IntelliJ IDEA**, que traen Maven integrado.

---

## 2. Base de datos: Oracle Cloud Autonomous Database

El proyecto usa una única instancia existente de **Oracle Autonomous Database** compartida por el equipo. No crees una base nueva para seguir el portal web. La conexión requiere el wallet y una cuenta Oracle autorizada, que deben solicitarse al responsable por un canal privado. El portal usa la cuenta de servicio `APP_PUNTOS_API` sobre el esquema `ADMIN`; el usuario de escritorio Swing puede tener necesidades de privilegios diferentes.

### 2.1 Configurar tu conexión local

1. Pide el wallet y las credenciales de la cuenta apropiada para la aplicación que desarrollarás.
2. Descomprime el wallet en cualquier carpeta de tu máquina, **fuera de este repositorio** (ej. `C:\Users\<tu_usuario>\oracle-wallet\`).
3. Copia `.env.example` a un nuevo archivo llamado `.env` (en la raíz del proyecto) y complétalo:

   ```
   DB_URL=jdbc:oracle:thin:@panpuntospremios_low?TNS_ADMIN=C:/ruta/a/tu/wallet
   DB_USER=APP_PUNTOS_API
   DB_PASSWORD=<secreto-proporcionado-privadamente>
   DB_SCHEMA=ADMIN
   ```

   El alias usado actualmente es `panpuntospremios_low`, definido dentro del `tnsnames.ora` del wallet. Si el wallet recibido usa otro alias, conserva el alias que realmente aparezca allí.

`.env` ya está en `.gitignore`: es local a tu máquina y nunca se sube al repositorio.

### 2.2 Esquema existente

El esquema compartido ya tiene las tablas de la aplicación y la migración aditiva de autenticación aplicada y verificada. **No ejecutes `sql/ddl.sql` ni `sql/auth_tokens_up.sql` en esta base compartida**; esos scripts no son pasos de alta por desarrollador. Consulta [`docs/GUIA_DESPLIEGUE_OCI_VERCEL.md`](docs/GUIA_DESPLIEGUE_OCI_VERCEL.md) para el acceso local de API, y solicita autorización antes de ejecutar cualquier SQL administrativo. `sql/ddl.sql` aplica solo al crear una base de datos aislada nueva, fuera del flujo de continuidad del portal.

---

## 3. Cómo correr el proyecto

Después de clonar el repositorio, configurar `.env` y colocar el wallet fuera del proyecto, compila y ejecuta la aplicación con:

```bash
mvn compile exec:java
```

Este es el comando recomendado durante el desarrollo porque primero actualiza las clases de `target/classes` y luego ejecuta `com.panpuntospremios.Main`.

Se abrirá la ventana principal de Swing. La navegación disponible es:

- **Clientes → Gestionar clientes**.
- **Catálogo e inventario → Gestionar productos**.
- **Catálogo e inventario → Gestionar menús**.
- **Catálogo e inventario → Inventario y abastecimiento**.
- **Pedidos → Nuevo pedido**.
- **Pedidos → Pedidos pendientes**.
- **Pedidos → Cobro y comprobantes**.
- **Reportes → Consultar reportes**.

### Pedidos y cobro

1. En **Nuevo pedido**, busca un cliente activo por identificador, agrega productos o menús y confirma. Para editar una cantidad, selecciona la fila, cambia el selector de cantidad y pulsa **Cambiar cantidad**. El pedido queda pendiente sin consumir inventario ni acreditar puntos.
2. En **Pedidos pendientes**, busca al cliente y selecciona un pedido para ver su detalle, anularlo o abrir el cobro. Usa **Buscar cliente** para actualizar la lista después de cobrar en otra ventana.
3. En **Cobro y comprobantes**, consulta el ID del pedido. Selecciona efectivo e introduce el importe recibido con punto decimal, o tarjeta y el resultado **Aprobada/Rechazada** del simulador. Pulsa **Cobrar**.
4. El cobro exitoso descuenta productos y componentes de menús, registra movimientos, acredita puntos y marca `PAGADO` en una transacción. El comprobante muestra los importes guardados y el cambio en efectivo. Puedes recuperarlo consultando el ID nuevamente y usar **Marcar entregado** cuando corresponda.

Los precios y puntos de un pedido confirmado se conservan aunque cambie el precio del catálogo. Los componentes de un menú no se pueden editar mientras tenga pedidos pendientes: primero deben cobrarse o anularse. Al confirmar y cobrar se vuelven a validar actividad y existencias; un pedido pendiente no reserva stock.

La tarjeta es una simulación y no solicita datos bancarios. El esquema no requiere migraciones: `referencia_pago` guarda el importe recibido para efectivo o una referencia `SIM-` para tarjeta. El comprobante identifica los ítems por sus IDs y muestra la fecha del pedido. Las pruebas automatizadas usan H2; además, se verificó en Oracle el flujo de cobro, su persistencia tras reconectar, el rechazo y la falta de stock sin cambios parciales. La regularización del inventario histórico inicial sigue registrada en la fase 1 del plan.

### Reportes

La pantalla **Reportes → Consultar reportes** ofrece el alcance provisional de ventas pagadas/entregadas, inventario de productos, movimientos de inventario y movimientos de puntos. Ventas y movimientos aceptan un rango de fechas inclusivo y filtros por estado o tipo; las fechas se escriben como `AAAA-MM-DD` y se pueden dejar vacías para consultar todo el historial. Inventario permite ver todos los productos o filtrar activos/inactivos. Las tablas permiten ordenar por columna.

Los reportes reflejan los datos guardados en Oracle. La suite prueba en H2 las consultas, filtros y el flujo cliente → abastecimiento → pedido → cobro → puntos → canje → reportes. El listado definitivo de reportes sigue pendiente de contrastarse con el PDF del enunciado.

### Comandos de desarrollo y verificación

| Objetivo | Comando | ¿Requiere Oracle? |
|---|---|---|
| Compilar el código | `mvn compile` | No |
| Ejecutar las pruebas aisladas | `mvn test` | No |
| Compilar y abrir la interfaz | `mvn compile exec:java` | La ventana principal no; los formularios con datos sí |
| Generar el JAR ejecutable y ejecutar pruebas | `mvn package` | No |
| Ejecutar el JAR generado | `java -jar target/pan-puntos-premios-1.0-SNAPSHOT.jar` | La ventana principal no; los formularios con datos sí |

La suite normal utiliza H2 en memoria para probar persistencia, reportes y transacciones sin modificar Oracle. Ejecuta la suite con:

```bash
mvn --batch-mode --no-transfer-progress test
```

Para ejecutar una sola clase de prueba:

```bash
mvn -Dtest=ProductoServicioTest test
```

En PowerShell, si el argumento `-D` se interpreta incorrectamente, se puede envolver entre comillas:

```powershell
mvn "-Dtest=ProductoServicioTest" test
```

### Pruebas opcionales contra Oracle

Estas pruebas requieren `.env` y el wallet. Crean datos con identificadores únicos, cierran y vuelven a abrir conexiones, verifican la persistencia y eliminan exclusivamente los registros temporales en un bloque de limpieza. No forman parte de `mvn test` ni de GitHub Actions.

Persistencia de clientes:

```powershell
mvn "-Dtest=ClienteDAOOraclePersistenciaIT" "-Doracle.it=true" test
```

Productos, menús e inventario:

```powershell
mvn "-Dtest=CatalogoInventarioOracleIT" "-Doracle.it=true" test
```

### Ejecutar el JAR

```bash
mvn package
java -jar target/pan-puntos-premios-1.0-SNAPSHOT.jar
```

`mvn package` genera un `.jar` ejecutable con todas las dependencias incluidas gracias al `maven-shade-plugin` configurado en `pom.xml`. Debes volver a generarlo cuando cambie el código Java.

Alternativa: abrir `Main.java` en tu IDE (VS Code / IntelliJ), dejar que Maven cargue las dependencias y usar **Run** sobre el método `main`.

### Si no aparece la interfaz

No uses solamente `mvn exec:java` inmediatamente después de clonar el repositorio o de modificar código. Ese objetivo ejecuta las clases que ya se encuentren compiladas en `target/classes`, pero no garantiza una compilación previa.

Si solo aparece un mensaje como:

```
Conexion exitosa a Oracle Cloud: jdbc:oracle:thin:@panpuntospremios_high
```

seguido inmediatamente de `BUILD SUCCESS`, Maven ejecutó una versión antigua de `Main.class` que únicamente probaba la conexión. Recompila y vuelve a ejecutar:

```bash
mvn compile exec:java
```

Con la versión actual, Maven permanece en ejecución mientras la ventana esté abierta. Los avisos acerca de hilos `oracle.jdbc.diagnostics` que no finalizaron pertenecen a la antigua prueba de conexión y no indican un error de Swing.

### Integración continua

El workflow `.github/workflows/java-ci.yml` compila el proyecto y ejecuta las pruebas aisladas cuando se abre o actualiza un pull request desde cualquier rama hacia `development`.

No se ejecuta por cada `push`, en pull requests dirigidos a otras ramas ni contra Oracle Cloud. Por ello no requiere `.env`, wallet o credenciales almacenadas en GitHub. Internamente utiliza:

```bash
mvn --batch-mode --no-transfer-progress test
```

---

## 4. Estructura del proyecto

```
Proyecto-Pan-Pruntos-y-Premios/
├── pom.xml                           Configuración, dependencias y plugins de Maven
├── .env.example                      Plantilla de variables de entorno (sin datos reales)
├── .github/workflows/java-ci.yml      Pruebas de PR dirigidos a development
├── sql/ddl.sql                       Script de creación de tablas y datos de prueba
├── diseno_pan_puntos_premios.md      Documento de diseño completo
├── PLAN_API_SPRING_BOOT.md            Plan por fases de API y portal de clientes
├── docs/GUIA_DESPLIEGUE_OCI_VERCEL.md Traspaso y guía de despliegue vigente
├── frontend/                          Portal cliente Next.js + React + TypeScript
├── Proyecto_Pan_Puntos_y_Premios.pdf Enunciado original del proyecto
└── src/
    ├── main/java/com/panpuntospremios/
    │   ├── Main.java                 Punto de entrada de la interfaz Swing
    │   ├── vista/                    Ventana principal y formularios Swing
    │   ├── modelo/                   Entidades y objetos del negocio
    │   ├── dao/                      Acceso JDBC a Oracle
    │   ├── servicio/                 Reglas y operaciones del negocio
    │   ├── api/                      API Spring Boot, seguridad y endpoints REST
    │   └── util/                     Conexión JDBC y manejo de transacciones
    └── test/java/com/panpuntospremios/ Pruebas aisladas e integraciones Oracle opcionales
```

---

## 5. Dependencias y plugins de Maven (`pom.xml`)

| Dependencia | Para qué sirve |
|---|---|
| `org.springframework.boot:spring-boot-starter-web` | Servidor REST de la API Spring Boot |
| `org.springframework.boot:spring-boot-starter-security` | Sesiones de cliente, autenticación y CSRF |
| `org.springframework.boot:spring-boot-starter-jdbc` | Acceso relacional de la API |
| `org.springframework.boot:spring-boot-starter-validation` | Validación de solicitudes REST |
| `org.springframework.boot:spring-boot-starter-actuator` | Health y diagnóstico mínimo del backend |
| `org.springframework.boot:spring-boot-starter-mail` | Transporte SMTP alternativo |
| `com.oracle.database.jdbc:ojdbc11` | Driver JDBC para conectarse a Oracle |
| `com.oracle.database.security:oraclepki` | Permite al driver leer el `cwallet.sso` del wallet (autenticación SSO) |
| `com.oracle.database.security:osdt_core` / `osdt_cert` | Librerías de soporte para `oraclepki` |
| `io.github.cdimascio:dotenv-java` | Carga las variables del archivo `.env` en tiempo de ejecución |
| `org.junit.jupiter:junit-jupiter` | Framework de pruebas automatizadas |
| `com.h2database:h2` | Base en memoria usada únicamente durante las pruebas aisladas |
| `org.apache.maven.plugins:maven-shade-plugin` | Empaqueta un `.jar` ejecutable con todas las dependencias incluidas |
| `org.codehaus.mojo:exec-maven-plugin` | Ejecuta `Main` mediante `mvn exec:java` |

---

## 6. Notas de seguridad

- Nunca subas `.env` ni la carpeta del wallet a git — ambos están (o deben estar) en `.gitignore`.
- Si el wallet se rota (botón "Rotate wallet" en la consola de Oracle Cloud), el wallet anterior deja de funcionar y hay que redistribuir el nuevo a todo el equipo.
- No uses `ADMIN` como cuenta de conexión del portal. La API utiliza `APP_PUNTOS_API`; solicita sus credenciales al responsable y rota cualquier secreto que se haya expuesto. El acceso de Swing puede tener una cuenta y permisos distintos.
