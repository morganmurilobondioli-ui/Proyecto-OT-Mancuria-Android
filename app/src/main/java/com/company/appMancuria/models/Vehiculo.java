package com.company.appMancuria.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

@IgnoreExtraProperties
public class Vehiculo {
    @DocumentId
    private String firestoreId;
    
    private String placa = "";
    private String marca = "";
    private String modelo = "";
    
    @PropertyName("anio")
    private int anio = 0;
    
    private String color = "";
    private String vin = "";
    
    @PropertyName("clientId")
    private String clienteId = "";
    
    private int ultimoKilometraje = 0;

    public Vehiculo() {}

    public Vehiculo(String placa, String marca, String modelo, int anio, String color, String vin, String clienteId) {
        this.placa     = placa != null ? placa.toUpperCase().trim() : "";
        this.marca     = marca != null ? marca.trim() : "";
        this.modelo    = modelo != null ? modelo.trim() : "";
        this.anio      = anio;
        this.color     = color != null ? color.trim() : "";
        this.vin       = vin != null ? vin.trim() : "";
        this.clienteId = clienteId != null ? clienteId : "";
        this.ultimoKilometraje = 0;
    }

    @Exclude
    public String getDescripcionCorta() {
        if (placa == null || placa.isEmpty()) return "Sin placa";
        StringBuilder sb = new StringBuilder(placa);
        
        boolean hasMarca = (marca != null && !marca.trim().isEmpty() && !marca.equalsIgnoreCase("Marca"));
        boolean hasModelo = (modelo != null && !modelo.trim().isEmpty() && !modelo.equalsIgnoreCase("Modelo"));
        
        if (hasMarca || hasModelo) {
            sb.append(" — ");
            if (hasMarca) sb.append(marca);
            if (hasMarca && hasModelo) sb.append(" ");
            if (hasModelo) sb.append(modelo);
        }
        if (anio > 0) {
            sb.append(" (").append(anio).append(")");
        }
        return sb.toString().trim();
    }

    @Exclude
    public String getFirestoreId() { return firestoreId; }
    @Exclude
    public void setFirestoreId(String id) { this.firestoreId = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String p) { this.placa = p; }

    public String getMarca() { return marca; }
    public void setMarca(String m) { this.marca = m; }

    public String getModelo() { return modelo; }
    public void setModelo(String m) { this.modelo = m; }

    @PropertyName("anio")
    public int getAnio() { return anio; }
    @PropertyName("anio")
    public void setAnio(int anio) { this.anio = anio; }

    public String getColor() { return color; }
    public void setColor(String c) { this.color = c; }

    public String getVin() { return vin; }
    public void setVin(String v) { this.vin = v; }

    @PropertyName("clientId")
    public String getClienteId() { return clienteId; }
    
    @PropertyName("clientId")
    public void setClienteId(String c) { this.clienteId = c; }

    public int getUltimoKilometraje() { return ultimoKilometraje; }
    public void setUltimoKilometraje(int k) { this.ultimoKilometraje = k; }
}
