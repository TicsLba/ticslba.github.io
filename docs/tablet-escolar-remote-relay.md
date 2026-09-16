# Tablet Escolar · ubicación y modo pérdida

Documento de arquitectura para la extensión 2.1. La ubicación precisa sólo se transmite cuando la función institucional está habilitada y el dispositivo ha otorgado los permisos de Android. Para operar fuera de la LAN, la consola y las tablets requieren un relay HTTPS autenticado; el GPS por sí solo no atraviesa NAT ni permite control remoto.

Comandos previstos: `LOCATION_NOW`, `LOCK_DEVICE`, `LOST_MODE_ON`, `LOST_MODE_OFF`.

El modo pérdida debe mostrar un aviso visible de dispositivo institucional, bloquear el uso normal mediante Device Owner y mantener una notificación persistente de ubicación cuando Android lo requiera. No se afirma que impida un restablecimiento físico de fábrica.
