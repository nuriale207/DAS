package com.example.das;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfoUserActivity extends AppCompatActivity {
    private String idUser;

    private String genero;
    private String edad;
    private String descripcion;
    private String nombre;
    private String intereses;
    private String id_FCM;

    private EditText viewGenero;
    private TextView viewNombreEdad;
    private EditText viewDescripcion;
    private EditText viewIntereses;

    private ImageView imagen;

    private Button botonVolver;
    private Button botonEnviarMensaje;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_user);


        //Se obtiene el id de los extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            idUser = extras.getString("id");

        }

        //Se cargan los elementos del layout

        viewGenero=findViewById(R.id.textViewGenero);
        viewNombreEdad=findViewById(R.id.textViewNombreEdad);
        viewDescripcion=findViewById(R.id.textViewDescripcion);
        viewIntereses=findViewById(R.id.textViewIntereses);

        botonVolver=findViewById(R.id.buttonVolver);
        botonEnviarMensaje=findViewById(R.id.buttonEnviarMensaje);

        imagen=findViewById(R.id.imageView3);

        cargarPerfil();
        cargarIntereses();
        cargarImagen();

        botonVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        botonEnviarMensaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Llamar a la actividad de chat con este usuario
            }
        });



    }
    private void cargarIntereses() {
        Data datos = new Data.Builder()
                    .putString("fichero", "DAS_users.php")
                    .putString("parametros", "funcion=obtenerInteresesUsuario&id=" + idUser)
                    .build();



        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosIntereses"+idUser).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP",resultado);
                            JSONArray jsonArray = null;
                            intereses ="";
                            try {
                                jsonArray = new JSONArray(resultado);
                                for(int x = 0; x < jsonArray.length(); x++) {
                                    JSONObject elemento = jsonArray.getJSONObject(x);

                                        if (!String.valueOf(elemento).contains("null")){
                                            intereses = intereses +"#"+elemento.getString("interes")+", ";

                                        }
                                    }
                                    if(intereses.length()>=2){
                                        intereses = intereses.substring(0, intereses.length()-2);

                                    }
                                    viewIntereses.setText(intereses);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }



                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(this).enqueueUniqueWork("getDatosIntereses"+idUser, ExistingWorkPolicy.REPLACE, requesContrasena);
    }
    private void cargarImagen() {
        //Metodo que carga la imagen de Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child("images/" + idUser + ".jpg");
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getApplicationContext()).load(uri).into(imagen);
            }
        });
    }

    private void cargarPerfil() {

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=datosUsuario&id=" + idUser)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosUsuario"+idUser).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);
                            try {

                                JSONObject jsonObject = new JSONObject(resultado);
                                nombre = jsonObject.getString("nombre");
                                edad = jsonObject.getString("edad");
                                genero = jsonObject.getString("genero");
                                descripcion = jsonObject.getString("descripcion");
                                id_FCM=jsonObject.getString("id_FCM");

                                viewNombreEdad.setText(nombre.toUpperCase()+", "+edad);
                                viewGenero.setText(genero);
                                viewDescripcion.setText(descripcion);
                                Log.i("MYAPP",id_FCM);



                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(this).enqueueUniqueWork("getDatosUsuario"+idUser, ExistingWorkPolicy.REPLACE, requesContrasena);
    }



}