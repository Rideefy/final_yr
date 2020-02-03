package com.example.rideefy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.multidex.MultiDex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
//import com.google.android.libraries.places.compat.AutocompleteFilter;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerMap extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private Button mLogout, mRequest, mSettings;
    private LatLng pickupLocation;
    private Boolean requestBol = false;
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;
    private Marker mdriverMarker;
    private Marker mpickupMarker;
    private LinearLayout mDriverInfo;
    private ImageView mDriverProfile;
    private TextView mDriverName, mDriverPhone,mDriverCar;
    private Button mSchedule;


    GoogleApiClient mgoogleApiClient;
    Location mLocation;
    LocationRequest mLocationRequest;

    int mLocationRequestCode = 101;

    String[] mPermissions ={Manifest.permission.ACCESS_FINE_LOCATION};

    GeoQuery geoQuery;

    String destination;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MultiDex.install(this);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        mLogout = (Button)findViewById(R.id.btnLogout);
        mRequest = (Button)findViewById(R.id.btnRequest);
        mSettings = (Button)findViewById(R.id.btnSetting);

        mDriverInfo=(LinearLayout)findViewById(R.id.llDriver);
        mDriverName=(TextView)findViewById(R.id.tvDriverName);
        mDriverPhone=(TextView)findViewById(R.id.tvDriverPhone);
        mDriverCar=(TextView)findViewById(R.id.tvDriverCar);
        mDriverProfile = (ImageView)findViewById(R.id.ivDriverProfile);

        mSchedule = (Button)findViewById(R.id.btnList);




        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMap.this,MainActivity.class));
                finish();
                return;
            }
        });



        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(requestBol)
                {
                    requestBol = false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverFoundID!=null)
                    {
                        DatabaseReference Ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("CustomerRequest");
                        Ref.removeValue();
                        driverFoundID=null;

                    }
                    driverFound=false;
                    radius=1;

                    String userID = FirebaseAuth.getInstance().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");

                    GeoFire geofire = new GeoFire(ref);
                    geofire.removeLocation(userID, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            Toast.makeText(CustomerMap.this, "Format", Toast.LENGTH_SHORT).show();
                        }
                    });

                    if(mpickupMarker != null)
                    {
                        mpickupMarker.remove();
                    }
                    mRequest.setText("CALL UBER");

                    mDriverInfo.setVisibility(View.GONE);
                    mDriverName.setText("");
                    mDriverPhone.setText("");
                    mDriverCar.setText("Car: ");
                    mDriverProfile.setImageResource(R.mipmap.ic_profile);
                }
                else
                {
                    requestBol=true;
                    String userID = FirebaseAuth.getInstance().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");

                    GeoFire geofire = new GeoFire(ref);
                    geofire.setLocation(userID, new GeoLocation(mLocation.getLatitude(), mLocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            Toast.makeText(CustomerMap.this, "error"+error, Toast.LENGTH_SHORT).show();
                        }
                    });

                    pickupLocation = new LatLng(mLocation.getLatitude(),mLocation.getLongitude());
                    mpickupMarker=mMap.addMarker(new MarkerOptions().position(pickupLocation));//.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));

                    mRequest.setText("GETTING YOUR DRIVER");

                    getClosestDriver();
                }


            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(CustomerMap.this,CustomerSettings.class));
            }
        });

        mSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMap.this,CustomerPassive.class);
                startActivity(intent);
            }
        });




        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

            autocompleteFragment.setCountry("IN");
            //autocompleteFragment.setTypeFilter();
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID,Place.Field.LAT_LNG,Place.Field.NAME));

// Specify the types of place data to return.
        //autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

// Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName();
               // Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
               // Log.i(TAG, "An error occurred: " + status);
            }
        });


    }



    private void getClosestDriver()
    {

        DatabaseReference driverAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");

        GeoFire geoFire = new GeoFire(driverAvailable);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(mLocation.getLatitude(),mLocation.getLongitude()),radius);
        //geoQuery.removeAllListeners();


        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            //Anytime driver in radius is found this function gets activated
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundID = key;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("CustomerRequest");
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    HashMap map = new HashMap();

                    map.put("CustomerRideID", customerID);
                    map.put("Destination",destination);
                    driverRef.updateChildren(map);

                    getDriverLocation();
                    getDriverInfo();

                    mRequest.setText("retrieving location....");



                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            //called when the query is finished i.e onKeyEntered function is called
            public void onGeoQueryReady() {
                if(!driverFound)
                {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getDriverLocation()
    {
        driverLocationRef = FirebaseDatabase.getInstance().getReference("driversWorking").child(driverFoundID).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol)
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
                    if(mdriverMarker != null)
                    {
                        mdriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc1.setLatitude(driverLatLng.latitude);
                    loc1.setLongitude(driverLatLng.longitude);

                    Float distance = loc1.distanceTo(loc2);

                    mdriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your Driver!"));//.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));

                    if(distance<100)
                    {
                        mRequest.setText("driver has arrived");
                    }else
                        {
                        mRequest.setText("Driver Found :" + distance.toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void getDriverInfo()
    {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(driverFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    Map<String, Object> map= (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("Name")!=null)
                    {
                        mDriverName.setText(map.get("Name").toString());
                    }
                    if(map.get("Phone")!=null)
                    {
                        mDriverPhone.setText(map.get("Phone").toString());
                    }
                    if(map.get("Car")!=null)
                    {
                        mDriverCar.setText(map.get("Car").toString());
                    }
                    if(map.get("profileImageUrl")!=null)
                    {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfile);
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
        mLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


    }


    //when map is connected and location is requested...
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

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
}
