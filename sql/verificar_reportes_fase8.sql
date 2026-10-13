-- Comparacion de solo lectura para los seis reportes obligatorios de Fase 8.
-- Ejecutar conectado como ADMIN (o con SELECT sobre las tablas ADMIN.*).
-- Cambiar DESDE/HASTA al mismo rango usado en FormReportes antes de ejecutar.
-- Valores usados en la validacion visual inicial: 2026-09-01 a 2026-10-13.
-- Este archivo no crea, modifica ni elimina objetos o datos.

DEFINE DESDE = 2026-09-01
DEFINE HASTA = 2026-10-13

PROMPT === 01. VENTAS POR PERIODO: DETALLE ===

SELECT p.id_pedido,
       p.fecha,
       c.identificador,
       c.nombre AS cliente,
       p.estado,
       p.metodo_pago,
       p.total,
       p.puntos_generados
  FROM ADMIN.PEDIDO p
  JOIN ADMIN.CLIENTE c ON c.id_cliente = p.id_cliente
 WHERE p.estado IN ('PAGADO', 'ENTREGADO')
   AND p.fecha >= TO_DATE('&&DESDE', 'YYYY-MM-DD')
   AND p.fecha <  TO_DATE('&&HASTA', 'YYYY-MM-DD') + 1
 ORDER BY p.fecha DESC, p.id_pedido DESC;

PROMPT === 01. VENTAS POR PERIODO: TOTALES DEL TITULO ===

SELECT NVL(SUM(p.total), 0) AS total_general,
       NVL(SUM(CASE WHEN p.metodo_pago = 'EFECTIVO' THEN p.total ELSE 0 END), 0) AS total_efectivo,
       NVL(SUM(CASE WHEN p.metodo_pago = 'TARJETA' THEN p.total ELSE 0 END), 0) AS total_tarjeta
  FROM ADMIN.PEDIDO p
 WHERE p.estado IN ('PAGADO', 'ENTREGADO')
   AND p.fecha >= TO_DATE('&&DESDE', 'YYYY-MM-DD')
   AND p.fecha <  TO_DATE('&&HASTA', 'YYYY-MM-DD') + 1;

PROMPT === 02. PRODUCTOS MAS VENDIDOS ===

SELECT t.codigo,
       t.nombre,
       t.categoria,
       t.unidades_individuales,
       t.unidades_en_menus,
       t.unidades_individuales + t.unidades_en_menus AS total_unidades
  FROM (
        SELECT p.codigo,
               p.nombre,
               p.categoria,
               NVL((SELECT SUM(pd.cantidad)
                      FROM ADMIN.PEDIDO_DETALLE pd
                      JOIN ADMIN.PEDIDO ped ON ped.id_pedido = pd.id_pedido
                     WHERE pd.id_producto = p.id_producto
                       AND pd.tipo_item = 'PRODUCTO'
                       AND ped.estado IN ('PAGADO', 'ENTREGADO')), 0) AS unidades_individuales,
               NVL((SELECT SUM(pd.cantidad)
                      FROM ADMIN.PEDIDO_DETALLE pd
                      JOIN ADMIN.PEDIDO ped ON ped.id_pedido = pd.id_pedido
                      JOIN ADMIN.MENU_DETALLE md ON md.id_menu = pd.id_menu
                     WHERE md.id_producto = p.id_producto
                       AND pd.tipo_item = 'MENU'
                       AND ped.estado IN ('PAGADO', 'ENTREGADO')), 0) AS unidades_en_menus
          FROM ADMIN.PRODUCTO p
       ) t
 ORDER BY total_unidades DESC, t.nombre ASC;

PROMPT === 03. PRODUCTOS AGOTADOS O CON BAJO INVENTARIO ===

SELECT id_producto,
       codigo,
       categoria,
       nombre,
       precio,
       existencia,
       CASE WHEN existencia = 0 THEN 'AGOTADO' ELSE 'BAJO INVENTARIO' END AS estado_stock
  FROM ADMIN.PRODUCTO
 WHERE existencia <= 5
 ORDER BY existencia ASC, categoria ASC, nombre ASC;

PROMPT === 04. PUNTOS ACUMULADOS POR CLIENTE ===

SELECT id_cliente,
       identificador,
       nombre,
       telefono,
       email,
       saldo_puntos,
       CASE WHEN activo = 'S' THEN 'ACTIVO' ELSE 'INACTIVO' END AS estado
  FROM ADMIN.CLIENTE
 ORDER BY saldo_puntos DESC, nombre ASC;

PROMPT === 05. HISTORIAL DE CANJES ===

SELECT c.id_canje,
       c.fecha,
       cl.identificador,
       cl.nombre AS cliente,
       r.nombre AS recompensa,
       c.puntos_utilizados
  FROM ADMIN.CANJE c
  JOIN ADMIN.CLIENTE cl ON cl.id_cliente = c.id_cliente
  JOIN ADMIN.RECOMPENSA r ON r.id_recompensa = c.id_recompensa
 ORDER BY c.fecha DESC, c.id_canje DESC;

PROMPT === 06. VENTAS DE MENUS COMPLETOS ===

SELECT m.id_menu,
       m.codigo,
       m.nombre AS menu,
       m.precio AS precio_actual,
       NVL(SUM(CASE WHEN p.estado IN ('PAGADO', 'ENTREGADO') THEN pd.cantidad ELSE 0 END), 0) AS cantidad_vendida,
       NVL(SUM(CASE WHEN p.estado IN ('PAGADO', 'ENTREGADO') THEN pd.subtotal ELSE 0 END), 0) AS total_recaudado
  FROM ADMIN.MENU m
  LEFT JOIN ADMIN.PEDIDO_DETALLE pd
         ON pd.id_menu = m.id_menu
        AND pd.tipo_item = 'MENU'
  LEFT JOIN ADMIN.PEDIDO p ON p.id_pedido = pd.id_pedido
 GROUP BY m.id_menu, m.codigo, m.nombre, m.precio
 ORDER BY cantidad_vendida DESC, total_recaudado DESC, m.nombre ASC;
