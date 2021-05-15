package com.example.das;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Firebase extends FirebaseMessagingService {

    public Firebase() {

    }


    //https://stackoverflow.com/questions/37787373/firebase-fcm-how-to-get-token
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", s).apply();
    }


    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i("notificacion", "Llega aqui");

//        String idOtro = remoteMessage.toIntent().getExtras().getString("idOtro");
//        String mensaje = remoteMessage.toIntent().getExtras().getString("mensaje");
//
//        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
//        gestorDB.guardarMensaje(idOtro,mensaje, 0);



        //Los mensajes recibidos desde un php se gestionan distinto a las notificaciones recibidas desde la consola
        if(remoteMessage.getNotification()==null) {
            String nombreEmisor;
            String mensaje;
            String id_remitente;
            String token_remitente;
            Log.i("MYAPP","El mensaje es nulo");

            nombreEmisor=remoteMessage.getData().get("nombreEmisor");
            mensaje=remoteMessage.getData().get("mensaje");
            id_remitente=remoteMessage.getData().get("idRemitente");
            token_remitente=remoteMessage.getData().get("tokenRemitente");
            Log.i("FIREBASE",  nombreEmisor+mensaje+id_remitente);
            BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
            gestorDB.guardarMensaje(id_remitente,mensaje, 0);

            recibirMensajeFCM(nombreEmisor,mensaje,id_remitente,token_remitente);

            Log.i("FIREBASE",  remoteMessage.getData().toString());
        }
        else{
            //En caso contrario se puede obtener directamente la información
            String titulo=remoteMessage.getNotification().getTitle();


            String body=remoteMessage.getNotification().getBody();
            Log.i("FIREBASE", body);

            sendNotification(titulo,body);
        }

    }
    //Metodo que gestiona el recibir un mensaje
    private void recibirMensajeFCM(String nombreEmisor,String mensaje, String id_remitente,String token_remitente){

        //Muestro una notificación con los datos del mensaje y creo en ella un intent al chat
        NotificationManager elManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(getApplicationContext(), "IdCanal");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel elCanal = new NotificationChannel("IdCanal", "NombreCanal",
                    NotificationManager.IMPORTANCE_DEFAULT);
            Intent i = new Intent(getApplicationContext(),ChatActivity.class);
            i.putExtra("id",id_remitente);
            i.putExtra("nombre",nombreEmisor);
            i.putExtra("token",token_remitente);
            PendingIntent intentEnNot = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
            elManager.createNotificationChannel(elCanal);
            elBuilder.setSmallIcon(R.drawable.logo_das)
                    .setContentTitle(nombreEmisor)
                    .setContentText(mensaje)
                    .setSubText(mensaje)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setAutoCancel(true).setContentIntent(intentEnNot);
            elManager.notify(1, elBuilder.build());
        }

    }


    //Emite una notificación recibida desde la consola de Firebase
    private void sendNotification(String titulo,String messageBody) {

        //Método que muestra la notificación recibida
        NotificationManager elManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(getApplicationContext(), "IdCanal");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel elCanal = new NotificationChannel("IdCanal", "NombreCanal",
                    NotificationManager.IMPORTANCE_DEFAULT);
            Intent i = new Intent(getApplicationContext(),MainActivity.class);
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
    public static String getToken(Context context){
        return  context.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "empty");
    }

    public static void enviarMensajeFCM(Context context, String mensaje, String idFCMDestinatario, String idAppRemitente){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("tokenDestinatario", idFCMDestinatario)
                            .appendQueryParameter("idRemitente", idAppRemitente)
                            .appendQueryParameter("mensaje", mensaje);
                    String parametros = builder.build().getEncodedQuery();

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
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();


    }

}
