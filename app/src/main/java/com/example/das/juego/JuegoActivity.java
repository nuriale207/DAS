package com.example.das.juego;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.das.ReproductorSonido;
import com.example.das.bd.BDLocal;
import com.example.das.chats.Firebase;
import com.example.das.R;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class JuegoActivity extends AppCompatActivity {
    private String miId;
    private String idOtro;
    private String tablero;
    private String miNombre;
    private String tokenOtro;
    private Boolean turno;
    private int meToca = 1;
    private String ganador = "null";
    private Thread hilo;
    private Handler handler;
    public static boolean running;
    public static String idChat;
    private byte[] imagenOtro;
    private  String nombreOtro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego);

        Bundle extras = getIntent().getExtras();
        //Se obtiene de los extras la información de los dos jugadores
        if (extras != null) {
            miId = extras.getString("miId");
            idOtro = extras.getString("idOtro");

            miNombre=extras.getString("miNombre");
            tokenOtro=extras.getString("tokenOtro");
            nombreOtro=extras.getString("nombreOtro");
            imagenOtro=extras.getByteArray("imagenOtro");

            handler = new Handler();
            //Mediante un hilo se comprueba cada segundo si ha ganado alguno de los dos jugadores
            Runnable runHilo = new Runnable() {
                @Override
                public void run() {
                    empezarHilo();
                    TextView textoGanador = findViewById(R.id.textoGanador);
                    Button empezarDeNuevo = findViewById(R.id.botonNuevaPartida);
                    if(!ganador.equals("null")){
                        //En caso de terminar la partida se muestra el mensaje correspondiente junto al botón empezar de nuevo
                        ponerListenerABoton();
                        if (ganador.equals(miId)){
                            if (!textoGanador.getText().toString().equals("¡Has ganado!")){
                                textoGanador.setText("¡Has ganado!");
                                ReproductorSonido.getReproductorSonido().reproducirFinPartida(JuegoActivity.this, R.raw.s_ganar_partida);                          }
                            empezarDeNuevo.setVisibility(View.VISIBLE);
                            textoGanador.setVisibility(View.VISIBLE);
                        }else if(ganador.equals(idOtro)){
                            if (!textoGanador.getText().toString().equals("Has perdido...")){
                                textoGanador.setText("Has perdido...");
                                ReproductorSonido.getReproductorSonido().reproducirFinPartida(JuegoActivity.this, R.raw.s_perder_partida);
                            }
                            empezarDeNuevo.setVisibility(View.VISIBLE);
                            textoGanador.setVisibility(View.VISIBLE);
                        }
                        else {
                            if (!textoGanador.getText().toString().equals("Empate")){
                                textoGanador.setText("Empate");
                                ReproductorSonido.getReproductorSonido().reproducirFinPartida(JuegoActivity.this, R.raw.s_empatar_partida);
                            }
                            empezarDeNuevo.setVisibility(View.VISIBLE);
                            textoGanador.setVisibility(View.VISIBLE);
                        }
                    } else{
                        //Si no se ha ganado se ocultan tanto el botón como el texto del ganador
                        empezarDeNuevo.setVisibility(View.INVISIBLE);
                        textoGanador.setVisibility(View.INVISIBLE);
                    }
                    handler.postDelayed(this, 1000);
                }
            };
            handler.post(runHilo);


            tablero = "_________";

            Handler handler = new Handler();
            //se dibuja el tablero guardado en la BD cada 200 milisegundos, se muestra un icono u otro en base a la partida almacenada en la BD
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
                            }else{
                                boton.setImageDrawable(null);
                            }
                        }
                    }
                    handler.postDelayed(this,200);
                }
            };
            handler.post(run);

            //Bucle que realiza el recuento de cada tipo de ficha del tablero y analiza qué ficha corresponde poner
            for(int fila = 1; fila <=3; fila++){
                for(int columna = 1; columna <=3; columna++){
                    int id = this.getResources().getIdentifier("ttt"+fila+columna, "id", this.getPackageName());
                    ImageButton boton = this.findViewById(id);
                    int indice = (fila-1)*3 + columna -1;
                    boton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ReproductorSonido.getReproductorSonido().habilitarCancioncilla();
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
                            if (tablero.charAt(indice) == '_' && meToca == 0){
                                if(turno){
                                    nuevoTablero.setCharAt(indice, 'O');
                                    meToca = 1;
                                }else if (!turno){
                                    nuevoTablero.setCharAt(indice, 'X');
                                    meToca = 1;
                                }
                            }

                            //Si el tablero se ha modificado
                            if (!nuevoTablero.toString().equals(tablero)){
                                ReproductorSonido.getReproductorSonido().reproducirSonido(JuegoActivity.this,R.raw.s_poner_ficha);
                                //Si no hay mensajes de ese usuario se almacena un mensaje para que se añada a lalista de chats
                                if(!hayMensajes()){
                                    BDLocal gestorDB = new BDLocal(getApplicationContext(), "DAS", null, 1);
                                    SQLiteDatabase bd = gestorDB.getWritableDatabase();
                                    gestorDB.guardarMensaje(idOtro, "Hola, he iniciado una partida al tres en raya. ", 1);

                                }
                                //Se envia un mensaje a través de Firebase indicando al otro jugador que se ha marcado una casilla
                                Firebase.enviarMensajeFCM(getBaseContext(),"TRESRAYA_010203: "+miNombre+" ha marcado una casilla. Es tu turno!",tokenOtro,miId);
                                tablero = nuevoTablero.toString();

                                Boolean ganado = false;
                                Boolean empatado=false;

                                // 0 1 2
                                // 3 4 5
                                // 6 7 8
                                //Se comprueba si alguno de los jugadores ha ganado
                                if (tablero.charAt(0) == tablero.charAt(1) && tablero.charAt(1) == tablero.charAt(2) && tablero.charAt(0) != '_' ||
                                        tablero.charAt(3) == tablero.charAt(4) && tablero.charAt(4) == tablero.charAt(5) && tablero.charAt(3) != '_' ||
                                        tablero.charAt(6) == tablero.charAt(7) && tablero.charAt(7) == tablero.charAt(8) && tablero.charAt(6) != '_' ||
                                        tablero.charAt(0) == tablero.charAt(3) && tablero.charAt(3) == tablero.charAt(6) && tablero.charAt(0) != '_' ||
                                        tablero.charAt(1) == tablero.charAt(4) && tablero.charAt(4) == tablero.charAt(7) && tablero.charAt(1) != '_' ||
                                        tablero.charAt(2) == tablero.charAt(5) && tablero.charAt(5) == tablero.charAt(8) && tablero.charAt(2) != '_' ||
                                        tablero.charAt(0) == tablero.charAt(4) && tablero.charAt(4) == tablero.charAt(8) && tablero.charAt(0) != '_' ||
                                        tablero.charAt(2) == tablero.charAt(4) && tablero.charAt(4) == tablero.charAt(6) && tablero.charAt(2) != '_'){
                                    ganado = true;
                                    Firebase.enviarMensajeFCM(getBaseContext(),"TRESRAYA_010203: Tu partida con "+miNombre+" ha terminado Lo siento has perdido!",tokenOtro,miId);
                                }
                                //Si el tablero es no vacío y nadie ha ganado se trata de un empate
                                else if(tablero.charAt(0)!='_'&&tablero.charAt(1)!='_'&&tablero.charAt(2)!='_'&&tablero.charAt(3)!='_'&&tablero.charAt(4)!='_'&&tablero.charAt(5)!='_'&&
                                        tablero.charAt(6)!='_'&&tablero.charAt(7)!='_'&&tablero.charAt(8)!='_'){
                                    empatado=true;
                                    Firebase.enviarMensajeFCM(getBaseContext(),"TRESRAYA_010203: Tu partida con "+miNombre+" ha terminado. Habeis empatado!",tokenOtro,miId);

                                }
                                //Se almacena el ID del que ha ganado
                                if(ganado){
                                    ganador = miId;
                                }
                                if(empatado){
                                    ganador="empate";

                                }
                                //Se actualiza el tablero en la BD
                                actualizarTableroBD();
                            }
                        }
                    });
                }
            }
        }
    }


//Método que comprueba si hay mensajes del otro jugador en la BD local
    private boolean hayMensajes() {
        boolean hayMensajes=true;
         BDLocal gestorDB = new BDLocal(getBaseContext(), "DAS", null, 1);
            SQLiteDatabase bd = gestorDB.getWritableDatabase();
            String[] campos = new String[]{"Mensaje"};
            String[] argumentos = new String[]{idOtro};
            Cursor cu = bd.query("Mensajes", campos, "IdUsuario=?", argumentos, null, null, null);
            if (cu.getCount() == 0) {
                hayMensajes=false;

            }
            return hayMensajes;

    }

    //https://stackoverflow.com/questions/5446565/android-how-do-i-check-if-activity-is-running
    //Métodos que modifican las variables públicas utilizadas por Firebase que indican si el juego está abierto
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
    //Método que obtiene la información de la partida de la BD
    private void empezarHilo(){
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("id1", miId)
                            .appendQueryParameter("id2",idOtro);
                    String parametros = builder.build().getEncodedQuery();

                    String direccion = "http://ec2-54-242-79-204.compute-1.amazonaws.com/nlebena001/WEB/obtenerDatosJuego.php";
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
                            ganador = json.getString("ganador");
                            if(ganador.equals("null")){
                                if(miId.equals(json.getString("turno")) || json.getString("turno").equals("null")){
                                    if (meToca != -1){
                                        meToca = 0;
                                    }
                                }else{
                                    meToca = 1;
                                }
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
    //Método que actualiza el tablero almacenado en la BD
    private void actualizarTableroBD(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("id1", miId)
                            .appendQueryParameter("id2",idOtro)
                            .appendQueryParameter("tablero", tablero)
                            .appendQueryParameter("turno", idOtro)
                            .appendQueryParameter("ganador", ganador);
                    String parametros = builder.build().getEncodedQuery();

                    String direccion = "http://ec2-54-242-79-204.compute-1.amazonaws.com/nlebena001/WEB/actualizarDatosJuego.php";
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
    //Pone al botón de empezar partida su listener
    private void ponerListenerABoton(){
        Button empezarPartida = findViewById(R.id.botonNuevaPartida);
        empezarPartida.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textoGanador = findViewById(R.id.textoGanador);
                meToca = 1;
                ReproductorSonido.getReproductorSonido().habilitarCancioncilla();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Uri.Builder builder = new Uri.Builder()
                                    .appendQueryParameter("id1", miId)
                                    .appendQueryParameter("id2",idOtro)
                                    .appendQueryParameter("tablero", "_________")
                                    .appendQueryParameter("turno", "null")
                                    .appendQueryParameter("ganador", "null");
                            String parametros = builder.build().getEncodedQuery();

                            String direccion = "http://ec2-54-242-79-204.compute-1.amazonaws.com/nlebena001/WEB/actualizarDatosJuego.php";
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
        });
    }

    @Override
    public void onBackPressed() {
        ReproductorSonido.getReproductorSonido().reproducirSonido(this, R.raw.s_atras);
        super.onBackPressed();
    }

}