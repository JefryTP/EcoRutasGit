package com.example.ecorutas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.libraries.places.api.model.Place;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import java.util.Arrays;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.libraries.places.api.Places;
import android.Manifest;
import android.location.Location;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import com.google.android.gms.tasks.OnSuccessListener;
import android.util.Log;
import android.widget.Toast;
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        ImageButton gpsButton = findViewById(R.id.gpsButton);
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Button chatButton = findViewById(R.id.chatButton);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Places.initialize(getApplicationContext(), "AIzaSyBotrxu7YaHHfmbD8Bo2eDeChGkQjn2GrY");
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));

        // Restringe las búsquedas a Perú
        autocompleteFragment.setCountry("PE");
        chatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ChatBotActivity.class);
                startActivity(intent);
            }
        });

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // Maneja el lugar seleccionado (por ejemplo, place.getName(), place.getAddress())
                String placeName = place.getName();
                String placeAddress = place.getAddress();
                LatLng placeLatLng = place.getLatLng();

                // Agregar registros para depuración
                Log.d("PlaceDebug", "Place Name: " + placeName);
                Log.d("PlaceDebug", "Place Address: " + placeAddress);
                Log.d("PlaceDebug", "Place LatLng: " + placeLatLng);

                // Verifica que la ubicación (LatLng) no sea nula
                if (placeLatLng != null) {
                    double placeLatitude = placeLatLng.latitude;
                    double placeLongitude = placeLatLng.longitude;

                    // Agregar registros para depuración
                    Log.d("PlaceDebug", "Place Latitude: " + placeLatitude);
                    Log.d("PlaceDebug", "Place Longitude: " + placeLongitude);

                    // Pasa la información a la nueva actividad
                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                    intent.putExtra("placeName", placeName);
                    intent.putExtra("placeAddress", placeAddress);
                    intent.putExtra("placeLatitude", placeLatitude);
                    intent.putExtra("placeLongitude", placeLongitude);
                    startActivity(intent);
                } else {
                    // Manejar la situación en la que la ubicación es nula
                    Toast.makeText(MainActivity.this, "Busqueda cancelada", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Status status) {
                // Maneja el error
                String errorMessage = "Error al seleccionar el lugar: " + status.getStatusMessage();
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        gpsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    // El GPS no está activado, dirige al usuario a la configuración del GPS
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    Toast.makeText(MainActivity.this, "Por favor, activa el GPS", Toast.LENGTH_SHORT).show();
                } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Solicita el permiso de ubicación si no está concedido
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    // El GPS está activado, puedes obtener la ubicación
                    LocationRequest locationRequest = LocationRequest.create();
                    locationRequest.setInterval(10000);
                    locationRequest.setFastestInterval(5000);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                    LocationCallback locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (locationResult == null) {
                                return;
                            }
                            for (Location location : locationResult.getLocations()) {
                                if (location != null) {
                                    // Pasa la información a la nueva actividad
                                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                                    intent.putExtra("placeName", "Ubicación actual");
                                    intent.putExtra("placeAddress", "");
                                    intent.putExtra("placeLatitude", location.getLatitude());
                                    intent.putExtra("placeLongitude", location.getLongitude());
                                    startActivity(intent);
                                    fusedLocationClient.removeLocationUpdates(this);
                                    break;
                                }
                            }
                        }
                    };

                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                }
            }
        });
    }
}