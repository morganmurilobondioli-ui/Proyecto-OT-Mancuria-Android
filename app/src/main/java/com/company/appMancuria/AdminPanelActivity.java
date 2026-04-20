package com.company.appMancuria;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
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
import com.company.appMancuria.utils.CodigosManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminPanelActivity extends AppCompatActivity {

    private static final String TAG = "AdminPanel";
    private FirebaseFirestore db;
    private TextView tvCodigoActual;
    private ImageButton btnToggleVisibilidad;
    private RecyclerView rvTrabajadores;
    private TrabajadorAdapter adapter;
    private List<Usuario> listaTrabajadores = new ArrayList<>();
    
    private String codigoReal = "";
    private boolean isCodigoVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        setupToolbar();
        tvCodigoActual = findViewById(R.id.tvCodigoActual);
        btnToggleVisibilidad = findViewById(R.id.btnToggleVisibilidad);
        rvTrabajadores = findViewById(R.id.rvTrabajadores);

        findViewById(R.id.btnRegenerarCodigo).setOnClickListener(v -> confirmarRegeneracionCodigo());
        findViewById(R.id.btnCopiarCodigo).setOnClickListener(v -> copiarCodigoAlPortapapeles());
        btnToggleVisibilidad.setOnClickListener(v -> toggleVisibilidadCodigo());

        setupRecyclerView();
        consultarCodigoActual();
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

    private void consultarCodigoActual() {
        db.collection("configuracion").document("codigo_acceso")
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error leyendo código: " + error.getMessage());
                        Toast.makeText(this, "Permiso denegado en Firestore", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        codigoReal = doc.getString("codigo");
                        Log.d(TAG, "Código cargado desde Firestore: " + codigoReal);
                        actualizarTextoCodigo();
                    } else {
                        Log.w(TAG, "El documento 'codigo_acceso' no existe");
                    }
                });
    }

    private void toggleVisibilidadCodigo() {
        isCodigoVisible = !isCodigoVisible;
        actualizarTextoCodigo();
        // Cambiar icono del ojo (cerrado/abierto)
        btnToggleVisibilidad.setImageResource(isCodigoVisible ? 
                android.R.drawable.ic_menu_close_clear_cancel : // X o algo que represente ocultar
                android.R.drawable.ic_menu_view); // Ojo
    }

    private void actualizarTextoCodigo() {
        if (isCodigoVisible) {
            tvCodigoActual.setText(codigoReal);
        } else {
            tvCodigoActual.setText("••••••");
        }
    }

    private void copiarCodigoAlPortapapeles() {
        if (codigoReal == null || codigoReal.isEmpty() || codigoReal.contains("-")) {
            Toast.makeText(this, "Código no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Código Mancuria", codigoReal);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    private void confirmarRegeneracionCodigo() {
        new AlertDialog.Builder(this)
                .setTitle("Regenerar Código")
                .setMessage("¿Estás seguro de cambiar el código de acceso? Los mecánicos deberán ingresar el nuevo código.")
                .setPositiveButton("Sí, cambiar", (d, w) -> {
                    String nuevoCodigo = CodigosManager.generarCodigoAleatorio();
                    db.collection("configuracion").document("codigo_acceso")
                            .update("codigo", nuevoCodigo)
                            .addOnSuccessListener(v -> Toast.makeText(this, "Código actualizado", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error actualizando: " + e.getMessage());
                                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void consultarTrabajadores() {
        db.collection("usuarios").addSnapshotListener((snap, err) -> {
            if (err != null || snap == null) return;
            listaTrabajadores.clear();
            for (QueryDocumentSnapshot doc : snap) {
                Usuario u = doc.toObject(Usuario.class);
                u.setId(doc.getId());
                if (u.getEstado() == null) u.setEstado("activo");
                listaTrabajadores.add(u);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void mostrarOpcionesTrabajador(Usuario u, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Cambiar Rol (" + (u.getRol().equals("admin") ? "a Mecánico" : "a Admin") + ")");
        popup.getMenu().add(u.getEstado().equals("activo") ? "Suspender" : "Reactivar");
        popup.getMenu().add("Eliminar");

        popup.setOnMenuItemClickListener(item -> {
            String op = item.getTitle().toString();
            if (op.contains("Cambiar Rol")) {
                String nuevoRol = u.getRol().equals("admin") ? "mecanico" : "admin";
                db.collection("usuarios").document(u.getId()).update("rol", nuevoRol);
            } else if (op.equals("Suspender") || op.equals("Reactivar")) {
                String nuevoEstado = u.getEstado().equals("activo") ? "suspendido" : "activo";
                db.collection("usuarios").document(u.getId()).update("estado", nuevoEstado);
            } else if (op.equals("Eliminar")) {
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
