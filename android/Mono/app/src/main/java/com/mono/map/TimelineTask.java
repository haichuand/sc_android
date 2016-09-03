package com.mono.map;

import android.os.AsyncTask;
import android.webkit.CookieManager;

import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * This class is used to extract route positions out of KMLs retrieved from Google Timeline.
 *
 * @author Gary Ng
 */
public class TimelineTask extends AsyncTask<Object, Void, List<LatLng>> {

    public static final String COOKIE_URL = "https://www.google.com/maps/timeline";
    public static final String KML_URL = "https://www.google.com/maps/timeline/kml";

    private TimelineListener listener;

    public TimelineTask(TimelineListener listener) {
        this.listener = listener;
    }

    @Override
    protected List<LatLng> doInBackground(Object... params) {
        List<LatLng> result = new ArrayList<>();

        int year = (int) params[0];
        int month = (int) params[1]; // 0 - January
        int day = (int) params[2];
        // Get KML Cookie
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(COOKIE_URL);
        // KML URL
        String url = String.format(
            Locale.getDefault(),
            "%s?pb=!1m8!1m3!1i%d!2i%d!3i%d!2m3!1i%d!2i%d!3i%d",
            KML_URL,
            year, month, day, year, month, day
        );

        try {
            // Request + Response
            StringBuilder builder = new StringBuilder();

            URLConnection connection = new URL(url).openConnection();
            connection.addRequestProperty("Cookie", cookie);

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                builder.append(nextLine + "\n");
            }
            // Parse KML Response
            List<LatLng> route = parse(builder.toString());
            // Add Route Positions
            result.addAll(route);
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
     * Parse KML string for route information.
     *
     * @param data KML string.
     * @return a list of positions.
     */
    private List<LatLng> parse(String data) {
        List<LatLng> result = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            InputStream inputStream = new ByteArrayInputStream(data.getBytes("UTF-8"));
            Document document = builder.parse(inputStream);

            NodeList nodeList = document.getDocumentElement().getChildNodes();
            Node node = nodeList.item(0);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                nodeList = element.getElementsByTagName("Placemark");

                for (int i = 0; i < nodeList.getLength(); i++) {
                    element = (Element) nodeList.item(i);
                    NodeList trackList = element.getElementsByTagName("gx:Track");

                    for (int j = 0; j < trackList.getLength(); j++) {
                        element = (Element) trackList.item(j);
                        NodeList coordList = element.getElementsByTagName("gx:coord");

                        for (int k = 0; k < coordList.getLength(); k++) {
                            element = (Element) coordList.item(k);

                            String[] values = element.getFirstChild().getNodeValue().split(" ");
                            double longitude = Double.valueOf(values[0]);
                            double latitude = Double.valueOf(values[1]);

                            result.add(new LatLng(latitude, longitude));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public interface TimelineListener {

        void onFinish(List<LatLng> result);
    }
}
