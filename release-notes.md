## ⚠️ IMPORTANTE - Dos versiones disponibles

Esta release incluye **dos APKs**:
- **PorraWC2026-v1.8.2.apk** (versión release, firmada con nuevo certificado)
- **PorraWC2026-v1.8.2-debug.apk** (versión debug, más compatible)

### Si Google Play Protect bloquea la instalación:

**Opción 1: Desactivar Play Protect temporalmente**
1. Abre Google Play Store
2. Toca tu perfil (arriba derecha)
3. Ve a **Play Protect**
4. Toca el ícono de configuración (⚙️)
5. Desactiva **"Escanear apps con Play Protect"**
6. Instala el APK
7. Reactiva Play Protect después

**Opción 2: Usar la versión debug**
Si la versión release sigue sin instalarse, usa **PorraWC2026-v1.8.2-debug.apk** que es más compatible.

**Opción 3: Instalar desde navegador**
1. Abre Chrome o tu navegador
2. Descarga el APK directamente desde GitHub
3. Cuando aparezca el aviso de Play Protect, toca **"Más detalles"** → **"Instalar de todos modos"**

---

## 🚀 Cambios incluidos (desde v1.7.6)

### Refactorización técnica
- Centralización del cálculo de puntos en PointsCalculator
- Unificación de normalización de nombres de equipos en TeamNameNormalizer
- Extracción de datos de partidos a MatchScheduleProvider
- Creación de LiveScoreService para obtener resultados en vivo
- Migración de claves API a BuildConfig con interceptores OkHttp
- Implementación de PrefsManager para preferencias con DataStore
- Refactorización de HomeViewModel para delegar en servicios

### Nuevas funcionalidades
- **Polling de minutos en vivo** usando la API de football-data.org (actualización cada 60s solo en primer plano)
- Conexión de pantallas huérfanas (GroupDetail, Results, Knockout, Players) a la navegación
- Implementación de ResultsViewModel.refreshLiveScores()
- HttpLoggingInterceptor condicional basado en tipo de build

### Testing
- **223 tests, 100% tasa de aprobación**
- Cobertura completa para todos los componentes principales

---

## 🔐 Credenciales del keystore (para futuras releases)

| Variable | Valor |
|----------|-------|
| **Keystore file** | `release.jks` |
| **Store password** | `PorraWC2026!` |
| **Key alias** | `porrawc2026` |
| **Key password** | `PorraWC2026!` |

**Guarda estas credenciales en un lugar seguro.** A partir de esta versión, todas las actualizaciones deben firmarse con este mismo keystore.
