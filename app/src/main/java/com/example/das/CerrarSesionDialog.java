package com.example.das;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class CerrarSesionDialog extends DialogFragment {
    //Diálogo que pregunta al usuario si realmente quiere cerrar sesión
//    public interface ListenerdelDialogoIniciarSesion {
//        void alpulsarCerrarSesion();
//
//    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("¿Seguro que quieres cerrar sesión?");
        //CerrarSesionDialog.ListenerdelDialogoIniciarSesion miListener = (CerrarSesionDialog.ListenerdelDialogoIniciarSesion) getFragmentManager();

        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = preferencias.edit();
                editor.remove("id");
                editor.remove("nombre");
                editor.remove("edad");
                editor.remove("genero");
                editor.remove("ubicacion");
                editor.remove("descripcion");
                editor.remove("intereses");

                editor.apply();
                Intent i=new Intent(getActivity(),LoginActivity.class);
                startActivity(i);
                getActivity().finish();
            }
        });



        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });


        AlertDialog dialog=builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

}
