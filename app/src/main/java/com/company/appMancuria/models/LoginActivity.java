package com.company.appMancuria.models;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.company.appMancuria.MainActivity;
import com.company.appMancuria.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText etUsuario, etPassword;
    private MaterialButton btnIngresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        etUsuario  = findViewById(R.id.etUserLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        btnIngresar = findViewById(R.id.btnLogin);

        btnIngresar.setOnClickListener(v -> intentarLoginInterno());

        // Botón Google (Temporal para migración progresiva)
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> {
            Toast.makeText(this, "Acceso con Google deshabilitado temporalmente. Use sus credenciales internas.", Toast.LENGTH_LONG).show();
        });

        // Animación de entrada
        findViewById(android.R.id.content).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_in));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verificar si hay una sesión interna activa
        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId != null) {
            verificarEstadoYEntrar(userId);
        }
    }

    private void intentarLoginInterno() {
        String userStr = etUsuario.getText().toString().trim();
        String passStr = etPassword.getText().toString().trim();

        if (userStr.isEmpty() || passStr.isEmpty()) {
            Toast.makeText(this, "Ingresa usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        btnIngresar.setEnabled(false);

        // Buscar el usuario en la colección "usuarios"
        db.collection("usuarios")
                .whereEqualTo("usuario", userStr)
                .whereEqualTo("password", passStr)
                .get()
                .addOnCompleteListener(task -> {
                    btnIngresar.setEnabled(true);
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Usuario u = document.toObject(Usuario.class);
                            u.setId(document.getId());
                            
                            if ("suspendido".equals(u.getEstado())) {
                                Toast.makeText(this, "Tu cuenta está suspendida", Toast.LENGTH_LONG).show();
                            } else {
                                iniciarSesionLocal(u);
                            }
                            return;
                        }
                    } else {
                        Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void iniciarSesionLocal(Usuario u) {
        // Guardar sesión en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("userId", u.getId())
                .putString("userNombre", u.getNombre())
                .putString("userRol", u.getRol())
                .apply();

        // Para mantener las reglas de Firestore (request.auth != null),
        // iniciamos sesión anónima en Firebase Auth si no lo está.
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(task -> irAlMain());
        } else {
            irAlMain();
        }
    }

    private void verificarEstadoYEntrar(String userId) {
        db.collection("usuarios").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && !"suspendido".equals(doc.getString("estado"))) {
                        irAlMain();
                    } else {
                        // Sesión inválida o suspendida
                        cerrarSesionLocal();
                    }
                });
    }

    private void cerrarSesionLocal() {
        getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE).edit().clear().apply();
    }

    private void irAlMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
