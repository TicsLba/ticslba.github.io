# PANGI V16

**Gestión inteligente de dispositivos educativos**

**Lema:** Observa · Protege · Responde

PANGI V16 es una reconstrucción completa del sistema de gestión institucional de tablets Android y su consola de administración Windows.

## Identidad técnica

- Producto: PANGI V16
- Android package: `cl.antumapu.pangi.v16`
- Android: PANGI V16 Agent
- Windows: PANGI V16 Console
- Relay: PANGI V16 Relay
- Provisioning: PANGI V16 Setup

## Principios no negociables

1. El launcher de Android no se entrega hasta que la sesión esté realmente preparada.
2. Reiniciar PANGI o perder supervisión nunca debe destruir una sesión activa.
3. Estudiante y Profesor usan usuarios Android temporales separados.
4. Administrador opera en el usuario propietario.
5. La supervisión visible usa las APIs oficiales de Android y no intenta ocultar permisos del sistema.
6. PANGI no registra contraseñas, PIN ni texto privado introducido en otras aplicaciones.
7. La primera versión debe priorizar estabilidad de sesión, teclado, HOME y cierre limpio antes de añadir funciones remotas avanzadas.

## Estado

Nueva base creada desde cero en la rama `pangi-v16`.
