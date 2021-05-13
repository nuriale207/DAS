package com.example.das;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class AdaptadorChats extends ArrayAdapter {
    private Activity context;
    private String[] ids;
    private String[] nombres;
    private String[] tokens;
    private byte[][] imagenes;

    public AdaptadorChats(Activity context, String[] ids,String[] nombres,String[] tokens, byte[][] imagenes) {
        super(context, R.layout.fila_chat, nombres);
        this.context = context;
        this.ids = ids;
        this.nombres = nombres;
        this.tokens = tokens;
        this.imagenes = imagenes;
    }


    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View fila=view;
        LayoutInflater inflater = context.getLayoutInflater();
        fila = inflater.inflate(R.layout.fila_chat, null, true);
        ImageView foto = (ImageView) fila.findViewById(R.id.fotoChat);
        TextView nombre = (TextView) fila.findViewById(R.id.nombreChat);

        //https://stackoverflow.com/questions/13854742/byte-array-of-image-into-imageview
        Bitmap bmp = BitmapFactory.decodeByteArray(imagenes[position], 0, imagenes[position].length);
        foto.setImageBitmap(Bitmap.createScaledBitmap(bmp, foto.getWidth(), foto.getHeight(), false));

        nombre.setText(nombres[position]);
        nombre.setTextColor(Color.BLACK);
        fila.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, ChatActivity.class);
                i.putExtra("id", ids[position]);
                i.putExtra("nombre", nombres[position]);
                i.putExtra("token", tokens[position]);
                i.putExtra("imagen", imagenes[position]);
                context.startActivity(i);
            }
        });
        return  fila;
    }
}
