# Plan de Mejora del Registro y Verificación Final

Tras realizar el "Clean" y "Rebuild" solicitados, he identificado una discrepancia entre la interfaz de usuario de registro y los requisitos de la API que podría estar impidiendo el registro correcto de nuevos usuarios.

## Hallazgos del Análisis
- **API:** El endpoint de registro (`api/Auth/register`) requiere `nombre`, `apellidoPaterno` y `apellidoMaterno` como campos separados.
- **Interfaz:** La pantalla `RegisterScreen` actualmente solo tiene un campo para "NOMBRE COMPLETO" y envía los apellidos como cadenas vacías.
- **Impacto:** Si el backend valida que los apellidos no estén vacíos, el registro fallará siempre.

## Propuesta de Cambios

### Interfaz de Usuario

#### [MODIFY] [RegisterScreen.kt](file:///C:/Users/alexi/Music/BioGuardMovil (1)/BioGuardMovil/app/src/main/java/com/example/bioguard_movil/ui/screens/RegisterScreen.kt)
- Añadir campos de entrada para "Apellido Paterno" y "Apellido Materno".
- Actualizar la lógica de validación para incluir estos nuevos campos.
- Pasar los valores correspondientes al `authViewModel.register`.

## Plan de Verificación

### Verificación Manual
- Ejecutar la aplicación tras los cambios.
- Navegar a la pantalla de registro.
- Completar todos los campos (Nombre, Apellido P., Apellido M., Correo, Contraseña).
- Verificar que el registro se procesa correctamente y redirige al flujo de bienvenida/onboarding.

## Preguntas Abiertas
- ¿Desea que mantenga el diseño actual de un solo campo y lo divida automáticamente, o prefiere los 3 campos explícitos para mayor precisión? (Recomiendo 3 campos dada la naturaleza médica de la app).
