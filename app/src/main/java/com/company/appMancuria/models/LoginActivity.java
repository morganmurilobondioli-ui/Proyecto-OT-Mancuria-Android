package com.company.appMancuria.models;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.company.appMancuria.MainActivity;
import com.company.appMancuria.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // API moderna (reemplaza el deprecated onActivityResult)
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            GoogleSignInAccount account = GoogleSignIn
                                    .getSignedInAccountFromIntent(result.getData())
                                    .getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Toast.makeText(this, "Error Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        MaterialButton btn = findViewById(R.id.btnGoogleLogin);

        // Animaciones táctiles (efecto press / release)
        Animation press   = AnimationUtils.loadAnimation(this, R.anim.button_press);
        Animation release = AnimationUtils.loadAnimation(this, R.anim.button_release);

        btn.setOnClickListener(v -> googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent()));

        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                v.startAnimation(press);
            else if (event.getAction() == MotionEvent.ACTION_UP ||
                     event.getAction() == MotionEvent.ACTION_CANCEL)
                v.startAnimation(release);
            return false;
        });

        // Animación de entrada de la pantalla
        View root = findViewById(android.R.id.content);
        root.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_in));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Si ya hay sesión activa, saltar directo al main
        if (mAuth.getCurrentUser() != null) irAlMain();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) verificarUsuarioEnFirestore(mAuth.getCurrentUser());
                    else Toast.makeText(this, "Error al autenticar con Firebase", Toast.LENGTH_SHORT).show();
                });
    }

    private void verificarUsuarioEnFirestore(FirebaseUser user) {
        if (user == null) return;
        DocumentReference docRef = db.collection("usuarios").document(user.getUid());
        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                irAlMain();
            } else {
                // Primer ingreso: crear perfil con rol "mecanico" por defecto
                Map<String, Object> nuevo = new HashMap<>();
                nuevo.put("nombre", user.getDisplayName());
                nuevo.put("correo", user.getEmail());
                nuevo.put("fotoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                nuevo.put("rol", "mecanico");
                docRef.set(nuevo).addOnSuccessListener(v -> irAlMain());
            }
        });
    }

    private void irAlMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
