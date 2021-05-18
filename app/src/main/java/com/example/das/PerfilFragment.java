package com.example.das;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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
    private Button verDescripcion;
    private Button verIntereses;
    private Button cambiarImagen;
    private Button cerrarSesion;

    private SeekBar barraDistancia;
    private TextView textoBarraDistancia;

    private ImageView imagen;

    //Firebase
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private String id;
    private String descripcion;
    private String interesesUsuario;
    private ArrayList<String> listaIntereses;
    private int distancia;


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
        cambiarImagen=getView().findViewById(R.id.button7);
        verDescripcion=getView().findViewById(R.id.button5);
        verIntereses=getView().findViewById(R.id.button6);
        cerrarSesion=getView().findViewById(R.id.button10);
        barraDistancia=getView().findViewById(R.id.seekBar);
        textoBarraDistancia=getView().findViewById(R.id.seekBarText);
        imagen = getView().findViewById(R.id.imageView3);
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        id = preferencias.getString("id", null);
        String nombre = preferencias.getString("nombre", "null");

        if (nombre.equals("null")) {
            cargarPerfil();
            obtenerIntereses(true);
            cargarImagen();
            distancia=preferencias.getInt("distancia",20);

        } else {
            editNombre.setText(preferencias.getString("nombre", ""));
            editEdad.setText(preferencias.getString("edad", ""));
            editGenero.setText(preferencias.getString("genero", ""));
            editUbicacion.setText(preferencias.getString("ubicacion", ""));
            descripcion = preferencias.getString("descripcion", "");
            interesesUsuario=preferencias.getString("intereses", "");
            distancia=preferencias.getInt("distancia",20);
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
                editTextField.setText(editNombre.getText().toString());
                AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                        .setTitle("Cambiar nombre")
                        .setMessage("Escribe el nuevo nombre")
                        .setView(editTextField)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String editTextInput = editTextField.getText().toString();
                                Log.d("onclick", "editext value is: " + editTextInput);
                                actualizar("editarNombre", "nombre=" + editTextInput);
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
        verDescripcion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextField = new EditText(v.getContext());
                editTextField.setText(descripcion);
                editTextField.setEnabled(false);

                AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                        .setTitle("Descripción")
                        .setView(editTextField)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String editTextInput = editTextField.getText().toString();
                                Log.d("onclick", "editext value is: " + editTextInput);
                                actualizar("anadirDescripcion", "descripcion=" + editTextInput);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setNeutralButton("Editar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //editTextField.setEnabled(true);
//                                String editTextInput = editTextField.getText().toString();
//                                Log.d("onclick", "editext value is: " + editTextInput);
                            }
                        })
                        .create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        dialog.setMessage("Escribe una nueva descripción");
                        editTextField.setEnabled(true);
//                        Boolean wantToCloseDialog = false;
//                        //Do stuff, possibly set wantToCloseDialog to true then...
//                        if(wantToCloseDialog)
//                            dialog.dismiss();
//                        //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                    }
                });

            }
        });

        verIntereses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String[] opciones =interesesUsuario.split("#");

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Intereses");
                builder.setItems(opciones, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user clicked on colors[which]

                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setNeutralButton("Editar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        obtenerIntereses(false);

                        //editTextField.setEnabled(true);
//                                String editTextInput = editTextField.getText().toString();
//                                Log.d("onclick", "editext value is: " + editTextInput);

                    }
                });
                builder.show();

            }
        });
        cambiarImagen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String[] opciones = {"Hacer una foto", "Elegir de la galería"};

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Añadir imagen");
                builder.setItems(opciones, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user clicked on colors[which]
                        if (which == 0) {
                            solicitarPermisoCamara();
                        } else {
                            solicitarPermisoGaleria();
                        }
                    }
                });
                builder.show();
            }
        });
        cerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CerrarSesionDialog dialogoIniciarSesion=new CerrarSesionDialog();
                dialogoIniciarSesion.show(getActivity().getSupportFragmentManager(), "etiqueta");

            }
        });
        barraDistancia.setProgress(distancia);
        textoBarraDistancia.setText(distancia+"Km");

        barraDistancia.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textoBarraDistancia.setText(String.valueOf(progress)+"Km");
                preferencias.edit().putInt("distancia",progress).apply();
                ((MainActivity)getActivity()).centrar();
                actualizar("editarDistancia","distancia="+progress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        actualizar("editarToken", "token=" + Firebase.getToken(getContext())+"&sesion=1");


    }
    private void solicitarPermisoGaleria() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO


            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR

            }
            //PEDIR EL PERMISO
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    2);

        } else {
            //EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD

            Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, 2);

        }
    }

    public void solicitarPermisoCamara(){
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO


            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR

            }
            //PEDIR EL PERMISO
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA},
                    1);

        } else {
            //EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD

            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, 1);

        }
    }

    private void abrirDialogoIntereses() {

        ArrayList<Integer> seleccion = new ArrayList<Integer>();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Intereses");
        String[] opciones = listaIntereses.toArray(new String[listaIntereses.size()]);

        builder.setMultiChoiceItems(opciones, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                seleccion.add(i);
            }
        });
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                String texto = "";
                for (int i = 0; i < seleccion.size(); i++) {
                    Log.i("MYAPP", String.valueOf(seleccion.get(i)));
                    texto = texto + "#" + opciones[seleccion.get(i)];
                }

                actualizar("anadirIntereses","intereses="+texto);

                dialog.cancel();
            }


        });
        Dialog dialog = builder.create();
        dialog.show();
    }

    private void obtenerIntereses(Boolean usuario) {
        Data datos;
        if(usuario){
            datos = new Data.Builder()
                    .putString("fichero", "DAS_users.php")
                    .putString("parametros", "funcion=obtenerInteresesUsuario&id=" + id)
                    .build();
        }
        else{
            listaIntereses=new ArrayList<String>();
            datos = new Data.Builder()
                    .putString("fichero", "DAS_users.php")
                    .putString("parametros", "funcion=obtenerIntereses")
                    .build();
        }

        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosIntereses").build();
        WorkManager.getInstance(this.getActivity()).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP",resultado);
                            JSONArray jsonArray = null;
                            interesesUsuario ="";
                            try {
                                jsonArray = new JSONArray(resultado);
                                for(int x = 0; x < jsonArray.length(); x++) {
                                    JSONObject elemento = jsonArray.getJSONObject(x);
                                    if(usuario){
                                        if (!String.valueOf(elemento).contains("null")){
                                            interesesUsuario = interesesUsuario +"#"+elemento.getString("interes");

                                        }
                                    }
                                    else{
                                        listaIntereses.add(elemento.getString("interes"));
                                    }

                                    //interesesUsuario = interesesUsuario.substring(0, interesesUsuario.length()-2);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (usuario){
                                SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                SharedPreferences.Editor editor = preferencias.edit();
                                editor.putString("intereses", interesesUsuario);



                                editor.apply();
                            }
                            else{
                                abrirDialogoIntereses();

                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getActivity()).enqueueUniqueWork("getDatosIntereses", ExistingWorkPolicy.REPLACE, requesContrasena);
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
        } else {
            proveedordelocalizacion.getLastLocation()
                    .addOnSuccessListener(this.getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Log.i("MYAPP", "Latitud: " + location.getLatitude());
                                Log.i("MYAPP", "Longitud: " + location.getLongitude());
                                double longitud = location.getLongitude();
                                double latitud = location.getLatitude();
                                Geocoder gcd = new Geocoder(getActivity(), Locale.getDefault());
                                try {
                                    List<Address> addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addresses.size() >= 1) {
                                        String ubicacion = addresses.get(0).getLocality();
                                        editUbicacion.setText(ubicacion);
                                        actualizar("editarUbicacion", "longitud=" + longitud + "&latitud=" + latitud);

                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            } else {
                                Log.i("MYAPP", "Latitud: (desconocida)");
                                Log.i("MYAPP", "Longitud: (desconocida)");
                                Toast toast = Toast.makeText(getActivity(), "Error al obtener la ubicación intentalo de nuevo más tarde", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        }
                    })
                    .addOnFailureListener(this.getActivity(), new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i("MYAPP", "Error al obtener la posición");
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
                final String selectedDate = year + "-" + (month + 1) + "-" + day;
                Log.i("MY", selectedDate);
                actualizar("editarFechaNacimiento", "fecha=" + selectedDate);


            }
        });

        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
    }

    private void actualizar(String funcion, String campo) {
        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=" + funcion + "&id=" + id + "&" + campo)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("actualizar" + funcion).build();
        WorkManager.getInstance(this.getActivity()).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);

                            if (resultado.contains("error")) {
                                Toast toast = Toast.makeText(getActivity(), "Ha ocurrido un error al actualizar el campo", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                                toast.show();
                            } else {
                                cargarPerfil();
                                obtenerIntereses(true);
                            }

                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getActivity()).enqueueUniqueWork("actualizar" + funcion, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    private void cargarImagen() {
        //Metodo que carga la imagen de Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child("images/" + id + ".jpg");
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
                .putString("parametros", "funcion=datosUsuario&id=" + id)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosUsuarioPerfil").build();
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
                                String nombre = jsonObject.getString("nombre");
                                String edad = jsonObject.getString("edad");
                                String genero = jsonObject.getString("genero");
                                String descripcionStr = jsonObject.getString("descripcion");
                                if(descripcionStr.equals("null")){
                                    descripcionStr="";
                                }
                                descripcion = descripcionStr;
                                Log.i("MYAPP", descripcionStr);
                                editNombre.setText(nombre);
                                editEdad.setText(edad);
                                editGenero.setText(genero);
                                double longitud = jsonObject.getDouble("longitud");
                                double latitud = jsonObject.getDouble("latitud");


                                Geocoder gcd = new Geocoder(getActivity(), Locale.getDefault());
                                String ubicacion = "";
                                try {
                                    List<Address> addresses = gcd.getFromLocation(latitud, longitud, 1);
                                    if (addresses.size() >= 1) {

                                        ubicacion = addresses.get(0).getLocality();
                                        editUbicacion.setText(ubicacion);

                                    } else {
                                        ubicacion = "Población desconocida";
                                        editUbicacion.setText(ubicacion);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                SharedPreferences.Editor editor = preferencias.edit();
                                editor.putString("nombre", nombre);
                                editor.putString("edad", edad);
                                editor.putString("genero", genero);
                                editor.putString("ubicacion", ubicacion);
                                editor.putString("descripcion", descripcionStr);
                                editor.putString("token", Firebase.getToken(getContext()));

                                editor.apply();



                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getActivity()).enqueueUniqueWork("getDatosUsuarioPerfil", ExistingWorkPolicy.REPLACE, requesContrasena);
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
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            //Código de:https://stackoverflow.com/questions/5991319/capture-image-from-camera-and-display-in-activity
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imagen.setImageBitmap(photo);
            guardarImagen();
//            BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
//            Bitmap bitmap = drawable.getBitmap();
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//            byte[] fototransformada = stream.toByteArray();
//            String fotoen64 = Base64.encodeToString(fototransformada, Base64.DEFAULT);
//            actualizarImagenBD(fotoen64);
        }
        else{
            final Uri imageUri = data.getData();
            final InputStream imageStream;
            try {
                //Código de:https://stackoverflow.com/questions/38352148/get-image-from-the-gallery-and-show-in-imageview
                imageStream = getActivity().getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imagen.setImageBitmap(selectedImage);
                guardarImagen();

//                BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
//                Bitmap bitmap = drawable.getBitmap();
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                byte[] fototransformada = stream.toByteArray();
//                String fotoen64 = Base64.encodeToString(fototransformada, Base64.DEFAULT);
//                actualizarImagenBD(fotoen64);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    private void guardarImagen() {
        //Método que almacena la imagen en Firebase storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        StorageReference imageRef = storageReference.child("images" + "/" + id + ".jpg");

        imageRef.putBytes(getByteArray())
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //if the upload is successful
                        //hiding the progress dialog
                        //and displaying a success toast
                        //String profilePicUrl = taskSnapshot.getDownloadUrl().toString();
                        Toast.makeText(getContext(), "La imagen se ha actualizado correctamente", Toast.LENGTH_LONG).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        //if the upload is not successful
                        //hiding the progress dialog
                        //and displaying error message
                        Toast.makeText(getContext(), exception.getCause().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        //calculating progress percentage
//                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
//                        //displaying percentage in progress dialog
//                        progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                    }
                });
    }

    private void actualizarImagenBD(String fotoen64) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/nlebena001/WEB/DAS_users.php";
                Log.i("MYAPP", direccion);

                //Los parámetros de la consulta los recibe de los input data
                String parametros = "funcion=editarImagen&"+ "id=" + id + "&imagen=" + fotoen64;
                Log.i("MYAPP", parametros);

                HttpURLConnection urlConnection = null;
                String result = "";
                try {
                    //Se realiza la conexión
                    URL destino = new URL(direccion);
                    urlConnection = (HttpURLConnection) destino.openConnection();
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setReadTimeout(5000);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setDoOutput(true);

                    //Se añaden los parámetros a la petición
                    PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                    out.print(parametros);
                    out.close();
                    Log.i("MYAPP", "vAMOS A HACER EL POST");


                    //En caso de obtener los datos correctamente se almacenan en una variable result que se devuelve a la clase que hizo la petición
                    int statusCode = urlConnection.getResponseCode();
                    Log.i("MYAPP", String.valueOf(statusCode));
                    Log.i("MYAPP", urlConnection.getResponseMessage());
                    if (statusCode == 200) {
                        Log.i("MYAPP", "CONEXION ESTABLECIDA");

                        BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        String line = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            result += line;
                        }
                        inputStream.close();





                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i("MYAPP", result);
                Data resultados = new Data.Builder()
                        .putString("resultado", result)
                        .build();
            }
        }).start();
    }

    public byte[] getByteArray(){
        // Get the data from an ImageView as bytes
        this.imagen.setDrawingCacheEnabled(true);
        imagen.buildDrawingCache();
        Bitmap bitmap = imagen.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        return data;
    }

//    @Override
//    public void alpulsarCerrarSesion() {
//        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getActivity());
//        SharedPreferences.Editor editor = preferencias.edit();
//        editor.remove("id");
//        editor.remove("nombre");
//        editor.remove("edad");
//        editor.remove("genero");
//        editor.remove("ubicacion");
//        editor.remove("descripcion");
//        editor.remove("intereses");
//
//        editor.apply();
//
//    }
}