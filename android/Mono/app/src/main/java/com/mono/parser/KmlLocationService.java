package com.mono.parser;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.mono.EventManager;
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
    private static final String TYPE = "userstay";
    private EventManager eventManager;
    private LocationDataSource locationDataSource;
    private String fileName = "";
    private KmlParser parser;
    ArrayList<LatLngTime> userStays;

    public KmlLocationService () {
        super(TAG);
    }

    public void onCreate () {
        super.onCreate();
        eventManager = EventManager.getInstance(this.getApplicationContext());
        locationDataSource = DatabaseHelper.getDataSource(this.getApplicationContext(), LocationDataSource.class);
    }

    public void onHandleIntent (Intent intent) {
        if(intent != null) {
            fileName = intent.getStringExtra("fileName");
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
            if(eventManager == null) {
                Log.d(TAG, "eventManager is null");
                return;
            }

            Event userstayEventByStart = eventManager.getUserstayEventByStartTime(llt.getStartTime());
            Event userstayEventByEnd = eventManager.getUserstayEventByEndTime(llt.getEndTime());

            //the userstay is stored in previous parsing
            // and the time duration does not change in the current parsing
            if(userstayEventByEnd != null) {
                continue;
            }

            if( userstayEventByStart != null) {
                //the userstay is stored in previous parsing
                // and the time duration extended in the current parsing
                if(userstayEventByStart.endTime != llt.getEndTime()) {
                    eventManager.updateEventTime(userstayEventByStart.id, llt.getStartTime(), llt.getEndTime());
                }
            }
            else {
                writeLocationAndEventToDB(llt);
            }
        }
    }

    /**
     * write corresponding location and event(TYPE: userstay) from given latlong and write them into database tables
     */
    private boolean writeLocationAndEventToDB(LatLngTime llt) {
        new detailedAddress().execute(llt);
        return true;
    }

    private class googleplaces extends AsyncTask<Object, Void, String> {
        String requestResult;

        ArrayList<Location> locationList;
        LatLngTime llt;
        String latitude;
        String longitude;
        String address;
        String place_id;
        String event_id;
        Long startTime;
        Long endTime;

        protected String doInBackground(Object ... params) {
            llt = (LatLngTime) params[0];
            latitude = String.valueOf(llt.getLat());
            longitude = String.valueOf(llt.getLng());
            startTime = llt.getStartTime();
            endTime = llt.getEndTime();
            address = (String)params[1];
            place_id = (String)params[2];
            Log.d(TAG, "google place LatLong: " + latitude + ", " +longitude);
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude +
                    "&radius=50&key=" + GOOGLE_API_KEY;
            requestResult = makeCall(url);
            return "";
        }

        protected void onPostExecute(String result) {
            if(requestResult != null) {
                locationList = parseGooglePlace(requestResult, address, place_id, llt);
                //TODO: remove later, for testing purpose only:
                String placeCandidates = "Place candidates: ";
                String locationId = "";
                int counter = 1;
                //todo: pick the location if the user had been there
                //now simply pick the first location of returned locations
                if(!locationList.isEmpty()) {
                    for (Location location : locationList) {
                        locationId = location.id;
                        //check if the location exists in the database
                        if (locationDataSource.getLocationByGooglePlaceId(location.googlePlaceId) == null) {
                            locationId = locationDataSource.createLocation(location.name, location.googlePlaceId, location.latitude, location.longitude, location.getAddress());
                            location.id = locationId;
                        }
                        placeCandidates += "Place " + counter + ". " + location.name + "\n";
                        counter++;
                    }
                    Location location = locationList.get(0);
                    event_id = eventManager.createEvent(-1, -1, null, Event.TYPE_USERSTAY, address, placeCandidates, null,
                            1,startTime, endTime, null, null, false);
                    Log.d(TAG, "event with id: " + event_id + " created");
                }
            }
        }

    }

    private class detailedAddress extends AsyncTask<Object, Void, String> {
        String requestResult;
        LatLngTime llt = null;

        protected String doInBackground(Object ... params) {
            llt = (LatLngTime) params[0];
            String latitude = String.valueOf(llt.getLat());
            String longitude = String.valueOf(llt.getLng());
            Log.d(TAG, "LatLong in the detailedAddress: "+ latitude + ", " +longitude);
            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude+ ","+ longitude +"&location_type=ROOFTOP&result_type=street_address"+ "&key=" + GOOGLE_API_KEY;
            requestResult = makeCall(url);
            return "";
        }

        protected void onPostExecute(String result) {
            if(requestResult != null) {
                String[] detailResult = getAddressByLatLong(requestResult);

                if(detailResult != null) {
                    new googleplaces().execute(llt, detailResult[0], detailResult[1]);
                }

            }
        }

    }

    private class PlaceDetailByPlaceId extends AsyncTask<String, Void, String> {
        String requestResult;
        String place_id;

        protected String doInBackground(String ... params) {
            place_id = params[0];
            Log.d(TAG, "Place_id get from detailed address: "+ place_id);
            String url = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" + place_id +"&key=" + GOOGLE_API_KEY;
            requestResult = makeCall(url);
            return "";
        }

        protected void onPostExecute(String result) {
            if(requestResult != null) {
                String detailResult = getPlaceDetail(requestResult);
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
    private static ArrayList<Location> parseGooglePlace(final String response, final String realAddress, final String placeId, final LatLngTime llt) {
        ArrayList<Location> locationList = new ArrayList<Location>();
        //add the real address get from the coordinates first
        locationList.add(new Location(realAddress, placeId, llt.getLat(), llt.getLng(), realAddress.split(",")));

        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.has("results")) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                for (int i = 0; i < jsonArray.length(); i++) {
                    Location location = null;
                    if (jsonArray.getJSONObject(i).has("name")) {
                        String name = jsonArray.getJSONObject(i).optString("name");
                        String googlePlaceId = jsonArray.getJSONObject(i).optString("place_id");
                        String[] placeAddress = jsonArray.getJSONObject(i).optString("vicinity").split(",");
                        Double latitude = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").optDouble("lat");
                        Double longitude = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").optDouble("lng");
                        location = new Location(name, googlePlaceId, latitude, longitude, placeAddress);
                        Log.d(TAG, location.toString());
                    }
                    if(location != null)
                        locationList.add(location);
                }

            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return locationList;
    }

    public static String[] getAddressByLatLong(String detailedAddressJson) {
        String addressInfo[] = {null, null} ;
        try {
            JSONObject jsonObject = new JSONObject(detailedAddressJson);
            if (jsonObject.has("results")) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                if (jsonArray.getJSONObject(0).has("formatted_address")) {
                    addressInfo[0] = jsonArray.getJSONObject(0).optString("formatted_address");
                }
                if (jsonArray.getJSONObject(0).has("place_id")) {
                    addressInfo[1] = jsonArray.getJSONObject(0).optString("place_id");
                }

            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "detailed address: "+ addressInfo[0]);
        return addressInfo;
    }

    public static String getPlaceDetail(String detailedAddressJson) {
        String placeName = null;
        try {
            JSONObject jsonObject = new JSONObject(detailedAddressJson);
            if (jsonObject.has("result")) {
                JSONObject jsonResult= jsonObject.optJSONObject("result");
                if (jsonResult.has("name")) {
                    placeName = jsonResult.optString("name");
                }

            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "place name of the real address: "+ placeName);
        return placeName;
    }
}
