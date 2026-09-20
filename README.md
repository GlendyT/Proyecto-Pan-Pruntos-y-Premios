# Pan, Puntos y Premios

Sistema de gestión de pedidos y fidelización para un restaurante de sándwiches. Java + Swing + Oracle Cloud (Autonomous Database).

Documentación completa del diseño (ER, DDL, clases, casos de uso): [`diseno_pan_puntos_premios.md`](diseno_pan_puntos_premios.md).
Enunciado original: [`Proyecto_Pan_Puntos_y_Premios.pdf`](Proyecto_Pan_Puntos_y_Premios.pdf).

---

## 1. Requisitos previos

| Herramienta | Versión usada | Notas |
|---|---|---|
| Java (JDK) | 21 | `java -version` para verificar |
| Maven | 3.9.16 | `mvn -version` para verificar |

### Si no tienes Maven instalado

1. Descarga el binario desde [maven.apache.org](https://maven.apache.org/download.cgi) (o usa `dlcdn.apache.org/maven/maven-3/<version>/binaries/apache-maven-<version>-bin.zip`).
2. Descomprime en una carpeta, por ejemplo `C:\Users\<tu_usuario>\Tools\apache-maven-<version>`.
3. Agrega la subcarpeta `bin` de esa ruta a la variable de entorno `Path` de tu usuario (Windows: Configuración → Variables de entorno → Path del usuario → Editar → Nuevo).
4. Abre una terminal **nueva** y confirma con `mvn -version`.

Alternativa sin instalar nada aparte: abrir el proyecto con **VS Code** (extensión "Extension Pack for Java") o **IntelliJ IDEA**, que traen Maven integrado.

---

## 2. Base de datos: Oracle Cloud Autonomous Database

Este proyecto usa una única instancia de **Oracle Autonomous Database (Always Free)** en Oracle Cloud, compartida por todo el equipo — no cada quien crea la suya. La conexión requiere dos cosas que **no están en este repositorio** (por seguridad) y que debes pedirle a quien administra la base de datos:

1. **El wallet** (carpeta con `tnsnames.ora`, `cwallet.sso`, `ojdbc.properties`, etc.) — se comparte por un canal privado (Drive, USB, chat), nunca por git.
2. **La contraseña del usuario `ADMIN`.**

### 2.1 Configurar tu conexión local

1. Pide el wallet y la contraseña a quien administra la base.
2. Descomprime el wallet en cualquier carpeta de tu máquina, **fuera de este repositorio** (ej. `C:\Users\<tu_usuario>\oracle-wallet\`).
3. Copia `.env.example` a un nuevo archivo llamado `.env` (en la raíz del proyecto) y complétalo:

   ```
   DB_URL=jdbc:oracle:thin:@panpuntospremios_high?TNS_ADMIN=C:/ruta/a/tu/wallet
   DB_USER=ADMIN
   DB_PASSWORD=la_contraseña_compartida
   ```

   El alias `panpuntospremios_high` viene definido dentro del `tnsnames.ora` del wallet; no lo cambies.

`.env` ya está en `.gitignore`: es local a tu máquina y nunca se sube al repositorio.

### 2.2 Crear las tablas (una sola vez por base de datos, no por persona)

Ejecuta [`sql/ddl.sql`](sql/ddl.sql) contra la base usando **Database Actions** (editor SQL en el navegador, disponible desde la consola de Oracle Cloud) o **SQL Developer** / VS Code con extensión Oracle, apuntando al mismo wallet.

---

## 3. Cómo correr el proyecto

```bash
mvn package
java -jar target/pan-puntos-premios-1.0-SNAPSHOT.jar
```

Si la conexión es correcta, verás:

```
Conexion exitosa a Oracle Cloud: jdbc:oracle:thin:@panpuntospremios_high
```

`mvn package` genera un `.jar` con todas las dependencias incluidas (gracias al `maven-shade-plugin` configurado en `pom.xml`), así que no hace falta manejar el classpath manualmente. Solo necesitas repetir `mvn package` cuando cambies código Java.

Alternativa: abrir `Main.java` en tu IDE (VS Code / IntelliJ) y usar el botón **Run** sobre el método `main`.

---

## 4. Estructura del proyecto

```
Proyecto-Pan-Pruntos-y-Premios/
├── pom.xml                          Dependencias: ojdbc11, oraclepki, osdt_core/cert, dotenv-java
├── .env.example                     Plantilla de variables de entorno (sin datos reales)
├── sql/ddl.sql                      Script de creación de tablas y datos de prueba
├── diseno_pan_puntos_premios.md     Documento de diseño completo (ER, DDL, clases, casos de uso)
├── Proyecto_Pan_Puntos_y_Premios.pdf  Enunciado original del proyecto
└── src/main/java/com/panpuntospremios/
    ├── Main.java                    Prueba de conexión a la base de datos
    └── util/ConexionOracle.java     Conexión JDBC (lee credenciales desde .env)
```

---

## 5. Dependencias de Maven (`pom.xml`)

| Dependencia | Para qué sirve |
|---|---|
| `com.oracle.database.jdbc:ojdbc11` | Driver JDBC para conectarse a Oracle |
| `com.oracle.database.security:oraclepki` | Permite al driver leer el `cwallet.sso` del wallet (autenticación SSO) |
| `com.oracle.database.security:osdt_core` / `osdt_cert` | Librerías de soporte para `oraclepki` |
| `io.github.cdimascio:dotenv-java` | Carga las variables del archivo `.env` en tiempo de ejecución |
| `org.apache.maven.plugins:maven-shade-plugin` | Empaqueta un `.jar` ejecutable con todas las dependencias incluidas |

---

## 6. Notas de seguridad

- Nunca subas `.env` ni la carpeta del wallet a git — ambos están (o deben estar) en `.gitignore`.
- Si el wallet se rota (botón "Rotate wallet" en la consola de Oracle Cloud), el wallet anterior deja de funcionar y hay que redistribuir el nuevo a todo el equipo.
- La contraseña de `ADMIN` es compartida por el equipo; si sospechas que se filtró, resetéala desde la consola de Oracle Cloud (Database → More actions → Administrator password → Reset) y avisa a todos.
