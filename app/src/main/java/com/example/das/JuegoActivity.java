package com.example.das;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;

public class JuegoActivity extends AppCompatActivity {
    private String miId;
    private String idOtro;
    private String tablero;
    private Boolean turno;
    private int meToca = 1;
    private Thread hilo;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            miId = extras.getString("miId");
            idOtro = extras.getString("idOtro");

            handler = new Handler();
            Runnable runHilo = new Runnable() {
                @Override
                public void run() {
                    empezarHilo();
                    handler.postDelayed(this, 1000);
                }
            };
            handler.post(runHilo);

            tablero = "_________";

            Handler handler = new Handler();

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    for(int fila = 1; fila <=3; fila++){
                        for(int columna = 1; columna <=3; columna++){
                            int id = JuegoActivity.this.getResources().getIdentifier("ttt"+fila+columna, "id", JuegoActivity.this.getPackageName());
                            ImageButton boton = JuegoActivity.this.findViewById(id);
                            int indice = (fila-1)*3 + columna -1;
                            if(tablero.charAt(indice) == 'O'){
                                boton.setImageDrawable(ContextCompat.getDrawable(JuegoActivity.this, R.drawable.circulo));
                            }else if (tablero.charAt(indice) == 'X'){
                                boton.setImageResource(R.drawable.cruz);
                            }
                        }
                    }
                    handler.postDelayed(this,200);
                }
            };
            handler.post(run);

            for(int fila = 1; fila <=3; fila++){
                for(int columna = 1; columna <=3; columna++){
                    int id = this.getResources().getIdentifier("ttt"+fila+columna, "id", this.getPackageName());
                    ImageButton boton = this.findViewById(id);
                    int indice = (fila-1)*3 + columna -1;
                    boton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int dif = 0;
                            for(int cont = 0;cont<9;cont++){
                                if (tablero.charAt(cont) == 'O'){
                                    dif++;
                                } else if(tablero.charAt(cont) == 'X'){
                                    dif--;
                                }
                            }
                            turno = dif == 0;

                            StringBuilder nuevoTablero = new StringBuilder(tablero);
                            if(turno && tablero.charAt(indice) == '_' && meToca == 0){
                                nuevoTablero.setCharAt(indice, 'O');
                                meToca = -1;
                            }else if (!turno && tablero.charAt(indice) == '_' && meToca == 0){
                                nuevoTablero.setCharAt(indice, 'X');
                                meToca = -1;
                            }

                            if (!nuevoTablero.toString().equals(tablero)){
                                tablero = nuevoTablero.toString();
                                actualizarTableroBD();
                            }
                        }
                    });

                }
            }

        }

    }

    private void empezarHilo(){
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("id1", miId)
                            .appendQueryParameter("id2",idOtro);
                    String parametros = builder.build().getEncodedQuery();

                    String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/nlebena001/WEB/obtenerDatosJuego.php";
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
                    if (statusCode == 200) {
                        BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        String line, result = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            result += line;
                        }
                        inputStream.close();

                        JSONObject json = new JSONObject(result);
                        if(!json.getString("tablero").equals("null")){
                            tablero = json.getString("tablero");
                            //meToca = miId.equals(json.getString("turno"));
                            if(miId.equals(json.getString("turno"))){
                                if (meToca != -1){
                                    meToca = 0;
                                }
                            }else{
                                meToca = 1;
                            }
                        }
                        if(json.getString("tablero").equals("null") && meToca == 1){
                            meToca = 0;
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        hilo = new Thread(run);
        hilo.start();
    }

    private void actualizarTableroBD(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("id1", miId)
                            .appendQueryParameter("id2",idOtro)
                            .appendQueryParameter("tablero", tablero)
                            .appendQueryParameter("turno", idOtro);
                    String parametros = builder.build().getEncodedQuery();

                    String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/nlebena001/WEB/actualizarDatosJuego.php";
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
                    if (statusCode == 200) {
                        BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        String line, result = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            result += line;
                        }
                        inputStream.close();
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }


}