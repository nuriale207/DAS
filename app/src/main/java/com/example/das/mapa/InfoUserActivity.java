package com.example.das.mapa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.das.ReproductorSonido;
import com.example.das.bd.BDLocal;
import com.example.das.bd.ConexionBDWorker;
import com.example.das.R;
import com.example.das.chats.ChatActivity;
import com.example.das.chats.GestorChats;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;


public class InfoUserActivity extends AppCompatActivity {
    private String idUser;
    private String genero;
    private String edad;
    private String descripcion;
    private String nombre;
    private String intereses;
    private String id_FCM;
    private TextView viewGenero;
    private TextView viewNombreEdad;
    private TextView viewDescripcion;
    private TextView viewIntereses;
    private ImageView imagen;
    private Button botonVolver;
    private Button botonEnviarMensaje;
    Boolean chat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_user);


        //Se obtiene el id de los extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            idUser = extras.getString("id");
            chat=extras.getBoolean("chat");


        }

        //Se cargan los elementos del layout
        viewGenero=findViewById(R.id.textViewGenero);
        viewNombreEdad=findViewById(R.id.textViewNombreEdad);
        viewDescripcion=findViewById(R.id.textViewDescripcion);
        viewIntereses=findViewById(R.id.textViewIntereses);

        botonVolver=findViewById(R.id.buttonVolver);
        botonEnviarMensaje=findViewById(R.id.buttonEnviarMensaje);

        imagen=findViewById(R.id.imageView3);

        //Se llama a los métodos para que obtengan todos los datos del usuarioq que se muestra
        cargarPerfil();
        cargarIntereses();
        cargarImagen();

        //Al volver, si el perfil se ha abierto desde el mapa se fuerza la recarga de este para que incluya posibles actualizaciones
        botonVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!chat){
                    Intent i=new Intent(InfoUserActivity.this, MainActivity.class);
                    i.putExtra("tab",1);
                    finish();
                    startActivity(i);
                }
                else {
                   finish();
                }


            }
        });

        //Al pulsa sobre el botón enviar mensaje se abre una actividad de chat con el usuario
        botonEnviarMensaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(InfoUserActivity.this, ChatActivity.class);
                i.putExtra("id", idUser);
                i.putExtra("nombre",nombre);
                i.putExtra("token", id_FCM);

                //https://stackoverflow.com/questions/9042932/getting-image-from-imageview
                Drawable drawable = imagen.getDrawable();
                BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
                Bitmap bitmap = bitmapDrawable .getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageInByte = stream.toByteArray();

                i.putExtra("imagen", imageInByte);

                //Se almacena el usuario en la BD local
                BDLocal gestorDB = new BDLocal (InfoUserActivity.this, "DAS", null, 1);
                SQLiteDatabase bd = gestorDB.getWritableDatabase();

                String[] campos = new String[] {"Id"};
                String[] argumentos = new String[] {idUser};
                Cursor cu = bd.query("Usuarios",campos,"Id=?",argumentos,null,null,null);
                if (cu.getCount() == 0){
                    ContentValues nuevo = new ContentValues();
                    nuevo.put("Id", idUser);
                    nuevo.put("Nombre", nombre);
                    nuevo.put("Token", id_FCM);
                    nuevo.put("Imagen", imageInByte);
                    bd.insert("Usuarios", null, nuevo);

                    //ChatsFragment chats = (ChatsFragment)getSupportFragmentManager().getFragment(null, "fragmentChats");
                    //chats.rellenarListas();

                    GestorChats.getGestorListas().actualizarChats();
                }
                startActivity(i);
                finish();
            }
        });
        //Método que detecta el scroll y hace que la imagen se oscurezca
        ScrollView scroll = findViewById(R.id.scroll);
        scroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                float maxScroll = 150;
                int scroll = (int) Math.min(maxScroll, scrollY);
                float relativo = (float) (scroll/maxScroll);
                imagen.setAlpha(1-relativo*2/3);
            }
        });

    }


    private void cargarIntereses() {
        //Método que carga los intereses del usuario
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
        WorkManager.getInstance(this).enqueueUniqueWork("getDatosIntereses"+idUser, ExistingWorkPolicy.REPLACE, requesContrasena);
    }
    //Método que carga la imagen del usuario desde Firebase
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
        //Método que obtiene los datos del usuario de la BD remota
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
                                if(descripcion.equals("null")){
                                    descripcion="";
                                }
                                viewDescripcion.setText(descripcion);
                                Log.i("MYAPP",id_FCM);



                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        WorkManager.getInstance(this).enqueueUniqueWork("getDatosUsuario"+idUser, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    @Override
    public void onBackPressed() {
        //Al pulsar atrás en el teléfono se fuerza la recarga de la activity main para que incluya los posibles cambios
        ReproductorSonido.getReproductorSonido().reproducirSonido(this, R.raw.s_atras);
        super.onBackPressed();
        if(!chat){
            Intent i=new Intent(InfoUserActivity.this, MainActivity.class);
            i.putExtra("tab",1);
            startActivity(i);
            finish();
        }
        else {
            finish();
        }

    }



}