# PermisosNuclearFront

Aplicación Android del proyecto PIN-SELES, desarrollada como TFG de 2º DAM en el IES Augustóbriga.

La app sirve para gestionar los permisos de trabajo con riesgo de incendio (PTRI) dentro de una central nuclear. Según el rol del usuario se pueden hacer unas cosas u otras: crear permisos, evaluarlos, autorizarlos, implantar el trabajo y cerrarlo cuando termina. También tiene un sistema de fichaje con GPS y notificaciones push.

## Tecnologías usadas

- Java
- Volley para las peticiones HTTP al backend
- SharedPreferences para guardar la sesión del usuario
- SQLite para guardar el fichaje activo de forma local
- Firebase Cloud Messaging para las notificaciones push
- AlarmManager para avisar cuando se supera el tiempo de turno
- GPS con FusedLocationProviderClient

El backend es una API REST en Node.js que está desplegada en Render.com. El repositorio del backend es independiente de este.

## Roles

Hay cuatro roles distintos:

- SOLICITANTE: crea permisos PTRI y puede anular o borrar los suyos
- BOMBERO: evalúa los permisos pendientes, los implanta y los cierra
- JEFE: autoriza o rechaza los permisos evaluados, y puede ver el historial completo
- ADMIN: puede hacer todo lo anterior

## Cómo ejecutarlo

1. Clona el repositorio
2. Abre el proyecto con Android Studio
3. Consigue el archivo google-services.json desde Firebase Console (Configuración del proyecto, sección de la app Android con el paquete com.bomberos.permisos) y ponlo dentro de la carpeta app/
4. Conecta un dispositivo Android o arranca un emulador con API 28 o superior
5. Dale a Run

El archivo google-services.json no está en el repositorio porque contiene claves privadas de Firebase. Sin él, la app compila pero las notificaciones push no funcionan.

## Estructura del proyecto

```
app/src/main/java/com/bomberos/permisos/
    MainActivity.java -- pantalla de arranque, redirige según sesión
    LoginActivity.java -- inicio de sesión
    HomeActivity.java -- menú principal
    PermisosActivity.java -- lista y gestión de permisos PTRI
    PlanoActivity.java -- mapa digitalizado de la central para abrir permisos
    FichajeActivity.java -- registro de entrada y salida con GPS
    UsuariosActivity.java -- gestión de usuarios (solo JEFE y ADMIN)
    PerfilActivity.java -- datos del usuario y cierre de sesión
    adapter/ -- adaptadores para los ListView
    service/ -- FCM y receptor de alarma de turno
    utils/ -- ApiHelper, SessionManager, DatabaseHelper
```

## Autores

Raúl Blázquez Ibáñez y Mauro Serrano Hevia -- 2º DAM, IES Augustóbriga
