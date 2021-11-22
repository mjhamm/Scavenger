package com.app.scavenger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

// Class to check whether or not the device is connected to the internet
// through Network or wifi and returns boolean depending on check

public class ConnectionDetector {

    private final Context context;

    public ConnectionDetector(Context context){
        this.context = context;
    }

    public boolean connectedToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            Network[] networks = connectivity.getAllNetworks();
            NetworkInfo networkInfo;
            for (Network mNetwork : networks) {
                networkInfo = connectivity.getNetworkInfo(mNetwork);
                // If connected to Internet through Network or Wifi - return true
                if (networkInfo != null) {
                    if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                        return true;
                    }
                }
            }
        }
        // else - return false
        return false;
    }

}