package com.example.das.registroLogin;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.das.R;
import com.example.das.chats.ChatActivity;
import com.example.das.mapa.MainActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {
    private ImageView imagen;
    private Button botonImagen;
    private EditText nombre;
    private EditText fechaNacimiento;
    private EditText genero;
    private EditText ubicacion;
    private Button registro;
    private String id;
    private FirebaseAuth firebaseAuth;
    private String id_FCM;
    //Firebase
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private double latitud;
    private double longitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);



        //Cargar elementos del layout
        imagen = findViewById(R.id.imageView);
        botonImagen = findViewById(R.id.añadirImagen);
        nombre = findViewById(R.id.editNombreUsuario);
        fechaNacimiento = findViewById(R.id.editFechaNacimiento);
        genero = findViewById(R.id.editGenero);
        registro = findViewById(R.id.registrarse);
        ubicacion = findViewById(R.id.editUbicacion);

        //Se obtiene el id de los extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getString("id");

        }

        Log.i("MYAPP", "El id del usuario es " + id);

        //En caso de haber girado la pantalla se añaden los valores escritos previamente
        if (savedInstanceState != null) {
            nombre.setText(savedInstanceState.getString("nombre"));
            fechaNacimiento.setText(savedInstanceState.getString("fechaNacimiento"));
            genero.setText(savedInstanceState.getString("genero"));
        }

        //Añadir listeners a los campos
        //Al hacer click en fecha de nacimiento se abre un dialog para seleccionar la fecha
        fechaNacimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }

        });
        //Al hacer click en el genero se abre un dialog para seleccionar el género
        genero.setOnClickListener(new View.OnClickListener() {
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
                        genero.setText(opciones[which]);
                    }
                });
                builder.show();
            }
        });

        //Al hacer click en el botón de la imagen se abre un dialog para seleccionar si se quiere añadir la imagen de la galería o de la cámara
        botonImagen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /***
                 * Código obtenido de:https://stackoverflow.com/questions/16389581/android-create-a-popup-that-has-multiple-selection-options
                 * Usuario: https://stackoverflow.com/users/1274911/zbr
                 */
                String[] opciones = {"Hacer una foto", "Elegir de la galería"};

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Añadir imagen");
                builder.setItems(opciones, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user clicked on colors[which]
                        if (which == 0) {
                            //Si se elige el primero se solicita permiso para usar la cámara
                            solicitarPermisoCamara();
                        } else {
                            //Se solicita el permiso para abrir la galería
                            solicitarPermisoGaleria();
                        }
                    }
                });
                builder.show();
            }
        });

        //Se obtiene el nuevo id de FCM del dispositivo
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        //En caso de error se imprime
                        if (!task.isSuccessful()) {
                            Log.i("MYAPP", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        id_FCM = token;
                        Log.i("id_FCM: ", token);


                    }
                });

        //al hacer click sobre registro se almacenan los datos en la BD y se pasa al siguiente paso
        registro.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                añadirUsuario(nombre.getText().toString(), fechaNacimiento.getText().toString(), genero.getText().toString(), ubicacion.getText().toString());

            }
        });

        //Al pulsar sobre ubicación se rellena el campo automáticamente tras dar el permiso de ubicación
        ubicacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MYAPP", "Obteniendo ubicacion...");
                obtenerUbicacion();

            }
        });

    }

    private void solicitarPermisoUbicacion() {
        //En caso de que el permiso esté denegado se solicita
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    3);

        }
    }

    private void obtenerUbicacion() {
        //Se obtiene la posición del usuario
        FusedLocationProviderClient proveedordelocalizacion =
                LocationServices.getFusedLocationProviderClient(this);
        //En caso de que el permiso esté denegado se solicita
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast toast = Toast.makeText(getApplicationContext(), "Es necesario aceptar el permiso de ubicación para usar la aplicación", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    3);
        } else {
            //Se obtiene el nombre de la localización actual
            proveedordelocalizacion.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Log.i("MYAPP", "Latitud: " + location.getLatitude());
                                Log.i("MYAPP", "Longitud: " + location.getLongitude());
                                //Se obtienen las cooordenadas
                                longitud = location.getLongitude();
                                latitud = location.getLatitude();
                                //Se busca el nombre de la ubicación
                                Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
                                try {
                                    List<Address> addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addresses.size() >= 1) {
                                        ubicacion.setText(addresses.get(0).getLocality());

                                    } else {
                                        //Si se desconoce el punto, se escribe población desconocida
                                        ubicacion.setText("Población desconocida");
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            } else {
                                //En caso de error se muestra un mensaje
                                Log.i("MYAPP", "Latitud: (desconocida)");
                                Log.i("MYAPP", "Longitud: (desconocida)");
                                Toast toast = Toast.makeText(getApplicationContext(), "Error al obtener la ubicación intentalo de nuevo más tarde", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //En caso de error se muestra un mensaje

                            Log.i("MYAPP", "Error al obtener la posición");
                            Toast toast = Toast.makeText(getApplicationContext(), "Error al obtener la ubicación intentalo de nuevo más tarde", Toast.LENGTH_LONG);
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
        //Método que muestra el datePickerDialog para ello hace uso de datePickerFragment
        DatePickerFragment newFragment = DatePickerFragment.newInstance(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                // +1 because January is zero
                //final String selectedDate = day + " / " + (month+1) + " / " + year;
                final String selectedDate = year + "-" + (month + 1) + "-" + day;
                Log.i("MY", selectedDate);
                //Al seleccionar la fecha se muestra en el editText fecha
                fechaNacimiento.setText(selectedDate);
            }
        });

        newFragment.show(this.getSupportFragmentManager(), "datePicker");
    }

    private void solicitarPermisoGaleria() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO


            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR

            }
            //PEDIR EL PERMISO
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    2);

        } else {
            //EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD

            Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, 2);

        }
    }

    public void solicitarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO
                Toast toast = Toast.makeText(getApplicationContext(), "Acepta el permiso para poder usar la cámara", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                toast.show();


            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR
                Toast toast = Toast.makeText(getApplicationContext(), "Acepta el permiso para poder usar la cámara", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                toast.show();
            }
            //PEDIR EL PERMISO
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    1);

        } else {
            //EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD

            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, 1);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // Si la petición se cancela, granResults estará vacío
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // PERMISO DE LA CÁMARA CONCEDIDO, SE LANZA LA CÁMARA
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);


                } else {
                    // PERMISO DENEGADO, DESHABILITAR LA FUNCIONALIDAD O EJECUTAR ALTERNATIVA
                    Toast toast = Toast.makeText(getApplicationContext(), "Acepta el permiso para poder usar la cámara", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return;
            }
            case 2: {
                // Si la petición se cancela, granResults estará vacío
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // PERMISO GALERÍA CONCEDIDO, SE ABRE LA GALERÍA
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    galleryIntent.setType("image/*");
                    startActivityForResult(galleryIntent, 2);


                } else {
                    // PERMISO DENEGADO, DESHABILITAR LA FUNCIONALIDAD O EJECUTAR ALTERNATIVA
                    Toast toast = Toast.makeText(getApplicationContext(), "Acepta el permiso para poder acceder a la galería", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return;
            }

            case 3: {
                obtenerUbicacion();
            }


        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            //Al hacer una foto se muestra en el imageView
            //Código de:https://stackoverflow.com/questions/5991319/capture-image-from-camera-and-display-in-activity
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            Bitmap resized = Bitmap.createScaledBitmap(photo, imagen.getWidth(), imagen.getHeight(), true);

            imagen.setImageBitmap(resize(photo,imagen.getWidth(),imagen.getHeight()));
        } else {
            final Uri imageUri = data.getData();
            final InputStream imageStream;
            try {
                //Código de:https://stackoverflow.com/questions/38352148/get-image-from-the-gallery-and-show-in-imageview
                //Al seleccionar la imagen se muestra en el imageView
                imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                Bitmap resized = Bitmap.createScaledBitmap(selectedImage, imagen.getWidth(), imagen.getHeight(), true);

                imagen.setImageBitmap(resize(selectedImage,imagen.getWidth(),imagen.getHeight()));
                //imagen.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }
    //Método que adapta las dimensiones de una imagen al imageView
    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void añadirUsuario(String nombre, String fecha, String genero, String ubicacion) {
        //Método que añade el usuario a la BD remota haciendo una solicitud a un Worker
        boolean valido = comprobarFormulario(nombre, fecha, genero, ubicacion);
        if (valido) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String direccion = "http://ec2-54-242-79-204.compute-1.amazonaws.com/nlebena001/WEB/DAS_users.php";
                    Log.i("MYAPP", direccion);

                    //Los parámetros de la consulta los recibe de los input data
                    String parametros = "funcion=insertarUsuario&nombreUsuario=" + nombre + "&fechaNacimiento=" + fecha + "&genero=" + genero + "&id=" + id + "&id_FCM=" + id_FCM + "&longitud=" + longitud + "&latitud=" + latitud ;
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
                            //Si los datos se han almacenado correctamente, se almacena la imagen y se abre la actividad de intereses
                            if(!result.contains("error")){
                                guardarImagen();
                                SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                preferencias.edit().putString("id", id).apply();
                                Intent i = new Intent(getApplicationContext(), InteresesActivity.class);
                                startActivity(i);
                                finish();
                            }

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
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean comprobarFormulario(String nombre, String fechaNacimiento, String genero, String ubicacion) {
        //Método que comprueba si los datos del usuario son correctos
        boolean valido = true;

        //Se calcula la edad del usuario
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        long difference = 0;
        try {
            Date fechNac = sdfDate.parse(fechaNacimiento);
            difference = now.getYear() - fechNac.getYear();

        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (nombre.length() < 2) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "El nombre tiene que tener al menos dos caracteres", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        }
        else if(nombre.length()>40){
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "El nombre tiene que tener menos de 40 caracteres", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        }
        else if (fechaNacimiento.length() == 0) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "Introduce tu fecha de nacimiento", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        } else if (difference < 16) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "Debes tener al menos 16 años para usar la aplicación", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        } else if (genero.length() == 0) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "Selecciona un género", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        } else if (ubicacion.length() == 0) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "Es necesario indicar la ubicación para usar la aplicación", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        }

        return valido;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        //Se guardan los datos incluidos hasta el momento
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("nombre", nombre.getText().toString());
        savedInstanceState.putString("fechaNacimiento", fechaNacimiento.getText().toString());
        savedInstanceState.putString("genero", genero.getText().toString());
        imagen.buildDrawingCache();
        Bitmap bitmap = imagen.getDrawingCache();

        //BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        // Bitmap bitmap = drawable.getBitmap();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, bs);
        savedInstanceState.putByteArray("imagen", bs.toByteArray());
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

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        //if the upload is not successful
                        //hiding the progress dialog
                        //and displaying error message
                        Toast.makeText(getApplication(), exception.getCause().getLocalizedMessage(), Toast.LENGTH_LONG).show();
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

    public byte[] getByteArray() {
        // Obtiene el byte[] del imageView
        this.imagen.setDrawingCacheEnabled(true);
        imagen.buildDrawingCache();
        Bitmap bitmap = imagen.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        return data;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i=new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(i);
        finish();
    }

}