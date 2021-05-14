package com.example.das;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

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
        GestorChats.getGestorListas().asignarAdaptadorChats(adaptador);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    public void rellenarListas(){
        BDLocal gestorDB = new BDLocal (getContext(), "DAS", null, 1);
        SQLiteDatabase bd = gestorDB.getWritableDatabase();

        String[] campos = new String[] {"Id", "Nombre", "Token", "Imagen"};
        Cursor cu = bd.query("Usuarios",campos,null,null,null,null,null);

        if(cu.getCount() != 0){
            cu.moveToFirst();
            ArrayList<String> idsAux = new ArrayList<>();
            ArrayList<String> nombresAux = new ArrayList<>();
            ArrayList<String> tokensAux = new ArrayList<>();
            ArrayList<byte[]> imagenesAux = new ArrayList<>();
            for(int i = 0; i < cu.getCount(); i++){

                String[] campos2 = new String[] {"Mensaje"};
                String[] argumentos = new String[] {cu.getString(0)};
                Cursor cu2 = bd.query("Mensajes",campos2,"IdUsuario=?",argumentos,null,null,null);
                if (cu2.getCount() != 0){
                    idsAux.add(cu.getString(0));
                    nombresAux.add(cu.getString(1));
                    tokensAux.add(cu.getString(2));
                    imagenesAux.add(cu.getBlob(3));
                }
                cu.moveToNext();
            }
            ids = new String[idsAux.size()];
            nombres = new String[nombresAux.size()];
            tokens = new String[tokensAux.size()];
            imagenes = new byte[imagenesAux.size()][];
            for (int j=0; j<idsAux.size();j++){
                ids[j] = idsAux.get(j);
                nombres[j] = nombresAux.get(j);
                tokens[j] = tokensAux.get(j);
                imagenes[j] = imagenesAux.get(j);
            }
        }
    }
}