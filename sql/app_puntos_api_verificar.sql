-- Ejecutar como APP_PUNTOS_API despues de los GRANT.
-- Solo consulta metadatos y hace SELECT sin devolver filas de negocio.

SELECT USER AS usuario_conectado FROM dual;

SELECT owner, table_name, privilege
FROM all_tab_privs_recd
WHERE owner = 'ADMIN'
  AND grantee = USER
  AND table_name IN ('CLIENTE', 'CLIENTE_TOKEN_ACCESO', 'MOVIMIENTO_PUNTOS')
ORDER BY table_name, privilege;

SELECT owner, table_name, column_name, privilege
FROM all_col_privs_recd
WHERE owner = 'ADMIN'
  AND grantee = USER
  AND table_name = 'CLIENTE'
  AND privilege = 'UPDATE'
ORDER BY column_name;

-- Cada consulta debe terminar correctamente y devolver cero filas.
SELECT id_cliente FROM ADMIN.CLIENTE WHERE 1 = 0;
SELECT id_token FROM ADMIN.CLIENTE_TOKEN_ACCESO WHERE 1 = 0;
SELECT id_movimiento FROM ADMIN.MOVIMIENTO_PUNTOS WHERE 1 = 0;
