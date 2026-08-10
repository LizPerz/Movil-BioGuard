# Analisis local y alertas sin Internet

## Proposito

BioGuard Movil evalua telemetria ya persistida en Room cifrado para detectar cambios relevantes sin depender del backend ni de servicios push. La implementacion combina reglas de seguridad conservadoras con un modelo estadistico personalizado de una clase (`personalized-one-class-v1`).

## Flujo

1. El wearable envia telemetria por `MessageClient` a un nodo cercano y previamente vinculado.
2. El movil limita y valida ruta, tamano, estructura, fecha y rangos numericos.
3. La lectura se guarda de forma durable en la cola Room antes de enviar ACK.
4. El modelo compara la lectura con hasta 120 lecturas recientes del mismo paciente.
5. Las reglas criticas funcionan incluso mientras el modelo aprende.
6. La politica exige persistencia para riesgos no criticos, aplica cooldown y crea una notificacion local sin biometria visible en la pantalla bloqueada.
7. Los registros no presentes en nube permanecen pendientes hasta una sincronizacion automatica o manual exitosa.

## Modelo

- Tipo: deteccion de anomalias personalizada, no supervisada y ejecutada en el dispositivo.
- Entradas disponibles: BPM, temperatura, GSR, HRV y SpO2.
- Entrenamiento: ventana movil del paciente; requiere al menos 20 muestras validas.
- Salida: nivel local, puntuacion, motivos, version y estado de preparacion.
- Privacidad: no envia datos a un proveedor ML ni descarga modelos remotos.

## Limitaciones

- No es un diagnostico medico ni un instrumento clinico certificado.
- SpO2 y temperatura solo se consideran si el hardware y una API publica autorizada entregan un valor valido.
- HRV y estres derivados dependen de calidad de contacto y cadencia de pulso; deben interpretarse como tendencias.
- El usuario debe poder desactivar el analisis personalizado y las notificaciones desde Ajustes.

## Puerta de liberacion

- Tests de reglas, baseline, datos faltantes, valores no finitos y cooldown en verde.
- Migraciones Room verificadas conservando datos.
- Pruebas fisicas con reloj puesto y fuera de muneca, Bluetooth intermitente, modo avion, reinicio y bateria restringida.
- Revision clinica documentada de umbrales antes de afirmar cualquier finalidad medica.
- Seguimiento de falsos positivos y falsos negativos con datos consentidos y anonimizados antes de ajustar umbrales.
