package com.example.rideefy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DriverRouteMap extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;

    GoogleApiClient mgoogleApiClient;
    static Location mLocation;
    LocationRequest mLocationRequest;
    FusedLocationProviderClient mFusedLocationProviderClient;
    static LatLng latLngDest, latLngSource;
    LatLng src ,dest;

    private Button mSet;
    Marker markerSource, markerDest;
    String area, driverSource, driverDest;

    List<Address> address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_route_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mSet = (Button)findViewById(R.id.btnSet);

        //polylines = new ArrayList<>();

        mSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("source",area);
                setResult(102,intent);
                //finish();
            }
        });




        int callingActivity = getIntent().getIntExtra("calling-activity", 0);

        switch (callingActivity) {
            case ActivityConstants.CustomerPassive:
                    driverRoute();
                break;
            case ActivityConstants.DriverPassive:
                // Activity2 is started from Activity3

                Intent intent = getIntent();
                if(intent.getExtras()!=null) {
                    String dattaMeghe = intent.getStringExtra("dest");
                    Geocoder geocoder = new Geocoder(DriverRouteMap.this, Locale.getDefault());
                    try {
                        List<Address> address = geocoder.getFromLocationName(dattaMeghe, 1);
                        double latitude = address.get(0).getLatitude();
                        double longitude = address.get(0).getLongitude();
                        latLngDest = new LatLng(latitude, longitude);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        buildGoogleApiClient();

        mMap.setMyLocationEnabled(true);


        //DirectionsResult results = getDirectionsDetails(latLngSource,latLngDest);
    }


    protected synchronized void buildGoogleApiClient() {
        mgoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                // .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mgoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mgoogleApiClient,mLocationRequest,this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {

            //final Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            mLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

            if(latLngDest!=null) {
                markerDest = mMap.addMarker(new MarkerOptions().position(latLngDest).title("DESTINATION").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                markerDest.setVisible(true);
            }



            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    Geocoder geocoder = new Geocoder(DriverRouteMap.this, Locale.getDefault());
                    try {
                        latLngSource = new LatLng(latLng.latitude,latLng.longitude);
                        address = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
                        area = address.get(0).getAddressLine(0);
                        Toast.makeText(DriverRouteMap.this, ""+area, Toast.LENGTH_SHORT).show();
                        if(markerSource!=null){
                            markerSource.remove();

                        }
                        markerSource = mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        markerSource.setVisible(true);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void driverRoute()
    {
        Intent intent = getIntent();
        String driverId = intent.getStringExtra("driverId").toString();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("driverSchedule").child(driverId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String,Object> map =(Map<String,Object>) dataSnapshot.getValue();
                if(dataSnapshot.getChildrenCount()>0){
                    driverSource= map.get("Source").toString();
                    driverDest = map.get("Dest").toString();

                    Geocoder geocoder = new Geocoder(DriverRouteMap.this, Locale.getDefault());
                    try {
                        List<Address> addressSrc = geocoder.getFromLocationName(driverSource,1);
                        LatLng src = new LatLng(addressSrc.get(0).getLatitude(),addressSrc.get(0).getLongitude());
                        mMap.addMarker(new MarkerOptions().position(src).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        List<Address> addressDest = geocoder.getFromLocationName(driverDest,1);
                        LatLng dest = new LatLng(addressDest.get(0).getLatitude(),addressDest.get(0).getLongitude());
                        mMap.addMarker(new MarkerOptions().position(dest).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



}






