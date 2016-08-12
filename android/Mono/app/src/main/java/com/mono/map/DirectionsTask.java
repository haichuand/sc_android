package com.mono.map;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to retrieve all positions belonging to a path between a start and end
 * position using the Google Maps Directions API.
 *
 * @author Gary Ng
 */
public class DirectionsTask extends AsyncTask<String, Void, List<LatLng>> {

    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String API_KEY = "AIzaSyC7w48Efb57G0XNMCndsPhaOSoYUhR4xTU";

    private static final int MAX_POINTS = 23;
    private static final double MAX_DISTANCE = 500000d;

    private List<LatLng> positions;
    private DirectionListener listener;

    public DirectionsTask(List<LatLng> positions, DirectionListener listener) {
        this.positions = positions;
        this.listener = listener;
    }

    @Override
    protected List<LatLng> doInBackground(String... params) {
        List<LatLng> result = new ArrayList<>();

        try {
            LatLng lastPoint = null;

            while (!positions.isEmpty()) {
                List<LatLng> route = new ArrayList<>();
                // Extract Local Route from Positions
                do {
                    if (lastPoint == null || SphericalUtil.computeDistanceBetween(lastPoint,
                            positions.get(0)) < MAX_DISTANCE) {
                        lastPoint = positions.remove(0);
                        route.add(lastPoint);
                    } else {
                        lastPoint = null;
                        break;
                    }
                } while (!positions.isEmpty() && route.size() < MAX_POINTS);
                // Start Position
                LatLng origin = route.remove(0);
                if (route.isEmpty()) {
                    result.add(origin);
                    continue;
                }
                // End Position
                LatLng destination = route.remove(route.size() - 1);
                // Directions URL
                String url = String.format(
                    "%s?origin=%s&destination=%s&waypoints=enc:%s:&key=%s",
                    DIRECTIONS_URL,
                    origin.latitude + "," + origin.longitude,
                    destination.latitude + "," + destination.longitude,
                    !route.isEmpty() ? PolyUtil.encode(route) : "",
                    API_KEY
                );
                // Request + Response
                StringBuilder builder = new StringBuilder();

                InputStream inputStream = new URL(url).openConnection().getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String nextLine;
                while ((nextLine = reader.readLine()) != null) {
                    builder.append(nextLine + "\n");
                }
                // Parse JSON Response
                try {
                    route.clear();
                    route = parse(builder.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // Start Position
                result.add(origin);
                // Route Positions
                result.addAll(route);
                // End Position
                result.add(destination);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    protected void onPostExecute(List<LatLng> result) {
        if (listener != null) {
            listener.onFinish(result);
        }
    }

    /**
     * Parse JSON string for route information.
     *
     * @param data The value of the JSON string.
     * @return a list of positions.
     * @throws JSONException
     */
    private List<LatLng> parse(String data) throws JSONException {
        List<LatLng> result = new ArrayList<>();

        JSONObject json = new JSONObject(data);
        String status = json.getString("status");

        if (status.equalsIgnoreCase("OK")) {
            JSONArray routes = json.getJSONArray("routes");

            JSONObject route = routes.getJSONObject(0);
            result = PolyUtil.decode(route.getJSONObject("overview_polyline").getString("points"));
        } else {
            System.err.println(status);

            if (json.has("error_message")) {
                System.err.println(json.getString("error_message"));
            }
        }

        return result;
    }

    public interface DirectionListener {

        void onFinish(List<LatLng> result);
    }
}
