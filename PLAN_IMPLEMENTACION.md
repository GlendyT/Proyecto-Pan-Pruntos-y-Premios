# Plan de implementación y seguimiento — Pan, Puntos y Premios

Fecha de creación: 2026-09-20.

Estado general: primeros dos puntos de la fase 1 completados; resto de la fase pendiente.

Este documento registra el alcance previsto, los avances, las verificaciones y las decisiones pendientes. El usuario autorizó iniciar los dos primeros puntos de la fase 1; el resto del plan permanece pendiente.

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

El código actual contiene una prueba de conexión en `Main.java` y la conexión JDBC configurada mediante `.env` en `ConexionOracle.java`. El usuario informó que la conexión Java ya funcionó; esta revisión no volvió a probarla.

El esquema local define 10 tablas: `cliente`, `producto`, `menu`, `menu_detalle`, `pedido`, `pedido_detalle`, `movimiento_inventario`, `movimiento_puntos`, `recompensa` y `canje`. El usuario confirmó que las tablas y columnas ya existen en Oracle. Los resultados aportados en `result.json` fueron revisados y concuerdan con el DDL en tablas, columnas, restricciones, valores predeterminados e identidades.

El diseño propone Java Swing, JDBC, modelos, DAO y servicios. Sus ejemplos Java aún no están implementados como módulos del proyecto. No se encontraron pruebas automatizadas ni funciones o procedimientos almacenados en el SQL revisado.

El PDF del enunciado mencionado en el README no está disponible en la ruta del proyecto. Queda pendiente confirmar los reportes obligatorios y cualquier exigencia específica de PL/SQL.

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
| 1 | Base técnica y esquema | En curso (puntos 1 y 2) | Inicio autorizado |
| 2 | Modelos y reglas | Pendiente | Fase 1 |
| 3 | Clientes y primera pantalla | Pendiente | Fase 2 |
| 4 | Productos, menús e inventario | Pendiente | Fases 2 y 3 |
| 5 | Pedidos | Pendiente | Fases 3 y 4 |
| 6 | Cobro y acumulación de puntos | Pendiente | Fase 5 |
| 7 | Recompensas y canjes | Pendiente | Fases 4 y 6 |
| 8 | Reportes y entrega | Pendiente | Fases anteriores |

## Fase 1. Base técnica y esquema

- [x] Verificar qué tablas, columnas y restricciones ya existen en Oracle.
- [x] Crear `sql/verificar_esquema.sql` para revisar la estructura esperada.
- [ ] Preparar ajustes incrementales necesarios conservando los datos existentes; no ejecutar de nuevo todo el DDL sobre tablas existentes.
- [ ] Separar los datos de prueba del script de creación del esquema.
- [ ] Resolver la coherencia entre existencias iniciales e historial: el SQL actual carga existencias sin movimientos de abastecimiento. Revisar los datos reales antes de preparar una regularización.
- [ ] Mejorar los mensajes de configuración faltante en `ConexionOracle.java`.
- [ ] Revisar Maven, la versión Java documentada, el empaquetado y la plantilla `.env.example`.
- [ ] Verificar compilación, conexión y compatibilidad del esquema.

Archivos previstos: `sql/verificar_esquema.sql`, scripts de ajustes y datos de prueba, `ConexionOracle.java`, `pom.xml`, `.env.example` y documentación de configuración.

Criterio de cierre: conexión comprobada y esquema compatible, con evidencia de verificaciones y conservación de los datos existentes.

Avance de los puntos 1 y 2: completados. El usuario ejecutó el script y aportó `result.json` (salida tabular de texto, aunque su extensión sea `.json`). La revisión encontró 10 tablas, 63 columnas y 34 restricciones con nombre con estado `OK` (107 comprobaciones). Las restricciones figuran habilitadas y validadas. El inventario de 104 filas incluye las 10 claves primarias, las tres claves únicas esperadas, las referencias de claves foráneas y los CHECK acordes con el DDL. También se revisaron los DEFAULT de las 63 columnas y las nueve identidades `ALWAYS`. No se detectaron errores Oracle en la salida. Esta evidencia verifica estructura; no certifica todavía los datos de negocio ni completa el resto de la fase 1.

Para repetir la verificación, abrir `sql/verificar_esquema.sql` en el cliente SQL conectado como propietario de las tablas y ejecutar sus consultas. Las secciones 1 a 3 muestran diferencias mediante la columna `ESTADO`; las secciones 4 y 5 requieren contrastar sus resultados con el DDL. Todas las sentencias son de solo lectura.

## Fase 2. Modelos y reglas

- [ ] Crear `Cliente`, `Producto`, `Menu`, `Pedido` y `Recompensa`.
- [ ] Crear `ItemVenta`, `ProductoIndividual` y `MenuCompleto`, aplicando herencia y polimorfismo.
- [ ] Crear los modelos de detalle, movimientos y canjes necesarios.
- [ ] Crear enums para categorías, estados, métodos de pago y tipos de movimiento.
- [ ] Crear excepciones para validaciones, inventario insuficiente, puntos insuficientes y pago rechazado.
- [ ] Implementar importes con `BigDecimal` y validar cantidades positivas.
- [ ] Implementar el cálculo de puntos: 2 por sándwich individual, 8 por menú completo y 0 por bebida o acompañamiento individual.
- [ ] Probar totales y puntos de pedidos mixtos, cantidades inválidas y límites relevantes.

Archivos previstos: clases en `modelo/`, `enums/`, `excepcion/` y pruebas de reglas en `src/test/java/`.

Criterio de cierre: cálculos y validaciones comprobados sin depender de la interfaz ni de una conexión a Oracle.

## Fase 3. Clientes y primera pantalla

- [ ] Crear `ClienteDAO` y su implementación JDBC con `PreparedStatement`.
- [ ] Crear `ClienteServicio` para registro, búsqueda, edición y desactivación.
- [ ] Validar identificadores duplicados y datos obligatorios.
- [ ] Crear `VentanaPrincipal` y `FormClientes`.
- [ ] Adaptar `Main.java` para iniciar Swing.
- [ ] Ejecutar operaciones de base de datos en segundo plano y actualizar la interfaz en el hilo de Swing.
- [ ] Verificar que los cambios se conserven al cerrar y volver a abrir la aplicación.

Criterio de cierre: administrar clientes desde la interfaz con persistencia en Oracle y mensajes comprensibles de error.

## Fase 4. Productos, menús e inventario

- [ ] Crear DAO y servicios de productos y menús.
- [ ] Crear `InventarioServicio` y persistencia de movimientos.
- [ ] Crear `FormProductos`, `FormMenus` y `FormInventario`.
- [ ] Implementar registro, edición y desactivación de productos y menús.
- [ ] Validar menús con un sándwich, una bebida y un acompañamiento.
- [ ] Implementar abastecimiento con cantidad positiva.
- [ ] Actualizar existencias y registrar movimientos dentro de la misma transacción.
- [ ] Mostrar existencias e historial de movimientos.
- [ ] Verificar que una operación fallida no deje cambios parciales.

Criterio de cierre: catálogo y menús administrables, con abastecimiento e historial coherentes.

## Fase 5. Pedidos

- [ ] Crear `PedidoDAO` y `PedidoServicio`, incluyendo persistencia del detalle.
- [ ] Crear `FormPedido` con búsqueda de cliente y selección de productos y menús.
- [ ] Permitir agregar y quitar ítems, modificando cantidades y actualizando total y puntos previstos.
- [ ] Consolidar las cantidades requeridas por producto, incluyendo componentes de menús.
- [ ] Validar disponibilidad al confirmar el pedido.
- [ ] Guardar cabecera y detalle de forma atómica como pedido `PENDIENTE`, sin descontar inventario ni acreditar puntos.
- [ ] Consultar pedidos pendientes y recuperar su detalle e importes guardados.
- [ ] Implementar la anulación de pedidos pendientes conforme a la decisión de alcance.
- [ ] Verificar pedidos mixtos, falta de existencias y persistencia del detalle.

Criterio de cierre: crear y consultar pedidos pendientes completos desde la interfaz.

## Fase 6. Cobro y acumulación de puntos

- [ ] Crear `VentaServicio` y `FormCobro`.
- [ ] Implementar efectivo con validación de importe recibido y cálculo de cambio.
- [ ] Implementar tarjeta con aprobación o rechazo simulado.
- [ ] Revalidar estado del pedido y existencias al cobrar.
- [ ] Descontar inventario de productos y componentes de menús.
- [ ] Registrar movimientos de inventario y puntos, actualizar saldo y marcar el pedido `PAGADO` en una sola transacción.
- [ ] Evitar cobros duplicados y proteger existencias y saldos frente a operaciones concurrentes.
- [ ] Implementar comprobante con los datos persistidos de la venta.
- [ ] Incorporar la transición de `PAGADO` a `ENTREGADO` si se mantiene ese estado en el flujo final.
- [ ] Probar pago rechazado, falta de stock, cobro repetido y rollback ante fallos.

Criterio de cierre: venta completa y atómica, con inventario, puntos, pago y comprobante coherentes.

## Fase 7. Recompensas y canjes

- [ ] Crear DAO de recompensas, canjes y movimientos de puntos.
- [ ] Crear `FidelizacionServicio` y `FormFidelizacion`.
- [ ] Permitir administrar el catálogo de recompensas y sus costos en puntos.
- [ ] Mostrar saldo, recompensas disponibles e historial de puntos del cliente.
- [ ] Validar cliente/recompensa activos, puntos suficientes y disponibilidad del producto o componentes del menú.
- [ ] Descontar puntos e inventario y registrar canje y movimientos en una misma transacción.
- [ ] Proteger el saldo y las existencias frente a canjes concurrentes.
- [ ] Integrar consultas de historial de pedidos y puntos en la pantalla de clientes.
- [ ] Probar canje exitoso, puntos insuficientes, stock insuficiente y rollback.

Criterio de cierre: canjes e historiales funcionales, sin saldos negativos ni cambios parciales.

## Fase 8. Reportes y entrega

- [ ] Confirmar el listado exacto de reportes obligatorios del enunciado.
- [ ] Definir consultas y filtros de reportes, considerando estados de pedidos y rangos de fechas.
- [ ] Crear `ReporteDAO`, `ReporteServicio` y `FormReportes`.
- [ ] Mostrar reportes en `JTable`; alcance provisional: ventas, inventario y movimientos.
- [ ] Probar el flujo completo: cliente → abastecimiento → pedido → cobro → puntos → canje → reportes.
- [ ] Verificar consistencia entre reportes y datos persistidos.
- [ ] Ejecutar pruebas de integración en un entorno de prueba separado de los datos compartidos del equipo.
- [ ] Comprobar errores de conexión y que la interfaz permanezca utilizable.
- [ ] Generar y ejecutar el JAR final.
- [ ] Actualizar README con configuración, ejecución, pruebas y uso de cada módulo.
- [ ] Contrastar los entregables con el enunciado y registrar limitaciones pendientes.

Criterio de cierre: flujo completo verificado, reportes confirmados y aplicación ejecutable con documentación suficiente.

## Decisiones y asuntos pendientes

| Asunto | Propuesta o situación | Estado |
|---|---|---|
| Interfaz | Java Swing, conforme al diseño existente | Incluido en el plan; pendiente de aprobación |
| Lógica de negocio | Servicios Java y DAO JDBC; no duplicar reglas entre capas | Incluido en el plan; pendiente de aprobación |
| PL/SQL | Confirmar si el enunciado exige funciones o procedimientos almacenados | Pendiente |
| Reportes | Confirmar la sección de reportes del PDF original | Pendiente |
| Anulaciones | Permitir inicialmente anular pedidos pendientes; definir aparte devoluciones de ventas pagadas y reversión de puntos/inventario | Pendiente de confirmar |
| Entrega de pedidos | Definir el uso del estado `ENTREGADO` en la interfaz | Pendiente de confirmar |
| Menús | Un componente de cada categoría; el esquema actual no garantiza por sí solo esta composición | Validar en fase 4 |
| Inventario inicial | Datos de ejemplo con stock, pero sin movimientos de abastecimiento | Resolver en fase 1 |
| Cobro con tarjeta | Simulado, según el documento de diseño | Incluido en el plan; pendiente de aprobación |
| Android/API | El diseño lo marca como extra; no forma parte de estas ocho fases | Opcional, fuera del alcance inicial |

## Bitácora de auditoría

| Fecha | Fase / actividad | Trabajo realizado | Archivos afectados | Verificación / evidencia | Pendientes |
|---|---|---|---|---|---|
| 2026-09-20 | Revisión inicial | Inspección del código, SQL, Maven y documentación; propuesta de ocho fases | Sin cambios durante la revisión | Dos clases Java existentes; diez tablas definidas en el DDL; módulos todavía documentados como ejemplos | Aprobar inicio y verificar esquema real |
| 2026-09-20 | Documentación del plan | Creación de este documento a solicitud del usuario | `PLAN_IMPLEMENTACION.md` | Plan con tareas, criterios de cierre, decisiones y bitácora | Implementación pendiente de aprobación |
| 2026-09-20 | Fase 1, puntos 1 y 2 | Inicio autorizado; usuario confirma tablas y columnas existentes; creación del script de verificación de solo lectura | `sql/verificar_esquema.sql`, `PLAN_IMPLEMENTACION.md` | Revisión estática de cobertura contra DDL: 10 tablas, 63 columnas, 34 restricciones con nombre; inventarios para revisión manual | Ejecutar consultas en Oracle y registrar diferencias o conformidad; no se modificó la base |
| 2026-09-20 | Fase 1, cierre de puntos 1 y 2 | Revisión de la ejecución aportada por el usuario | `PLAN_IMPLEMENTACION.md`; evidencia: `result.json` | 107 estados OK; claves, CHECK y DEFAULT revisados; nueve identidades ALWAYS; sin errores Oracle detectados | Resto de la fase 1 pendiente; no se modificó la base |

Para cada próximo avance, agregar una fila indicando qué se completó y cómo se verificó. Si una prueba no se pudo ejecutar, registrar el motivo; no marcar la tarea como verificada.
