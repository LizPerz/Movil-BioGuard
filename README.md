# BioGuard Móvil

Aplicación Android (minSdk 28, targetSdk 36) de monitoreo del Guardián Nocturno: recibe lecturas biométricas del reloj Wear OS, las sincroniza con el backend y mantiene una cola offline local (Room).

## Estructura

- `app/src/main/java/com/example/bioguard_movil/` — código fuente
- `app/schemas/` — esquemas exportados de Room (se versionan para poder testear migraciones)
- `app/src/test/java/com/example/bioguard_movil/data/local/BioGuardMigrationTest.kt` — test de migración Room v1→v3

## Build y firma

La firma de release se resuelve en `app/build.gradle.kts` desde `keystore.properties` (no versionado) o variables de entorno (CI):

| Propiedad | Variable de entorno |
|---|---|
| `storeFile` | `BIOGUARD_MOVIL_STORE_FILE` |
| `storePassword` | `BIOGUARD_MOVIL_STORE_PASSWORD` |
| `keyAlias` | `BIOGUARD_MOVIL_KEY_ALIAS` |
| `keyPassword` | `BIOGUARD_MOVIL_KEY_PASSWORD` |

Ejemplo local (`keystore.properties`, no commiteado):

```properties
storeFile=C:/ruta/bioguard-release.jks
storePassword=***
keyAlias=bioguard
keyPassword=***
```

Sin ese archivo, `assembleRelease`/`bundleRelease` generan artefactos sin firmar (los tests y debug builds no se ven afectados).

## Firebase Cloud Messaging (notificaciones push)

El código de FCM ya existe (`service/BioGuardMessagingService.kt`), pero para que funcione en runtime hace falta:

1. Descargar `google-services.json` desde Firebase Console (proyecto de BioGuard) y colocarlo en la raíz del módulo: `app/google-services.json`.
2. Aplicar el plugin de Google Services en `app/build.gradle.kts` (root `plugins` + `alias(libs.plugins.google.services)`), y registrarlo en `gradle/libs.versions.toml`:

   ```toml
   google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
   ```

3. Crear el canal de notificaciones y solicitar `POST_NOTIFICATIONS` en runtime (ya se pide en `MainActivity.requestRuntimePermissions()`).

**Advertencia:** mientras `google-services.json` no esté presente NO se debe aplicar el plugin `com.google.gms.google-services`, porque el build falla al no encontrar el archivo.

## CI

`.github/workflows/ci.yml` corre unit tests y compila el release AAB. Los secretos `BIOGUARD_MOVIL_*` se configuran en GitHub → Settings → Secrets.
