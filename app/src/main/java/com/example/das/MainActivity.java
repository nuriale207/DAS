package com.example.das;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TabHost;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Se comprueba si hay un usuario logeado. En caso de no haberlo se abre la actividad de login
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        String userId=preferencias.getString("id",null);

        if(userId==null){
            Intent i=new Intent(this, LoginActivity.class);
            startActivity(i);
        }

        // initiating the tabhost
        TabHost tabhost = (TabHost) findViewById(R.id.tabhost);

        // setting up the tab host
        tabhost.setup();

        // Code for adding Tab 1 to the tabhost
        TabHost.TabSpec spec = tabhost.newTabSpec("Chats");
        spec.setContent(R.id.tab1);

        // setting the name of the tab 1 as "Tab One"
        spec.setIndicator("Chats");

        // adding the tab to tabhost
        tabhost.addTab(spec);

        // Code for adding Tab 2 to the tabhost
        spec = tabhost.newTabSpec("Mapa");
        spec.setContent(R.id.tab2);

        // setting the name of the tab 1 as "Tab Two"
        spec.setIndicator("Mapa");
        tabhost.addTab(spec);

        // Code for adding Tab 3 to the tabhost
        spec = tabhost.newTabSpec("Perfil");
        spec.setContent(R.id.tab3);
        spec.setIndicator("Perfil");
        tabhost.addTab(spec);

    }
}