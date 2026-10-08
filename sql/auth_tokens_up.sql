-- ============================================================================
-- PROYECTO: Pan, Puntos y Premios
-- FASE 1 API SPRING BOOT: Migración Aditiva Hacia Adelante (UP)
-- Base de datos: Oracle Autonomous Database (Always Free)
-- Descripción:
--   1. Añade soporte aditivo para verificación de correo en la tabla CLIENTE
--      sin alterar datos preexistentes (email_verificado por defecto 'N').
--   2. Crea la tabla CLIENTE_TOKEN_ACCESO para autenticación de un solo uso
--      (One-Time Token / Passwordless Login) con almacenamiento de hash SHA-256.
--   3. Establece restricciones de integridad, expiración e índices de búsqueda.
--   4. Documenta concesión de privilegios mínimos para el usuario de la API.
-- ============================================================================

-- 1. Modificación aditiva en la tabla CLIENTE (compatible hacia atrás con Swing)
ALTER TABLE cliente ADD (
    email_verificado CHAR(1) DEFAULT 'N' NOT NULL,
    fecha_verificacion_email TIMESTAMP NULL,
    CONSTRAINT ck_cliente_email_verif CHECK (email_verificado IN ('S', 'N'))
);

COMMENT ON COLUMN cliente.email_verificado IS 'Indica si el correo fue verificado explícitamente (S/N). Por defecto N.';
COMMENT ON COLUMN cliente.fecha_verificacion_email IS 'Fecha y hora en que se verificó la dirección de correo electrónico.';

-- 2. Creación de la tabla de tokens de acceso temporal
CREATE TABLE cliente_token_acceso (
    id_token NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente NUMBER NOT NULL,
    token_hash VARCHAR2(64) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_uso TIMESTAMP NULL,
    direccion_ip VARCHAR2(45) NULL,
    origen_solicitud VARCHAR2(100) NULL,
    CONSTRAINT fk_token_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
    CONSTRAINT uq_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_token_expiracion CHECK (fecha_expiracion > fecha_creacion)
);

COMMENT ON TABLE cliente_token_acceso IS 'Tokens de un solo uso para acceso al portal de puntos por enlace temporal.';
COMMENT ON COLUMN cliente_token_acceso.id_token IS 'Identificador autogenerado del token.';
COMMENT ON COLUMN cliente_token_acceso.id_cliente IS 'Identificador del cliente asociado.';
COMMENT ON COLUMN cliente_token_acceso.token_hash IS 'Hash SHA-256 (64 caracteres hexadecimales) del token secreto.';
COMMENT ON COLUMN cliente_token_acceso.fecha_creacion IS 'Momento de emisión del token.';
COMMENT ON COLUMN cliente_token_acceso.fecha_expiracion IS 'Momento en que el token deja de ser válido (ej. 15 minutos).';
COMMENT ON COLUMN cliente_token_acceso.fecha_uso IS 'Momento en que el token fue consumido. Nulo si no se ha usado.';
COMMENT ON COLUMN cliente_token_acceso.direccion_ip IS 'Dirección IPv4/IPv6 de la solicitud para auditoría y rate limiting.';
COMMENT ON COLUMN cliente_token_acceso.origen_solicitud IS 'Encabezado de origen o User-Agent simplificado.';

-- 3. Índices complementarios para optimización de consultas y mantenimiento
CREATE INDEX idx_token_cliente_creacion ON cliente_token_acceso (id_cliente, fecha_creacion);
CREATE INDEX idx_token_limpieza ON cliente_token_acceso (fecha_expiracion, fecha_uso);

-- ============================================================================
-- 4. Guía de Privilegios Mínimos (Least Privilege)
--    NO utilizar el usuario ADMIN en la API Spring Boot.
--    Ejecutar como ADMIN para crear el usuario de servicio de la API:
--
--    CREATE USER app_puntos_api IDENTIFIED BY "Password_Segura_Aqui_123#";
--    GRANT CREATE SESSION TO app_puntos_api;
--
--    Privilegios estrictos requeridos por la API:
--    GRANT SELECT ON cliente TO app_puntos_api;
--    La API solo actualiza estas dos columnas al verificar el correo con el primer enlace:
--    GRANT UPDATE (email_verificado, fecha_verificacion_email) ON cliente TO app_puntos_api;
--    GRANT SELECT, INSERT, UPDATE ON cliente_token_acceso TO app_puntos_api;
--    GRANT SELECT ON movimiento_puntos TO app_puntos_api;
-- ============================================================================
