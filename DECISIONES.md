# Decisiones

## 1. Resumen

| Tarea | Estado | Comentario |
|---|---|---|
| B1. Stored Procedures y ADO.NET | Completa | Login, listado y cambio de estado usan Stored Procedures y ADO.NET. Los valores se envían como parámetros SQL separados, indicando su tipo y tamaño; los controllers ya no construyen SQL. |
| B2. JWT | Completa | La API emite access tokens JWT con firma y vigencia de dos minutos, y verifica su firma antes de aceptarlos. Los técnicos solo consultan y modifican órdenes asignadas a su cuenta; el supervisor puede consultar todas. El refresh token se guarda como hash; al renovarlo, el anterior se invalida y se entrega uno nuevo. |
| A1. Órdenes en Kotlin + MVVM | Completa | Se reconstruyó el flujo de órdenes con Kotlin, Compose, MVVM y UDF, usando Repository y Retrofit en lugar de `AsyncTask` y XML. |
| A2. Sesión y tokens en el cliente | Completa | El access token y el refresh token se guardan cifrados con `AES-256-GCM` mediante una llave en `AndroidKeyStore`. La contraseña del usuario no se guarda. `AuthInterceptor` agrega el header `Authorization` en un único punto, renueva los tokens una sola vez ante un `401`; si la renovación falla, limpia la sesión y vuelve al login. |
| Opcionales |  |  |

## 2. Decisiones técnicas

1. **Backend — técnico autenticado:** la API obtiene el ID del técnico desde el JWT validado, no del body ni de un parámetro enviado por el cliente. `GetOrders` lista las órdenes asignadas a ese técnico y `UpdateStatus` solo permite cambiar sus propias órdenes.
2. **Android — Compose para el flujo de órdenes:** se eligió Compose en lugar de las vistas XML heredadas para construir esta pantalla.
3. **Android — Repository y Retrofit:** las llamadas de red se separan de la pantalla mediante Repository y Retrofit, y se usan corrutinas en lugar de `AsyncTask`.
4. **Backend — ADO.NET con Stored Procedures:** el acceso a datos queda fuera de los controllers. Cada valor se envía como parámetro SQL separado, con tipo y tamaño explícitos; no se usa un ORM.
5. **Backend — tokens:** la clave se configura mediante `.env`, ignorado por Git. La API verifica la firma, el emisor, la audiencia y el vencimiento del access token, configurado a dos minutos. Los refresh tokens se guardan como hash; al renovar, el anterior se invalida y se entrega uno nuevo.
6. **Android — Cifrado directo con AndroidKeyStore:** para proteger los tokens en `SessionManager`, se implementó `KeystoreCrypto` generando una llave simétrica AES de 256. Se descartó `EncryptedSharedPreferences` por encontrarse deprecada por Google.

## 3. Cambios en el contrato de la API

- `POST /api/auth/login`: las credenciales inválidas ahora responden `401 Unauthorized` en vez de `200` con `ok: false`. En un login correcto se mantiene el campo `token`.
- `GET /api/workorders`: ya no recibe `technicianId`; los técnicos consultan sus órdenes según el JWT y los supervisores pueden consultar todas. El filtro opcional `status` se conserva.
- `PUT /api/workorders/{id}/status`: conserva ruta y cuerpo. Solo un técnico autenticado puede cambiar sus propias órdenes; una orden inexistente o ajena responde `404 Not Found`.
- `POST /api/auth/refresh`: recibe el refresh token y devuelve un access token y un refresh token nuevos; el anterior queda invalidado.

## 4. Hallazgos

| # | Dónde | Problema | Riesgo | ¿Corregido? |
|---|---|---|---|---|
| 1 | `backend/db/01_schema.sql`, `backend/db/02_seed.sql`, login | Las contraseñas se guardan y comparan en texto plano. | Una filtración de la base expone credenciales reutilizables. |  |
| 2 | Controllers heredados | Las consultas SQL se construían concatenando valores recibidos del cliente. | Inyección SQL y cambios de datos no autorizados. | Sí; las operaciones B1 usan procedimientos y parámetros ADO.NET. |
| 3 | Autorización heredada de órdenes | Solo se comprobaba que el encabezado comenzara con `Bearer ` y el cliente enviaba el ID del técnico. | Un cliente podía falsificar el encabezado o consultar/modificar órdenes ajenas. | Corregido con validación JWT; los técnicos solo acceden a órdenes propias y el supervisor tiene acceso de lectura a todas. |
| 4 | Generación heredada del JWT | La clave estaba en el código y los tokens no tenían expiración. | Una clave expuesta o un token robado podía reutilizarse indefinidamente. | Clave por configuración; la API verifica la firma y el access token vence en dos minutos. |
| 5 | Refresh tokens | El flujo de refresh no existía en el código heredado. | El usuario tendría que iniciar sesión cada vez que vence el access token. | Corregido: se guarda el hash; al renovar, el token anterior se invalida y se guarda uno nuevo. |
| 6 | `android/.../LoginActivity.java` | Guardaba la contraseña del usuario y el token en texto plano en `SharedPreferences` y escribía el token JWT en Logcat (`Log.d`). | Exposición de credenciales y robo de sesión mediante logs o acceso al almacenamiento local. | Sí; tokens cifrados con `AndroidKeyStore`, la contraseña no se guarda y se eliminó el `Log.d` del token. |
| 7 | `android/.../OrdersActivity.java` | Usaba `AsyncTask` y `ProgressDialog` dentro de la Activity; además, tanto `onResume()` como el listener inicial del `Spinner` disparaban `loadOrders()`. | Fugas de memoria, cierre inesperado (`WindowLeaked`) al girar la pantalla mientras carga y doble petición HTTP al iniciar o rotar. | Sí; migrado a `OrdersViewModel` con corrutinas (`viewModelScope`), `StateFlow` y Jetpack Compose. |
| 8 | `android/.../OrdersActivity.java` (manejo de errores HTTP) | `LoadOrdersTask` ocultaba excepciones mostrando `"No hay órdenes"` ante fallos de red, y `UpdateStatusTask` asumía éxito (`return true`) sin verificar si el código HTTP era `200`, `401`, `403` o `404`. | El usuario no distinguía una lista vacía de una caída de red y veía confirmaciones falsas cuando una actualización fallaba. | Sí; Retrofit valida códigos `2xx` y la pantalla diferencia explícitamente entre estado vacío (`Empty`) y error (`Error` con botón `"Reintentar"`). |
| 9 | `android/.../AndroidManifest.xml` | Tiene `usesCleartextTraffic="true"` y `allowBackup="true"`. | En producción permitiría tráfico HTTP sin cifrar (MITM) y extracción de archivos mediante backup. | No; se conserva `usesCleartextTraffic` para conectar a `http://10.0.2.2:5080` y probar en el emulador local, pero en producción debe usarse HTTPS y `allowBackup="false"`. |

## 5. Con más tiempo

1. Migrar las contraseñas a hashes seguros.
2. Agregar manejo global de errores y pruebas automatizadas de la API.
3. Migrar también `LoginActivity` a Kotlin + MVVM + Jetpack Compose.

## 6. Uso de IA

Se usaron Gemini Argon y GitHub Copilot

## 7. Tiempo invertido

| Tarea | Tiempo aproximado |
|---|---:|
| B1. Stored Procedures y ADO.NET | 1 h  10 min|
| B2. JWT | 1 h  |
| A1. Órdenes en Kotlin + MVVM | 40 min |
| A2. Sesión y tokens en el cliente | 30 min |

## 8. Cómo probar

1. En PowerShell, genera una clave aleatoria de 32 bytes:

   ```powershell
   $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $keyBytes = New-Object byte[] 32
   $rng.GetBytes($keyBytes)
   [Convert]::ToBase64String($keyBytes)
   $rng.Dispose()
   ```

2. Crea `.env` en la raíz del repositorio, al mismo nivel que `docker-compose.yml`, y pega el resultado:

   ```text
   JWT_SIGNING_KEY=<clave_generada>
   ```

3. Desde esa misma raíz, inicia el entorno:

   ```powershell
   docker compose up --build
   ```

4. Ejecuta las solicitudes de `backend/requests.http` en orden. Las solicitudes de órdenes usan el Bearer token obtenido en el login y ya no envían `technicianId`. El access token vence en dos minutos. El archivo incluye pruebas de refresh, reutilización del refresh anterior, acceso del supervisor y rechazo de cambios de órdenes ajenas.

5. En el emulador, iniciar sesión (`jperez` / `123456`), realizar el flujo de cambio de órdenes y verificar que la sesión se refresca a pesar de haber pasado mucho tiempo.
