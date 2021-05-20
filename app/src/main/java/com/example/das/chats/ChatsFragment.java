package com.example.das.chats;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.das.bd.BDLocal;
import com.example.das.R;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {
    String[] ids = {};
    String[] nombres = {};
    String[] tokens = {};
    byte[][] imagenes = {};
    Handler handler;
    AdaptadorChats adaptador;

    public static boolean running;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rellenarListas();

        ListView lista=getView().findViewById(R.id.listaChats);
        adaptador = new AdaptadorChats(getActivity(), ids,nombres,tokens, imagenes);
        lista.setAdapter(adaptador);
        GestorChats.getGestorListas().asignarAdaptadorChats(adaptador);
        handler = new Handler();
        Runnable actualizadorChat = new Runnable() {
            @Override
            public void run() {
                if(GestorChats.getGestorListas().comprobarNuevoChat()){
                    actualizarListaChats();
                }
                handler.postDelayed(this,2000);
            }
        };
        handler.postDelayed(actualizadorChat, 2000);
    }

    public void actualizarListaChats(){
        rellenarListas();
        ListView lista=getView().findViewById(R.id.listaChats);
        adaptador = new AdaptadorChats(getActivity(), ids,nombres,tokens, imagenes);
        lista.setAdapter(adaptador);
        lista.setSelection(0);
    }

    //https://stackoverflow.com/questions/5446565/android-how-do-i-check-if-activity-is-running
    @Override
    public void onStart() {
        super.onStart();
        running = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        running = false;
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
            cu.close();
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