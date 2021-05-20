package com.example.das.chats;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

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
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.das.ReproductorSonido;
import com.example.das.bd.BDLocal;
import com.example.das.bd.ConexionBDWorker;
import com.example.das.mapa.InfoUserActivity;
import com.example.das.juego.JuegoActivity;
import com.example.das.R;
import com.example.das.mapa.MainActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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

    private LinearLayout layoutPerfil;
    private ImageView imagenOtroChat;
    private Boolean mostrarImagen;
    public static boolean running;
    public static String idChat;
    TextView nombreOtroChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        //Se obtienen las views
        Bundle extras = getIntent().getExtras();
        imagenOtroChat = findViewById(R.id.imagenOtroChat);
        nombreOtroChat = findViewById(R.id.nombreOtroChat);
        layoutPerfil=findViewById(R.id.linearLayout3);

        //Se obtiene el id de las preferencias
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        miId = preferencias.getString("id", "null");

        //Se obtiene de los extras la información del otro usuario para mostrarla
        if (extras != null) {
            idOtro = extras.getString("id");
            nombreOtro = extras.getString("nombre");
            //Si no se tiene el nombre del otro usuario se añade a la BD local
            if(nombreOtro==null){
                String mensaje = extras.getString("mensaje");
                anadirUsuarioABDLocal(idOtro,mensaje);
            }
            else{
                //Se obtienen el token e imagen del otro usuario de los extras y se añade el adaptador con los mensajes
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
        //Al hacer click sobre el botón juego se abre la pantalla de juego
        ImageView botonJuego = findViewById(R.id.botonJuego);
        botonJuego.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ChatActivity.this, JuegoActivity.class);
                i.putExtra("miId", miId);
                i.putExtra("idOtro", idOtro);

                i.putExtra("miNombre",preferencias.getString("nombre","nombre"));
                i.putExtra("tokenOtro",tokenOtro);
                i.putExtra("nombreOtro",nombreOtro);

                i.putExtra("imagenOtro",imagenOtro);
                startActivity(i);
            }
        });

        //Al hacer click sobre enviar mensaje se llama al método enviarMensaje()
        ImageButton enviarMensaje = findViewById(R.id.botonEnviarMensaje);
        enviarMensaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarMensaje();
            }
        });
        //Al clickar sobre el nombre del otro usuario se abre una actividad con su información
        nombreOtroChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(ChatActivity.this, InfoUserActivity.class);
                i.putExtra("id",idOtro);
                i.putExtra("chat",true);
                startActivity(i);

            }
        });
        //Se establece un handler que analiza cada 2 segundos si hay un nuevo mensaje en el chat
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


    //Método que añade el usuario a la BD local del servidor
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

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        WorkManager.getInstance(this).enqueueUniqueWork("getDatosUsuario"+id_remitente, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    //https://stackoverflow.com/questions/5446565/android-how-do-i-check-if-activity-is-running
    //mëtodos que analizan los flags utilizados por Firebase para determinar si el chat está abierto y con que usuario
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


    //Método que se cominica con Firebase para enviar el mensaje y lo almacena en la BD local
    private void enviarMensaje() {
        EditText mensajeEscrito = findViewById(R.id.mensaje_escrito);
        String texto=mensajeEscrito.getText().toString();
        if(texto.length()>0){
            ReproductorSonido.getReproductorSonido().reproducirSonido(this, R.raw.s_enviar_mensaje);
            mensajes.add(texto);
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

    }





    //Obtiene los mensajes del chat de la BD local
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
        ReproductorSonido.getReproductorSonido().reproducirSonido(this, R.raw.s_atras);
        super.onBackPressed();
        Intent i=new Intent(ChatActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }


    //Actualiza la lista de mensajes para mostrar el nuevo mensaje
    public void actualizarListaMensajes(){
        obtenerMensajesChat();
        ListView lista=findViewById(R.id.mensajes);
        adaptador = new AdaptadorMensajes(this, mensajes,mios);
        lista.setAdapter(adaptador);
        lista.setSelection(adaptador.getCount() - 1);
        ReproductorSonido.getReproductorSonido().reproducirSonido(this, R.raw.s_recibir_mensaje);
    }
}