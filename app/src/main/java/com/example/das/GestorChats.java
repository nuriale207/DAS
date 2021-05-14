package com.example.das;

public class GestorChats {

    private static GestorChats gestorChats;
    private AdaptadorChats adaptadorChats;

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

}
