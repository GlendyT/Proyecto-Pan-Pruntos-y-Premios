-- ============================================================================
-- PROYECTO: Pan, Puntos y Premios
-- FASE 1 API SPRING BOOT: Script de Verificación de Migración
-- Base de datos: Oracle Autonomous Database (Always Free)
-- Descripción:
--   Verifica contra el catálogo del diccionario de datos de Oracle que las
--   modificaciones aditivas de la Fase 1 existan correctamente, sus tipos,
--   restricciones e índices estén activos, y que los clientes preexistentes
--   conserven su integridad con email_verificado = 'N'.
-- ============================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED;

DECLARE
    v_errores NUMBER := 0;
    v_total_checks NUMBER := 0;

    PROCEDURE registrar_check(p_nombre VARCHAR2, p_condicion BOOLEAN, p_detalle VARCHAR2 DEFAULT '') IS
    BEGIN
        v_total_checks := v_total_checks + 1;
        IF p_condicion THEN
            DBMS_OUTPUT.PUT_LINE('[OK] ' || p_nombre);
        ELSE
            DBMS_OUTPUT.PUT_LINE('[ERROR] ' || p_nombre || ' - ' || p_detalle);
            v_errores := v_errores + 1;
        END IF;
    END;

    v_count NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('====================================================');
    DBMS_OUTPUT.PUT_LINE('VERIFICACION DE MIGRACION FASE 1 - API SPRING BOOT');
    DBMS_OUTPUT.PUT_LINE('====================================================');

    -- 1. Verificar existencia de tabla CLIENTE_TOKEN_ACCESO
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'CLIENTE_TOKEN_ACCESO';
    registrar_check('Tabla CLIENTE_TOKEN_ACCESO existe', v_count = 1, 'No se encontro en USER_TABLES');

    -- 2. Verificar columnas en CLIENTE_TOKEN_ACCESO
    SELECT COUNT(*) INTO v_count FROM user_tab_cols 
    WHERE table_name = 'CLIENTE_TOKEN_ACCESO' 
      AND column_name IN ('ID_TOKEN', 'ID_CLIENTE', 'TOKEN_HASH', 'FECHA_CREACION', 'FECHA_EXPIRACION', 'FECHA_USO', 'DIRECCION_IP', 'ORIGEN_SOLICITUD');
    registrar_check('Columnas completas en CLIENTE_TOKEN_ACCESO', v_count = 8, 'Se esperaban 8 columnas, encontradas: ' || v_count);

    -- 3. Verificar columnas añadidas en CLIENTE
    SELECT COUNT(*) INTO v_count FROM user_tab_cols 
    WHERE table_name = 'CLIENTE' 
      AND column_name IN ('EMAIL_VERIFICADO', 'FECHA_VERIFICACION_EMAIL');
    registrar_check('Nuevas columnas EMAIL_VERIFICADO y FECHA_VERIFICACION_EMAIL en CLIENTE', v_count = 2, 'Columnas faltantes en CLIENTE');

    -- 4. Verificar restricción CHECK ck_cliente_email_verif
    SELECT COUNT(*) INTO v_count FROM user_constraints 
    WHERE table_name = 'CLIENTE' 
      AND constraint_name = 'CK_CLIENTE_EMAIL_VERIF' 
      AND status = 'ENABLED';
    registrar_check('Restriccion CK_CLIENTE_EMAIL_VERIF habilitada', v_count = 1, 'Restriccion ausente o inactiva');

    -- 5. Verificar restricción de clave foránea FK_TOKEN_CLIENTE
    SELECT COUNT(*) INTO v_count FROM user_constraints 
    WHERE table_name = 'CLIENTE_TOKEN_ACCESO' 
      AND constraint_name = 'FK_TOKEN_CLIENTE' 
      AND constraint_type = 'R' 
      AND status = 'ENABLED';
    registrar_check('Clave foranea FK_TOKEN_CLIENTE habilitada', v_count = 1, 'FK no encontrada o inactiva');

    -- 6. Verificar restricción UNIQUE en token_hash
    SELECT COUNT(*) INTO v_count FROM user_constraints 
    WHERE table_name = 'CLIENTE_TOKEN_ACCESO' 
      AND constraint_name = 'UQ_TOKEN_HASH' 
      AND constraint_type = 'U' 
      AND status = 'ENABLED';
    registrar_check('Restriccion UNIQUE UQ_TOKEN_HASH habilitada', v_count = 1, 'Constraint UNIQUE ausente');

    -- 7. Verificar restricción CHECK ck_token_expiracion
    SELECT COUNT(*) INTO v_count FROM user_constraints 
    WHERE table_name = 'CLIENTE_TOKEN_ACCESO' 
      AND constraint_name = 'CK_TOKEN_EXPIRACION' 
      AND status = 'ENABLED';
    registrar_check('Restriccion CK_TOKEN_EXPIRACION habilitada', v_count = 1, 'Constraint CHECK de expiracion ausente');

    -- 8. Verificar índices auxiliares
    SELECT COUNT(*) INTO v_count FROM user_indexes 
    WHERE index_name IN ('IDX_TOKEN_CLIENTE_CREACION', 'IDX_TOKEN_LIMPIEZA');
    registrar_check('Indices auxiliares creados', v_count = 2, 'Faltan indices de optimizacion');

    -- 9. Verificar que ningun cliente existente quedo con email_verificado nulo
    SELECT COUNT(*) INTO v_count FROM cliente WHERE email_verificado IS NULL;
    registrar_check('Integridad de clientes: ningun email_verificado es NULL', v_count = 0, 'Se detectaron ' || v_count || ' filas con NULL');

    DBMS_OUTPUT.PUT_LINE('----------------------------------------------------');
    DBMS_OUTPUT.PUT_LINE('RESUMEN: ' || (v_total_checks - v_errores) || '/' || v_total_checks || ' comprobaciones correctas.');
    IF v_errores = 0 THEN
        DBMS_OUTPUT.PUT_LINE('RESULTADO: Migracion de Fase 1 verificada exitosamente.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('RESULTADO: Se encontraron ' || v_errores || ' incidencias en el esquema.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('====================================================');
END;
/
