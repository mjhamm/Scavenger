package com.app.scavenger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

class ConnectionDetector {

    private Context context;

    ConnectionDetector(Context context){
        this.context = context;
    }

    boolean isConnectingToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (NetworkInfo networkInfo : info)
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

}