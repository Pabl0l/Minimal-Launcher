# Minimal Launcher

Launcher Android minimalista, monocromo (blanco sobre negro), optimizado para bajo consumo. Pensado para Infinix Hot 5G.

## Características

- **Menú principal** estilo ahorro de batería: reloj grande, fecha y **hasta 10 accesos directos editables** (6 por defecto), en lista o cuadrícula.
- Cada acceso: eliges la **app** y un **icono** de un catálogo monocromo (64 glifos). Reordenables arrastrando.
- **Cajón de apps**: lista alfabética A-Z, **buscador** e **índice lateral** para saltar por letra.
- **47 fondos animados** monocromo dibujados a mano sobre `Canvas` (estrellas, lluvia, nebulosa, ADN, radar…). Se pausan solos cuando no se ven.
- **Fondo animado en la pantalla de bloqueo** vía live wallpaper — ver abajo.
- **17 caras de reloj** (digitales, en palabras, romano, binario, terminal, analógico) y 31 tipografías del sistema.
- **Gestos configurables** en las 4 direcciones y **panel de saldo** integrado.
- **12 plantillas** de estilo que fijan fuente + fondo + accesos de una sola vez.
- Solo librerías AndroidX básicas. Nada de frameworks pesados. Sin permisos declarados.

## Fondo animado en la pantalla de bloqueo

Android **no permite** a una app de terceros reemplazar la pantalla de bloqueo ni dibujar
en el Always-On Display del fabricante: no hay API pública para eso. La única vía oficial
es un **live wallpaper**, que el sistema renderiza *detrás* del keyguard.

`MinimalWallpaperService` hace exactamente eso: reusa las mismas 47 animaciones y las
dibuja en el fondo de pantalla, sin permisos.

**Cómo activarlo:** Ajustes de la app → *Pantalla de bloqueo* → **Activar fondo animado**.

En esa sección también se configura:

| Ajuste | Qué hace |
|--------|----------|
| Fondo de bloqueo | Animación propia del bloqueo, o *Igual que el inicio* |
| Reloj / Fecha / Batería | Textos opcionales que pinta el propio wallpaper (apagados por defecto: el keyguard ya trae su reloj) |
| Fluidez | Techo de fps (15–60, por defecto 30). Menos fps = menos batería |

**Batería:** al apagarse la pantalla el sistema llama a `onVisibilityChanged(false)` y el
bucle de dibujo se detiene por completo. Con el fondo en *Ninguno* y sin textos, el
wallpaper baja a 1 fps (solo sondea bloqueo/desbloqueo).

**Aviso:** algunos fabricantes (Transsion/XOS, Xiaomi…) imponen su propio fondo de
pantalla de bloqueo tipo "revista"/carrusel, que taparía el live wallpaper. Si no se ve la
animación al bloquear, desactiva esa función en los Ajustes del sistema.

> Nota: el menú principal del launcher tiene su propio fondo opaco, así que el live
> wallpaper se aprecia sobre todo en la pantalla de bloqueo.

## Gestos

| Acción | Resultado |
|--------|-----------|
| Toca un acceso configurado | Abre la app |
| Toca un acceso vacío | Elegir app |
| Mantén pulsado un acceso | Menú: elegir app / elegir icono / quitar |
| Desliza (4 direcciones) | Acción configurable: cajón, cámara, navegador, saldo, ajustes o una app concreta |

## Compilar (Android Studio)

1. Abre **Android Studio** → *Open* → selecciona la carpeta `MinimalLauncher`.
2. Espera el *Gradle Sync*. Si pide generar el Gradle Wrapper, acepta (o ejecuta `gradle wrapper` si tienes Gradle instalado).
3. Conecta el teléfono con **depuración USB** activada, o usa *Build > Build APK(s)*.
4. Instala:
   - Desde Android Studio: botón ▶ *Run*.
   - O copia el APK de `app/build/outputs/apk/debug/app-debug.apk` al teléfono e instálalo (permitir orígenes desconocidos).

## Ponerlo como launcher por defecto

Tras instalar, pulsa el botón **Home** del teléfono → elige **Minimal** → *Siempre*.
Para revertir: Ajustes → Aplicaciones → Aplicaciones predeterminadas → App de inicio.

## Requisitos

- minSdk 24 (Android 7) · targetSdk 34
- Kotlin · AGP 8.5 · Gradle 8.7 · JDK 17

## Estructura

```
app/src/main/
├── java/com/minimal/launcher/
│   ├── HomeActivity.kt            menú principal (reloj + accesos)
│   ├── AppDrawerActivity.kt       cajón A-Z + buscador + índice (y selector)
│   ├── SettingsActivity.kt        panel de ajustes "liquid glass"
│   ├── SettingsUi.kt / Sheets.kt  controles y hojas con preview en vivo
│   ├── FinanceActivity.kt         panel de saldo
│   ├── Finance.kt                 billeteras y movimientos (JSON en prefs)
│   ├── AnimatedBackgroundView.kt  motor de los 47 fondos animados
│   ├── MinimalWallpaperService.kt live wallpaper (fondo del bloqueo)
│   ├── LockOverlay.kt             reloj/fecha/batería sobre el wallpaper
│   ├── Backgrounds.kt             catálogo de fondos
│   ├── ClockFaces.kt              17 caras de reloj
│   ├── AnalogClockView.kt         reloj analógico dibujado a mano
│   ├── Fonts.kt                   31 tipografías del sistema
│   ├── Templates.kt               12 plantillas de estilo
│   ├── Gestures.kt                acciones de deslizamiento
│   ├── GestureFrameLayout.kt      detección de gestos
│   ├── Motion.kt                  easing e interacciones tipo iOS
│   ├── AppsRepository.kt          consulta/cachea apps lanzables
│   ├── Prefs.kt                   toda la persistencia (SharedPreferences/JSON)
│   ├── IconCatalog.kt             catálogo de iconos
│   ├── IconGridAdapter.kt         grid del selector de iconos
│   ├── AppListAdapter.kt          lista del cajón
│   ├── SideIndexView.kt           índice lateral A-Z
│   └── AppInfo.kt / Slot
└── res/
    ├── layout/                    pantallas y filas
    ├── xml/wallpaper.xml          declaración del live wallpaper
    └── drawable/ic_cat_*          iconos monocromo
```

## Personalizar

- **Añadir iconos**: crea un vector `ic_cat_*.xml` en `res/drawable` y añádelo a la lista en `IconCatalog.kt`.
- **Nº de accesos**: desde Ajustes → *Accesos directos* → *Cantidad* (límites en `Prefs.MIN_SLOTS` / `Prefs.MAX_SLOTS`).
- **Añadir un fondo animado**: escribe un `drawXxx` en `AnimatedBackgroundView.kt`, añádelo al `when` de `onDraw` y registra la clave en `Backgrounds.kt`. Aparece solo en los dos selectores y en el live wallpaper.
- **Formato de la fecha**: `format24Hour` en `res/layout/activity_home.xml` (menú principal) y `dateFormat` en `LockOverlay.kt` (bloqueo).
