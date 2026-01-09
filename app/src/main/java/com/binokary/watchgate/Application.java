package com.binokary.watchgate;

import android.content.Context;

public class Application extends android.app.Application {

    private static Application instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}