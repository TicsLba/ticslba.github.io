# Tablet Escolar 2.1 · Gestión, Informes y Recuperación

## Objetivo
Tablet Escolar 2.1 amplía la consola de aula a una plataforma de gestión de flota institucional. Mantiene la supervisión en vivo por LAN y añade inventario, telemetría histórica por dispositivo, informes de uso y un modo de recuperación por pérdida o robo.

## Principios
- **El dispositivo es la unidad permanente de seguimiento.** Cada tablet conserva un `deviceId` estable.
- **Supervisión en tiempo real, no vigilancia histórica.** La pantalla en vivo no se graba por defecto.
- **Sin keylogging ni captura de credenciales.** No se almacenan textos escritos, contraseñas, PIN, tokens ni contenido de formularios.
- **Telemetría mínima y explicable.** Los informes almacenan eventos técnicos y métricas agregadas necesarias para administración escolar.
- **Ubicación institucional, no seguimiento personal.** La ubicación se asocia al equipo y se activa por política administrativa o durante Modo pérdida.
- **Fail-safe.** La ausencia del servidor remoto nunca debe impedir el uso normal dentro del colegio; la LAN sigue funcionando de forma independiente.

## Arquitectura

### 1. Agente Android
Responsable de:
- identidad estable del equipo;
- sesión temporal Estudiante/Profesor;
- supervisión de pantalla en vivo dentro de LAN;
- beacon técnico local;
- acumulación de telemetría de uso por dispositivo;
- obtención de ubicación cuando la política institucional la habilita;
- ejecución de comandos administrativos;
- Modo pérdida;
- envío HTTPS al relay cuando exista Internet.

### 2. Consola Windows
Tres áreas principales:

#### Aula
- mosaico en vivo;
- mensajes;
- abrir URL/app;
- Atención;
- cerrar sesión;
- estado de batería, sesión y supervisión.

#### Dispositivos
Inventario persistente con:
- ID estable;
- nombre institucional;
- fabricante/modelo;
- Android;
- estado administrado;
- última conexión;
- batería;
- última IP;
- última ubicación disponible;
- estado de recuperación (`normal`, `perdida`, `bloqueada`, `recuperada`).

#### Informes
Filtros:
- dispositivo;
- rango de fechas;
- rol de sesión;
- curso cuando corresponda.

Indicadores:
- tiempo en línea;
- tiempo de sesión;
- cantidad de sesiones;
- uso estudiante/profesor;
- duración media de sesión;
- batería mínima/media/máxima;
- desconexiones prolongadas;
- último contacto;
- aplicaciones en primer plano de forma agregada, sólo cuando el permiso correspondiente esté habilitado;
- eventos de recuperación y mantenimiento.

Exportación prevista: CSV/Excel y PDF.

### 3. Relay HTTPS
La LAN y el relay son canales separados.

`Tablet -> HTTPS -> Relay <- HTTPS -> Consola`

El relay nunca recibe pantalla en vivo por defecto. Sólo recibe telemetría pequeña y comandos pendientes.

Funciones mínimas:
- `POST /v1/telemetry`
- `GET /v1/devices`
- `GET /v1/devices/{id}/telemetry`
- `POST /v1/devices/{id}/commands`
- `GET /v1/devices/{id}/commands/pending`
- `POST /v1/devices/{id}/commands/{commandId}/ack`

Autenticación:
- HTTPS obligatorio;
- firma HMAC con clave derivada por establecimiento;
- timestamp y nonce para evitar replay;
- nunca exponer puertos de la tablet a Internet.

## Modelo de telemetría

Ejemplo conceptual:

```json
{
  "deviceId": "TAB-XXXXXXXX",
  "deviceName": "TABLET-08",
  "timestamp": 1789516800000,
  "battery": 74,
  "managed": true,
  "session": {
    "active": true,
    "role": "student",
    "course": "8° Básico"
  },
  "usage": {
    "onlineSecondsDelta": 60,
    "sessionSecondsDelta": 60,
    "screenInteractive": true
  },
  "location": {
    "enabled": true,
    "lat": -33.0,
    "lon": -71.0,
    "accuracyM": 22,
    "capturedAt": 1789516780000
  },
  "recovery": {
    "state": "normal"
  }
}
```

El nombre del usuario **no se necesita para los informes por dispositivo**. Puede mostrarse en vivo en la consola, pero no se incorpora al historial salvo que posteriormente exista una política institucional explícita que lo justifique.

## Uso de aplicaciones
No se registran páginas web, búsquedas ni contenido. Cuando Android permita Usage Access, se guardan sólo agregados como:

```text
com.google.android.apps.docs -> 1420 s
com.android.chrome            -> 2780 s
org.geogebra.android          -> 630 s
```

La consola resolverá nombre amigable cuando sea posible. Si Usage Access no está disponible, el informe sigue funcionando sin esta sección.

## Ubicación
Estados:
- `off`: política deshabilitada;
- `managed`: ubicación institucional periódica;
- `lost`: frecuencia reforzada por Modo pérdida.

La consola muestra siempre:
- última ubicación;
- precisión;
- fecha/hora de captura;
- antigüedad del dato.

No debe presentarse una ubicación antigua como si fuera actual.

## Modo pérdida
Sólo disponible a Administrador.

Al activarlo:
1. se registra el evento;
2. se envía comando remoto o LAN;
3. el dispositivo entra en estado `lost`;
4. se fuerza bloqueo mediante APIs de administración disponibles;
5. se muestra una pantalla institucional de recuperación;
6. se aumenta la frecuencia de telemetría/ubicación dentro de límites razonables de batería;
7. la consola conserva confirmación y última posición.

Comandos:
- `REQUEST_LOCATION`
- `LOST_MODE_ON`
- `LOST_MODE_OFF`
- `LOCK_NOW`
- `FORCE_LOGOUT`

No se promete impedir un restablecimiento físico de fábrica fuera de las capacidades oficiales de Android.

## Persistencia Windows
Base local por establecimiento, separada de la clave técnica:

- `devices.json`: inventario no sensible;
- `telemetry.db` o equivalente: eventos agregados;
- `settings.json`: configuración protegida por DPAPI;
- nombres de usuarios no incluidos en telemetría histórica por defecto.

Tablas lógicas:

### Devices
`device_id, device_name, model, android, first_seen, last_seen, last_battery, managed, recovery_state`

### UsageBuckets
`device_id, bucket_start, online_seconds, session_seconds, role, course, interactive_seconds`

### AppUsageBuckets
`device_id, bucket_start, package_name, seconds`

### LocationSamples
`device_id, captured_at, lat, lon, accuracy_m, reason`

### Events
`timestamp, device_id, event_type, metadata_minimal`

## Retención propuesta
- inventario: mientras el equipo pertenezca al establecimiento;
- métricas diarias: 24 meses por defecto;
- eventos técnicos: 12 meses;
- ubicación normal: conservar sólo última ubicación y, opcionalmente, 30 días si la institución lo habilita;
- ubicación de Modo pérdida: conservar mientras el incidente esté abierto y luego aplicar política institucional.

## Diseño visual Windows 2.1
Navegación principal:
- **Aula**
- **Dispositivos**
- **Informes**
- **Recuperación**

Ficha de dispositivo:
- nombre + ID;
- estado grande (`En línea`, `Sin conexión`, `Modo pérdida`);
- batería;
- modelo/Android;
- uso hoy / 7 días / 30 días;
- última ubicación;
- últimas incidencias;
- acciones administrativas.

## Diseño visual Android 2.1
La pantalla de administración agrega tarjetas:
- Estado del dispositivo;
- Telemetría e informes;
- Ubicación institucional;
- Recuperación y Modo pérdida;
- Privacidad y retención.

Estudiante/Profesor no ven controles administrativos.

## Fases de implementación
1. **2.1-A**: informes locales por dispositivo usando beacons LAN.
2. **2.1-B**: ubicación institucional y comandos de recuperación dentro de LAN.
3. **2.1-C**: relay HTTPS para funcionamiento entre redes.
4. **2.1-D**: exportación PDF/Excel y panel de salud de flota.

## Criterio de versión estable
No llamar “final” a 2.1 hasta probar en hardware real:
- creación/cierre de sesión temporal;
- telemetría sin duplicados;
- reinicio;
- pérdida de Wi-Fi;
- reconexión;
- Modo pérdida;
- ubicación con pantalla encendida/apagada;
- comandos por LAN;
- comandos por relay;
- desinstalación/liberación administrativa controlada;
- actualización firmada sobre la versión anterior.
