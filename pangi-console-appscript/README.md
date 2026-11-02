# Pangi Console 3.1 — despliegue

Pangi Console es una consola de flota para tablets institucionales administradas con Pangi como Device Owner.

## Qué muestra

- Tablet en línea / fuera de línea.
- Última conexión (heartbeat).
- Fabricante, modelo y versión Android.
- Batería y estado de carga.
- GPS activo/inactivo.
- Última ubicación y precisión.
- Mapa de la flota.
- Comando “Ubicar ahora”.
- Comando “Bloquear”.

El estado “fuera de línea/apagada” se infiere cuando una tablet deja de reportar durante 180 segundos. Pangi intenta además registrar ACTION_SHUTDOWN, pero el heartbeat es la fuente principal porque un equipo sin energía no puede enviar datos.

## Privacidad

La consola identifica dispositivos institucionales por un UUID generado por Pangi. No registra identidad de estudiantes, cuentas, cámara, micrófono, mensajes ni contenido personal. La ubicación funciona mediante un servicio en primer plano y Android muestra una notificación persistente mientras el monitoreo está activo.

## 1. Crear la consola

1. Crea una Hoja de cálculo de Google nueva, por ejemplo “Pangi Console”.
2. Abre Extensiones → Apps Script.
3. Reemplaza Code.gs por el contenido de este paquete.
4. Crea un archivo HTML llamado exactamente Index y pega Index.html.
5. Ejecuta manualmente setupPangiConsole() una sola vez y concede los permisos solicitados.
6. Vuelve a la hoja. Se crearán:
   - Devices
   - Config
7. En Config encontrarás:
   - DEVICE_TOKEN: token que se configura en las tablets.
   - ADMIN_TOKEN: token para entrar a la consola web.

No publiques estos tokens.

## 2. Desplegar como Web App

1. En Apps Script elige Implementar → Nueva implementación.
2. Tipo: Aplicación web.
3. Ejecutar como: tú.
4. El acceso debe permitir que las tablets institucionales puedan invocar la URL sin iniciar sesión. El DEVICE_TOKEN sigue siendo obligatorio para cada solicitud.
5. Copia la URL HTTPS terminada en /exec.

## 3. Vincular una tablet

En Pangi 3.1:

Administración → Consola de flota → Configurar consola

Ingresa:
- URL HTTPS /exec.
- DEVICE_TOKEN.
- Alias de la tablet, por ejemplo SAM-01 o TAB-AULA-07.

Pulsa “Probar conexión” y luego activa “Monitoreo remoto”.

## 4. Uso de la consola

Abre la misma URL /exec en un navegador.

- Ingresa ADMIN_TOKEN.
- Verde = tablet con heartbeat dentro de los últimos 180 segundos.
- Gris = tablet fuera de línea.
- “Ubicar ahora” solicita una actualización de ubicación.
- “Bloquear” envía LOCK; la tablet lo ejecuta mediante DevicePolicyManager.lockNow() cuando recibe el comando.

El heartbeat se realiza aproximadamente cada 60 segundos. Por lo tanto, un comando puede tardar hasta cerca de un minuto en ejecutarse.

## Importante sobre bloqueo

LOCK usa el bloqueo de sistema de Android. Si la tablet no tiene PIN, patrón o contraseña de pantalla configurados, Android puede limitarse a apagar la pantalla. Para seguridad física real conviene configurar un método de desbloqueo en los dispositivos.

## Launcher

Pangi Console no altera esta regla de Pangi 3.x:

Pangi NO es HOME y NO reemplaza el launcher Samsung/Lenovo.
