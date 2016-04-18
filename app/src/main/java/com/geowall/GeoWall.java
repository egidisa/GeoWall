package com.geowall;

import android.app.Application;

import com.firebase.client.Firebase;

/**
 * Created by Sara on 17/04/2016.
 */
public class GeoWall extends Application {
    @Override
    public void onCreate(){
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
