package com.company.appMancuria.models;


import android.content.Context;//Permite acceder a cosas del entorno de Android
import android.content.Intent;//Sirve para cambiar de pantalla.
import android.content.SharedPreferences;//Guardar datos pequeños
import android.graphics.Color;
import android.os.Bundle;//Se usa para entregar información cuando se crea la pantalla
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.company.appMancuria.MainActivity;
import com.company.appMancuria.R; //Conectar los recursos xml
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;//Maneja el login real
import com.google.firebase.auth.FirebaseUser;//Representa al usuario actual
import com.google.firebase.firestore.FirebaseFirestore;//Base de datos

//Es una pantalla
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText etUsuario, etPassword;
    private MaterialButton btnIngresar;
    private TextView tvForgotPassword;

    //Oncreate es lo que android llama por primera vez
    //Es el punto de arranque del activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        //Abrimos una pequeña memoria en el telefono
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
        //variables java con los elementos de la pantalla
        etUsuario = findViewById(R.id.etUserLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        btnIngresar = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Click principal del login: toma correo/password y llama a FirebaseAuth.
        if (btnIngresar != null) {
            btnIngresar.setOnClickListener(v -> intentarLoginInterno());
        }

        // Flujo "olvide mi contrasena": Firebase envia un correo oficial de recuperacion.
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> enviarRecuperacionPassword());
        }

        findViewById(android.R.id.content)
                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_in));
    }

    private void intentarLoginInterno() {
        // Se leen los textos escritos por el usuario
        String email = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Primera validacion local: no se consulta Firebase si faltan credenciales.
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa tus credenciales", Toast.LENGTH_SHORT).show();
            return;
        }

        // Se bloquea el boton para evitar dos intentos simultaneos por doble toque.
        btnIngresar.setEnabled(false);

        // FirebaseAuth valida correo y password contra el proyecto Firebase.
        // Si es correcto, aun falta revisar el perfil interno en Firestore.
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> cargarPerfilUsuario())
                .addOnFailureListener(e -> {
                    btnIngresar.setEnabled(true);
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                });
    }

    private void enviarRecuperacionPassword() {
        // Para recuperar password, Firebase necesita saber a que correo enviar el enlace.
        String email = etUsuario.getText().toString().trim();
        if (email.isEmpty()) {
            etUsuario.setError("Ingresa tu correo");
            etUsuario.requestFocus();
            Toast.makeText(this, "Escribe tu correo para recuperar la contraseña", Toast.LENGTH_LONG).show();
            return;
        }

        tvForgotPassword.setEnabled(false); //Desactive el botón temporalmente


        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    tvForgotPassword.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Te enviamos un correo al Spam para restablecer tu contraseña.",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e -> {
                    tvForgotPassword.setEnabled(true);
                    Toast.makeText(this, "No se pudo enviar el correo de recuperación", Toast.LENGTH_LONG).show();
                });
    }

    private void cargarPerfilUsuario() {
        // getCurrentUser() devuelve el usuario autenticado por FirebaseAuth.
        // Si es null, significa que no hay sesion valida aunque el login haya intentado continuar.
        FirebaseUser currentUser = mAuth.getCurrentUser(); //Sirve para obtener el usuario que ya está autenticado en FirebaseAuth en ese momento.
        if (currentUser == null) {
            btnIngresar.setEnabled(true);
            Toast.makeText(this, "No se pudo validar la sesion", Toast.LENGTH_SHORT).show();
            return;
        }

        // El UID de FirebaseAuth se usa como ID del documento en "usuarios".
        // Asi se une la cuenta autenticada con el perfil interno de la empresa.
        db.collection("usuarios").document(currentUser.getUid()) //busca el documento cuyo ID es el UID del usuario autenticado.
                .get()
                .addOnCompleteListener(task -> { //task contiene el resultado de la lectura en Firestore
                    btnIngresar.setEnabled(true);

                    // Si existe Auth pero no existe documento en Firestore, no es un usuario autorizado.
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        cerrarSesionSinPerfil();
                        return;
                    }

                    // Firestore convierte el documento a un objeto Usuario usando constructor vacio y setters.
                    Usuario u = task.getResult().toObject(Usuario.class);
                    if (u == null) {
                        cerrarSesionSinPerfil();
                        return;
                    }

                    u.setId(task.getResult().getId()); //Guarda el ID del documento dentro del objeto Usuario

                    // El estado "suspendido" bloquea el acceso aunque el correo/password sean correctos.
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
        // Cierra la sesion de FirebaseAuth y borra los datos locales de SharedPreferences.
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE).edit().clear().apply();
    }

    private void guardarSesionLocal(Usuario u) {
        // Se guardan solo datos pequeños para pintar la UI rapidamente.
        // La fuente real de permisos sigue siendo Firestore.
        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("userId", u.getId())
                .putString("userNombre", u.getNombre())
                .putString("userRol", u.getRol())
                .apply();
        irAlMain();
    }

    private void irAlMain() {
        // CLEAR_TASK elimina pantallas anteriores: al entrar no se puede volver al login con "atras".
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
