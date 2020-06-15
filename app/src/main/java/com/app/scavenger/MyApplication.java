package com.app.scavenger;

import android.app.Application;
import android.os.Build;

import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

//        new Instabug.Builder(this, "3766a9dd38dc29081800eb5e0b31c5c0")
//                .setInvocationEvents(InstabugInvocationEvent.NONE)
//                .build();
    }
}
