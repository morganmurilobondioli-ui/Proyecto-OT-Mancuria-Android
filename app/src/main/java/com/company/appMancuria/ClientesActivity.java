package com.company.appMancuria;

import android.app.AlertDialog;
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
import com.company.appMancuria.utils.Validaciones;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // Quitamos el orderBy para garantizar que carguen TODOS
        db.collection("clientes")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Log.e(TAG, "Error de Firestore: " + err.getMessage(), err);
                        Toast.makeText(this, "Error al cargar: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    if (snap != null) {
                        listaClientes.clear();
                        for (QueryDocumentSnapshot doc : snap) {
                            try {
                                Cliente c = doc.toObject(Cliente.class);
                                // Forzamos el ID real de Firestore en caso de que el campo 'id' de la DB esté vacío
                                c.setId(doc.getId());
                                listaClientes.add(c);
                            } catch (Exception e) {
                                Log.e(TAG, "Error mapeando cliente: " + doc.getId(), e);
                            }
                        }
                        Log.d(TAG, "Clientes cargados: " + listaClientes.size());
                        filtrar(""); 
                    }
                });
    }

    private void mostrarDialogoNuevoCliente(Cliente clienteExistente) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cliente, null);
        TextInputEditText etNombre  = dialogView.findViewById(R.id.etNombreCliente);
        TextInputEditText etDoc     = dialogView.findViewById(R.id.etDocumentoCliente);
        TextInputEditText etTel     = dialogView.findViewById(R.id.etTelefonoCliente);
        RadioGroup        rgTipo    = dialogView.findViewById(R.id.rgTipoCliente);
        RadioButton       rbPersona = dialogView.findViewById(R.id.rbPersona);
        RadioButton       rbEmpresa = dialogView.findViewById(R.id.rbEmpresa);

        // Configuración inicial de límites
        actualizarLimiteDocumento(etDoc, rbEmpresa.isChecked());

        rgTipo.setOnCheckedChangeListener((group, checkedId) -> {
            boolean esEmpresa = (checkedId == R.id.rbEmpresa);
            actualizarLimiteDocumento(etDoc, esEmpresa);
            // Limpiar si excede el nuevo límite al cambiar
            String current = etDoc.getText().toString();
            int max = esEmpresa ? 11 : 8;
            if (current.length() > max) {
                etDoc.setText(current.substring(0, max));
            }
        });

        if (clienteExistente != null) {
            etNombre.setText(clienteExistente.getNombre());
            etDoc.setText(clienteExistente.getDocumento());
            etTel.setText(clienteExistente.getTelefono());
            if ("Empresa".equals(clienteExistente.getTipo())) {
                rbEmpresa.setChecked(true);
                actualizarLimiteDocumento(etDoc, true);
            } else {
                rbPersona.setChecked(true);
                actualizarLimiteDocumento(etDoc, false);
            }
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
                
                // Validar longitud según tipo
                int longEsperada = rbEmpresa.isChecked() ? 11 : 8;
                if (doc.length() != longEsperada) {
                    etDoc.setError("Debe tener " + longEsperada + " dígitos");
                    return;
                }

                // Validar duplicados (DNI/RUC y Teléfono)
                for (Cliente c : listaClientes) {
                    // Si estamos editando, ignorar al propio cliente
                    if (clienteExistente != null && c.getId().equals(clienteExistente.getId())) continue;

                    if (doc.equals(c.getDocumento())) {
                        etDoc.setError("Este documento ya existe");
                        Toast.makeText(this, "Error: El DNI/RUC ya está registrado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!tel.isEmpty() && tel.equals(c.getTelefono())) {
                        etTel.setError("Este teléfono ya existe");
                        Toast.makeText(this, "Error: El teléfono ya está registrado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if (clienteExistente != null) {
                    db.collection("clientes").document(clienteExistente.getId())
                            .update("nombre", nombre, "documento", doc, "tipo", tipo, "telefono", tel)
                            .addOnSuccessListener(v -> dialog.dismiss());
                } else {
                    Cliente nuevo = new Cliente(doc, nombre, tipo, tel, "");
                    db.collection("clientes").add(nuevo).addOnSuccessListener(ref -> dialog.dismiss());
                }
            });
        });
        dialog.show();
    }

    private void actualizarLimiteDocumento(TextInputEditText et, boolean esEmpresa) {
        int limite = esEmpresa ? 11 : 8;
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limite)});
        et.setHint(esEmpresa ? "RUC (11 dígitos)" : "DNI (8 dígitos)");
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
                        items.add(doc.getString("placa") + " — " + doc.getString("marca"));
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
        TextInputEditText etPlaca  = v.findViewById(R.id.etPlacaVehiculo);
        new AlertDialog.Builder(this).setTitle("Nuevo vehículo").setView(v)
                .setPositiveButton("Guardar", (d, w) -> {
                    Vehiculo veh = new Vehiculo(etPlaca.getText().toString().trim().toUpperCase(), "", "", 0, "", "", cliente.getId());
                    db.collection("clientes").document(cliente.getId()).collection("vehiculos").add(veh)
                            .addOnSuccessListener(ref -> {
                                db.collection("clientes").document(cliente.getId()).update("cantidadVehiculos", listaClientes.size());
                            });
                }).show();
    }
}
