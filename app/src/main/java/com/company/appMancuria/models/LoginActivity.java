package com.company.appMancuria.models;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.company.appMancuria.MainActivity;
import com.company.appMancuria.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText etUsuario, etPassword;
    private MaterialButton btnIngresar;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        if (prefs.getString("userId", null) != null && mAuth.getCurrentUser() != null) {
            irAlMain();
            return;
        }

        setContentView(R.layout.activity_login);
        getWindow().setStatusBarColor(Color.parseColor("#db2d2c"));

        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        View navigationBarSpacer = findViewById(R.id.navigationBarSpacer);
        View root = findViewById(R.id.rootLogin);

        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                if (statusBarSpacer != null) {
                    statusBarSpacer.getLayoutParams().height = statusBarHeight;
                    statusBarSpacer.requestLayout();
                }

                int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                if (navigationBarSpacer != null) {
                    navigationBarSpacer.getLayoutParams().height = navBarHeight;
                    navigationBarSpacer.requestLayout();
                }
                return insets;
            });
        }

        etUsuario = findViewById(R.id.etUserLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        btnIngresar = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        if (btnIngresar != null) {
            btnIngresar.setOnClickListener(v -> intentarLoginInterno());
        }
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> enviarRecuperacionPassword());
        }

        findViewById(android.R.id.content)
                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_in));
    }

    private void intentarLoginInterno() {
        String email = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa tus credenciales", Toast.LENGTH_SHORT).show();
            return;
        }

        btnIngresar.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> cargarPerfilUsuario())
                .addOnFailureListener(e -> {
                    btnIngresar.setEnabled(true);
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                });
    }

    private void enviarRecuperacionPassword() {
        String email = etUsuario.getText().toString().trim();
        if (email.isEmpty()) {
            etUsuario.setError("Ingresa tu correo");
            etUsuario.requestFocus();
            Toast.makeText(this, "Escribe tu correo para recuperar la contraseña", Toast.LENGTH_LONG).show();
            return;
        }

        tvForgotPassword.setEnabled(false);
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    tvForgotPassword.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Te enviamos un correo para restablecer tu contraseña. Revisa también spam o correo no deseado.",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e -> {
                    tvForgotPassword.setEnabled(true);
                    Toast.makeText(this, "No se pudo enviar el correo de recuperación", Toast.LENGTH_LONG).show();
                });
    }

    private void cargarPerfilUsuario() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            btnIngresar.setEnabled(true);
            Toast.makeText(this, "No se pudo validar la sesion", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("usuarios").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    btnIngresar.setEnabled(true);
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        cerrarSesionSinPerfil();
                        return;
                    }

                    Usuario u = task.getResult().toObject(Usuario.class);
                    if (u == null) {
                        cerrarSesionSinPerfil();
                        return;
                    }

                    u.setId(task.getResult().getId());
                    if ("suspendido".equals(u.getEstado())) {
                        Toast.makeText(this, "Cuenta suspendida", Toast.LENGTH_LONG).show();
                        limpiarSesion();
                        return;
                    }

                    guardarSesionLocal(u);
                });
    }

    private void cerrarSesionSinPerfil() {
        limpiarSesion();
        Toast.makeText(this, "Usuario sin perfil autorizado", Toast.LENGTH_LONG).show();
    }

    private void limpiarSesion() {
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE).edit().clear().apply();
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
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
