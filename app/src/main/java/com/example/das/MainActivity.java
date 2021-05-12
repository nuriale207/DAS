package com.example.das;

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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private FirebaseAuth firebaseAuth;
    private String id;
    private JSONArray usuarios;
    private LatLng coordenadasActuales;
    private double distanciaMax=18;

    private GoogleMap miMapa;

    FusedLocationProviderClient proveedordelocalizacion;
    LocationCallback actualizador;
    Marker jugador;
    ArrayList<Marker> listaMarkers = new ArrayList<>(5);
    ArrayList<GroundOverlay> listaCirculos = new ArrayList<>(5);
    SharedPreferences preferencias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Obtener la instancia de autenticación de Firebase
        //FirebaseAuth.getInstance ().signOut ();
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user == null) {
//            // En caso de no haber un usuario loggeado se abre la actividad de login
//            Intent i=new Intent(this, LoginActivity.class);
//            startActivity(i);
//        }
//        else{
//
//            //Se comprueba si el usuario ha completado el proceso de registro
//            id=user.getUid();
//            usuarioRegistrado(id);
//
//        }


        //Se comprueba si hay un usuario logeado. En caso de no haberlo se abre la actividad de login
        preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        id = preferencias.getString("id", null);
        int distancia=preferencias.getInt("distancia",0);
        if (id == null) {
            // En caso de no haber un usuario loggeado se abre la actividad de login
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finish();
        } else {
            //Se comprueba si el usuario ha completado el proceso de registro
            usuarioRegistrado(id);
        }

        if(distancia!=0){
            distanciaMax= distancia;
        }
        else{
            distanciaMax=20;
        }


        // initiating the tabhost
        TabHost tabhost = (TabHost) findViewById(R.id.tabhost);

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


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 24);
        } else {
            SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
            elfragmento.getMapAsync(this);
        }


    }

    //Comprueba si el usuario ha sido registrado
    public void usuarioRegistrado(String id) {

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=datosUsuario&id=" + id)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("existeUsuario1").build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {

                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);
                            JSONObject jsonObject = null;
                            try {
                                jsonObject = new JSONObject(resultado);
                                String nombre = jsonObject.getString("nombre");
                                if (nombre.equals("null")) {
                                    Intent i = new Intent(MainActivity.this, RegisterActivity.class);
                                    i.putExtra("id", id);
                                    startActivity(i);
                                    finish();

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("existeUsuario1", ExistingWorkPolicy.REPLACE, requesContrasena);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
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
                            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.estilo_mapa));

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

                            //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordenadasActuales, animateZomm));

                            //googleMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel), 2000, null);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    circle.getCenter(), getZoomLevel(circle)));


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
                    //Se crea un círculo con la distancia actual o 20km por defecto
                    Circle circle= googleMap.addCircle(new CircleOptions()
                            .center(pos)
                            .radius(distanciaMax*1000)
                            .strokeColor(Color.RED)
                            .strokeWidth(5));
                    circle.setVisible(false);
                    //circle.setVisible(false);
                    float currentZoomLevel = getZoomLevel(circle);
                    float animateZomm = currentZoomLevel + 5;

                    //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordenadasActuales, animateZomm));

                    //googleMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel), 2000, null);
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            circle.getCenter(), getZoomLevel(circle)));



                    //Se actualizan los marcadores de brote.
                    //actualizarMarcadores(googleMap);
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

       miMapa.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                String idClicado = (String) (marker.getTag());
                Log.i("MYAPP","Usuario clicado: "+idClicado);
                //Using position get Value from arraylist
                Intent i=new Intent(getApplicationContext(),InfoUserActivity.class);
                i.putExtra("id",idClicado);
                startActivity(i);

                return false;
            }
        });



    }
    public float getZoomLevel(Circle circle) {
        float zoomLevel=0;
        if (circle != null){
            double radius = circle.getRadius();
            double scale = radius / 500;
            zoomLevel =(int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel +.5f;
    }
    private void cargarUsuariosCercanos() {

        for(int i=0;i<usuarios.length();i++){
            try {
                JSONObject infoUsuario=usuarios.getJSONObject(i);
                double longitud=infoUsuario.getDouble("longitud");
                double latitud=infoUsuario.getDouble("latitud");
                LatLng locVecino=new LatLng(latitud,longitud);
                String idUsuario=infoUsuario.getString("id");
                String nombre=infoUsuario.getString("nombre");
                if (!idUsuario.equals(id)){
                    double distancia=calcularDistancia(locVecino,coordenadasActuales);
                    Log.i("MYAPP","Distancia a: "+infoUsuario.getString("id")+" "+distancia);

                    if (distancia<distanciaMax){
                        generarMarcador(idUsuario,nombre,locVecino);
//                        Marker marker=miMapa.addMarker(new MarkerOptions()
//                                .position(locVecino)
//                                .title(nombre));
//                        marker.setTag(idUsuario);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }

    private void generarMarcador(String idUsuario, String nombre, LatLng locVecino) {
        //Metodo que carga la imagen de Firebase Storage
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

    private void cargarCoordenadasUsuarios() {
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
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getApplication()).enqueueUniqueWork("cargarCoordenadas", ExistingWorkPolicy.REPLACE, requesContrasena);
    }
}