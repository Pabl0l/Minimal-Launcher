# Plan del Proyecto: MinimalLauncher

## Visión General
- **Objetivo**: Launcher Android minimalista, monocromo (blanco sobre negro), optimizado para bajo consumo. Pensado para Infinix Hot 5G.
- **Stack tecnológico**: Kotlin · AGP 8.5 · Gradle 8.7 · JDK 17 · AndroidX
- **Arquitectura**: App Android nativa con Activities (sin frameworks pesados)

## Características Principales
- Menú principal estilo ahorro de batería: reloj grande, fecha y hasta 10 accesos directos editables
- Cajón de apps: lista alfabética A-Z, buscador e índice lateral
- 47 fondos animados monocromo dibujados a mano sobre Canvas
- Live wallpaper para pantalla de bloqueo (MinimalWallpaperService)
- 17 caras de reloj (digital, palabras, romano, binario, terminal, analógico)
- 31 tipografías del sistema
- Gestos configurables en 4 direcciones
- Panel de saldo/finanzas integrado
- 12 plantillas de estilo
- Catálogo de 64 iconos monocromo
- Solo librerías AndroidX básicas. Sin permisos declarados.

## Módulos

### Módulo 1: HomeActivity (Menú Principal)
- **Descripción**: Pantalla principal con reloj, fecha y accesos directos editables
- **Dependencias**: ClockFaces, Fonts, Backgrounds, AnimatedBackgroundView, GestureFrameLayout, Gestures, Prefs
- **Estado**: [X] Completado
- **Archivos**:
  - `HomeActivity.kt` - Activity principal
  - `AnalogClockView.kt` - Reloj analógico dibujado a mano
  - `ClockFaces.kt` - 17 caras de reloj
  - `Fonts.kt` - 31 tipografías del sistema

### Módulo 2: AppDrawerActivity (Cajón de Apps)
- **Descripción**: Lista alfabética de apps instaladas con buscador e índice lateral
- **Dependencias**: AppsRepository, AppListAdapter, SideIndexView, IconCatalog, IconGridAdapter
- **Estado**: [X] Completado
- **Archivos**:
  - `AppDrawerActivity.kt` - Activity del cajón
  - `AppListAdapter.kt` - Adapter de la lista
  - `SideIndexView.kt` - Índice lateral A-Z
  - `AppsRepository.kt` - Consulta/cache de apps
  - `AppInfo.kt` - Modelo de datos

### Módulo 3: SettingsActivity (Ajustes)
- **Descripción**: Panel de ajustes "liquid glass" con preview en vivo
- **Dependencias**: SettingsUi, Sheets, Prefs, Fonts, Backgrounds
- **Estado**: [X] Completado
- **Archivos**:
  - `SettingsActivity.kt` - Activity de ajustes
  - `SettingsUi.kt` - Controles de ajustes
  - `Sheets.kt` - Hojas con preview en vivo

### Módulo 4: FinanceActivity (Finanzas)
- **Descripción**: Panel de saldo con billeteras y movimientos (JSON en prefs)
- **Dependencias**: Finance, Prefs
- **Estado**: [X] Completado
- **Archivos**:
  - `FinanceActivity.kt` - Activity de finanzas
  - `Finance.kt` - Modelo de billeteras y movimientos

### Módulo 5: Fondos Animados
- **Descripción**: Motor de 47 fondos animados monocromo sobre Canvas
- **Dependencias**: Backgrounds (catálogo)
- **Estado**: [X] Completado
- **Archivos**:
  - `AnimatedBackgroundView.kt` - Motor de animaciones
  - `Backgrounds.kt` - Catálogo de fondos

### Módulo 6: Live Wallpaper (Pantalla de Bloqueo)
- **Descripción**: Fondo animado para pantalla de bloqueo vía WallpaperService
- **Dependencias**: AnimatedBackgroundView, Backgrounds, Prefs
- **Estado**: [X] Completado
- **Archivos**:
  - `MinimalWallpaperService.kt` - Servicio de wallpaper
  - `LockOverlay.kt` - Overlay de reloj/fecha/batería

### Módulo 7: Gestos
- **Descripción**: Detección de gestos de deslizamiento configurables
- **Dependencias**: GestureFrameLayout
- **Estado**: [X] Completado
- **Archivos**:
  - `Gestures.kt` - Acciones de deslizamiento
  - `GestureFrameLayout.kt` - Detección de gestos

### Módulo 8: Iconos
- **Descripción**: Catálogo de 64 iconos monocromo y selector
- **Dependencias**: IconCatalog
- **Estado**: [X] Completado
- **Archivos**:
  - `IconCatalog.kt` - Catálogo de iconos
  - `IconGridAdapter.kt` - Grid del selector

### Módulo 9: Persistencia
- **Descripción**: Toda la persistencia vía SharedPreferences y JSON
- **Estado**: [X] Completado
- **Archivos**:
  - `Prefs.kt` - Gestión de preferencias

### Módulo 10: Animaciones y Movimiento
- **Descripción**: Easing e interacciones tipo iOS
- **Estado**: [X] Completado
- **Archivos**:
  - `Motion.kt` - Easing y animaciones

### Módulo 11: Plantillas
- **Descripción**: 12 plantillas de estilo que fijan fuente + fondo + accesos
- **Dependencias**: Fonts, Backgrounds, Prefs
- **Estado**: [X] Completado
- **Archivos**:
  - `Templates.kt` - Plantillas de estilo

## Recursos
- `res/layout/` - Pantallas y filas
- `res/xml/wallpaper.xml` - Declaración del live wallpaper
- `res/drawable/ic_cat_*` - Iconos monocromo

## Testing
- [ ] Tests unitarios por módulo
- [ ] Tests de integración
- [ ] Coverage mínimo: 80%

## Estado Actual
- **Última actualización**: 2026-09-29
- **Progreso general**: 100% (código funcional completo)
- **Siguiente paso**: Evaluar mejoras, bugs pendientes o nuevas funcionalidades según indicaciones del usuario
