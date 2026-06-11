# Mancuria Gear - App Android para gestion de ordenes de trabajo

Mancuria Gear es una aplicacion Android nativa desarrollada para el Taller Automotriz Mancuria, ubicado en Chincha, Peru. Su finalidad es digitalizar el flujo operativo del taller mediante el registro de clientes, vehiculos, servicios y ordenes de trabajo (OT) en una base de datos centralizada con Firebase Firestore.

El proyecto forma parte de una propuesta de innovacion SENATI orientada a reducir la dependencia de coordinaciones verbales, mensajes de WhatsApp y registros no estructurados. La app permite que el personal autorizado consulte y actualice el estado de los servicios desde dispositivos moviles, manteniendo trazabilidad desde el ingreso del vehiculo hasta su entrega.

## Objetivo del proyecto

Desarrollar un sistema digital que permita al Taller Mancuria centralizar la informacion operativa de sus clientes, vehiculos y ordenes de trabajo, mejorando el seguimiento de servicios, la transparencia interna y la evidencia ante reclamos o consultas posteriores.

## Alcance de este repositorio

Este repositorio contiene la aplicacion Android del sistema Mancuria. La vision completa del proyecto documentado tambien contempla un portal web publico de consulta por placa, un panel administrativo web y reportes operativos; esos componentes no forman parte principal de este workspace Android.

## Funcionalidades implementadas

### Autenticacion y roles

- Inicio de sesion interno con usuarios registrados en Firestore.
- Persistencia de sesion local mediante `SharedPreferences`.
- Roles diferenciados: `admin` y `mecanico`.
- Bloqueo de acceso para usuarios suspendidos.
- Panel de administracion visible solo para usuarios administradores.

### Gestion de clientes

- Registro de clientes tipo persona o empresa.
- Captura de documento, nombre, telefono y correo.
- Validacion basica de longitud para DNI y RUC.
- Busqueda dinamica por nombre o documento.
- Asociacion de uno o varios vehiculos por cliente.

### Gestion de vehiculos

- Registro de placa, marca, modelo, anio, color y VIN.
- Formateo de placa en el flujo de registro.
- Control de ultimo kilometraje registrado por vehiculo.
- Actualizacion del contador de vehiculos por cliente.

### Gestion de ordenes de trabajo

- Creacion guiada de nuevas OTs.
- Seleccion de cliente y vehiculo desde Firestore.
- Seleccion de servicios desde un catalogo administrable.
- Registro de kilometraje, monto y fecha de ingreso.
- Validacion para evitar kilometrajes menores al ultimo registrado.
- Listado de ordenes en tiempo real.
- Busqueda de OTs por placa o cliente.

### Seguimiento de estados e historial

- Estados operativos: `Pendiente`, `En Proceso`, `Finalizado` y `Entregado`.
- Actualizacion del estado de la OT desde la pantalla de detalle.
- Edicion de servicio, trabajo realizado, kilometraje y monto antes de la entrega.
- Historial de cambios con fecha, usuario y accion realizada.
- Bloqueo de edicion cuando la orden queda marcada como entregada.

### Panel administrativo

- Gestion de trabajadores internos.
- Creacion, edicion, suspension, reactivacion y eliminacion de usuarios.
- Gestion del catalogo de servicios.
- Creacion, edicion, eliminacion y busqueda de servicios.
- Carga inicial de servicios predeterminados si el catalogo esta vacio.

## Modelo de datos principal en Firestore

- `usuarios`: datos del personal, credenciales internas, rol y estado.
- `clientes`: informacion de personas o empresas atendidas por el taller.
- `clientes/{clienteId}/vehiculos`: vehiculos asociados a cada cliente.
- `servicios`: catalogo de trabajos o servicios disponibles para OTs.
- `ordenes_trabajo`: ordenes creadas, estado, placa, cliente, kilometraje, monto e historial.

## Stack tecnologico

- Android Studio
- Java
- Gradle Kotlin DSL
- Firebase Firestore
- Firebase Authentication
- Material Components
- AppCompat
- RecyclerView
- ConstraintLayout
- Glide

## Arquitectura actual

La app esta organizada como una aplicacion Android nativa basada en Activities, modelos Java, adaptadores RecyclerView y conexion directa con Firestore.

Actividades principales:

- `LoginActivity`: autenticacion interna.
- `MainActivity`: listado y busqueda de ordenes de trabajo.
- `ClientesActivity`: administracion de clientes y vehiculos.
- `NuevaOrdenActivity`: creacion de OTs.
- `DetalleOrdenActivity`: seguimiento, edicion e historial de OTs.
- `AdminPanelActivity`: gestion de trabajadores y catalogo de servicios.

Modelos principales:

- `Cliente`
- `Vehiculo`
- `OrdenTrabajo`
- `Servicio`
- `Usuario`

## Problema que resuelve

Antes del sistema, la informacion operativa del taller dependia principalmente de conversaciones, memoria del personal y coordinacion informal. Esto generaba perdida de informacion, poca trazabilidad, interrupciones constantes entre mecanicos y encargado, dificultad para responder reclamos y ausencia de historial formal por vehiculo.

Mancuria Gear convierte cada atencion en una orden digital consultable, con estado, historial, kilometraje, monto y datos del cliente/vehiculo almacenados en Firestore.

## Estado del proyecto

El repositorio representa un prototipo funcional de la aplicacion Android. Para una version productiva completa se recomienda reforzar las reglas de seguridad de Firestore, mejorar el manejo de credenciales, consolidar Firebase Authentication, implementar respaldos periodicos y conectar de forma segura el portal publico de consulta mediante una capa backend.
