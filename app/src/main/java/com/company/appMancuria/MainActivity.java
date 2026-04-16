package com.company.appMancuria;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private OrdenAdapter adapter;
    private List<OrdenTrabajo> listaCompleta = new ArrayList<>();
    private String userRol = "mecanico";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Aplicar insets para evitar que choque con la barra de estado y de navegación
        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        setupTopBar();
        setupRecyclerView();
        setupBuscador();
        obtenerRolUsuario();
        consultarOrdenes();

        // Botón Nueva Orden
        findViewById(R.id.btnNuevaOrden).setOnClickListener(v ->
                startActivity(new Intent(this, NuevaOrdenActivity.class)));

        // Botón Clientes
        ((MaterialButton) findViewById(R.id.btnClientes)).setOnClickListener(v ->
                startActivity(new Intent(this, ClientesActivity.class)));
    }

    private void setupTopBar() {
        ImageView ivPhoto = findViewById(R.id.ivUserPhoto);
        TextView tvName   = findViewById(R.id.tvUserName);
        ImageButton btnLogout = findViewById(R.id.btnLogout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round).into(ivPhoto);
            }
        }

        btnLogout.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas salir de Mancuria?")
                .setPositiveButton("Salir", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Cancelar", null).show()
        );
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String r = doc.getString("rol");
                        userRol = r != null ? r : "mecanico";
                    }
                });
    }
}
