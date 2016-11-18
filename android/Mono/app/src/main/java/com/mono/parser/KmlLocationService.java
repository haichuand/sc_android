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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by xuejing on 2/22/16.
 */
public class KmlLocationService extends IntentService {
    private static final String TAG = "KmlLocationService";
    private final String GOOGLE_API_KEY = "AIzaSyDYP8RiorJWNGwP8gSuaxoevvFQkyJH_6c";
    private static final String TYPE = "userstay";
    private EventManager eventManager;
    private LocationDataSource locationDataSource;
    private int loopNumber =0;

    private KmlParser parser;
    ArrayList<LatLngTime> userStays;
    ArrayList<KmlEvents> newuserStays;
    SharedPreferences sharedPreferences;

    public KmlLocationService() {
        super(TAG);
    }

    public void onCreate() {
        super.onCreate();
        eventManager = EventManager.getInstance(this.getApplicationContext());
        locationDataSource = DatabaseHelper.getDataSource(this.getApplicationContext(), LocationDataSource.class);
    }

    public void onHandleIntent(Intent intent) {
        if (intent != null) {
            String data = intent.getStringExtra("dataString");
            loopNumber = intent.getIntExtra("loopNumber", 0);
            parser = new KmlParser();
            getNewUserstaysForOneDay(data);
        }
    }

    public void getNewUserstaysForOneDay(String data) {
        newuserStays = parser.newKmlParse(data);
        if (newuserStays == null) {
           // Log.d(TAG, "No userStay data parsed");
            return;
        }
        saveNewUserStayKMLEvents();
    }

    public void saveNewUserStayKMLEvents() {

        for (KmlEvents kmlevent : newuserStays) {
            if (eventManager == null) {
                return;
            }

            Event userstayEventByStart = eventManager.getUserstayEventByStartTime(kmlevent.getStartTime());
            Event userstayEventByEnd = eventManager.getUserstayEventByEndTime(kmlevent.getEndTime());

            //the userstay is stored in previous parsing
            // and the time duration does not change in the current parsing
            if (userstayEventByEnd != null) {
                continue;
            }

            if (userstayEventByStart != null) {
                //the userstay is stored in previous parsing
                // and the time duration extended in the current parsing
                if (userstayEventByStart.endTime != kmlevent.getEndTime()) {
                    eventManager.updateEventTime(userstayEventByStart.id, kmlevent.getStartTime(), kmlevent.getEndTime());
                }
            } else {
                writeLocationAndEventToDB(kmlevent);
            }
        }
    }

    /**
     * write corresponding location and event(TYPE: userstay) from given latlong and write them into database tables
     */

    private boolean writeLocationAndEventToDB(KmlEvents kmlevent) {
        new detailedEvent().execute(kmlevent);
        return true;
    }


    private class detailedEvent extends AsyncTask<Object, Void, String> {
        String requestResult;
        KmlEvents kmlevent = null;
        Event event;
       // String event_id;


        protected String doInBackground(Object... params) {
            kmlevent = (KmlEvents) params[0];
            String latitude = String.valueOf(kmlevent.getLat());
            String longitude = String.valueOf(kmlevent.getLng());
            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude + "&location_type=ROOFTOP&result_type=street_address" + "&key=" + GOOGLE_API_KEY;
            requestResult = makeCall(url);
            event = createEvent(requestResult, kmlevent);
            return "";
        }

        protected void onPostExecute(String result) {

            if (event != null) {
                if ((loopNumber % 30 == 0) || (loopNumber == 365)) {
                    if (!checkEventOverlap(kmlevent, event.location, event)) {
                        GlobalEventList.getInstance().monthList.add(event);
                        eventManager.createEvents(EventManager.EventAction.ACTOR_NONE, GlobalEventList.getInstance().monthList, null);
                        GlobalEventList.getInstance().monthList.clear();
                    }
                } else {
                    if (!checkEventOverlap(kmlevent, event.location, event)) {
                        GlobalEventList.getInstance().monthList.add(event);
                    }
                }
            }

        }
    }

    private Event createEvent(String requestResult, KmlEvents kmlevent)
    {
        String notes = "";
        String distance = "";
        Event event;
        if (requestResult != null) {

            String[] detailResult = getAddressByLatLong(requestResult);
            long locationId;
            if (detailResult != null) {

                String placeId = detailResult[1];
                String address[] = kmlevent.getAddress().split(",");
                distance = kmlevent.getNotes().trim();
                Location location = new Location(kmlevent.getName(), placeId, kmlevent.getLat(), kmlevent.getLng(), address);
                if (distance != "") {
                    String disinMiles = String.format("%.2f", Double.parseDouble(distance.split("m")[0]) * 0.000621371);
                    if (!disinMiles.equals("0.00")) {
                        notes = "Distance Travelled : " + disinMiles + "miles";
                    } else {
                        notes = kmlevent.getAddress();
                    }
                }

                if (locationDataSource.getLocationByGooglePlaceId(placeId) == null) {
                    locationId = locationDataSource.createLocation(kmlevent.getName(), placeId, kmlevent.getLat(), kmlevent.getLng(), kmlevent.getAddress());
                    //todo:this id would be used to map a location record to a event in eventLocationTable
                    location.id = locationId;
                }

                //get location info from shared pref to check any user defined address present
                sharedPreferences = getSharedPreferences(SuperCalyPreferences.USER_DEFINED_LOCATION, MODE_PRIVATE);
                Gson gson = new Gson();
                String storedHashMapString = sharedPreferences.getString(SuperCalyPreferences.USER_DEFINED_LOCATION, "default");
                java.lang.reflect.Type type = new TypeToken<HashMap<String, Location>>() {
                }.getType();
                if (storedHashMapString != "default") {
                    HashMap<String, Location> testHashMap = gson.fromJson(storedHashMapString, type);
                    //use values
                    Location toastString = testHashMap.get(kmlevent.getName());


                    event = new Event(Event.TYPE_USERSTAY);
                    event.title = location.name;
                    event.description = notes;
                    event.startTime = kmlevent.getStartTime();
                    event.endTime = kmlevent.getEndTime();

                    if (toastString != null) {
                        event.location = toastString;
                        location = toastString;
                        event.title = toastString.name;
                    } else {
                        event.location = location;
                    }


                } else {

                    //create a userstay event
                    event = new Event(Event.TYPE_USERSTAY);
                    event.title = location.name;
                    event.description = notes;
                    event.location = location;
                    event.startTime = kmlevent.getStartTime();
                    event.endTime = kmlevent.getEndTime();


                }
                return event;
            }

        }
       return null;
    }

    private Boolean checkEventOverlap(KmlEvents kmlevent, Location location, Event event) {

        Boolean eventFound = false;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(kmlevent.getStartTime());
        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH);
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);

        long[] calendarIds = Settings.getInstance(getApplicationContext()).getCalendarsArray();
        List<Event> events = eventManager.getEvents(mYear, mMonth, mDay, calendarIds);

        //make changes to all the events
        for (int i = 0; i < events.size(); i++) {

            if ((events.get(i).startTime >= kmlevent.getStartTime()) && (events.get(i).endTime <= kmlevent.getEndTime()))// if event lies in between userstay period
            {
                if(location != null)
                {
                    List<Location> tempLocations = new ArrayList<>();
                    tempLocations.add(location);
                    events.get(i).tempLocations = tempLocations;
                    eventManager.updateEvent(
                            EventManager.EventAction.ACTOR_SELF,
                            events.get(i),
                            null
                    );
                    eventFound = true;
                }


            }

        }
        return eventFound;
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
            if (code == HttpsURLConnection.HTTP_OK) {
                int data = isw.read();
                while (data != -1) {
                    builder.append((char) data);
                    data = isw.read();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
        return builder.toString();
    }


    public static String[] getAddressByLatLong(String detailedAddressJson) {
        String addressInfo[] = {null, null};
        try {
            JSONObject jsonObject = new JSONObject(detailedAddressJson);
            if (jsonObject.has("results")) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                if (jsonArray.length() != 0){
                    if (jsonArray.getJSONObject(0).has("formatted_address")) {
                        addressInfo[0] = jsonArray.getJSONObject(0).optString("formatted_address");
                    }
                    if (jsonArray.getJSONObject(0).has("place_id")) {
                        addressInfo[1] = jsonArray.getJSONObject(0).optString("place_id");
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return addressInfo;
    }
}



