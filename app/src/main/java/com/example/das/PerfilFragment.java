package com.example.das;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class PerfilFragment extends Fragment {
    private EditText editNombre;
    private EditText editGenero;
    private EditText editEdad;
    private EditText editUbicacion;


    private Button cambiarNombre;
    private Button cambiarGenero;
    private Button cambiarEdad;
    private Button cambiarUbicacion;

    private ImageView imagen;

    private String id;

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        //al crear la actividad se carga la lista de ranking
        super.onActivityCreated(savedInstanceState);
        editNombre = getView().findViewById(R.id.editTextNombre);
        editGenero = getView().findViewById(R.id.editTextGenero);
        editEdad = getView().findViewById(R.id.editTextEdad);
        editUbicacion = getView().findViewById(R.id.editTextUbicacion);

        cambiarNombre = getView().findViewById(R.id.button);
        cambiarEdad = getView().findViewById(R.id.button2);
        cambiarGenero = getView().findViewById(R.id.button3);
        cambiarUbicacion = getView().findViewById(R.id.button4);


        imagen = getView().findViewById(R.id.imageView3);
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        id = preferencias.getString("id", null);
        String nombre = preferencias.getString("nombre", null);
        if (nombre == null) {
            cargarPerfil();
            cargarImagen();
        } else {
            editNombre.setText(preferencias.getString("nombre", ""));
            editEdad.setText(preferencias.getString("edad", ""));
            editGenero.setText(preferencias.getString("genero", ""));
            editUbicacion.setText(preferencias.getString("ubicacion", ""));
            cargarImagen();

        }

        cambiarEdad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        cambiarGenero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /***
                 * Código obtenido de:https://stackoverflow.com/questions/16389581/android-create-a-popup-that-has-multiple-selection-options
                 * Usuario: https://stackoverflow.com/users/1274911/zbr
                 */
                String[] opciones = {"Hombre", "Mujer", "No binario"};

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Género");
                builder.setItems(opciones, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user clicked on colors[which]
                        actualizar("editarGenero", "genero=" + opciones[which]);

                    }
                });
                builder.show();
            }

        });

        cambiarUbicacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obtenerUbicacion();


            }
        });
        cambiarNombre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextField = new EditText(v.getContext());

                AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                        .setTitle("Cambiar nombre")
                        .setMessage("Escribe el nuevo nombre")
                        .setView(editTextField)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String editTextInput = editTextField.getText().toString();
                                Log.d("onclick", "editext value is: " + editTextInput);
                                actualizar("editarNombre","nombre="+editTextInput);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .create();
                dialog.show();
            }

        });
    }

        private void obtenerUbicacion() {
            //Se obtiene la posición del usuario
            FusedLocationProviderClient proveedordelocalizacion =
                    LocationServices.getFusedLocationProviderClient(this.getContext());
            if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast toast = Toast.makeText(getActivity(), "Es necesario aceptar el permiso de ubicación para usar la aplicación", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                toast.show();

                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        3);
            }
            else{
                proveedordelocalizacion.getLastLocation()
                        .addOnSuccessListener(this.getActivity(), new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    Log.i("MYAPP","Latitud: " + location.getLatitude());
                                    Log.i("MYAPP","Longitud: " + location.getLongitude());
                                    double longitud=location.getLongitude();
                                    double latitud=location.getLatitude();
                                    Geocoder gcd = new Geocoder(getActivity(),Locale.getDefault());
                                    try {
                                        List<Address> addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                        if(addresses.size()>=1){
                                            String ubicacion=addresses.get(0).getLocality();
                                            actualizar("editUbicacion", "longitud="+longitud+"&latitud="+latitud);

                                        }

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }


                                } else {
                                    Log.i("MYAPP","Latitud: (desconocida)");
                                    Log.i("MYAPP","Longitud: (desconocida)");
                                    Toast toast = Toast.makeText(getActivity(), "Error al obtener la ubicación intentalo de nuevo más tarde", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                                    toast.show();
                                }
                            }
                        })
                        .addOnFailureListener(this.getActivity(), new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i("MYAPP","Error al obtener la posición");
                                Toast toast = Toast.makeText(getActivity(), "Error al obtener la ubicación intentalo de nuevo más tarde", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        });
            }

        }


    /***
     * Código obtenido de: https://programacionymas.com/blog/como-pedir-fecha-android-usando-date-picker
     */
    private void showDatePickerDialog() {
        DatePickerFragment newFragment = DatePickerFragment.newInstance(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                // +1 because January is zero
                //final String selectedDate = day + " / " + (month+1) + " / " + year;
                final String selectedDate = year + "-" + (month+1) + "-" + day;
                Log.i("MY",selectedDate);
                actualizar("editarFechaNacimiento", "fecha="+selectedDate);


            }
        });

        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
    }

    private void actualizar(String funcion, String campo) {
        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion="+funcion+"&id="+id+"&"+campo)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("actualizar"+funcion).build();
        WorkManager.getInstance(this.getActivity()).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);

                            if(resultado.contains("error")){
                                Toast toast = Toast.makeText(getActivity(), "Ha ocurrido un error al actualizar el campo", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                            else{
                                cargarPerfil();
                            }

                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getActivity()).enqueueUniqueWork("actualizar"+funcion, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    private void cargarImagen() {
        //Metodo que carga la imagen de Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child("images/"+id+".jpg");
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getActivity()).load(uri).into(imagen);
            }
        });
    }

    private void cargarPerfil() {

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=datosUsuario&id=" +id)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosUsuario").build();
        WorkManager.getInstance(this.getActivity()).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);
                            try {
                                JSONObject jsonObject = new JSONObject(resultado);
                                String nombre=jsonObject.getString("nombre");
                                String edad=jsonObject.getString("edad");
                                String genero=jsonObject.getString("genero");
                                editNombre.setText(nombre);
                                editEdad.setText(edad);
                                editGenero.setText(genero);
                                double longitud=jsonObject.getDouble("longitud");
                                double latitud=jsonObject.getDouble("latitud");


                                Geocoder gcd = new Geocoder(getActivity(), Locale.getDefault());
                                String ubicacion="";
                                try {
                                    List<Address> addresses = gcd.getFromLocation(latitud, longitud, 1);
                                    if(addresses.size()>=1){

                                        ubicacion=addresses.get(0).getLocality();
                                        editUbicacion.setText(ubicacion);

                                    }
                                    else{
                                        ubicacion="Población desconocida";
                                        editUbicacion.setText(ubicacion);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                SharedPreferences.Editor editor = preferencias.edit();
                                editor.putString("nombre",nombre);
                                editor.putString("edad",edad);
                                editor.putString("genero",genero);
                                editor.putString("ubicacion",ubicacion);
                                editor.apply();

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }



                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getActivity()).enqueueUniqueWork("getDatosUsuario", ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }
}