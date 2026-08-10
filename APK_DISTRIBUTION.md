# Distribución temporal por APK

## Reglas de release

1. Móvil y Wearable deben compilarse con el mismo keystore de release para conservar la identidad requerida por Wear OS Data Layer.
2. El secreto `BIOGUARD_KEYSTORE_BASE64` contiene el `.jks` compartido codificado en base64. Las contraseñas y alias se guardan exclusivamente como GitHub Secrets.
3. Cada build de `main` genera APK firmado, AAB y `SHA256SUMS.txt`. El APK es el artefacto de distribución actual; el AAB se conserva para la migración futura a Google Play.
4. Nunca distribuir un APK de debug ni uno cuyo `apksigner verify` haya fallado.
5. Publicar el APK y su SHA-256 mediante un canal autenticado. El hash debe comunicarse por un canal independiente cuando sea posible.
6. Conservar el keystore en custodia cifrada y con respaldo probado. Perderlo impide actualizar instalaciones existentes con la misma identidad.

## Configuración de GitHub Secrets

```text
BIOGUARD_KEYSTORE_BASE64
BIOGUARD_STORE_PASSWORD
BIOGUARD_KEY_ALIAS
BIOGUARD_KEY_PASSWORD
```

El valor base64 se genera fuera del repositorio:

```bash
base64 -w 0 bioguard-release.jks
```

## Instalación y actualización

- Verificar `SHA256SUMS.txt` antes de instalar.
- Instalar Móvil en el teléfono y Wearable en el reloj.
- No desinstalar para actualizar: instalar la versión superior sobre la existente preserva datos y vínculo cuando la firma coincide.
- El pipeline asigna un `versionCode` creciente usando `github.run_number`.
- Ante una vulnerabilidad crítica, retirar el APK anterior y exigir actualización.

## Backend móvil

La aplicación móvil continúa usando `https://bioguard-api-lkvnq.ondigitalocean.app/`. El backend derivado desplegado en Render es un ambiente de pruebas separado y no reemplaza ese endpoint en las APK productivas.
