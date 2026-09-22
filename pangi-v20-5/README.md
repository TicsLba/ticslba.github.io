# PANGI V20.5

**Gestión inteligente de dispositivos educativos**  
**Observa · Protege · Responde**

PANGI V20.5 reúne en una sola versión el agente Android, la consola Windows, el Relay remoto y las herramientas de aprovisionamiento.

## Componentes

- **PANGI Agent** — Android, paquete cl.antumapu.pangi.v205.
- **PANGI Console** — Windows x64, gestión LAN + Relay.
- **PANGI Relay** — Windows x64, servidor de telemetría, comandos y archivos.
- **PANGI Setup** — scripts PowerShell para piloto y Device Owner.

## Funciones integradas

### Acceso y sesiones
- Estudiante / Profesor / Administración.
- Usuarios Android temporales separados y efímeros.
- HOME institucional antes de entregar el launcher.
- Handoff seguro: PANGI prepara el usuario en segundo plano y espera confirmación de HOME, políticas, apps e IME.
- Estado ACTIVE persistente frente a reinicio del proceso.
- Reinicio completo del dispositivo termina la sesión temporal.
- Cierre de sesión elimina el usuario temporal.

### Políticas
- Play Store disponible para Administrador.
- Play Store bloqueado para Estudiante y Profesor.
- Instalación y fuentes desconocidas bloqueadas en usuarios temporales.
- Wi-Fi y Bluetooth configurables.
- VPN, depuración, factory reset, safe boot y almacenamiento USB restringidos en perfiles temporales.
- Ubicación institucional habilitable.
- Apps institucionales del propietario disponibles por política.

### Supervisión y control
- MediaProjection con consentimiento oficial de Android.
- La pérdida de supervisión no destruye una sesión activa.
- LAN + Relay HTTPS.
- Mosaico e individual.
- Solicitud de pantalla, FPS, mensajes, Atención, apertura de URL/app.
- Bloqueo, reinicio, volumen, inactividad.
- Instalación APK remota y desinstalación.
- Envío de archivos institucionales.
- GPS, ring y modo pérdida.

### Inventario e historial
- Estado en línea, batería, carga, almacenamiento, Wi-Fi, Android, modelo y versión.
- Responsable, rol y curso de la sesión.
- Telemetría e historial técnico.
- Uso de aplicaciones por foreground.
- Registro de URL sólo cuando PANGI puede observarla por flujo administrado; no keylogging.
- Retención configurable en Relay.

## Compatibilidad de sesiones

Para garantizar el handoff seguro sin mostrar el launcher antes de tiempo, las sesiones compartidas requieren **Android 9 / API 28 o superior** y soporte OEM para usuarios administrados.

En Android 8 PANGI puede instalarse y administrarse, pero no habilita el flujo de sesión compartida segura.

## Estado de esta versión

La compilación CI valida código y toolchain. La aceptación final exige piloto físico en los modelos Samsung/Lenovo reales antes de despliegue masivo.
