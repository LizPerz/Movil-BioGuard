# BioGuard Movil

Aplicacion Android local-first (minSdk 28, targetSdk 36). Recibe telemetria del reloj Wear OS exclusivamente mediante el canal local del Wearable Data Layer, persiste primero en Room cifrado y sincroniza con la API de produccion cuando existe conectividad.

## Componentes

- `app/src/main/java/com/bioguard/movil/`: codigo de la aplicacion.
- `app/schemas/`: contratos Room versionados y validados mediante pruebas de migracion.
- `app/src/test/`: pruebas unitarias, de seguridad de payloads, ML, notificaciones y persistencia.
- `ON_DEVICE_ANALYSIS.md`: modelo, limites y controles del analisis local.
- `APK_DISTRIBUTION.md`: firma, verificacion y distribucion temporal por APK.

## Build y firma

La firma release se configura mediante `keystore.properties` no versionado o estas variables de entorno:

| Propiedad | Variable |
|---|---|
| `storeFile` | `BIOGUARD_STORE_FILE` |
| `storePassword` | `BIOGUARD_STORE_PASSWORD` |
| `keyAlias` | `BIOGUARD_KEY_ALIAS` |
| `keyPassword` | `BIOGUARD_KEY_PASSWORD` |

Las tareas `assembleRelease` y `bundleRelease` fallan si faltan credenciales. Movil y wearable deben usar el mismo certificado para el Data Layer de Wear OS.

## Alertas locales y ML

Las alertas se calculan y muestran completamente en el telefono, incluso sin Internet. No se utiliza Firebase ni otro proveedor push. El servicio procesa lecturas autenticadas del reloj, guarda primero en Room cifrado y aplica reglas conservadoras junto con un modelo personalizado de deteccion de anomalias.

Este analisis no es un diagnostico medico ni sustituye un dispositivo clinico. Consulte `ON_DEVICE_ANALYSIS.md` para sus limites y criterios de liberacion.

## CI y DevSecOps

`.github/workflows/ci.yml` ejecuta tests, lint, SBOM y genera APK/AAB firmados. `.github/workflows/security.yml` agrega CodeQL, deteccion de secretos, revision de dependencias y escaneo Trivy del SBOM.
