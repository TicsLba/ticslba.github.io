# Aula Móvil 11 · Especificación cerrada

## Arquitectura
Aula Móvil 11 se compone de tres piezas: agente Android administrado como Device Owner/Profile Owner, Centro de Gestión Windows y Relay HTTPS para gestión fuera de la LAN.

## Sesiones
- El usuario Propietario queda protegido por Aula Móvil antes de entregar el launcher.
- Estudiante y Profesor se crean como usuarios Android administrados y efímeros.
- El launcher OEM no se libera durante creación/cambio de usuario; sólo después de completar la preparación inicial.
- Cerrar sesión elimina el usuario temporal, sus cuentas y sus datos.
- Si Aula Móvil se reinicia durante una sesión ACTIVE, recupera la sesión en vez de volver al selector.
- Un reinicio físico de la tablet termina la sesión temporal y devuelve el equipo al acceso institucional.
- Inactividad predeterminada: Estudiante 10 min, Profesor 30 min, configurable. Aviso final: 2 minutos.

## Perfiles y aplicaciones
- Administrador: Google Play e instalación disponibles.
- Estudiante/Profesor: Google Play y tiendas conocidas ocultas/bloqueadas; instalación y APK manual bloqueados.
- Cuentas Google personales sí se permiten dentro de la sesión temporal.
- Aplicaciones instaladas permanentemente por Administrador pueden habilitarse en usuarios temporales mediante el catálogo institucional.
- Políticas por perfil permiten excluir paquetes concretos de Profesor o Estudiante.

## Seguridad Android
Se bloquean en perfiles temporales: desinstalación, borrado/gestión de apps, instalación, fuentes desconocidas, depuración, creación/cambio de usuario, factory reset, modo seguro, cambios VPN, USB de archivos/OTG y desactivación de ubicación. Wi-Fi y Bluetooth permanecen configurables. La ubicación se mantiene habilitada.

El modo Administrador relaja temporalmente restricciones. Al cerrarlo se restauran. El restablecimiento de fábrica institucional exige contraseña y la frase exacta BORRAR TABLET.

## Supervisión
La sesión inicial requiere MediaProjection antes de liberar Android. Android controla el consentimiento oficial y Aula Móvil no lo evade.

Si MediaProjection se interrumpe después, la sesión continúa. La consola marca la falta de supervisión y puede solicitar nuevamente la autorización. Pantalla en vivo: 0,5 / 1 / 2 / 4 / 6 FPS.

No existe keylogger. Aula Móvil no lee ni registra contraseñas ni texto escrito.

## Gestión
Comandos disponibles incluyen mensajes, atención, URL, abrir app, volver a HOME, ubicación, solicitud de supervisión, pantalla remota, bloqueo, modo pérdida, sonido, volumen, FPS, inactividad, reinicio, instalación/desinstalación administrada, inventario, archivos, cambio de contraseña administrativa y políticas.

## Relay
El Relay HTTPS permite telemetría, cola de órdenes offline, acuses de ejecución, auditoría, ubicación histórica cifrada y pantalla remota. La pantalla remota guarda únicamente el último cuadro cifrado y lo reemplaza; no implementa grabación histórica.

Retención predeterminada: 30 días, configurable.

## Límites deliberados
- Android exige consentimiento del sistema para MediaProjection; no se intenta saltar ese control.
- El apagado remoto no se promete porque Android estándar no ofrece una API Device Owner universal para ello.
- No se captura texto, contraseñas ni búsquedas escritas.
- El historial web completo de navegadores de terceros no se obtiene mediante técnicas invasivas; se auditan URLs gestionadas por Aula Móvil y actividad de aplicaciones mediante UsageStats.
- La versión se considera RC hasta superar el piloto físico en el Lenovo institucional.
