package com.mono.parser;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.EventDataSource;
import com.mono.db.dao.LocationDataSource;
import com.mono.model.Event;
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
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by xuejing on 2/22/16.
 */
public class KmlLocationService extends IntentService{
    private static final String TAG = "KmlLocationService";
    private final String GOOGLE_API_KEY = "AIzaSyDYP8RiorJWNGwP8gSuaxoevvFQkyJH_6c";
    private final String TYPE = "userstay";
    private EventDataSource eventDataSource;
    private LocationDataSource locationDataSource;
    private String fileName = "";
    private Context context;
    private KmlParser parser;
    ArrayList<LatLngTime> userStays;

    public KmlLocationService () {
        super(TAG);
    }

    public void onCreate () {
        super.onCreate();
    }

    public void onHandleIntent (Intent intent) {
        if(intent != null) {
            fileName = intent.getStringExtra("fileName");
            Log.d(TAG, "onhandleIntent: starting service with fileName retrieve from intent: " + fileName);
            eventDataSource = DatabaseHelper.getDataSource(this.context, EventDataSource.class);
            locationDataSource = DatabaseHelper.getDataSource(this.context, LocationDataSource.class);
            parser = new KmlParser();
            getAndSaveNewUserStayEvents(fileName);
        }
    }

    public void getAndSaveNewUserStayEvents(String fileName) {
        userStays = parser.parse(fileName);

        if(userStays == null) {
            Log.d(TAG, "No userstay available!");
            return;
        }
        Log.d(TAG, "The number of userstays parsed from " + fileName + " is " + userStays.size());

        for(LatLngTime llt : userStays) {
            if(eventDataSource == null) {
                Log.d(TAG, "eventDataSource is null");
                return;
            }

            Event userstayEvent = eventDataSource.getUserstayEventByStartTime(llt.getStartTime());

            if( userstayEvent != null) {
                if(userstayEvent.endTime != llt.getEndTime()) {
                    eventDataSource.updateTime(userstayEvent.id, llt.getStartTime(), llt.getEndTime());
                }
            }
            else {
                String event_id = eventDataSource.createEvent(-1, this.TYPE, "Userstay", "", "random location", 12, llt.getStartTime(), llt.getEndTime(),
                        llt.getStartTime());
                Log.d(TAG, "event with id: " + event_id + " created");
                writeLocationAndEventToDB(String.valueOf(llt.getLat()), String.valueOf(llt.getLng()), event_id);
            }
        }
    }

    /**
     * write corresponding location and event(TYPE: userstay) from given latlong and write them into database tables
     */
    private boolean writeLocationAndEventToDB(String latitude, String longitude, String event_id) {
        new googleplaces().execute(latitude, longitude, String.valueOf(event_id));
        return true;
    }

    private class googleplaces extends AsyncTask<String, Void, String> {
        String requestResult;

        ArrayList<Location> locationList;
        String event_id;

        protected String doInBackground(String ... params) {
            String latitude = params[0];
            String longitude = params[1];
            event_id = params[2];
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude +
                    "&radius=10&key=" + GOOGLE_API_KEY;
            requestResult = makeCall(url);
            return "";
        }

        protected void onPostExecute(String result) {
            if(requestResult != null) {
                locationList = parseGooglePlace(requestResult);
                //todo: pick the location if the user had been there
                //now simply pick the first location of returned locations
                if(!locationList.isEmpty()) {
                    Location location = locationList.get(0);
                    new detailedAddress().execute(location, String.valueOf(event_id));
//                    locationDataSource.createLocationAsCandidates(location.name, location.googlePlaceId,
//                            location.latitude,location.longitude,location.getAddress(), event_id);
                }
            }
        }

    }

    private class detailedAddress extends AsyncTask<Object, Void, String> {
        String requestResult;
        Location loc = null;
        String event_id;

        protected String doInBackground(Object ... params) {
            loc = (Location)params[0];
            String place_id = loc.googlePlaceId;
            event_id = (String)params[1];
            String url = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" +place_id+ "&key=" + GOOGLE_API_KEY;
            requestResult = makeCall(url);
            return "";
        }

        protected void onPostExecute(String result) {
            if(requestResult != null) {
                String[] detailedAddress = getAddressByLatLong(requestResult).split(",");
                loc.setAddress(detailedAddress);
                ContentValues values = new ContentValues();
                values.put(DatabaseValues.Event.TITLE, loc.name+" "+loc.getAddress());
                values.put(DatabaseValues.Event.DESC, "dummy description about a userstay event");
                eventDataSource.updateValues(event_id, values);
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
            if(code == HttpsURLConnection.HTTP_OK) {
                int data = isw.read();
                while (data != -1) {
                    builder.append((char) data);
                    data = isw.read();
                }
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
        ArrayList<Location> locationList = new ArrayList<Location>();
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
                        locationList.add(location);
                }

            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "parseGooglePlace: "+ locationList.size());
        return locationList;
    }

    public static String getAddressByLatLong(String detailedAddressJson) {
        String address =  "";
        try {
            JSONObject jsonObject = new JSONObject(detailedAddressJson);
            if (jsonObject.has("result")) {
                JSONObject resultObject = jsonObject.getJSONObject("result");
                if (resultObject.has("formatted_address")) {
                    address = resultObject.optString("formatted_address");
                }

            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "detailed address: "+ address);
        return address;
    }
}
