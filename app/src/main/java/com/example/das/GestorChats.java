package com.example.das;

import android.content.Context;

public class GestorChats {

    private static GestorChats gestorChats;
    private AdaptadorChats adaptadorChats;
    private Boolean flagNuevoMensaje = false;
    private Boolean getFlagNuevoChat =  false;
    private String idNuevoChat;
    private String mensajeNuevo;

    public static GestorChats getGestorListas() {
        if (gestorChats == null) {
            gestorChats = new GestorChats();
        }
        return gestorChats;
    }

    public void asignarAdaptadorChats(AdaptadorChats adaptador){
        this.adaptadorChats = adaptador;
    }

    public void actualizarChats(){
        this.adaptadorChats.notifyDataSetChanged();
    }

    public void activarNuevoMensaje(){
        flagNuevoMensaje = true;
    }

    public Boolean comprobarNuevoMensaje(){
        Boolean res = false;
        if(flagNuevoMensaje){
            res = true;
            flagNuevoMensaje = false;
        }
        return res;
    }

//    //public void activarNuevoChat(){
//        getFlagNuevoChat = true;
//    }

    public void activarNuevoChat(String id_chat,String mensaje){

        getFlagNuevoChat = true;
        idNuevoChat= id_chat;
        mensajeNuevo = mensaje;
    }




    public Boolean comprobarNuevoChat(){
        Boolean res = false;
        if(getFlagNuevoChat){
            res = true;
            getFlagNuevoChat = false;
        }
        return res;
    }

    public String getIdNuevoChat(){
        return this.idNuevoChat;

    }
    public String getMensajeNuevo(){
        return this.mensajeNuevo;

    }
}
