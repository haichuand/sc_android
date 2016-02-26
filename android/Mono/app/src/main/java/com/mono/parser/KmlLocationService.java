package com.mono.parser;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.ResultReceiver;
import android.util.Log;

import com.mono.db.DatabaseHelper;
import com.mono.db.dao.EventDataSource;
import com.mono.db.dao.LocationDataSource;
import com.mono.dummy.KML;
import com.mono.model.Location;
import com.mono.parser.KmlParser;
import com.mono.parser.LatLngTime;

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
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by xuejing on 2/22/16.
 */
public class KmlLocationService extends IntentService{
    private static final String TAG = "KmlLocationService";
    private final String GOOGLE_API_KEY = "AIzaSyA-xbzcIj0WtqWJk69PwSKhS3fhGw-KDwU";
    private final String TYPE = "userstay";
    private EventDataSource eventDataSource;
    private LocationDataSource locationDataSource;
    private Context context;
    private ResultReceiver mReceiver;
    private KmlParser parser;
    private HashMap<Integer, LatLngTime> map;
    ArrayList<LatLngTime> userStays;

    public KmlLocationService () {
        super(TAG);
    }

    public void onHandleIntent (Intent intent) {
        eventDataSource = DatabaseHelper.getDataSource(this.context, EventDataSource.class);
        locationDataSource = DatabaseHelper.getDataSource(this.context, LocationDataSource.class);
        getAndSaveNewUserStayEvents();

    }

    public void getAndSaveNewUserStayEvents() {
        userStays = parser.parse(KML.KML_FILENAME);
        if(userStays == null) {
            Log.d(TAG, "No userstay available!");
            return;
        }for(LatLngTime llt : userStays) {
            long event_id = eventDataSource.createEvent(-1,"","","",-1,llt.getStartTime(), llt.getEndTime(),
                    llt.getStartTime(),this.TYPE);
            writeLocationAndEventToDB(String.valueOf(llt.getLat()), String.valueOf(llt.getLng()), event_id);
        }
    }

    /**
     * write corresponding location and event(TYPE: userstay) from given latlong and write them into database tables
     */
    private boolean writeLocationAndEventToDB(String latitude, String longitude, long event_id) {
        new googleplaces().execute(latitude, longitude, String.valueOf(event_id));
        return true;
    }

    private class googleplaces extends AsyncTask<String, Void, String> {
        String temp;
        ArrayList<Location> locationList;
        long event_id;

        protected String doInBackground(String ... params) {
            String latitude = params[0];
            String longitude = params[1];
            event_id = Long.parseLong(params[2]);
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude +
                    "&radius=10&key=" + GOOGLE_API_KEY;
            temp = makeCall(url);
            return "";
        }
        protected void onPostExecute(String result) {
            if(temp != null) {
                locationList = parseGooglePlace(temp);
                //todo: pick the location if the user had been there
                for(Location location: locationList) {
                    if(location != null) {
                        locationDataSource.createLocationAsCandidates(location.name, location.googlePlaceId,
                                location.latitude,location.longitude,location.address.toString(), event_id);
                    }

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
    private static ArrayList<Location> parseGooglePlace(final String response) {
        ArrayList<Location> temp = new ArrayList<Location>();
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
