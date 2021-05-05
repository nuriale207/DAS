package com.example.das;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private Button botonIniciarSesion;
    private Button botonRegistroGoogle;
    private EditText inputEmail;
    private EditText inputPassword;
    private String idUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Cargar los elementos de la ventana

        botonIniciarSesion = findViewById(R.id.botonIniciarSesion);
        botonRegistroGoogle = findViewById(R.id.botonRegistrarse);
        inputEmail = findViewById(R.id.nombreUsuarioEdit);
        inputPassword = findViewById(R.id.contraseñaEdit);


        //Obtener la instancia de autenticación de Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        botonIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
               //Se comprueba si el usuario existe, en caso de que exista se inicia sesión


                //if not created create user
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (!task.isSuccessful()) {

                                    if(task.getException().getMessage().contains("email address is already in use")){
                                        //Hacer login con ese email y password

                                        firebaseAuth.signInWithEmailAndPassword(email, password)
                                                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                                        if (task.isSuccessful()) {
                                                            // Inicio de sesión correcto
                                                            Log.i("MY", "signInWithEmail:success");
                                                            Toast.makeText(LoginActivity.this, "Signing in" ,
                                                                    Toast.LENGTH_SHORT).show();
                                                            FirebaseUser user =  firebaseAuth.getCurrentUser();
                                                            loginUsuario(user.getUid());
                                                            Log.i("MY", "firebase: "+user.getUid(), task.getException());



                                                        } else {
                                                            // Error al iniciar sesión
                                                            Log.i("MY", "signInWithEmail:failure", task.getException());
                                                            Toast.makeText(LoginActivity.this, "Authentication failed."+ task.getException().getMessage(),
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });

                                    }
                                    else{
                                        Toast.makeText(LoginActivity.this, "Authentication failed." + task.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();

                                    }
                                } else {
                                    FirebaseUser user =  firebaseAuth.getCurrentUser();
                                    loginUsuario(user.getUid());
                                    Log.i("MY", "firebase: "+user.getUid(), task.getException());
                                    Toast.makeText(LoginActivity.this, "User created successfully.", Toast.LENGTH_SHORT).show();

                                }
                            }
                        });
            }
        });

        botonRegistroGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Configure Google Sign In
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(v.getContext(), gso);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, 1);


            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 1) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.i("MY", "firebaseAuthWithGoogle:" + account.getId());
                loginUsuario(account.getId());


                //firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.i("MY", "Google sign in failed", e);
            }
        }
    }

    public void loginUsuario(String id){

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=datosUsuario&id=" +id)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("existeUsuario").build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);
                            if (resultado.contains("error")|| resultado.contains("null")){
                                Intent i=new Intent(LoginActivity.this, RegisterActivity.class);
                                i.putExtra("id",id);
                                startActivity(i);

                            }
                            else{
                                startActivity(new Intent(LoginActivity.this, ChatsActivity.class));

                                SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                preferencias.edit().putString("id",id).apply();
                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("existeUsuario", ExistingWorkPolicy.REPLACE, requesContrasena);



    }





}