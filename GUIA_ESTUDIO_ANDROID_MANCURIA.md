# Guia de estudio Android - Proyecto Mancuria

Esta guia esta pensada para estudiar el proyecto como programador Android: primero entiendes el flujo general, luego las pantallas, despues los modelos, adaptadores, Firebase y finalmente los conceptos base de Java que aparecen en el codigo.

## 1. Que es este proyecto

Mancuria Gear es una aplicacion Android nativa hecha en Java para digitalizar las ordenes de trabajo de un taller automotriz.

El flujo real del negocio es:

1. Un trabajador inicia sesion.
2. La app valida si existe su perfil interno y si esta activo.
3. El trabajador ve las ordenes de trabajo.
4. Puede registrar clientes.
5. Puede registrar vehiculos por cliente.
6. Puede crear una orden de trabajo.
7. Puede actualizar estado, servicios, kilometraje, monto, piezas usadas y observaciones.
8. Puede generar un PDF de la orden.
9. Si el usuario es admin, puede administrar trabajadores, servicios y datos de empresa para el PDF.

## 2. Tecnologias usadas

- Android nativo: la app corre como aplicacion Android real.
- Java: lenguaje principal del proyecto.
- Gradle Kotlin DSL: sistema de configuracion y dependencias.
- Firebase Auth: autenticacion con correo y password.
- Firebase Firestore: base de datos principal.
- SharedPreferences: almacenamiento local pequeno para recordar la sesion.
- RecyclerView: listas eficientes de ordenes, clientes, servicios y trabajadores.
- Material Components: componentes visuales como botones, TextInputLayout, Chips y cards.
- Glide: carga imagenes, por ejemplo la foto de perfil.
- PdfDocument: genera PDFs desde codigo Android.

Archivo clave:

```text
app/build.gradle.kts
```

Ahi se declaran dependencias como Firebase, AppCompat, Material, RecyclerView indirectamente y Glide.

## 3. Como arranca la app

Android no arranca por cualquier archivo Java. Arranca por la Activity marcada como launcher en:

```text
app/src/main/AndroidManifest.xml
```

La entrada principal es:

```xml
<activity
    android:name=".models.LoginActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Eso significa:

- `LoginActivity` es la primera pantalla.
- `MAIN` dice: esta Activity puede iniciar la app.
- `LAUNCHER` dice: aparece en el lanzador del telefono.
- Las demas Activities tienen `exported="false"`, por lo que son pantallas internas.

## 4. Mapa de carpetas

```text
app/src/main/java/com/company/appMancuria/
```

Contiene las pantallas principales:

- `MainActivity.java`: listado de ordenes y navegacion principal.
- `ClientesActivity.java`: gestion de clientes y vehiculos.
- `NuevaOrdenActivity.java`: creacion de ordenes de trabajo.
- `DetalleOrdenActivity.java`: detalle, edicion, estados, piezas y PDF.
- `AdminPanelActivity.java`: trabajadores, servicios y configuracion de empresa.
- `PerfilActivity.java`: perfil del usuario.

Modelos:

```text
app/src/main/java/com/company/appMancuria/models/
```

- `Cliente.java`
- `Vehiculo.java`
- `OrdenTrabajo.java`
- `Servicio.java`
- `Usuario.java`
- `EmpresaConfig.java`
- `LoginActivity.java`

Adaptadores:

```text
app/src/main/java/com/company/appMancuria/adapters/
```

Los adaptadores conectan listas Java con layouts XML para RecyclerView.

Utilidades:

```text
app/src/main/java/com/company/appMancuria/utils/
```

Incluye validaciones, codigos y generacion de PDF.

Layouts:

```text
app/src/main/res/layout/
```

Cada XML define la interfaz visual de una pantalla, item o dialogo.

## 5. Conceptos Java basicos que aparecen en el proyecto

### Clase

Una clase es un molde. Define datos y comportamientos.

Ejemplo:

```java
public class Cliente {
    private String nombre = "";
}
```

`Cliente` es el molde. Cada cliente concreto en memoria es un objeto.

### Objeto

Un objeto es una instancia real de una clase.

```java
Cliente nuevoCliente = new Cliente(doc, nombre, tipo, tel, correo);
```

Aqui `nuevoCliente` es un objeto creado con `new`.

### Atributo o campo

Un atributo guarda estado dentro del objeto.

```java
private String nombre = "";
private int cantidadVehiculos = 0;
```

`private` significa que otros archivos no deberian modificarlo directamente.

### Constructor

Un constructor crea un objeto y le asigna valores iniciales.

```java
public Cliente() {}
```

Este constructor vacio es muy importante para Firestore. Firestore necesita poder crear el objeto sin parametros y luego llenar sus campos.

```java
public Cliente(String documento, String nombre, String tipo, String telefono, String correo) {
    this.documento = documento != null ? documento : "";
    this.nombre = nombre != null ? nombre : "";
}
```

Este constructor es para crear un cliente desde tu codigo. `this.documento` significa "el atributo documento de este objeto".

### Getter

Un getter devuelve un valor.

```java
public String getNombre() {
    return nombre;
}
```

Sirve para leer el campo sin tocarlo directamente. Firestore tambien usa getters para convertir objetos a documentos.

### Setter

Un setter cambia un valor.

```java
public void setNombre(String nombre) {
    this.nombre = nombre;
}
```

Sirve para escribir el campo controladamente. Firestore tambien usa setters cuando lee documentos.

### `void`

`void` significa que el metodo no devuelve nada.

```java
private void setupRecyclerView() {
}
```

Ese metodo hace una operacion, pero no retorna un valor.

### `return`

`return` termina un metodo y opcionalmente devuelve algo.

```java
if (currentUserId == null) {
    irALogin();
    return;
}
```

Aqui evita que la pantalla siga cargando si no hay usuario valido.

### `private`, `public`, `protected`

- `public`: accesible desde otras clases.
- `private`: solo accesible dentro de la misma clase.
- `protected`: accesible por clases hijas o del mismo paquete.

En Android es normal que muchos metodos internos sean `private` porque solo esa pantalla los usa.

### `static`

Un metodo o clase `static` pertenece a la clase, no a un objeto especifico.

```java
public static boolean esDniValido(String dni)
```

Puedes llamarlo como:

```java
Validaciones.esDniValido("12345678");
```

sin crear `new Validaciones()`.

### List

Una `List` guarda muchos elementos ordenados.

```java
private List<OrdenTrabajo> listaCompleta = new ArrayList<>();
```

Aqui se guardan varias ordenes de trabajo.

### Map

Un `Map` relaciona una clave con un valor.

```java
private final Map<String, String> clienteNombreToId = new HashMap<>();
```

Ejemplo mental:

```text
"Juan Perez (12345678)" -> "idFirestore123"
```

Sirve para mostrar texto amigable al usuario, pero guardar el ID real.

### Listener

Un listener es una funcion que espera un evento.

Ejemplos:

- Click en boton.
- Cambio de texto.
- Respuesta de Firebase.
- Cambio en una coleccion Firestore.

```java
btnGuardar.setOnClickListener(v -> guardarOrden());
```

Cuando el usuario toca el boton, se ejecuta `guardarOrden()`.

### Lambda

Esto:

```java
v -> guardarOrden()
```

es una forma corta de escribir una funcion anonima. `v` representa la vista clickeada.

## 6. Firebase y modelo de datos

La app usa estas colecciones principales:

```text
usuarios
clientes
clientes/{clienteId}/vehiculos
servicios
ordenes_trabajo
configuracion/empresa
```

### `usuarios`

Guarda perfil interno:

- `nombre`
- `correo`
- `usuario`
- `rol`: `admin` o `mecanico`
- `estado`: `activo` o `suspendido`

Firebase Auth confirma credenciales, pero Firestore confirma permisos internos.

### `clientes`

Guarda:

- documento
- nombre
- tipo
- telefono
- correo
- cantidadVehiculos

### `clientes/{clienteId}/vehiculos`

Subcoleccion de vehiculos de un cliente.

Guarda:

- placa
- marca
- modelo
- anio
- color
- vin
- ultimoKilometraje

### `servicios`

Catalogo de servicios seleccionables:

- mantenimiento menor
- reparacion de alternador
- direccion
- suspension
- etc.

### `ordenes_trabajo`

Es el centro del sistema.

Guarda:

- clienteId
- clienteNombre
- vehiculoId
- placa
- marcaModelo
- fallareportada
- trabajoRealizado
- estado
- montoManoObra
- montoTotal
- piezasUsadas
- historial
- kilometraje
- fechaIngreso
- creadoPorId
- creadoPorNombre

## 7. Flujo completo de pantallas

```text
LoginActivity
    |
    v
MainActivity
    |----> NuevaOrdenActivity
    |----> ClientesActivity
    |----> DetalleOrdenActivity
    |----> PerfilActivity
    |
    v
AdminPanelActivity solo si userRol == "admin"
```

## 8. LoginActivity: autenticacion

Archivo:

```text
app/src/main/java/com/company/appMancuria/models/LoginActivity.java
```

Responsabilidad:

- Iniciar Firebase Auth.
- Revisar si ya existe sesion local.
- Mostrar formulario de login.
- Validar correo y password.
- Buscar perfil en Firestore.
- Bloquear usuarios suspendidos.
- Guardar datos basicos en SharedPreferences.
- Abrir `MainActivity`.

Flujo:

```text
onCreate()
    -> FirebaseAuth.getInstance()
    -> FirebaseFirestore.getInstance()
    -> revisar SharedPreferences
    -> si hay sesion valida: irAlMain()
    -> si no: mostrar layout de login
```

Metodo clave:

```java
private void intentarLoginInterno()
```

Hace:

1. Lee email y password.
2. Si estan vacios, muestra Toast.
3. Deshabilita el boton para evitar doble click.
4. Llama a `signInWithEmailAndPassword`.
5. Si funciona, carga perfil Firestore.
6. Si falla, reactiva boton y muestra error.

Despues:

```java
private void cargarPerfilUsuario()
```

Este metodo busca:

```text
usuarios/{uidFirebaseAuth}
```

Si no existe, cierra sesion. Si existe y esta suspendido, tambien limpia sesion. Si esta activo, guarda:

```text
userId
userNombre
userRol
```

en `MancuriaPrefs`.

## 9. MainActivity: pantalla principal

Archivo:

```text
app/src/main/java/com/company/appMancuria/MainActivity.java
```

Responsabilidad:

- Validar que todavia exista sesion.
- Mostrar nombre/foto del usuario.
- Consultar rol.
- Listar ordenes en tiempo real.
- Filtrar ordenes por placa o cliente.
- Navegar a nueva orden, clientes, detalle, perfil o admin.

Metodo clave:

```java
private void consultarOrdenes()
```

Consulta:

```text
ordenes_trabajo orderBy fechaIngreso desc
```

Usa:

```java
addSnapshotListener
```

Eso significa que Firestore avisa en tiempo real cuando cambia la coleccion. No hace falta refrescar manualmente.

Cada documento se convierte a:

```java
OrdenTrabajo o = doc.toObject(OrdenTrabajo.class);
```

Luego:

```java
o.setId(doc.getId());
```

porque el ID del documento no siempre vive dentro de los campos del documento.

## 10. NuevaOrdenActivity: crear OT

Archivo:

```text
app/src/main/java/com/company/appMancuria/NuevaOrdenActivity.java
```

Responsabilidad:

- Cargar clientes.
- Cargar vehiculos del cliente seleccionado.
- Cargar catalogo de servicios.
- Permitir crear cliente y vehiculo sin salir de la pantalla.
- Validar kilometraje.
- Guardar la orden.
- Actualizar ultimo kilometraje del vehiculo.

Flujo:

```text
onCreate()
    -> bindViews()
    -> setFechaActual()
    -> escucharClientes()
    -> cargarCatalogoServicios()
    -> setupListeners()
```

Operacion clave:

```java
private void guardarOrden()
```

Hace:

1. Valida que haya al menos un servicio.
2. Lee kilometraje.
3. Evita que el kilometraje sea menor al ultimo registrado.
4. Lee monto.
5. Construye un objeto `OrdenTrabajo`.
6. Agrega datos del usuario creador.
7. Guarda en `ordenes_trabajo`.
8. Actualiza `ultimoKilometraje` en el vehiculo.
9. Cierra la pantalla.

Esto es una operacion de negocio: no solo guarda texto, tambien mantiene consistencia del kilometraje.

## 11. DetalleOrdenActivity: seguimiento de OT

Archivo:

```text
app/src/main/java/com/company/appMancuria/DetalleOrdenActivity.java
```

Responsabilidad:

- Leer una orden por ID.
- Mostrar placa, cliente, estado, fecha, servicios, trabajo, kilometraje y montos.
- Editar servicios y observaciones.
- Agregar piezas usadas.
- Calcular total.
- Avanzar estado.
- Registrar historial.
- Generar PDF.

Estados:

```text
Pendiente -> En Proceso -> Finalizado -> Entregado
```

Metodo clave:

```java
private void gestionarCambioEstado()
```

Decide el proximo estado. Tiene reglas:

- Si esta `Pendiente`, pasa a `En Proceso`.
- Si esta `En Proceso`, exige al menos un servicio y pasa a `Finalizado`.
- Si esta `Finalizado`, exige monto mayor a cero y pide confirmacion antes de `Entregado`.

Metodo clave:

```java
private void cambiarEstado(String nuevoEstado)
```

Hace:

1. Lee datos editados.
2. Crea entrada de historial.
3. Prepara un `Map<String, Object>` con campos a actualizar.
4. Actualiza el documento Firestore.

Metodo clave:

```java
private void actualizarDatos()
```

Guarda cambios sin necesariamente avanzar estado. Tambien crea bitacora con diferencias.

Cuando la orden queda `Entregado`, la pantalla bloquea edicion:

```java
boolean esEditable = !"Entregado".equals(orden.getEstado());
```

## 12. ClientesActivity: clientes y vehiculos

Archivo:

```text
app/src/main/java/com/company/appMancuria/ClientesActivity.java
```

Responsabilidad:

- Listar clientes.
- Filtrar por nombre o documento.
- Crear/editar cliente.
- Ver vehiculos del cliente.
- Agregar vehiculo.
- Actualizar contador de vehiculos.

Metodo clave:

```java
private void consultarClientes()
```

Escucha la coleccion `clientes` en tiempo real.

Metodo clave:

```java
private void mostrarDialogoNuevoCliente(Cliente clienteExistente)
```

Si recibe `null`, crea cliente. Si recibe un cliente, edita.

Esta tecnica se usa mucho: un mismo dialogo sirve para "nuevo" y "editar".

## 13. AdminPanelActivity: administracion

Archivo:

```text
app/src/main/java/com/company/appMancuria/AdminPanelActivity.java
```

Responsabilidad:

- Administrar datos de empresa para PDF.
- Crear, editar, suspender, reactivar y eliminar trabajadores.
- Crear, editar y eliminar servicios.
- Inicializar servicios predeterminados si no existen.

Operacion importante:

```java
private void crearTrabajadorAuth(...)
```

Crea usuario en Firebase Auth y luego crea perfil en Firestore.

Usa una app secundaria de Firebase:

```java
FirebaseAuth secondaryAuth = getSecondaryAuth();
```

Esto evita que al crear un trabajador nuevo se cierre la sesion del administrador actual. Es una solucion importante y profesional.

## 14. OrdenTrabajo: el modelo mas importante

Archivo:

```text
app/src/main/java/com/company/appMancuria/models/OrdenTrabajo.java
```

Este modelo representa una OT.

Tiene:

- campos simples: `placa`, `estado`, `kilometraje`
- listas: `fallasReportadas`, `piezasUsadas`, `historial`
- constructores
- getters y setters
- clases internas: `LogEntrada` y `PiezaUsada`
- normalizacion de fallas
- calculo de subtotales y total

### Por que hay constructor vacio

```java
public OrdenTrabajo() {}
```

Firestore lo necesita para crear el objeto al leer documentos.

### Por que hay dos constructores con parametros

Uno recibe una falla como texto:

```java
String fallaReportada
```

Otro recibe una lista:

```java
List<String> fallasReportadas
```

Esto permite compatibilidad con datos antiguos y datos nuevos.

### Que hace `normalizarFallas`

Convierte distintos formatos a una lista limpia:

- Si viene una lista, limpia cada item.
- Si viene un texto con saltos de linea, lo separa.
- Si viene un texto simple, lo convierte en lista de un elemento.
- Evita vacios.
- Evita duplicados.

Esta parte es clave porque Firestore puede tener datos viejos como string y datos nuevos como arreglo.

### Que hace `@Exclude`

Le dice a Firestore:

```text
No guardes este getter/setter como campo directo.
```

Ejemplo:

```java
@Exclude
public double getSubtotal()
```

El subtotal se calcula, no se guarda.

### Que hace `@PropertyName("fallareportada")`

Hace que el campo Java:

```java
fallasReportadas
```

se lea/escriba en Firestore como:

```text
fallareportada
```

Esto mantiene compatibilidad con el nombre usado en la base de datos.

## 15. Adaptadores RecyclerView

Un RecyclerView necesita un Adapter.

Responsabilidad del Adapter:

1. Inflar el layout de cada item.
2. Recibir una lista de objetos.
3. Poner los datos del objeto en los TextView.
4. Manejar click sobre cada item.

Ejemplo:

```text
OrdenAdapter
```

Toma una lista de `OrdenTrabajo` y la muestra usando:

```text
res/layout/item_orden.xml
```

Metodo clave:

```java
onBindViewHolder
```

Este metodo se ejecuta por cada item visible. Ahi se asignan textos, colores y clicks.

## 16. Diferencia entre Activity, Model, Adapter y Utility

### Activity

Pantalla con UI y eventos.

Ejemplo:

```text
MainActivity
```

### Model

Clase que representa datos.

Ejemplo:

```text
OrdenTrabajo
```

### Adapter

Puente entre lista de datos y RecyclerView.

Ejemplo:

```text
OrdenAdapter
```

### Utility

Clase con funciones reutilizables.

Ejemplo:

```text
Validaciones
OtPdfGenerator
```

## 17. Operaciones asincronas

Firebase no responde de inmediato. Por eso se usan listeners:

```java
.addOnSuccessListener(...)
.addOnFailureListener(...)
.addOnCompleteListener(...)
.addSnapshotListener(...)
```

La app no se queda congelada esperando. Firebase trabaja en segundo plano y cuando termina llama al listener.

Ejemplo mental:

```text
Usuario toca Guardar
    -> app envia datos a Firebase
    -> Firebase responde despues
    -> addOnSuccessListener muestra Toast y cierra pantalla
```

## 18. SharedPreferences

`SharedPreferences` guarda datos pequenos localmente.

En este proyecto guarda:

```text
userId
userNombre
userRol
userFotoUrl
```

No es una base de datos grande. Es como una libreta pequena de configuracion.

## 19. Que deberias estudiar primero para volverte pro

Orden recomendado:

1. Java basico: clases, objetos, constructores, getters, setters, listas, maps.
2. Ciclo de vida Android: `onCreate`, `onResume`, intents.
3. XML layouts: como cada ID conecta con `findViewById`.
4. RecyclerView y Adapter.
5. Firebase Auth.
6. Firestore: documentos, colecciones, subcolecciones, listeners.
7. Manejo de estado en UI.
8. Validaciones y consistencia de datos.
9. Separacion de responsabilidades: Activity, Repository, ViewModel.
10. Seguridad: reglas Firestore, roles, permisos y backend.

## 20. Mejoras tecnicas futuras

El proyecto funciona como prototipo, pero para subir de nivel profesional conviene:

- Separar Firestore en clases Repository.
- Evitar que las Activities tengan tanta logica.
- Usar ViewModel y LiveData/StateFlow.
- Migrar gradualmente a Kotlin.
- Usar reglas de seguridad Firestore estrictas.
- No guardar roles solo en cliente; validarlos tambien en reglas/backend.
- Mejorar validaciones de placa, DNI, RUC, telefono y montos.
- Manejar errores Firebase de forma visible y consistente.
- Agregar pruebas unitarias para modelos y validaciones.
- Agregar transacciones o batch writes cuando una operacion actualiza varios documentos.

## 21. Lectura guiada para comprender cada linea

Para estudiar este proyecto de forma seria, hazlo en este orden:

1. Abre `AndroidManifest.xml` y confirma que `LoginActivity` es la pantalla inicial.
2. Abre `activity_login.xml` y busca los IDs usados por `LoginActivity`.
3. Lee `LoginActivity.onCreate`.
4. Lee `intentarLoginInterno`.
5. Lee `cargarPerfilUsuario`.
6. Abre `MainActivity` y lee `onCreate`.
7. Sigue los metodos que llama: `setupTopBar`, `setupRecyclerView`, `setupBuscador`, `obtenerRolUsuario`.
8. Lee `consultarOrdenes` y luego `OrdenAdapter`.
9. Lee `NuevaOrdenActivity.guardarOrden`.
10. Lee `OrdenTrabajo`.
11. Lee `DetalleOrdenActivity.gestionarCambioEstado`.
12. Lee `DetalleOrdenActivity.actualizarDatos`.
13. Lee `AdminPanelActivity.crearTrabajadorAuth`.
14. Lee `OtPdfGenerator.generate`.

Si entiendes esos puntos, entiendes la columna vertebral de la app.

