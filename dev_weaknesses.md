# Puntos Débiles y Riesgos del Proyecto

## 🔴 Críticos (Requieren atención inmediata)
_Ninguno identificado_

## 🟡 Moderados (Planificar revisión)

### Arquitectura: Actividades Independientes
- **Riesgo**: LasActivities (Home, Settings, Finance, AppDrawer) no siguen un patrón arquitectónico moderno (MVVM, MVI). La lógica de negocio está mezclada con la UI.
- **Impacto**: Mantenibilidad a largo plazo, dificultad para testing
- **Probabilidad**: Media
- **Mitigación**: Evaluar migración a un patrón más estructurado si se añaden funcionalidades complejas
- **Estado**: [ ] Pendiente

### Testing
- **Riesgo**: No existen tests unitarios ni de integración
- **Impacto**: Regresiones silenciosas, difícil de refactorizar con confianza
- **Probabilidad**: Alta (si se hace cualquier cambio)
- **Mitigación**: Añadir tests críticos antes de cambios significativos
- **Estado**: [ ] Pendiente

### Dependencias Obsoletas
- **Riesgo**: Kotlin 1.9.24 y AGP 8.5.2 podrían quedar desactualizadas
- **Impacto**: Incompatibilidades futuras, misses de seguridad
- **Probabilidad**: Baja (a corto plazo)
- **Mitigación**: Actualizar a versiones latest cuando sea necesario
- **Estado**: [ ] Pendiente

## 🟢 Leves (Monitorear)

### Documentación del Código
- **Riesgo**: Falta de KDoc en funciones públicas
- **Impacto**: Dificultad para nuevos desarrolladores
- **Mitigación**: Añadir documentación incrementalmente

### Tamaño del Código
- **Riesgo**: Algunos archivos pueden crecer demasiado (AnimatedBackgroundView.kt con 47 fondos)
- **Impacto**: Legibilidad
- **Mitigación**: Extraer fondos a archivos separados si crece más

### Sin Persistencia de Estado en Repos
- **Riesgo**: Todo está en SharedPreferences/JSON local, sin backup
- **Impacto**: Pérdida de datos al desinstalar
- **Mitigación**: Aceptar como feature del launcher (sin cloud)

## Acciones Recomendadas (Priorizadas)
1. **Preguntar al usuario** qué cambios específicos quiere realizar
2. Evaluar añadir tests antes de cambios significativos
3. Considerar refactoring si se añaden funcionalidades complejas

## Historial de Mitigaciones
| Fecha | Riesgo | Acción tomada | Resultado |
|-------|--------|---------------|-----------|
| 2026-09-29 | Documentación incompleta | Creación de archivos .md | Resuelto |
