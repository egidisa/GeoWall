package com.geowall.services;

import android.util.Log;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.geowall.Constants;
import com.geowall.domain.Wall;
import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alessio on 09/05/2016.
 */
public class FirebaseManager {
    static FirebaseManager fm = null;
    Firebase ref = null;
    private final String TAG = FirebaseManager.class.getSimpleName();

    boolean wallsLoaded = false;

    private FirebaseManager() {
        ref = new Firebase(Constants.FIREBASE_URL);
    }

    public static FirebaseManager getInstance() {
        if (fm == null)
            fm = new FirebaseManager();
        return fm;
    }

    public Firebase getRef() {
        return ref;
    }

    public List<Wall> getWalls() {
        Log.i(TAG, "BEGIN: FirebaseManager.getWalls()");
        final Firebase wallsRef = ref.child("walls");

        FirebaseManager.MyValueEventListener vel = fm.new MyValueEventListener();
        wallsRef.addListenerForSingleValueEvent(vel);

        Log.i(TAG, "END: FirebaseManager.getWalls()");
        return vel.getWalls();
    }

    class MyValueEventListener implements ValueEventListener {
        private final String TAG = MyValueEventListener.class.getSimpleName();

        List<Wall> walls;

        public List<Wall> getWalls() {
            Log.i(TAG, "BEGIN: MyValueEventListener.getWalls()");

            Log.i(TAG, "END: MyValueEventListener.getWalls()");
            return walls;
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            Log.i(TAG, "BEGIN: MyValueEventListener.onDataChange()");
            walls = new ArrayList<Wall>();
            Wall w = null;
            for (DataSnapshot ds : snapshot.getChildren()) {
                w = ds.getValue(Wall.class);
                walls.add(w);
            }
            wallsLoaded = true;
            Log.i(TAG, "END: MyValueEventListener.onDataChange()");
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }
}
