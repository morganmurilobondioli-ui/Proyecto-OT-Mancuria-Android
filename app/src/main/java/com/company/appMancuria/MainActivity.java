package com.company.appMancuria;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.company.appMancuria.adapters.OrdenAdapter;
import com.company.appMancuria.models.LoginActivity;
import com.company.appMancuria.models.OrdenTrabajo;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private OrdenAdapter adapter;
    private List<OrdenTrabajo> listaCompleta = new ArrayList<>();
    private String userRol = "mecanico";
    private ImageButton btnAdminPanel;
    private String currentUserId;
    private ImageView ivUserPhoto;
    private TextView tvUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Recupera el usuario cacheado en el telefono despues de un login exitoso.
        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);

        // Doble validacion de sesion: debe existir cache local y tambien usuario activo en FirebaseAuth.
        if (currentUserId == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            prefs.edit().clear().apply();
            irALogin();
            return;
        }

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        btnAdminPanel = findViewById(R.id.btnAdminPanel);

        setupTopBar();
        setupRecyclerView();
        setupBuscador();
        obtenerRolUsuario();

        // Botón Nueva Orden
        findViewById(R.id.btnNuevaOrden).setOnClickListener(v ->
                startActivity(new Intent(this, NuevaOrdenActivity.class)));

        // Botón Clientes
        ((MaterialButton) findViewById(R.id.btnClientes)).setOnClickListener(v ->
                startActivity(new Intent(this, ClientesActivity.class)));

        // Botón Admin Panel
        btnAdminPanel.setOnClickListener(this::mostrarMenuUsuario);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvUserName == null || ivUserPhoto == null) return;

        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        tvUserName.setText(prefs.getString("userNombre", "Usuario"));
        cargarFotoUsuario(prefs.getString("userFotoUrl", ""));
    }

    private void setupTopBar() {
        ivUserPhoto = findViewById(R.id.ivUserPhoto);
        tvUserName = findViewById(R.id.tvUserName);
        ImageButton btnLogout = findViewById(R.id.btnLogout);

        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        String nombre = prefs.getString("userNombre", "Usuario");
        String fotoUrl = prefs.getString("userFotoUrl", "");
        tvUserName.setText(nombre);
        
        // La foto ahora es opcional o estática si no viene de Google
        cargarFotoUsuario(fotoUrl);
        ivUserPhoto.setOnClickListener(this::mostrarMenuUsuario);

        btnLogout.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas salir de Mancuria?")
                .setPositiveButton("Salir", (d, w) -> {
                    cerrarSesion();
                })
                .setNegativeButton("Cancelar", null).show()
        );
    }

    private void cargarFotoUsuario(String fotoUrl) {
        Object source = (fotoUrl == null || fotoUrl.isEmpty()) ? R.mipmap.ic_launcher_round : fotoUrl;
        Glide.with(this)
                .load(source)
                .circleCrop()
                .placeholder(R.mipmap.ic_launcher_round)
                .into(ivUserPhoto);
    }

    private void mostrarMenuUsuario(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Mi perfil");
        if ("admin".equals(userRol)) {
            popup.getMenu().add("Administracion");
        }
        popup.getMenu().add("Cerrar sesion");

        popup.setOnMenuItemClickListener(item -> {
            String opcion = item.getTitle().toString();
            if (opcion.equals("Mi perfil")) {
                startActivity(new Intent(this, PerfilActivity.class));
            } else if (opcion.equals("Administracion")) {
                startActivity(new Intent(this, AdminPanelActivity.class));
            } else if (opcion.equals("Cerrar sesion")) {
                confirmarCierreSesion();
            }
            return true;
        });
        popup.show();
    }

    private void confirmarCierreSesion() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesion")
                .setMessage("Deseas salir de Mancuria?")
                .setPositiveButton("Salir", (d, w) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cerrarSesion() {
        // Limpiar SharedPreferences
        getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        
        // Limpiar Firebase Auth
        FirebaseAuth.getInstance().signOut();
        
        // Google Sign Out por si acaso sigue vinculado
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        
        irALogin();
    }

    private void irALogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvOrdenes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(false);
        adapter = new OrdenAdapter(listaCompleta, this::abrirDetalleOrden);
        rv.setAdapter(adapter);
    }

    private void abrirDetalleOrden(OrdenTrabajo orden) {
        Intent i = new Intent(this, DetalleOrdenActivity.class);
        i.putExtra("ordenId", orden.getId());
        startActivity(i);
    }

    private void setupBuscador() {
        TextInputEditText et = findViewById(R.id.etBuscador);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) { filtrar(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filtrar(String texto) {
        List<OrdenTrabajo> f = new ArrayList<>();
        String t = texto.toLowerCase();
        for (OrdenTrabajo o : listaCompleta) {
            if (o.getPlaca().toLowerCase().contains(t)
                    || o.getClienteNombre().toLowerCase().contains(t))
                f.add(o);
        }
        adapter.filtrar(f);
    }

    private void consultarOrdenes() {
        db.collection("ordenes_trabajo")
                .orderBy("fechaIngreso", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    listaCompleta.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        OrdenTrabajo o = doc.toObject(OrdenTrabajo.class);
                        o.setId(doc.getId());
                        listaCompleta.add(o);
                    }
                    adapter.filtrar(new ArrayList<>(listaCompleta));
                });
    }

    private void obtenerRolUsuario() {
        // El rol no se confia solo desde SharedPreferences: se vuelve a leer desde Firestore.
        db.collection("usuarios").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userRol = doc.getString("rol");
                        if (userRol == null) userRol = "mecanico";

                        String estado = doc.getString("estado");
                        if ("suspendido".equals(estado)) {
                            // Si el admin suspendio la cuenta mientras estaba logueada,
                            // la app fuerza salida al volver a consultar el perfil.
                            mostrarBloqueoSuspension();
                            return;
                        }
                        
                        String nombre = doc.getString("nombre");
                        String fotoUrl = doc.getString("fotoUrl");
                        if (nombre == null || nombre.isEmpty()) nombre = "Usuario";
                        if (fotoUrl == null) fotoUrl = "";

                        getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE).edit()
                                // Cache local para pintar rapido la barra superior y mostrar menu correcto.
                                .putString("userNombre", nombre)
                                .putString("userFotoUrl", fotoUrl)
                                .putString("userRol", userRol)
                                .apply();
                        tvUserName.setText(nombre);
                        cargarFotoUsuario(fotoUrl);
                        
                        consultarOrdenes();
                    }
                });
    }

    private void mostrarBloqueoSuspension() {
        new AlertDialog.Builder(this)
                .setTitle("Cuenta Suspendida")
                .setMessage("Tu acceso ha sido revocado por el administrador.")
                .setCancelable(false)
                .setPositiveButton("Salir", (d, w) -> {
                    cerrarSesion();
                }).show();
    }
}
