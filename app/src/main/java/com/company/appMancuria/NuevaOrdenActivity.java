package com.company.appMancuria;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.company.appMancuria.models.Cliente;
import com.company.appMancuria.models.OrdenTrabajo;
import com.company.appMancuria.models.Vehiculo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NuevaOrdenActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration clientesListener;

    private String selectedClienteId = null;
    private String selectedClienteNombre = null;
    private String selectedVehiculoId = null;
    private String selectedPlaca = null;
    private String selectedMarcaModelo = null;
    private int ultimoKilometrajeVehiculo = 0;

    private final Map<String, String> clienteNombreToId = new HashMap<>();
    private final Map<String, String> vehiculoDescToId = new HashMap<>();
    private final Map<String, Integer> vehiculoDescToKm = new HashMap<>();

    private AutoCompleteTextView actvCliente, actvVehiculo, actvServicio;
    private TextInputLayout layoutVehiculo;
    private MaterialButton btnNuevoVehiculo, btnGuardar;
    private TextInputEditText etKilometraje, etMonto, etFechaIngreso;
    private LinearLayout llServiciosSeleccionados;
    
    private MaterialCardView cardNumVehiculo, cardNumDetalles;
    private TextView tvNumVehiculo, tvNumDetalles;

    private List<String> listaNombresServicios = new ArrayList<>();
    private List<String> serviciosSeleccionados = new ArrayList<>();
    private ArrayAdapter<String> adapterServicios;

    private static final SimpleDateFormat SDF_DISPLAY = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_orden);

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setFechaActual();
        escucharClientes();
        cargarCatalogoServicios();
        setupListeners();
    }

    private void bindViews() {
        actvCliente = findViewById(R.id.actvCliente);
        actvVehiculo = findViewById(R.id.actvVehiculo);
        actvServicio = findViewById(R.id.actvServicio);
        layoutVehiculo = findViewById(R.id.layoutVehiculo);
        btnNuevoVehiculo = findViewById(R.id.btnNuevoVehiculo);
        btnGuardar = findViewById(R.id.btnGuardar);
        etKilometraje = findViewById(R.id.etKilometraje);
        etMonto = findViewById(R.id.etMonto);
        etFechaIngreso = findViewById(R.id.etFechaIngreso);
        llServiciosSeleccionados = findViewById(R.id.llServiciosSeleccionados);
        
        cardNumVehiculo = findViewById(R.id.cardNumVehiculo);
        cardNumDetalles = findViewById(R.id.cardNumDetalles);
        tvNumVehiculo = findViewById(R.id.tvNumVehiculo);
        tvNumDetalles = findViewById(R.id.tvNumDetalles);

        adapterServicios = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, listaNombresServicios);
        actvServicio.setAdapter(adapterServicios);
    }

    private void escucharClientes() {
        clientesListener = db.collection("clientes").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            List<String> nombres = new ArrayList<>();
            clienteNombreToId.clear();
            for (QueryDocumentSnapshot doc : snap) {
                String nombre = doc.getString("nombre");
                String docId = doc.getString("documento");
                if (nombre != null) {
                    String item = nombre + (docId != null ? " (" + docId + ")" : "");
                    nombres.add(item);
                    clienteNombreToId.put(item, doc.getId());
                }
            }
            actvCliente.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nombres));
        });
    }

    private void cargarVehiculos(String clienteId) {
        db.collection("clientes").document(clienteId).collection("vehiculos").get().addOnSuccessListener(snap -> {
            List<String> descs = new ArrayList<>();
            vehiculoDescToId.clear();
            vehiculoDescToKm.clear();
            for (QueryDocumentSnapshot doc : snap) {
                Vehiculo v = doc.toObject(Vehiculo.class);
                if (v.getPlaca().isEmpty()) continue;
                
                String desc = v.getDescripcionCorta();
                descs.add(desc);
                vehiculoDescToId.put(desc, doc.getId());
                vehiculoDescToKm.put(desc, v.getUltimoKilometraje());
            }
            activarSeccion(2);
            actvVehiculo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, descs));
            if (!descs.isEmpty()) actvVehiculo.showDropDown();
        });
    }

    private void setupListeners() {
        actvCliente.setOnClickListener(v -> actvCliente.showDropDown());
        actvVehiculo.setOnClickListener(v -> { if(actvVehiculo.isEnabled()) actvVehiculo.showDropDown(); });
        actvServicio.setOnClickListener(v -> { if(actvServicio.isEnabled()) actvServicio.showDropDown(); });

        actvCliente.setOnItemClickListener((parent, v, pos, id) -> {
            String item = (String) parent.getItemAtPosition(pos);
            selectedClienteId = clienteNombreToId.get(item);
            selectedClienteNombre = item.split(" \\(")[0];
            actvVehiculo.setText("");
            selectedVehiculoId = null;
            desactivarSeccion(3);
            cargarVehiculos(selectedClienteId);
        });

        actvVehiculo.setOnItemClickListener((parent, v, pos, id) -> {
            String desc = (String) parent.getItemAtPosition(pos);
            selectedVehiculoId = vehiculoDescToId.get(desc);
            ultimoKilometrajeVehiculo = vehiculoDescToKm.getOrDefault(desc, 0);
            
            // ✅ Mostrar kilometraje anterior y pre-cargar el valor como Kilometraje Inicial
            etKilometraje.setHint("Anterior: " + ultimoKilometrajeVehiculo + " km");
            etKilometraje.setText(String.valueOf(ultimoKilometrajeVehiculo));
            
            selectedPlaca = desc.split(" — ")[0];
            selectedMarcaModelo = desc.contains(" — ") ? desc.split(" — ")[1] : "";
            activarSeccion(3);
        });

        actvServicio.setOnItemClickListener((parent, view, position, id) -> {
            String servicio = (String) parent.getItemAtPosition(position);
            agregarServicioAChips(servicio);
            actvServicio.setText(""); // Limpiar para permitir seleccionar otro
        });

        btnGuardar.setOnClickListener(v -> guardarOrden());
        findViewById(R.id.btnNuevoCliente).setOnClickListener(v -> mostrarDialogoNuevoCliente());
        btnNuevoVehiculo.setOnClickListener(v -> { if(selectedClienteId != null) mostrarDialogoNuevoVehiculo(); });
    }

    private void agregarServicioAChips(String servicio) {
        if (serviciosSeleccionados.contains(servicio)) {
            Toast.makeText(this, "Servicio ya seleccionado", Toast.LENGTH_SHORT).show();
            return;
        }

        serviciosSeleccionados.add(servicio);

        Chip chip = new Chip(this);
        chip.setText(servicio);
        chip.setCloseIconVisible(true);
        chip.setTextColor(Color.BLACK); // ✅ Letra negra para que se vea bien
        chip.setCloseIconTint(ColorStateList.valueOf(Color.BLACK));
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0"))); // Gris un poco más oscuro
        
        // Ajustar ancho para que se vea uno abajo del otro
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 4, 0, 4);
        chip.setLayoutParams(params);

        chip.setOnCloseIconClickListener(v -> {
            llServiciosSeleccionados.removeView(chip);
            serviciosSeleccionados.remove(servicio);
        });

        llServiciosSeleccionados.addView(chip);
    }

    private void activarSeccion(int num) {
        int colorRojo = Color.parseColor("#db2d2c");
        if (num == 2) {
            // ✅ Corregido: Referencia a la tarjeta del número
            if (cardNumVehiculo != null) cardNumVehiculo.setCardBackgroundColor(ColorStateList.valueOf(colorRojo));
            layoutVehiculo.setEnabled(true);
            actvVehiculo.setEnabled(true);
            btnNuevoVehiculo.setEnabled(true);
        } else if (num == 3) {
            // ✅ Corregido: Referencia a la tarjeta del número
            if (cardNumDetalles != null) cardNumDetalles.setCardBackgroundColor(ColorStateList.valueOf(colorRojo));
            actvServicio.setEnabled(true);
            etKilometraje.setEnabled(true);
            etMonto.setEnabled(true);
            btnGuardar.setEnabled(true);
        }
    }

    private void desactivarSeccion(int num) {
        if (num == 3) {
            if (cardNumDetalles != null) cardNumDetalles.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
            actvServicio.setEnabled(false);
            etKilometraje.setEnabled(false);
            etMonto.setEnabled(false);
            btnGuardar.setEnabled(false);
        }
    }

    private void cargarCatalogoServicios() {
        db.collection("servicios").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            listaNombresServicios.clear();
            for (QueryDocumentSnapshot doc : value) {
                String nombre = doc.getString("nombre");
                if (nombre != null) listaNombresServicios.add(nombre);
            }
            Collections.sort(listaNombresServicios, String::compareToIgnoreCase);
            adapterServicios.notifyDataSetChanged();
        });
    }

    private void setFechaActual() { etFechaIngreso.setText(SDF_DISPLAY.format(new Date())); }

    private void guardarOrden() {
        if (serviciosSeleccionados.isEmpty()) { 
            Toast.makeText(this, "Selecciona al menos un servicio", Toast.LENGTH_SHORT).show(); 
            return; 
        }

        // Unir todos los servicios en un solo string separado por saltos de línea para el campo fallaReportada
        String serviciosUnidos = TextUtils.join("\n", serviciosSeleccionados);
        
        String kmStr = etKilometraje.getText().toString().trim();
        int km = kmStr.isEmpty() ? 0 : Integer.parseInt(kmStr);
        
        // ✅ VALIDACIÓN DE KILOMETRAJE ANTERIOR
        if (km < ultimoKilometrajeVehiculo) {
            Toast.makeText(this, "El kilometraje no puede ser menor al registrado anteriormente (" + ultimoKilometrajeVehiculo + " km)", Toast.LENGTH_LONG).show();
            return;
        }

        double monto = etMonto.getText().toString().trim().isEmpty() ? 0.0 : Double.parseDouble(etMonto.getText().toString().trim());

        OrdenTrabajo orden = new OrdenTrabajo(selectedClienteId, selectedClienteNombre, selectedVehiculoId, selectedPlaca, selectedMarcaModelo, serviciosUnidos, monto, km);
        
        // Guardar la OT y actualizar el último kilometraje en el vehículo
        db.collection("ordenes_trabajo").add(orden).addOnSuccessListener(ref -> {
            if (selectedClienteId != null && selectedVehiculoId != null) {
                db.collection("clientes").document(selectedClienteId)
                        .collection("vehiculos").document(selectedVehiculoId)
                        .update("ultimoKilometraje", km)
                        .addOnCompleteListener(task -> {
                            Toast.makeText(this, "Orden guardada y kilometraje actualizado", Toast.LENGTH_SHORT).show();
                            finish();
                        });
            } else {
                finish();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void mostrarDialogoNuevoCliente() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_cliente, null);
        TextInputEditText etNombre = v.findViewById(R.id.etNombreCliente);
        TextInputEditText etDoc = v.findViewById(R.id.etDocumentoCliente);
        new AlertDialog.Builder(this).setTitle("Nuevo cliente").setView(v).setPositiveButton("Guardar", (d, w) -> {
            String nombre = etNombre.getText().toString().trim();
            String doc = etDoc.getText().toString().trim();
            if(!nombre.isEmpty()) db.collection("clientes").add(new Cliente(doc, nombre, "Persona", "", ""));
        }).show();
    }

    private void mostrarDialogoNuevoVehiculo() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_vehiculo, null);
        TextInputEditText etPlaca = v.findViewById(R.id.etPlacaVehiculo);
        TextInputEditText etMarca = v.findViewById(R.id.etMarcaVehiculo);
        TextInputEditText etModelo = v.findViewById(R.id.etModeloVehiculo);
        TextInputEditText etAnio = v.findViewById(R.id.etAnioVehiculo);
        TextInputEditText etColor = v.findViewById(R.id.etColorVehiculo);
        TextInputEditText etVin = v.findViewById(R.id.etVinVehiculo);

        new AlertDialog.Builder(this).setTitle("Nuevo vehículo").setView(v).setPositiveButton("Guardar", (d, w) -> {
            String placa = etPlaca.getText().toString().trim().toUpperCase();
            String marca = etMarca.getText().toString().trim();
            String modelo = etModelo.getText().toString().trim();
            int anio = etAnio.getText().toString().isEmpty() ? 0 : Integer.parseInt(etAnio.getText().toString());
            String color = etColor.getText().toString().trim();
            String vin = etVin.getText().toString().trim();

            if (!placa.isEmpty()) {
                Vehiculo veh = new Vehiculo(placa, marca, modelo, anio, color, vin, selectedClienteId);
                db.collection("clientes").document(selectedClienteId).collection("vehiculos").add(veh)
                    .addOnSuccessListener(ref -> {
                        // Actualizar contador del cliente
                        db.collection("clientes").document(selectedClienteId).collection("vehiculos").get()
                                .addOnSuccessListener(snapCount -> {
                                    db.collection("clientes").document(selectedClienteId).update("cantidadVehiculos", snapCount.size());
                                    Toast.makeText(this, "Vehículo registrado", Toast.LENGTH_SHORT).show();
                                });
                    });
            }
        }).show();
    }
}
