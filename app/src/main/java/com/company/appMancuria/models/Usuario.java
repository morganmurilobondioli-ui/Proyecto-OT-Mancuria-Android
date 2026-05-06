package com.company.appMancuria.models;

public class Usuario {
    private String id;
    private String nombre;
    private String correo; // Ahora actuará como login o correo de referencia
    private String usuario; // Nuevo: Nombre de usuario único para login
    private String password; // Nuevo: Contraseña
    private String fotoUrl;
    private String rol;
    private String estado; // "activo" o "suspendido"

    public Usuario() {}

    public Usuario(String id, String nombre, String correo, String usuario, String password, String fotoUrl, String rol, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.usuario = usuario;
        this.password = password;
        this.fotoUrl = fotoUrl;
        this.rol = rol;
        this.estado = estado;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
