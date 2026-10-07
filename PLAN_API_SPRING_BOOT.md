# Plan de Trabajo e Implementación: Panel Web para Cajero/Administrador con Spring Boot

**Proyecto:** Pan, Puntos y Premios
**Fecha:** Octubre 2026
**Objetivo:** Extender el sistema existente (Java 17 + Oracle) con una **API REST en Spring Boot** y un **panel web** que permita al cajero/administrador operar el negocio completo (clientes, catálogo, inventario, pedidos, cobro, puntos y canjes) desde el navegador, como segunda interfaz en paralelo a la aplicación de escritorio Swing.

---

## 1. Justificación, alcance y límites

### 1.1 Relación con el enunciado del PDF

El PDF exige explícitamente **Java + Swing + Oracle** como entregable calificado (sección 17, "Consideraciones finales"), y los 5 casos de uso obligatorios (sección 10) describen siempre a un **"Usuario"** (el cajero/empleado) operando el sistema en nombre del cliente desde el mostrador. La única mención a que el cliente final toque el sistema directamente es el punto extra de la página 8: *"Si el cliente realiza la consulta de cuántos puntos tiene acumulados desde una aplicación Android."*

Este plan va **más allá** de ese punto extra, por decisión explícita del equipo: en vez de un portal de solo consulta para el cliente, se construye un **panel operativo completo para el cajero/administrador**, equivalente en funcionalidad a Swing. Esto es una ampliación voluntaria del alcance, no un requisito del enunciado — **Swing sigue siendo la entrega oficial que se califica** y no se modifica ni se retira.

### 1.2 Decisiones de alcance confirmadas

| Decisión | Resuelto como |
|---|---|
| ¿Quién usa el sitio web? | El cajero/administrador. El cliente final **no** tiene acceso por ahora. |
| ¿Qué puede hacer desde el sitio? | Todo lo que hoy hace en Swing: gestionar clientes, catálogo (productos/menús), inventario, pedidos, cobro, recompensas y canjes. |
| ¿Requiere login? | **No por ahora.** Mismo nivel de confianza que Swing hoy (quien tiene acceso a la red/URL puede operar). Si el sitio se expone fuera de una red confiable, esto debe revisarse antes de usarlo en producción. |
| ¿Se modifica Swing o las clases `Servicio`/`DAO`? | No. El panel web es un segundo consumidor de las mismas clases `Servicio` que ya usa Swing; no se reescribe lógica de negocio existente. |

### 1.3 Principio de no regresión

La integración de Spring Boot se diseña para **no romper** la aplicación Swing existente ni las 61 pruebas unitarias actuales. Los controladores REST **solo delegan** a las clases `Servicio` ya probadas — está prohibido reescribir reglas de negocio dentro de un controlador.

---

## 2. Arquitectura de coexistencia (Swing + Spring Boot)

```text
                          ┌─────────────────────────────┐
                          │  Navegador (cajero/admin)   │
                          └──────────────┬──────────────┘
                                         │ HTTP (JSON)
                                         ▼
┌───────────────────────────┐     ┌─────────────────────────────┐
│    Escritorio (Swing)     │     │      API Spring Boot        │
│  (VentanaPrincipal, etc.) │     │   (@RestController REST)    │
└─────────────┬─────────────┘     └──────────────┬──────────────┘
              │                                  │
              └─────────────────┬────────────────┘
                                │ Invocan los mismos métodos
                                ▼
                  ┌───────────────────────────────┐
                  │        Capa de Servicios      │
                  │ ClienteServicio, PedidoServicio,│
                  │ VentaServicio, PuntosYCanjesServicio, ... │
                  └───────────────┬───────────────┘
                                  │
                                  ▼
                  ┌───────────────────────────────┐
                  │          Capa DAO             │
                  │   (ClienteDAO, PedidoDAO...)  │
                  └───────────────┬───────────────┘
                                  │ JDBC (pool de conexiones)
                                  ▼
                  ┌───────────────────────────────┐
                  │        Oracle Database        │
                  └───────────────────────────────┘
```

Diferencia clave frente a la versión anterior de este plan: antes cada `Servicio` se construía con una única `Connection` por operación (igual que hace Swing). Para atender varias peticiones HTTP concurrentes, la capa de configuración de Spring Boot obtiene una conexión de un **pool** (HikariCP, incluido por defecto) por cada petición, y construye el `Servicio` correspondiente con esa conexión — sin cambiar la firma de ningún `Servicio` ni `DAO` existente.

---

## 3. Plan de fases

| Fase | Descripción | Entregable clave | Estimación |
|---|---|---|:---:|
| **Fase A** | Configuración de dependencias y entorno | `pom.xml` actualizado con dependencias Web + pool de conexiones | 1 sesión |
| **Fase B** | Configuración de Spring, pool de conexiones y fábrica de servicios | `ApiApplication`, `AppConfig` (DataSource/HikariCP) | 1 sesión |
| **Fase C** | Controladores REST — módulo Clientes y Catálogo | `ClienteController`, `ProductoController`, `MenuController`, `InventarioController` | 1-2 sesiones |
| **Fase D** | Controladores REST — módulo Pedidos, Cobro y Fidelización | `PedidoController`, `VentaController`, `PuntosYCanjesController` | 1-2 sesiones |
| **Fase E** | Manejo global de errores | `GlobalExceptionHandler` mapeando excepciones de negocio a códigos HTTP | 1 sesión |
| **Fase F** | Panel web (frontend) para el cajero | Dashboard en `/resources/static`: clientes, catálogo, pedidos, cobro, fidelización | 2-3 sesiones |
| **Fase G** | Documentación de API y pruebas de integración | Swagger/OpenAPI o colección Postman; pruebas manuales de cada flujo | 1 sesión |

---

## 4. Detalle de implementación por fase

### Fase A: Configuración Maven (`pom.xml`)
- [ ] Incorporar el BOM de Spring Boot (`spring-boot-dependencies`) en `<dependencyManagement>` para no alterar la versión padre de Maven ni el plugin Shade actual.
- [ ] Agregar dependencias:
  - `spring-boot-starter-web` (servidor embebido Tomcat y serialización Jackson JSON).
  - `spring-boot-starter-jdbc` (incluye HikariCP para el pool de conexiones).
  - `spring-boot-starter-validation` (validación de los DTOs de entrada en los endpoints de escritura).
- [ ] Configurar los perfiles o el `exec-maven-plugin` para poder ejecutar independientemente:
  - `com.panpuntospremios.Main` (app Swing de escritorio, sin cambios).
  - `com.panpuntospremios.api.ApiApplication` (servidor API Spring Boot).

### Fase B: Configuración de beans e infraestructura
- [ ] Crear el paquete `com.panpuntospremios.api`.
- [ ] Crear la clase principal `ApiApplication.java` anotada con `@SpringBootApplication`.
- [ ] Crear `AppConfig.java` (`@Configuration`) que registre un `DataSource` (HikariCP) usando los mismos parámetros de conexión que hoy lee [`ConexionOracle.java`](src/main/java/com/panpuntospremios/util/ConexionOracle.java) desde `.env`/wallet, sin modificar esa clase (la sigue usando Swing tal cual).
- [ ] Crear un componente por petición (`@RequestScope` o fábrica equivalente) que tome una `Connection` del pool y construya los `Servicio` necesarios (`ClienteServicio`, `ProductoServicio`, `MenuServicio`, `InventarioServicio`, `PedidoServicio`, `VentaServicio`, `PuntosYCanjesServicio`) con esa conexión, cerrándola al terminar la petición.
- [ ] Configurar `application.properties` en `src/main/resources/`: puerto `8080`, tamaño del pool HikariCP, CORS deshabilitado o restringido a la red interna (no hay autenticación, así que no debe exponerse a Internet sin revisar esto primero).

### Fase C: Controladores REST — Clientes y catálogo

Todos delegan directamente en el `Servicio` ya existente y probado; no se duplica validación.

* **Clientes** (`ClienteServicio`):
  - `GET /api/clientes` → `listarActivos()`
  - `GET /api/clientes/{identificador}` → `buscarPorIdentificador()`
  - `POST /api/clientes` → `registrar()`
  - `PUT /api/clientes/{id}` → `actualizar()`
  - `POST /api/clientes/{id}/desactivar` → `desactivar()`
* **Productos** (`ProductoServicio`):
  - `GET /api/productos`, `GET /api/productos/{id}` → `listarActivos()`, `buscarPorId()`
  - `POST /api/productos`, `PUT /api/productos/{id}`, `POST /api/productos/{id}/desactivar` → `registrar()`, `actualizar()`, `desactivar()`
* **Menús** (`MenuServicio`):
  - `GET /api/menus` → `listarActivos()`
  - `POST /api/menus`, `PUT /api/menus/{id}`, `POST /api/menus/{id}/desactivar` → `registrar()`, `actualizar()`, `desactivar()`
* **Inventario** (`InventarioServicio`):
  - `GET /api/inventario/movimientos` → `listarMovimientos()`
  - `POST /api/inventario/abastecer` → `abastecer(idProducto, cantidad)`

### Fase D: Controladores REST — Pedidos, cobro y fidelización

* **Pedidos** (`PedidoServicio`):
  - `POST /api/pedidos` → `crearPedido(pedido, items)`
  - `GET /api/pedidos/{id}` → `buscarPorId()`
  - `GET /api/pedidos/{id}/detalle` → `listarDetalle()`
  - `GET /api/clientes/{idCliente}/pedidos/pendientes` → `listarPendientesPorCliente()`
  - `GET /api/clientes/{idCliente}/pedidos` → `listarPorCliente()` (historial completo)
  - `POST /api/pedidos/{id}/anular` → `anular()`
* **Cobro** (`VentaServicio`):
  - `POST /api/pedidos/{id}/cobrar` → `cobrar(idPedido, metodo, recibido, tarjetaAprobada)`
  - `GET /api/pedidos/{id}/comprobante` → `comprobante()`
  - `POST /api/pedidos/{id}/entregar` → `entregar()`
* **Puntos y canjes** (`PuntosYCanjesServicio`):
  - `GET /api/recompensas` → `listarRecompensasActivas()`
  - `POST /api/recompensas`, `PUT /api/recompensas/{id}`, `POST /api/recompensas/{id}/desactivar` → `registrarRecompensa()`, `actualizarRecompensa()`, `desactivarRecompensa()`
  - `POST /api/clientes/{idCliente}/canjes` (body: `idRecompensa`) → `canjear()`
  - `GET /api/clientes/{idCliente}/puntos/historial` → `listarMovimientosPuntos()`
  - `GET /api/clientes/{idCliente}/canjes` → `listarCanjesPorCliente()`

### Fase E: Manejo global de errores
- [ ] Crear `GlobalExceptionHandler.java` (`@RestControllerAdvice`) que capture `ValidacionException`, `StockInsuficienteException`, `PuntosInsuficientesException`, `PagoRechazadoException` y errores `SQLException`, devolviendo JSON estructurado (`{"error": "..."}`) con el código HTTP adecuado (400 para validaciones y negocio, 404 para no encontrado, 500 para fallos inesperados).

### Fase F: Panel web (frontend para el cajero)
Ubicación: `src/main/resources/static/`
- [ ] `index.html` + rutas simples por sección (o una sola página con pestañas, igual que `FormPuntosYCanjes`):
  - **Clientes:** buscar, registrar, editar, ver saldo e historial de puntos/pedidos.
  - **Catálogo:** administrar productos y menús.
  - **Inventario:** abastecer y ver historial de movimientos.
  - **Pedidos:** armar un pedido (buscar cliente, agregar productos/menús, confirmar).
  - **Cobro:** cobrar un pedido pendiente (efectivo/tarjeta), ver comprobante, marcar entregado.
  - **Fidelización:** catálogo de recompensas, canjear, historial.
- [ ] `app.js`: funciones con `fetch()` contra los endpoints `/api/...`.
- [ ] `styles.css`: estilos visuales simples y responsivos.
- [ ] Sin pantalla de login (ver decisión de alcance en la sección 1.2).

### Fase G: Documentación y pruebas de integración
- [ ] Documentar los endpoints (Swagger/OpenAPI o una colección Postman exportada en el repo).
- [ ] Probar manualmente cada flujo completo desde el panel web: cliente → pedido → cobro → puntos → canje → reportes básicos, igual que se hizo con Swing.

---

## 5. Matriz de riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|---|:---:|---|
| Conflicto entre dependencias de Spring Boot y el plugin Shade de Maven | Alto | Usar `<dependencyManagement>` en vez de declarar `spring-boot-starter-parent` como parent directo del proyecto. |
| Sin autenticación: cualquiera con acceso a la URL puede operar el negocio | Alto si se expone fuera de una red confiable | Decisión aceptada para esta fase bajo el supuesto de red interna/confiable, igual que Swing hoy. Revisar antes de exponer el sitio fuera de esa red; agregar login sería trabajo adicional no cubierto por este plan. |
| Múltiples peticiones HTTP concurrentes compitiendo por conexiones/bloqueos de Oracle | Medio | Pool de conexiones (HikariCP) con tamaño acotado; los `Servicio` ya usan `SELECT ... FOR UPDATE` en orden consistente (visto en `PedidoServicio`, `VentaServicio`, `PuntosYCanjesServicio`), así que el mismo mecanismo que evita condiciones de carrera en Swing aplica aquí. |
| Modificación accidental de reglas de negocio en los controladores | Alto | Prohibido reescribir lógica en los controladores: solo delegan a las clases `Servicio` ya probadas. Revisión de código antes de mezclar a `main`. |
| Entrada JSON inválida o incompleta en endpoints de escritura | Medio | DTOs de petición con `spring-boot-starter-validation`; los `Servicio` ya validan de nuevo internamente (`ValidacionException`), así que hay doble resguardo. |

---

## 6. Checklist de validación final

- [ ] `mvn clean test` sigue ejecutando las 61 pruebas unitarias existentes con 0 errores (nada de lo agregado las toca).
- [ ] La aplicación Swing abre normalmente y ejecuta todas sus funciones previas, sin cambios de comportamiento.
- [ ] `mvn spring-boot:run` (o ejecución de `ApiApplication`) inicia el servidor en el puerto `8080` sin interferir con la ejecución de Swing.
- [ ] Desde el panel web se puede completar el flujo entero: registrar/buscar cliente → crear pedido → cobrar → ver comprobante → canjear una recompensa → ver historial de puntos y pedidos.
- [ ] Los errores de negocio (stock insuficiente, puntos insuficientes, pago rechazado) se muestran en el panel web con un mensaje claro, no como un error genérico.
- [ ] Se documentó que el sitio no tiene login y se recomienda restringir su acceso a la red interna del negocio.

---

## 7. Recomendaciones y observaciones técnicas clave para la ejecución

Para garantizar una implementación fluida sin afectar el proyecto principal ni las entregas académicas, tomar en cuenta los siguientes tres puntos:

### 7.1 Empaquetado y plugins de Maven (`spring-boot-maven-plugin` vs `maven-shade-plugin`)
* **Situación:** El `pom.xml` actual utiliza `maven-shade-plugin` configurado para compilar y empaquetar el JAR ejecutable de escritorio apuntando a la clase principal `com.panpuntospremios.Main` (Swing).
* **Precaución:** Si se añade `spring-boot-maven-plugin` al ciclo de vida estándar de Maven en `<build><plugins>`, su objetivo `repackage` puede sobreescribir el empaquetado del JAR principal e imponer el cargador de clases de Spring Boot, alterando la ejecución de escritorio.
* **Solución recomendada:** No asociar el goal `repackage` al flujo general de `mvn package`. Utilizar perfiles de Maven (`<profiles>`), o ejecutar la API en desarrollo mediante `exec-maven-plugin` o directamente ejecutando la clase principal `ApiApplication.main()` desde el IDE.

### 7.2 Ciclo de vida y retorno de conexiones al pool (HikariCP)
* **Situación:** Las implementaciones DAO (`ClienteDAOImpl`, `PedidoDAOImpl`, etc.) reciben una instancia de `java.sql.Connection` en su constructor, desacopladas del pool.
* **Precaución:** En un entorno web con peticiones HTTP concurrentes, si una conexión tomada del pool de HikariCP para atender un request no se cierra, se producirá una fuga de conexiones (*connection leak*) que agotará rápidamente el límite de conexiones de Oracle Autonomous Database.
* **Solución recomendada:** El componente encargado de proveer la conexión por petición (ya sea un bean con `@RequestScope`, un `HandlerInterceptor` o un filtro HTTP) debe garantizar invariablemente el cierre (`connection.close()`) dentro de un bloque `finally` al terminar el ciclo de vida del request HTTP, devolviéndola limpiamente al pool.

### 7.3 Prioridad académica con respecto a la entrega oficial (Fase 8 vs Panel Web)
* **Situación:** El documento del curso (`Proyecto_Pan_Puntos_y_Premios.pdf`) estipula que la interfaz formalmente calificada y obligatoria es Java + Swing + Oracle. La API Spring Boot y el panel web representan una extensión de valor agregado no exigida en la rúbrica base.
* **Precaución:** En `PLAN_IMPLEMENTACION.md`, la **Fase 8 (Reportes y entrega)** aún está catalogada como pendiente para la aplicación Swing.
* **Solución recomendada:** Garantizar primero la implementación, pruebas y verificación completa de los reportes obligatorios en Swing antes de dedicar esfuerzos intensivos al desarrollo web, asegurando la cobertura del 100% de los criterios de evaluación de la materia.

