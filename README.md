# Mancuria OT - Mobile App (Android) 🛠️📱
Mancuria OT es la extensión móvil del Sistema Inteligente de Gestión de Activos y Automatización de Suministros (SIGA-IA). Esta aplicación ha sido diseñada específicamente para los técnicos y administradores de Mancuria Taller Automotriz Especializado en Chincha, permitiendo la gestión eficiente de órdenes de trabajo (OT) y el seguimiento de inventario directamente desde el taller.

## 🎯 Objetivo del Proyecto
Digitalizar el flujo de trabajo operativo del taller, eliminando el uso de papel y permitiendo que la información capturada por el motor de IA del sistema (parsing de facturas y documentos) esté disponible instantáneamente para los técnicos en sus dispositivos móviles.

## 🚀 Funcionalidades Avanzadas
1. Gestión de Órdenes de Trabajo (OT)
  Registro Digital: Apertura de nuevas OTs con captura de datos del cliente y del vehículo.
  Seguimiento de Estado: Actualización en tiempo real del progreso del servicio (Diagnóstico, Reparación, Listo).
  Validación de Identidad: Implementación de lógica de validación para DNI, RUC y formatos de Placas Peruanas (XXX-123).

2. Integración con IA (SIGA-IA)
  Suministros Automatizados: Consulta de repuestos registrados mediante el motor de IA que procesa facturas PDF en la versión de escritorio.
  Control de Activos: Registro fotográfico de piezas y daños vinculados directamente a la OT mediante Firebase Storage.

4. Roles y Seguridad
  Acceso Multinivel: Diferenciación de permisos para Administradores (vistas de costos y reportes) y Técnicos (vistas de tareas y reparaciones).
  Sincronización Offline: Capacidad de trabajar en el taller con sincronización automática al detectar conexión mediante Firestore Online/Offline persistence.

## 🛠️ Stack Tecnológico
- Lenguaje: Java / Kotlin (Android Studio).
- Arquitectura: Model-View-ViewModel (MVVM) para una separación clara de la lógica y la interfaz.
- Backend: * Firebase Firestore: Base de datos NoSQL en tiempo real.
- Firebase Auth: Gestión segura de usuarios del taller.
- Firebase Storage: Almacenamiento de evidencias fotográficas de los vehículos.
- UI/UX: Componentes de Material Design 3 para una interfaz moderna y funcional en entornos industriales.
