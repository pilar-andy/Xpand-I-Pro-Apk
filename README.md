# Xpand Pro

App Android nativa para **estirar la pantalla en juegos** (beta con Blood Strike).
Usa [Shizuku](https://shizuku.rikka.app/) para cambiar la resolución con `wm size`, sin root.
Interfaz negra y roja, con un botón grande: **ACTIVAR / RESTAURAR**.

## Estado: Fase 1 (núcleo)
Prioridad: que activar y restaurar el estirado funcione de forma confiable en cualquier teléfono.

- Un botón que activa el estirado y, cuando está activo, lo restaura.
- Formatos: 16:9, 16:10, 4:3 (por defecto) y 5:4. La resolución se calcula a partir de la pantalla real de cada teléfono.
- Red de seguridad: tras activar, 15 s para confirmar; si no se confirma, se restaura sola.
- Se guarda el estado original antes de tocar nada, y después de cada cambio se verifica leyendo `wm size`.
- Guía de conexión con Shizuku dentro de la app (instalar, abrir, dar permiso).
- Botón para abrir Blood Strike si está instalado.

Más detalle en [docs/PLAN.md](docs/PLAN.md). Cómo instalarla: [docs/INSTALACION.md](docs/INSTALACION.md).

## Compilar
El APK se compila solo en GitHub: pestaña **Actions → Compilar APK → artefacto `XpandPro-debug-apk`**.
Para publicarlo en **Releases**, crea una etiqueta `v0.1.0`.

Localmente (JDK 17 + Android SDK + Gradle 8.7): `gradle :app:assembleDebug`.

## Estructura
```
app/src/main/
  aidl/…/IShellService.aidl      interfaz del servicio que corre con permisos de shell
  java/com/xpandpro/app/
    shell/ShellService.kt        ejecuta solo `wm size …` (lista blanca)
    shizuku/ShizukuManager.kt    estado, permiso y conexión con Shizuku
    display/                     lógica pura: leer, calcular, activar, restaurar
    ui/MainActivity.kt           pantalla principal
  res/                           tema negro y rojo
app/src/test/                    pruebas de la lógica con un teléfono simulado
.github/workflows/build-apk.yml  compila el APK y corre las pruebas
```
