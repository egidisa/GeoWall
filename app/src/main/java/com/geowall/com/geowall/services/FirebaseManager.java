package com.geowall.com.geowall.services;

import com.firebase.client.Firebase;
import com.geowall.Constants;

/**
 * Created by Alessio on 09/05/2016.
 */
public class FirebaseManager {
    final static Firebase ref;

    static {
        ref = new Firebase(Constants.FIREBASE_URL);
    }

    private FirebaseManager() {
    }

    public static Firebase getInstance() {
        return ref;
    }
}
