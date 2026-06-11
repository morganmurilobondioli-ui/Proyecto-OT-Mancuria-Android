package com.company.appMancuria;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.company.appMancuria.models.EmpresaConfig;
import com.company.appMancuria.models.OrdenTrabajo;
import com.company.appMancuria.utils.OtPdfGenerator;
import com.company.appMancuria.utils.OtPdfGenerator.PdfResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetalleOrdenActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private OrdenTrabajo orden;
    private EmpresaConfig empresaConfig = new EmpresaConfig();
    private String ordenId;
    private final List<OrdenTrabajo.PiezaUsada> piezasUsadas = new ArrayList<>();
    private boolean cargandoDatos = false;

    private TextView tvPlaca, tvCliente, tvEstado, tvFecha, tvHistorial, tvTotalPiezas, tvTotalFinal;
    private TextInputEditText etServicio, etTrabajo, etKm, etMonto;
    private LinearLayout llPiezasUsadas;
    private MaterialButton btnSiguiente, btnActualizar, btnAgregarPieza, btnDescargarPdf;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_orden);

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        ordenId = getIntent().getStringExtra("ordenId");

        if (ordenId == null) {
            finish();
            return;
        }

        setupToolbar();
        bindViews();
        setupChangeDetection();
        cargarConfigEmpresa();
        cargarDatosOrden();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarDetalle);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvPlaca = findViewById(R.id.tvDetallePlaca);
        tvCliente = findViewById(R.id.tvDetalleCliente);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvFecha = findViewById(R.id.tvDetalleFecha);
        tvHistorial = findViewById(R.id.tvHistorial);
        tvTotalPiezas = findViewById(R.id.tvTotalPiezas);
        tvTotalFinal = findViewById(R.id.tvTotalFinal);
        llPiezasUsadas = findViewById(R.id.llPiezasUsadas);

        etServicio = findViewById(R.id.etDetalleServicio);
        etTrabajo = findViewById(R.id.etDetalleTrabajo);
        etKm = findViewById(R.id.etDetalleKm);
        etMonto = findViewById(R.id.etDetalleMonto);

        btnSiguiente = findViewById(R.id.btnSiguienteEstado);
        btnActualizar = findViewById(R.id.btnActualizarDatos);
        btnAgregarPieza = findViewById(R.id.btnAgregarPieza);
        btnDescargarPdf = findViewById(R.id.btnDescargarPdf);

        btnActualizar.setOnClickListener(v -> actualizarDatos());
        btnSiguiente.setOnClickListener(v -> gestionarCambioEstado());
        btnAgregarPieza.setOnClickListener(v -> mostrarDialogoPieza(null, -1));
        btnDescargarPdf.setOnClickListener(v -> generarPdfOrden());
        
        btnActualizar.setEnabled(false);
    }

    private void setupChangeDetection() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                actualizarTotales();
                verificarCambios();
            }
        };
        etServicio.addTextChangedListener(watcher);
        etTrabajo.addTextChangedListener(watcher);
        etKm.addTextChangedListener(watcher);
        etMonto.addTextChangedListener(watcher);
    }

    private void verificarCambios() {
        if (orden == null || cargandoDatos) return;

        String currentServicio = etServicio.getText().toString().trim();
        String currentTrabajo = etTrabajo.getText().toString().trim();
        String currentKmStr = etKm.getText().toString().trim();
        String currentMontoStr = etMonto.getText().toString().trim();

        int currentKm = 0;
        try { currentKm = Integer.parseInt(currentKmStr); } catch (Exception ignored) {}
        
        double currentMonto = parseMonto(currentMontoStr);

        boolean hayCambios = !currentServicio.equals(orden.getFallaReportada()) ||
                             !currentTrabajo.equals(orden.getTrabajoRealizado()) ||
                             currentKm != orden.getKilometraje() ||
                             Math.abs(currentMonto - orden.getMontoManoObra()) > 0.009 ||
                             !piezasIguales(piezasUsadas, orden.getPiezasUsadas());

        btnActualizar.setEnabled(hayCambios && !"Entregado".equals(orden.getEstado()));
    }

    private void cargarDatosOrden() {
        db.collection("ordenes_trabajo").document(ordenId).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            orden = snapshot.toObject(OrdenTrabajo.class);
            if (orden != null) {
                orden.setId(snapshot.getId());
                mostrarDatos();
                verificarCambios();
            }
        });
    }

    private void mostrarDatos() {
        cargandoDatos = true;
        tvPlaca.setText(orden.getPlaca());
        tvCliente.setText(orden.getClienteNombre());
        tvEstado.setText(orden.getEstado().toUpperCase());
        tvFecha.setText(SDF.format(new Date(orden.getFechaIngreso())));

        if (!etServicio.hasFocus()) etServicio.setText(orden.getFallaReportada());
        if (!etTrabajo.hasFocus()) etTrabajo.setText(orden.getTrabajoRealizado());
        if (!etKm.hasFocus()) etKm.setText(String.valueOf(orden.getKilometraje()));
        if (!etMonto.hasFocus()) etMonto.setText(formatMontoInput(orden.getMontoManoObra()));

        piezasUsadas.clear();
        piezasUsadas.addAll(copiarPiezas(orden.getPiezasUsadas()));
        renderPiezas();
        actualizarTotales();

        int color;
        switch (orden.getEstado()) {
            case "En Proceso": color = Color.parseColor("#F57C00"); break;
            case "Finalizado": color = Color.parseColor("#2E7D32"); break;
            case "Entregado":  color = Color.parseColor("#757575"); break;
            default:           color = Color.parseColor("#E53935"); break;
        }
        tvEstado.setBackgroundTintList(ColorStateList.valueOf(color));

        boolean esEditable = !"Entregado".equals(orden.getEstado());
        etServicio.setEnabled(esEditable);
        etTrabajo.setEnabled(esEditable);
        etKm.setEnabled(esEditable);
        etMonto.setEnabled(esEditable);
        btnAgregarPieza.setEnabled(esEditable);

        actualizarTextoBotonSiguiente();
        
        StringBuilder sb = new StringBuilder();
        if (orden.getHistorial() != null) {
            for (int i = orden.getHistorial().size() - 1; i >= 0; i--) {
                OrdenTrabajo.LogEntrada log = orden.getHistorial().get(i);
                sb.append("📅 ").append(log.fecha).append("\n")
                  .append("👤 ").append(log.usuario).append("\n")
                  .append("📝 ").append(log.accion).append("\n")
                  .append("────────────────────\n");
            }
        }
        tvHistorial.setText(sb.length() > 0 ? sb.toString() : "No hay registros de actividad...");
        cargandoDatos = false;
    }

    private void actualizarTextoBotonSiguiente() {
        switch (orden.getEstado()) {
            case "Pendiente":
                btnSiguiente.setText("INICIAR TRABAJO (En Proceso)");
                btnSiguiente.setVisibility(View.VISIBLE);
                break;
            case "En Proceso":
                btnSiguiente.setText("FINALIZAR TRABAJO");
                btnSiguiente.setVisibility(View.VISIBLE);
                break;
            case "Finalizado":
                btnSiguiente.setText("MARCAR COMO ENTREGADO");
                btnSiguiente.setVisibility(View.VISIBLE);
                break;
            default:
                btnSiguiente.setVisibility(View.GONE);
                break;
        }
    }

    private void gestionarCambioEstado() {
        String proximoEstado = "";
        switch (orden.getEstado()) {
            case "Pendiente": proximoEstado = "En Proceso"; break;
            case "En Proceso": 
                if (etServicio.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Debe ingresar el tipo de servicio", Toast.LENGTH_LONG).show();
                    return;
                }
                proximoEstado = "Finalizado"; 
                break;
            case "Finalizado": 
                if (calcularMontoTotalActual() <= 0) {
                    Toast.makeText(this, "El monto debe ser mayor a S/ 0.00 para entregar", Toast.LENGTH_LONG).show();
                    return;
                }
                mostrarConfirmacionEntrega();
                return;
        }
        
        if (!proximoEstado.isEmpty()) {
            cambiarEstado(proximoEstado);
        }
    }

    private void mostrarConfirmacionEntrega() {
        new AlertDialog.Builder(this)
            .setTitle("Confirmación de Conformidad")
            .setMessage("¿El cliente ha verificado el trabajo y está conforme con la entrega del vehículo?")
            .setPositiveButton("Sí, Entregar", (d, w) -> cambiarEstado("Entregado"))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void cambiarEstado(String nuevoEstado) {
        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        String userName = prefs.getString("userNombre", "Usuario");
        
        String trabajoActual = etTrabajo.getText().toString().trim();
        String servicioActual = etServicio.getText().toString().trim();
        
        OrdenTrabajo.LogEntrada log = new OrdenTrabajo.LogEntrada(
                SDF.format(new Date()),
                userName,
                "CAMBIO DE ESTADO: " + orden.getEstado() + " → " + nuevoEstado +
                (!servicioActual.isEmpty() ? "\nServicio: " + servicioActual : "") +
                (!trabajoActual.isEmpty() ? "\nDetalles: " + trabajoActual : "")
        );

        List<OrdenTrabajo.LogEntrada> historial = orden.getHistorial();
        if (historial == null) historial = new ArrayList<>();
        historial.add(log);

        db.collection("ordenes_trabajo").document(ordenId)
                .update("estado", nuevoEstado, "historial", historial)
                .addOnSuccessListener(v -> Toast.makeText(this, "Orden: " + nuevoEstado, Toast.LENGTH_SHORT).show());
    }

    private void actualizarDatos() {
        String servicio = etServicio.getText().toString().trim();
        String trabajo = etTrabajo.getText().toString().trim();
        String kmStr = etKm.getText().toString().trim();
        String montoStr = etMonto.getText().toString().trim();

        if (kmStr.isEmpty() || montoStr.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos técnicos", Toast.LENGTH_SHORT).show();
            return;
        }

        int km = Integer.parseInt(kmStr);
        double montoManoObra = parseMonto(montoStr);
        double totalPiezas = calcularTotalPiezas();
        double montoTotal = montoManoObra + totalPiezas;
        double monto = montoTotal;

        if (km < orden.getKilometraje()) {
            Toast.makeText(this, "El kilometraje no puede ser menor al registrado anteriormente (" + orden.getKilometraje() + " km)", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        String userName = prefs.getString("userNombre", "Usuario");
        
        StringBuilder bitacora = new StringBuilder("ACTUALIZACIÓN DE DATOS:");
        if (!servicio.equals(orden.getFallaReportada())) bitacora.append("\n- TIPO SERVICIO: ").append(servicio);
        if (!trabajo.equals(orden.getTrabajoRealizado())) bitacora.append("\n- DETALLES: ").append(trabajo);
        if (km != orden.getKilometraje()) bitacora.append("\n- KM: ").append(orden.getKilometraje()).append(" → ").append(km);
        if (monto != orden.getMontoTotal()) bitacora.append("\n- MONTO: S/ ").append(orden.getMontoTotal()).append(" → S/ ").append(monto);

        OrdenTrabajo.LogEntrada log = new OrdenTrabajo.LogEntrada(
                SDF.format(new Date()),
                userName,
                bitacora.toString()
        );

        List<OrdenTrabajo.LogEntrada> historial = orden.getHistorial();
        if (historial == null) historial = new ArrayList<>();
        historial.add(log);

        btnActualizar.setEnabled(false);
        
        db.collection("ordenes_trabajo").document(ordenId)
                .update("fallareportada", parseServiciosDesdeTexto(servicio),
                        "trabajoRealizado", trabajo, 
                        "kilometraje", km, 
                        "montoManoObra", montoManoObra,
                        "piezasUsadas", copiarPiezas(piezasUsadas),
                        "montoTotal", monto,
                        "historial", historial)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnActualizar.setEnabled(true);
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarConfigEmpresa() {
        db.collection("configuracion").document("empresa").addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) {
                empresaConfig = new EmpresaConfig();
                return;
            }
            EmpresaConfig config = snapshot.toObject(EmpresaConfig.class);
            empresaConfig = config != null ? config : new EmpresaConfig();
        });
    }

    private void mostrarDialogoPieza(OrdenTrabajo.PiezaUsada piezaExistente, int index) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pieza_usada, null);
        TextInputEditText etNombre = dialogView.findViewById(R.id.etPiezaNombre);
        TextInputEditText etCantidad = dialogView.findViewById(R.id.etPiezaCantidad);
        TextInputEditText etPrecio = dialogView.findViewById(R.id.etPiezaPrecio);

        if (piezaExistente != null) {
            etNombre.setText(piezaExistente.getNombre());
            etCantidad.setText(String.valueOf(piezaExistente.getCantidad()));
            etPrecio.setText(formatMontoInput(piezaExistente.getPrecioUnitario()));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(piezaExistente == null ? "Agregar pieza usada" : "Editar pieza usada")
                .setView(dialogView)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String cantidadStr = etCantidad.getText().toString().trim();
            String precioStr = etPrecio.getText().toString().trim();

            if (nombre.isEmpty()) { etNombre.setError("Obligatorio"); return; }
            if (cantidadStr.isEmpty()) { etCantidad.setError("Obligatorio"); return; }
            if (precioStr.isEmpty()) { etPrecio.setError("Obligatorio"); return; }

            int cantidad;
            try {
                cantidad = Math.max(1, Integer.parseInt(cantidadStr));
            } catch (Exception ex) {
                etCantidad.setError("Cantidad invalida");
                return;
            }

            OrdenTrabajo.PiezaUsada pieza = new OrdenTrabajo.PiezaUsada(nombre, cantidad, parseMonto(precioStr));
            if (index >= 0 && index < piezasUsadas.size()) {
                piezasUsadas.set(index, pieza);
            } else {
                piezasUsadas.add(pieza);
            }
            renderPiezas();
            actualizarTotales();
            verificarCambios();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void renderPiezas() {
        llPiezasUsadas.removeAllViews();
        boolean esEditable = orden == null || !"Entregado".equals(orden.getEstado());

        if (piezasUsadas.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Sin piezas registradas.");
            empty.setTextColor(Color.parseColor("#757575"));
            empty.setTextSize(13);
            empty.setPadding(0, 6, 0, 10);
            llPiezasUsadas.addView(empty);
            return;
        }

        for (int i = 0; i < piezasUsadas.size(); i++) {
            OrdenTrabajo.PiezaUsada pieza = piezasUsadas.get(i);
            int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView title = new TextView(this);
            title.setText(pieza.getNombre());
            title.setTextColor(Color.parseColor("#212121"));
            title.setTextSize(14);
            title.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView detail = new TextView(this);
            detail.setText(pieza.getCantidad() + " x S/ " + formatMontoInput(pieza.getPrecioUnitario()) +
                    " = S/ " + formatMontoInput(pieza.getSubtotal()));
            detail.setTextColor(Color.parseColor("#616161"));
            detail.setTextSize(12);

            texts.addView(title);
            texts.addView(detail);
            row.addView(texts);

            TextView edit = new TextView(this);
            edit.setText(esEditable ? "Editar" : "");
            edit.setTextColor(getColor(R.color.rojo_mancuria));
            edit.setTextSize(12);
            edit.setTypeface(null, android.graphics.Typeface.BOLD);
            edit.setPadding(14, 8, 6, 8);
            edit.setOnClickListener(v -> {
                if (esEditable) mostrarDialogoPieza(pieza, index);
            });
            row.addView(edit);

            TextView delete = new TextView(this);
            delete.setText(esEditable ? "Quitar" : "");
            delete.setTextColor(Color.parseColor("#757575"));
            delete.setTextSize(12);
            delete.setPadding(10, 8, 0, 8);
            delete.setOnClickListener(v -> {
                if (!esEditable) return;
                piezasUsadas.remove(index);
                renderPiezas();
                actualizarTotales();
                verificarCambios();
            });
            row.addView(delete);

            llPiezasUsadas.addView(row);
        }
    }

    private void actualizarTotales() {
        double totalPiezas = calcularTotalPiezas();
        double totalFinal = calcularMontoTotalActual();
        if (tvTotalPiezas != null) tvTotalPiezas.setText("Piezas: S/ " + formatMontoInput(totalPiezas));
        if (tvTotalFinal != null) tvTotalFinal.setText("Total final: S/ " + formatMontoInput(totalFinal));
    }

    private double calcularMontoTotalActual() {
        return parseMonto(etMonto.getText().toString().trim()) + calcularTotalPiezas();
    }

    private double calcularTotalPiezas() {
        double total = 0.0;
        for (OrdenTrabajo.PiezaUsada pieza : piezasUsadas) {
            if (pieza != null) total += pieza.getSubtotal();
        }
        return total;
    }

    private List<OrdenTrabajo.PiezaUsada> copiarPiezas(List<OrdenTrabajo.PiezaUsada> source) {
        List<OrdenTrabajo.PiezaUsada> copia = new ArrayList<>();
        if (source == null) return copia;
        for (OrdenTrabajo.PiezaUsada pieza : source) {
            if (pieza != null) {
                copia.add(new OrdenTrabajo.PiezaUsada(pieza.getNombre(), pieza.getCantidad(), pieza.getPrecioUnitario()));
            }
        }
        return copia;
    }

    private boolean piezasIguales(List<OrdenTrabajo.PiezaUsada> a, List<OrdenTrabajo.PiezaUsada> b) {
        List<OrdenTrabajo.PiezaUsada> left = a != null ? a : new ArrayList<>();
        List<OrdenTrabajo.PiezaUsada> right = b != null ? b : new ArrayList<>();
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            OrdenTrabajo.PiezaUsada p1 = left.get(i);
            OrdenTrabajo.PiezaUsada p2 = right.get(i);
            if (p1 == null || p2 == null) return p1 == p2;
            if (!p1.getNombre().equals(p2.getNombre())) return false;
            if (p1.getCantidad() != p2.getCantidad()) return false;
            if (Math.abs(p1.getPrecioUnitario() - p2.getPrecioUnitario()) > 0.009) return false;
        }
        return true;
    }

    private double parseMonto(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Math.max(0.0, Double.parseDouble(value.trim().replace(",", ".")));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private String formatMontoInput(double value) {
        return String.format(Locale.US, "%.2f", Math.max(0.0, value));
    }

    private void generarPdfOrden() {
        if (orden == null) return;
        if (btnActualizar.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Guarda los cambios")
                    .setMessage("Hay datos sin guardar. Guarda la OT antes de descargar el PDF para que salga con la informacion correcta.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        btnDescargarPdf.setEnabled(false);
        try {
            PdfResult result = OtPdfGenerator.generate(this, orden, empresaConfig);
            mostrarPdfGenerado(result);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo generar el PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            btnDescargarPdf.setEnabled(true);
        }
    }

    private void mostrarPdfGenerado(PdfResult result) {
        new AlertDialog.Builder(this)
                .setTitle("PDF descargado")
                .setMessage("Archivo: " + result.getFileName() + "\nUbicacion: " + result.getDisplayPath())
                .setPositiveButton("Abrir PDF", (dialog, which) -> abrirPdf(result.getUri()))
                .setNegativeButton("Compartir", (dialog, which) -> compartirPdf(result))
                .setNeutralButton("Cerrar", null)
                .show();
    }

    private void abrirPdf(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Abrir PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "No hay una app disponible para abrir PDF", Toast.LENGTH_LONG).show();
        }
    }

    private void compartirPdf(PdfResult result) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, result.getUri());
        intent.putExtra(Intent.EXTRA_SUBJECT, result.getFileName());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Compartir PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "No hay una app disponible para compartir PDF", Toast.LENGTH_LONG).show();
        }
    }

    private List<String> parseServiciosDesdeTexto(String texto) {
        List<String> servicios = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty()) return servicios;

        for (String item : texto.split("\\r?\\n")) {
            String servicio = item.trim();
            if (!servicio.isEmpty() && !servicios.contains(servicio)) {
                servicios.add(servicio);
            }
        }
        return servicios;
    }
}
