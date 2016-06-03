package com.geowall;

import com.geowall.domain.Wall;

/**
 * Defines all the constants used in the application.
 */
public class Constants {
    public static final String PACKAGE_NAME = "com.geowall";
    public static final String FIREBASE_URL ="https://geowallapp.firebaseio.com/";
    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES_NAME";

    // Default Wall: Pisa Tower ,\m/
    public static final Wall PISA = new Wall();
    public static final float EPSILON = 10.0f;
    public static final int PERMISSION_TO_ACCESS_FINE_LOCATION = 100;

    static {
        PISA.setId(new String("Torre di Pisa"));
        PISA.setLat(new Double(43.722920));
        PISA.setLon(new Double(10.396631));
        PISA.setName("Torre di Pisa");
        PISA.setMsgCount(new Long(0));
    }
    
    // GeoFencing constants
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    /**
     * For this sample, geofences expire after twelve hours.
     */
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 50;
}
