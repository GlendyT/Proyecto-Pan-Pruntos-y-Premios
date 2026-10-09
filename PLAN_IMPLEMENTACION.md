# Plan de implementación y seguimiento — Pan, Puntos y Premios

Fecha de creación: 2026-09-20.

Estado general: fases 2 a 7 completadas; fases 1 y 8 en curso. Fases 6 y 7 verificadas en pruebas aisladas; fase 6 además se verificó en Oracle. Auditoría actualizada el 2026-10-04.

Este documento registra el alcance previsto, los avances, las verificaciones y las decisiones pendientes. Las marcas reflejan los archivos presentes y las verificaciones disponibles en el repositorio al 2026-10-04.

## Cómo llevar el seguimiento

- Marcar `[x]` únicamente cuando una tarea esté implementada y verificada.
- Registrar cada avance en la bitácora con fecha, archivos afectados, verificación y pendientes.
- Usar los estados: pendiente, en curso, bloqueada y completada. Documentar el motivo de cualquier bloqueo.
- Actualizar este plan cuando cambie el alcance y dejar constancia de la decisión.
- No registrar contraseñas, contenido del wallet ni otros secretos.

## Diagnóstico inicial

- [x] Revisar `README.md` y `diseno_pan_puntos_premios.md`.
- [x] Revisar `pom.xml`, `Main.java` y `ConexionOracle.java`.
- [x] Revisar `sql/ddl.sql` y los datos iniciales.
- [x] Preparar el plan por fases.
- [x] Guardar el plan en la raíz del proyecto.
- [x] Obtener aprobación para iniciar los dos primeros puntos de la fase 1.

El `Main.java` actual inicia `VentanaPrincipal` en el hilo de eventos de Swing. `FormClientes` obtiene la conexión JDBC configurada mediante `.env` en `ConexionOracle.java`. El usuario aportó una ejecución que confirmó la conexión a Oracle; la auditoría del 2026-09-25 verificó además la compilación, pero no repitió operaciones de escritura contra la base.

El esquema local define 10 tablas: `cliente`, `producto`, `menu`, `menu_detalle`, `pedido`, `pedido_detalle`, `movimiento_inventario`, `movimiento_puntos`, `recompensa` y `canje`. El usuario confirmó que las tablas y columnas ya existen en Oracle. Los resultados aportados en `result.json` fueron revisados y concuerdan con el DDL en tablas, columnas, restricciones, valores predeterminados e identidades.

El proyecto ya contiene modelos, enums, excepciones, DAO y servicios para clientes, productos, menús, inventario, pedidos, cobros, fidelización y reportes, además de sus formularios Java Swing. La suite aislada actual contiene 67 pruebas automatizadas; las funciones o procedimientos almacenados siguen sin estar definidos en el SQL revisado.

## Arquitectura prevista

```text
src/main/java/com/panpuntospremios/
├── Main.java
├── modelo/       Objetos y cálculos del negocio
├── enums/        Categorías, estados y métodos de pago
├── dao/          Consultas y escrituras SQL
├── servicio/     Operaciones y transacciones
├── excepcion/    Errores del negocio
├── vista/        Ventanas y formularios Swing
└── util/         Conexión y utilidades

src/test/java/com/panpuntospremios/
sql/
```

Las vistas invocarán servicios; los servicios coordinarán las reglas y los DAO. Las operaciones que deban ser atómicas compartirán una conexión y una transacción. Los nombres de archivos listados abajo son los previstos y podrán ajustarse dejando constancia.

## Resumen de fases

| Fase | Alcance | Estado | Dependencias |
|---|---|---|---|
| 1 | Base técnica y esquema | En curso (3 de 8 tareas verificadas) | Inicio autorizado |
| 2 | Modelos y reglas | Completada (8 de 8 tareas verificadas) | Fase 1 |
| 3 | Clientes y primera pantalla | Completada (7 de 7 tareas verificadas) | Fase 2 |
| 4 | Productos, menús e inventario | Completada (9 de 9 tareas verificadas) | Fases 2 y 3 |
| 5 | Pedidos | Completada (9 de 9; verificación aislada) | Fases 3 y 4 |
| 6 | Cobro y acumulación de puntos | Completada (12 de 12 tareas verificadas) | Fase 5 |
| 7 | Recompensas y canjes | Completada (10 de 10 tareas verificadas) | Fases 4 y 6 |
| 8 | Reportes y entrega | En curso (7 de 11 tareas verificadas; alcance provisional confirmado por el usuario) | Fases anteriores |

## Fase 1. Base técnica y esquema

- [x] Verificar qué tablas, columnas y restricciones ya existen en Oracle.
- [ ] Crear `sql/verificar_esquema.sql` para revisar la estructura esperada. **Se reportó como creado y ejecutado, pero el archivo no está presente ni registrado en Git; debe recuperarse o recrearse.**
- [x] Preparar ajustes incrementales necesarios conservando los datos existentes; no ejecutar de nuevo todo el DDL sobre tablas existentes. **La verificación histórica no encontró diferencias que requirieran ajustes; por tanto, no correspondía generar ni ejecutar scripts de alteración.**
- [ ] Separar los datos de prueba del script de creación del esquema.
- [ ] Resolver la coherencia entre existencias iniciales e historial: el SQL actual carga existencias sin movimientos de abastecimiento. Revisar los datos reales antes de preparar una regularización.
- [ ] Mejorar los mensajes de configuración faltante en `ConexionOracle.java`.
- [ ] Revisar Maven, la versión Java documentada, el empaquetado y la plantilla `.env.example`.
- [x] Verificar compilación, conexión y compatibilidad del esquema.

Archivos previstos: `sql/verificar_esquema.sql`, scripts de ajustes y datos de prueba, `ConexionOracle.java`, `pom.xml`, `.env.example` y documentación de configuración.

Criterio de cierre: conexión comprobada y esquema compatible, con evidencia de verificaciones y conservación de los datos existentes.

Auditoría detallada del 2026-09-25:

1. **Verificación del esquema — completa.** La salida aportada registró 107 comprobaciones con estado `OK`.
2. **Script repetible de verificación — pendiente.** `sql/verificar_esquema.sql` no existe en el árbol actual ni en el historial de Git, aunque hay evidencia de que se utilizó fuera del repositorio.
3. **Ajustes incrementales — completa para el esquema verificado.** Al no encontrarse diferencias entre Oracle y el DDL esperado, no había ajustes que preparar. Si el esquema cambia, deberá repetirse la comparación antes de crear migraciones.
4. **Separación de datos de prueba — pendiente.** `sql/ddl.sql` todavía mezcla las diez sentencias `CREATE TABLE` con los `INSERT` de clientes, productos, menú y recompensas.
5. **Coherencia de existencias e historial — pendiente.** Los productos de ejemplo se insertan con existencias positivas, pero no se insertan movimientos de abastecimiento en `movimiento_inventario`.
6. **Mensajes por configuración faltante — pendiente.** `ConexionOracle` permite que `.env` falte, pero pasa valores nulos a `DriverManager` sin indicar qué variable (`DB_URL`, `DB_USER` o `DB_PASSWORD`) debe configurarse.
7. **Maven, Java, empaquetado y plantilla — pendiente parcialmente.** `mvn package` genera correctamente el JAR sombreado y su manifiesto contiene `Main-Class: com.panpuntospremios.Main`; `pom.xml` y el README ya coinciden en la compatibilidad con Java 17 o superior. Durante esta auditoría se retiró `.env.example` de `.gitignore`, pero el archivo sigue sin estar registrado en Git y su `DB_URL` está incompleto (`jdbc:oracle:thin:`), por lo que todavía no constituye una plantilla reproducible para un clon nuevo.
8. **Compilación, conexión y compatibilidad — completa.** `mvn compile`, `mvn test` y `mvn package` finalizaron con `BUILD SUCCESS`; la conexión y la compatibilidad estructural cuentan con la evidencia aportada previamente. Desde el cierre de las fases 2 y 3 también existen pruebas automatizadas aisladas y una integración opcional con Oracle.

Evidencia histórica de los puntos 1 y 2: el usuario ejecutó el script y aportó `result.json` (salida tabular de texto, aunque su extensión sea `.json`). La revisión encontró 10 tablas, 63 columnas y 34 restricciones con nombre con estado `OK` (107 comprobaciones). Las restricciones figuran habilitadas y validadas. El inventario de 104 filas incluye las 10 claves primarias, las tres claves únicas esperadas, las referencias de claves foráneas y los CHECK acordes con el DDL. También se revisaron los DEFAULT de las 63 columnas y las nueve identidades `ALWAYS`. No se detectaron errores Oracle en la salida. Esta evidencia verifica estructura; no certifica todavía los datos de negocio ni completa el resto de la fase 1. En la auditoría del 2026-09-25 se comprobó que `sql/verificar_esquema.sql` no está en el árbol de trabajo ni en el historial de Git, por lo que la tarea de conservar ese script vuelve a quedar pendiente.

Cuando se recupere o recree `sql/verificar_esquema.sql`, se deberá ejecutar en el cliente SQL conectado como propietario de las tablas y conservarlo en Git para que cualquier integrante pueda repetir la verificación.

## Fase 2. Modelos y reglas

- [x] Crear `Cliente`, `Producto`, `Menu`, `Pedido` y `Recompensa`.
- [x] Crear `ItemVenta`, `ProductoIndividual` y `MenuCompleto`, aplicando herencia y polimorfismo.
- [x] Crear los modelos de detalle, movimientos y canjes necesarios.
- [x] Crear enums para categorías, estados, métodos de pago y tipos de movimiento.
- [x] Crear excepciones para validaciones, inventario insuficiente, puntos insuficientes y pago rechazado.
- [x] Implementar importes con `BigDecimal` y validar cantidades positivas.
- [x] Implementar el cálculo de puntos: 2 por sándwich individual, 8 por menú completo y 0 por bebida o acompañamiento individual.
- [x] Probar totales y puntos de pedidos mixtos, cantidades inválidas y límites relevantes.

Archivos previstos: clases en `modelo/`, `enums/`, `excepcion/` y pruebas de reglas en `src/test/java/`.

Criterio de cierre: cálculos y validaciones comprobados sin depender de la interfaz ni de una conexión a Oracle.

Verificación del 2026-09-25: `ItemVenta` conserva los importes con `BigDecimal`, exige cantidades mayores que cero y rechaza precios nulos o negativos mediante `ValidacionException`. Se agregó `ItemVentaTest` con seis pruebas para pedido mixto, precisión decimal, reglas de puntos, cantidades inválidas, precios inválidos y una cantidad alta. `mvn test` terminó con `BUILD SUCCESS`: 6 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas. La fase cumple su criterio de cierre sin interfaz ni conexión a Oracle.

## Fase 3. Clientes y primera pantalla

- [x] Crear `ClienteDAO` y su implementación JDBC con `PreparedStatement`.
- [x] Crear `ClienteServicio` para registro, búsqueda, edición y desactivación.
- [x] Validar identificadores duplicados, datos obligatorios y formatos de identificador, nombre, teléfono y correo.
- [x] Crear `VentanaPrincipal` y `FormClientes`.
- [x] Adaptar `Main.java` para iniciar Swing.
- [x] Ejecutar operaciones de base de datos en segundo plano y actualizar la interfaz en el hilo de Swing.
- [x] Verificar que los cambios se conserven al cerrar y volver a abrir la aplicación.

Criterio de cierre: administrar clientes desde la interfaz con persistencia en Oracle y mensajes comprensibles de error.

Verificación del 2026-09-25: `FormClientes` usa `SwingWorker` para conexión, consultas y escrituras JDBC; los resultados y mensajes se procesan en `done()`, dentro del hilo de eventos de Swing. Los controles se deshabilitan durante cada operación y la conexión se cierra al cerrar el formulario. El formulario impide introducir números en el nombre, letras en el teléfono y un signo como primer carácter del identificador; el correo se valida al guardar. `ClienteServicio` aplica las reglas completas tanto al registrar como al editar, evitando que puedan omitirse desde otra interfaz. `ClienteDAOPersistenciaTest` verifica registro, edición y desactivación mediante conexiones nuevas usando H2 en memoria, sin depender de Oracle ni modificar datos compartidos. La integración opcional `ClienteDAOOraclePersistenciaIT` registró un cliente temporal en Oracle, cerró y reabrió conexiones, verificó la edición y eliminó el mismo registro en `finally`. La suite aislada terminó con 16 pruebas correctas y la integración Oracle con 1 prueba correcta; ambas finalizaron con `BUILD SUCCESS`. La fase cumple su criterio de cierre.

## Fase 4. Productos, menús e inventario

- [x] Crear DAO y servicios de productos y menús.
- [x] Crear `InventarioServicio` y persistencia de movimientos.
- [x] Crear `FormProductos`, `FormMenus` y `FormInventario`.
- [x] Implementar registro, edición y desactivación de productos y menús.
- [x] Validar menús con un sándwich, una bebida y un acompañamiento.
- [x] Implementar abastecimiento con cantidad positiva.
- [x] Actualizar existencias y registrar movimientos dentro de la misma transacción.
- [x] Mostrar existencias e historial de movimientos.
- [x] Verificar que una operación fallida no deje cambios parciales.

Criterio de cierre: catálogo y menús administrables, con abastecimiento e historial coherentes.

Verificación del 2026-09-25: se implementaron `ProductoDAO`, `MenuDAO` y `MovimientoInventarioDAO` con sus implementaciones JDBC, además de `ProductoServicio`, `MenuServicio` e `InventarioServicio`. Los menús exigen exactamente un producto activo de cada categoría. El abastecimiento bloquea el producto con `SELECT ... FOR UPDATE` y actualiza existencia y movimiento mediante `Transaccion`; una prueba fuerza el fallo del movimiento y confirma que la existencia vuelve a su valor original. `FormProductos`, `FormMenus` y `FormInventario` ejecutan JDBC en segundo plano, permiten administrar el catálogo y muestran existencias e historial. `mvn test` terminó con 24 pruebas correctas. La integración opcional `CatalogoInventarioOracleIT` creó productos, menú, componentes y abastecimiento temporales en Oracle, verificó los datos tras reconectar y los eliminó al finalizar; terminó con `BUILD SUCCESS`. La fase cumple su criterio de cierre.

## Fase 5. Pedidos

- [x] Crear `PedidoDAO` y `PedidoServicio`, incluyendo persistencia del detalle.
- [x] Crear `FormPedido` con búsqueda de cliente y selección de productos y menús.
- [x] Permitir agregar, quitar ítems y cambiar cantidades, actualizando total y puntos previstos en vivo.
- [x] Consolidar las cantidades requeridas por producto, incluyendo componentes de menús.
- [x] Validar disponibilidad al confirmar el pedido.
- [x] Guardar cabecera y detalle de forma atómica como pedido `PENDIENTE`, sin descontar inventario ni acreditar puntos.
- [x] Consultar pedidos pendientes y recuperar su detalle e importes guardados.
- [x] Implementar la anulación de pedidos pendientes conforme a la decisión de alcance.
- [x] Verificar pedidos mixtos, falta de existencias y persistencia del detalle.

Criterio de cierre: crear y consultar pedidos pendientes completos desde la interfaz.

Correcciones del 2026-09-30: el servicio recalcula importes y puntos desde los ítems revalidados; consulta cliente, productos y menús vigentes; rechaza precios modificados desde la carga del catálogo. Bloquea cliente, menús y productos (estos últimos por ID creciente), consolida existencias y revierte errores SQL y de ejecución. Rechaza transacciones externas para evitar confirmar trabajo ajeno. La anulación bloquea la misma fila que el cobro. Los formularios usan `SwingWorker` y conexiones por operación con `try-with-resources`, cierran recursos incluso si se cierra la ventana y actualizan Swing en el hilo de eventos. Se incorporó el botón **Cambiar cantidad**.

Verificación: las cinco pruebas originales de pedidos y las once de `PedidoFlujoTest` pasan. Se cubren pedido mixto, conservación del inventario y saldo, recuperación tras cerrar y reabrir conexión, totales recalculados, stock compartido entre menú y producto, catálogo desactualizado, estados inválidos y rollback tras insertar cabecera y detalles. Las pruebas nuevas ejecutan `sql/ddl.sql` exclusivamente en H2. Los formularios se compilaron y revisaron; no se realizó una sesión visual ni se ejecutaron estas operaciones contra Oracle compartido.

## Fase 6. Cobro y acumulación de puntos

- [x] Crear `VentaServicio` y `FormCobro`.
- [x] Implementar efectivo con validación de importe recibido y cálculo de cambio.
- [x] Implementar tarjeta con aprobación o rechazo simulado.
- [x] Revalidar estado del pedido y existencias al cobrar.
- [x] Descontar inventario de productos y componentes de menús.
- [x] Registrar movimientos de inventario y puntos, actualizar saldo y marcar el pedido `PAGADO` en una sola transacción.
- [x] Evitar cobros duplicados y proteger existencias y saldos frente a operaciones concurrentes.
- [x] Implementar comprobante con los datos persistidos de la venta.
- [x] Incorporar la transición de `PAGADO` a `ENTREGADO`.
- [x] Probar pago rechazado, falta de stock, cobro repetido y rollback ante fallos.
- [x] Abrir los formularios de pedidos, cobro y comprobante, y confirmar que cargan datos de Oracle. **Confirmado por el usuario el 2026-09-30.**
- [x] Completar un cobro de prueba en Oracle y comprobar tras reconectar estado, método/referencia, existencias, movimientos de inventario, saldo y movimiento de puntos; consultar el comprobante persistido. Verificar también rechazo simulado y stock insuficiente sin cambios parciales. **Confirmado por el usuario el 2026-09-30.**

Criterio de cierre: venta completa y atómica, con inventario, puntos, pago y comprobante coherentes.

Implementación del 2026-09-30: `VentaDAO` concentra bloqueos y escrituras de pago y puntos; `VentaServicio` coordina la transacción y `ComprobanteVenta` presenta el pedido y detalle guardados. `FormCobro` se abre desde **Pedidos → Cobro y comprobantes** o desde un pedido pendiente. Permite consultar por ID, cobrar, recuperar el comprobante y marcar entregado; utiliza JDBC en segundo plano y cierre por operación.

Decisiones de implementación:

- Se conservan los precios y puntos guardados al confirmar el pedido; no se recalculan con precios actuales al cobrar. Se valida su coherencia con el detalle.
- El esquema existente se conserva. `referencia_pago` almacena `RECIBIDO:<importe>` para efectivo (el cambio se deriva del total persistido) o `SIM-<UUID>` para tarjeta simulada. No se solicitan ni guardan datos de tarjeta. El comprobante identifica cliente e ítems por sus IDs guardados y muestra la fecha del pedido, pues el esquema no tiene fecha de pago separada.
- Los movimientos de inventario registran cantidades positivas de salida con tipo `VENTA` y existencia resultante; los de puntos usan `ACUMULACION` y referencia `PEDIDO:<id>`.
- Un menú con pedidos pendientes no puede cambiar sus componentes desde el servicio de catálogo. Puede cambiar nombre/precio, que no altera los importes del pedido. Se permite cambiar componentes después de cobrar o anular los pendientes. La confirmación y el cobro bloquean el menú para coordinarse con su edición.
- La entrega solo admite `PAGADO → ENTREGADO` y no vuelve a mover inventario ni puntos. La anulación solo admite pedidos pendientes.
- El saldo y las existencias actuales son el punto de partida. La regularización histórica del inventario inicial continúa pendiente en fase 1; no se inventaron movimientos históricos ni se modificó Oracle.

Verificación: `VentaServicioTest` aporta 16 pruebas de efectivo exacto/con cambio/inválido, tarjeta aprobada/rechazada, stock insuficiente después de descontar otro producto, rollback por fallo en puntos o estado final, desbordamiento del saldo, cobro repetido, entrega y comprobante persistido. Las pruebas con dos conexiones/hilos cubren doble cobro, competencia por stock entre clientes, acumulación simultánea del mismo cliente y anulación frente a cobro. Suite automatizada: 56 pruebas, 0 fallos, 0 errores, 0 omitidas. El usuario confirmó que los formularios abren y cargan datos de Oracle, y posteriormente confirmó que completó y verificó el flujo de cobro, incluido el rechazo y la falta de stock sin cambios parciales, y la persistencia del pago, inventario, puntos y comprobante tras reconectar. Fase 6 completada.

## Fase 7. Recompensas y canjes

- [x] Crear DAO de recompensas, canjes y movimientos de puntos.
- [x] Crear `PuntosYCanjesServicio` y `FormPuntosYCanjes`.
- [x] Permitir administrar el catálogo de recompensas y sus costos en puntos.
- [x] Mostrar saldo, recompensas disponibles e historial de puntos del cliente.
- [x] Validar cliente/recompensa activos, puntos suficientes y disponibilidad del producto o componentes del menú.
- [x] Descontar puntos e inventario y registrar canje y movimientos en una misma transacción.
- [x] Proteger el saldo y las existencias frente a canjes concurrentes.
- [x] Integrar consultas de historial de pedidos y puntos en la pantalla de clientes de forma segura. **Se cargan en serie dentro de una tarea y las tareas encadenadas preservan su seguimiento; el cierre cancela el trabajo y libera la conexión.**
- [x] Probar canje exitoso, puntos insuficientes y stock insuficiente.
- [x] Probar rollback ante un fallo forzado después de que la transacción haya escrito cambios.

Criterio de cierre: canjes e historiales funcionales, sin saldos negativos ni cambios parciales.

Correcciones y verificación del 2026-10-04: las consultas de ambos historiales se agruparon en una sola tarea secuencial. El seguimiento conserva correctamente trabajadores encadenados y cierra conexiones si la ventana se cierra mientras conecta o consulta. Se añadió un fallo SQL forzado en el movimiento del primer producto de un menú; la prueba verifica que se reviertan existencias, saldo, movimientos y canje. `PuntosYCanjesServicioTest` ahora tiene seis pruebas, incluida la concurrencia de dos canjes con puntos justos. La fase 7 queda completada.

## Fase 8. Reportes y entrega

- [x] Confirmar el listado exacto de reportes obligatorios del enunciado. El archivo `Proyecto_Pan_Puntos_y_Premios.pdf` se encuentra en la raíz del proyecto y en la Sección 14 (Pág. 7) detalla los 6 reportes obligatorios a visualizar mediante `JTable`:
  - **01. Ventas por período:** ventas pagadas dentro de un rango de fechas y el total vendido desglosado por efectivo y tarjeta. *(Actualmente `FormReportes` lista filas individuales; falta cálculo consolidado/desglosado de totales).*
  - **02. Productos más vendidos:** productos según la cantidad de unidades vendidas, desglosando sándwiches individuales y los componentes de los menús cuando corresponda. *(Pendiente de implementar consulta y vista).*
  - **03. Productos agotados o con bajo inventario:** productos cuya existencia sea cero o menor o igual a cinco unidades (`existencia <= 5`). *(Actualmente se lista todo el inventario; falta filtro específico).*
  - **04. Puntos acumulados por cliente:** saldo actual de puntos de cada cliente. *(Existe en `FormClientes`; debe incorporarse en `FormReportes` como consulta dedicada).*
  - **05. Historial de canjes:** canjes realizados, identificando cliente, fecha, recompensa y puntos utilizados. *(Existe por cliente en `FormPuntosYCanjes`; falta consulta global con nombre de recompensa en `FormReportes`).*
  - **06. Ventas de menús completos:** menús vendidos, las cantidades y los importes generados por cada menú. *(Pendiente de implementar consulta y vista).*
- [ ] Implementar en `ReporteDAO` y `ReporteDAOImpl` las consultas SQL para los reportes faltantes (02, 03 filtro <= 5, 04, 05 con recompensas, 06 y resumen de 01).
- [ ] Actualizar `ReporteServicio` con los nuevos tipos de reporte y validaciones correspondientes.
- [ ] Actualizar `FormReportes` para exponer en el combo los 6 reportes oficiales y presentarlos en `JTable`.
- [x] Probar el flujo completo: cliente → abastecimiento → pedido → cobro → puntos → canje → reportes. **Prueba aislada H2.**
- [x] Verificar consistencia entre reportes y datos persistidos. **Pruebas H2 comprueban pedidos, saldo, existencias y tipos de movimiento.**
- [x] Ejecutar pruebas de integración en un entorno de prueba separado de los datos compartidos del equipo. **H2 en memoria; no escribe en Oracle.**
- [ ] Comprobar errores de conexión y que la interfaz permanezca utilizable.
- [ ] Generar y ejecutar el JAR final.
- [x] Actualizar README con configuración, ejecución, pruebas y uso de reportes.
- [ ] Contrastar los entregables con el enunciado y registrar limitaciones pendientes.

Criterio de cierre: los 6 reportes obligatorios implementados y verificados en `FormReportes` mediante `JTable`, flujo completo verificado y aplicación ejecutable con documentación suficiente.

Implementación histórica del 2026-10-04 y actualización del 2026-10-08: en 2026-10-04 se definió un alcance provisional de 4 vistas tabulares. El 2026-10-08 se analizó `Proyecto_Pan_Puntos_y_Premios.pdf`, formalizando la lista de los 6 reportes obligatorios requeridos para la ponderación completa de la rúbrica (Criterio 8: 2 puntos).

## Cómo se verá y correrá la interfaz (Fase 3 en adelante)

La interfaz se construye con **Java Swing**, que es la librería de ventanas incluida en Java. No requiere instalar nada extra.

### Dónde se crea
Todos los formularios y ventanas van en el paquete `vista/`:
```
src/main/java/com/panpuntospremios/vista/
├── VentanaPrincipal.java   — ventana principal con el menú de navegación
├── FormClientes.java       — pantalla para registrar y buscar clientes
├── FormProductos.java      — pantalla para administrar productos
├── FormMenus.java          — pantalla para administrar menús y sus componentes
├── FormInventario.java     — pantalla para abastecer y ver historial
├── FormularioAsincrono.java — base reutilizable para JDBC fuera del hilo visual
├── FormPedido.java         — pantalla para crear pedidos
├── FormCobro.java          — pantalla para cobrar un pedido
├── FormFidelizacion.java   — pantalla para canjes y puntos
└── FormReportes.java       — pantalla para ver reportes en tabla
```

### Cómo se conecta todo
Cada formulario llama a un servicio, el servicio aplica las reglas del negocio y llama al DAO, y el DAO ejecuta el SQL contra Oracle:
```
Vista (formulario Swing)
  → Servicio (reglas del negocio)
    → DAO (consultas SQL)
      → Oracle Cloud
```

### Cómo se corre
El `Main.java` actual abre `VentanaPrincipal` mediante el hilo de eventos de Swing, en vez de limitarse a probar la conexión.

Opción 1 — compilar y ejecutar directamente con Maven (recomendada durante el desarrollo):
```bash
mvn compile exec:java
```

Opción 2 — generar el JAR y correrlo:
```bash
mvn package && java -jar target/pan-puntos-premios-1.0-SNAPSHOT.jar
```

Ambas opciones abren la ventana principal de escritorio. Los módulos implementados se encuentran en **Clientes**, **Catálogo e inventario** y **Pedidos** (nuevo pedido, pendientes, cobro y comprobantes).

`mvn exec:java` por sí solo ejecuta las clases que ya existan en `target/classes`, pero no garantiza que el código fuente se compile antes. Por ello, después de clonar el repositorio o modificar archivos Java se debe usar `mvn compile exec:java`. Una vez compilada la versión actual, `mvn exec:java` puede utilizarse para volver a ejecutarla sin recompilar. La opción con `mvn package` crea además el JAR ejecutable con sus dependencias.

### Diagnóstico: se ejecuta una versión antigua y no aparece la interfaz

Si el comando muestra únicamente un mensaje como `Conexion exitosa a Oracle Cloud` y termina con `BUILD SUCCESS`, se está ejecutando un `Main.class` antiguo que solo probaba la conexión. El `Main.java` actual inicia Swing y mantiene el proceso activo mientras la ventana esté abierta.

La solución normal es recompilar y ejecutar:

```bash
mvn compile exec:java
```

Los avisos de Maven sobre hilos `oracle.jdbc.diagnostics` que permanecen activos pueden proceder de esa antigua prueba JDBC; no son la causa de que falte la interfaz. Después de recompilar, la ventana principal debe aparecer y Maven permanecerá en ejecución hasta que se cierre.

### Comandos reproducibles de compilación y pruebas

Compilar sin ejecutar la aplicación:

```bash
mvn compile
```

Ejecutar las 61 pruebas aisladas, sin wallet ni conexión a Oracle:

```bash
mvn test
```

El comando equivalente usado por GitHub Actions es:

```bash
mvn --batch-mode --no-transfer-progress test
```

Generar el JAR sombreado; `package` también compila y ejecuta las pruebas aisladas:

```bash
mvn package
```

Las integraciones reales con Oracle están excluidas de la suite normal y deben habilitarse explícitamente. En PowerShell:

```powershell
mvn "-Dtest=ClienteDAOOraclePersistenciaIT" "-Doracle.it=true" test
mvn "-Dtest=CatalogoInventarioOracleIT" "-Doracle.it=true" test
```

Ambas integraciones usan datos temporales únicos y ejecutan su limpieza al finalizar. No deben añadirse al workflow de CI porque requieren `.env`, wallet y acceso a la base compartida.

### GitHub Actions

`.github/workflows/java-ci.yml` se activa únicamente para pull requests desde cualquier rama cuyo destino sea `development`. Usa JDK 17, caché de Maven, permisos de lectura y la suite aislada; no recibe secretos de Oracle.

---

## Decisiones y asuntos pendientes

| Asunto | Propuesta o situación | Estado |
|---|---|---|
| Interfaz | Java Swing, conforme al diseño existente | Implementada para clientes, productos, menús e inventario |
| Lógica de negocio | Servicios Java y DAO JDBC; no duplicar reglas entre capas | Implementada y probada para fases 2 a 4 |
| PL/SQL | Confirmar si el enunciado exige funciones o procedimientos almacenados | Pendiente |
| Reportes | Confirmados los 6 reportes obligatorios de la Sección 14 del PDF; actualizar DAO, servicio y FormReportes para cubrirlos formalmente | En curso |
| Anulaciones | Solo pedidos pendientes; devoluciones de ventas pagadas fuera de este alcance | Implementada con bloqueo frente a cobros |
| Entrega de pedidos | Botón Marcar entregado en cobro y comprobantes; solo desde PAGADO | Implementada y probada en H2 |
| Menús | Un componente de cada categoría; el esquema actual no garantiza por sí solo esta composición | Validado en `MenuServicio`; fase 4 completada |
| Inventario inicial | Datos de ejemplo con stock, pero sin movimientos de abastecimiento | Resolver en fase 1 |
| Cobro con tarjeta | Aprobación o rechazo seleccionable; referencia SIM, sin datos de tarjeta | Implementado y probado en H2 |
| Android/API | El diseño lo marca como extra; no forma parte de estas ocho fases | Opcional, fuera del alcance inicial |

## Bitácora de auditoría

| Fecha | Fase / actividad | Trabajo realizado | Archivos afectados | Verificación / evidencia | Pendientes |
|---|---|---|---|---|---|
| 2026-10-08 | Auditoría de requisitos del enunciado | Se analizó `Proyecto_Pan_Puntos_y_Premios.pdf` incorporado a la raíz del repositorio; se contrastaron los 6 reportes obligatorios de la Sección 14 con las consultas y vistas actuales; se formalizaron las tareas pendientes de la Fase 8 en el plan de implementación | `PLAN_IMPLEMENTACION.md` | Contraste analítico detallado del PDF contra el código Java y SQL | Implementar consultas SQL en DAO, servicio y opciones en `FormReportes` para los 6 reportes obligatorios |
| 2026-10-04 | Correcciones de fase 7 e inicio de fase 8 | Se serializaron las consultas de historial; se probó rollback después de una escritura; se implementaron consultas y pantalla de reportes provisionales | `FormPuntosYCanjes.java`, `PuntosYCanjesServicioTest.java`, DAO/servicio/modelo/formulario de reportes, `VentanaPrincipal.java`, `README.md`, `PLAN_IMPLEMENTACION.md` | `mvn clean test`: 67 pruebas, 0 fallos, 0 errores, 0 omitidas; incluye canje con rollback forzado y flujo integral de reportes en H2 | Confirmar reportes exactos con enunciado; revisión visual/Oracle de reportes; generar y ejecutar JAR final |
| 2026-10-04 | Auditoría de fase 7 en `aaf4761` | Comparación del plan con los DAO, servicio, vista y pruebas del commit | `PLAN_IMPLEMENTACION.md` | `mvn --batch-mode --no-transfer-progress clean test`: 61 pruebas correctas. Se confirmó que la prueba de stock no fuerza rollback tras escrituras y se detectó que las dos consultas de historial comparten conexión y estado de tarea | Serializar/aislar las cargas de historiales y añadir una prueba de rollback después de una escritura; fase 7 queda en curso |
| 2026-10-04 | Auditoría de fase 6 tras integrar fase 7 | Se revisaron servicio, bloqueo/concurrencia, UI, comprobante, pruebas y seguimiento | `PLAN_IMPLEMENTACION.md` | `mvn --batch-mode --no-transfer-progress test`: 61 pruebas, 0 fallos, 0 errores, 0 omitidas; 16 pruebas de cobro. El usuario había confirmado la verificación manual y contra Oracle el 2026-09-30 | No se encontraron defectos funcionales bloqueantes; se corrigieron los conteos y el resumen general desactualizados |
| 2026-09-30 | Cierre de fase 6 | Se registró el resultado de la validación del flujo completo de cobro en Oracle realizada por el usuario | `PLAN_IMPLEMENTACION.md`, `README.md` | El usuario confirmó cobro y comprobante persistidos tras reconectar, junto con estado, inventario y puntos; confirmó además rechazo y stock insuficiente sin cambios parciales. Suite aislada previa: 56 pruebas correctas | Fase 1 y fases 7–8 siguen pendientes |
| 2026-09-30 | Corrección de fase 5 e implementación de fase 6 | Integridad de fase 5; formularios asíncronos; cobro atómico, comprobantes, entrega y protección concurrente | Servicios y DAO de pedidos/ventas/menús; formularios; modelo de comprobante; pruebas; README y plan | Suite final: 56 pruebas correctas; Maven package genera el JAR. Pruebas aisladas con el DDL en H2, reconexión y dos conexiones concurrentes | Usuario confirmó apertura y carga de datos de las pantallas en Oracle. Pendiente cobro controlado y comprobar todos sus efectos tras reconectar; regularización histórica del inventario inicial permanece en fase 1 |
| 2026-09-30 | Auditoría de fase 5 | Revisión de servicio, DAO, formularios, navegación y cobertura; se corrigió el cierre prematuro de la fase | `PLAN_IMPLEMENTACION.md` | `mvn --batch-mode --no-transfer-progress test`: 29 pruebas, 0 fallos, 0 errores, 0 omitidas; cinco pruebas de pedidos. Hallazgos funcionales obtenidos por revisión estática; sin escrituras en Oracle | Corregir atomicidad y coherencia cabecera/detalle, completar pruebas de pedidos mixtos y rollback, resolver concurrencia de anulación y ciclo de vida de formularios antes de fase 6 |
| 2026-09-20 | Revisión inicial | Inspección del código, SQL, Maven y documentación; propuesta de ocho fases | Sin cambios durante la revisión | Dos clases Java existentes; diez tablas definidas en el DDL; módulos todavía documentados como ejemplos | Aprobar inicio y verificar esquema real |
| 2026-09-20 | Documentación del plan | Creación de este documento a solicitud del usuario | `PLAN_IMPLEMENTACION.md` | Plan con tareas, criterios de cierre, decisiones y bitácora | Implementación pendiente de aprobación |
| 2026-09-20 | Fase 1, puntos 1 y 2 | Inicio autorizado; usuario confirma tablas y columnas existentes; creación del script de verificación de solo lectura | `sql/verificar_esquema.sql`, `PLAN_IMPLEMENTACION.md` | Revisión estática de cobertura contra DDL: 10 tablas, 63 columnas, 34 restricciones con nombre; inventarios para revisión manual | Ejecutar consultas en Oracle y registrar diferencias o conformidad; no se modificó la base |
| 2026-09-20 | Fase 1, cierre de puntos 1 y 2 | Revisión de la ejecución aportada por el usuario | `PLAN_IMPLEMENTACION.md`; evidencia: `result.json` | 107 estados OK; claves, CHECK y DEFAULT revisados; nueve identidades ALWAYS; sin errores Oracle detectados | Resto de la fase 1 pendiente; no se modificó la base |
| 2026-09-25 | Diagnóstico de ejecución de la interfaz | Se comparó el código fuente con el bytecode de `target/classes`; se detectó que `mvn exec:java` ejecutaba un `Main.class` antiguo dedicado a probar Oracle. Se recompilaron 29 archivos y se documentó el comando correcto | `PLAN_IMPLEMENTACION.md`, `README.md`; clases generadas en `target/classes` | `mvn compile` terminó con `BUILD SUCCESS`; el bytecode actualizado crea `VentanaPrincipal` mediante `SwingUtilities.invokeLater` | Continuar las fases funcionales pendientes |
| 2026-09-25 | Auditoría de avance por fases | Se contrastaron todas las tareas con el árbol actual, el código y el historial completo de Git; se actualizaron marcas y estados | `PLAN_IMPLEMENTACION.md` | Fase 1: 3/8; fase 2: 6/8; fase 3: 5/7; fases 4–8 sin tareas completas. `mvn compile`, `mvn test` y `mvn package` terminaron con `BUILD SUCCESS`; no hay pruebas para ejecutar | Recuperar `sql/verificar_esquema.sql`, separar datos de prueba, regularizar el inventario inicial, validar configuración, alinear Java y versionar `.env.example`; además, completar las tareas pendientes de fases posteriores |
| 2026-09-25 | Cierre de fase 2 | Se añadieron validaciones de cantidad y precio a `ItemVenta`, se habilitó JUnit Jupiter y se probaron cálculos y reglas del modelo | `pom.xml`, `ValidacionException.java`, `ItemVenta.java`, `ItemVentaTest.java`, `PLAN_IMPLEMENTACION.md` | `mvn test`: 6 pruebas, 0 fallos, 0 errores, 0 omitidas; `BUILD SUCCESS` | Continuar fase 3; los pendientes no bloqueantes de fase 1 permanecen registrados |
| 2026-09-25 | Cierre de fase 3 | Se movieron conexión y CRUD de clientes a `SwingWorker`, se añadió cierre de recursos y se verificó persistencia con conexiones nuevas en una base aislada y en Oracle | `FormClientes.java`, `ClienteDAOPersistenciaTest.java`, `ClienteDAOOraclePersistenciaIT.java`, `pom.xml`, `PLAN_IMPLEMENTACION.md` | Suite aislada: 7 pruebas correctas. Integración Oracle: 1 prueba correcta, con alta y eliminación del cliente temporal; ambos comandos terminaron con `BUILD SUCCESS` | Considerar CI con GitHub Actions; continuar fase 4 y resolver antes la coherencia del inventario inicial de fase 1 |
| 2026-09-25 | Corrección de validaciones de clientes | Se reemplazaron las comprobaciones mínimas por reglas de formato en servicio y filtros de entrada en registro y edición | `ClienteServicio.java`, `FormClientes.java`, `ClienteServicioValidacionTest.java`, `PLAN_IMPLEMENTACION.md` | `mvn test`: 16 pruebas, 0 fallos, 0 errores, 0 omitidas; se cubren identificador negativo, nombre numérico, teléfono con letras, longitud telefónica, correo inválido, duplicados, datos válidos y edición | Los registros inválidos creados antes de esta corrección deben editarse o desactivarse manualmente |
| 2026-09-25 | Integración continua | Se agregó GitHub Actions para compilar y ejecutar las pruebas aisladas únicamente en pull requests desde cualquier rama hacia `development` | `.github/workflows/java-ci.yml`, `README.md`, `PLAN_IMPLEMENTACION.md` | Workflow con JDK 17, caché Maven, permisos de solo lectura y `mvn test`; no utiliza secretos ni ejecuta la integración Oracle | Confirmar la primera ejecución al abrir o actualizar un pull request hacia `development` |
| 2026-09-25 | Cierre de fase 4 | Se implementaron catálogo de productos y menús, abastecimiento, historial, formularios Swing y transacciones con rollback | DAO y servicios de productos, menús e inventario; `Transaccion.java`; `FormProductos.java`, `FormMenus.java`, `FormInventario.java`, `FormularioAsincrono.java`; pruebas de fase 4; documentación | Suite aislada: 24 pruebas correctas. Integración Oracle: 1 prueba correcta con creación, reconexión y limpieza de datos temporales; `BUILD SUCCESS` | Antes de ventas, resolver la regularización histórica del inventario inicial registrada en fase 1; continuar con fase 5 |
| 2026-09-25 | Documentación operativa | Se documentaron comandos de compilación, ejecución, pruebas aisladas, empaquetado, JAR, integraciones Oracle y CI; se actualizó la navegación de fases 3 y 4 | `README.md`, `PLAN_IMPLEMENTACION.md` | Comandos contrastados con `pom.xml`, pruebas e integraciones ejecutadas; suite actual de 24 pruebas | Mantener los comandos y conteos actualizados al incorporar nuevas fases |
| 2026-10-01 | Cierre de fase 7 | Se implementaron `RecompensaDAO`/`CanjeDAO`/`MovimientoPuntosDAO`, `PuntosYCanjesServicio` (bloqueo ordenado de cliente/recompensa/productos, consolidación de componentes de menú, transacción atómica de canje) y `FormPuntosYCanjes` con pestañas de canje, administración de recompensas, historial de puntos e historial de pedidos; se integró en `FormClientes` mediante el botón Ver puntos e historial | `RecompensaDAO.java`, `RecompensaDAOImpl.java`, `CanjeDAO.java`, `CanjeDAOImpl.java`, `MovimientoPuntosDAO.java`, `MovimientoPuntosDAOImpl.java`, `PuntosYCanjesServicio.java`, `FormPuntosYCanjes.java`, `FormClientes.java`, `VentanaPrincipal.java`, `PedidoDAO.java`, `PedidoDAOImpl.java`, `PuntosYCanjesServicioTest.java`, `PLAN_IMPLEMENTACION.md` | `mvn clean test`: 61 pruebas, 0 fallos, 0 errores, 0 omitidas; incluye canje exitoso de producto y de menú, puntos insuficientes, stock insuficiente y una prueba con dos hilos/conexiones que confirma que dos canjes simultáneos con puntos justos para uno solo no dejan saldo negativo. No se ejecutó contra Oracle compartido | Continuar con fase 8 (reportes y entrega) |

Para cada próximo avance, agregar una fila indicando qué se completó y cómo se verificó. Si una prueba no se pudo ejecutar, registrar el motivo; no marcar la tarea como verificada.
