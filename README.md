# BioGuard Móvil

Aplicación móvil de salud y monitoreo biométrico diseñada para ofrecer seguridad, control y seguimiento de pacientes en tiempo real, interactuando con dispositivos portátiles (wearables) y una plataforma en la nube.

## Requisitos
* Android Studio Ladybug o superior
* Android 8.0+ (API 26) o superior — probado en emuladores Pixel y dispositivos físicos
* JDK 17
* Conexión a Internet (para consumo de API REST)

## Arrancar el proyecto
```bash
# Clonar el repositorio
git clone https://github.com/LizPerz/Movil-BioGuard.git
cd BioGuardMovil

# Compilar la APK debug
./gradlew assembleDebug

# Instalar en el dispositivo (conectado por ADB)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*También puedes instalar directamente desde Android Studio usando `Run > Run 'app'`.*

## Estructura del proyecto
```text
app/src/main/java/com/example/bioguard_movil/
├── MainActivity.kt                         # Activity principal y punto de entrada
├── data/                                   # Capa de datos
│   ├── local/                              # Base de datos Room (Entities, DAOs)
│   └── repository/                         # Repositorios (Auth, Sensores, Alertas)
├── datastore/                              # Preferencias (UserPreferences, Tokens)
├── navigation/                             # Configuración de rutas (Screen.kt)
├── network/                                # Configuración Retrofit y ApiService
├── service/                                # Servicios en segundo plano
│   ├── BioGuardMessagingService.kt         # Firebase Cloud Messaging
│   ├── BioGuardMonitoringService.kt        # Guardián Nocturno
│   └── WearableConnector.kt                # Comunicación con WearOS
└── ui/                                     # Capa de presentación (Compose)
    ├── components/                         # Componentes reutilizables (Botones, Inputs)
    ├── model/                              # Modelos de vista
    ├── screens/                            # Pantallas (Login, Dashboard, QrScanner)
    ├── theme/                              # Tema global (Colores, Tipografía)
    └── viewmodel/                          # ViewModels con lógica de estado
```

## Módulo de Seguridad y Acceso
### Cómo funciona
La aplicación utiliza un sistema de autenticación seguro basado en tokens y validación estricta para proteger los datos médicos:
1. **Inicio de Sesión Base:** Autenticación por correo y contraseña.
2. **Código de Acceso:** Uso de un PIN seguro de 8 caracteres implementado con recuadros independientes (`BasicTextField`) y validación de foco dinámico.
3. **Escaneo QR:** Soporte para vinculación rápida de dispositivos y cuidadores mediante códigos QR.

### Flujo de datos de autenticación
```text
UI (LoginScreen) → AuthViewModel → AuthRepository
    ↓
SecureTokenStorage (Encriptado) & UserPreferences (DataStore)
```

## Módulo del Guardián Nocturno
### Por qué existe
El sistema necesita monitorear al paciente durante la noche de forma continua sin que la aplicación esté abierta en pantalla.

### Cómo funciona
`BioGuardMonitoringService` es un servicio en primer plano (Foreground Service) que asegura que la aplicación siga recolectando datos biométricos (frecuencia cardíaca, temperatura, GSR) enviados por el reloj, emitiendo alertas inmediatas si los niveles salen de los rangos seguros.

| Componente | Descripción |
| :--- | :--- |
| **Primer plano** | Muestra una notificación persistente indicando que el monitoreo está activo. |
| **Alertas** | Si detecta anomalías, dispara notificaciones de alta prioridad al cuidador. |
| **Sincronización** | Envía ráfagas de datos a la API mediante Retrofit. |

## Permisos
Los permisos se solicitan dinámicamente según la versión de Android y las funciones requeridas:
* `INTERNET`: Comunicación con la API.
* `CAMERA`: Escaneo de códigos QR para vinculación.
* `POST_NOTIFICATIONS`: (Android 13+) Alertas del Guardián Nocturno.
* `FOREGROUND_SERVICE`: Ejecución ininterrumpida de servicios médicos.
* `ACCESS_NETWORK_STATE`: Verificación de conectividad antes de sincronizar datos locales (Room) a la nube.

## Construcción y Testing
```bash
# Build debug
./gradlew assembleDebug

# Build release (requiere configuración de firma)
./gradlew assembleRelease

# Tests unitarios
./gradlew testDebugUnitTest

# Lint check
./gradlew lintDebug
```

## Tecnologías
* **Kotlin** + **Jetpack Compose** (UI declarativa moderna).
* **MVVM** (Arquitectura Modelo-Vista-ViewModel).
* **Retrofit + OkHttp** (Consumo de API REST).
* **Room Database** (Almacenamiento y caché offline).
* **DataStore** (Preferencias de usuario y tokens seguros).
* **Corrutinas Kotlin + StateFlow** (Programación reactiva y asíncrona).
* **CámaraX / Zxing** (Escaneo de códigos QR).
* **Firebase Cloud Messaging** (Notificaciones push).
