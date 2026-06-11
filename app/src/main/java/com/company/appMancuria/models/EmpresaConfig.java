package com.company.appMancuria.models;

public class EmpresaConfig {
    private String nombreComercial = "Mancuria Automotriz";
    private String razonSocial = "Mancuria Automotriz";
    private String ruc = "10437061161";
    private String direccion = "Av. Jose Olaya 420, Chincha Alta, Ica";
    private String telefono = "946359204";
    private String correo = "mancuriaautomotriz@gmail.com";
    private String rubro = "Electricidad, electronica, mecatronica automotriz, mantenimientos preventivos y correctivos";
    private String notaPdf = "Documento interno generado para identificar la orden de trabajo del vehiculo.";

    public EmpresaConfig() {}

    public String getNombreComercial() { return valor(nombreComercial, "Mancuria Automotriz"); }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }

    public String getRazonSocial() { return valor(razonSocial, getNombreComercial()); }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getRuc() { return valor(ruc, "10437061161"); }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getDireccion() { return valor(direccion, "Av. Jose Olaya 420, Chincha Alta, Ica"); }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return valor(telefono, "946359204"); }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return valor(correo, "mancuriaautomotriz@gmail.com"); }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getRubro() { return valor(rubro, "Electricidad, electronica, mecatronica automotriz, mantenimientos preventivos y correctivos"); }
    public void setRubro(String rubro) { this.rubro = rubro; }

    public String getNotaPdf() { return valor(notaPdf, "Documento interno generado para identificar la orden de trabajo del vehiculo."); }
    public void setNotaPdf(String notaPdf) { this.notaPdf = notaPdf; }

    private String valor(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }
}
