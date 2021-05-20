package com.example.das.chats;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.das.R;
import com.google.android.gms.maps.model.GroundOverlay;

import java.util.ArrayList;


public class AdaptadorMensajes extends ArrayAdapter {
    private Activity context;
    private ArrayList<String> mensajes;
    private ArrayList<Boolean> mios;

    //Adaptador de la lista de mensajes de una conversación
    public AdaptadorMensajes(Activity context, ArrayList<String> mensajes, ArrayList<Boolean> mios) {
        super(context, R.layout.fila_mensaje, mensajes);
        this.context = context;
        this.mensajes = mensajes;
        this.mios = mios;
    }

    //Muestra los mensajes en función de quién los envia
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View fila=view;
        LayoutInflater inflater = context.getLayoutInflater();
        fila = inflater.inflate(R.layout.fila_mensaje, null, true);
        //Establece el color del texto del mensaje a blanco
        TextView mensaje = (TextView) fila.findViewById(R.id.textoMensaje);
        mensaje.setText(mensajes.get(position));
        mensaje.setTextColor(Color.WHITE);
        //Si el mensaje no es mío se sitúa a la izquierda y se le asigna el fondo del mensaje de la otra persona
        if (!mios.get(position)){
            mensaje.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            mensaje.setGravity(Gravity.LEFT);
            parent.setForegroundGravity(Gravity.LEFT);
            mensaje.setBackground(getContext().getDrawable(R.drawable.fondo_mensaje_otro));
            fila.findViewById(R.id.espacioIzq).setVisibility(View.GONE);
            fila.findViewById(R.id.espacioDer).setVisibility(View.INVISIBLE);
        } else{
            //Si el mensaje es mío se sitúa a la derecha con el fondo y alineación de texto correspondientes
            mensaje.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            mensaje.setGravity(Gravity.RIGHT);
            parent.setForegroundGravity(Gravity.RIGHT);
            mensaje.setBackground(getContext().getDrawable(R.drawable.fondo_mensaje_yo));
            fila.findViewById(R.id.espacioDer).setVisibility(View.GONE);
            fila.findViewById(R.id.espacioIzq).setVisibility(View.INVISIBLE);
        }

        return  fila;
    }
}
