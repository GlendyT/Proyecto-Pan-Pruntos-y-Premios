-- Esquema limpio de la aplicación de escritorio. Ejecutar solo en una base aislada nueva.
-- No ejecutar en la base compartida del equipo.

CREATE TABLE cliente (
    id_cliente NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    identificador VARCHAR2(20) NOT NULL UNIQUE,
    nombre VARCHAR2(100) NOT NULL,
    telefono VARCHAR2(20),
    email VARCHAR2(100),
    saldo_puntos NUMBER DEFAULT 0 NOT NULL,
    activo CHAR(1) DEFAULT 'S' NOT NULL,
    fecha_registro DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT ck_cliente_puntos CHECK (saldo_puntos >= 0),
    CONSTRAINT ck_cliente_activo CHECK (activo IN ('S', 'N'))
);

CREATE TABLE producto (
    id_producto NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR2(20) NOT NULL UNIQUE,
    categoria VARCHAR2(20) NOT NULL,
    nombre VARCHAR2(100) NOT NULL,
    precio NUMBER(10, 2) NOT NULL,
    existencia NUMBER DEFAULT 0 NOT NULL,
    activo CHAR(1) DEFAULT 'S' NOT NULL,
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_existencia CHECK (existencia >= 0),
    CONSTRAINT ck_producto_categoria CHECK (categoria IN ('SANDWICH', 'BEBIDA', 'ACOMPANAMIENTO')),
    CONSTRAINT ck_producto_activo CHECK (activo IN ('S', 'N'))
);

CREATE TABLE menu (
    id_menu NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR2(20) NOT NULL UNIQUE,
    nombre VARCHAR2(100) NOT NULL,
    precio NUMBER(10, 2) NOT NULL,
    activo CHAR(1) DEFAULT 'S' NOT NULL,
    CONSTRAINT ck_menu_precio CHECK (precio >= 0),
    CONSTRAINT ck_menu_activo CHECK (activo IN ('S', 'N'))
);

CREATE TABLE menu_detalle (
    id_menu NUMBER NOT NULL,
    id_producto NUMBER NOT NULL,
    rol VARCHAR2(20) NOT NULL,
    CONSTRAINT pk_menu_detalle PRIMARY KEY (id_menu, id_producto),
    CONSTRAINT fk_menudet_menu FOREIGN KEY (id_menu) REFERENCES menu(id_menu),
    CONSTRAINT fk_menudet_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    CONSTRAINT ck_menudet_rol CHECK (rol IN ('SANDWICH', 'BEBIDA', 'ACOMPANAMIENTO'))
);

CREATE TABLE pedido (
    id_pedido NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente NUMBER NOT NULL,
    fecha DATE DEFAULT SYSDATE NOT NULL,
    estado VARCHAR2(15) DEFAULT 'PENDIENTE' NOT NULL,
    total NUMBER(10, 2) DEFAULT 0 NOT NULL,
    puntos_generados NUMBER DEFAULT 0 NOT NULL,
    metodo_pago VARCHAR2(10),
    referencia_pago VARCHAR2(50),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
    CONSTRAINT ck_pedido_estado CHECK (estado IN ('PENDIENTE', 'PAGADO', 'ENTREGADO', 'ANULADO')),
    CONSTRAINT ck_pedido_metodo CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA')),
    CONSTRAINT ck_pedido_total CHECK (total >= 0)
);

CREATE TABLE pedido_detalle (
    id_detalle NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido NUMBER NOT NULL,
    tipo_item VARCHAR2(10) NOT NULL,
    id_producto NUMBER,
    id_menu NUMBER,
    cantidad NUMBER NOT NULL,
    precio_unitario NUMBER(10, 2) NOT NULL,
    subtotal NUMBER(10, 2) NOT NULL,
    puntos NUMBER DEFAULT 0 NOT NULL,
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    CONSTRAINT fk_detalle_menu FOREIGN KEY (id_menu) REFERENCES menu(id_menu),
    CONSTRAINT ck_detalle_tipo CHECK (tipo_item IN ('PRODUCTO', 'MENU')),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_referencia CHECK (
        (tipo_item = 'PRODUCTO' AND id_producto IS NOT NULL AND id_menu IS NULL) OR
        (tipo_item = 'MENU' AND id_menu IS NOT NULL AND id_producto IS NULL)
    )
);

CREATE TABLE movimiento_inventario (
    id_movimiento NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_producto NUMBER NOT NULL,
    fecha DATE DEFAULT SYSDATE NOT NULL,
    tipo VARCHAR2(15) NOT NULL,
    cantidad NUMBER NOT NULL,
    existencia_resultante NUMBER NOT NULL,
    CONSTRAINT fk_movinv_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    CONSTRAINT ck_movinv_tipo CHECK (tipo IN ('ABASTECIMIENTO', 'VENTA', 'CANJE'))
);

CREATE TABLE movimiento_puntos (
    id_movimiento NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente NUMBER NOT NULL,
    fecha DATE DEFAULT SYSDATE NOT NULL,
    tipo VARCHAR2(15) NOT NULL,
    puntos NUMBER NOT NULL,
    referencia VARCHAR2(50),
    CONSTRAINT fk_movpts_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
    CONSTRAINT ck_movpts_tipo CHECK (tipo IN ('ACUMULACION', 'CANJE'))
);

CREATE TABLE recompensa (
    id_recompensa NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    puntos_necesarios NUMBER NOT NULL,
    id_producto NUMBER,
    id_menu NUMBER,
    activo CHAR(1) DEFAULT 'S' NOT NULL,
    CONSTRAINT fk_recompensa_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    CONSTRAINT fk_recompensa_menu FOREIGN KEY (id_menu) REFERENCES menu(id_menu),
    CONSTRAINT ck_recompensa_puntos CHECK (puntos_necesarios > 0),
    CONSTRAINT ck_recompensa_activo CHECK (activo IN ('S', 'N')),
    CONSTRAINT ck_recompensa_item CHECK (
        (id_producto IS NOT NULL AND id_menu IS NULL) OR
        (id_menu IS NOT NULL AND id_producto IS NULL)
    )
);

CREATE TABLE canje (
    id_canje NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente NUMBER NOT NULL,
    id_recompensa NUMBER NOT NULL,
    fecha DATE DEFAULT SYSDATE NOT NULL,
    puntos_utilizados NUMBER NOT NULL,
    CONSTRAINT fk_canje_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
    CONSTRAINT fk_canje_recompensa FOREIGN KEY (id_recompensa) REFERENCES recompensa(id_recompensa),
    CONSTRAINT ck_canje_puntos CHECK (puntos_utilizados > 0)
);
