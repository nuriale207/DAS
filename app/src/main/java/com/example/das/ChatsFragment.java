package com.example.das;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ChatsFragment extends Fragment {
    String[] ids = {};
    String[] nombres = {};
    String[] tokens = {};
    byte[][] imagenes = {};
    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rellenarListas();

        ListView lista=getView().findViewById(R.id.listaChats);
        AdaptadorChats adaptador = new AdaptadorChats(getActivity(), ids,nombres,tokens, imagenes);
        lista.setAdapter(adaptador);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    private void rellenarListas(){
        BDLocal gestorDB = new BDLocal (getContext(), "DAS", null, 1);
        SQLiteDatabase bd = gestorDB.getWritableDatabase();

        String[] campos = new String[] {"Id", "Nombre", "Token", "Imagen"};
        Cursor cu = bd.query("Usuarios",campos,null,null,null,null,null);

        if(cu.getCount() != 0){
            cu.moveToFirst();
            for(int i = 0; i < cu.getCount(); i++){
                ids[ids.length] = cu.getString(0);
                nombres[nombres.length] = cu.getString(1);
                tokens[tokens.length] = cu.getString(2);
                imagenes[imagenes.length] = cu.getBlob(3);
                cu.moveToNext();
            }
        }
    }
}