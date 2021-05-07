package com.example.das;

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
import android.widget.TabHost;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private String id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Obtener la instancia de autenticación de Firebase
        //FirebaseAuth.getInstance ().signOut ();
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user == null) {
//            // En caso de no haber un usuario loggeado se abre la actividad de login
//            Intent i=new Intent(this, LoginActivity.class);
//            startActivity(i);
//        }
//        else{
//
//            //Se comprueba si el usuario ha completado el proceso de registro
//            id=user.getUid();
//            usuarioRegistrado(id);
//
//        }


        //Se comprueba si hay un usuario logeado. En caso de no haberlo se abre la actividad de login
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        id=preferencias.getString("id",null);

        if(id==null){
            // En caso de no haber un usuario loggeado se abre la actividad de login
            Intent i=new Intent(this, LoginActivity.class);
            startActivity(i);
        }
        else{
            //Se comprueba si el usuario ha completado el proceso de registro
            usuarioRegistrado(id);
        }

        // initiating the tabhost
        TabHost tabhost = (TabHost) findViewById(R.id.tabhost);

        // setting up the tab host
        tabhost.setup();

        // Code for adding Tab 1 to the tabhost
        TabHost.TabSpec spec = tabhost.newTabSpec("Chats");
        spec.setContent(R.id.tab1);

        // setting the name of the tab 1 as "Tab One"
        spec.setIndicator("Chats");

        // adding the tab to tabhost
        tabhost.addTab(spec);

        // Code for adding Tab 2 to the tabhost
        spec = tabhost.newTabSpec("Mapa");
        spec.setContent(R.id.tab2);

        // setting the name of the tab 1 as "Tab Two"
        spec.setIndicator("Mapa");
        tabhost.addTab(spec);

        // Code for adding Tab 3 to the tabhost
        spec = tabhost.newTabSpec("Perfil");
        spec.setContent(R.id.tab3);
        spec.setIndicator("Perfil");
        tabhost.addTab(spec);

    }

    //Comprueba si el usuario ha sido registrado
    public void usuarioRegistrado(String id){

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
                                Intent i=new Intent(MainActivity.this, RegisterActivity.class);
                                i.putExtra("id",id);
                                startActivity(i);
                                finish();

                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("existeUsuario", ExistingWorkPolicy.REPLACE, requesContrasena);



    }
}