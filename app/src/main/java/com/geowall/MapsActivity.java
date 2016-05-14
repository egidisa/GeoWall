package com.geowall;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.geowall.com.geowall.domain.Wall;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status>, LocationListener {

    protected static final String TAG = "MapActivity";

    // Google API
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    /**
     * The list of geofences used for walls.
     */
    protected List<Geofence> mGeofenceList;
    private List<Wall> mWallList;
    /**
     * Used when requesting to add geofences.
     */
    private PendingIntent mGeofencePendingIntent;
    protected LocationRequest mLocationRequest;

    //Current location info.
    private Location mCurrentLocation;
    private String mLastUpdateTime;

    /**
     * Used to persist application state about user info.
     */
    private SharedPreferences mSharedPreferences;

    // Firebase references
    private Firebase mRef;
    private GeoFire geoFire;

    Lock l = new ReentrantLock();
    final Condition c1 = l.newCondition();
    boolean wallsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Empty list for storing walls data
        mWallList = new ArrayList<Wall>();
        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        // Retrieve an instance of the SharedPreferences object.
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,MODE_PRIVATE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO aprire dialog  per inserire nome e raggio (?)
                createNewWall(mCurrentLocation);
                //TODO alla conferma del dialog inserire nuovo landmark ed aprire la bacheca appena creata


                //TODO se c'è già bacheca vicina non dovrebbe poterne inserire una nuova
                Snackbar.make(view, "The is already a wall close to you!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        mRef = new Firebase(Constants.FIREBASE_URL);
        geoFire = new GeoFire(mRef);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //load walls from Firebase
        loadWalls();
        Log.i(TAG, "Walls loaded");



        // Set the geofences used.
        //populateGeofenceList();
       // Log.i(TAG, "Geofence list loaded");
        // getLocation();
        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
        Log.i(TAG,"THISONCRETE"+this.toString());
        Log.i(TAG,"GEOFENCE_LIST_CREATE"+mGeofenceList.toString());
        Log.i(TAG, "END: onCreate()");

    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(Constants.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }


    //Method that shows a dialog where the user defines a new wall parameters. The new wall
    // is then pushed into the db
    private void createNewWall(final Location mLastLocation) {
        final AlertDialog.Builder newWallDialog = new AlertDialog.Builder(this);

        //final EditText wallName = (EditText) findViewById(R.id.wallname);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialoglayout, null);
        final EditText wallName = (EditText)view.findViewById(R.id.wallname);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        newWallDialog.setView(view);

        newWallDialog.setTitle("Create new wall");
        newWallDialog.setMessage("let's try this");
        newWallDialog.setIcon(R.drawable.common_google_signin_btn_icon_light);
        newWallDialog.setPositiveButton("Create", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dial, int id){
                //TODO check name and open wall activity
                Firebase wallRef = mRef.child("walls");
                Wall newWall = new Wall();
                newWall.setName(wallName.getText().toString());
                if (mLastLocation!=null){
                    newWall.setLat(mLastLocation.getLatitude());
                    newWall.setLon(mLastLocation.getLongitude());
                    loadWalls();
                    connectGeofences();
                }else{
                    //newWall.setLat(43.763789);
                    //newWall.setLon(10.406054);
                }
                newWall.setMsgCount(new Long(0));
                newWall.setId(new Long (3));
                wallRef.push().setValue(newWall);

            }
        } );
        newWallDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dial, int id){
                dial.dismiss();
            }
        });

        AlertDialog dialog = newWallDialog.create();
        /*final NumberPicker np = (NumberPicker) dialog.findViewById(R.id.numberPicker1);
        np.setMaxValue(100);
        np.setMinValue(0);
        np.setWrapSelectorWheel(false);*/
        dialog.show();
        //TODO - reload walls
    }

//    private void getLocation() {
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(LocationServices.API)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();
//    }

    // onStart dell'Activity
    @Override
    protected void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client.connect();
        mGoogleApiClient.connect();
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "Maps Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app URL is correct.
//                Uri.parse("android-app://com.geowall/http/host/path")
//        );
//        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"ONSTOPGEOFENCE_LIST"+mGeofenceList.toString());
        Log.i(TAG,"WALL_LIST"+mWallList.toString());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.geowall/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient.disconnect();
    }
    public void connectGeofences(){
        Log.i(TAG,"GEOFENCE_LIST_CONNECT"+mGeofenceList.toString());
        try {
            Log.i(TAG,"CONNECT_GEOFENCES");
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
            Log.i(TAG,"THISCONNECT"+this.toString());
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }
    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        startLocationUpdates();

        //TODO - scommentare
        /*try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }*/

//        mMap.setMyLocationEnabled(true);
//        //set initial position as the current one
//
//        // Add a marker in Sydney and move the camera
//        LatLng myLoc = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
//        CameraPosition cameraPosition = new CameraPosition.Builder()
//                .target(myLoc)              // Sets the center of the map to Mountain View
//                .zoom(15)                   // Sets the zoom
//                .bearing(360)                // Sets the orientation of the camera to east
//                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
//                .build();                   // Creates a CameraPosition from the builder
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        //geoFire.setLocation("users/7aa8e365-2717-46c5-89ce-3e94c09e9594/", new GeoLocation(37.7853889, -122.4056973));
        //geoFire.setLocation("0/loc", new GeoLocation(myLoc.latitude,myLoc.longitude));
        // Firebase bacheca = mRef.child("0").child("msgCount");
        //TODO - inserimento dati in fb
        //Firebase bacheca = mRef.child("users").child("d202c104-b6ad-428d-bda1-a52302d4fe6b").child("msgCount");
        //Long l = new Long(7);
        //bacheca.setValue(l);
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc));
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //loadWalls();
        Log.i(TAG,"loadWallFinished"+wallsLoaded);
        //populateGeofenceList();
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        //set initial position as the current one
        //displayWalls();

    }

    private void displayWalls() {
        for(Wall w:mWallList) {
            LatLng temp = new LatLng(w.getLat(), w.getLon());
            mMap.addMarker(new MarkerOptions().position(temp).title(w.getName()).snippet("hellow, can you hear me?"));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //logout selected
        if (id == R.id.action_logout) {
            mRef.unauth();
            loadLoginView();
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadLoginView() {
        Intent intent = new Intent(this, LoginActivity.class);
        //ogni task contiene più attività. Settare questi flag serve a iniziare
        //un nuovo task, clear task serve per "pulire" ogni task già esistente
        //che verrebbe associato con l'attività da resettare prima che l'attività parta.
        //L'attività diventa la nuova root di un task che altrimenti sarebbe vuoto, tutte
        //le vecchie attività vengono terminate.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);     //pulisce stack delle activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);   //previene che l'user premendo back torni alla main activity
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void loadWalls() {
        final Firebase wallsRef = mRef.child("walls");
        wallsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for(DataSnapshot ds : snapshot.getChildren()){
                    Wall w = ds.getValue(Wall.class);
                    //popupate wall list
                    mWallList.add(w);
                    //add marker
                    //LatLng temp2 = new LatLng(w.getLat(), w.getLon());
                    //mMap.addMarker(new MarkerOptions().position(temp2).title(w.getName()).snippet("hellow, can you hear me?"));
                    //TODO log here
                    Log.i(TAG,"LOAD_WALLS_ONCE - "+w.getName().toString());
                    mGeofenceList.add(new Geofence.Builder()
                            // Set the request ID of the geofence. This is a string to identify this
                            // geofence.
                            .setRequestId(w.getName())

                            // Set the circular region of this geofence.
                            .setCircularRegion(
                                    w.getLat(),
                                    w.getLon(),
                                    Constants.GEOFENCE_RADIUS_IN_METERS
                            )

                            // Set the expiration duration of the geofence. This geofence gets automatically
                            // removed after this period of time.
                            .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                            // Set the transition types of interest. Alerts are only generated for these
                            // transition. We track entry and exit transitions in this sample.
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                    Geofence.GEOFENCE_TRANSITION_EXIT)

                            // Create the geofence.
                            .build());
                }
                displayWalls();
                //connectGeofences();
                wallsLoaded = true;
                Log.i(TAG,"GEOFENCE_LIST"+mGeofenceList.toString());
                Log.i(TAG,"WALL_LIST"+mWallList.toString());
                Log.i(TAG,"DATA_LOADED - "+ wallsLoaded);
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
        /*
        Query queryRef = mRef.child("walls");
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChild) {
                Wall wall = snapshot.getValue(Wall.class);
                //popupate wall list
                mWallList.add(wall);
                //TODO log here
                Log.i(TAG,"LOAD_WALLS - "+wall.toString());
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
            }
            @Override
            public void onChildChanged(DataSnapshot snapshot,String previousChild) {
            }
            @Override
            public void onChildMoved(DataSnapshot snapshot,String previousChild) {
            }
            @Override
            public void onCancelled(FirebaseError e){}

        });*/
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Load geofence data from mWallList
     */
    public void populateGeofenceList() {
        Log.i(TAG, "BEGIN: populateGeofenceList()");
        for (Wall w : mWallList) {
            Log.i(TAG,w.toString());
            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(w.getName())

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            w.getLat(),
                            w.getLon(),
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());
            Log.i(TAG, "POPULATEGEOFENCELIST   - "+w.toString());
        }
        Log.i(TAG,mGeofenceList.toString());

        Log.i(TAG, "END: populateGeofenceList()");
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //updateUI();
        //TODO update camera solo all'inizio
        LatLng myLoc = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(myLoc)              // Sets the center of the map to Mountain View
                .zoom(15)                   // Sets the zoom
                .bearing(360)                // Sets the orientation of the camera to east
                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        Toast.makeText(this, getResources().getString(R.string.location_updated_message).concat(location.toString()),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
//            // Update state and save in shared preferences.
//            mGeofencesAdded = !mGeofencesAdded;
//            SharedPreferences.Editor editor = mSharedPreferences.edit();
//            //editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
//            //editor.apply();
//
//            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
//            // geofences enables the Add Geofences button.
//            setButtonsEnabledState();
//
//            Toast.makeText(
//                    this,
//                    getString(mGeofencesAdded ? R.string.geofences_added :
//                            R.string.geofences_removed),
//                    Toast.LENGTH_SHORT
//            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }
}
