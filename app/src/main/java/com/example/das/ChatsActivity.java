package com.example.das;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class ChatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);


        //Se comprueba si hay un usuario logeado. En caso de no haberlo se abre la actividad de login
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        String userId=preferencias.getString("id",null);

        if(userId==null){
            Intent i=new Intent(this, LoginActivity.class);
            startActivity(i);
        }

    }
}