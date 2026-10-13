-- Verificacion de solo lectura del esquema base de Pan, Puntos y Premios.
-- Ejecutar conectado como propietario de las tablas (ADMIN).
-- Las diez filas deben mostrar OK; cualquier FALTA indica una tabla ausente.
-- El script no crea, modifica ni elimina objetos o datos.

SELECT esperado.nombre_tabla,
       CASE WHEN existente.table_name IS NULL THEN 'FALTA' ELSE 'OK' END AS estado
  FROM (
        SELECT 'CLIENTE' nombre_tabla FROM dual UNION ALL
        SELECT 'PRODUCTO' FROM dual UNION ALL
        SELECT 'MENU' FROM dual UNION ALL
        SELECT 'MENU_DETALLE' FROM dual UNION ALL
        SELECT 'PEDIDO' FROM dual UNION ALL
        SELECT 'PEDIDO_DETALLE' FROM dual UNION ALL
        SELECT 'MOVIMIENTO_INVENTARIO' FROM dual UNION ALL
        SELECT 'MOVIMIENTO_PUNTOS' FROM dual UNION ALL
        SELECT 'RECOMPENSA' FROM dual UNION ALL
        SELECT 'CANJE' FROM dual
       ) esperado
  LEFT JOIN user_tables existente
    ON existente.table_name = esperado.nombre_tabla
 ORDER BY esperado.nombre_tabla;

-- Comprueba el numero esperado de columnas por tabla.
-- Cero filas significa que todas las tablas base tienen el conteo esperado.
SELECT esperado.nombre_tabla,
       esperado.columnas_esperadas,
       COUNT(col.column_name) AS columnas_encontradas
  FROM (
        -- CLIENTE tiene 8 columnas del esquema base y 2 agregadas para
        -- verificar el correo del portal: EMAIL_VERIFICADO y FECHA_VERIFICACION_EMAIL.
        SELECT 'CLIENTE' nombre_tabla, 10 columnas_esperadas FROM dual UNION ALL
        SELECT 'PRODUCTO', 7 FROM dual UNION ALL
        SELECT 'MENU', 5 FROM dual UNION ALL
        SELECT 'MENU_DETALLE', 3 FROM dual UNION ALL
        SELECT 'PEDIDO', 8 FROM dual UNION ALL
        SELECT 'PEDIDO_DETALLE', 9 FROM dual UNION ALL
        SELECT 'MOVIMIENTO_INVENTARIO', 6 FROM dual UNION ALL
        SELECT 'MOVIMIENTO_PUNTOS', 6 FROM dual UNION ALL
        SELECT 'RECOMPENSA', 6 FROM dual UNION ALL
        SELECT 'CANJE', 5 FROM dual
       ) esperado
  LEFT JOIN user_tab_columns col
    ON col.table_name = esperado.nombre_tabla
 GROUP BY esperado.nombre_tabla, esperado.columnas_esperadas
HAVING COUNT(col.column_name) <> esperado.columnas_esperadas
 ORDER BY esperado.nombre_tabla;

-- Resume restricciones para revisar que estén habilitadas y validadas.
SELECT constraint_type,
       status,
       validated,
       COUNT(*) AS cantidad
  FROM user_constraints
 WHERE table_name IN (
       'CLIENTE', 'PRODUCTO', 'MENU', 'MENU_DETALLE', 'PEDIDO',
       'PEDIDO_DETALLE', 'MOVIMIENTO_INVENTARIO', 'MOVIMIENTO_PUNTOS',
       'RECOMPENSA', 'CANJE'
 )
 GROUP BY constraint_type, status, validated
 ORDER BY constraint_type, status, validated;

-- La autenticacion web es una migracion adicional y no forma parte del DDL base.
SELECT 'CLIENTE_TOKEN_ACCESO' AS tabla_esperada,
       CASE WHEN existente.table_name IS NULL THEN 'FALTA' ELSE 'OK' END AS estado
  FROM dual
  LEFT JOIN user_tables existente
    ON existente.table_name = 'CLIENTE_TOKEN_ACCESO';

SELECT esperado.nombre_columna,
       CASE WHEN existente.column_name IS NULL THEN 'FALTA' ELSE 'OK' END AS estado
  FROM (
        SELECT 'EMAIL_VERIFICADO' nombre_columna FROM dual UNION ALL
        SELECT 'FECHA_VERIFICACION_EMAIL' FROM dual
       ) esperado
  LEFT JOIN user_tab_columns existente
    ON existente.table_name = 'CLIENTE'
   AND existente.column_name = esperado.nombre_columna
 ORDER BY esperado.nombre_columna;
