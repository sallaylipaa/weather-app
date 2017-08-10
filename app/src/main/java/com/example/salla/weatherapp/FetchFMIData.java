package com.example.salla.weatherapp;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Salla on 7.8.2017.
 */

public class FetchFMIData extends AsyncTask<String, Void, Boolean> {

    private static final String TAG = "FetchFMIData";

    private MainActivity _mainActivity;
    public FetchFMIData(MainActivity mainActivity) {
        _mainActivity = mainActivity;
    }

    protected Boolean doInBackground(String... urls) {

        ArrayList<String> temperatures = new ArrayList<>();
        ArrayList<String> weather_types = new ArrayList<>();

        //Open connection to FMI and read the queried xml (wfs) document
        try {
            URL url = new URL(urls[0]);
            URLConnection conn = url.openConnection();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(conn.getInputStream());

            NodeList nodes = doc.getElementsByTagName("BsWfs:BsWfsElement");

            //Read the data for the next 12 hours, the query gives 36 hours total
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                NodeList title = element.getElementsByTagName("BsWfs:ParameterName");
                Element line_title = (Element) title.item(0);

                if (line_title.getTextContent().equals("Temperature")) {
                    NodeList value = element.getElementsByTagName("BsWfs:ParameterValue");
                    Element line_value = (Element) value.item(0);
                    Double double_value = Double.parseDouble(line_value.getTextContent());
                    String short_value = String.format("%.1f", double_value);
                    temperatures.add(short_value + " °C");
                }
                else if (line_title.getTextContent().equals("WeatherSymbol3")) {
                    NodeList value = element.getElementsByTagName("BsWfs:ParameterValue");
                    Element line_value = (Element) value.item(0);
                    weather_types.add(line_value.getTextContent());
                }
                if (weather_types.size() > 11) { break; }
            }

            Log.d(TAG, "Size of list is " + temperatures.size());

            //Get the first time point of the forecast
            if (temperatures.size() > 0 && weather_types.size() > 0) {
                _mainActivity.setTemperature(temperatures.get(0));
                _mainActivity.setWeatherType(weather_types.get(0));
                _mainActivity.setTempForecast(temperatures);
                _mainActivity.setTypeForecast(weather_types);
                Log.d(TAG, "temp length: " + temperatures.size());
                Log.d(TAG, "type length: " + weather_types.size());
            }
            else {
                Log.d(TAG, "Nothing was found...");
                return false;
            }

            return true;
        }
        catch (Exception e) {
            Log.d(TAG, "Got an exception");
            e.printStackTrace();
            return false;
        }
    }

    //Call to update UI with collected data
    protected void onPostExecute(Boolean result) {

        if (result) {
            _mainActivity.updateUIWithData();
        }
    }

}
