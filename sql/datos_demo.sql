-- Datos de demostración para una base aislada después de crear el esquema.
-- No ejecutar en la base compartida del equipo.

INSERT INTO cliente (identificador, nombre, telefono, email) VALUES ('CLI001', 'Juan Perez', '55551234', 'juan.perez@correo.com');
INSERT INTO cliente (identificador, nombre, telefono, email) VALUES ('CLI002', 'Maria Lopez', '55555678', 'maria.lopez@correo.com');

INSERT INTO producto (codigo, categoria, nombre, precio, existencia) VALUES ('SAN001', 'SANDWICH', 'Sandwich de pollo', 25.00, 25);
INSERT INTO producto (codigo, categoria, nombre, precio, existencia) VALUES ('SAN002', 'SANDWICH', 'Sandwich de jamon', 22.00, 20);
INSERT INTO producto (codigo, categoria, nombre, precio, existencia) VALUES ('BEB001', 'BEBIDA', 'Gaseosa', 8.00, 50);
INSERT INTO producto (codigo, categoria, nombre, precio, existencia) VALUES ('ACO001', 'ACOMPANAMIENTO', 'Papas', 10.00, 35);

INSERT INTO menu (codigo, nombre, precio) VALUES ('MEN001', 'Menu completo de pollo', 37.00);

INSERT INTO menu_detalle (id_menu, id_producto, rol)
    SELECT m.id_menu, p.id_producto, 'SANDWICH' FROM menu m, producto p WHERE m.codigo = 'MEN001' AND p.codigo = 'SAN001';
INSERT INTO menu_detalle (id_menu, id_producto, rol)
    SELECT m.id_menu, p.id_producto, 'BEBIDA' FROM menu m, producto p WHERE m.codigo = 'MEN001' AND p.codigo = 'BEB001';
INSERT INTO menu_detalle (id_menu, id_producto, rol)
    SELECT m.id_menu, p.id_producto, 'ACOMPANAMIENTO' FROM menu m, producto p WHERE m.codigo = 'MEN001' AND p.codigo = 'ACO001';

INSERT INTO recompensa (nombre, puntos_necesarios, id_producto)
    SELECT 'Bebida gratuita', 10, id_producto FROM producto WHERE codigo = 'BEB001';
INSERT INTO recompensa (nombre, puntos_necesarios, id_producto)
    SELECT 'Acompanamiento gratuito', 15, id_producto FROM producto WHERE codigo = 'ACO001';
INSERT INTO recompensa (nombre, puntos_necesarios, id_producto)
    SELECT 'Sandwich individual gratuito', 30, id_producto FROM producto WHERE codigo = 'SAN001';
INSERT INTO recompensa (nombre, puntos_necesarios, id_menu)
    SELECT 'Menu completo gratuito', 50, id_menu FROM menu WHERE codigo = 'MEN001';

COMMIT;
