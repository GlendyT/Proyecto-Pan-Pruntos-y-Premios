-- Verificacion de solo lectura del inventario y sus movimientos.
-- Ejecutar conectado como ADMIN o como un usuario con SELECT sobre
-- ADMIN.PRODUCTO y ADMIN.MOVIMIENTO_INVENTARIO.
-- No crea, modifica ni elimina datos u objetos.

PROMPT === INVENTARIO ACTUAL ===

SELECT id_producto,
       codigo,
       categoria,
       nombre,
       existencia,
       activo
  FROM ADMIN.PRODUCTO
 ORDER BY id_producto;

PROMPT === HISTORIAL DE MOVIMIENTOS POR PRODUCTO (CRONOLOGICO) ===

SELECT p.id_producto,
       p.codigo,
       p.nombre,
       m.id_movimiento,
       m.fecha,
       m.tipo,
       m.cantidad,
       m.existencia_resultante
  FROM ADMIN.MOVIMIENTO_INVENTARIO m
  JOIN ADMIN.PRODUCTO p
    ON p.id_producto = m.id_producto
 ORDER BY p.id_producto, m.fecha, m.id_movimiento;

PROMPT === EXISTENCIA ACTUAL COMPARADA CON EL ULTIMO MOVIMIENTO ===

WITH ultimo_movimiento AS (
    SELECT m.id_producto,
           m.existencia_resultante,
           ROW_NUMBER() OVER (
               PARTITION BY m.id_producto
               ORDER BY m.fecha DESC, m.id_movimiento DESC
           ) AS orden
      FROM ADMIN.MOVIMIENTO_INVENTARIO m
)
SELECT p.id_producto,
       p.codigo,
       p.nombre,
       p.existencia AS existencia_actual,
       u.existencia_resultante AS existencia_ultimo_movimiento,
       CASE
           WHEN u.id_producto IS NULL THEN 'SIN MOVIMIENTOS'
           WHEN p.existencia = u.existencia_resultante THEN 'COINCIDE'
           ELSE 'DIFERENCIA'
       END AS revision
  FROM ADMIN.PRODUCTO p
  LEFT JOIN ultimo_movimiento u
    ON u.id_producto = p.id_producto
   AND u.orden = 1
 ORDER BY p.id_producto;
