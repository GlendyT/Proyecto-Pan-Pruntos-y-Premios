# Progreso de implementación

## Lo que ya está implementado

### Conexión a la base de datos
- Conexión JDBC a Oracle Cloud Autonomous Database funcionando mediante wallet y variables de entorno (`.env`).

### Modelos (`src/main/java/com/panpuntospremios/modelo/`)
Clases que representan las entidades del negocio:

- `Cliente` — datos del cliente: identificador, nombre, teléfono, email, saldo de puntos, estado activo y fecha de registro
- `Producto` — datos del producto: código, categoría, nombre, precio, existencia y estado activo
- `Menu` — datos del menú: código, nombre, precio, estado activo y lista de componentes
- `Pedido` — datos del pedido: cliente, fecha, estado, total, puntos generados y método de pago
- `Recompensa` — datos de la recompensa: nombre, puntos necesarios, producto o menú asociado y estado activo
- `ItemVenta` — clase abstracta con `calcularSubtotal()` y `calcularPuntos()` abstracto
- `ProductoIndividual` — extiende `ItemVenta`; otorga 2 puntos por sándwich, 0 por bebida o acompañamiento
- `MenuCompleto` — extiende `ItemVenta`; otorga 8 puntos por menú
- `PedidoDetalle` — detalle de cada ítem dentro de un pedido
- `MovimientoInventario` — registro histórico de movimientos de inventario
- `MovimientoPuntos` — registro histórico de movimientos de puntos de un cliente
- `Canje` — registro de canjes de recompensas por puntos

### Enums (`src/main/java/com/panpuntospremios/enums/`)
Un enum (enumeración) es una lista fija de opciones válidas que nunca cambia. Se usan para evitar errores de tipeo y que Java avise si se usa un valor que no existe. Por ejemplo, en vez de escribir el String `"PAGADO"` a mano (donde cualquier error de tipeo pasaría sin queja), se usa `EstadoPedido.PAGADO` y el compilador lo valida.

- `Categoria` — las 3 categorías posibles de un producto: `SANDWICH`, `BEBIDA`, `ACOMPANAMIENTO`
- `EstadoPedido` — los 4 estados posibles de un pedido: `PENDIENTE`, `PAGADO`, `ENTREGADO`, `ANULADO`
- `MetodoPago` — las 2 formas de pago aceptadas: `EFECTIVO`, `TARJETA`
- `TipoItem` — si un ítem del pedido es un producto suelto o un menú: `PRODUCTO`, `MENU`
- `TipoMovimiento` — los tipos de movimiento posibles en inventario y puntos: `ABASTECIMIENTO`, `VENTA`, `CANJE`, `ACUMULACION`

### Excepciones (`src/main/java/com/panpuntospremios/excepcion/`)
Las excepciones son errores propios del negocio. Cuando algo sale mal en una operación, en vez de que el programa se caiga sin explicación, se lanza una excepción con un mensaje claro que indica exactamente qué falló y por qué.

- `StockInsuficienteException` — se lanza cuando un cliente quiere comprar o canjear algo pero no hay suficiente inventario
- `PuntosInsuficientesException` — se lanza cuando un cliente intenta canjear una recompensa pero no tiene suficientes puntos
- `PagoRechazadoException` — se lanza cuando el pago con tarjeta no es aprobado
