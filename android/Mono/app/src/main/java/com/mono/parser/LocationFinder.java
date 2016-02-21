package com.mono.parser;

/**
 * Created by xuejing on 2/17/16.
 */

import android.os.AsyncTask;
import android.util.Log;

import com.mono.model.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;


public class LocationFinder {

    private String Google_API_KEY;
    private ArrayList<Location> locationList;

    public LocationFinder(){
        this.Google_API_KEY = "AIzaSyA-xbzcIj0WtqWJk69PwSKhS3fhGw-KDwU";
    }

    /**
     * get a list of location candidates from given latlong and write them into database table
     */
    public boolean writeLocationToDB(String latitude, String longitude) {
        new googleplaces().execute(latitude, longitude);
        return true;
    }

    private class googleplaces extends AsyncTask<String, Void, String> {
        String temp;

        protected String doInBackground(String ... params) {
            String latitude = params[0];
            String longitude = params[1];
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude + "&radius=10&key=" + Google_API_KEY;
            temp = makeCall(url);
            return "";
        }
        protected void onPostExecute(String result) {
            if(temp != null) {
                locationList = parseGoogleParse(temp);
                for(Location location: locationList) {
                    //TODO:write the result to database
                    System.out.println(location.name);
                }
            }
        }

    }

    public static String makeCall(String urlString) {
        URL url;
        HttpsURLConnection urlConnection = null;
        StringBuilder builder = new StringBuilder();
        try {
            url = new URL(urlString);
            urlConnection = (HttpsURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            InputStreamReader isw = new InputStreamReader(in);
            long code = urlConnection.getResponseCode();
            Log.d("PlaceFinder","http request result: " + code);
            int data = isw.read();
            while (data != -1) {
                builder.append((char)data);
                data = isw.read();
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            urlConnection.disconnect();
        }
        return builder.toString();
    }

    /**
     *
     * @param response
     * @return List of Places return by google place web service
     */
    private static ArrayList parseGoogleParse(final String response) {
        ArrayList temp = new ArrayList();
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.has("results")) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                for (int i = 0; i < jsonArray.length(); i++) {
                    Location location = null;
                    if (jsonArray.getJSONObject(i).has("name")) {
                        String name = jsonArray.getJSONObject(i).optString("name");
                        String[] address = jsonArray.getJSONObject(i).optString("vicinity").split(",");
                        String googlePlaceId = jsonArray.getJSONObject(i).optString("place_id");
                        Double latitude = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").optDouble("lat");
                        Double longitude = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").optDouble("lng");
                        location = new Location(name, googlePlaceId, latitude, longitude, address);
                    }
                    if(location != null)
                        temp.add(location);
                }

            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return temp;
    }
}
