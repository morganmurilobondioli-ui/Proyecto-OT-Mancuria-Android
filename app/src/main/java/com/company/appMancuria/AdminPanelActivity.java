package com.company.appMancuria;

import android.app.AlertDialog;
import android.os.Bundle;
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

import com.company.appMancuria.adapters.TrabajadorAdapter;
import com.company.appMancuria.models.Usuario;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminPanelActivity extends AppCompatActivity {

    private static final String TAG = "AdminPanel";
    private FirebaseFirestore db;
    private RecyclerView rvTrabajadores;
    private TrabajadorAdapter adapter;
    private List<Usuario> listaTrabajadores = new ArrayList<>();

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
        rvTrabajadores = findViewById(R.id.rvTrabajadores);
        ExtendedFloatingActionButton fab = findViewById(R.id.fabNuevoUsuario);
        fab.setOnClickListener(v -> mostrarDialogoUsuario(null));

        setupRecyclerView();
        consultarTrabajadores();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAdmin);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvTrabajadores.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrabajadorAdapter(listaTrabajadores, this::mostrarOpcionesTrabajador);
        rvTrabajadores.setAdapter(adapter);
    }

    private void consultarTrabajadores() {
        db.collection("usuarios").addSnapshotListener((snap, err) -> {
            if (err != null) {
                Log.e(TAG, "Error consultando trabajadores", err);
                return;
            }
            if (snap != null) {
                listaTrabajadores.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    Usuario u = doc.toObject(Usuario.class);
                    u.setId(doc.getId());
                    if (u.getEstado() == null) u.setEstado("activo");
                    listaTrabajadores.add(u);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void mostrarDialogoUsuario(Usuario usuarioExistente) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_usuario, null);
        TextInputEditText etNombre = v.findViewById(R.id.etNombreUsuario);
        TextInputEditText etLogin  = v.findViewById(R.id.etUserLogin);
        TextInputEditText etPass   = v.findViewById(R.id.etPasswordUsuario);
        RadioGroup        rgRol    = v.findViewById(R.id.rgRolUsuario);
        RadioButton       rbAdmin  = v.findViewById(R.id.rbAdmin);

        if (usuarioExistente != null) {
            etNombre.setText(usuarioExistente.getNombre());
            etLogin.setText(usuarioExistente.getUsuario());
            etPass.setText(usuarioExistente.getPassword());
            if ("admin".equals(usuarioExistente.getRol())) rbAdmin.setChecked(true);
        }

        new AlertDialog.Builder(this)
                .setTitle(usuarioExistente == null ? "Nuevo Usuario" : "Editar Usuario")
                .setView(v)
                .setPositiveButton("Guardar", (d, w) -> {
                    String nombre = etNombre.getText().toString().trim();
                    String login  = etLogin.getText().toString().trim();
                    String pass   = etPass.getText().toString().trim();
                    String rol    = rbAdmin.isChecked() ? "admin" : "mecanico";

                    if (nombre.isEmpty() || login.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (usuarioExistente == null) {
                        // Crear nuevo
                        Usuario nuevo = new Usuario(null, nombre, "", login, pass, "", rol, "activo");
                        db.collection("usuarios").add(nuevo)
                                .addOnSuccessListener(ref -> Toast.makeText(this, "Usuario creado", Toast.LENGTH_SHORT).show());
                    } else {
                        // Actualizar
                        db.collection("usuarios").document(usuarioExistente.getId())
                                .update("nombre", nombre, "usuario", login, "password", pass, "rol", rol)
                                .addOnSuccessListener(ref -> Toast.makeText(this, "Usuario actualizado", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarOpcionesTrabajador(Usuario u, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("✏️ Editar");
        popup.getMenu().add(u.getEstado().equals("activo") ? "🚫 Suspender" : "✅ Reactivar");
        popup.getMenu().add("🗑️ Eliminar");

        popup.setOnMenuItemClickListener(item -> {
            String op = item.getTitle().toString();
            if (op.contains("Editar")) {
                mostrarDialogoUsuario(u);
            } else if (op.contains("Suspender") || op.contains("Reactivar")) {
                String nuevoEstado = u.getEstado().equals("activo") ? "suspendido" : "activo";
                db.collection("usuarios").document(u.getId()).update("estado", nuevoEstado);
            } else if (op.contains("Eliminar")) {
                confirmarEliminacion(u);
            }
            return true;
        });
        popup.show();
    }

    private void confirmarEliminacion(Usuario u) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Trabajador")
                .setMessage("¿Deseas eliminar a " + u.getNombre() + "? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    db.collection("usuarios").document(u.getId()).delete()
                            .addOnSuccessListener(v -> Toast.makeText(this, "Eliminado correctamente", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
