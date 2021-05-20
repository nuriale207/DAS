package com.example.das.registroLogin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.das.bd.ConexionBDWorker;
import com.example.das.mapa.MainActivity;
import com.example.das.R;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class InteresesActivity extends AppCompatActivity {
    private EditText descripcion;
    private EditText intereses;
    private Button botonContinuar;
    private Button botonSaltar;
    private FirebaseAuth firebaseAuth;
    private String id;
    private ArrayList<String> listaSeleccionIntereses;
    private ArrayList<String> listaIntereses = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intereses);

        //Se obtienen los elementos del layout
        descripcion = findViewById(R.id.textoDescripción);
        intereses = findViewById(R.id.textoIntereses);
        botonContinuar = findViewById(R.id.buttonContinuar);
        botonSaltar = findViewById(R.id.buttonSaltar);

        //Se cargan los intereses disponibles de la base de datos
        obtenerIntereses();

        //Se obtiene el id del usuario logeado
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        id = preferencias.getString("id", null);


        //Al hacer click en intereses se abre un dialog para seleccionar los intereses
        intereses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /***
                 * Código obtenido de:https://stackoverflow.com/questions/16389581/android-create-a-popup-that-has-multiple-selection-options
                 * Usuario: https://stackoverflow.com/users/1274911/zbr
                 */
                listaSeleccionIntereses = new ArrayList<String>();
                intereses.setText("");

                ArrayList<Integer> seleccion = new ArrayList<Integer>();
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Intereses");
                String[] opciones = listaIntereses.toArray(new String[listaIntereses.size()]);

                builder.setMultiChoiceItems(opciones, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        //Se mira si es selección o deselección del interés
                        if(!seleccion.contains(i)){
                            seleccion.add(i);

                        }
                        else {
                            seleccion.remove((Integer)i);
                        }                    }
                });
                builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Al aceptar se escriben en el campo intereses los intereses seleccionados
                        listaSeleccionIntereses = new ArrayList<String>();
                        String texto = "";
                        for (int i = 0; i < seleccion.size(); i++) {
                            listaSeleccionIntereses.add(opciones[seleccion.get(i)]);
                            texto = texto + "#" + opciones[seleccion.get(i)] + ", ";
                        }
                        if(texto.length()>2){
                            texto = texto.substring(0, texto.length() - 2);

                        }
                        intereses.setText(texto);

                        dialog.cancel();
                    }


                });
                Dialog dialog = builder.create();
                dialog.show();

            }
        });

        //Al hacer click sobre el boton continuar se actualizan la descripción y los intereses en la BD
        botonContinuar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strDescripcion = descripcion.getText().toString();
                if (strDescripcion.length() > 0 && strDescripcion.length()<280) {
                    guardarDescripcion();
                }
                else if(descripcion.length()>280){
                    Toast toast = Toast.makeText(getApplicationContext(), "La descripción no puede sobrepasar los 280 caracteres", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                    toast.show();
                }

                String strIntereses = intereses.getText().toString();
                if (strIntereses.length() > 0) {
                    guardarIntereses();
                }
                Intent i = new Intent(v.getContext(), MainActivity.class);
                startActivity(i);
                finish();
            }
        });

        //Al hacer click sobre el boton saltar se accede a la pantalla principal de la aplicación
        botonSaltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), MainActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    private void guardarDescripcion() {
        //Método que almacena la descripción en la BD
        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=anadirDescripcion&id=" + id + "&descripcion=" + descripcion.getText().toString())
                .build();

        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("guardarDescripcion").build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);
                            Log.i("MYAPP", resultado);
                            JSONArray jsonArray = null;
                            try {
                                jsonArray = new JSONArray(resultado);
                                for (int x = 0; x < jsonArray.length(); x++) {
                                    JSONObject elemento = jsonArray.getJSONObject(x);
                                    Log.i("MYAPP", String.valueOf(elemento));
                                    listaIntereses.add(elemento.getString("nombre"));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("guardarDescripcion", ExistingWorkPolicy.REPLACE, requesContrasena);


    }

    private void guardarIntereses() {
        //Método que almacena los intereses en la BD
        String intereses = "";
        for (int i = 0; i < listaSeleccionIntereses.size(); i++) {
            intereses = intereses + listaSeleccionIntereses.get(i) + "#";

        }
        intereses = intereses.substring(0, intereses.length() - 1);

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=anadirIntereses&id=" + id + "&intereses=" + intereses)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("guardarIntereses").build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");
                            Log.i("MYAPP", resultado);
                            Log.i("MYAPP", resultado);
                            JSONArray jsonArray = null;
                            try {
                                jsonArray = new JSONArray(resultado);
                                for (int x = 0; x < jsonArray.length(); x++) {
                                    JSONObject elemento = jsonArray.getJSONObject(x);
                                    Log.i("MYAPP", String.valueOf(elemento));
                                    listaIntereses.add(elemento.getString("nombre"));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("guardarIntereses", ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    private void obtenerIntereses() {
        //Método que obtiene todos los posibles intereses de la BD
        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=obtenerIntereses")
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("obtenerIntereses").build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");
                            Log.i("MYAPP", resultado);
                            Log.i("MYAPP", resultado);
                            JSONArray jsonArray = null;
                            try {
                                jsonArray = new JSONArray(resultado);
                                for (int x = 0; x < jsonArray.length(); x++) {
                                    JSONObject elemento = jsonArray.getJSONObject(x);
                                    Log.i("MYAPP", String.valueOf(elemento));


                                    listaIntereses.add(elemento.getString("interes"));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("obtenerIntereses", ExistingWorkPolicy.REPLACE, requesContrasena);


    }
}