package com.example.das;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapaFragment extends SupportMapFragment implements OnMapReadyCallback {

    FusedLocationProviderClient proveedordelocalizacion;
    LocationCallback actualizador;
    Marker jugador;
    Boolean seguir = true;
    ArrayList<Marker> listaMarkers = new ArrayList<>(5);
    ArrayList<GroundOverlay> listaCirculos = new ArrayList<>(5);

    public MapaFragment() {
        // Required empty public constructor
    }

    public static MapaFragment newInstance(String param1, String param2) {
        MapaFragment fragment = new MapaFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 24);
        } else {
            //SupportMapFragment elfragmento = (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
            this.getMapAsync(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Si no tiene permisos, vuelve a la ventana principal
        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        //Se crea el proveedor de localización
        proveedordelocalizacion =
                LocationServices.getFusedLocationProviderClient(this.getActivity());
        proveedordelocalizacion.getLastLocation()
                .addOnSuccessListener((Executor) this, new OnSuccessListener<Location>() {
                    @Override
                    //Si es exitoso
                    public void onSuccess(Location location) {
                        if (location != null) {
                            //Se almacenan las coordenadas actuales
                            LatLng nuevascoordenadas = new LatLng(location.getLatitude(), location.getLongitude());

                            //Se le pone el estilo al mapa para que parezca Marte
                            //Web para mapas editados: https://mapstyle.withgoogle.com/
                            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MapaFragment.this.getContext(), R.raw.estilo_mapa));

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

                            googleMap.moveCamera(otravista);
                        } else {
                            return;
                        }
                    }
                });

        //Se establece la actualización periódica de la posición del jugador y de los marcadores de brote.
        actualizador = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    LatLng pos = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                    //Se elimina el antiguo marcador y se pone uno nuevo.
                    jugador.remove();
                } else {
                    return;
                }
            }
        };

        LocationRequest peticion = LocationRequest.create();
        peticion.setInterval(60000);
        peticion.setFastestInterval(1000);
        peticion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        proveedordelocalizacion.requestLocationUpdates(peticion, actualizador, null);

    }
}