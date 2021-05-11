package com.example.das;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ListView;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String[] mensajes = {"Hola", "Que tal?", "Muy bien, gracias"};
        boolean[] mios = {true,false,true};
        ListView lista=findViewById(R.id.mensajes);
        AdaptadorMensajes adaptador = new AdaptadorMensajes(this, mensajes,mios);
        lista.setAdapter(adaptador);

    }

}