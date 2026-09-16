# Tablet Escolar 2.1 · Relay HTTPS (Google Apps Script)

Este relay permite que una tablet reporte telemetría y reciba comandos aunque esté conectada a una red distinta a la consola Windows.

## Qué almacena el relay
- `device_id` estable.
- marca de tiempo.
- un `payload` cifrado AES-GCM por Tablet Escolar.
- comandos pendientes también cifrados/autenticados.

El relay no necesita descifrar nombre, curso, ubicación ni resto de la telemetría. La consola autorizada los descifra con la clave técnica del establecimiento.

## Instalación
1. Crea una Hoja de cálculo de Google dedicada, por ejemplo `Tablet Escolar · Relay`.
2. Copia el ID de la hoja desde su URL.
3. Abre **Extensiones > Apps Script**.
4. Sustituye `Code.gs` por el contenido incluido en esta carpeta y usa también `appsscript.json`.
5. En **Configuración del proyecto > Propiedades de secuencia de comandos** crea:
   - `SCHOOL_KEY`: la misma clave técnica configurada en Tablet Escolar.
   - `SHEET_ID`: el ID de la hoja creada en el paso 1.
6. Implementa como **Aplicación web**:
   - ejecutar como: propietario del script;
   - acceso: cualquier usuario que pueda llegar al endpoint. La autenticación real la realiza Tablet Escolar mediante HMAC y cifrado de aplicación.
7. Copia la URL HTTPS terminada en `/exec`.
8. En la consola Windows abre Configuración y guarda esa URL como Relay.
9. Con las tablets visibles inicialmente por LAN, usa **Configurar relay en seleccionadas**. Desde ese momento cada sesión temporal heredará la URL del relay.

## Seguridad
- HTTPS obligatorio.
- `schoolTag` evita mezclar establecimientos.
- todas las solicitudes sensibles llevan HMAC-SHA256.
- la telemetría de usuario y ubicación viaja dentro de un payload AES-GCM.
- no se abren puertos de la tablet hacia Internet.
- el relay no transmite la pantalla en vivo.

## Retención
La función `cleanupOldTelemetry(days)` permite eliminar filas antiguas de `TelemetryLog`. Puede ejecutarse manualmente o mediante un activador diario. Una política razonable para telemetría detallada es 30–90 días y conservar resúmenes por un plazo institucional definido.

## Limitaciones
El modo remoto depende de que la tablet tenga acceso a Internet y de que Android permita al agente ejecutar su servicio. La ubicación depende además de que los servicios de ubicación del dispositivo estén disponibles. Tablet Escolar no promete impedir un restablecimiento físico de fábrica fuera de las capacidades oficiales de Android.
