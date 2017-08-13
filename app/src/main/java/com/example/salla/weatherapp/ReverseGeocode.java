package com.example.salla.weatherapp;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by Salla on 12.8.2017.
 */

//This class calls Google Geocode API with user coordinates and reads the city from the JSON array returned

public class ReverseGeocode extends AsyncTask<Double, Void, String> {

    private static final String TAG = "ReverseGeocode";
    private MainActivity _mainActivity;
    public ReverseGeocode(MainActivity mainActivity) {
        _mainActivity = mainActivity;
    }

    @Override
    protected String doInBackground(Double... coordinates) {

        Log.d(TAG, "In ReverseGeocode");
        String city = null;

        String lat = String.valueOf(coordinates[0]);
        String lon = String.valueOf(coordinates[1]);
        String url_s = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lon + "&sensor=false";

        InputStream inputStream = null;
        String data = null;
        try {
            URL url = new URL(url_s);

            inputStream = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            data = sb.toString();

        }
        catch (Exception e) {
            Log.d(TAG, "Connection to Google geocode API failed");
        }
        finally {
            try {
                if(inputStream != null ) {
                    inputStream.close();
                }
            } catch (Exception e ) { Log.d(TAG, "Input stream failed"); }
        }

        //Find administrative_area_level_3 object which contains the city/municipality, from address_components
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(data);
            JSONArray jsonArray = jsonObject.getJSONArray("results");
            if (jsonArray != null && jsonArray.length() > 0 ) {

                JSONObject oneObject = jsonArray.getJSONObject(1);
                JSONArray address = oneObject.getJSONArray("address_components");
                if (address != null && address.length() > 0 ) {

                    JSONObject innerObject = address.getJSONObject(1);
                    city = innerObject.getString("long_name");

                }

            }

        } catch (JSONException e ) { Log.d(TAG, "JSON read failed"); }

        return city;
    }

    @Override
    protected void onPostExecute(String city) {
        if (city != null) {
            Log.d(TAG, "City found");
            _mainActivity.setCity(city);
            Toast.makeText(_mainActivity, "location: " + city, Toast.LENGTH_LONG).show();

            //Now we are finished with the location, start the call for weather data
            _mainActivity.startFetchFMIDataThread();
        }
        else {
            Log.d(TAG, "No city was found");
            _mainActivity.setCity("Tampere");
        }
    }



}
