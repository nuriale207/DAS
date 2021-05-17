package com.example.das;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {
    private String miId;
    private String idOtro;
    private String nombreOtro;
    private String tokenOtro;
    private byte[] imagenOtro;
    private AdaptadorMensajes adaptador;
    ArrayList<String> mensajes = new ArrayList<>();
    ArrayList<Boolean> mios = new ArrayList<>();
    private Handler handler;

    private ImageView imagenOtroChat;
    private Boolean mostrarImagen;
    public static boolean running;
    public static String idChat;
    TextView nombreOtroChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle extras = getIntent().getExtras();
        imagenOtroChat = findViewById(R.id.imagenOtroChat);
        nombreOtroChat = findViewById(R.id.nombreOtroChat);
        if (extras != null) {
            idOtro = extras.getString("id");

            nombreOtro = extras.getString("nombre");
            if(nombreOtro==null){
                String mensaje = extras.getString("mensaje");
                anadirUsuarioABDLocal(idOtro,mensaje);
            }
            else{
                tokenOtro = extras.getString("token");
                imagenOtro = extras.getByteArray("imagen");
                //https://stackoverflow.com/questions/13854742/byte-array-of-image-into-imageview
                Bitmap bmp = BitmapFactory.decodeByteArray(imagenOtro, 0, imagenOtro.length);
                imagenOtroChat.setImageBitmap(Bitmap.createScaledBitmap(bmp, 150, 150, false));
                obtenerMensajesChat();
                ListView lista=findViewById(R.id.mensajes);
                adaptador = new AdaptadorMensajes(this, mensajes,mios);
                lista.setAdapter(adaptador);

                lista.setSelection(adaptador.getCount() - 1);

                nombreOtroChat.setText(nombreOtro);

            }
        }

        ImageView botonJuego = findViewById(R.id.botonJuego);
        botonJuego.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ChatActivity.this, JuegoActivity.class);
                startActivity(i);
            }
        });

        ImageButton enviarMensaje = findViewById(R.id.botonEnviarMensaje);
        enviarMensaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarMensaje();
            }
        });

        handler = new Handler();
        Runnable actualizadorChat = new Runnable() {
            @Override
            public void run() {
                if(GestorChats.getGestorListas().comprobarNuevoMensaje()){
                    actualizarListaMensajes();
                }
                handler.postDelayed(this,2000);
            }
        };
        handler.postDelayed(actualizadorChat, 2000);
    }


    private void anadirUsuarioABDLocal(String id_remitente,String mensaje) {
        //Metodo que carga la imagen de Firebase Storage
        //Metodo que carga la imagen de Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child("images/" + id_remitente + ".jpg");
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getApplicationContext()).load(uri).into(imagenOtroChat);
                anadirUsuarioABDLocal2(id_remitente,mensaje);
            }
        });
    }


    private void anadirUsuarioABDLocal2(String id_remitente,String mensaje) {

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=datosUsuario&id=" + id_remitente)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosUsuario"+id_remitente).build();
        WorkManager.getInstance(this.getBaseContext()).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);
                            try {

                                JSONObject jsonObject = new JSONObject(resultado);
                                String nombre = jsonObject.getString("nombre");
                                String token = jsonObject.getString("id_FCM");

                                Drawable drawable = imagenOtroChat.getDrawable();
                                BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
                                Bitmap bitmap = bitmapDrawable .getBitmap();
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byte[] imageInByte = stream.toByteArray();

                                BDLocal gestorDB = new BDLocal (getBaseContext(), "DAS", null, 1);
                                SQLiteDatabase bd = gestorDB.getWritableDatabase();
                                ContentValues nuevo = new ContentValues();
                                nuevo.put("Id", id_remitente);
                                nuevo.put("Nombre", nombre);
                                nuevo.put("Token", token);
                                nuevo.put("Imagen", imageInByte);
                                bd.insert("Usuarios", null, nuevo);
                                gestorDB.guardarMensaje(id_remitente,mensaje, 0);

                                nombreOtro=nombre;
                                tokenOtro=token;
                                nombreOtroChat.setText(nombreOtro);

                                ListView lista=findViewById(R.id.mensajes);

                                actualizarListaMensajes();
                              /*  adaptador = new AdaptadorMensajes((Activity) getBaseContext(), mensajes,mios);
                                lista.setAdapter(adaptador);

                                lista.setSelection(adaptador.getCount() - 1);

                                actualizarListaMensajes();*/



                                //https://stackoverflow.com/questions/13854742/byte-array-of-image-into-imageview
//                                Bitmap bmp = BitmapFactory.decodeByteArray(imagenOtro, 0, imagenOtro.length);
//                                imagenOtroChat.setImageBitmap(Bitmap.createScaledBitmap(bmp, 150, 150, false));


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(this).enqueueUniqueWork("getDatosUsuario"+id_remitente, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    //https://stackoverflow.com/questions/5446565/android-how-do-i-check-if-activity-is-running
    @Override
    public void onStart() {
        super.onStart();
        running = true;
        idChat=idOtro;
    }

    @Override
    public void onStop() {
        super.onStop();
        running = false;
    }


//    private void enviarMensaje() {
//        EditText mensajeEscrito = findViewById(R.id.mensaje_escrito);
//        mensajes.add(mensajeEscrito.getText().toString());
//        mios.add(true);
//        adaptador.notifyDataSetChanged();
//        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
//
//        String nombreEmisor = preferencias.getString("nombre", "null");
//        String idRemitente=preferencias.getString("id", "null");
//        final String[] tokenRemitente = {""};
//
//        FirebaseMessaging.getInstance().getToken()
//                .addOnCompleteListener(new OnCompleteListener<String>() {
//                    @Override
//                    public void onComplete(@NonNull Task<String> task) {
//                        if (!task.isSuccessful()) {
//                            Log.i("MYAPP", "Fetching FCM registration token failed", task.getException());
//                            return;
//                        }
//
//                        // Get new FCM registration token
//                        tokenRemitente[0] = task.getResult();
//
//                       }
//                });
//
//        //String miId = "0"; //RELLENAR ESTO CON MI ID, QUE ESTARÁ EN ALGUN SITIO (PREFERENCIAS, BD LOCAL...)
//
//        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
//        gestorDB.guardarMensaje(idOtro,mensajeEscrito.getText().toString(), 1);
//        //Firebase.enviarMensajeFCM(this,mensajeEscrito.getText().toString(),tokenOtro,miId);
//        Firebase.enviarMensajeFCM(this,nombreEmisor,mensajeEscrito.getText().toString(),idRemitente,tokenRemitente[0],tokenOtro);
//        mensajeEscrito.setText("");
//        ListView lista=findViewById(R.id.mensajes);
//        lista.setSelection(adaptador.getCount() - 1);
//    }
    private void enviarMensaje() {
        EditText mensajeEscrito = findViewById(R.id.mensaje_escrito);
        mensajes.add(mensajeEscrito.getText().toString());
        mios.add(true);
        adaptador.notifyDataSetChanged();


        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
        gestorDB.guardarMensaje(idOtro,mensajeEscrito.getText().toString(), 1);
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);

        String idRemitente=preferencias.getString("id", "null");
        Firebase.enviarMensajeFCM(this,mensajeEscrito.getText().toString(),tokenOtro,idRemitente);

        mensajeEscrito.setText("");
        ListView lista=findViewById(R.id.mensajes);
        lista.setSelection(adaptador.getCount() - 1);
    }


    public void obtenerMensajesChat(){
        mensajes = new ArrayList<>();
        mios = new ArrayList<>();
        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
        SQLiteDatabase bd = gestorDB.getWritableDatabase();

        String[] campos = new String[] {"Mensaje", "Mio"};
        String[] argumentos = new String[] {idOtro};
        Cursor cu = bd.query("Mensajes",campos,"IdUsuario=?",argumentos,null,null,null);

        if(cu.getCount() != 0){
            cu.moveToFirst();
            for(int i = 0; i < cu.getCount(); i++){
                mensajes.add(cu.getString(0));
                if (cu.getFloat(1) == 0){
                    mios.add(false);
                } else{
                    mios.add(true);
                }
                cu.moveToNext();
            }
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i=new Intent(ChatActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }

    public void actualizarListaMensajes(){
        obtenerMensajesChat();
        ListView lista=findViewById(R.id.mensajes);
        adaptador = new AdaptadorMensajes(this, mensajes,mios);
        lista.setAdapter(adaptador);
        lista.setSelection(adaptador.getCount() - 1);
    }
}