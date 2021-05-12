package com.example.das;

import androidx.appcompat.app.AppCompatActivity;

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
    private String idUsuario;
    private AdaptadorMensajes adaptador;
    ArrayList<String> mensajes = new ArrayList<>();
    ArrayList<Boolean> mios = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        //ListView lista=findViewById(R.id.mensajes);
        EditText mensajeEscrito = findViewById(R.id.mensaje_escrito);
        mensajes.add(mensajeEscrito.getText().toString());
        mios.add(true);
        adaptador.notifyDataSetChanged();
        mensajeEscrito.setText("");
    }

}