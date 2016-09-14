package com.mono.parser;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mono.EventManager;
import com.mono.MainActivity;
import com.mono.SuperCalyPreferences;
import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.EventDataSource;
import com.mono.db.dao.LocationDataSource;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.parser.KmlParser;
import com.mono.parser.LatLngTime;
import com.mono.settings.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
    ArrayList<KmlEvents> newuserStays;
    SharedPreferences sharedPreferences;

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
            String type = intent.getStringExtra(KmlDownloadingService.TYPE);
            parser = new KmlParser();
            if(type.equals(KmlDownloadingService.FIRST_TIME)) {
                fileName = intent.getStringExtra("fileName");
                getNewUserstaysForOneDay(fileName);
                //delete file when parsing is done
                String storage = Environment.getExternalStorageDirectory().getPath() + "/";
                File file = new File(storage + MainActivity.APP_DIR + fileName);
                if (file.exists()) {
                    file.delete();
                    Log.d(TAG, fileName + "has been parsed and deleted!");
                }
            }
            if(type.equals(KmlDownloadingService.REGULAR)) {
                getNewUserstaysForOneDay(KmlDownloadingService.KML_FILENAME_TODAY);
            }
        }
    }

    public void getNewUserstaysForOneDay(String fileName) {
        userStays = parser.parse(fileName);

        if(userStays == null) {
            return;
        }
        else if ( userStays.size() == 0)
        {
            newuserStays = parser.newKmlParse(fileName);
            Log.d(TAG, "kml files of new Format");

            if(newuserStays == null)
            {
                Log.d(TAG, "No userStay data parsed");
                return;
            }

            saveNewUserStayKMLEvents();
            Log.d(TAG, "Parsing for One Day from " + fileName + " is " + newuserStays.size());

        }
        else {
            saveNewUserStayEvents();
            Log.d(TAG, "Parsing for One Day from " + fileName + " is " + userStays.size());
        }
    }


    public void saveNewUserStayEvents() {

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

    public void saveNewUserStayKMLEvents() {

        for(KmlEvents kmlevent : newuserStays) {
            if(eventManager == null) {
                Log.d(TAG, "eventManager is null");
                return;
            }

            Event userstayEventByStart = eventManager.getUserstayEventByStartTime(kmlevent.getStartTime());
            Event userstayEventByEnd = eventManager.getUserstayEventByEndTime(kmlevent.getEndTime());

            //the userstay is stored in previous parsing
            // and the time duration does not change in the current parsing
            if(userstayEventByEnd != null) {
                continue;
            }

            if( userstayEventByStart != null) {
                //the userstay is stored in previous parsing
                // and the time duration extended in the current parsing
                if(userstayEventByStart.endTime != kmlevent.getEndTime()) {
                    eventManager.updateEventTime(userstayEventByStart.id, kmlevent.getStartTime(), kmlevent.getEndTime());
                }
            }
            else {
                writeLocationAndEventToDB(kmlevent);
            }
        }
    }


    private  boolean writeLocationAndEventToDB(KmlEvents kmlevent)
    {
        new detailedEvent().execute(kmlevent);
        return true;
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
            // String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude +
            //      "&radius=50&key=" + GOOGLE_API_KEY;
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latitude + "," + longitude +"&rankby=distance&type=establishment&key="+ GOOGLE_API_KEY;
            requestResult = makeCall(url);
            return "";
        }

        protected void onPostExecute(String result) {
            if(requestResult != null) {
                locationList = parseGooglePlace(requestResult, address, place_id, llt);
                long locationId;
                String firstPlaceName = "";
                //todo: pick the location if the user had been there
                //now simply pick the first location of returned locations
                if(!locationList.isEmpty()) {

                    Location firstLocation = locationList.get(0);

                    if(locationList.size() >= 2)
                    {
                        firstLocation = locationList.get(1);
                    }

                    firstPlaceName = firstLocation.name;
                    firstLocation.setAddress(locationList.get(0).address);
                    //check if the location exists in the database
                    if (locationDataSource.getLocationByGooglePlaceId(firstLocation.googlePlaceId) == null) {
                        locationId = locationDataSource.createLocation(firstLocation.name, firstLocation.googlePlaceId, firstLocation.getLatitude(), firstLocation.getLongitude(), firstLocation.getAddress());
                        //todo:this id would be used to map a location record to a event in eventLocationTable
                        firstLocation.id = locationId;
                    }

                    //get location info from shared pref to check any user defined address present
                    sharedPreferences = getSharedPreferences(SuperCalyPreferences.USER_DEFINED_LOCATION, MODE_PRIVATE);
                    Gson gson = new Gson();
                    String storedHashMapString = sharedPreferences.getString(SuperCalyPreferences.USER_DEFINED_LOCATION, "default");
                    java.lang.reflect.Type type = new TypeToken<HashMap<String, Location>>(){}.getType();
                    if(storedHashMapString != "default") {
                        HashMap<String, Location> testHashMap = gson.fromJson(storedHashMapString, type);
                        //use values
                        Location toastString = testHashMap.get(firstPlaceName);
                        if (toastString != null) {
                            Log.i("testtoast", toastString.name);
                            //create a userstay event
                            event_id = eventManager.createEvent(0, -1, -1, null, Event.TYPE_USERSTAY, firstPlaceName, address, toastString,
                                    1, startTime, endTime, null, null, false, null, null, null);
                            Log.d(TAG, "event with id: " + event_id + " created");
                        }
                        else
                        {
                            event_id = eventManager.createEvent(0, -1, -1, null, Event.TYPE_USERSTAY, firstPlaceName, address, firstLocation,
                                    1, startTime, endTime, null, null, false, null, null, null);
                            Log.d(TAG, "event with id: " + event_id + " created");
                        }
                    }
                    else
                    {
                        //create a userstay event
                        event_id = eventManager.createEvent(0, -1, -1, null, Event.TYPE_USERSTAY, firstPlaceName, address, firstLocation,
                                1, startTime, endTime, null, null, false, null, null, null);
                        Log.d(TAG, "event with id: " + event_id + " created");
                    }
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

    private class detailedEvent extends AsyncTask<Object, Void, String> {
        String requestResult;
        String requestResult2;
        String notes = "";
        String distance = "";
        KmlEvents kmlevent = null;
        String event_id;
        Boolean isPresent;

        protected String doInBackground(Object ... params) {
            kmlevent = (KmlEvents) params[0];
            String latitude = String.valueOf(kmlevent.getLat());
            String longitude = String.valueOf(kmlevent.getLng());
            Log.d(TAG, "LatLong in the detailedAddress: "+ latitude + ", " +longitude);
            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude+ ","+ longitude +"&location_type=ROOFTOP&result_type=street_address"+ "&key=" + GOOGLE_API_KEY;
            requestResult = makeCall(url);
            //Driving events additions
            if(kmlevent.getName().equalsIgnoreCase("Driving"))
            {
                String[] arr = kmlevent.getNotes().split(" ");
                if(arr.length == 3)
                {
                    String url2 = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + arr[1]+ ","+ arr[0] +"&location_type=ROOFTOP&result_type=street_address"+ "&key=" + GOOGLE_API_KEY;
                    requestResult2 = makeCall(url2);
                    distance = arr[2];
                }
            }
            return "";
        }

        protected void onPostExecute(String result) {
            if(requestResult != null) {

                String[] detailResult = getAddressByLatLong(requestResult);
                String[] detailResult2 = null;
                if(requestResult2 != null)
                {
                    detailResult2  = getAddressByLatLong(requestResult2);
                }
                long locationId;
                if(detailResult != null) {

                    String placeId =  detailResult[1];
                    String address[] = kmlevent.getAddress().split(",");
                    Location location = new Location(kmlevent.getName(), placeId, kmlevent.getLat(), kmlevent.getLng(), address);

                    if(detailResult2 != null)
                    {
                        if(detailResult2[0]!= null)
                        {
                            if ((detailResult[0]!= null)&&(detailResult2[0]!=null)) {
                                notes = "Driving from :" + detailResult[0] + " To " + detailResult2[0] + "\n";
                            }
                        }
                        if(distance != "")
                        {
                            double disinMiles = Integer.parseInt(distance.split("m")[0]) * 0.00062;
                            notes += "Distance Travelled :" + disinMiles + "miles";
                        }

                    }
                    else {
                        notes = kmlevent.getAddress();
                    }

                    if (locationDataSource.getLocationByGooglePlaceId(placeId) == null) {
                        locationId = locationDataSource.createLocation(kmlevent.getName(), placeId, kmlevent.getLat(),kmlevent.getLng(), kmlevent.getAddress());
                        //todo:this id would be used to map a location record to a event in eventLocationTable
                        location.id = locationId;
                    }



                    //get location info from shared pref to check any user defined address present
                    sharedPreferences = getSharedPreferences(SuperCalyPreferences.USER_DEFINED_LOCATION, MODE_PRIVATE);
                    Gson gson = new Gson();
                    String storedHashMapString = sharedPreferences.getString(SuperCalyPreferences.USER_DEFINED_LOCATION, "default");
                    java.lang.reflect.Type type = new TypeToken<HashMap<String, Location>>(){}.getType();
                    if(storedHashMapString != "default") {
                        HashMap<String, Location> testHashMap = gson.fromJson(storedHashMapString, type);
                        //use values
                        Location toastString = testHashMap.get(kmlevent.getName());
                        isPresent = checkEventOverlap(kmlevent, notes, location);
                        if(!isPresent) {
                            if (toastString != null) {
                                Log.i("testtoast", toastString.name);
                                //create a userstay event
                                event_id = eventManager.createEvent(0, -1, -1, null, Event.TYPE_USERSTAY, location.name, notes, toastString,
                                        1, kmlevent.getStartTime(), kmlevent.getEndTime(), null, null, false, null, null, null);
                                Log.d(TAG, "event with id: " + event_id + " created");
                            } else {
                                event_id = eventManager.createEvent(0, -1, -1, null, Event.TYPE_USERSTAY, location.name, notes, location,
                                        1, kmlevent.getStartTime(), kmlevent.getEndTime(), null, null, false, null, null, null);
                                Log.d(TAG, "event with id: " + event_id + " created");
                            }
                        }
                    }
                    else
                    {
                        isPresent = checkEventOverlap(kmlevent, notes, location);
                        if(!isPresent) {
                            //create a userstay event
                            event_id = eventManager.createEvent(0, -1, -1, null, Event.TYPE_USERSTAY, location.name, notes, location,
                                    1, kmlevent.getStartTime(), kmlevent.getEndTime(), null, null, false, null, null, null);
                            Log.d(TAG, "event with id: " + event_id + " created");
                        }
                    }
                }

            }
        }

    }

    private Boolean checkEventOverlap(KmlEvents kmlevent,String notes,Location location)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(kmlevent.getStartTime());

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH);
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);

        long[] calendarIds = Settings.getInstance(getApplicationContext()).getCalendarsArray();
        List<Event> events = eventManager.getEvents(mYear, mMonth, mDay, calendarIds);

        for (Event event : events) {

            long diff = kmlevent.getEndTime()-event.endTime;

            if((kmlevent.getStartTime() >= event.startTime)&& (kmlevent.getEndTime()-event.endTime<900000)) // end time window of 15 minutes
            {
                event.location = location;
                event.description = notes;
                return true;
            }

        }
      return false;
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
        if(realAddress != null)
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

