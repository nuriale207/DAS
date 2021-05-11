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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

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

        //En caso de haber girado la pantalla se añaden los valores escritos previamente
        if(savedInstanceState!=null){
            inputEmail.setText(savedInstanceState.getString("email"));
            inputPassword.setText(savedInstanceState.getString("contraseña"));
        }
        //Obtener la instancia de autenticación de Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        botonIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
               //Se comprueba si el usuario existe, en caso de que exista se inicia sesión


                //Si no existe un usuario se crea
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
                                    //Se crea un usuario nuevo
                                    FirebaseUser user =  firebaseAuth.getCurrentUser();
                                    loginUsuario(user.getUid());
                                    Log.i("MY", "firebase: "+user.getUid(), task.getException());
                                    Toast.makeText(LoginActivity.this, "User created successfully.", Toast.LENGTH_SHORT).show();

                                }
                            }
                        });
            }
        });

        //En caso de pulsar el botón iniciar sesión con google se lleva a cabo el proceso con Google
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
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("MYAPP", "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("MYAPP", "signInWithCredential:failure", task.getException());
                            //updateUI(null);
                        }
                    }
                });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Resultado de lanzar el intent desde GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 1) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // El inicio de sesión se completó correctamente, autenticación con firebase with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.i("MY", "firebaseAuthWithGoogle:" + account.getId()+ "  :"+account.getIdToken());
                firebaseAuthWithGoogle(account.getIdToken());
                FirebaseUser user = firebaseAuth.getCurrentUser();
                loginUsuario(user.getUid());


                //En caso de error se muestra el error
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
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("existeUsuario3").build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);


                            JSONObject jsonObject = null;
                            try {
                                jsonObject = new JSONObject(resultado);
                                String nombre = jsonObject.getString("nombre");
                                if (nombre.equals("null")){
                                    Intent i=new Intent(LoginActivity.this, RegisterActivity.class);
                                    i.putExtra("id",id);
                                    startActivity(i);

                                }
                                else{
                                    SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    preferencias.edit().putString("id",id).apply();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }



                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("existeUsuario3", ExistingWorkPolicy.REPLACE, requesContrasena);



    }
    //En caso de que la aplicación se detenga se almacenan los nombres escritos hasta el momento
    @Override
    protected void onSaveInstanceState (Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("email",inputEmail.getText().toString());
        savedInstanceState.putString("contraseña",inputPassword.getText().toString());
    }





}