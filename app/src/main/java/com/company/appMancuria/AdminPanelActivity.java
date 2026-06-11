package com.company.appMancuria;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupMenu;
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

import com.company.appMancuria.adapters.ServicioAdapter;
import com.company.appMancuria.adapters.TrabajadorAdapter;
import com.company.appMancuria.models.Servicio;
import com.company.appMancuria.models.Usuario;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminPanelActivity extends AppCompatActivity {

    private static final String TAG = "AdminPanel";
    private FirebaseFirestore db;
    
    // Gestión de Trabajadores
    private RecyclerView rvTrabajadores;
    private TrabajadorAdapter adapterTrabajadores;
    private List<Usuario> listaTrabajadores = new ArrayList<>();

    // Gestión de Servicios (Catálogo)
    private RecyclerView rvServicios;
    private ServicioAdapter adapterServicios;
    private List<Servicio> listaServicios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        View mainView = findViewById(R.id.rootAdminPanel);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        setupToolbar();
        bindViews();
        setupRecyclers();
        setupBuscadorServicios();
        
        consultarTrabajadores();
        consultarServicios();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAdmin);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        rvTrabajadores = findViewById(R.id.rvTrabajadores);
        rvServicios = findViewById(R.id.rvServicios);
        
        findViewById(R.id.fabNuevoUsuario).setOnClickListener(v -> mostrarDialogoUsuario(null));
        findViewById(R.id.btnNuevoServicio).setOnClickListener(v -> mostrarDialogoServicio(null));
    }

    private void setupRecyclers() {
        rvTrabajadores.setLayoutManager(new LinearLayoutManager(this));
        adapterTrabajadores = new TrabajadorAdapter(listaTrabajadores, this::mostrarOpcionesTrabajador);
        rvTrabajadores.setAdapter(adapterTrabajadores);

        rvServicios.setLayoutManager(new LinearLayoutManager(this));
        adapterServicios = new ServicioAdapter(listaServicios, this::mostrarOpcionesServicio);
        rvServicios.setAdapter(adapterServicios);
    }

    private void setupBuscadorServicios() {
        TextInputEditText etBuscador = findViewById(R.id.etBuscadorServicios);
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapterServicios.filtrar(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void consultarServicios() {
        // Quitamos orderBy para evitar errores si no hay índices configurados en Firebase
        db.collection("servicios").addSnapshotListener((snap, err) -> {
            if (err != null) {
                Toast.makeText(this, "Error Firestore: " + err.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (snap != null) {
                if (snap.isEmpty()) {
                    inicializarServiciosDefault();
                } else {
                    listaServicios.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Servicio s = doc.toObject(Servicio.class);
                        s.setId(doc.getId());
                        listaServicios.add(s);
                    }
                    // Ordenar alfabéticamente en la app
                    Collections.sort(listaServicios, (s1, s2) -> s1.getNombre().compareToIgnoreCase(s2.getNombre()));
                    adapterServicios.actualizarLista(listaServicios);
                }
            }
        });
    }

    private void inicializarServiciosDefault() {
        List<String> defaults = Arrays.asList(
            "Afinamiento electrónico", "Mantenimiento menor", "Mantenimiento mayor",
            "Reparación de arrancador", "Reparación de alternador", "Mantenimiento alternador",
            "Mantenimiento arrancador", "Reparación de luces delanteras", "Reparación de luces posteriores",
            "Reparación de inyectores", "Limpieza de tanque de combustible", "Reparación de turbo compresor",
            "Desmontar tablero general", "Suspensión", "Dirección", "Desmontaje de cremallera del timón"
        );
        for (String s : defaults) {
            Map<String, Object> data = new HashMap<>();
            data.put("nombre", s);
            db.collection("servicios").add(data);
        }
    }

    private void mostrarOpcionesServicio(Servicio s, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("✏️ Editar");
        popup.getMenu().add("🗑️ Eliminar");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().contains("Editar")) mostrarDialogoServicio(s);
            else confirmarEliminacionServicio(s);
            return true;
        });
        popup.show();
    }

    private void mostrarDialogoServicio(Servicio s) {
        // ✅ Corregido: Ahora usa el diseño dialog_servicio.xml para visibilidad total
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_servicio, null);
        TextInputEditText et = dialogView.findViewById(R.id.etNombreServicioDialog);
        
        if (s != null) et.setText(s.getNombre());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(s == null ? "Nuevo Servicio" : "Editar Servicio")
                .setView(dialogView)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String nombre = et.getText().toString().trim();
                if (nombre.isEmpty()) { et.setError("Obligatorio"); return; }

                if (s == null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("nombre", nombre);
                    db.collection("servicios").add(data).addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Servicio agregado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss(); // ✅ Corregido: Se cierra el diálogo al terminar
                    });
                } else {
                    db.collection("servicios").document(s.getId()).update("nombre", nombre).addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            });
        });
        dialog.show();
    }

    private void confirmarEliminacionServicio(Servicio s) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar")
                .setMessage("¿Eliminar '" + s.getNombre() + "' del catálogo?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    db.collection("servicios").document(s.getId()).delete()
                        .addOnSuccessListener(v -> Toast.makeText(this, "Servicio eliminado", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void consultarTrabajadores() {
        db.collection("usuarios").addSnapshotListener((snap, err) -> {
            if (err != null) return;
            if (snap != null) {
                listaTrabajadores.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    Usuario u = doc.toObject(Usuario.class);
                    u.setId(doc.getId());
                    if (u.getEstado() == null) u.setEstado("activo");
                    listaTrabajadores.add(u);
                }
                adapterTrabajadores.notifyDataSetChanged();
            }
        });
    }

    private void mostrarDialogoUsuario(Usuario usuarioExistente) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_usuario, null);
        TextInputEditText etNombre = dialogView.findViewById(R.id.etNombreUsuario);
        TextInputEditText etCorreo = dialogView.findViewById(R.id.etUserLogin);
        TextInputEditText etPasswordTemporal = dialogView.findViewById(R.id.etPasswordUsuario);
        RadioButton rbAdmin = dialogView.findViewById(R.id.rbAdmin);

        if (usuarioExistente != null) {
            etNombre.setText(usuarioExistente.getNombre());
            etCorreo.setText(usuarioExistente.getCorreo());
            etPasswordTemporal.setText("");
            etPasswordTemporal.setHint("No se edita desde la app");
            etPasswordTemporal.setEnabled(false);
            if ("admin".equals(usuarioExistente.getRol())) rbAdmin.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(usuarioExistente == null ? "Nuevo Trabajador" : "Editar Trabajador")
                .setView(dialogView)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String nombre = etNombre.getText().toString().trim();
                String correo = etCorreo.getText().toString().trim();
                String passwordTemporal = etPasswordTemporal.getText().toString().trim();
                String rol    = rbAdmin.isChecked() ? "admin" : "mecanico";

                if (nombre.isEmpty()) { etNombre.setError("Obligatorio"); return; }
                if (correo.isEmpty()) { etCorreo.setError("Obligatorio"); return; }

                if (usuarioExistente == null) {
                    if (passwordTemporal.length() < 6) {
                        etPasswordTemporal.setError("Minimo 6 caracteres");
                        return;
                    }
                    crearTrabajadorAuth(nombre, correo, passwordTemporal, rol, dialog);
                } else {
                    db.collection("usuarios").document(usuarioExistente.getId())
                            .update("nombre", nombre, "correo", correo, "usuario", correo, "rol", rol)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Trabajador actualizado", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                }
            });
        });
        dialog.show();
    }

    private void crearTrabajadorAuth(String nombre, String correo, String passwordTemporal, String rol, AlertDialog dialog) {
        FirebaseAuth secondaryAuth = getSecondaryAuth();
        if (secondaryAuth == null) {
            Toast.makeText(this, "No se pudo inicializar Firebase Auth", Toast.LENGTH_LONG).show();
            return;
        }

        secondaryAuth.createUserWithEmailAndPassword(correo, passwordTemporal)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() == null) {
                        secondaryAuth.signOut();
                        Toast.makeText(this, "No se pudo obtener el UID creado", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = authResult.getUser().getUid();
                    Usuario nuevo = new Usuario(uid, nombre, correo, correo, "", rol, "activo");
                    db.collection("usuarios").document(uid).set(nuevo)
                            .addOnSuccessListener(ref -> {
                                secondaryAuth.signOut();
                                Toast.makeText(this, "Trabajador creado", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                secondaryAuth.signOut();
                                Toast.makeText(this, "Auth creado, pero fallo el perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    secondaryAuth.signOut();
                    Toast.makeText(this, "No se pudo crear el trabajador: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private FirebaseAuth getSecondaryAuth() {
        final String secondaryAppName = "AdminUserCreation";
        try {
            return FirebaseAuth.getInstance(FirebaseApp.getInstance(secondaryAppName));
        } catch (IllegalStateException ignored) {
            FirebaseOptions options = FirebaseOptions.fromResource(this);
            if (options == null) return null;
            FirebaseApp secondaryApp = FirebaseApp.initializeApp(this, options, secondaryAppName);
            return FirebaseAuth.getInstance(secondaryApp);
        }
    }

    private void mostrarOpcionesTrabajador(Usuario u, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("✏️ Editar");
        popup.getMenu().add(u.getEstado().equals("activo") ? "🚫 Suspender" : "✅ Reactivar");
        popup.getMenu().add("🗑️ Eliminar");

        popup.setOnMenuItemClickListener(item -> {
            String op = item.getTitle().toString();
            if (op.contains("Editar")) mostrarDialogoUsuario(u);
            else if (op.contains("Suspender") || op.contains("Reactivar")) {
                String nuevoEstado = u.getEstado().equals("activo") ? "suspendido" : "activo";
                db.collection("usuarios").document(u.getId()).update("estado", nuevoEstado);
            } else if (op.contains("Eliminar")) confirmarEliminacion(u);
            return true;
        });
        popup.show();
    }

    private void confirmarEliminacion(Usuario u) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar")
                .setMessage("¿Eliminar a " + u.getNombre() + "?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    db.collection("usuarios").document(u.getId()).delete()
                        .addOnSuccessListener(v -> Toast.makeText(this, "Eliminado correctamente", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null).show();
    }
}
