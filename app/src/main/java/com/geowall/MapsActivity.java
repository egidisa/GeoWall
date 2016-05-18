package com.geowall;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.geowall.domain.Wall;
import com.geowall.services.FirebaseManager;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status>, LocationListener {

    protected static final String TAG = MapsActivity.class.getSimpleName();

    // Google API
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    /**
     * The list of geofences used for walls.
     */
    protected List<Geofence> mGeofenceList;

    protected HashMap<String, Wall> mWallMap;
    protected Map<Marker, String> markersMap;
    protected Map<String, Marker> invertedMarkersMap;
    protected List<Marker> enabledWalls;
    /**
     * Used when requesting to add geofences.
     */
    private PendingIntent mGeofencePendingIntent;
    protected LocationRequest mLocationRequest;

    //Current location info.
    private Location mCurrentLocation;
    private String mLastUpdateTime;


    public static Toast wallInfoToast;


    // Firebase references
    FirebaseManager fm;
    private Firebase mRef;
    private GeoFire geoFire;
    private boolean geofencingActive = false;
    private boolean markersReady = false;



    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "BEGIN:BroadcastReceiver.onReceive()");
            ArrayList<Geofence> triggeringGeofences = (ArrayList<Geofence>) intent.getSerializableExtra(GeofenceTransitionsIntentService.TRIGGERING_GEOFENCES);
            // HashMap<String, Marker> tmpMarkersMap = new HashMap<>();
            // Log.i(TAG, "onReceive():markersMap = " + markersMap.toString());
            // for (Marker m : markersMap.keySet()) {
                // tmpMarkersMap.put(markersMap.get(m), m);
                // Log.i(TAG, "onReceive(): m = " + m.toString());
            // }

            // Log.i(TAG, "onReceive():tmpMarkersMap = " + tmpMarkersMap.toString());
            if (triggeringGeofences != null && markersReady) {
                // Change markers icon of exited geofences to AZURE
                if (intent.getIntExtra(GeofenceTransitionsIntentService.GEOFENCE_TRANSITION, 0) == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    Log.i(TAG, "onReceive():Geofence exited, marker changed");
                    for (Geofence g : triggeringGeofences) {
						String markerId = g.getRequestId(); // = wallId

                        Marker m = invertedMarkersMap.get(markerId);
                        m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.landmark_blue));
                        boolean removeResult = enabledWalls.remove(m);
                        Log.i(TAG, removeResult == true ? "onReceive():Wall disabled: " + mWallMap.get(markerId).getName() : "onReceive():Wall NOT disabled: " + mWallMap.get(markerId).getName());
                    }
                }
                // Change markers icon of entered geofences to GREEN
                else if (intent.getIntExtra(GeofenceTransitionsIntentService.GEOFENCE_TRANSITION, 0) == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.i(TAG, "onReceive():Geofence entered, marker changed");
                    Log.i(TAG, "onReceive(): triggeringGeofences = " + triggeringGeofences);
                    for (Geofence g : triggeringGeofences) {
                        String markerId = g.getRequestId(); // = to wallId
						Marker m = invertedMarkersMap.get(markerId);
                        Log.i(TAG, "onReceive(): geofenceId = " + markerId);
                        Log.i(TAG, "onReceive(): marker = " + invertedMarkersMap.get(markerId));
						
						m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.landmark_greent));
						enabledWalls.add(m);
						Log.i(TAG, enabledWalls.add(m) == true ? "onReceive():Wall enabled: " + mWallMap.get(markerId).getName() : "onReceive():Wall NOT enabled: " + mWallMap.get(markerId).getName());
                    }
                } else
                    Log.e(TAG, "onReceive(): Unknown geofence transition");

            }

            Log.i(TAG, "END:BroadcastReceiver.onReceive()");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "BEGIN:onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Empty map for storing walls data

        mWallMap = new HashMap<>();
        // Empty map for storing markers
        markersMap = new HashMap<>();
		// inverted map of markersMap
        invertedMarkersMap = new HashMap<>();
        //Set to store the walls that can be opened
        enabledWalls = new ArrayList<>();
        
        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;
        mCurrentLocation = null;


        fm = FirebaseManager.getInstance();
        mRef = fm.getRef();
        geoFire = new GeoFire(mRef);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //load walls from Firebase
        loadWalls();
        Log.i(TAG, "Walls loaded:" + mWallMap.toString());

        // Set the geofences used.
        //populateGeofenceList();
        // Log.i(TAG, "Geofence list loaded");
        // getLocation();
        //fab definition
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewWall(mCurrentLocation);
                //TODO se c'è già bacheca vicina non dovrebbe poterne inserire una nuova
                Snackbar.make(view, "The is already a wall close to you!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
        Log.i(TAG, "onCreate():mWallMap = " + mWallMap.toString());
        Log.i(TAG, "onCreate():mGeofenceList = " + mGeofenceList.toString());
        Log.i(TAG, "END:onCreate()");

    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "BEGIN:buildGoogleApiClient()");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).addApi(AppIndex.API)

                .build();

        Log.i(TAG, "END:buildGoogleApiClient()");
    }

    protected void createLocationRequest() {
        Log.i(TAG, "BEGIN:createLocationRequest()");
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
        Log.i(TAG, "END:createLocationRequest()");
    }

    protected void startLocationUpdates() {
        Log.i(TAG, "BEGIN:startLocationUpdates()");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.PERMISSION_TO_ACCESS_FINE_LOCATION);

            Log.i(TAG, "startLocationUpdates(): PERMISSIONS TO ACCESS_FINE_LOCATION not granted!");
            return;
        }
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        Log.i(TAG, "END:startLocationUpdates()");
    }

    // method used to handle properly the permissions requested to the user
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "BEGIN:onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.PERMISSION_TO_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                Log.i(TAG, "onRequestPermissionsResult():requestCode received " + Constants.PERMISSION_TO_ACCESS_FINE_LOCATION);
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    //TODO
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            default:
                Log.i(TAG, "onRequestPermissionsResult():requestCode received UNKNOWN.");
        }
        Log.i(TAG, "END:onRequestPermissionsResult()");
    }

    //Method that shows a dialog where the user defines a new wall parameters. The new wall
    // is then pushed into the db
    private void createNewWall(final Location mLastLocation) {
        Log.i(TAG, "BEGIN:createNewWall()");
        final AlertDialog.Builder newWallDialog = new AlertDialog.Builder(this);

        //final EditText wallName = (EditText) findViewById(R.id.wallname);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialoglayout, null);
        final EditText wallName = (EditText) view.findViewById(R.id.wallname);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        newWallDialog.setView(view);

        newWallDialog.setTitle("Create new wall");
        newWallDialog.setMessage("Enter a valid wall name");
        newWallDialog.setIcon(R.drawable.new_wall_icon);
        newWallDialog.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dial, int id) {
                //TODO check name and open wall activity
                Firebase wallRef = mRef.child("walls");
                Wall newWall = new Wall();
                newWall.setName(wallName.getText().toString());
                //TODO - vedere se ci sono caratteri non consentiti per le key in fb e i vari limiti di lunghezza
                if (mLastLocation != null) {
                    newWall.setLat(mLastLocation.getLatitude());
                    newWall.setLon(mLastLocation.getLongitude());


                    loadWalls();
                } else {

                    //newWall.setLat(43.763789);
                    //newWall.setLon(10.406054);
                }
                //newWall.setMessages(new ArrayList<Message>());

                newWall.setMsgCount(new Long(0));
                newWall.setId("");
                Firebase pushRef = wallRef.push();
                pushRef.setValue(newWall);
                String wallId = pushRef.getKey();
                Firebase updateRef = wallRef.child(wallId).child("id");
                updateRef.setValue(wallId);
            }
        });


        newWallDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dial, int id) {
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
        Log.i(TAG, "END:createNewWall()");
    }


    // onStart dell'Activity
    @Override
    protected void onStart() {
        Log.i(TAG, "BEGIN:onStart()");
        super.onStart();
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
        Log.i(TAG, "onStart():mWallMap = " + mWallMap.toString());
        Log.i(TAG, "onStart():mGeofenceList = " + mGeofenceList.toString());

        Log.i(TAG, "END:onStart()");
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "BEGIN:onStop()");
        super.onStop();


        Log.i(TAG, "onStop():mWallMap = " + mWallMap.toString());
        Log.i(TAG, "onStop():mGeofenceList = " + mGeofenceList.toString());

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
        Log.i(TAG, "END:onStop()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(GeofenceTransitionsIntentService.NOTIFICATION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        Log.i(TAG, "onConnected():mWallMap = " + mWallMap.toString());
        Log.i(TAG, "onConnected():mGeofenceList" + mGeofenceList.toString());

        createLocationRequest();
        startLocationUpdates();
        connectGeofences();

        Log.i(TAG, "END:onConnected()");
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

        Log.i(TAG, "BEGIN:onMapReady()");
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            public void onInfoWindowClick(Marker marker) {

                    String wallKey = markersMap.get(marker);
                Log.i(TAG,"onInfoWindowClick():wallKey = " + wallKey);
                Log.i(TAG,"onInfoWindowClick():marker = " + marker.toString());
                Log.i(TAG,"onInfoWindowClick():mWallMap = " + mWallMap.toString());
                String wallName = mWallMap.get(wallKey).getName();
//                for (Wall w : mWallMap.values()) {
//                    if (w.getLat() == marker.getPosition().latitude && w.getLon() == marker.getPosition().longitude) {
//                        wallKey = w.getId();
//                        wallName = w.getName();
//                    }
//                }
                //TODO deve partire intent solo se dentro wall
                Log.i(TAG,"seOnInfoWindowClickListener():enabledWalls = " + enabledWalls.toString());
                boolean wallEnabled = enabledWalls.contains(marker);
                if (wallEnabled) {
                    Intent intent = new Intent(MapsActivity.this, WallActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("EXTRA_WALL_KEY", wallKey);
                    intent.putExtra("EXTRA_WALL_NAME", wallName);
                    startActivity(intent);
					
                }else {
                    Toast.makeText(getApplicationContext(), "The selected wall is too far! Get closer to open it!", Toast.LENGTH_LONG).show();
                }

            }
        });
        //loadWalls();
        //Log.i(TAG, "loadWallFinished" + wallsLoaded);
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

        // Show user position on map, with blue marker and circle
        mMap.setMyLocationEnabled(true);

        //set initial position as the current one
        //displayWalls();
        Log.i(TAG, "END:onMapReady()");
    }

    public void connectGeofences() {
        Log.i(TAG, "BEGIN:connectGeofences()");

        Log.i(TAG, "connectGeofences():mWallMap = " + mWallMap.toString());
        Log.i(TAG, "connectGeofences():mGeofenceList = " + mGeofenceList.toString());
        // TODO: check if correct; for now, first remove the old geofences
//        if(geofencingActive)
//        {
//            try {
//                LocationServices.GeofencingApi.removeGeofences(
//                        mGoogleApiClient,
//                        // A pending intent that that is reused when calling removeGeofences(). This
//                        // pending intent is used to generate an intent when a matched geofence
//                        // transition is observed.
//                        getGeofencePendingIntent()
//                ).setResultCallback(this); // Result processed in onResult().
//                geofencingActive = true;
//
//            } catch (SecurityException securityException) {
//                // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
//                logSecurityException(securityException);
//            } catch (IllegalStateException ise){
//                Log.e(TAG,"Error during geofences activation.");
//                Log.e(TAG,ise.getMessage());
//            }
//        }
//        else
        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
            geofencingActive = true;

        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        } catch (IllegalStateException ise) {
            Log.e(TAG, "Error during geofences activation.");
            Log.e(TAG, ise.getMessage());
        }
        Log.i(TAG, "END:connectGeofences()");
    }


    private void displayWallMarker(Wall w) {
        Log.i(TAG, "BEGIN:displayWallMarker()");
        LatLng temp = new LatLng(w.getLat(), w.getLon());
        Marker m = mMap.addMarker(new MarkerOptions().position(temp).title(w.getName()).snippet(w.getName()));
        markersMap.put(m, w.getId().toString()); // TODO rimuovere il toString
        mMap.addCircle(new CircleOptions().center(temp).radius(Constants.GEOFENCE_RADIUS_IN_METERS).fillColor(0x3399ccff).strokeColor(0x556699ff).visible(true));
        Log.i(TAG, "END:displayWallMarker()");
    }

    private void displayWallsMarkers() {
        Log.i(TAG, "BEGIN:displayWallMarkers()");
        Marker m = null;
        for (Wall w : mWallMap.values()) {
            LatLng temp = new LatLng(w.getLat(), w.getLon());
            m = mMap.addMarker(new MarkerOptions().position(temp).title("Name: "+w.getName()).snippet("Tap here to open wall").icon(BitmapDescriptorFactory.fromResource(R.drawable.landmark_accent)));



            markersMap.put(m, w.getId());
            invertedMarkersMap.put(w.getId(), m);
            mMap.addCircle(new CircleOptions().center(temp).radius(Constants.GEOFENCE_RADIUS_IN_METERS).fillColor(0x3399ccff).strokeColor(0x556699ff).visible(true));
            markersReady = true;
        }

		//remove the marker placeholder for "Torre di Pisa" hardcoded
        invertedMarkersMap.get("Torre di Pisa").setVisible(false);
        Log.i(TAG, "END:displayWallMarkers()");
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
        Log.i(TAG, "BEGIN:loadWalls()");
        //First load: default constant wall
        Wall pisa = Constants.PISA;
        mWallMap.put(Constants.PISA.getName(), Constants.PISA);
        mGeofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(pisa.getName())

                // Set the circular region of this geofence.
                .setCircularRegion(
                        pisa.getLat(),
                        pisa.getLon(),
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

        //Retrieving Firebase walls
        final Firebase wallsRef = mRef.child("walls");

        wallsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {


                Log.i(TAG, "BEGIN:onDataChange()");
                Wall w = null;
                Wall w1 = null;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    w = ds.getValue(Wall.class);


                    w1 = new Wall();
                    w1.setName(w.getName());
                    w1.setId(w.getId());
                    w1.setLat(w.getLat());
                    w1.setLon(w.getLon());
                    w1.setMsgCount(w.getMsgCount());

                    //popupate wall list


                    mWallMap.put(ds.getKey(), w1); //TODO la key dovrà essere l'ID della Wall

                    //populate mGeofenceList

                    Log.i(TAG, "onDataChange():ds.getKey() = " + ds.getKey());
                    mGeofenceList.add(new Geofence.Builder()
                            .setRequestId(ds.getKey())//TODO la key dovrà essere l'ID della wall
                            .setCircularRegion(
                                    w1.getLat(),
                                    w1.getLon(),
                                    Constants.GEOFENCE_RADIUS_IN_METERS
                            )


                            .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)


                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                    Geofence.GEOFENCE_TRANSITION_EXIT)


                            .build());
                }


                //TODO is mMap always ready here?
                displayWallsMarkers();
                connectGeofences();

                Log.i(TAG, "onDataChange():GEOFENCE_LIST = " + mGeofenceList.toString());


                Log.i(TAG, "onDataChange():WALL_MAP = " + mWallMap.toString());

                Log.i(TAG, "BEGIN:onDataChange()");
            }


            //
            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });

        Log.i(TAG, "END:loadWalls()");
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {

        Log.i(TAG, "BEGIN:getGeofencingRequest()");
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.

        Log.i(TAG, "END:getGeofencingRequest()");
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

        Log.i(TAG, "BEGIN:getGeofencePendingIntent()");
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);


        // mappa da passare solo per visualizzare il nome della wall associato al suo ID che viene scritto nella geofenceRequest
        HashMap<String, String> tmpWallMap = new HashMap<String, String>();
        for (String k : mWallMap.keySet()) {
            tmpWallMap.put(k, mWallMap.get(k).getName());
        }
        Log.i(TAG, "getGeofencePendingIntent():tmpWallMap = " + tmpWallMap.toString());
        intent.putExtra("wallsMap", tmpWallMap);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().

        Log.i(TAG, "END:getGeofencePendingIntent()");
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "BEGIN:onLocationChanged()");
        // update iff location changed more than Constants.EPSILON
		Location lastLocation = mCurrentLocation;
        if (isLocationChanged(location)) {
            mCurrentLocation = location;
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

			if (lastLocation == null) {
                LatLng myLoc = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(myLoc)              // Sets the center of the map to Mountain View
                        .zoom(15)                   // Sets the zoom
                        .bearing(360)                // Sets the orientation of the camera to east
                        .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            Toast.makeText(this, getResources().getString(R.string.location_updated_message),
                    Toast.LENGTH_LONG).show();

        }
        Log.i(TAG, "END:onLocationChanged()");
    }

    private boolean isLocationChanged(Location location) {
        float distance = 0.0f;
        if (mCurrentLocation == null)
            return true;
		else {
            distance = mCurrentLocation.distanceTo(location);
            Log.i(TAG, "isLocationChanged():distance = " + distance);
            if (distance < Constants.EPSILON)
                return false;
            return true;
        }
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     * <p/>
     * <p/>
     * <p/>
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {

        Log.i(TAG, "BEGIN:onResult()");
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

        Log.i(TAG, "END:onResult()");
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }
}