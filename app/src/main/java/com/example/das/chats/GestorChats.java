package com.example.das.chats;

public class GestorChats {
    //Clase que gestiona los cambios que ha habido en la lista de chat y la lista de mensajes
    private static GestorChats gestorChats;
    private AdaptadorChats adaptadorChats;
    private Boolean flagNuevoMensaje = false;
    private Boolean getFlagNuevoChat =  false;

    public static GestorChats getGestorListas() {
        if (gestorChats == null) {
            gestorChats = new GestorChats();
        }
        return gestorChats;
    }

    //Asigna el adaptador para los chats para poder actualizarlo
    public void asignarAdaptadorChats(AdaptadorChats adaptador){
        this.adaptadorChats = adaptador;
    }

    //Actualiza la lista de chats mostrada
    public void actualizarChats(){
        this.adaptadorChats.notifyDataSetChanged();
    }

    //Método empleado por firebase al recibir un nuevo mensaje
    public void activarNuevoMensaje(){
        flagNuevoMensaje = true;
    }

    //Comprueba si ha habido un mensaje nuevo en el chat, es utilizado por el chat fragment para actualizar su lista de mensajes
    public Boolean comprobarNuevoMensaje(){
        Boolean res = false;
        if(flagNuevoMensaje){
            res = true;
            flagNuevoMensaje = false;
        }
        return res;
    }

    //Método empleado para indicar que se ha iniciado una nueva conversación
    public void activarNuevoChat(){
        getFlagNuevoChat = true;
    }

    //Comprueba si el flag del nuevo chat está a uno
    public Boolean comprobarNuevoChat(){
        Boolean res = false;
        if(getFlagNuevoChat){
            res = true;
            getFlagNuevoChat = false;
        }
        return res;
    }
}
