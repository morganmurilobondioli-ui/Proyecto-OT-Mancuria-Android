package com.company.appMancuria;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.appMancuria.adapters.ClienteAdapter;
import com.company.appMancuria.models.Cliente;
import com.company.appMancuria.models.Vehiculo;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ClientesActivity extends AppCompatActivity {

    private static final String TAG = "ClientesActivity";
    private FirebaseFirestore db;
    private ClienteAdapter adapter;
    private List<Cliente> listaClientes = new ArrayList<>();
    private List<Cliente> listaFiltrada = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        setupToolbar();
        setupRecyclerView();
        setupBuscador();

        findViewById(R.id.fabNuevoCliente).setOnClickListener(v -> mostrarDialogoNuevoCliente(null));

        consultarClientes();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvClientes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClienteAdapter(listaFiltrada, this::mostrarDetalleCliente);
        rv.setAdapter(adapter);
    }

    private void setupBuscador() {
        TextInputEditText etBuscador = findViewById(R.id.etBuscadorClientes);
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrar(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filtrar(String texto) {
        listaFiltrada.clear();
        String query = texto.toLowerCase().trim();
        if (query.isEmpty()) {
            listaFiltrada.addAll(listaClientes);
        } else {
            for (Cliente c : listaClientes) {
                String nombre = c.getNombre() != null ? c.getNombre().toLowerCase() : "";
                String doc = c.getDocumento() != null ? c.getDocumento().toLowerCase() : "";
                if (nombre.contains(query) || doc.contains(query)) {
                    listaFiltrada.add(c);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void consultarClientes() {
        db.collection("clientes").addSnapshotListener((snap, err) -> {
            if (err != null) return;
            if (snap != null) {
                listaClientes.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    Cliente c = doc.toObject(Cliente.class);
                    c.setId(doc.getId());
                    listaClientes.add(c);
                }
                filtrar(""); 
            }
        });
    }

    private void mostrarDialogoNuevoCliente(Cliente clienteExistente) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cliente, null);
        TextInputEditText etNombre  = dialogView.findViewById(R.id.etNombreCliente);
        TextInputEditText etDoc     = dialogView.findViewById(R.id.etDocumentoCliente);
        TextInputLayout   tilDoc    = dialogView.findViewById(R.id.tilDocumentoCliente);
        TextInputEditText etTel     = dialogView.findViewById(R.id.etTelefonoCliente);
        RadioGroup        rgTipo    = dialogView.findViewById(R.id.rgTipoCliente);
        RadioButton       rbPersona = dialogView.findViewById(R.id.rbPersona);
        RadioButton       rbEmpresa = dialogView.findViewById(R.id.rbEmpresa);

        actualizarEstadoDocumento(tilDoc, etDoc, rbEmpresa.isChecked());

        rgTipo.setOnCheckedChangeListener((group, checkedId) -> {
            boolean esEmpresa = (checkedId == R.id.rbEmpresa);
            actualizarEstadoDocumento(tilDoc, etDoc, esEmpresa);
            String current = etDoc.getText().toString();
            int max = esEmpresa ? 11 : 8;
            if (current.length() > max) etDoc.setText(current.substring(0, max));
        });

        if (clienteExistente != null) {
            etNombre.setText(clienteExistente.getNombre());
            etDoc.setText(clienteExistente.getDocumento());
            etTel.setText(clienteExistente.getTelefono());
            if ("Empresa".equals(clienteExistente.getTipo())) rbEmpresa.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(clienteExistente != null ? "Editar cliente" : "Nuevo cliente")
                .setView(dialogView)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String nombre = etNombre.getText().toString().trim();
                String doc    = etDoc.getText().toString().trim();
                String tel    = etTel.getText().toString().trim();
                String tipo   = rbEmpresa.isChecked() ? "Empresa" : "Persona";

                if (nombre.isEmpty()) { etNombre.setError("Obligatorio"); return; }
                int longEsperada = rbEmpresa.isChecked() ? 11 : 8;
                if (doc.length() != longEsperada) { etDoc.setError("Faltan dígitos"); return; }

                if (clienteExistente != null) {
                    db.collection("clientes").document(clienteExistente.getId())
                            .update("nombre", nombre, "documento", doc, "tipo", tipo, "telefono", tel)
                            .addOnSuccessListener(v -> dialog.dismiss());
                } else {
                    db.collection("clientes").add(new Cliente(doc, nombre, tipo, tel, "")).addOnSuccessListener(ref -> dialog.dismiss());
                }
            });
        });
        dialog.show();
    }

    private void actualizarEstadoDocumento(TextInputLayout til, TextInputEditText et, boolean esEmpresa) {
        int limite = esEmpresa ? 11 : 8;
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limite)});
        til.setHint(esEmpresa ? "RUC (11 dígitos)" : "DNI (8 dígitos)");
    }

    private void mostrarDetalleCliente(Cliente cliente) {
        String[] opciones = {"✏️ Editar", "🚗 Vehículos"};
        new AlertDialog.Builder(this).setTitle(cliente.getNombre())
                .setItems(opciones, (d, w) -> {
                    if (w == 0) mostrarDialogoNuevoCliente(cliente);
                    else verVehiculosCliente(cliente);
                }).show();
    }

    private void verVehiculosCliente(Cliente cliente) {
        db.collection("clientes").document(cliente.getId()).collection("vehiculos").get()
                .addOnSuccessListener(snap -> {
                    List<String> items = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Vehiculo v = doc.toObject(Vehiculo.class);
                        if (v != null) items.add(v.getDescripcionCorta());
                    }
                    items.add("➕ Agregar vehículo");
                    new AlertDialog.Builder(this).setTitle("Vehículos")
                            .setItems(items.toArray(new String[0]), (d, w) -> {
                                if (w == items.size() - 1) mostrarDialogoNuevoVehiculo(cliente);
                            }).show();
                });
    }

    private void mostrarDialogoNuevoVehiculo(Cliente cliente) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_vehiculo, null);
        TextInputEditText etPlaca = v.findViewById(R.id.etPlacaVehiculo);
        TextInputEditText etMarca = v.findViewById(R.id.etMarcaVehiculo);
        TextInputEditText etModelo = v.findViewById(R.id.etModeloVehiculo);
        TextInputEditText etAnio = v.findViewById(R.id.etAnioVehiculo);
        TextInputEditText etColor = v.findViewById(R.id.etColorVehiculo);
        TextInputEditText etVin = v.findViewById(R.id.etVinVehiculo);

        etPlaca.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;
                String text = s.toString().toUpperCase().replace("-", "");
                if (text.length() > 3) text = text.substring(0, 3) + "-" + text.substring(3);
                etPlaca.setText(text);
                etPlaca.setSelection(text.length());
                isUpdating = false;
            }
        });

        new AlertDialog.Builder(this).setTitle("Nuevo vehículo").setView(v)
                .setPositiveButton("Guardar", (d, w) -> {
                    String placa = etPlaca.getText().toString().trim().toUpperCase();
                    String marca = etMarca.getText().toString().trim();
                    String modelo = etModelo.getText().toString().trim();
                    int anio = etAnio.getText().toString().isEmpty() ? 0 : Integer.parseInt(etAnio.getText().toString());
                    String color = etColor.getText().toString().trim();
                    String vin = etVin.getText().toString().trim();
                    
                    if (placa.isEmpty()) return;

                    Vehiculo veh = new Vehiculo(placa, marca, modelo, anio, color, vin, cliente.getId());
                    db.collection("clientes").document(cliente.getId()).collection("vehiculos").add(veh)
                            .addOnSuccessListener(ref -> {
                                // Actualizar contador real de vehículos
                                db.collection("clientes").document(cliente.getId()).collection("vehiculos").get()
                                        .addOnSuccessListener(snapCount -> {
                                            db.collection("clientes").document(cliente.getId())
                                                    .update("cantidadVehiculos", snapCount.size());
                                            Toast.makeText(this, "Vehículo registrado", Toast.LENGTH_SHORT).show();
                                        });
                            });
                }).show();
    }
}
