package com.example.das.mapa;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.das.bd.BDLocal;
import com.example.das.bd.ConexionBDWorker;
import com.example.das.chats.Firebase;
import com.example.das.R;
import com.example.das.registroLogin.LoginActivity;
import com.example.das.registroLogin.RegisterActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private FirebaseAuth firebaseAuth;
    private String id;
    private JSONArray usuarios;
    private LatLng coordenadasActuales;
    private double distanciaMax=18;

    private GoogleMap miMapa;
    private Button boton;
    private TabHost tabhost;

    FusedLocationProviderClient proveedordelocalizacion;
    LocationCallback actualizador;
    Marker usuario;
    SharedPreferences preferencias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Se comprueba si hay un usuario logeado. En caso de no haberlo se abre la actividad de login
        preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        id = preferencias.getString("id", null);
        //int tab=preferencias.getInt("tab",0);
        int distancia=preferencias.getInt("distancia",0);
        if (id == null) {
            // En caso de no haber un usuario loggeado se abre la actividad de login
            Intent i = new Intent(this, LoginActivity.class);
            finish();

            startActivity(i);
        }

        //Se mira si hay una distancia máxima establecida si es 0 se establece e 20km
        if(distancia!=0){
            distanciaMax= distancia;
        }
        else{
            distanciaMax=20;
        }


        // initiating the tabhost
        tabhost = (TabHost) findViewById(R.id.tabhost);

        // setting up the tab host
        tabhost.setup();

        // Code for adding Tab 1 to the tabhost
        TabHost.TabSpec spec = tabhost.newTabSpec("Chats");
        spec.setContent(R.id.tab1);

        // setting the name of the tab 1 as "Tab One"
        spec.setIndicator("Chats");

        // adding the tab to tabhost
        tabhost.addTab(spec);

        // Code for adding Tab 2 to the tabhost
        spec = tabhost.newTabSpec("Mapa");
        spec.setContent(R.id.tab2);

        // setting the name of the tab 1 as "Tab Two"
        spec.setIndicator("Mapa");
        tabhost.addTab(spec);

        // Code for adding Tab 3 to the tabhost
        spec = tabhost.newTabSpec("Perfil");
        spec.setContent(R.id.tab3);
        spec.setIndicator("Perfil");
        tabhost.addTab(spec);

        tabhost.getTabWidget().getChildAt(0).setSelected(true);
        tabhost.getTabWidget()
                .getChildAt(0)
                .setBackgroundResource(
                        R.drawable.fondo_boton_perfil);
        tabhost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            //Se gestiona el fondo de los tabs
            @Override
            public void onTabChanged(String tabId) {
                for (int i = 0; i < tabhost.getTabWidget().getChildCount(); i++) {

                    if (tabhost.getTabWidget().getChildAt(i).isSelected()) {
                        tabhost.getTabWidget()
                                .getChildAt(i)
                                .setBackgroundResource(
                                        R.drawable.fondo_boton_perfil);
                    }
                    else{
                        tabhost.getTabWidget()
                                .getChildAt(i)
                                .setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            }
        });
        //Se obtiene la pestaña que está seleccionada de los extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int tab=extras.getInt("tab",0);
            tabhost.setCurrentTab(tab);

        }

        //En caso de girar la pantalla se fija la vista en la pestaña que estaba abierta
        if(savedInstanceState!=null){
            tabhost.setCurrentTab(savedInstanceState.getInt("LastTab"));


        }

        //Al abrir la aplicación se vuelca el contenido de la BD remota en la local, para tener todos los posibles cambios
        actualizarPerfiles();
        actualizar("editarToken", "token=" + Firebase.getToken(this)+"&sesion=1");

        //Se solicita el permiso de la ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 24);
        } else {
            SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
            elfragmento.getMapAsync(this);
        }

        //Al clickas el botón centrar se cargan las coordenadas de los usuarios de nuevo y se centra la vista del mapa
        boton=findViewById(R.id.botonCentrar);

        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarCoordenadasUsuarios();
                centrar();
            }
        });

        //Se obtiene el token FCM
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.i("MYAPP", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = token;
                        Log.i("MYAPP", msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });

    }

    //Al girar el teléfono se almacena en qué pestaña se estaba
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("LastTab", tabhost.getCurrentTab());
    }
    //Al obtener el permiso de ubicación se carga el mapa
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 24 && resultCode == Activity.RESULT_OK) {
            //Código de:https://stackoverflow.com/questions/5991319/capture-image-from-camera-and-display-in-activity
            SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
            elfragmento.getMapAsync(this);
        }

    }
    //Al obtener el permiso de ubicación se carga el mapa

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 24:{
                // Si la petición se cancela, granResults estará vacío
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // PERMISO CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
                    SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
                    elfragmento.getMapAsync(this);

                }
                else {
                    // PERMISO DENEGADO, DESHABILITAR LA FUNCIONALIDAD O EJECUTAR ALTERNATIVA


                }
                return;
            }

        }
    }
    //Método que centra la vista en el mapa en base a la distancia
    public void centrar() {
        //Se crea un círculo con la distancia actual o 20km por defecto
        Circle circle=miMapa.addCircle(new CircleOptions()
                .center(coordenadasActuales)
                .radius(distanciaMax*1000)
                .strokeColor(Color.RED)
                .strokeWidth(5));
        circle.isVisible();
        circle.setVisible(false);
        //circle.setVisible(false);
        float currentZoomLevel = getZoomLevel(circle);
        miMapa.animateCamera(CameraUpdateFactory.newLatLngZoom(
                circle.getCenter(), currentZoomLevel));

        miMapa.clear();
        miMapa.addMarker(new MarkerOptions()
                .position(coordenadasActuales)
                .title("Yo"));
        cargarCoordenadasUsuarios();
    }

//    //Comprueba si el usuario ha sido registrado
//    public void usuarioRegistrado(String id) {
//
//        Data datos = new Data.Builder()
//                .putString("fichero", "DAS_users.php")
//                .putString("parametros", "funcion=datosUsuario&id=" + id)
//                .build();
//        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("existeUsuario1").build();
//        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
//                .observe(this, new Observer<WorkInfo>() {
//                    @Override
//                    public void onChanged(WorkInfo workInfo) {
//
//                        if (workInfo != null && workInfo.getState().isFinished()) {
//                            String resultado = workInfo.getOutputData().getString("resultado");
//                            Log.i("MYAPP", "inicio realizado");
//
//                            Log.i("MYAPP", resultado);
//                            JSONObject jsonObject = null;
//                            try {
//                                jsonObject = new JSONObject(resultado);
//                                String nombre = jsonObject.getString("nombre");
//                                if (nombre.equals("null")) {
//                                    Intent i = new Intent(MainActivity.this, RegisterActivity.class);
//                                    i.putExtra("id", id);
//                                    startActivity(i);
//                                    finish();
//
//                                }
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//
//
//                        }
//                    }
//                });
//        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
//        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("existeUsuario1", ExistingWorkPolicy.REPLACE, requesContrasena);
//
//
//    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Al cargar el mapa
        //Si no tiene permisos, vuelve a la ventana principal
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(MainActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        }
        miMapa=googleMap;
        //Se crea el proveedor de localización
        proveedordelocalizacion =
                LocationServices.getFusedLocationProviderClient(this);
        proveedordelocalizacion.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    //Si es exitoso
                    public void onSuccess(Location location) {
                        if (location != null) {
                            //Se almacenan las coordenadas actuales
                            LatLng nuevascoordenadas = new LatLng(location.getLatitude(), location.getLongitude());
                            coordenadasActuales=nuevascoordenadas;
                            //Se le pone el estilo al mapa para que sea blanco
                            //Web para mapas editados: https://mapstyle.withgoogle.com/
                            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.estilo_mapa_nuevo));

                            //Se crea un círculo con la distancia actual o 20km por defecto
                            Circle circle= googleMap.addCircle(new CircleOptions()
                                    .center(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .radius(distanciaMax*1000)
                                    .strokeColor(Color.RED)
                                    .strokeWidth(5));
                            circle.isVisible();
                            circle.setVisible(false);
                            //circle.setVisible(false);
                            float currentZoomLevel = getZoomLevel(circle);
                            float animateZomm = currentZoomLevel + 5;

                           //Se centra la vista en el mapa
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    circle.getCenter(), getZoomLevel(circle)));

                            usuario=googleMap.addMarker(new MarkerOptions()
                                    .position(nuevascoordenadas)
                                    .title("Yo"));


//                            CameraPosition Poscam = new CameraPosition.Builder()
//                                    .zoom(currentZoomLevel)
//                                    .tilt(80)
//                                    .build();
//                            CameraUpdate otravista = CameraUpdateFactory.newCameraPosition(Poscam);

                            //Se añade en el mapa un marcador en la ubicación actual del jugador, para representarlo.


                            //googleMap.moveCamera(otravista);
                            //Se cargan los marcadores de los usuarios que se encuentran cerca
                            cargarCoordenadasUsuarios();
                        } else {
                            return;
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    //Si falla no se hace nada
                    public void onFailure(@NonNull Exception e) {

                    }
                });

        //Se establece la actualización periódica de la posición del jugador y de los marcadores de brote.
        actualizador = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    distanciaMax=preferencias.getInt("distancia",20);
                    LatLng pos = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                    coordenadasActuales=pos;
                    if(usuario != null){
                        usuario.remove();
                    }

                    usuario=googleMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("Yo"));
                } else {
                    return;
                }
            }
        };




        LocationRequest peticion = LocationRequest.create();
        peticion.setInterval(3000);
        peticion.setFastestInterval(1000);
        peticion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        proveedordelocalizacion.requestLocationUpdates(peticion, actualizador, null);

        //Se establece el listener de los marcadores del mapa
       miMapa.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //Se abre la actividad infouser al clicar sobre un marcador
                String idClicado = (String) (marker.getTag());
                if (idClicado!=null){
                    Log.i("MYAPP","Usuario clicado: "+idClicado);
                    //Using position get Value from arraylist
                    Intent i=new Intent(getApplicationContext(), InfoUserActivity.class);
                    i.putExtra("id",idClicado);
                    finish();
                    startActivity(i);
                }


                return false;
            }
        });



    }
    //Método que obtiene el nivel de zoom en base a un círculo del mapa
    /** stackoverflow
     * pregunta: https://stackoverflow.com/questions/11309632/how-to-find-zoom-level-based-on-circle-draw-on-map/27654602
     * usuario: https://stackoverflow.com/users/649256/anand-tiwari**/
    public float getZoomLevel(Circle circle) {
        float zoomLevel=0;
        if (circle != null){
            double radius = circle.getRadius();
            double scale = radius / 500;
            zoomLevel =(int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel +.5f;
    }

    //Método que carga a los usuarios cercanos y sus coordenadas en base a la distancia
    private void cargarUsuariosCercanos() {

        for(int i=0;i<usuarios.length();i++){
            try {
                JSONObject infoUsuario=usuarios.getJSONObject(i);
                double longitud=infoUsuario.getDouble("longitud");
                double latitud=infoUsuario.getDouble("latitud");
                int distanciaMaxVecino=infoUsuario.getInt("distancia");
                LatLng locVecino=new LatLng(latitud,longitud);
                String idUsuario=infoUsuario.getString("id");
                String nombre=infoUsuario.getString("nombre");
                //Si el usuario no es el loggeado
                if (!idUsuario.equals(id)){
                    double distancia=calcularDistancia(locVecino,coordenadasActuales);
                    Log.i("MYAPP","Distancia a: "+infoUsuario.getString("id")+" "+distancia);
                    //Se comprueba si la distancia establecida es menor a la del usuario cercano y a la del logeado
                    if (distancia<distanciaMax && distancia<distanciaMaxVecino){
                        generarMarcador(idUsuario,nombre,locVecino);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }

    private void generarMarcador(String idUsuario, String nombre, LatLng locVecino) {
        //Metodo que carga la imagen de Firebase Storage y crea un marcador con ella
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child("images/" + idUsuario + ".jpg");
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {

                Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(uri)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                //Cambia el tamaño de la imagen al del icono del mapa y lo muestra
                                Bitmap resized = Bitmap.createScaledBitmap(resource, 150, 150, true);

                                Marker marker=miMapa.addMarker(new MarkerOptions()
                                        .position(locVecino)
                                        .title(nombre)
                                        .icon(BitmapDescriptorFactory.fromBitmap(resized))
                                        .anchor(0.5f, 1));
                                marker.setTag(idUsuario);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            }
        });

    }

    //Método que calcula la distancia entre dos coordenadas
    private double calcularDistancia(LatLng loc1,LatLng loc2) {
        int Radius = 6371;// radio de la tierra en  kilómetros
        double lat1 = loc2.latitude;
        double lat2 = loc1.latitude;
        double lon1 = loc2.longitude;
        double lon2 = loc1.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return Radius * c;

    }

    public void cargarCoordenadasUsuarios() {
        //Carga las coordenadas de los usuarios de la BD que tengan la sesión iniciada
        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=" + "obtenerCoordenadasUsuarios")
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("cargarCoordenadas").build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);

                            try {
                                JSONArray jsonArray = new JSONArray(resultado);
                                usuarios=jsonArray;
                                cargarUsuariosCercanos();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        WorkManager.getInstance(getApplication()).enqueueUniqueWork("cargarCoordenadas", ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    private void actualizarImagen(String idUsuario) {
        //Metodo que carga la imagen de Firebase Storage, para actualizar la almacenada en la BD local
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child("images/" + idUsuario + ".jpg");
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getApplicationContext()).asBitmap().load(uri).into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        resource.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] imageInByte = stream.toByteArray();

                        ContentValues valores = new ContentValues();
                        valores.put("Imagen", imageInByte);

                        String[] argumentos = new String[] {idUsuario};

                        BDLocal gestorDB = new BDLocal (MainActivity.this, "DAS", null, 1);
                        SQLiteDatabase bd = gestorDB.getWritableDatabase();
                        bd.update("Usuarios", valores, "Id=?", argumentos);

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
            }
        });
    }


    private void actualizarPerfil(String idUsuario) {
        //Método que obtiene los datos de un usuario para actualizar la BD local
        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=datosUsuario&id=" + idUsuario)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosUsuario"+id).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
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
                                String token = jsonObject.getString("id_FCM");

                                ContentValues valores = new ContentValues();
                                valores.put("Nombre", nombre);
                                valores.put("Token", token);

                                String[] argumentos = new String[] {idUsuario};

                                BDLocal gestorDB = new BDLocal (MainActivity.this, "DAS", null, 1);
                                SQLiteDatabase bd = gestorDB.getWritableDatabase();
                                bd.update("Usuarios", valores, "Id=?", argumentos);
                                actualizarImagen(idUsuario);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(this).enqueueUniqueWork("getDatosUsuario"+id, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

    private void actualizarPerfiles(){
        //Método que busca en la BD remota los perfiles de los usuarios y actualiza la info de la BD local
        BDLocal gestorDB = new BDLocal (this, "DAS", null, 1);
        SQLiteDatabase bd = gestorDB.getWritableDatabase();

        String[] campos = new String[] {"Id"};
        Cursor cu = bd.query("Usuarios",campos,null,null,null,null,null);

        if(cu.getCount() != 0){
            cu.moveToFirst();
            for(int i = 0; i < cu.getCount(); i++){
                String idUsuario = cu.getString(0);
                actualizarPerfil(idUsuario);
                cu.moveToNext();
            }
        }
    }


    private void actualizar(String funcion, String campo) {
        //Método para actualizar el campo indicado en la BD
        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=" + funcion + "&id=" + id + "&" + campo)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("actualizar" + funcion).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);

                            if (resultado.contains("error")) {
                                Toast toast = Toast.makeText(MainActivity.this, "Ha ocurrido un error al actualizar el campo", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
                                toast.show();
                            }

                        }
                    }
                });
        WorkManager.getInstance(MainActivity.this).enqueueUniqueWork("actualizar" + funcion, ExistingWorkPolicy.REPLACE, requesContrasena);
    }
}