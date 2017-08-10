package com.example.salla.weatherapp;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Salla on 8.8.2017.
 */

public class GeocodeCity extends AsyncTask<Double, Void, List<Address>> {

    private static final String TAG = "FindLocation";

    private MainActivity _mainActivity;
    public GeocodeCity(MainActivity mainActivity) {
        _mainActivity = mainActivity;
    }

    @Override
    protected List<Address> doInBackground(Double... coordinates) {
        Log.d(TAG, "In FindLocation");

        Geocoder geocoder = new Geocoder(_mainActivity, Locale.getDefault());
        List<Address> addresses = new ArrayList<>();
        try {
            addresses = geocoder.getFromLocation(coordinates[0], coordinates[1], 1);
        } catch(Exception e) {
            Log.d(TAG, "Reverse geocoding failed");
        }

        return addresses;
    }

    @Override
    protected void onPostExecute(List<Address> addresses) {
        Log.d(TAG, "FindLocation - onPostExecute");

        String city;
        if (addresses == null || addresses.size() == 0) {
            Log.d(TAG, "No address was found");
            //Set default city Tampere
            city = "Tampere";
        }
        else {

            Address address = addresses.get(0);
            Log.d(TAG, address.toString());
            //Address->locality has the city stored
            city = address.getLocality();
            Log.d(TAG, "Address was found, city is " + city);
        }
        _mainActivity.setCity(city);
        Toast.makeText(_mainActivity, "location: " + city, Toast.LENGTH_LONG).show();
        // Now we are finished with the location -> can get data for it
        _mainActivity.startFetchFMIDataThread();
    }

}
