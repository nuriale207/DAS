package com.example.das;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {
    private String idOtro;
    private String nombreOtro;
    private String tokenOtro;
    private AdaptadorMensajes adaptador;
    ArrayList<String> mensajes = new ArrayList<>();
    ArrayList<Boolean> mios = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            idOtro = extras.getString("id");
            nombreOtro = extras.getString("nombre");
            tokenOtro = extras.getString("token");
        }
        super.onCreate(savedInstanceState);
        obtenerMensajesChat();
        setContentView(R.layout.activity_chat);
        ListView lista=findViewById(R.id.mensajes);
        adaptador = new AdaptadorMensajes(this, mensajes,mios);
        lista.setAdapter(adaptador);
        EditText mensajeEscrito = findViewById(R.id.mensaje_escrito);
        TextView.OnEditorActionListener listenerTeclado = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND){
                    enviarMensaje();
                }
                return false;
            }
        };

        ImageButton enviarMensaje = findViewById(R.id.botonEnviarMensaje);
        enviarMensaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarMensaje();
            }
        });

    }

    private void enviarMensaje() {
        EditText mensajeEscrito = findViewById(R.id.mensaje_escrito);
        mensajes.add(mensajeEscrito.getText().toString());
        mios.add(true);
        adaptador.notifyDataSetChanged();
        mensajeEscrito.setText("");

        String miId = "0"; //RELLENAR ESTO CON MI ID, QUE ESTARÁ EN ALGUN SITIO (PREFERENCIAS, BD LOCAL...)

        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
        gestorDB.guardarMensaje(idOtro,mensajeEscrito.getText().toString(), 1);
        Firebase.enviarMensajeFCM(this,mensajeEscrito.getText().toString(),tokenOtro,miId);
    }


    public void obtenerMensajesChat(){
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


}