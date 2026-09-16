# Tablet Escolar 2.1.0

Estado del código: **estable para instalación controlada**.

## Componentes
- Android: agente institucional para tablets administradas.
- Windows: Centro de gestión docente y técnico.
- Relay HTTPS opcional: telemetría y comandos entre redes.

## Funciones incluidas
- Sesiones temporales de Estudiante y Profesor.
- Administración Device Owner desde el usuario Propietario.
- Supervisión de pantalla en vivo dentro de la LAN.
- Modo Atención, mensajes, apertura de enlaces y aplicaciones, cierre remoto de sesión.
- Inventario persistente de dispositivos.
- Registro de responsable de uso por sesión: nombre, rol, curso cuando corresponde y dispositivo utilizado.
- Informes por dispositivo y por responsable.
- Telemetría de batería, estado de administración y última conexión.
- Ubicación institucional con fecha y precisión cuando Android entrega una ubicación válida.
- Modo pérdida, bloqueo remoto y pantalla institucional de recuperación.
- Relay HTTPS opcional para telemetría y comandos fuera de la red local.
- Exportación CSV de informes.

## Privacidad
Tablet Escolar no implementa keylogging, no registra contraseñas, PIN ni tokens, y no mantiene grabaciones históricas de pantalla por defecto. La supervisión visual es en tiempo real. La ubicación se asocia al dispositivo institucional y debe usarse conforme a la política del establecimiento.

## Compatibilidad objetivo
- Android: API 26 o superior; hardware de referencia Lenovo TB-8505F con Android 10.
- Windows: x64, aplicación WPF autocontenida.

## Instalación Android
1. Instalar la APK firmada suministrada en el paquete de distribución.
2. En un equipo preparado para Device Owner, ejecutar:
   `adb shell dpm set-device-owner cl.antumapu.aulacontrol/.AdminReceiver`
3. Completar la configuración inicial desde la tablet.

## Relay remoto
Es opcional. La supervisión de pantalla continúa operando exclusivamente por LAN. El relay se usa para telemetría, ubicación y comandos remotos, y debe publicarse exclusivamente por HTTPS.

## Cierre de versión
El código y las compilaciones Android/Windows deben aprobar CI antes de distribuirse. La validación física de cada modelo de tablet continúa siendo una prueba operativa del despliegue y no debe sustituirse por la compilación automatizada.
