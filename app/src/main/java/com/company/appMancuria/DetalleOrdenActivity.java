package com.company.appMancuria;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.company.appMancuria.models.OrdenTrabajo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetalleOrdenActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private OrdenTrabajo orden;
    private String ordenId;

    private TextView tvPlaca, tvCliente, tvEstado, tvFecha, tvHistorial;
    private TextInputEditText etServicio, etTrabajo, etKm, etMonto;
    private MaterialButton btnSiguiente, btnActualizar;

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

        etServicio = findViewById(R.id.etDetalleServicio);
        etTrabajo = findViewById(R.id.etDetalleTrabajo);
        etKm = findViewById(R.id.etDetalleKm);
        etMonto = findViewById(R.id.etDetalleMonto);

        btnSiguiente = findViewById(R.id.btnSiguienteEstado);
        btnActualizar = findViewById(R.id.btnActualizarDatos);

        btnActualizar.setOnClickListener(v -> actualizarDatos());
        btnSiguiente.setOnClickListener(v -> gestionarCambioEstado());
        
        btnActualizar.setEnabled(false);
    }

    private void setupChangeDetection() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                verificarCambios();
            }
        };
        etServicio.addTextChangedListener(watcher);
        etTrabajo.addTextChangedListener(watcher);
        etKm.addTextChangedListener(watcher);
        etMonto.addTextChangedListener(watcher);
    }

    private void verificarCambios() {
        if (orden == null) return;

        String currentServicio = etServicio.getText().toString().trim();
        String currentTrabajo = etTrabajo.getText().toString().trim();
        String currentKmStr = etKm.getText().toString().trim();
        String currentMontoStr = etMonto.getText().toString().trim();

        int currentKm = 0;
        try { currentKm = Integer.parseInt(currentKmStr); } catch (Exception ignored) {}
        
        double currentMonto = 0.0;
        try { currentMonto = Double.parseDouble(currentMontoStr); } catch (Exception ignored) {}

        boolean hayCambios = !currentServicio.equals(orden.getFallaReportada()) ||
                             !currentTrabajo.equals(orden.getTrabajoRealizado()) ||
                             currentKm != orden.getKilometraje() ||
                             currentMonto != orden.getMontoTotal();

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
        tvPlaca.setText(orden.getPlaca());
        tvCliente.setText(orden.getClienteNombre());
        tvEstado.setText(orden.getEstado().toUpperCase());
        tvFecha.setText(SDF.format(new Date(orden.getFechaIngreso())));

        if (!etServicio.hasFocus()) etServicio.setText(orden.getFallaReportada());
        if (!etTrabajo.hasFocus()) etTrabajo.setText(orden.getTrabajoRealizado());
        if (!etKm.hasFocus()) etKm.setText(String.valueOf(orden.getKilometraje()));
        if (!etMonto.hasFocus()) etMonto.setText(String.valueOf(orden.getMontoTotal()));

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
                if (orden.getMontoTotal() <= 0) {
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
        double monto = Double.parseDouble(montoStr);

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
