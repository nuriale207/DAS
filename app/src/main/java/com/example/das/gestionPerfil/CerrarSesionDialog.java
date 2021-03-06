package com.example.das.gestionPerfil;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.das.bd.BDLocal;
import com.example.das.bd.ConexionBDWorker;
import com.example.das.registroLogin.LoginActivity;

public class CerrarSesionDialog extends DialogFragment {
    //Diálogo que pregunta al usuario si realmente quiere cerrar sesión

    private String id;
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
                //Al hacer click en aceptar se elimina la información del usuario actual de las preferencias
                SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getActivity());
                id=preferencias.getString("id","0");
                SharedPreferences.Editor editor = preferencias.edit();
                editor.remove("id");
                editor.remove("nombre");
                editor.remove("edad");
                editor.remove("genero");
                editor.remove("ubicacion");
                editor.remove("descripcion");
                editor.remove("intereses");
                editor.remove("distancia");


                //Se resetea la BD local
                BDLocal gestorDB = new BDLocal (getActivity(), "DAS", null, 1);
                SQLiteDatabase bd = gestorDB.getWritableDatabase();

                //bd.delete("Usuarios",null,null);
                bd.execSQL("delete from Usuarios");
                //bd.delete("Mensajes",null,null);
                bd.execSQL("delete from Mensajes");

                editor.apply();
                //Se elimina el token FCM de la bd remota y además se indica con un 0 en la sesión que el usuario ha cerrado sesión
                eliminarTokenFCM();
                Intent i=new Intent(getActivity(), LoginActivity.class);
                getActivity().finish();

                startActivity(i);
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
    private void eliminarTokenFCM() {
        //Se  comunica con la bd para indicar que se ha cerrado sesión y vaciar el token FCM
        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=" + "editarToken" + "&id=" + id + "&" + "token="+"logged out&sesion=0")
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("eliminarToken" + id).build();
        WorkManager.getInstance(getActivity()).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);


                            if (resultado.contains("error")) {
                                Toast toast = Toast.makeText(getContext(), "Ha ocurrido un error interno", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                                toast.show();
                            } else {
                                Intent i=new Intent(getActivity(),LoginActivity.class);
                                getActivity().finish();

                                startActivity(i);
                            }

                        }
                    }
                });
        WorkManager.getInstance(getActivity()).enqueueUniqueWork("eliminarToken" + id, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

}
