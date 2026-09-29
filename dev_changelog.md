# Changelog del Proyecto

---

## [2026-09-29 00:00] - Inicialización del Proyecto

### Qué se hizo
- Exploración completa de la estructura del proyecto MinimalLauncher
- Análisis de todas las dependencias, arquitectura y módulos
- Creación de archivos de contexto: dev_plan.md, dev_changelog.md, dev_weaknesses.md

### Por qué (Justificación)
- Establecer contexto base para continuar el desarrollo
- Documentar el estado actual del proyecto para futuras sesiones

### Decisiones tomadas
- **Documento de plan**: Se documentaron los 11 módulos existentes con su estado actual
- **Enfoque**: Análisis completo antes de implementar cambios

### Investigación realizada
- Exploración de estructura de archivos (25 archivos Kotlin)
- Revisión de build.gradle.kts (dependencias y configuración)
- Análisis del README.md (documentación existente)

### Resultado
- Proyecto completamente documentado
- Todos los módulos identificados y catalogados
- Base lista para recibir cambios del usuario

### Archivos modificados
- `dev_plan.md` - Creado: plan maestro del proyecto
- `dev_changelog.md` - Creado: este registro de cambios
- `dev_weaknesses.md` - Creado: análisis de puntos débiles

---

## [2026-09-29 01:00] - Fase 1: Limpieza de código base

### Qué se hizo
- Crear `Dimens.kt` con funciones `dp()`, `dpF()` y `sp()` centralizadas
- Reemplazar `dp()` duplicada en 7 archivos (SideIndexView, LockOverlay, AnalogClockView, HomeActivity, AnimatedBackgroundView, SettingsUi, Sheets)
- Reemplazar colores hardcodeados en `FinanceActivity.kt` por `R.color.*`
- Aumentar áreas táctiles a 48dp+ en layouts (home_shortcut, item_app, home_shortcut_grid)
- Añadir colores `positive`, `negative`, `positive_dim`, `negative_dim`, `surface` a `colors.xml`

### Por qué
- Eliminar código duplicado (DRY)
- Mejorar accesibilidad táctil
- Centralizar colores para mantener consistencia

### Commits
- `8f9d898` feat: Fase 1 - Limpieza de código base

---

## [2026-09-29 01:30] - Fase 2: Mejoras visuales

### Qué se hizo
- Botones "Ingreso" y "Egreso" con colores `positive`/`negative` (verde/rojo sutil)
- Nuevos drawables `pill_bg_green.xml` y `pill_bg_red.xml` con bordes de color
- Gear icon más grande (36dp) con padding y `transitionName`
- Animación de rotación del gear al abrir ajustes (90° en 200ms)
- Parallax en FinanceActivity: el total se desplaza al 30% del scroll y se atenúa
- Letter-spacing mejorado en fecha y labels de finanzas
- Font-weight medium en total y botones de acción

### Por qué
- Añadir jerarquía visual con colores de acción
- Mejorar la experiencia de interacción con animaciones
- Añadir profundidad con parallax

### Commits
- `fff5258` feat: Fase 2 - Mejoras visuales

---

## [2026-09-29 02:00] - Fase 3: Refactorizar a Coroutines

### Qué se hizo
- Añadir dependencias: `kotlinx-coroutines-android:1.8.1`, `lifecycle-runtime-ktx:2.8.4`, `lifecycle-viewmodel-ktx:2.8.4`
- Refactorizar `AppDrawerActivity.loadAppsAsync()` de `Thread { ... runOnUiThread { } }` a `lifecycleScope.launch(Dispatchers.IO)`

### Por qué
- Eliminar código legacy de threads
- Gestionar correctamente el ciclo de vida con coroutines
- Preparar la base para MVVM reactivo

### Commits
- `3aae646` feat: Fase 3 - Refactorizar a Coroutines

---

## [2026-09-29 02:30] - Fase 4a: Búsqueda universal

### Qué se hizo
- Nuevo `UniversalSearch.kt`: búsqueda mixta de apps + ajustes + web
- Nuevo `SearchResultsAdapter.kt`: adapter para resultados tipados
- Nuevo layout `item_search_result.xml` con icono, label y subtitle
- Nuevo icono `ic_cat_apps.xml`
- `AppDrawerActivity`: al escribir, cambia de lista de apps a búsqueda universal
- Resultados incluyen: apps instaladas (máx 15), atajos de ajustes (6ategorías), y opción de buscar en Google
- En modo pick (selección de app), mantiene la lista normal

### Por qué
- Una sola barra de búsqueda para todo (apps, ajustes, web)
- Experiencia de búsqueda moderna tipo iOS/Android stock

### Commits
- `ff6c715` feat: Fase 4a - Búsqueda universal en el drawer

---

## [2026-09-29 03:00] - Fase 4b: Widget de información del sistema

### Qué se hizo
- Nuevo `SystemInfo.kt`: obtiene batería, WiFi y almacenamiento del dispositivo
- Nuevo `SystemInfoView.kt`: vista custom dibujada sobre Canvas (estilo minimalista monocromo)
- Widget en el home muestra: batería (porcentaje + estado de carga), WiFi (SSID + señal), almacenamiento (usado/total)
- Se actualiza automáticamente cada vez que el home se reanuda
- Diseño consistente con la estética del launcher

### Por qué
- Información útil sin permisos de red
- Widget visual que añade funcionalidad al home sin romper el diseño minimalista

### Commits
- `f767d0d` feat: Fase 4b - Widget de información del sistema en el home

---
