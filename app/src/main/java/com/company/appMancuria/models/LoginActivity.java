package com.company.appMancuria.models;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.company.appMancuria.MainActivity;
import com.company.appMancuria.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText etUsuario, etPassword;
    private MaterialButton btnIngresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Status bar roja
        getWindow().setStatusBarColor(Color.parseColor("#db2d2c"));

        // Referencias a los spacers para insets
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        View navigationBarSpacer = findViewById(R.id.navigationBarSpacer);
        View root = findViewById(R.id.rootLogin);

        // Gestionar insets (Status Bar y Navigation Bar)
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            // Ajustar altura del spacer superior (Status Bar)
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            statusBarSpacer.getLayoutParams().height = statusBarHeight;
            statusBarSpacer.requestLayout();

            // Ajustar altura del spacer inferior (Botones de navegación)
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            navigationBarSpacer.getLayoutParams().height = navBarHeight;
            navigationBarSpacer.requestLayout();

            return insets;
        });

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Views
        etUsuario   = findViewById(R.id.etUserLogin);
        etPassword  = findViewById(R.id.etPasswordLogin);
        btnIngresar = findViewById(R.id.btnLogin);

        btnIngresar.setOnClickListener(v -> intentarLoginInterno());

        // Animación de entrada
        findViewById(android.R.id.content)
                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_in));
    }

    private void intentarLoginInterno() {
        String userStr = etUsuario.getText().toString().trim();
        String passStr = etPassword.getText().toString().trim();

        if (userStr.isEmpty() || passStr.isEmpty()) {
            Toast.makeText(this, "Ingresa tus credenciales", Toast.LENGTH_SHORT).show();
            return;
        }

        btnIngresar.setEnabled(false);
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ejecutarConsultaLogin(userStr, passStr);
                } else {
                    btnIngresar.setEnabled(true);
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ejecutarConsultaLogin(userStr, passStr);
        }
    }

    private void ejecutarConsultaLogin(String userStr, String passStr) {
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
                                Toast.makeText(this, "Cuenta suspendida", Toast.LENGTH_LONG).show();
                            } else {
                                guardarSesionLocal(u);
                            }
                            return;
                        }
                    } else {
                        Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarSesionLocal(Usuario u) {
        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("userId", u.getId())
                .putString("userNombre", u.getNombre())
                .putString("userRol", u.getRol())
                .apply();
        irAlMain();
    }

    private void irAlMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
