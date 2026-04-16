package com.company.appMancuria;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    private FirebaseFirestore db;
    private ClienteAdapter adapter;
    private List<Cliente> listaClientes = new ArrayList<>();
    private List<Cliente> listaFiltrada = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        // Aplicar insets para Safe Areas
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

        ExtendedFloatingActionButton fab = findViewById(R.id.fabNuevoCliente);
        fab.setOnClickListener(v -> mostrarDialogoNuevoCliente(null));

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
        for (Cliente c : listaClientes) {
            if (c.getNombre().toLowerCase().contains(query) || 
                (c.getDocumento() != null && c.getDocumento().toLowerCase().contains(query))) {
                listaFiltrada.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void consultarClientes() {
        db.collection("clientes")
                .orderBy("nombre", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    listaClientes.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Cliente c = doc.toObject(Cliente.class);
                        c.setId(doc.getId());
                        listaClientes.add(c);
                    }
                    filtrar(""); // Actualizar vista inicial
                });
    }

    private void mostrarDialogoNuevoCliente(Cliente clienteExistente) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_cliente, null);

        TextInputEditText etNombre  = dialogView.findViewById(R.id.etNombreCliente);
        TextInputEditText etDoc     = dialogView.findViewById(R.id.etDocumentoCliente);
        TextInputEditText etTel     = dialogView.findViewById(R.id.etTelefonoCliente);
        TextInputEditText etCorreo  = dialogView.findViewById(R.id.etCorreoCliente);
        RadioGroup        rgTipo    = dialogView.findViewById(R.id.rgTipoCliente);
        RadioButton       rbPersona = dialogView.findViewById(R.id.rbPersona);
        RadioButton       rbEmpresa = dialogView.findViewById(R.id.rbEmpresa);

        boolean esEdicion = clienteExistente != null;
        if (esEdicion) {
            etNombre.setText(clienteExistente.getNombre());
            etDoc.setText(clienteExistente.getDocumento());
            etTel.setText(clienteExistente.getTelefono());
            etCorreo.setText(clienteExistente.getCorreo());
            if ("Empresa".equals(clienteExistente.getTipo())) rbEmpresa.setChecked(true);
            else rbPersona.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(esEdicion ? "Editar cliente" : "Nuevo cliente")
                .setView(dialogView)
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

                if (esEdicion) {
                    actualizarClienteYOrdenes(clienteExistente.getId(), nombre, doc, tipo, tel, correo, dialog);
                } else {
                    Cliente nuevo = new Cliente(doc, nombre, tipo, tel, correo);
                    db.collection("clientes").add(nuevo)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Cliente registrado ✓", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                }
            });
        });
        dialog.show();
    }

    private void actualizarClienteYOrdenes(String clienteId, String nombre, String doc, String tipo, String tel, String correo, AlertDialog dialog) {
        db.collection("ordenes_trabajo")
                .whereEqualTo("clienteId", clienteId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    
                    // Actualizar documento del cliente
                    Map<String, Object> clienteUpdate = new HashMap<>();
                    clienteUpdate.put("nombre", nombre);
                    clienteUpdate.put("documento", doc);
                    clienteUpdate.put("tipo", tipo);
                    clienteUpdate.put("telefono", tel);
                    clienteUpdate.put("correo", correo);
                    batch.update(db.collection("clientes").document(clienteId), clienteUpdate);

                    // Actualizar denormalización en órdenes de trabajo
                    for (DocumentSnapshot docOrden : querySnapshot.getDocuments()) {
                        batch.update(docOrden.getReference(), "clienteNombre", nombre);
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cliente y órdenes actualizados", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
    }

    private void mostrarDetalleCliente(Cliente cliente) {
        String[] opciones = {"✏️ Editar cliente", "🚗 Ver vehículos"};
        new AlertDialog.Builder(this)
                .setTitle(cliente.getNombre())
                .setItems(opciones, (d, w) -> {
                    if (w == 0) mostrarDialogoNuevoCliente(cliente);
                    else        verVehiculosCliente(cliente);
                }).show();
    }

    private void verVehiculosCliente(Cliente cliente) {
        db.collection("clientes").document(cliente.getId()).collection("vehiculos").get()
                .addOnSuccessListener(snap -> {
                    List<String> items = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String placa = doc.getString("placa");
                        String marca = doc.getString("marca");
                        String mod   = doc.getString("modelo");
                        items.add((placa != null ? placa : "") + " — " + marca + " " + mod);
                        ids.add(doc.getId());
                    }
                    items.add("➕ Agregar vehículo");
                    String[] arr = items.toArray(new String[0]);

                    new AlertDialog.Builder(this)
                            .setTitle(cliente.getNombre() + " — Vehículos")
                            .setItems(arr, (d, w) -> {
                                if (w == arr.length - 1) {
                                    mostrarDialogoNuevoVehiculo(cliente, null);
                                } else {
                                    Vehiculo v = snap.getDocuments().get(w).toObject(Vehiculo.class);
                                    v.setId(ids.get(w));
                                    mostrarOpcionesVehiculo(cliente, v);
                                }
                            }).show();
                });
    }

    private void mostrarOpcionesVehiculo(Cliente cliente, Vehiculo vehiculo) {
        String[] opts = {"✏️ Editar vehículo", "❌ Eliminar (opcional)"};
        new AlertDialog.Builder(this)
                .setTitle("Vehículo: " + vehiculo.getPlaca())
                .setItems(opts, (d, w) -> {
                    if (w == 0) mostrarDialogoNuevoVehiculo(cliente, vehiculo);
                }).show();
    }

    private void mostrarDialogoNuevoVehiculo(Cliente cliente, Vehiculo vehiculoExistente) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_vehiculo, null);
        TextInputEditText etPlaca  = v.findViewById(R.id.etPlacaVehiculo);
        TextInputEditText etMarca  = v.findViewById(R.id.etMarcaVehiculo);
        TextInputEditText etModelo = v.findViewById(R.id.etModeloVehiculo);
        TextInputEditText etAnio   = v.findViewById(R.id.etAnioVehiculo);
        TextInputEditText etColor  = v.findViewById(R.id.etColorVehiculo);
        TextInputEditText etVin    = v.findViewById(R.id.etVinVehiculo);

        boolean esEdicion = vehiculoExistente != null;
        if (esEdicion) {
            etPlaca.setText(vehiculoExistente.getPlaca());
            etMarca.setText(vehiculoExistente.getMarca());
            etModelo.setText(vehiculoExistente.getModelo());
            etAnio.setText(String.valueOf(vehiculoExistente.getAnio()));
            etColor.setText(vehiculoExistente.getColor());
            etVin.setText(vehiculoExistente.getVin());
        }

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
                .setTitle(esEdicion ? "Editar vehículo" : "Agregar vehículo")
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

                if (esEdicion) {
                    actualizarVehiculoYOrdenes(cliente.getId(), vehiculoExistente, placa, marca, modelo, anio, color, vin, dialog);
                } else {
                    Vehiculo nuevo = new Vehiculo(placa, marca, modelo, anio, color, vin, cliente.getId());
                    db.collection("clientes").document(cliente.getId())
                            .collection("vehiculos").add(nuevo)
                            .addOnSuccessListener(ref -> {
                                db.collection("clientes").document(cliente.getId())
                                        .update("cantidadVehiculos", com.google.firebase.firestore.FieldValue.increment(1));
                                Toast.makeText(this, "Vehículo agregado ✓", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                }
            });
        });
        dialog.show();
    }

    private void actualizarVehiculoYOrdenes(String clienteId, Vehiculo vehiculoOld, String placa, String marca, String modelo, int anio, String color, String vin, AlertDialog dialog) {
        db.collection("ordenes_trabajo")
                .whereEqualTo("vehiculoId", vehiculoOld.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    
                    // Actualizar vehículo
                    Map<String, Object> vehiculoUpdate = new HashMap<>();
                    vehiculoUpdate.put("placa", placa);
                    vehiculoUpdate.put("marca", marca);
                    vehiculoUpdate.put("modelo", modelo);
                    vehiculoUpdate.put("anio", anio);
                    vehiculoUpdate.put("color", color);
                    vehiculoUpdate.put("vin", vin);
                    
                    batch.update(db.collection("clientes").document(clienteId)
                            .collection("vehiculos").document(vehiculoOld.getId()), vehiculoUpdate);

                    // Actualizar denormalización en órdenes
                    String marcaModelo = marca + " " + modelo + (anio > 0 ? " " + anio : "");
                    for (DocumentSnapshot docOrden : querySnapshot.getDocuments()) {
                        batch.update(docOrden.getReference(), "placa", placa, "marcaModelo", marcaModelo);
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Vehículo y órdenes actualizados", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
    }
}
