package com.example.salla.weatherapp;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.jar.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private double mLatitude;
    private double mLongitude;
    private final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private String mCity = null;
    private String mTemperature = null;
    private String mWeathertype = null;
    private ArrayList<String> mTempForecast = new ArrayList<>();
    private ArrayList<String> mTypeForecast = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Show something while getting data from FMI
        TextView textView = (TextView) findViewById(R.id.location_city);
        textView.setText( R.string.loading_text );


        Log.d(TAG, "onCreate");

        if (isOnline(this)) {
            Log.d(TAG, "Internet is OK");
        } else {
            Log.d(TAG, "Error with net");
            AlertDialog.Builder networkErrorDialog = new AlertDialog.Builder(this).setMessage("No internet connection");
            networkErrorDialog.show();
        }

        //Get data
        executeLocationAndData();

    }

    @Override
    protected void onResume() {
        super.onResume();
        executeLocationAndData();
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo networkinfo = cm.getActiveNetworkInfo();
        if (networkinfo != null && networkinfo.isConnected()) {
            return true;
        }
        return false;
    }

    //Starts the calls to check user location and fetch weather data
    public void executeLocationAndData() {
        Log.d(TAG, "In executeLocationAndData");

        //Location permission has to be checked/asked every time
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Log.d(TAG, "Location permission was missing");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        }
        else
        {
            Log.d(TAG, "Location permission is OK.");
            findUserLocation();
        }
    }


    //Handles the result of the request for location permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    findUserLocation();
                }
            }
        }
    }

    // Get user location coordinates from gps
    public void findUserLocation() {

        FusedLocationProviderClient fusedLocationClient;
        //Location services requires a new check
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        mLatitude = location.getLatitude();
                        mLongitude = location.getLongitude();
                        Log.d(TAG, "Lat: " + String.valueOf(mLatitude) + " Lon: " + String.valueOf(mLongitude));

                        //Location was found, now reverse geocode it to get city
                        try {
                            new GeocodeCity(MainActivity.this).execute(mLatitude, mLongitude);
                        } catch (Exception e) {
                            Log.d(TAG, "Coordinate parameters failed");
                        }

                    }
                }
            });

        }
    }

    //Query FMI for observation or forecast data
    public void startFetchFMIDataThread() {
        if (mCity != null) {
            //Observation data
            //String url = "http://data.fmi.fi/fmi-apikey/1b9baab5-e4a7-482f-8ef5-e25cc912be6a/wfs?request=getFeature&storedquery_id=fmi::observations::weather::simple&place=" + mCity + "&";
            //Forecast data
            String url = "http://data.fmi.fi/fmi-apikey/1b9baab5-e4a7-482f-8ef5-e25cc912be6a/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::simple&place=" + mCity + "&";

            try {
                new FetchFMIData(this).execute(url);
                Log.d(TAG, "Fetching data for location " + mCity);
            } catch (Exception e) {
                Log.d(TAG, "Malformed URL");
            }
        }
        else {
            Log.d(TAG, "Trying to fetch data for null location");
        }
    }

    public void setCity(String city) {
        mCity = city;
    }

    public void setTemperature(String temperature) { mTemperature = temperature; }

    public void setWeatherType(String weather_type) { mWeathertype = weather_type; }

    public void setTempForecast(ArrayList<String> array) { mTempForecast = array; }

    public void setTypeForecast(ArrayList<String> array) { mTypeForecast = array; }

    // The UI shows location, current temperature and weather type, and forecast for the next 12 hours
    public void updateUIWithData() {
        TextView textView_city = (TextView) findViewById(R.id.location_city);
        textView_city.setText( mCity );

        //Current view
        TextView textView_tempe = (TextView) findViewById(R.id.current_temperature);
        textView_tempe.setText( mTemperature );

        TextView textView_type = (TextView) findViewById(R.id.current_weather_type);
        String weather_type_text = getWeatherTypeText(mWeathertype);
        textView_type.setText( weather_type_text );


        // Forecast view
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int nextHour = currentHour + 1;

        ArrayList<String> times = new ArrayList<>();
        for (int i = 0; i < 12; ++i) {
            times.add(String.valueOf(nextHour) + ":00");
            ++nextHour;
            if (nextHour > 23) {
                nextHour = 0;
            }
        }

        ListView forecastView3;
        forecastView3 = (ListView) findViewById(R.id.forecast_time);

        ArrayAdapter<String> arrayAdapter3 = new ArrayAdapter<>(this, R.layout.item_forecast, R.id.time, times);
        forecastView3.setAdapter(arrayAdapter3);

        ListView forecastView;
        forecastView = (ListView) findViewById(R.id.forecast_temp);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.item_forecast, R.id.temperature, mTempForecast);
        forecastView.setAdapter(arrayAdapter);

        ListView forecastView2;
        forecastView2 = (ListView) findViewById(R.id.forecast_type);

        ArrayList<String> typeTextList = new ArrayList<>();
        for (int i = 0; i < mTypeForecast.size(); ++i) {
            typeTextList.add(getWeatherTypeText(mTypeForecast.get(i)));
        }

        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<>(this, R.layout.item_forecast, R.id.type, typeTextList);
        forecastView2.setAdapter(arrayAdapter2);

    }

    public String getWeatherTypeText(String weatherType) {
        String type_text;
        switch (weatherType) {
            case ("1.0"): type_text = "selkeää"; break;
            case ("2.0"): type_text = "puolipilvistä"; break;
            case ("21.0"): type_text = "heikkoja sadekuuroja"; break;
            case ("22.0"): type_text = "sadekuuroja"; break;
            case ("23.0"): type_text = "voimakkaita sadekuuroja"; break;
            case ("3.0"): type_text = "pilvistä"; break;
            case ("31.0"): type_text = "heikkoa vesisadetta"; break;
            case ("32.0"): type_text = "vesisadetta"; break;
            case ("33.0"): type_text = "voimakasta vesisadetta"; break;
            case ("41.0"): type_text = "heikkoja lumikuuroja"; break;
            case ("42.0"): type_text = "lumikuuroja"; break;
            case ("43.0"): type_text = "voimakkaita lumikuuroja"; break;
            case ("51.0"): type_text = "heikkoa lumisadetta"; break;
            case ("52.0"): type_text = "lumisadetta"; break;
            case ("53.0"): type_text = "voimakasta lumisadetta"; break;
            case ("61.0"): type_text = "ukkoskuuroja"; break;
            case ("62.0"): type_text = "voimakkaita ukkoskuuroja"; break;
            case ("63.0"): type_text = "ukkosta"; break;
            case ("64.0"): type_text = "voimakasta ukkosta"; break;
            case ("71.0"): type_text = "heikkoja räntäkuuroja"; break;
            case ("72.0"): type_text = "räntäkuuroja"; break;
            case ("73.0"): type_text = "voimakkaita räntäkuuroja"; break;
            case ("81.0"): type_text = "heikkoa räntäsadetta"; break;
            case ("82.0"): type_text = "räntäsadetta"; break;
            case ("83.0"): type_text = "voimakasta räntäsadetta"; break;
            case ("91.0"): type_text = "utua"; break;
            case ("92.0"): type_text = "sumua"; break;
            default: type_text = "tuntematon"; break;
        }
        return type_text;
    }

}



