package com.example.das.chats;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.das.R;
import com.example.das.bd.BDLocal;
import com.example.das.chats.ChatActivity;
import com.example.das.chats.ChatsFragment;
import com.example.das.chats.GestorChats;
import com.example.das.juego.JuegoActivity;
import com.example.das.mapa.MainActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Firebase extends FirebaseMessagingService {
    private String nombreRemitente;
    private String token_remitente;
    private byte[] imagenRemitente;
    private String idTresRaya="TRESRAYA_010203:";

    public Firebase() {

    }


    //https://stackoverflow.com/questions/37787373/firebase-fcm-how-to-get-token
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", s).apply();
    }


    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");


        if (remoteMessage.getNotification() == null) {

            String mensaje;
            String id_remitente;
            Log.i("MYAPP", "El mensaje es nulo");

            mensaje = remoteMessage.getData().get("mensaje");
            id_remitente = remoteMessage.getData().get("idRemitente");

            obtenerInfoRemitente(id_remitente, mensaje);

            Log.i("FIREBASE", remoteMessage.getData().toString());
        } else {
            //En caso contrario se puede obtener directamente la información
            String titulo = remoteMessage.getNotification().getTitle();


            String body = remoteMessage.getNotification().getBody();
            Log.i("FIREBASE", body);

            sendNotification(titulo, body);
        }

    }

    private void obtenerInfoRemitente(String id_remitente, String mensaje) {
        BDLocal gestorDB = new BDLocal(getBaseContext(), "DAS", null, 1);
        SQLiteDatabase bd = gestorDB.getWritableDatabase();
        String[] campos = new String[]{"Nombre", "Token", "Imagen"};
        String[] argumentos = new String[]{id_remitente};
        Cursor cu = bd.query("Usuarios", campos, "Id=?", argumentos, null, null, null);
        if (cu.getCount() != 0) {
            cu.moveToFirst();
            String nombre = cu.getString(0);
            String token = cu.getString(1);
            byte[] imagen = cu.getBlob(2);

            nombreRemitente = nombre;
            token_remitente = token;
            imagenRemitente = imagen;
            if(!mensaje.contains(idTresRaya)){
                gestorDB.guardarMensaje(id_remitente, mensaje, 0);

            }

            GestorChats.getGestorListas().activarNuevoMensaje();
            recibirMensajeFCM(mensaje, id_remitente, nombreRemitente, token_remitente, imagenRemitente);

        } else {
            //anadirUsuarioABDLocal(id_remitente,mensaje);
            obtenerImagen(id_remitente,mensaje);



        }

    }

    private void recibirMensajeFCM(String mensaje, String id_remitente, String nombreRemitente, String token_remitente, byte[] imagenRemitente) {
        //obtenerInfoRemitente(id_remitente,mensaje);
        //Muestro una notificación con los datos del mensaje y creo en ella un intent al chat
        boolean mostrarNotificacion = true;
        boolean mensajeJuego=false;
        if(mensaje.contains(idTresRaya)){
            boolean juegoAbierto=false;
            mensajeJuego=true;
            juegoAbierto= JuegoActivity.running;
            if(juegoAbierto){
                String idJuego=JuegoActivity.idChat;
                if(idJuego.equals(id_remitente)){
                    mostrarNotificacion=false;

                }
            }
            mensaje=mensaje.substring(idTresRaya.length());
        }
        boolean runningChat = ChatActivity.running;
        boolean runningChatsFragment= ChatsFragment.running;
        if (runningChat && !mensajeJuego) {
            String idChat = ChatActivity.idChat;
            if (idChat.equals(id_remitente)) {
                mostrarNotificacion = false;
            }

        }
        if(runningChatsFragment){
            GestorChats.getGestorListas().activarNuevoChat();

        }
        if (mostrarNotificacion) {
            NotificationManager elManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(getApplicationContext(), "IdCanal");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel elCanal = new NotificationChannel("IdCanal", "NombreCanal",
                        NotificationManager.IMPORTANCE_DEFAULT);
                Intent i = new Intent(getApplicationContext(), ChatActivity.class);
                i.putExtra("id", id_remitente);
                i.putExtra("nombre", nombreRemitente);
                i.putExtra("token", token_remitente);
                i.putExtra("imagen", imagenRemitente);

                Bitmap bitmap = BitmapFactory.decodeByteArray(imagenRemitente, 0, imagenRemitente.length);
                PendingIntent intentEnNot = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
                elManager.createNotificationChannel(elCanal);
                elBuilder.setSmallIcon(R.drawable.logo_nuevo_das)
                        .setContentTitle(nombreRemitente)
                        .setLargeIcon(bitmap)
                        .setContentText(mensaje)
                        .setSubText(mensaje)
                        .setVibrate(new long[]{0, 1000, 500, 1000})
                        .setAutoCancel(true).setContentIntent(intentEnNot);
                elManager.notify(1, elBuilder.build());
            }
        }

    }

    private void obtenerImagen(String id_remitente,String mensaje) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference pathReference = storageRef.child("images/" + id_remitente + ".jpg");
                pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(getApplicationContext())
                                .asBitmap()
                                .load(uri)
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {

                                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                        byte[] imageInByte = stream.toByteArray();
                                        anadirUsuarioABDLocal(id_remitente,  mensaje,imageInByte);
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {
                                    }
                                });
                    }
                });
            }
        });
    }

//
private void anadirUsuarioABDLocal(String id_remitente,String mensaje,byte[] imageInByte) {
    new Thread(new Runnable() {
        @Override
        public void run() {
            try {

                String direccion = "http://ec2-54-242-79-204.compute-1.amazonaws.com/nlebena001/WEB/DAS_users.php";
                Log.i("MYAPP", direccion);

                //Los parámetros de la consulta los recibe de los input data
                String parametros = "funcion=datosUsuario&id=" + id_remitente;
                Log.i("MYAPP", parametros);

                HttpURLConnection urlConnection = null;
                String result = "";
                try {
                    //Se realiza la conexión
                    URL destino = new URL(direccion);
                    urlConnection = (HttpURLConnection) destino.openConnection();
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setReadTimeout(5000);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setDoOutput(true);

                    //Se añaden los parámetros a la petición
                    PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                    out.print(parametros);
                    out.close();
                    Log.i("MYAPP", "vAMOS A HACER EL POST");


                    //En caso de obtener los datos correctamente se almacenan en una variable result que se devuelve a la clase que hizo la petición
                    int statusCode = urlConnection.getResponseCode();
                    Log.i("MYAPP", String.valueOf(statusCode));
                    Log.i("MYAPP", urlConnection.getResponseMessage());
                    if (statusCode == 200) {
                        Log.i("MYAPP", "CONEXION ESTABLECIDA");

                        BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        String line = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            result += line;
                        }
                        inputStream.close();

                        JSONObject jsonObject = new JSONObject(result);
                        String nombre = jsonObject.getString("nombre");
                        String id_FCM = jsonObject.getString("id_FCM");

                        BDLocal gestorDB = new BDLocal(getApplicationContext(), "DAS", null, 1);
                        SQLiteDatabase bd = gestorDB.getWritableDatabase();

                        String[] campos = new String[]{"Id"};
                        String[] argumentos = new String[]{id_remitente};
                        Cursor cu = bd.query("Usuarios", campos, "Id=?", argumentos, null, null, null);
                        if (cu.getCount() == 0) {
                            ContentValues nuevo = new ContentValues();
                            nuevo.put("Id", id_remitente);
                            nuevo.put("Nombre", nombre);
                            nuevo.put("Token", id_FCM);
                            nuevo.put("Imagen", imageInByte);
                            bd.insert("Usuarios", null, nuevo);

                            //ChatsFragment chats = (ChatsFragment)getSupportFragmentManager().getFragment(null, "fragmentChats");
                            //chats.rellenarListas();


                        }
                        if(!mensaje.contains(idTresRaya)){
                            gestorDB.guardarMensaje(id_remitente, mensaje, 0);


                        }
                        else{
                            gestorDB.guardarMensaje(id_remitente, "Hola, he iniciado una partida al tres en raya. ", 0);

                        }
                        //GestorChats.getGestorListas().activarNuevoChat();

                        recibirMensajeFCM(mensaje, id_remitente, nombre, id_FCM, imageInByte);


                    }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
            }).start();

}


    //Emite una notificación recibida desde la consola de Firebase
    private void sendNotification(String titulo, String messageBody) {

        //Método que muestra la notificación recibida
        NotificationManager elManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(getApplicationContext(), "IdCanal");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel elCanal = new NotificationChannel("IdCanal", "NombreCanal",
                    NotificationManager.IMPORTANCE_DEFAULT);
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent intentEnNot = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
            elManager.createNotificationChannel(elCanal);
            elBuilder.setSmallIcon(R.drawable.logo_nuevo_das)
                    .setContentTitle(titulo)
                    .setContentText(messageBody)
                    .setSubText(titulo)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setAutoCancel(true).setContentIntent(intentEnNot);
            elManager.notify(1, elBuilder.build());
        }
    }


    //https://stackoverflow.com/questions/37787373/firebase-fcm-how-to-get-token
    public static String getToken(Context context) {
        return context.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "empty");
    }

    public static void enviarMensajeFCM(Context context, String mensaje, String idFCMDestinatario, String idAppRemitente) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("tokenDestinatario", idFCMDestinatario)
                            .appendQueryParameter("idRemitente", idAppRemitente)
                            .appendQueryParameter("mensaje", mensaje);
                    String parametros = builder.build().getEncodedQuery();

                    //String direccion = "http://ec2-54-242-79-204.compute-1.amazonaws.com/igarcia353/WEB/fcmDAS.php";
                    String direccion = "http://ec2-54-242-79-204.compute-1.amazonaws.com/nlebena001/WEB/fcm.php";
                    HttpURLConnection urlConnection = null;
                    URL destino = new URL(direccion);
                    urlConnection = (HttpURLConnection) destino.openConnection();
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setReadTimeout(5000);

                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                    out.print(parametros);
                    out.close();

                    int statusCode = urlConnection.getResponseCode();

                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String line, result = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                    inputStream.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

}
