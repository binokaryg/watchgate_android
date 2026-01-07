package com.binokary.watchgate;

import android.content.Context;

public class Application extends android.app.Application {

    private static Context context;//TODO: Is this memory leak?

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}