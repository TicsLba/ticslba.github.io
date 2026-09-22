# PANGI V20.5 · Privacidad y seguridad

PANGI está diseñado para administrar dispositivos institucionales compartidos sin capturar credenciales personales.

- No implementa keylogging.
- No registra contraseñas, PIN ni texto escrito en otras aplicaciones.
- La supervisión de pantalla requiere el consentimiento oficial de MediaProjection cuando Android lo exige.
- No se crea grabación histórica de pantalla por defecto.
- Se conserva únicamente el último cuadro remoto cuando la supervisión remota está habilitada.
- Las sesiones Estudiante/Profesor se ejecutan en usuarios Android temporales; al cerrar la sesión se elimina el contenedor con sus cuentas y datos locales.
- La ubicación, inventario, eventos y comandos se usan para protección y operación del dispositivo institucional.
- Las consultas de navegación sólo se registran cuando la URL es observable por un flujo administrado; PANGI no extrae historial privado completo de navegadores de terceros.
- La retención de Relay es configurable y debe ajustarse a la política institucional.
