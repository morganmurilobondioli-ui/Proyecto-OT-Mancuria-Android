package com.company.appMancuria;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.company.appMancuria.models.Cliente;
import com.company.appMancuria.models.OrdenTrabajo;
import com.company.appMancuria.models.Vehiculo;
import com.company.appMancuria.utils.Validaciones;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NuevaOrdenActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    // Datos seleccionados
    private String selectedClienteId   = null;
    private String selectedClienteNombre = null;
    private String selectedVehiculoId  = null;
    private String selectedPlaca       = null;
    private String selectedMarcaModelo = null;
    private int ultimoKilometrajeVehiculo = 0;

    // Maps para resolver ID desde texto mostrado
    private final Map<String, String> clienteNombreToId = new HashMap<>();
    private final Map<String, String> vehiculoDescToId  = new HashMap<>();
    private final Map<String, String> vehiculoDescToPlaca = new HashMap<>();
    private final Map<String, String> vehiculoDescToMarca = new HashMap<>();
    private final Map<String, Integer> vehiculoDescToKm = new HashMap<>();

    // UI
    private AutoCompleteTextView actvCliente, actvVehiculo;
    private TextInputLayout layoutVehiculo;
    private MaterialButton btnNuevoVehiculo, btnGuardar;
    private TextInputEditText etFalla, etKilometraje, etMonto, etFechaIngreso;
    private TextView tvNumVehiculo, tvNumDetalles;
    private View cardVehiculo, cardDetalles;

    private static final SimpleDateFormat SDF_DISPLAY =
            new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_orden);

        // Aplicar insets para Safe Areas (evitar choque con status bar y notch)
        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bindViews();
        setFechaActual();
        cargarClientes();
        setupListeners();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void bindViews() {
        actvCliente     = findViewById(R.id.actvCliente);
        actvVehiculo    = findViewById(R.id.actvVehiculo);
        layoutVehiculo  = findViewById(R.id.layoutVehiculo);
        btnNuevoVehiculo= findViewById(R.id.btnNuevoVehiculo);
        btnGuardar      = findViewById(R.id.btnGuardar);
        etFalla         = findViewById(R.id.etFalla);
        etKilometraje   = findViewById(R.id.etKilometraje);
        etMonto         = findViewById(R.id.etMonto);
        etFechaIngreso  = findViewById(R.id.etFechaIngreso);
        tvNumVehiculo   = findViewById(R.id.tvNumVehiculo);
        tvNumDetalles   = findViewById(R.id.tvNumDetalles);
        cardVehiculo    = findViewById(R.id.cardVehiculo);
        cardDetalles    = findViewById(R.id.cardDetalles);
    }

    private void setFechaActual() {
        etFechaIngreso.setText(SDF_DISPLAY.format(new Date()));
    }

    // ── CARGAR CLIENTES EN EL DROPDOWN (Ordenados por los más recientes primero) ──
    private void cargarClientes() {
        db.collection("clientes")
                .orderBy("fechaRegistro", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<String> nombres = new ArrayList<>();
                    clienteNombreToId.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        String nombre = doc.getString("nombre");
                        String docId = doc.getString("documento");
                        if (nombre != null && !nombre.isEmpty()) {
                            String item = nombre + (docId != null ? " (" + docId + ")" : "");
                            nombres.add(item);
                            clienteNombreToId.put(item, doc.getId());
                        }
                    }
                    ArrayAdapter<String> adp = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line, nombres);
                    actvCliente.setAdapter(adp);
                });
    }

    // ── CARGAR VEHÍCULOS DEL CLIENTE SELECCIONADO ───────────
    private void cargarVehiculos(String clienteId) {
        db.collection("clientes").document(clienteId)
                .collection("vehiculos").get()
                .addOnSuccessListener(snap -> {
                    List<String> descs = new ArrayList<>();
                    vehiculoDescToId.clear();
                    vehiculoDescToPlaca.clear();
                    vehiculoDescToMarca.clear();
                    vehiculoDescToKm.clear();

                    for (QueryDocumentSnapshot doc : snap) {
                        String placa  = doc.getString("placa");
                        String marca  = doc.getString("marca");
                        String modelo = doc.getString("modelo");
                        Long   anioL  = doc.getLong("anio");
                        int    anio   = anioL != null ? anioL.intValue() : 0;
                        Long   kmL    = doc.getLong("ultimoKilometraje");
                        int    km     = kmL != null ? kmL.intValue() : 0;

                        if (placa == null || placa.isEmpty()) continue;

                        String desc = placa;
                        String marcaModelo = (marca != null ? marca : "") + " "
                                + (modelo != null ? modelo : "");
                        if (!marcaModelo.trim().isEmpty()) {
                            desc += " — " + marcaModelo.trim();
                            if (anio > 0) desc += " " + anio;
                        }

                        descs.add(desc);
                        vehiculoDescToId.put(desc, doc.getId());
                        vehiculoDescToPlaca.put(desc, placa);
                        vehiculoDescToMarca.put(desc, marcaModelo.trim()
                                + (anio > 0 ? " " + anio : ""));
                        vehiculoDescToKm.put(desc, km);
                    }

                    activarSeccion(2);

                    ArrayAdapter<String> adp = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line, descs);
                    actvVehiculo.setAdapter(adp);
                    actvVehiculo.setEnabled(true);
                    btnNuevoVehiculo.setEnabled(true);
                    btnNuevoVehiculo.setTextColor(getColor(android.R.color.holo_red_dark));
                    btnNuevoVehiculo.setStrokeColor(
                            ColorStateList.valueOf(Color.parseColor("#db2d2c")));

                    // Mostrar dropdown automáticamente si hay vehículos
                    if (!descs.isEmpty()) {
                        actvVehiculo.showDropDown();
                    } else {
                        Toast.makeText(this,
                                "Este cliente no tiene vehículos. Agrega uno.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupListeners() {
        // Mostrar todos los clientes al hacer click en el buscador
        actvCliente.setOnClickListener(v -> actvCliente.showDropDown());
        actvCliente.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) actvCliente.showDropDown();
        });

        // Mostrar vehículos al hacer click
        actvVehiculo.setOnClickListener(v -> {
            if (actvVehiculo.isEnabled()) actvVehiculo.showDropDown();
        });
        actvVehiculo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && actvVehiculo.isEnabled()) actvVehiculo.showDropDown();
        });

        // Cliente seleccionado
        actvCliente.setOnItemClickListener((parent, v, pos, id) -> {
            String item = (String) parent.getItemAtPosition(pos);
            selectedClienteId     = clienteNombreToId.get(item);
            selectedClienteNombre = item.split(" \\(")[0];
            // Resetear vehículo
            actvVehiculo.setText("");
            selectedVehiculoId  = null;
            selectedPlaca       = null;
            selectedMarcaModelo = null;
            desactivarSeccion(3);
            cargarVehiculos(selectedClienteId);
        });

        // Vehículo seleccionado
        actvVehiculo.setOnItemClickListener((parent, v, pos, id) -> {
            String desc = (String) parent.getItemAtPosition(pos);
            selectedVehiculoId  = vehiculoDescToId.get(desc);
            selectedPlaca       = vehiculoDescToPlaca.get(desc);
            selectedMarcaModelo = vehiculoDescToMarca.get(desc);
            ultimoKilometrajeVehiculo = vehiculoDescToKm.getOrDefault(desc, 0);
            
            etKilometraje.setHint("Anterior: " + ultimoKilometrajeVehiculo + " km");
            activarSeccion(3);
        });

        // Botón nuevo cliente
        findViewById(R.id.btnNuevoCliente).setOnClickListener(v ->
                mostrarDialogoNuevoCliente());

        // Botón nuevo vehículo
        btnNuevoVehiculo.setOnClickListener(v -> {
            if (selectedClienteId != null) mostrarDialogoNuevoVehiculo();
        });

        // Guardar orden
        btnGuardar.setOnClickListener(v -> guardarOrden());
    }

    private void activarSeccion(int num) {
        if (num == 2) {
            tvNumVehiculo.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#db2d2c")));
            ((android.widget.LinearLayout) tvNumVehiculo.getParent())
                    .getChildAt(1).setEnabled(true);
        }
        if (num == 3) {
            tvNumDetalles.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#db2d2c")));
            etFalla.setEnabled(true);
            etKilometraje.setEnabled(true);
            etMonto.setEnabled(true);
            btnGuardar.setEnabled(true);
        }
    }

    private void desactivarSeccion(int num) {
        if (num == 3) {
            tvNumDetalles.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
            etFalla.setEnabled(false);
            etKilometraje.setEnabled(false);
            etMonto.setEnabled(false);
            btnGuardar.setEnabled(false);
        }
    }

    private void guardarOrden() {
        if (selectedClienteId == null) {
            Toast.makeText(this, "Selecciona un cliente.", Toast.LENGTH_SHORT).show(); return;
        }
        if (selectedVehiculoId == null) {
            Toast.makeText(this, "Selecciona un vehículo.", Toast.LENGTH_SHORT).show(); return;
        }

        String falla = etFalla.getText().toString().trim();
        if (falla.isEmpty()) {
            Toast.makeText(this, "Describe la falla reportada.", Toast.LENGTH_SHORT).show(); return;
        }

        String kmStr    = etKilometraje.getText().toString().trim();
        String montoStr = etMonto.getText().toString().trim();
        int    km    = kmStr.isEmpty()    ? 0    : Integer.parseInt(kmStr.replaceAll(",", ""));
        double monto = montoStr.isEmpty() ? 0.0  : Double.parseDouble(montoStr);

        // Validación de kilometraje
        if (km < ultimoKilometrajeVehiculo) {
            Toast.makeText(this, "El kilometraje no puede ser menor al anterior (" + ultimoKilometrajeVehiculo + " km)", Toast.LENGTH_LONG).show();
            return;
        }

        // Validación de duplicidad (Placa en estado Pendiente o En Proceso)
        db.collection("ordenes_trabajo")
            .whereEqualTo("placa", selectedPlaca)
            .get()
            .addOnSuccessListener(snap -> {
                boolean existeAbierta = false;
                for (QueryDocumentSnapshot doc : snap) {
                    String estado = doc.getString("estado");
                    if ("Pendiente".equals(estado) || "En Proceso".equals(estado)) {
                        existeAbierta = true;
                        break;
                    }
                }

                if (existeAbierta) {
                    new AlertDialog.Builder(this)
                        .setTitle("Orden duplicada")
                        .setMessage("Ya existe una orden activa para la placa " + selectedPlaca + ". No se puede crear una nueva.")
                        .setPositiveButton("Entendido", null)
                        .show();
                } else {
                    procederGuardarOrden(falla, monto, km);
                }
            });
    }

    private void procederGuardarOrden(String falla, double monto, int km) {
        OrdenTrabajo orden = new OrdenTrabajo(
                selectedClienteId, selectedClienteNombre,
                selectedVehiculoId, selectedPlaca,
                selectedMarcaModelo != null ? selectedMarcaModelo : "",
                falla, monto, km);

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        db.collection("ordenes_trabajo").add(orden)
                .addOnSuccessListener(ref -> {
                    // Actualizar último kilometraje del vehículo
                    db.collection("clientes").document(selectedClienteId)
                        .collection("vehiculos").document(selectedVehiculoId)
                        .update("ultimoKilometraje", km);

                    Toast.makeText(this, "✅ Orden guardada correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("GUARDAR ORDEN");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDialogoNuevoCliente() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_cliente, null);
        TextInputEditText etNombre  = v.findViewById(R.id.etNombreCliente);
        TextInputEditText etDoc     = v.findViewById(R.id.etDocumentoCliente);
        TextInputEditText etTel     = v.findViewById(R.id.etTelefonoCliente);
        TextInputEditText etCorreo  = v.findViewById(R.id.etCorreoCliente);
        RadioButton rbEmpresa       = v.findViewById(R.id.rbEmpresa);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Registrar nuevo cliente")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String nombre = etNombre.getText().toString().trim();
                String doc    = etDoc.getText().toString().trim();
                String tel    = etTel.getText().toString().trim();
                String correo = etCorreo.getText().toString().trim();
                String tipo   = rbEmpresa.isChecked() ? "Empresa" : "Persona";

                if (nombre.isEmpty()) { etNombre.setError("Obligatorio"); return; }
                
                if (tipo.equals("Persona")) {
                    if (!Validaciones.esDniValido(doc)) { etDoc.setError("DNI inválido (8 dígitos)"); return; }
                } else {
                    if (!Validaciones.esRucValido(doc)) { etDoc.setError("RUC inválido (11 dígitos)"); return; }
                    if (!doc.startsWith("20")) { etDoc.setError("RUC de empresa debe iniciar con 20"); return; }
                }

                if (!Validaciones.esTelefonoValido(tel)) { etTel.setError("Teléfono inválido (9 dígitos, inicia con 9)"); return; }

                Cliente nuevo = new Cliente(doc, nombre, tipo, tel, correo);
                db.collection("clientes").add(nuevo)
                        .addOnSuccessListener(ref -> {
                            Toast.makeText(this, "Cliente registrado ✓", Toast.LENGTH_SHORT).show();
                            cargarClientes();
                            dialog.dismiss();
                        });
            });
        });
        dialog.show();
    }

    private void mostrarDialogoNuevoVehiculo() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_vehiculo, null);
        TextInputEditText etPlaca  = v.findViewById(R.id.etPlacaVehiculo);
        TextInputEditText etMarca  = v.findViewById(R.id.etMarcaVehiculo);
        TextInputEditText etModelo = v.findViewById(R.id.etModeloVehiculo);
        TextInputEditText etAnio   = v.findViewById(R.id.etAnioVehiculo);
        TextInputEditText etColor  = v.findViewById(R.id.etColorVehiculo);
        TextInputEditText etVin    = v.findViewById(R.id.etVinVehiculo);

        // Automatización de entrada de placa
        etPlaca.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;
                String str = s.toString().toUpperCase().replaceAll("[^A-Z0-9]", "");
                if (str.length() > 3) {
                    str = str.substring(0, 3) + "-" + str.substring(3, Math.min(str.length(), 6));
                }
                etPlaca.setText(str);
                etPlaca.setSelection(str.length());
                isEditing = false;
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Agregar vehículo")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String placa  = etPlaca.getText().toString().trim().toUpperCase();
                String marca  = etMarca.getText().toString().trim();
                String modelo = etModelo.getText().toString().trim();
                String anioS  = etAnio.getText().toString().trim();
                String color  = etColor.getText().toString().trim();
                String vin    = etVin.getText().toString().trim().toUpperCase();

                if (!Validaciones.esPlacaValida(placa)) { etPlaca.setError("Formato: XXX-123"); return; }
                
                int anio = 0;
                try { anio = Integer.parseInt(anioS); } catch (NumberFormatException e) {}
                if (!Validaciones.esAnioValido(anio)) { etAnio.setError("Año inválido (1920 - actual+1)"); return; }
                
                if (!vin.isEmpty() && !Validaciones.esVinValido(vin)) {
                    etVin.setError("VIN inválido (17 caracteres, sin I, O, Q)");
                    return;
                }

                Vehiculo vehiculo = new Vehiculo(placa, marca, modelo, anio, color, vin, selectedClienteId);

                db.collection("clientes").document(selectedClienteId)
                        .collection("vehiculos").add(vehiculo)
                        .addOnSuccessListener(ref -> {
                            db.collection("clientes").document(selectedClienteId)
                                    .update("cantidadVehiculos", com.google.firebase.firestore.FieldValue.increment(1));
                            Toast.makeText(this, "Vehículo agregado ✓", Toast.LENGTH_SHORT).show();
                            cargarVehiculos(selectedClienteId);
                            dialog.dismiss();
                        });
            });
        });
        dialog.show();
    }
}
