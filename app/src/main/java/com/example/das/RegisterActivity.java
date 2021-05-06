package com.example.das;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RegisterActivity extends AppCompatActivity {
    private ImageView imagen;
    private Button botonImagen;
    private EditText nombre;
    private EditText fechaNacimiento;
    private EditText genero;
    private Button registro;
    private String id;

    //Firebase
    private FirebaseStorage storage;
    private StorageReference storageReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        String currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.i("MY",currentuser);

        //Cargar elementos del layout
        imagen=findViewById(R.id.imageView);
        botonImagen=findViewById(R.id.añadirImagen);
        nombre=findViewById(R.id.editNombreUsuario);
        fechaNacimiento=findViewById(R.id.editFechaNacimiento);
        genero=findViewById(R.id.editGenero);
        registro=findViewById(R.id.registrarse);

        //Se obtiene el id de los extras
        Bundle extras=getIntent().getExtras();
        if (extras!=null){
            id=extras.getString("id");

        }

        //En caso de haber girado la pantalla se añaden los valores escritos previamente
        if(savedInstanceState!=null){
            nombre.setText(savedInstanceState.getString("nombre"));
            fechaNacimiento.setText(savedInstanceState.getString("fechaNacimiento"));
            genero.setText(savedInstanceState.getString("genero"));

//            Bitmap b = BitmapFactory.decodeByteArray(
//                    getIntent().getByteArrayExtra("imagen"),0,getIntent().getByteArrayExtra("byteArray").length);
//           imagen.setImageBitmap(b);
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
                        if(which==0){
                            solicitarPermisoCamara();
                        }
                        else{
                            solicitarPermisoGaleria();
                        }
                    }
                });
                builder.show();
            }
        });

        registro.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                añadirUsuario(nombre.getText().toString(),fechaNacimiento.getText().toString(),genero.getText().toString());

            }
        });

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

    public void solicitarPermisoCamara(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO


            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR

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
                    // PERMISO CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
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
                    // PERMISO CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
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


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            //Código de:https://stackoverflow.com/questions/5991319/capture-image-from-camera-and-display-in-activity
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imagen.setImageBitmap(photo);
        }
        else{
            final Uri imageUri = data.getData();
            final InputStream imageStream;
            try {
                //Código de:https://stackoverflow.com/questions/38352148/get-image-from-the-gallery-and-show-in-imageview
                imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imagen.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void añadirUsuario(String nombre, String fecha, String genero) {
        //Método que añade el usuario a la BD remota haciendo una solicitud a un Worker
        boolean valido= comprobarFormulario(nombre, fecha, genero);
        if (valido) {

//            Toast toast = Toast.makeText(getApplicationContext(), "Las contraseñas no coinciden", Toast.LENGTH_LONG);
//            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
//            toast.show();
//        } else {
            Data datos = new Data.Builder()
                    .putString("fichero", "DAS_users.php")
                    .putString("parametros", "funcion=insertarUsuario&nombreUsuario=" + nombre + "&fechaNacimiento=" + fecha + "&genero=" + genero+"&id="+id)
                    .build();
            OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("existeUsuario").build();
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                    .observe(this, new Observer<WorkInfo>() {
                        @Override
                        public void onChanged(WorkInfo workInfo) {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                String resultado = workInfo.getOutputData().getString("resultado");
                                Log.i("MYAPP", "inicio realizado");

                                Log.i("MYAPP", resultado);
                                if (resultado.contains("error")){
                                    Toast toast=Toast.makeText(getApplicationContext(),"Ya existe un usuario con ese id", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM| Gravity.CENTER, 0, 0);
                                    toast.show();
                                }
                                else{
                                    guardarImagen();
                                    Toast toast=Toast.makeText(getApplicationContext(),"Usuario registrado", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM| Gravity.CENTER, 0, 0);
                                    toast.show();
                                    SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    preferencias.edit().putString("id",id).apply();

                                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(i);
                                    finish();


                                }


                            }
                        }
                    });
            //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
            WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("existeUsuario", ExistingWorkPolicy.REPLACE, requesContrasena);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean comprobarFormulario(String nombre, String fechaNacimiento, String genero) {
        boolean valido = true;

        //Se calcula la edad del usuario

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        long difference=0;
        try {
            Date fechNac=sdfDate.parse(fechaNacimiento);
            difference = now.getYear()-fechNac.getYear();

        } catch (ParseException e) {
            e.printStackTrace();
        }


//        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        LocalDate fechaNac = LocalDate.parse(fechaNacimiento, fmt);
//        LocalDate ahora = LocalDate.now();
//
//        Period periodo = Period.between(fechaNac, ahora);
//        System.out.printf("Tu edad es: %s años, %s meses y %s días",
//                periodo.getYears(), periodo.getMonths(), periodo.getDays());
        if (nombre.length() < 2) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "El nombre tiene que tener al menos dos caracteres", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        } else if (fechaNacimiento.length() == 0) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "Introduce tu fecha de nacimiento", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        } else if (difference< 16) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "Debes tener al menos 16 años para usar la aplicación", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        } else if (genero.length() == 0) {
            valido = false;
            Toast toast = Toast.makeText(getApplicationContext(), "Selecciona un género", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
            toast.show();
        }

    return valido;
    }

    @Override
    protected void onSaveInstanceState (Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("nombre",nombre.getText().toString());
        savedInstanceState.putString("fechaNacimiento",fechaNacimiento.getText().toString());
        savedInstanceState.putString("genero",genero.getText().toString());
        imagen.buildDrawingCache();
        Bitmap bitmap = imagen.getDrawingCache();

        //BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
       // Bitmap bitmap = drawable.getBitmap();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, bs);
        savedInstanceState.putByteArray("imagen",bs.toByteArray());
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
                        Toast.makeText(getApplication(), "La imagen se ha subido con éxito", Toast.LENGTH_LONG).show();

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

}