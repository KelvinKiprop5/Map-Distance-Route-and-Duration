package com.example.kevoh3.androidmapdistancerouteduration.Classes;

import android.app.Application;

import com.android.volley.RequestQueue;

public class CustomApplication extends Application {

    private RequestQueue requestQueue;
    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = VolleySingleton.getInstance(getApplicationContext()).getRequestQueue();
    }
    public RequestQueue getVolleyRequestQueue(){
        return requestQueue;
    }

}
