package com.example.das;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Firebase extends FirebaseMessagingService {
    private String nombreRemitente;
    private String token_remitente;
    private byte[] imagenRemitente;

    public Firebase() {

    }


    //https://stackoverflow.com/questions/37787373/firebase-fcm-how-to-get-token
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", s).apply();
    }


    //    public void onMessageReceived(RemoteMessage remoteMessage) {
//        Log.i("notificacion", "Llega aqui");
//
////        String idOtro = remoteMessage.toIntent().getExtras().getString("idOtro");
////        String mensaje = remoteMessage.toIntent().getExtras().getString("mensaje");
////
////        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
////        gestorDB.guardarMensaje(idOtro,mensaje, 0);
//
//
//
//        //Los mensajes recibidos desde un php se gestionan distinto a las notificaciones recibidas desde la consola
//        if(remoteMessage.getNotification()==null) {
//            String nombreEmisor;
//            String mensaje;
//            String id_remitente;
//            String token_remitente;
//            Log.i("MYAPP","El mensaje es nulo");
//
//            nombreEmisor=remoteMessage.getData().get("nombreEmisor");
//            mensaje=remoteMessage.getData().get("mensaje");
//            id_remitente=remoteMessage.getData().get("idRemitente");
//            token_remitente=remoteMessage.getData().get("tokenRemitente");
//            Log.i("FIREBASE",  nombreEmisor+mensaje+id_remitente);
//            BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
//            gestorDB.guardarMensaje(id_remitente,mensaje, 0);
//
//            recibirMensajeFCM(nombreEmisor,mensaje,id_remitente,token_remitente);
//
//            Log.i("FIREBASE",  remoteMessage.getData().toString());
//        }
//        else{
//            //En caso contrario se puede obtener directamente la información
//            String titulo=remoteMessage.getNotification().getTitle();
//
//
//            String body=remoteMessage.getNotification().getBody();
//            Log.i("FIREBASE", body);
//
//            sendNotification(titulo,body);
//        }
//
//    }
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");
        Log.i("DAS", "----------------------------------------------------------------------------------------------------------------------");

//        String idOtro = remoteMessage.toIntent().getExtras().getString("idOtro");
//        String mensaje = remoteMessage.toIntent().getExtras().getString("mensaje");
//
//        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
//        gestorDB.guardarMensaje(idOtro,mensaje, 0);


        //Los mensajes recibidos desde un php se gestionan distinto a las notificaciones recibidas desde la consola
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
            gestorDB.guardarMensaje(id_remitente, mensaje, 0);

            GestorChats.getGestorListas().activarNuevoMensaje();
            recibirMensajeFCM(mensaje, id_remitente, nombreRemitente, token_remitente, imagenRemitente);

        } else {

            //anadirUsuarioABDLocal(id_remitente,mensaje);
            GestorChats.getGestorListas().activarNuevoChat();

            recibirMensajeFCM(mensaje, id_remitente);


        }

    }

    private void recibirMensajeFCM(String mensaje, String id_remitente, String nombreRemitente, String token_remitente, byte[] imagenRemitente) {
        //obtenerInfoRemitente(id_remitente,mensaje);
        //Muestro una notificación con los datos del mensaje y creo en ella un intent al chat
        boolean mostrarNotificacion = true;
        boolean running = ChatActivity.running;
        if (running) {
            String idChat = ChatActivity.idChat;
            if (idChat.equals(id_remitente)) {
                mostrarNotificacion = false;
            }

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
                PendingIntent intentEnNot = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
                elManager.createNotificationChannel(elCanal);
                elBuilder.setSmallIcon(R.drawable.logo_das)
                        .setContentTitle(nombreRemitente)
                        .setContentText(mensaje)
                        .setSubText(mensaje)
                        .setVibrate(new long[]{0, 1000, 500, 1000})
                        .setAutoCancel(true).setContentIntent(intentEnNot);
                elManager.notify(1, elBuilder.build());
            }
        }

    }

    private void recibirMensajeFCM(String mensaje, String id_remitente) {

        //obtenerInfoRemitente(id_remitente,mensaje);
        //Muestro una notificación con los datos del mensaje y creo en ella un intent al chat
        boolean mostrarNotificacion = true;
        boolean running = ChatActivity.running;
        if (running) {
            String idChat = ChatActivity.idChat;
            if (idChat.equals(id_remitente)) {
                mostrarNotificacion = false;
            }

        }
        if (mostrarNotificacion) {
            NotificationManager elManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(getApplicationContext(), "IdCanal");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel elCanal = new NotificationChannel("IdCanal", "NombreCanal",
                        NotificationManager.IMPORTANCE_DEFAULT);
                Intent i = new Intent(getApplicationContext(), ChatActivity.class);
                i.putExtra("id", id_remitente);
                i.putExtra("mensaje", mensaje);
                PendingIntent intentEnNot = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
                elManager.createNotificationChannel(elCanal);
                elBuilder.setSmallIcon(R.drawable.logo_das)
                        .setContentTitle(nombreRemitente)
                        .setContentText(mensaje)
                        .setSubText(mensaje)
                        .setVibrate(new long[]{0, 1000, 500, 1000})
                        .setAutoCancel(true).setContentIntent(intentEnNot);
                elManager.notify(1, elBuilder.build());
            }
        }
    }

//    private void anadirUsuarioABDLocal(String id_remitente,String mensaje) {
//        //Metodo que carga la imagen de Firebase Storage
//        FirebaseStorage storage = FirebaseStorage.getInstance();
//        StorageReference storageRef = storage.getReference();
//        StorageReference pathReference = storageRef.child("images/" + id_remitente + ".jpg");
//        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//            @Override
//            public void onSuccess(Uri uri) {
//
//                Glide.with(getApplicationContext())
//                        .asBitmap()
//                        .load(uri)
//                        .into(new CustomTarget<Bitmap>() {
//                            @Override
//                            public void onResourceReady(@NonNull Bitmap bmp, @Nullable Transition<? super Bitmap> transition) {
//
//
//                                int size     = bmp.getRowBytes() * bmp.getHeight();
//                                ByteBuffer b = ByteBuffer.allocate(size);
//
//                                bmp.copyPixelsToBuffer(b);
//
//                                byte[] bytes = new byte[size];
//
//                                try {
//                                    b.get(bytes, 0, bytes.length);
//                                } catch (BufferUnderflowException e) {
//                                    // always happens
//                                }
//                                anadirUsuarioABDLocal(id_remitente,bytes,mensaje);
//                            }
//
//                            @Override
//                            public void onLoadCleared(@Nullable Drawable placeholder) {
//                            }
//                        });
//            }
//        });
//    }
//
//
//    private void anadirUsuarioABDLocal(String id_remitente,byte[] imagen,String mensaje) {
//
//        Data datos = new Data.Builder()
//                .putString("fichero", "DAS_users.php")
//                .putString("parametros", "funcion=datosUsuario&id=" + id_remitente)
//                .build();
//        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosUsuario"+id_remitente).build();
//        WorkManager.getInstance(this.getBaseContext()).getWorkInfoByIdLiveData(requesContrasena.getId())
//                .observe((LifecycleOwner) getApplicationContext(), new Observer<WorkInfo>() {
//                    @Override
//                    public void onChanged(WorkInfo workInfo) {
//                        if (workInfo != null && workInfo.getState().isFinished()) {
//                            String resultado = workInfo.getOutputData().getString("resultado");
//                            Log.i("MYAPP", "inicio realizado");
//
//                            Log.i("MYAPP", resultado);
//                            try {
//
//                                JSONObject jsonObject = new JSONObject(resultado);
//                                String nombre = jsonObject.getString("nombre");
//                                String token = jsonObject.getString("id_FCM");
//
//
//                                BDLocal gestorDB = new BDLocal (getBaseContext(), "DAS", null, 1);
//                                SQLiteDatabase bd = gestorDB.getWritableDatabase();
//                                ContentValues nuevo = new ContentValues();
//                                nuevo.put("Id", id_remitente);
//                                nuevo.put("Nombre", nombre);
//                                nuevo.put("Token", token);
//                                nuevo.put("Imagen", imagen);
//                                bd.insert("Usuarios", null, nuevo);
//                                gestorDB.guardarMensaje(id_remitente,mensaje, 0);
//
//                                nombreRemitente=nombre;
//                                token_remitente=token;
//                                imagenRemitente=imagen;
//                                recibirMensajeFCM(mensaje,id_remitente,nombreRemitente,token_remitente,imagenRemitente);
//
//
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//
//
//                        }
//                    }
//                });
//        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
//        WorkManager.getInstance(this).enqueueUniqueWork("getDatosUsuario"+id_remitente, ExistingWorkPolicy.REPLACE, requesContrasena);
//    }

//    //Metodo que gestiona el recibir un mensaje
//    private void recibirMensajeFCM(String nombreEmisor,String mensaje, String id_remitente,String token_remitente){
//
//        //Muestro una notificación con los datos del mensaje y creo en ella un intent al chat
//        NotificationManager elManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
//        NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(getApplicationContext(), "IdCanal");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel elCanal = new NotificationChannel("IdCanal", "NombreCanal",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            Intent i = new Intent(getApplicationContext(),ChatActivity.class);
//            i.putExtra("id",id_remitente);
//            i.putExtra("nombre",nombreEmisor);
//            i.putExtra("token",token_remitente);
//            PendingIntent intentEnNot = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
//            elManager.createNotificationChannel(elCanal);
//            elBuilder.setSmallIcon(R.drawable.logo_das)
//                    .setContentTitle(nombreEmisor)
//                    .setContentText(mensaje)
//                    .setSubText(mensaje)
//                    .setVibrate(new long[]{0, 1000, 500, 1000})
//                    .setAutoCancel(true).setContentIntent(intentEnNot);
//            elManager.notify(1, elBuilder.build());
//        }
//
//    }


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
            elBuilder.setSmallIcon(R.drawable.logo_das)
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

                    //String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/igarcia353/WEB/fcmDAS.php";
                    String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/nlebena001/WEB/fcm.php";
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
//    public static void enviarMensajeFCM(Context context, String nombreEmisor, String mensaje, String idRemitente,String tokenRemitente,String tokenDestinatario){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Uri.Builder builder = new Uri.Builder()
//                            .appendQueryParameter("nombreEmisor", nombreEmisor)
//                            .appendQueryParameter("mensaje", mensaje)
//                            .appendQueryParameter("idRemitente", idRemitente)
//                            .appendQueryParameter("tokenRemitente", tokenRemitente)
//                            .appendQueryParameter("tokenDestinatario", tokenDestinatario);
//                    String parametros = builder.build().getEncodedQuery();
//
//                    String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/nlebena001/WEB/enviarMensaje.php";
//                    HttpURLConnection urlConnection = null;
//                    URL destino = new URL(direccion);
//                    urlConnection = (HttpURLConnection) destino.openConnection();
//                    urlConnection.setConnectTimeout(5000);
//                    urlConnection.setReadTimeout(5000);
//
//                    urlConnection.setRequestMethod("POST");
//                    urlConnection.setDoOutput(true);
//                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//
//                    PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
//                    out.print(parametros);
//                    out.close();
//
//                    int statusCode = urlConnection.getResponseCode();
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//
//
//    }

}
