# Porra Mundial 2026 

Aplicación Android para hacer porras del Mundial 2026. Carga tu Excel con predicciones, consulta partidos en tiempo real, sigue a tus goleadores y compite por puntos.

## Características

| Sección | Descripción |
|---------|-------------|
| **Próximos Partidos** | Lista de partidos con hora, equipos, canal TV y estado (LIVE/terminado) |
| **Goleadores** | Top 3 goleadores seleccionados con foto de perfil y puntuación |
| **Partidos** | Resultados de fase de grupos y eliminatorias con predicciones |
| **50 Preguntas** | Cuestionario Verdadero/Falso sobre el Mundial |
| **Puntuación** | Total de puntos acumulados en todas las categorías |

## Cómo funciona

1. **Carga tu Excel** con las predicciones (resultados, goleadores, preguntas, eliminatorias)
2. La app calcula automáticamente los puntos según aciertos
3. **Fotos de goleadores**: descarga automática desde Wikipedia de los 3 jugadores que hayas elegido
4. Resultados en tiempo real vía API de football-data.org
5. Canales TV automáticos vía scraping de la web de RTVE

## Sistema de Puntuación

| Categoría | Puntos |
|-----------|--------|
| Resultado exacto del partido | 30 pts |
| Goles de un equipo acertados | 10 pts c/u |
| 1er Goleador | 50 pts/gol |
| 2do Goleador | 30 pts/gol |
| 3er Goleador | 10 pts/gol |
| Pregunta Verdadero/Falso | 20 pts c/u |

## Tecnología

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **MVVM**: ViewModel + Hilt
- **Persistencia**: Room Database
- **Red**: Retrofit + OkHttp
- **Excel**: Apache POI
- **Imágenes**: Coil

## Requisitos

- Android 8.0 (API 26) o superior
- Archivo Excel con el formato de la porra (ver `NombreApellido.xlsx` de ejemplo)

## Instalación

Descarga el APK desde [Releases](https://github.com/J4ime/PorraWC2026/releases) e instálalo en tu dispositivo Android.

## Licencia

MIT
