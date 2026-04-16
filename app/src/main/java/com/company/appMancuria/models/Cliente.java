package com.company.appMancuria.models;

public class Cliente {
    private String id = "";
    private String documento = ""; // DNI (8) o RUC (11)
    private String nombre = "";
    private String tipo = "Persona";   // "Persona" o "Empresa"
    private String telefono = "";
    private String correo = "";
    private long fechaRegistro = System.currentTimeMillis();
    private int cantidadVehiculos = 0; // denormalizado para mostrar en lista

    public Cliente() {}

    public Cliente(String documento, String nombre, String tipo, String telefono, String correo) {
        this.documento      = documento != null ? documento : "";
        this.nombre         = nombre    != null ? nombre    : "";
        this.tipo           = tipo      != null ? tipo      : "Persona";
        this.telefono       = telefono  != null ? telefono  : "";
        this.correo         = correo    != null ? correo    : "";
        this.fechaRegistro  = System.currentTimeMillis();
        this.cantidadVehiculos = 0;
    }

    public String getId()                          { return id != null ? id : ""; }
    public void   setId(String id)                 { this.id = id; }
    public String getDocumento()                   { return documento != null ? documento : ""; }
    public void   setDocumento(String d)           { this.documento = d; }
    public String getNombre()                      { return nombre != null ? nombre : ""; }
    public void   setNombre(String n)              { this.nombre = n; }
    public String getTipo()                        { return tipo != null ? tipo : "Persona"; }
    public void   setTipo(String t)                { this.tipo = t; }
    public String getTelefono()                    { return telefono != null ? telefono : ""; }
    public void   setTelefono(String t)            { this.telefono = t; }
    public String getCorreo()                      { return correo != null ? correo : ""; }
    public void   setCorreo(String c)              { this.correo = c; }
    public long   getFechaRegistro()               { return fechaRegistro; }
    public void   setFechaRegistro(long f)         { this.fechaRegistro = f; }
    public int    getCantidadVehiculos()           { return cantidadVehiculos; }
    public void   setCantidadVehiculos(int n)      { this.cantidadVehiculos = n; }
}
