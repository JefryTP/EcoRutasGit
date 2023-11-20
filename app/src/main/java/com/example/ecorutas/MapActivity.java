package com.example.ecorutas;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private String placeName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Recibe la información del lugar seleccionado
        placeName = getIntent().getStringExtra("placeName");

        // Muestra el nombre en TextView
        TextView textViewPlaceName = findViewById(R.id.textViewPlaceName);
        textViewPlaceName.setText(placeName);

        // Inicializa el fragmento de mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }
    private void getDirections(LatLng origin, LatLng destination) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + destination.latitude + "," + destination.longitude;
        String key = "key=AIzaSyBotrxu7YaHHfmbD8Bo2eDeChGkQjn2GrY";
        String parameters = str_origin + "&" + str_dest + "&" + key;
        String url = "https://maps.googleapis.com/maps/api/directions/json?" + parameters;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray routes = response.getJSONArray("routes");
                    JSONObject route = routes.getJSONObject(0);
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject overview_polyline = route.getJSONObject("overview_polyline");
                    String encodedString = overview_polyline.getString("points");
                    List<LatLng> list = decodePoly(encodedString);
                    Polyline line = mMap.addPolyline(new PolylineOptions()
                            .addAll(list)
                            .width(12)
                            .color(Color.parseColor("#05b1fb"))//Google maps blue color
                            .geodesic(true)
                    );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
    private double distanceBetween(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, results);
        return results[0];
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        double placeLatitude = getIntent().getDoubleExtra("placeLatitude", 0.0);
        double placeLongitude = getIntent().getDoubleExtra("placeLongitude", 0.0);
        LatLng placeLocation = new LatLng(placeLatitude, placeLongitude);
        // Agrega un marcador en el lugar seleccionado y mueve la cámara
        mMap.addMarker(new MarkerOptions().position(placeLocation).title("Marker in " + placeName));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLocation, 18));
        BitmapDescriptor greenIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("points");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                LatLng closestPoint = null;
                double minDistance = Double.MAX_VALUE;

                for (DataSnapshot pointSnapshot : dataSnapshot.getChildren()) {
                    double latitude = pointSnapshot.child("latitud").getValue(Double.class);
                    double longitude = pointSnapshot.child("longitud").getValue(Double.class);
                    LatLng point = new LatLng(latitude, longitude);
                    mMap.addMarker(new MarkerOptions().position(point).icon(greenIcon).title("Punto de Reciclaje"));

                    double distance = distanceBetween(placeLocation, point);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestPoint = point;
                    }
                }

                if (closestPoint != null) {
                    getDirections(placeLocation, closestPoint);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onBackPressed() {
        // Limpia los datos de MainActivity
        Intent intent = new Intent(MapActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        // Llama al método onBackPressed de la clase base
        super.onBackPressed();
    }
}