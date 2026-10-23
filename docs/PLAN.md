# Plan de Xpand Pro

Objetivo: app para otras personas, en distintos teléfonos y versiones de Android (mín. Android 7.0).
Prioridad absoluta: **activar y restaurar el estirado funciona siempre** antes que cualquier extra.

## Cómo funciona el estirado
Se reduce la resolución lógica del lado largo según un multiplicador (p. ej. 1080×2400 a 1.30x → 1080×1846) con `wm size`.
Si el teléfono rellena el panel con esa imagen, el juego se ve "estirado" (personajes más anchos). Restaurar es `wm size reset`
(o volver al valor que hubiera antes). Shizuku da el permiso de shell necesario, sin root.

**Importante:** Android conserva por defecto la proporción y deja barras negras cuando no coincide con la del panel
(`LogicalDisplay`), y cada fabricante lo cambia a su manera. Por eso la app incluye una prueba de compatibilidad
(círculo + borde) que cada usuario hace en su teléfono; no hay lógica por modelo.

### Experimento v1.3: densidad y `wm scaling off`
Según el código de Android, `wm scaling off` **desactiva** el escalado (la imagen se muestra a su tamaño lógico, sin ajustarla al panel),
así que en teoría no estira; se incluye solo como variante de la prueba para descartarlo con datos. `wm density` ajusta el tamaño de la
interfaz. La restauración deshace siempre tamaño, densidad y escalado (`wm scaling auto`). La lista blanca de comandos es estricta
(coincidencia completa) y tiene pruebas contra inyección.

## Fase 1 — Núcleo confiable (esta entrega)
- [x] Proyecto Android nativo (Kotlin, vistas XML, sin dependencias pesadas)
- [x] Conexión con Shizuku (UserService) y guía de estados
- [x] Activar / restaurar con verificación y reversión automática si algo falla
- [x] Confirmación de 15 s con restauración automática
- [x] Presets 16:9, 16:10, 4:3, 5:4 calculados desde la pantalla real
- [x] Interfaz negra y roja, botón principal
- [x] Pruebas unitarias de la lógica y workflow de GitHub Actions
- [ ] Probar en teléfonos reales (Blood Strike) y confirmar el nombre de paquete del juego

## Fase 2 — Comodidad en el juego
- Botón flotante / Quick Settings tile para activar y restaurar sin salir del juego
- Activar y abrir Blood Strike con un solo toque; restaurar al cerrar el juego
- Notificación con acción "Restaurar"
- Ajuste de densidad (`wm density`) para que la interfaz del juego no se vea demasiado grande

## Fase 3 — Compatibilidad amplia
- Resolución personalizada, perfiles por juego
- Casos especiales: Samsung/One UI, pantallas plegables y multi-pantalla, tablets
- Diagnóstico dentro de la app (modelo, Android, resolución física) para reportar fallos

## Fase 4 — Distribución
- APK firmado con llave fija (los de prueba cambian de firma en cada compilación)
- Releases en GitHub, actualizaciones, versión pública

## Notas de diseño
- Solo se permiten comandos `wm size` en el servicio privilegiado.
- El estado original se guarda en disco **antes** de cambiar la pantalla.
- Si el usuario sale de la app durante la cuenta atrás, se da por confirmado (ve el teléfono con normalidad).
- Si se reinicia el teléfono estirado, el cambio persiste: abrir Xpand Pro y pulsar RESTAURAR
  (o Ajustes > Pantalla). Una opción de recuperación al arrancar queda para la Fase 2.
