# Arquitectura PANGI V16

## Capas

### 1. PANGI V16 Agent — Android
Responsable de:
- Device Owner / Profile Owner.
- Puerta institucional antes del launcher.
- Creación de usuarios temporales Estudiante/Profesor.
- Persistencia del estado de sesión.
- Políticas por rol.
- Supervisión mediante MediaProjection.
- Telemetría e inventario.
- Ejecución de comandos autorizados.

### 2. PANGI V16 Console — Windows
Responsable de:
- Vista de dispositivos.
- Grupos y cursos.
- Supervisión en mosaico e individual.
- Comandos remotos.
- Distribución institucional de aplicaciones y archivos.
- Historial y exportación.

### 3. PANGI V16 Relay
Responsable de:
- Comunicación autenticada fuera de LAN.
- Cola de comandos.
- Telemetría.
- Estados de ejecución.
- Retención configurable.

### 4. PANGI V16 Setup
Responsable de:
- Instalación.
- Verificación de requisitos.
- Device Owner.
- Diagnóstico.
- Validación de firma y versión.

## Máquina de estados Android

```
UNPROVISIONED
    ↓
OWNER_GATE
    ↓
IDENTIFYING
    ↓
CREATING_USER
    ↓
PREPARING_USER
    ↓
WAITING_SUPERVISION
    ↓
ACTIVE
    ↓
CLOSING
    ↓
OWNER_GATE
```

Estados de recuperación:
- RECOVER_ACTIVE
- RECOVER_CLOSING
- ERROR_SAFE_GATE

## Regla crítica de HOME

El usuario temporal se crea y se prepara sin ser visible.
Antes de cambiar a él deben quedar listas, como mínimo:

- PANGI habilitado en el usuario.
- Profile Owner aplicado.
- HOME protegido registrado.
- teclado/IME válido disponible.
- políticas del rol aplicadas.
- apps institucionales disponibles.
- estado de sesión escrito de forma durable.

Solo después se solicita el cambio de usuario.

## Regla crítica de sesión

Una vez en ACTIVE:
- muerte/reinicio del proceso PANGI => recuperar ACTIVE.
- pérdida de MediaProjection => mantener ACTIVE y marcar supervisión como no disponible.
- pérdida de red => mantener ACTIVE y encolar telemetría.
- reinicio completo del dispositivo => cerrar sesión temporal y volver a OWNER_GATE.

## Perfiles

### Estudiante
- usuario temporal.
- instalación bloqueada.
- Play Store bloqueado.
- cuentas personales permitidas.
- cierre sin clave admin.
- inactividad predeterminada: 10 min.

### Profesor
- usuario temporal.
- instalación bloqueada.
- Play Store bloqueado.
- cuentas personales permitidas.
- cierre sin clave admin.
- inactividad predeterminada: 30 min.

### Administrador
- usuario propietario.
- acceso con contraseña administrativa.
- Google Play y mantenimiento disponibles.
- instalación manual permitida.
- acciones destructivas con confirmación reforzada.
