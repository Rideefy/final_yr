package com.example.rideefy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.multidex.MultiDex;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverMap extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,com.google.android.gms.location.LocationListener {

    private static GoogleMap mMap;
    GoogleApiClient mgoogleApiClient;
    static Location mLocation;
    LocationRequest mLocationRequest;
    FusedLocationProviderClient mFusedLocationProviderClient;
    LatLng mLatlng;

    int mLocationRequestCode = 101;
    String[] mPermissions ={Manifest.permission.ACCESS_FINE_LOCATION};
    private Button mLogout, mDriverSettings, mDriverPassive;

    String customerID="";

    private Boolean isLoggingout = false;

    private LinearLayout mCustomerLayout;
    private ImageView mCustomerProfileImage;
    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MultiDex.install(this);

        setContentView(R.layout.activity_driver_map2);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        mLogout = (Button)findViewById(R.id.btnLogout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isLoggingout = true;

                disconnectDriver();

                String userID = FirebaseAuth.getInstance().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        Toast.makeText(DriverMap.this, "Bye bye", Toast.LENGTH_SHORT).show();
                    }
                });

                FirebaseAuth.getInstance().signOut();
                DatabaseReference refRemove = FirebaseDatabase.getInstance().getReference("driversWorking");
                GeoFire geoFire1 = new GeoFire(ref);
                geoFire1.removeLocation(userID, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        Toast.makeText(DriverMap.this, "removed", Toast.LENGTH_SHORT).show();
                    }
                });



                startActivity(new Intent(DriverMap.this,MainActivity.class));
                finish();
                return;




            }
        });

        mDriverSettings = (Button)findViewById(R.id.btnSetting);
        mDriverSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(DriverMap.this, DriverSettings.class);
                startActivity(intent);

            }
        });

        mDriverPassive = (Button)findViewById(R.id.btnPassive);
        mDriverPassive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(DriverMap.this, DriverPassiveInfo.class));

            }
        });

        getAssignedCustomer();

        mCustomerLayout = (LinearLayout)findViewById(R.id.llCustomer);
        mCustomerProfileImage = (ImageView)findViewById(R.id.ivImage);
        mCustomerName = (TextView)findViewById(R.id.tvNameField);
        mCustomerPhone = (TextView)findViewById(R.id.tvPhoneField);
        mCustomerDestination = (TextView)findViewById(R.id.tvDestination);

    }

    private void getAssignedCustomer()
    {
        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(driverID).child("CustomerRequest").child("CustomerRideID");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    customerID = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getCustomerDestination();
                    getAssignedCustomerPickupInfo();

                   /* String sample = String.valueOf(dataSnapshot.getKey());
                    Toast.makeText(DriverMap.this, "text="+sample, Toast.LENGTH_SHORT).show();
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot;
                    if(map.get("CustomerRideID")!=null)
                    {
                        customerID = map.get("CustomerRideID").toString();


                    } */
                }
                else
                {
                    customerID = "";
                    if(mpickupMarker!=null)
                    {
                        mpickupMarker.remove();;
                    }
                    if(assignedCustomerPickupLocaionRefListener != null) {
                        assignedCustomerRef.removeEventListener(assignedCustomerPickupLocaionRefListener);
                    }
                    mCustomerLayout.setVisibility(View.GONE);
                    mCustomerName.setText("");
                    mCustomerPhone.setText("");
                    mCustomerDestination.setText("Destination: ");
                    mCustomerProfileImage.setImageResource(R.mipmap.ic_profile);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private Marker mpickupMarker;
    private DatabaseReference assignedCustomerPickupLocaionRef;
    private ValueEventListener assignedCustomerPickupLocaionRefListener;
    private void getAssignedCustomerPickupLocation()
    {
        assignedCustomerPickupLocaionRef = FirebaseDatabase.getInstance().getReference("CustomerRequest").child(customerID).child("l");
        assignedCustomerPickupLocaionRefListener = assignedCustomerPickupLocaionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !customerID.equals(""))
                {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if(map.get(0)!= null)
                    {
                        locationLat = Double.parseDouble(map.get(0).toString());

                    }
                    if(map.get(1)!= null)
                    {
                        locationLng = Double.parseDouble(map.get(1).toString());

                    }

                    LatLng driverLatLng = new LatLng(locationLat,locationLng);
                    mpickupMarker =  mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Pickup Location"));
                }
                else
                {
                    Toast.makeText(DriverMap.this, "no marker", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private void getCustomerDestination()
    {

        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(driverID).child("CustomerRequest").child("Destination");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    String destination = dataSnapshot.getValue().toString();
                    mCustomerDestination.setText("Destination: "+destination);

                }
                else
                {
                    mCustomerDestination.setText("Destinnation: ");
                }
                /*mCustomerLayout.setVisibility(View.GONE);
                mCustomerName.setText("");
                mCustomerPhone.setText("");
                mCustomerProfileImage.setImageResource(R.mipmap.ic_profile);*/
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void getAssignedCustomerPickupInfo()
    {
        mCustomerLayout.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    Map<String, Object> map= (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("Name")!=null)
                    {
                        mCustomerName.setText(map.get("Name").toString());
                    }
                    if(map.get("Phone")!=null)
                    {
                        mCustomerPhone.setText(map.get("Phone").toString());
                    }
                    if(map.get("profileImageUrl")!=null)
                    {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }




    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    //jab map ready hoga to ye...
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        buildGoogleApiClient();

        mMap.setMyLocationEnabled(true);
    }




   protected synchronized void buildGoogleApiClient()
    {
        mgoogleApiClient = new GoogleApiClient.Builder(this)
                                .addConnectionCallbacks(this)
                               // .addOnConnectionFailedListener(this)
                                .addApi(LocationServices.API)
                                .build();
        mgoogleApiClient.connect();
    }





    @Override
    public void onLocationChanged(Location location) {

        if(getApplicationContext()!=null) {

            final Geocoder geocoder =new Geocoder(this, Locale.getDefault());
            mLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

           /* mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                   // Toast.makeText(DriverMap.this, ""+latLng, Toast.LENGTH_SHORT).show();
                    mLatlng = latLng;

                    try {
                        List<Address> addresses = geocoder.getFromLocation(mLatlng.latitude,mLatlng.longitude,1);
                        String city = addresses.get(0).getAddressLine(0);
                        Toast.makeText(DriverMap.this, ""+city, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.putExtra("source",city);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });*/


            String userID = FirebaseAuth.getInstance().getUid();
            DatabaseReference refDriverAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
            DatabaseReference refdriverWorking = FirebaseDatabase.getInstance().getReference("driversWorking");

            GeoFire geoFireAvailable = new GeoFire(refDriverAvailable);
            GeoFire geoFireWorking = new GeoFire(refdriverWorking);


            switch (customerID){

                case "" :
                    if(isLoggingout!=true){


                        geoFireAvailable.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Toast.makeText(DriverMap.this, "error" + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                        geoFireWorking.removeLocation(userID, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Toast.makeText(DriverMap.this, "Driver Available", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    break;


                default:

                        geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Toast.makeText(DriverMap.this, "error" + error, Toast.LENGTH_SHORT).show();
                            }
                        });

                        geoFireAvailable.removeLocation(userID, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Toast.makeText(DriverMap.this, "Driver Working "+customerID, Toast.LENGTH_SHORT).show();
                            }
                        });

                        break;

            }

            /* if(!customerID.isEmpty())
            {
                geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        Toast.makeText(DriverMap.this, "error"+error, Toast.LENGTH_SHORT).show();
                    }
                });

                geoFireAvailable.removeLocation(userID, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        Toast.makeText(DriverMap.this, "Removed DriverAvailable", Toast.LENGTH_SHORT).show();
                    }
                });

            }
            else
            {
                geoFireAvailable.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        Toast.makeText(DriverMap.this, "error"+error, Toast.LENGTH_SHORT).show();
                    }
                });
                geoFireWorking.removeLocation(userID, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        Toast.makeText(DriverMap.this, "Removed DriverWorking", Toast.LENGTH_SHORT).show();
                    }
                });
            }*/
        }






       /* geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                Toast.makeText(DriverMap.this, "bye bye", Toast.LENGTH_SHORT).show();
            }
        }); */

    }


    //when map is connected and location is requested...
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onConnected(@Nullable Bundle bundle) {



        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(mPermissions,mLocationRequestCode);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mgoogleApiClient,mLocationRequest,this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void disconnectDriver()
    {
        String userID = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");

        final GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                Toast.makeText(DriverMap.this, "Finaaly removed", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!isLoggingout)
        {
            disconnectDriver();
        }


    }

    public String onClickMapFunction(Context context){
        final String[] city = new String[1];
        final Geocoder geocoder =new Geocoder(context, Locale.getDefault());
        LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Toast.makeText(DriverMap.this, ""+latLng, Toast.LENGTH_SHORT).show();
                mLatlng = latLng;

                try {
                    List<Address> addresses = geocoder.getFromLocation(mLatlng.latitude,mLatlng.longitude,1);
                    city[0] = addresses.get(0).getAddressLine(0);
                    Toast.makeText(DriverMap.this, ""+ city[0], Toast.LENGTH_SHORT).show();
                   // Intent intent = new Intent();
                   // intent.putExtra("source",city);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return city[0];
    }
}
