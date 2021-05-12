package com.example.das;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

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
        String idOtro = remoteMessage.toIntent().getExtras().getString("idOtro");
        String mensaje = remoteMessage.toIntent().getExtras().getString("mensaje");

        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
        gestorDB.guardarMensaje(idOtro,mensaje, 0);
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

                    String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/igarcia353/WEB/fcm.php";
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
