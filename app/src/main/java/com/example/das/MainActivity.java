package com.example.das;

import androidx.annotation.NonNull;
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
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;

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
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private FirebaseAuth firebaseAuth;
    private String id;


    FusedLocationProviderClient proveedordelocalizacion;
    LocationCallback actualizador;
    Marker jugador;
    Boolean seguir = true;
    ArrayList<Marker> listaMarkers = new ArrayList<>(5);
    ArrayList<GroundOverlay> listaCirculos = new ArrayList<>(5);


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
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        id=preferencias.getString("id",null);

        if(id==null){
            // En caso de no haber un usuario loggeado se abre la actividad de login
            Intent i=new Intent(this, LoginActivity.class);
            startActivity(i);
        }
        else{
            //Se comprueba si el usuario ha completado el proceso de registro
            usuarioRegistrado(id);
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
    public void usuarioRegistrado(String id){

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=datosUsuario&id=" +id)
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
                            if (resultado.contains("error")|| resultado.contains("null")){
                                Intent i=new Intent(MainActivity.this, RegisterActivity.class);
                                i.putExtra("id",id);
                                startActivity(i);
                                finish();

                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getApplication().getBaseContext()).enqueueUniqueWork("existeUsuario", ExistingWorkPolicy.REPLACE, requesContrasena);



    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Si no tiene permisos, vuelve a la ventana principal
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(MainActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        }
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

                            //Se le pone el estilo al mapa para que parezca Marte
                            //Web para mapas editados: https://mapstyle.withgoogle.com/
                            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.estilo_mapa));

                            //Se le asignan varios listeners para decidir si la cámara tiene que seguir al jugador o no, de modo que
                            //cuando se pulsa el botón para centrar la cámara siga al jugador, pero tras interactuar con el mapa, deje de seguir al jugador.
                            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                                @Override
                                public void onMapClick(LatLng latLng) {
                                    seguir = false;
                                }
                            });

                            googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                                @Override
                                public void onMapLongClick(LatLng latLng) {
                                    seguir = false;
                                }
                            });

                            googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                                @Override
                                public void onCameraMoveStarted(int reason) {
                                    if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                                        seguir = false;
                                    }
                                }
                            });

                            //Se establece un listener para que no salgan etiquetas al pulsar en los marcadores
                            googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                                @Override
                                public boolean onMarkerClick(Marker marker) {
                                    return true;
                                }
                            });

                            //Se restringe el movimiento del usuario en el mapa, para que no pueda hacer demasiado zoom-out, y no pueda cambiar el tilt.
                            googleMap.getUiSettings().setTiltGesturesEnabled(false);
                            googleMap.setMinZoomPreference(17);
                            googleMap.setMaxZoomPreference(19);
                            googleMap.getUiSettings().setScrollGesturesEnabledDuringRotateOrZoom(false);
                            googleMap.getUiSettings().setZoomControlsEnabled(true);
                            CameraPosition Poscam = new CameraPosition.Builder()
                                    .target(nuevascoordenadas)
                                    .zoom(18)
                                    .tilt(80)
                                    .build();
                            CameraUpdate otravista = CameraUpdateFactory.newCameraPosition(Poscam);

                            //Se establece el botón de centrar, que al pulsarlo mueve la cámara a la localización actual del jugador.
                            /*ImageView botonCentrar = findViewById(R.id.botonCentrar);
                            botonCentrar.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    LocationServices.getFusedLocationProviderClient(MainActivity.this);
                                    seguir = true;
                                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapaActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        return;
                                    }
                                    proveedordelocalizacion.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location location) {
                                            LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                                            CameraUpdate actualizar = CameraUpdateFactory.newLatLng(pos);
                                            googleMap.animateCamera(actualizar);
                                        }
                                    });
                                }
                            });*/

                            //Se añade en el mapa un marcador en la ubicación actual del jugador, para representarlo.
                            jugador = googleMap.addMarker(new MarkerOptions()
                                    .position(nuevascoordenadas)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador))
                                    .title("Yo"));

                            //Se inicializan los marcadores de brote en posiciones aleatorias
                            //iniciarMarcadores(googleMap);
                            //actualizarMarcadores(googleMap);

                            googleMap.moveCamera(otravista);
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
                    LatLng pos = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                    CameraUpdate actualizar = CameraUpdateFactory.newLatLng(pos);
                    if (seguir){googleMap.animateCamera(actualizar);}
                    //Se elimina el antiguo marcador y se pone uno nuevo.
                    jugador.remove();
                    jugador = googleMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador)));
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

    }
}