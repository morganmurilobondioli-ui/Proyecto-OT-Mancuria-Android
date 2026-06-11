# Guia de flujo de usuario - Mancuria Android

Esta guia acompana los comentarios agregados al codigo. La idea es que estudies como nace, entra, se valida, se modifica, recupera contrasena y sale un usuario.

## 1. Archivos que debes leer

```text
app/src/main/java/com/company/appMancuria/models/LoginActivity.java
app/src/main/java/com/company/appMancuria/AdminPanelActivity.java
app/src/main/java/com/company/appMancuria/PerfilActivity.java
app/src/main/java/com/company/appMancuria/MainActivity.java
app/src/main/java/com/company/appMancuria/models/Usuario.java
```

## 2. Idea principal

El proyecto separa al usuario en dos capas:

```text
FirebaseAuth
    -> correo
    -> contrasena
    -> sesion
    -> UID

Firestore / usuarios/{uid}
    -> nombre
    -> correo de referencia
    -> fotoUrl
    -> rol
    -> estado
```

FirebaseAuth responde: "Este correo y password son validos".

Firestore responde: "Este trabajador esta autorizado, tiene este rol y esta activo o suspendido".

## 3. Como inicia sesion

Archivo:

```text
LoginActivity.java
```

Flujo:

```text
onCreate()
    -> inicializa FirebaseAuth
    -> inicializa FirebaseFirestore
    -> revisa SharedPreferences
    -> si ya hay sesion valida, abre MainActivity
    -> si no, muestra formulario
```

Cuando toca ingresar:

```text
intentarLoginInterno()
    -> lee email y password
    -> valida campos vacios
    -> desactiva boton
    -> mAuth.signInWithEmailAndPassword(email, password)
    -> si funciona: cargarPerfilUsuario()
    -> si falla: muestra error
```

Despues del Auth:

```text
cargarPerfilUsuario()
    -> obtiene currentUser
    -> lee usuarios/{currentUser.uid}
    -> si no existe perfil: cierra sesion
    -> si estado == suspendido: cierra sesion
    -> si todo esta bien: guarda sesion local y abre MainActivity
```

## 4. Como se crea un usuario

Archivo:

```text
AdminPanelActivity.java
```

Solo un admin llega a esta pantalla desde el menu de usuario.

Flujo:

```text
fabNuevoUsuario
    -> mostrarDialogoUsuario(null)
    -> usuarioExistente == null significa "crear"
    -> valida nombre, correo y password temporal
    -> crearTrabajadorAuth(...)
```

Crear trabajador tiene dos pasos:

```text
crearTrabajadorAuth()
    -> usa FirebaseAuth secundario
    -> createUserWithEmailAndPassword(correo, passwordTemporal)
    -> obtiene UID
    -> crea Usuario en Firestore con document(uid)
    -> cierra la sesion secundaria
```

Por que usa Auth secundario:

Si se usara el Auth principal, al crear un trabajador nuevo la app podria dejar autenticado al trabajador creado y sacar al administrador. La instancia secundaria evita eso.

## 5. Como se edita un trabajador

Archivo:

```text
AdminPanelActivity.java
```

Flujo:

```text
mostrarOpcionesTrabajador()
    -> Editar
    -> mostrarDialogoUsuario(usuarioExistente)
    -> actualiza usuarios/{id}
```

Importante:

Editar en este panel cambia el documento Firestore:

```text
nombre
correo
usuario
rol
```

Pero no cambia necesariamente el correo real de FirebaseAuth. Para cambiar password, el usuario usa perfil propio o recuperacion por correo.

## 6. Como se suspende o reactiva

Archivo:

```text
AdminPanelActivity.java
```

Flujo:

```text
mostrarOpcionesTrabajador()
    -> Suspender/Reactivar
    -> update("estado", "suspendido" o "activo")
```

No borra la cuenta Auth. Solo cambia estado en Firestore.

Luego `LoginActivity` hace cumplir esa regla:

```text
if estado == suspendido
    -> limpiarSesion()
```

Y `MainActivity` tambien vuelve a revisar el estado al cargar el rol.

## 7. Como modifica su perfil

Archivo:

```text
PerfilActivity.java
```

Flujo:

```text
cargarPerfil()
    -> correo desde FirebaseAuth
    -> nombre/foto desde Firestore
```

Guardar:

```text
guardarCambios()
    -> valida nombre
    -> valida nueva password si existe
    -> si hay foto nueva: subirFotoYGuardar()
    -> si no: guardarPerfil()
```

Foto:

```text
subirFotoYGuardar()
    -> sube usuarios/{uid}/perfil.jpg a Firebase Storage
    -> obtiene URL
    -> guarda URL en Firestore
```

Nombre/foto:

```text
guardarPerfil()
    -> update usuarios/{uid}
    -> guarda cache local en SharedPreferences
```

## 8. Como cambia su contrasena

Archivo:

```text
PerfilActivity.java
```

Firebase exige reautenticacion para cambiar password.

Flujo:

```text
actualizarPasswordSiCorresponde()
    -> si nuevaPassword esta vacia: termina
    -> obtiene email actual de FirebaseAuth
    -> crea credencial con email + passwordActual
    -> currentUser.reauthenticate(credential)
    -> currentUser.updatePassword(nuevaPassword)
```

Por que pide password actual:

Cambiar contrasena es una accion sensible. Firebase no permite hacerlo solo porque la pantalla esta abierta; necesita comprobar nuevamente que el usuario conoce la password actual.

## 9. Como recupera contrasena si la olvido

Archivo:

```text
LoginActivity.java
```

Flujo:

```text
enviarRecuperacionPassword()
    -> lee email
    -> valida que no este vacio
    -> mAuth.sendPasswordResetEmail(email)
    -> Firebase envia correo con enlace seguro
```

La app no ve la nueva contrasena. Firebase gestiona el enlace y el cambio.

## 10. Como se cierra sesion

Archivo:

```text
MainActivity.java
```

Flujo:

```text
cerrarSesion()
    -> borra SharedPreferences
    -> FirebaseAuth.signOut()
    -> GoogleSignIn.signOut()
    -> vuelve a LoginActivity
```

Cerrar sesion debe limpiar:

- cache local
- sesion Firebase
- sesion Google por compatibilidad

## 11. Orden recomendado para estudiar en codigo

1. `Usuario.java`
2. `LoginActivity.onCreate`
3. `LoginActivity.intentarLoginInterno`
4. `LoginActivity.cargarPerfilUsuario`
5. `LoginActivity.enviarRecuperacionPassword`
6. `MainActivity.obtenerRolUsuario`
7. `MainActivity.cerrarSesion`
8. `AdminPanelActivity.mostrarDialogoUsuario`
9. `AdminPanelActivity.crearTrabajadorAuth`
10. `AdminPanelActivity.mostrarOpcionesTrabajador`
11. `PerfilActivity.cargarPerfil`
12. `PerfilActivity.guardarCambios`
13. `PerfilActivity.actualizarPasswordSiCorresponde`

