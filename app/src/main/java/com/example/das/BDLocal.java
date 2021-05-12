package com.example.das;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;


public class BDLocal extends SQLiteOpenHelper {

    public BDLocal(@Nullable Context context, @Nullable String name,
                   @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Mensajes (" +
                "'IdUsuario' VARCHAR(10000) , " +
                "'Mensaje' VARCHAR(10000) , " +
                "'Mio' INT(1))");

        db.execSQL("CREATE TABLE Usuarios (" +
                "'Id' VARCHAR(10000) PRIMARY KEY NOT NULL, " +
                "'Nombre' VARCHAR(10000) NOT NULL, " +
                "'Token' VARCHAR(10000) NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void guardarMensaje(String idUsuario, String mensaje, int mio){
        ContentValues valores = new ContentValues();
        valores.put("IdUsuario", idUsuario);
        valores.put("Mensaje", mensaje);
        valores.put("Mio", mio);
        SQLiteDatabase bd = this.getWritableDatabase();
        bd.insert("Mensajes", null, valores);
    }
}
