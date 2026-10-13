# Fase 9 — Aceptación y seguridad del portal

**Estado:** documentada; aceptación parcial confirmada; endurecimiento de infraestructura aplazado por decisión del usuario.  
**Decisión operativa:** mantener estable la VM OCI y no modificar ahora reglas de red, SSH, ACL de Autonomous Database, Nginx ni el servicio en producción.

## Alcance

Esta fase verifica que el portal publicado y sus controles de aplicación funcionan con las decisiones actuales. La revisión documental o funcional no cambia por sí misma la infraestructura. Los ajustes de OCI se posponen hasta que el responsable autorice una ventana y se prepare una comprobación de conectividad/recuperación.

## Estado de aceptación conocido

| Comprobación | Evidencia | Estado |
|---|---|---|
| Enlace por correo lleva al dominio Vercel de producción | Usuario generó un enlace después de configurar `APP_PORTAL_BASE_URL` y confirmó que abre el portal | Confirmado |
| Sesión y consulta de puntos del cliente | Usuario confirmó que pudo acceder desde el enlace publicado | Confirmado |
| Seis reportes Swing contra Oracle | Usuario confirmó coincidencia de valores de los seis reportes | Confirmado |
| Error al fallar conexión de reporte | ORA-12541 se presentó; usuario confirmó que la ventana siguió respondiendo al cerrar el aviso | Confirmado |
| Java CI | El usuario compartió `gh run list`: las tres ejecuciones más recientes de Java CI aparecen exitosas; una ejecución anterior falló y luego hubo ejecuciones exitosas | Confirmado con esa salida |
| Tokens vencidos/reutilizados, correo desconocido, cuotas y cooldown en pruebas | Hay cobertura en pruebas existentes; revisar la suite/código antes de repetir en producción | Revisión documental por completar |
| CSRF/logout, `Secure`/`HttpOnly`, `no-store`, aislamiento entre dos clientes | Parte de los controles existe en configuración/código; falta registrar una prueba integral dedicada en ambiente de aceptación | Pendiente de aceptación documental |

## Controles de aplicación para revisar sin tocar OCI

1. **Identidad:** `GET /api/v1/me/**` debe obtener cliente desde `ClientePrincipal`; nunca aceptar un ID de cliente enviado por el navegador para elegir cuenta.
2. **Sesión y cookies:** revisar `SecurityConfig` y respuestas de producción. Confirmar cookie `HttpOnly`, `Secure`, `SameSite` según configuración; logout debe invalidar la sesión.
3. **CSRF:** mantener la protección para solicitudes con sesión; las excepciones deben limitarse a los endpoints públicos de solicitud y consumo de token y a los endpoints técnicos requeridos.
4. **Token:** un enlace expira, se consume una sola vez, se persiste el hash en lugar del token claro y el fragmento se limpia del navegador después de leerlo.
5. **Anti-enumeración y abuso:** las solicitudes responden de forma genérica; respetar cooldown/cuotas. Las pruebas de correo deben usar una cuenta controlada por el equipo.
6. **Privacidad:** las respuestas personales de saldo e historial no se almacenan en caché; no imprimir tokens, credenciales o datos sensibles en logs.
7. **Correo:** confirmar entrega mediante el evento de EmailJS y recepción real. Las claves privadas viven solo en el `.env` del backend y no se copian a Vercel.
8. **Compatibilidad Swing:** mantener por separado `DB_*` de API y `SWING_DB_*`; el portal no debe cambiar los privilegios de la cuenta de escritorio.

## Infraestructura expresamente aplazada

Los siguientes puntos se documentan, pero no se ejecutan en esta fase porque el usuario pidió no alterar la VM:

- La regla SSH existente puede permitir acceso desde Internet (`0.0.0.0/0`). No editarla ahora; limitarla requiere conocer la IP administrativa estable y tener un medio alternativo de acceso.
- ADB reportó endpoint público y ACL deshabilitada con mTLS requerido. No activar ACL sin inventariar IP/origen de todas las conexiones autorizadas, incluida Swing, ni preparar recuperación de acceso.
- No modificar Nginx, certificados, firewall de Linux/OCI, wallet, `.env`, systemd, puerto Spring ni DNS durante la aceptación de solo lectura.
- No reiniciar la VM ni el servicio como parte de una revisión documental.

Estos elementos no se deben marcar como endurecidos. Si en el futuro se desea cerrarlos, será una tarea de infraestructura separada con respaldo de configuración, cambio acotado, verificación SSH y health check, y un procedimiento de reversión.

## Pruebas manuales recomendadas en ambiente controlado

No hacer pruebas de carga o abuso contra el correo/producción. Para aceptación funcional, usar cuentas de prueba autorizadas y registrar únicamente resultado, fecha y código HTTP; nunca copiar tokens.

| Prueba | Resultado esperado |
|---|---|
| Solicitar enlace para correo desconocido | Respuesta genérica; no revela existencia de la cuenta |
| Consumir enlace vigente | Sesión creada; se muestra información del mismo cliente |
| Reabrir token ya consumido o vencido | Rechazo y opción para pedir enlace nuevo |
| Consultar `/api/v1/me` sin sesión | `401` JSON |
| Solicitar saldo/movimientos con sesión | Solo los datos de ese cliente y cabeceras `no-store` |
| Logout y posterior llamada `/me` | Sesión terminada; llamada vuelve a `401` |
| Enviar operación con sesión sin CSRF requerido | `403`; con token CSRF válido, comportamiento normal |

## Criterio de salida

- Aceptación ya confirmada: portal, enlace de correo, consulta de puntos, reportes de escritorio, respuesta ante ORA-12541 y las ejecuciones CI compartidas.
- Pendiente: documentar una revisión completa de los controles de aplicación indicados arriba. La infraestructura permanece sin cambios.
- La fase 9 solo puede declararse cerrada cuando el responsable decida si acepta los riesgos de infraestructura aplazados o autoriza un plan independiente para mitigarlos. La documentación no afirma que la VM/ADB hayan sido endurecidas.
