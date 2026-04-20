package com.company.appMancuria.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

@IgnoreExtraProperties
public class Cliente {
    @DocumentId
    private String firestoreId; // ID real de Firestore
    
    private String documento = ""; 
    private String nombre = "";
    private String tipo = "Persona";   
    private String telefono = "";
    private String correo = "";
    private long fechaRegistro = 0;
    private int cantidadVehiculos = 0; 

    public Cliente() {}

    public Cliente(String documento, String nombre, String tipo, String telefono, String correo) {
        this.documento      = documento != null ? documento : "";
        this.nombre         = nombre    != null ? nombre    : "";
        this.tipo           = tipo      != null ? tipo      : "Persona";
        this.telefono       = telefono  != null ? telefono  : "";
        this.correo         = correo    != null ? correo    : "";
        this.fechaRegistro  = System.currentTimeMillis();
    }

    // Getter para el ID que usa el ID de Firestore
    public String getId() { return firestoreId; }
    public void setId(String id) { this.firestoreId = id; }

    public String getDocumento() { return documento; }
    public void setDocumento(String d) { this.documento = d; }

    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }

    public String getTipo() { return tipo; }
    public void setTipo(String t) { this.tipo = t; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String t) { this.telefono = t; }

    public String getCorreo() { return correo; }
    public void setCorreo(String c) { this.correo = c; }

    public long getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(long f) { this.fechaRegistro = f; }

    public int getCantidadVehiculos() { return cantidadVehiculos; }
    public void setCantidadVehiculos(int n) { this.cantidadVehiculos = n; }
}
