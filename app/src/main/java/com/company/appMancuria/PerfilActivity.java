package com.company.appMancuria;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private ImageView ivFoto;
    private TextInputEditText etNombre, etCorreo, etPasswordActual, etPassword;
    private MaterialButton btnCambiarFoto, btnGuardar;
    private NestedScrollView scrollPerfil;
    private Uri selectedPhotoUri;
    private String fotoUrlActual = "";

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .placeholder(R.mipmap.ic_launcher_round)
                            .into(ivFoto);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        getWindow().setStatusBarColor(Color.parseColor("#db2d2c"));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        bindViews();
        setupInsets();
        cargarPerfil();
    }

    private void setupInsets() {
        View root = findViewById(R.id.rootPerfil);
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        View navigationBarSpacer = findViewById(R.id.navigationBarSpacer);
        View keyboardContentSpacer = findViewById(R.id.keyboardContentSpacer);

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

            int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            if (keyboardContentSpacer != null) {
                int spacerHeight = keyboardHeight > 0 ? keyboardHeight + 96 : 180;
                keyboardContentSpacer.getLayoutParams().height = spacerHeight;
                keyboardContentSpacer.requestLayout();
            }
            return insets;
        });
    }

    private void bindViews() {
        Toolbar toolbar = findViewById(R.id.toolbarPerfil);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivFoto = findViewById(R.id.ivPerfilFoto);
        scrollPerfil = findViewById(R.id.scrollPerfil);
        etNombre = findViewById(R.id.etPerfilNombre);
        etCorreo = findViewById(R.id.etPerfilCorreo);
        etPasswordActual = findViewById(R.id.etPerfilPasswordActual);
        etPassword = findViewById(R.id.etPerfilPassword);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);

        btnCambiarFoto.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnGuardar.setOnClickListener(v -> guardarCambios());
        etPasswordActual.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) scrollToSaveButton();
        });
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) scrollToSaveButton();
        });
        etPasswordActual.setOnClickListener(v -> scrollToSaveButton());
        etPassword.setOnClickListener(v -> scrollToSaveButton());
    }

    private void scrollToSaveButton() {
        scrollPerfil.postDelayed(this::ensureSaveButtonVisible, 300);
        scrollPerfil.postDelayed(this::ensureSaveButtonVisible, 650);
    }

    private void ensureSaveButtonVisible() {
        Rect visibleFrame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleFrame);

        int[] buttonLocation = new int[2];
        btnGuardar.getLocationOnScreen(buttonLocation);

        int buttonBottom = buttonLocation[1] + btnGuardar.getHeight();
        int requiredScroll = buttonBottom - visibleFrame.bottom + 32;

        if (requiredScroll > 0) {
            scrollPerfil.smoothScrollBy(0, requiredScroll);
        }
    }

    private void cargarPerfil() {
        etCorreo.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");

        db.collection("usuarios").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String nombre = doc.getString("nombre");
                    String fotoUrl = doc.getString("fotoUrl");

                    etNombre.setText(nombre != null ? nombre : "");
                    fotoUrlActual = fotoUrl != null ? fotoUrl : "";

                    Object source = fotoUrlActual.isEmpty() ? R.mipmap.ic_launcher_round : fotoUrlActual;
                    Glide.with(this)
                            .load(source)
                            .circleCrop()
                            .placeholder(R.mipmap.ic_launcher_round)
                            .into(ivFoto);
                });
    }

    private void guardarCambios() {
        String nombre = etNombre.getText().toString().trim();
        String passwordActual = etPasswordActual.getText().toString().trim();
        String nuevaPassword = etPassword.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombre.setError("Obligatorio");
            return;
        }
        if (!nuevaPassword.isEmpty() && nuevaPassword.length() < 6) {
            etPassword.setError("Minimo 6 caracteres");
            return;
        }
        if (!nuevaPassword.isEmpty() && passwordActual.isEmpty()) {
            etPasswordActual.setError("Necesaria para cambiar contraseña");
            return;
        }

        btnGuardar.setEnabled(false);
        if (selectedPhotoUri != null) {
            subirFotoYGuardar(nombre, passwordActual, nuevaPassword);
        } else {
            guardarPerfil(nombre, fotoUrlActual, passwordActual, nuevaPassword);
        }
    }

    private void subirFotoYGuardar(String nombre, String passwordActual, String nuevaPassword) {
        StorageReference ref = storage.getReference()
                .child("usuarios")
                .child(currentUser.getUid())
                .child("perfil.jpg");

        ref.putFile(selectedPhotoUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> guardarPerfil(nombre, uri.toString(), passwordActual, nuevaPassword))
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(this, "No se pudo subir la foto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void guardarPerfil(String nombre, String fotoUrl, String passwordActual, String nuevaPassword) {
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        data.put("fotoUrl", fotoUrl != null ? fotoUrl : "");

        db.collection("usuarios").document(currentUser.getUid())
                .update(data)
                .addOnSuccessListener(v -> {
                    guardarPrefs(nombre, fotoUrl);
                    actualizarPasswordSiCorresponde(passwordActual, nuevaPassword);
                })
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(this, "No se pudo guardar el perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void actualizarPasswordSiCorresponde(String passwordActual, String nuevaPassword) {
        if (nuevaPassword.isEmpty()) {
            btnGuardar.setEnabled(true);
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
            volverAPrincipal();
            return;
        }

        String email = currentUser.getEmail();
        if (email == null || email.isEmpty()) {
            btnGuardar.setEnabled(true);
            Toast.makeText(this, "No se pudo validar el correo del usuario", Toast.LENGTH_LONG).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, passwordActual);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(v -> currentUser.updatePassword(nuevaPassword)
                        .addOnSuccessListener(ok -> {
                            btnGuardar.setEnabled(true);
                            Toast.makeText(this, "Perfil y contraseña actualizados", Toast.LENGTH_SHORT).show();
                            volverAPrincipal();
                        })
                        .addOnFailureListener(e -> {
                            btnGuardar.setEnabled(true);
                            Toast.makeText(this, "No se pudo cambiar la contraseña: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(this, "Contraseña actual incorrecta o sesión no válida", Toast.LENGTH_LONG).show();
                });
    }

    private void volverAPrincipal() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void guardarPrefs(String nombre, String fotoUrl) {
        SharedPreferences prefs = getSharedPreferences("MancuriaPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("userNombre", nombre)
                .putString("userFotoUrl", fotoUrl != null ? fotoUrl : "")
                .apply();
    }
}
