package com.koushikdutta.tether;

import android.app.Application;

import android.os.Handler;



public class TetherApplication extends Application {
    static Handler mHandler = new Handler();
    static TetherApplication mInstance;
    public static int mVersionCode = 0;

    public TetherApplication() {
        mInstance = this;
    }


    public void onCreate() {
        super.onCreate();
    }
}
