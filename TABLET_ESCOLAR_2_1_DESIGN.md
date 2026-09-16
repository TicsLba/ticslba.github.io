# Tablet Escolar 2.1 · Gestión, Informes y Recuperación

## Objetivo
Tablet Escolar 2.1 amplía la consola de aula a una plataforma de gestión de flota institucional. Mantiene la supervisión en vivo por LAN y añade inventario, telemetría histórica por dispositivo, informes de uso por dispositivo y por usuario, y un modo de recuperación por pérdida o robo.

## Principios
- **El dispositivo es la unidad permanente de inventario.** Cada tablet conserva un `deviceId` estable.
- **El usuario es la unidad de responsabilidad de cada sesión.** Cada sesión queda asociada a una identidad, rol, curso, dispositivo, hora de inicio, hora de término y duración.
- **Supervisión en tiempo real, no vigilancia histórica.** La pantalla en vivo no se graba por defecto.
- **Sin keylogging ni captura de credenciales.** No se almacenan textos escritos, contraseñas, PIN, tokens ni contenido de formularios.
- **Telemetría mínima y explicable.** Los informes almacenan identidad de sesión, eventos técnicos y métricas agregadas necesarias para administración escolar.
- **Ubicación institucional, no seguimiento personal.** La ubicación se asocia al equipo y se activa por política administrativa o durante Modo pérdida.
- **Fail-safe.** La ausencia del servidor remoto nunca debe impedir el uso normal dentro del colegio; la LAN sigue funcionando de forma independiente.

## Identidad y responsabilidad de uso
Para que los informes sirvan para establecer responsables, el nombre ingresado manualmente no debe considerarse por sí solo una identidad verificada.

Cada sesión tendrá:
- `sessionId` único;
- `deviceId`;
- `userId` estable cuando exista autenticación institucional;
- `displayName`;
- rol (`student` o `teacher`);
- curso para estudiantes;
- `identitySource` (`declared`, `workspace`, `roster`, etc.);
- hora de inicio;
- última actividad;
- hora de término;
- motivo de cierre (`manual`, `inactivity`, `remote`, `reboot`, `lost_mode`).

### Fase inicial
Mientras la identificación siga siendo nombre + curso, Tablet Escolar guardará igualmente el usuario responsable declarado y marcará la fuente como **Identidad declarada**.

### Fase institucional recomendada
Para atribución fuerte, la sesión debe verificarse con una identidad institucional, preferentemente Google Workspace del establecimiento. En ese modo:
- el usuario selecciona o inicia su identidad institucional;
- Tablet Escolar conserva el identificador institucional necesario para la sesión;
- no almacena la contraseña de Google;
- los informes distinguen claramente **Identidad verificada** de **Identidad declarada**.

Esto evita que un estudiante pueda atribuir el uso a otro escribiendo simplemente su nombre.

## Arquitectura

### 1. Agente Android
Responsable de:
- identidad estable del equipo;
- sesión temporal Estudiante/Profesor;
- identidad responsable de la sesión;
- supervisión de pantalla en vivo dentro de LAN;
- beacon técnico local;
- acumulación de telemetría de uso por dispositivo y por sesión;
- uso agregado de aplicaciones cuando Usage Access esté habilitado;
- obtención de ubicación cuando la política institucional la habilita;
- ejecución de comandos administrativos;
- Modo pérdida;
- envío HTTPS al relay cuando exista Internet.

### 2. Consola Windows
Cuatro áreas principales:

#### Aula
- mosaico en vivo;
- usuario responsable visible;
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
- usuario actual;
- estado de recuperación (`normal`, `perdida`, `bloqueada`, `recuperada`).

#### Informes
Dos vistas principales:

**Por dispositivo**
- horas de uso;
- sesiones;
- usuarios que utilizaron el equipo;
- uso estudiante/profesor;
- batería;
- aplicaciones agregadas;
- eventos técnicos;
- última conexión;
- incidencias.

**Por usuario**
- nombre / identificador institucional;
- estado de identidad (`declarada` o `verificada`);
- curso y rol;
- dispositivos utilizados;
- cantidad de sesiones;
- horas acumuladas;
- primera y última sesión del período;
- aplicaciones utilizadas de forma agregada;
- cierres por inactividad;
- cierres remotos;
- incidencias asociadas a la sesión.

Filtros:
- usuario;
- dispositivo;
- curso;
- rol;
- rango de fechas;
- tipo de identidad.

Exportación prevista: CSV/Excel y PDF.

#### Recuperación
- solicitar ubicación;
- bloquear ahora;
- activar/desactivar Modo pérdida;
- ver última ubicación y precisión;
- historial de acciones administrativas.

### 3. Relay HTTPS
La LAN y el relay son canales separados.

`Tablet -> HTTPS -> Relay <- HTTPS -> Consola`

El relay nunca recibe pantalla en vivo por defecto. Sólo recibe telemetría pequeña y comandos pendientes.

Funciones mínimas:
- `POST /v1/telemetry`
- `GET /v1/devices`
- `GET /v1/devices/{id}/telemetry`
- `GET /v1/users/{id}/usage`
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
    "sessionId": "SES-...",
    "active": true,
    "userId": "workspace-id-or-null",
    "displayName": "Nombre Apellido",
    "identitySource": "workspace",
    "role": "student",
    "course": "8° Básico"
  },
  "usage": {
    "onlineSecondsDelta": 60,
    "sessionSecondsDelta": 60,
    "interactiveSecondsDelta": 42
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

## Registro de responsabilidad
La consola conserva un registro de auditoría de sesión separado de la telemetría agregada:

`timestamp, device_id, event, user, course`

Eventos mínimos:
- `LOGIN`;
- `LOGOUT`;
- `CHANGE`;
- `FORCE_LOGOUT`;
- `INACTIVITY_LOGOUT`;
- `LOST_MODE_LOCK`.

El objetivo es poder responder preguntas como:
- ¿Quién utilizó TABLET-08 entre 10:00 y 11:00?
- ¿Qué tablets utilizó un estudiante durante septiembre?
- ¿Cuánto tiempo estuvo activa cada sesión?
- ¿Quién fue el último usuario antes de una incidencia?
- ¿Qué usuario tenía la sesión activa cuando se emitió un bloqueo remoto?

## Uso de aplicaciones
No se registran páginas web, búsquedas ni contenido. Cuando Android permita Usage Access, se guardan sólo agregados por sesión y por período, por ejemplo:

```text
com.google.android.apps.docs -> 1420 s
com.android.chrome            -> 2780 s
org.geogebra.android          -> 630 s
```

Esos agregados sí pueden asociarse al usuario responsable de la sesión, porque forman parte del informe de uso. No se guarda el contenido visto o escrito dentro de esas aplicaciones.

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

La ubicación se atribuye al **dispositivo**, no se presenta como ubicación personal histórica del estudiante o profesor. Si una sesión estaba activa al momento de un evento, el informe puede indicar quién era el usuario responsable del equipo en ese instante, sin convertir el sistema en seguimiento personal continuo.

## Modo pérdida
Sólo disponible a Administrador.

Al activarlo:
1. se registra el evento;
2. se registra el usuario responsable que tenía la sesión activa, si existía;
3. se envía comando remoto o LAN;
4. el dispositivo entra en estado `lost`;
5. se fuerza bloqueo mediante APIs de administración disponibles;
6. se muestra una pantalla institucional de recuperación;
7. se aumenta la frecuencia de telemetría/ubicación dentro de límites razonables de batería;
8. la consola conserva confirmación y última posición.

Comandos:
- `REQUEST_LOCATION`
- `LOST_MODE_ON`
- `LOST_MODE_OFF`
- `LOCK_NOW`
- `FORCE_LOGOUT`

No se promete impedir un restablecimiento físico de fábrica fuera de las capacidades oficiales de Android.

## Persistencia Windows
Base local por establecimiento, separada de la clave técnica:

- `devices.json`: inventario técnico;
- `telemetry.db` o equivalente: métricas agregadas;
- `session-accountability.csv` / tabla equivalente: trazabilidad por usuario;
- `settings.json`: configuración protegida por DPAPI.

Tablas lógicas:

### Devices
`device_id, device_name, model, android, first_seen, last_seen, last_battery, managed, recovery_state`

### Sessions
`session_id, device_id, user_id, display_name, identity_source, role, course, started_at, ended_at, end_reason`

### UsageBuckets
`session_id, device_id, user_id, bucket_start, online_seconds, session_seconds, interactive_seconds`

### AppUsageBuckets
`session_id, device_id, user_id, bucket_start, package_name, seconds`

### LocationSamples
`device_id, captured_at, lat, lon, accuracy_m, reason`

### Events
`timestamp, device_id, session_id, user_id, event_type, metadata_minimal`

## Retención propuesta
- inventario: mientras el equipo pertenezca al establecimiento;
- sesiones e informes de responsabilidad: 24 meses por defecto;
- métricas agregadas: 24 meses;
- eventos técnicos: 12 meses;
- ubicación normal: sólo última ubicación y, opcionalmente, 30 días si la institución lo habilita;
- ubicación de Modo pérdida: mientras el incidente esté abierto y luego según política institucional.

La retención debe ser configurable y acompañarse de permisos de acceso en la consola para que no cualquier usuario pueda consultar informes nominales.

## Diseño visual Windows 2.1
Navegación principal:
- **Aula**
- **Dispositivos**
- **Informes**
- **Recuperación**

Informes incorpora dos pestañas:
- **Por dispositivo**
- **Por usuario**

Ficha de usuario:
- nombre;
- identidad declarada/verificada;
- curso/rol;
- tiempo de uso hoy / 7 / 30 días;
- dispositivos utilizados;
- sesiones;
- últimas incidencias;
- exportar informe.

## Diseño visual Android 2.1
La pantalla de administración agrega tarjetas:
- Estado del dispositivo;
- Telemetría e informes;
- Identidad y responsabilidad;
- Ubicación institucional;
- Recuperación y Modo pérdida;
- Privacidad y retención.

Estudiante/Profesor no ven controles administrativos.

## Fases de implementación
1. **2.1-A**: auditoría de sesiones e informes locales por dispositivo y usuario usando beacons LAN.
2. **2.1-B**: identidad institucional verificada + uso agregado de aplicaciones.
3. **2.1-C**: ubicación institucional y comandos de recuperación dentro de LAN.
4. **2.1-D**: relay HTTPS para funcionamiento entre redes.
5. **2.1-E**: exportación PDF/Excel y panel de salud de flota.

## Criterio de versión estable
No llamar “final” a 2.1 hasta probar en hardware real:
- creación/cierre de sesión temporal;
- atribución correcta de usuario;
- cambio de usuario;
- identidad verificada cuando esté habilitada;
- telemetría sin duplicados;
- informes por usuario y dispositivo;
- reinicio;
- pérdida de Wi-Fi;
- reconexión;
- Modo pérdida;
- ubicación con pantalla encendida/apagada;
- comandos por LAN;
- comandos por relay;
- desinstalación/liberación administrativa controlada;
- actualización firmada sobre la versión anterior.
