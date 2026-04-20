package com.company.appMancuria;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.google.firebase.firestore.ListenerRegistration;
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

    private static final String TAG = "NuevaOrden";
    private FirebaseFirestore db;
    private ListenerRegistration clientesListener;

    private String selectedClienteId   = null;
    private String selectedClienteNombre = null;
    private String selectedVehiculoId  = null;
    private String selectedPlaca       = null;
    private String selectedMarcaModelo = null;
    private int ultimoKilometrajeVehiculo = 0;

    private final Map<String, String> clienteNombreToId = new HashMap<>();
    private final Map<String, String> vehiculoDescToId  = new HashMap<>();
    private final Map<String, String> vehiculoDescToPlaca = new HashMap<>();
    private final Map<String, String> vehiculoDescToMarca = new HashMap<>();
    private final Map<String, Integer> vehiculoDescToKm = new HashMap<>();

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
        escucharClientes();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clientesListener != null) clientesListener.remove();
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

    private void escucharClientes() {
        // Quitamos el orderBy para evitar que Firestore oculte documentos sin ese campo
        clientesListener = db.collection("clientes")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error escuchando clientes: " + e.getMessage(), e);
                        return;
                    }
                    if (snap == null) return;

                    List<String> nombres = new ArrayList<>();
                    clienteNombreToId.clear();
                    
                    for (QueryDocumentSnapshot doc : snap) {
                        try {
                            String nombre = doc.getString("nombre");
                            String docId = doc.getString("documento");
                            if (nombre != null && !nombre.isEmpty()) {
                                String item = nombre + (docId != null ? " (" + docId + ")" : "");
                                nombres.add(item);
                                clienteNombreToId.put(item, doc.getId());
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error en doc: " + doc.getId(), ex);
                        }
                    }
                    
                    Log.d(TAG, "Clientes detectados: " + nombres.size());
                    ArrayAdapter<String> adp = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line, nombres);
                    actvCliente.setAdapter(adp);
                });
    }

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
                        Long   kmL    = doc.getLong("ultimoKilometraje");
                        int    km     = kmL != null ? kmL.intValue() : 0;

                        if (placa == null || placa.isEmpty()) continue;

                        String desc = placa + " — " + (marca != null ? marca : "") + " " + (modelo != null ? modelo : "");
                        descs.add(desc);
                        vehiculoDescToId.put(desc, doc.getId());
                        vehiculoDescToPlaca.put(desc, placa);
                        vehiculoDescToMarca.put(desc, (marca != null ? marca : "") + " " + (modelo != null ? modelo : ""));
                        vehiculoDescToKm.put(desc, km);
                    }

                    activarSeccion(2);
                    ArrayAdapter<String> adp = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line, descs);
                    actvVehiculo.setAdapter(adp);
                    actvVehiculo.setEnabled(true);
                    btnNuevoVehiculo.setEnabled(true);

                    if (!descs.isEmpty()) actvVehiculo.showDropDown();
                });
    }

    private void setupListeners() {
        actvCliente.setOnClickListener(v -> actvCliente.showDropDown());
        actvCliente.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actvCliente.showDropDown(); });
        actvVehiculo.setOnClickListener(v -> { if (actvVehiculo.isEnabled()) actvVehiculo.showDropDown(); });

        actvCliente.setOnItemClickListener((parent, v, pos, id) -> {
            String item = (String) parent.getItemAtPosition(pos);
            selectedClienteId     = clienteNombreToId.get(item);
            selectedClienteNombre = item.split(" \\(")[0];
            actvVehiculo.setText("");
            selectedVehiculoId  = null;
            desactivarSeccion(3);
            cargarVehiculos(selectedClienteId);
        });

        actvVehiculo.setOnItemClickListener((parent, v, pos, id) -> {
            String desc = (String) parent.getItemAtPosition(pos);
            selectedVehiculoId  = vehiculoDescToId.get(desc);
            selectedPlaca       = vehiculoDescToPlaca.get(desc);
            selectedMarcaModelo = vehiculoDescToMarca.get(desc);
            ultimoKilometrajeVehiculo = vehiculoDescToKm.getOrDefault(desc, 0);
            etKilometraje.setHint("Anterior: " + ultimoKilometrajeVehiculo + " km");
            activarSeccion(3);
        });

        findViewById(R.id.btnNuevoCliente).setOnClickListener(v -> mostrarDialogoNuevoCliente());
        btnNuevoVehiculo.setOnClickListener(v -> { if (selectedClienteId != null) mostrarDialogoNuevoVehiculo(); });
        btnGuardar.setOnClickListener(v -> guardarOrden());
    }

    private void activarSeccion(int num) {
        if (num == 2) {
            tvNumVehiculo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#db2d2c")));
            ((android.widget.LinearLayout) tvNumVehiculo.getParent()).getChildAt(1).setEnabled(true);
        }
        if (num == 3) {
            tvNumDetalles.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#db2d2c")));
            etFalla.setEnabled(true);
            etKilometraje.setEnabled(true);
            etMonto.setEnabled(true);
            btnGuardar.setEnabled(true);
        }
    }

    private void desactivarSeccion(int num) {
        if (num == 3) {
            tvNumDetalles.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
            etFalla.setEnabled(false);
            etKilometraje.setEnabled(false);
            etMonto.setEnabled(false);
            btnGuardar.setEnabled(false);
        }
    }

    private void guardarOrden() {
        if (selectedClienteId == null || selectedVehiculoId == null) return;
        String falla = etFalla.getText().toString().trim();
        if (falla.isEmpty()) return;
        String kmStr = etKilometraje.getText().toString().trim();
        int km = kmStr.isEmpty() ? 0 : Integer.parseInt(kmStr.replaceAll(",", ""));
        double monto = etMonto.getText().toString().trim().isEmpty() ? 0.0 : Double.parseDouble(etMonto.getText().toString().trim());
        OrdenTrabajo orden = new OrdenTrabajo(selectedClienteId, selectedClienteNombre, selectedVehiculoId, selectedPlaca, selectedMarcaModelo, falla, monto, km);
        db.collection("ordenes_trabajo").add(orden).addOnSuccessListener(ref -> finish());
    }

    private void mostrarDialogoNuevoCliente() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_cliente, null);
        TextInputEditText etNombre  = v.findViewById(R.id.etNombreCliente);
        TextInputEditText etDoc     = v.findViewById(R.id.etDocumentoCliente);
        new AlertDialog.Builder(this).setTitle("Nuevo cliente").setView(v)
                .setPositiveButton("Guardar", (d, w) -> {
                    Cliente nuevo = new Cliente(etDoc.getText().toString().trim(), etNombre.getText().toString().trim(), "Persona", "", "");
                    db.collection("clientes").add(nuevo);
                }).show();
    }

    private void mostrarDialogoNuevoVehiculo() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_vehiculo, null);
        TextInputEditText etPlaca  = v.findViewById(R.id.etPlacaVehiculo);
        new AlertDialog.Builder(this).setTitle("Nuevo vehículo").setView(v)
                .setPositiveButton("Guardar", (d, w) -> {
                    Vehiculo veh = new Vehiculo(etPlaca.getText().toString().trim().toUpperCase(), "", "", 0, "", "", selectedClienteId);
                    db.collection("clientes").document(selectedClienteId).collection("vehiculos").add(veh);
                }).show();
    }
}
