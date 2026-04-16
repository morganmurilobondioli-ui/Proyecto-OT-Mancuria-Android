package com.company.appMancuria.models;

public class Vehiculo {
    private String id = "";
    private String placa = "";
    private String marca = "";
    private String modelo = "";
    private int    anio = 0;
    private String color = "";
    private String vin = ""; // Número de chasis (17 caracteres)
    private String clienteId = "";
    private int ultimoKilometraje = 0;

    public Vehiculo() {}

    public Vehiculo(String placa, String marca, String modelo, int anio, String color, String vin, String clienteId) {
        this.placa     = placa     != null ? placa     : "";
        this.marca     = marca     != null ? marca     : "";
        this.modelo    = modelo    != null ? modelo    : "";
        this.anio      = anio;
        this.color     = color     != null ? color     : "";
        this.vin       = vin       != null ? vin       : "";
        this.clienteId = clienteId != null ? clienteId : "";
        this.ultimoKilometraje = 0;
    }

    /** Descripción corta para mostrar en dropdowns: "ABC-123 — Toyota Corolla 2020" */
    public String getDescripcionCorta() {
        String info = placa;
        if (!marca.isEmpty() || !modelo.isEmpty()) {
            info += " — " + marca + " " + modelo;
            if (anio > 0) info += " " + anio;
        }
        return info.trim();
    }

    public String getId()               { return id != null ? id : ""; }
    public void   setId(String id)      { this.id = id; }
    public String getPlaca()            { return placa != null ? placa : ""; }
    public void   setPlaca(String p)    { this.placa = p; }
    public String getMarca()            { return marca != null ? marca : ""; }
    public void   setMarca(String m)    { this.marca = m; }
    public String getModelo()           { return modelo != null ? modelo : ""; }
    public void   setModelo(String m)   { this.modelo = m; }
    public int    getAnio()             { return anio; }
    public void   setAnio(int a)        { this.anio = a; }
    public String getColor()            { return color != null ? color : ""; }
    public void   setColor(String c)    { this.color = c; }
    public String getVin()              { return vin != null ? vin : ""; }
    public void   setVin(String v)      { this.vin = v; }
    public String getClienteId()        { return clienteId != null ? clienteId : ""; }
    public void   setClienteId(String c){ this.clienteId = c; }
    public int    getUltimoKilometraje() { return ultimoKilometraje; }
    public void   setUltimoKilometraje(int k) { this.ultimoKilometraje = k; }
}
