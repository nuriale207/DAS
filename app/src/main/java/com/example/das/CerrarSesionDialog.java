package com.example.das;

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

public class CerrarSesionDialog extends DialogFragment {
    //Diálogo que pregunta al usuario si realmente quiere cerrar sesión
//    public interface ListenerdelDialogoIniciarSesion {
//        void alpulsarCerrarSesion();
//
//    }
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

                BDLocal gestorDB = new BDLocal (getActivity(), "DAS", null, 1);
                SQLiteDatabase bd = gestorDB.getWritableDatabase();

                bd.delete("Usuarios",null,null);
                bd.delete("Mensajes",null,null);

                editor.apply();
                //eliminarTokenFCM();
                Intent i=new Intent(getActivity(),LoginActivity.class);
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

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=" + "editarToken" + "&id=" + id + "&" + "token="+"")
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("actualizar" + id).build();
        WorkManager.getInstance(this.getActivity()).getWorkInfoByIdLiveData(requesContrasena.getId())
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
                                startActivity(new Intent(getActivity(), LoginActivity.class));
                                getActivity().finish();
                            }

                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getActivity()).enqueueUniqueWork("actualizar" + id, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

}
