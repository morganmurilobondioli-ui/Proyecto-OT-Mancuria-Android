package com.company.appMancuria.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class OrdenTrabajo {
    private String id = "";
    private String clienteId = "";
    private String clienteNombre = "";
    private String vehiculoId = "";
    private String placa = "";
    private String marcaModelo = "";
    private String creadoPorId = "";
    private String creadoPorNombre = "";
    @Exclude
    private List<String> fallasReportadas = new ArrayList<>();
    
    private String trabajoRealizado = "";
    private String estado = "Pendiente";
    private double montoTotal = 0.0;
    private long   fechaIngreso = System.currentTimeMillis();
    private int    kilometraje = 0;
    private List<LogEntrada> historial = new ArrayList<>();

    public OrdenTrabajo() {}

    public OrdenTrabajo(String clienteId, String clienteNombre, String vehiculoId,
                        String placa, String marcaModelo, String fallaReportada,
                        double montoTotal, int kilometraje) {
        this(clienteId, clienteNombre, vehiculoId, placa, marcaModelo,
                normalizarFallas(fallaReportada), montoTotal, kilometraje);
    }

    public OrdenTrabajo(String clienteId, String clienteNombre, String vehiculoId,
                        String placa, String marcaModelo, List<String> fallasReportadas,
                        double montoTotal, int kilometraje) {
        this.clienteId      = clienteId      != null ? clienteId      : "";
        this.clienteNombre  = clienteNombre  != null ? clienteNombre  : "";
        this.vehiculoId     = vehiculoId     != null ? vehiculoId     : "";
        this.placa          = placa          != null ? placa          : "";
        this.marcaModelo    = marcaModelo    != null ? marcaModelo    : "";
        this.fallasReportadas = normalizarFallas(fallasReportadas);
        this.estado         = "Pendiente";
        this.montoTotal     = montoTotal;
        this.fechaIngreso   = System.currentTimeMillis();
        this.kilometraje    = kilometraje;
    }

    public static class LogEntrada {
        public String fecha;
        public String usuario;
        public String accion;

        public LogEntrada() {}
        public LogEntrada(String fecha, String usuario, String accion) {
            this.fecha = fecha;
            this.usuario = usuario;
            this.accion = accion;
        }
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getClienteId() { return clienteId != null ? clienteId : ""; }
    public void setClienteId(String c) { this.clienteId = c; }

    public String getClienteNombre() { return clienteNombre != null ? clienteNombre : ""; }
    public void setClienteNombre(String c) { this.clienteNombre = c; }

    public String getVehiculoId() { return vehiculoId != null ? vehiculoId : ""; }
    public void setVehiculoId(String v) { this.vehiculoId = v; }

    public String getPlaca() { return placa != null ? placa : ""; }
    public void setPlaca(String p) { this.placa = p; }

    public String getMarcaModelo() { return marcaModelo != null ? marcaModelo : ""; }
    public void setMarcaModelo(String m) { this.marcaModelo = m; }

    public String getCreadoPorId() { return creadoPorId != null ? creadoPorId : ""; }
    public void setCreadoPorId(String id) { this.creadoPorId = id; }

    public String getCreadoPorNombre() { return creadoPorNombre != null ? creadoPorNombre : ""; }
    public void setCreadoPorNombre(String nombre) { this.creadoPorNombre = nombre; }

    @Exclude
    public String getFallaReportada() {
        return String.join("\n", getFallasReportadas());
    }

    @Exclude
    public void setFallaReportada(String f) {
        this.fallasReportadas = normalizarFallas(f);
    }

    @Exclude
    public List<String> getFallasReportadas() {
        return fallasReportadas != null ? fallasReportadas : new ArrayList<>();
    }

    @Exclude
    public void setFallasReportadas(List<String> fallas) {
        this.fallasReportadas = normalizarFallas(fallas);
    }

    @PropertyName("fallareportada")
    public List<String> getFallasReportadasFirestore() {
        return getFallasReportadas();
    }

    @PropertyName("fallareportada")
    public void setFallasReportadasFirestore(Object value) {
        this.fallasReportadas = normalizarFallas(value);
    }

    public String getTrabajoRealizado() { return trabajoRealizado != null ? trabajoRealizado : ""; }
    public void setTrabajoRealizado(String t) { this.trabajoRealizado = t; }

    public String getEstado() { return estado != null ? estado : "Pendiente"; }
    public void setEstado(String e) { this.estado = e; }

    public double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(double m) { this.montoTotal = m; }

    public long getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(long f) { this.fechaIngreso = f; }

    public int getKilometraje() { return kilometraje; }
    public void setKilometraje(int k) { this.kilometraje = k; }

    public List<LogEntrada> getHistorial() { return historial; }
    public void setHistorial(List<LogEntrada> h) { this.historial = h; }

    private static List<String> normalizarFallas(Object value) {
        List<String> resultado = new ArrayList<>();
        if (value == null) return resultado;

        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                agregarFallaNormalizada(resultado, item != null ? item.toString() : "");
            }
            return resultado;
        }

        String texto = value.toString();
        if (texto.contains("\n")) {
            for (String item : texto.split("\\r?\\n")) {
                agregarFallaNormalizada(resultado, item);
            }
        } else {
            agregarFallaNormalizada(resultado, texto);
        }
        return resultado;
    }

    private static void agregarFallaNormalizada(List<String> destino, String falla) {
        String limpia = falla != null ? falla.trim() : "";
        if (!limpia.isEmpty() && !destino.contains(limpia)) {
            destino.add(limpia);
        }
    }
}
