-- ============================================================================
-- PROYECTO: Pan, Puntos y Premios
-- FASE 1 API SPRING BOOT: Migración Reversible Hacia Atrás (DOWN / ROLLBACK)
-- Base de datos: Oracle Autonomous Database (Always Free)
-- Descripción:
--   Revierte de manera segura y limpia todos los cambios introducidos por
--   la Fase 1 sin afectar los datos originales de la tabla CLIENTE ni otras
--   tablas preexistentes del sistema.
-- ============================================================================

-- 1. Eliminar la tabla de tokens y sus dependencias (índices y FKs asociados)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE cliente_token_acceso CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN -- ORA-00942: table or view does not exist
            RAISE;
        END IF;
END;
/

-- 2. Eliminar la restricción CHECK de email_verificado en CLIENTE
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE cliente DROP CONSTRAINT ck_cliente_email_verif';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2443 THEN -- ORA-02443: Cannot drop constraint - nonexistent constraint
            RAISE;
        END IF;
END;
/

-- 3. Eliminar las columnas aditivas de la tabla CLIENTE
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE cliente DROP (email_verificado, fecha_verificacion_email)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -904 THEN -- ORA-00904: invalid identifier (columna no existe)
            RAISE;
        END IF;
END;
/
