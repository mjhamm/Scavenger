package com.app.scavenger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

// Class to check whether or not the device is connected to the internet
// through Network or wifi and returns boolean depending on check

class ConnectionDetector {

    private final Context context;

    ConnectionDetector(Context context){
        this.context = context;
    }

    boolean connectedToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            for (NetworkInfo networkInfo : info) {
                // If connected to Internet through Network or Wifi - return true
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        // else - return false
        return false;
    }

}