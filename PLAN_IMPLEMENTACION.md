# Plan de implementación y seguimiento — Pan, Puntos y Premios

Fecha de creación: 2026-09-20.

Estado general: fases 1 a 8 completadas. La fase 1 se cerró con verificación del esquema y conciliación de inventario en Oracle el 2026-10-13. El 2026-10-13 `mvn package` generó el JAR de escritorio y terminó con 94 pruebas correctas; el usuario abrió el JAR, comparó los seis reportes con Oracle y confirmó el manejo de ORA-12541. El portal Next.js está publicado en Vercel y el usuario confirmó que el enlace recibido por correo abre el dominio público y permite iniciar sesión. Las tres ejecuciones más recientes de Java CI compartidas por el usuario aparecen exitosas; queda pendiente la fase 9 de seguridad y aceptación del portal.

Este documento registra el alcance previsto, los avances, las verificaciones y las decisiones pendientes. Las marcas reflejan los archivos presentes y las verificaciones disponibles en el repositorio al 2026-10-13.

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

El proyecto ya contiene modelos, enums, excepciones, DAO y servicios para clientes, productos, menús, inventario, pedidos, cobros, fidelización y reportes, además de sus formularios Java Swing. La última ejecución local registrada contiene 94 pruebas automatizadas correctas; las funciones o procedimientos almacenados siguen sin estar definidos en el SQL revisado.

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
| 1 | Base técnica y esquema | Completada (8 de 8 tareas verificadas) | Inicio autorizado |
| 2 | Modelos y reglas | Completada (8 de 8 tareas verificadas) | Fase 1 |
| 3 | Clientes y primera pantalla | Completada (7 de 7 tareas verificadas) | Fase 2 |
| 4 | Productos, menús e inventario | Completada (9 de 9 tareas verificadas) | Fases 2 y 3 |
| 5 | Pedidos | Completada (9 de 9; verificación aislada) | Fases 3 y 4 |
| 6 | Cobro y acumulación de puntos | Completada (12 de 12 tareas verificadas) | Fase 5 |
| 7 | Recompensas y canjes | Completada (10 de 10 tareas verificadas) | Fases 4 y 6 |
| 8 | Reportes y entrega | Completada (14 de 14 tareas verificadas) | Fases anteriores |

## Fase 1. Base técnica y esquema

- [x] Verificar qué tablas, columnas y restricciones ya existen en Oracle.
- [x] Crear `sql/verificar_esquema.sql` con consultas de solo lectura para comprobar tablas, conteos de columnas y estado de restricciones.
- [x] Preparar ajustes incrementales necesarios conservando los datos existentes; no ejecutar de nuevo todo el DDL sobre tablas existentes. **La verificación histórica no encontró diferencias que requirieran ajustes; por tanto, no correspondía generar ni ejecutar scripts de alteración.**
- [x] Separar el esquema y los datos de demostración en `sql/crear_esquema.sql` y `sql/datos_demo.sql`; conservar `sql/ddl.sql` como fixture combinado para H2.
- [x] Revisar la coherencia entre existencias iniciales e historial antes de considerar una regularización. **Validado con los datos Oracle del 2026-10-13; los saldos actuales concuerdan con los movimientos disponibles y con el inventario semilla conocido. No fue necesario modificar datos ni inventar movimientos históricos.**
- [x] Mejorar los mensajes de configuración faltante en `ConexionOracle.java` y separar credenciales de API (`DB_*`) y escritorio (`SWING_DB_*`).
- [x] Revisar `pom.xml`, Java 17, el plugin de empaquetado y la plantilla versionada `.env.example`; la ejecución del JAR final sigue pendiente en fase 8.
- [x] Verificar compilación, conexión y compatibilidad del esquema.

Archivos previstos: `sql/verificar_esquema.sql`, scripts de ajustes y datos de prueba, `ConexionOracle.java`, `pom.xml`, `.env.example` y documentación de configuración.

Criterio de cierre: conexión comprobada y esquema compatible, con evidencia de verificaciones y conservación de los datos existentes.

Auditoría retrospectiva iniciada el 2026-09-25 y actualizada el 2026-10-13:

1. **Verificación del esquema en Oracle — completada y repetida el 2026-10-13.** `result.json` confirma las 10 tablas base, la tabla `CLIENTE_TOKEN_ACCESO`, las columnas `EMAIL_VERIFICADO` y `FECHA_VERIFICACION_EMAIL`, y restricciones habilitadas/validadas. La consulta de conteo señaló 10 columnas para `CLIENTE` frente a las 8 originales del esquema base; son las dos columnas agregadas por la autenticación web y no una anomalía de Oracle.
2. **Script repetible de verificación — ejecutado el 2026-10-13.** `sql/verificar_esquema.sql` es de solo lectura. Se corrigió el conteo esperado de `CLIENTE` a 10 para incluir las columnas de autenticación web comprobadas en `result.json`.
3. **Ajustes incrementales — completa para el esquema verificado.** Al no encontrarse diferencias entre Oracle y el DDL esperado, no había ajustes que preparar. Si el esquema cambia, deberá repetirse la comparación antes de crear migraciones.
4. **Separación de datos de prueba — completada el 2026-10-13.** Se añadieron `sql/crear_esquema.sql` y `sql/datos_demo.sql`. `sql/ddl.sql` se conserva como fixture combinado que consumen las pruebas H2.
5. **Coherencia de existencias e historial — revisada el 2026-10-13.** La salida de `sql/verificar_inventario_movimientos.sql` muestra que la existencia actual coincide con la del movimiento más reciente para los productos con movimientos. Los productos semilla `SAN001`, `BEB001` y `ACO001` también concuerdan con su existencia inicial en `sql/ddl.sql` menos las ventas observadas; `SAN002` conserva sus 20 unidades semilla y no tiene movimientos. `POLLO` y `BEB002` coinciden con sus abastecimientos registrados; `BRW001` parte de un abastecimiento de 80 y tiene una venta de 7, por lo que quedan 73. No se insertaron movimientos retroactivos porque no hay evidencia de una fecha de apertura que permita registrarlos correctamente.
6. **Mensajes por configuración faltante — completado el 2026-10-13.** `ConexionOracle` valida URL, usuario y contraseña antes de conectarse. Swing prioriza `SWING_DB_URL`, `SWING_DB_USER` y `SWING_DB_PASSWORD`; si no se configuran las variables específicas, solo conserva el fallback legado cuando `DB_USER` no es `APP_PUNTOS_API`.
7. **Maven, Java y plantilla — revisión estática completada el 2026-10-13.** `pom.xml` y README coinciden en Java 17 o superior; el empaquetado usa `maven-shade-plugin`, y `.env.example` está versionado con URL TNS y marcadores para API y Swing. La generación y ejecución del JAR final se valida en fase 8.
8. **Compilación, conexión y compatibilidad — completa.** `mvn compile`, `mvn test` y `mvn package` finalizaron con `BUILD SUCCESS`; la conexión y la compatibilidad estructural cuentan con la evidencia aportada previamente. Desde el cierre de las fases 2 y 3 también existen pruebas automatizadas aisladas y una integración opcional con Oracle.

Evidencia de Oracle aportada el 2026-10-13: `result.json` confirma las diez tablas base, objetos y columnas de autenticación web, restricciones habilitadas/validadas y los saldos/resultados de movimientos descritos arriba. La revisión fue de solo lectura; no se modificó la base compartida.

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
- El saldo y las existencias actuales son el punto de partida. La conciliación de inventario se completó en fase 1 el 2026-10-13; no se agregaron movimientos históricos sin fecha de apertura verificable ni se modificó Oracle.

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

- [x] Confirmar el listado exacto de reportes obligatorios del enunciado. La Seccion 14 del PDF exige seis reportes mediante `JTable`:
  - **01. Ventas por periodo:** `ReporteDAOImpl.consultarVentasPorPeriodo` lista las ventas del rango y calcula total general, efectivo y tarjeta; `FormReportes` presenta los totales en el titulo y el detalle en `JTable`.
  - **02. Productos mas vendidos:** `consultarProductosMasVendidos` desglosa unidades individuales y componentes vendidos dentro de menus.
  - **03. Productos agotados o con bajo inventario:** `consultarBajoInventario` filtra `existencia <= 5`.
  - **04. Puntos acumulados por cliente:** `consultarPuntosPorCliente` muestra el saldo actual de cada cliente.
  - **05. Historial de canjes:** `consultarHistorialCanjes` identifica cliente, fecha, recompensa y puntos utilizados.
  - **06. Ventas de menus completos:** `consultarVentasMenus` presenta cantidades e importes vendidos por menu.
  - Los seis reportes estan conectados desde `FormReportes` y cubiertos por `ReporteServicioTest` en H2. La validación manual se completó: el usuario consultó los seis reportes en la interfaz y confirmó coincidencia con las consultas de solo lectura en Oracle.
- [x] Implementar en `ReporteDAO` y `ReporteDAOImpl` las consultas SQL para los reportes faltantes (02, 03 filtro <= 5, 04, 05 con recompensas, 06 y resumen de 01).
- [x] Actualizar `ReporteServicio` con los nuevos tipos de reporte y validaciones correspondientes.
- [x] Actualizar `FormReportes` para exponer en el combo los 6 reportes oficiales y presentarlos en `JTable`.
- [x] Probar el flujo completo: cliente → abastecimiento → pedido → cobro → puntos → canje → reportes. **Prueba aislada H2.**
- [x] Verificar consistencia entre reportes y datos persistidos. **Pruebas H2 comprueban pedidos, saldo, existencias y tipos de movimiento.**
- [x] Ejecutar pruebas de integración en un entorno de prueba separado de los datos compartidos del equipo. **H2 en memoria; no escribe en Oracle.**
- [x] Abrir el JAR y consultar los seis reportes oficiales en la interfaz. **El usuario confirmó que el JAR abrió y que consultó los reportes el 2026-10-13; el 2026-10-14 confirmó el flujo de autenticación publicado: el enlace enviado por correo abre Vercel y permite acceder.**
- [x] Comparar los seis reportes contra las consultas de solo lectura en Oracle para el mismo rango de fechas y filtros. El usuario confirmó que los seis coinciden; Ventas por periodo usa 2026-09-01 a 2026-10-13.
- [x] Comprobar errores de conexión y que la interfaz permanezca utilizable. El usuario confirmó que el aviso ORA-12541 se cierra y la ventana sigue respondiendo.
- [x] Generar el JAR final. **`mvn package` generó el artefacto el 2026-10-13; suite local: 94 pruebas correctas.**
- [x] Confirmar integración continua. **El usuario compartió `gh run list`: las tres ejecuciones más recientes de Java CI están exitosas; una ejecución anterior de la rama `glendy` falló, seguida por ejecuciones correctas.**
- [x] Actualizar README con configuración, ejecución, pruebas y uso de los seis reportes obligatorios y cuatro consultas complementarias.
- [x] Contrastar los entregables con el enunciado y registrar limitaciones pendientes. **Auditoría documental del 2026-10-13: Java/Swing/Oracle, seis reportes obligatorios con JTable, SQL de esquema y datos, diagramas UML/ER e historial Git con commits en más de cinco días distintos; limitaciones manuales permanecen registradas abajo.**

Criterio de cierre: los 6 reportes obligatorios implementados y verificados en `FormReportes` mediante `JTable`, flujo completo verificado y aplicación ejecutable con documentación suficiente.

Implementación histórica del 2026-10-04 y auditoría documental del 2026-10-13: el enunciado define seis reportes obligatorios; las cuatro consultas generales existentes se conservan como opciones complementarias. El código y las pruebas aisladas cubren los seis reportes obligatorios. El JAR abrió y el usuario confirmó que consultó los seis reportes. Fase 8 cerrada el 2026-10-13: el usuario confirmó la coincidencia de los seis reportes con Oracle, que la ventana continuó respondiendo tras cerrar ORA-12541, que el portal web publicado funciona y compartió las tres ejecuciones más recientes de Java CI en estado exitoso. Una ejecución anterior falló y fue seguida por ejecuciones verdes.

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
| Reportes | Seis reportes obligatorios implementados en DAO, servicio, FormReportes y pruebas H2; el usuario confirmó la revisión visual y coincidencia contra Oracle | Completada |
| Anulaciones | Solo pedidos pendientes; devoluciones de ventas pagadas fuera de este alcance | Implementada con bloqueo frente a cobros |
| Entrega de pedidos | Botón Marcar entregado en cobro y comprobantes; solo desde PAGADO | Implementada y probada en H2 |
| Menús | Un componente de cada categoría; el esquema actual no garantiza por sí solo esta composición | Validado en `MenuServicio`; fase 4 completada |
| Inventario inicial | Las existencias actuales concuerdan con los datos semilla conocidos y con los movimientos registrados; no se crearon movimientos históricos sin fecha verificable | Conciliado en fase 1 el 2026-10-13 |
| Cobro con tarjeta | Aprobación o rechazo seleccionable; referencia SIM, sin datos de tarjeta | Implementado y probado en H2 |
| Android/API | El diseño lo marca como extra; no forma parte de estas ocho fases | Opcional, fuera del alcance inicial |

## Bitácora de auditoría

| Fecha | Fase / actividad | Trabajo realizado | Archivos afectados | Verificación / evidencia | Pendientes |
|---|---|---|---|---|---|
| 2026-10-13 | Cierre de fase 1: conciliación de inventario | Se compararon existencias de los siete productos con el último movimiento y con los datos semilla conocidos; se cerró la fase sin agregar registros históricos | `result.json`, `sql/verificar_inventario_movimientos.sql`, `PLAN_IMPLEMENTACION.md` | Todos los productos con movimientos coinciden con `existencia_resultante`; `SAN001`, `BEB001` y `ACO001` concuerdan con saldo semilla menos ventas; `SAN002` conserva las 20 unidades semilla sin movimientos; `POLLO`, `BRW001` y `BEB002` concuerdan con los abastecimientos y ventas reportados. Solo lectura; ninguna escritura en Oracle | Completar pendientes de fase 8 y verificar CI |
| 2026-10-13 | Revisión de resultados SQL de los seis reportes | Se analizaron los resultados que el usuario exportó tras ejecutar `sql/verificar_reportes_fase8.sql` | `result.json`, `sql/verificar_reportes_fase8.sql`, `PLAN_IMPLEMENTACION.md` | Las consultas devolvieron resultados sin errores; ventas suman 14,143 (10 efectivo + 14,133 tarjeta), productos vendidos concuerdan con los componentes del menú, bajo inventario y canjes no tienen filas, saldo de puntos de Tony es 56 y el menú MEN003 registra 7 unidades/14,133. El 2026-10-14 el usuario confirmó que los seis reportes coinciden visualmente con Oracle; probó el enlace de correo en producción y confirmó que abre el portal; también confirmó que la ventana permaneció utilizable al cerrar ORA-12541 | Revisar GitHub Actions |
| 2026-10-13 | Verificación actual de esquema Oracle | Se revisó el resultado del verificador ejecutado como ADMIN; las 10 tablas base y los objetos de autenticación web están presentes, con restricciones habilitadas y validadas. Se ajustó el conteo esperado de columnas de `CLIENTE` por sus dos columnas adicionales | `result.json`, `sql/verificar_esquema.sql`, `PLAN_IMPLEMENTACION.md` | Resultado aportado por el usuario: 10/10 tablas base `OK`; `CLIENTE_TOKEN_ACCESO` `OK`; `EMAIL_VERIFICADO` y `FECHA_VERIFICACION_EMAIL` `OK`; restricciones reportadas como `ENABLED` y `VALIDATED`. No hubo cambios en Oracle | Validación visual/de reportes contra Oracle; generar y ejecutar JAR final; confirmar CI |
| 2026-10-13 | Auditoría de instrucciones y actualización de fase 1/8 | Se contrastaron los seis reportes con DAO, servicio, JTable y pruebas; se actualizaron README y seguimiento; se separaron scripts de esquema/datos, se creó un verificador de solo lectura y se actualizó el ER para la tabla de tokens web | `README.md`, `PLAN_IMPLEMENTACION.md`, `diseno_pan_puntos_premios.md`, `sql/crear_esquema.sql`, `sql/datos_demo.sql`, `sql/verificar_esquema.sql` | Revisión estática de código, pruebas existentes y entregables; `git diff --check` sin errores. No se ejecutaron pruebas ni SQL contra Oracle en esa auditoría | Validación visual/Oracle de reportes; generar/ejecutar JAR y confirmar CI |
| 2026-10-13 | Auditoría de entregables y empaquetado de fase 8 | Se contrastaron las secciones de requisitos y entregables de `Instrucciones.md` con código, SQL, diagramas e historial Git; se generó y abrió el JAR y se consultaron los seis reportes | `PLAN_IMPLEMENTACION.md`, `README.md`, `sql/verificar_reportes_fase8.sql`, `target/pan-puntos-premios-1.0-SNAPSHOT.jar` | Revisión documental; `mvn package`: `BUILD SUCCESS`, 94 pruebas correctas (0 fallos/errores), JAR sombreado de 37,460,411 bytes. El usuario confirma que la interfaz abrió y consultó los reportes; falta comparación independiente contra Oracle | Comparar reportes con `sql/verificar_reportes_fase8.sql`; comprobar manejo de error de conexión; revisar GitHub Actions |

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
