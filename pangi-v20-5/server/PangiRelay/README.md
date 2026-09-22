# PANGI Relay 11

Servidor central para operar tablets desde redes distintas. Mantiene telemetría, cola de comandos, auditoría y **solo el último cuadro de pantalla cifrado** cuando una consola solicita supervisión remota.

## Variables

- `AULAMOVIL_SCHOOL_KEY`: misma clave técnica configurada en la flota. No la publiques.
- `AULAMOVIL_DATA`: carpeta de datos (opcional).
- `AULAMOVIL_RETENTION_DAYS`: retención de telemetría/eventos. Predeterminado: 30.

## Internet / HTTPS

El proceso puede ejecutarse detrás de Caddy, nginx, IIS o un balanceador TLS. Las tablets aceptan únicamente un endpoint `https://...`.

Ejemplo de endpoint configurado en PANGI:

`https://aulamovil.colegio.cl/relay`

El servidor no almacena grabaciones de pantalla. Para la supervisión remota reemplaza el archivo del último cuadro; al dejar de solicitarla, dejan de llegar cuadros.
