package com.mono.parser;

/**
 * Created by xuejing on 2/17/16.
 */
import android.os.Environment;
import android.util.Log;

import com.mono.MainActivity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class KmlParser {
    private static final String TAG = "KmlParser";
    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;

    public KmlParser () {
        factory = DocumentBuilderFactory.newInstance();
    }

    public ArrayList<KmlEvents> newKmlParse(String data)
    {

            factory.setNamespaceAware(true);
            ArrayList<KmlEvents> result = new ArrayList<>();
            DateTime kmlDate = null;
            try {
                builder = factory.newDocumentBuilder();
                InputStream inputStream = new ByteArrayInputStream(data.getBytes("UTF-8"));
                Document document = builder.parse(inputStream);

                NodeList nodeList = document.getDocumentElement().getChildNodes();
                Node node = nodeList.item(0);

                String dateStr = node.getChildNodes().item(0).getChildNodes().item(0).getNodeValue().split("to")[2].trim();
                kmlDate = DateTime.parse(dateStr);

                if(node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    NodeList placemarksList = elem.getElementsByTagName("Placemark");

                    String format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

                    if( placemarksList.getLength()> 0 )
                    {
                        for(int i = 0; i < placemarksList.getLength(); i++) {
                            try{
                                Node placemarkNode = placemarksList.item(i);
                                KmlEvents kmlevent = new KmlEvents();
                                if(placemarkNode.getNodeType() == Node.ELEMENT_NODE)
                                {
                                    String address = "";

                                    String name = placemarkNode.getChildNodes().item(0).getChildNodes().item(0).getNodeValue();

                                    if (!((i == 0)&&(name.equalsIgnoreCase("Driving")))) // ignore days when only event is driving
                                    {
                                        if(placemarkNode.getChildNodes().item(1).hasChildNodes())
                                        {
                                                     address = placemarkNode.getChildNodes().item(1).getChildNodes().item(0).getNodeValue();
                                         }

                                    String description = placemarkNode.getChildNodes().item(3).getChildNodes().item(0).getNodeValue();
                                    String gxTrackFrom = placemarkNode.getChildNodes().item(5).getChildNodes().item(1).getChildNodes().item(0).getNodeValue();
                                    String TimeSpanStart = placemarkNode.getChildNodes().item(6).getChildNodes().item(0).getChildNodes().item(0).getNodeValue();
                                    String TimeSpanEnd = placemarkNode.getChildNodes().item(6).getChildNodes().item(1).getChildNodes().item(0).getNodeValue();

                                    //To get Start Time and end time
                                         SimpleDateFormat formatter = new SimpleDateFormat(format);
                                         Date startdate = formatter.parse(TimeSpanStart);
                                         long startmillis = startdate.getTime();
                                         Date enddate = formatter.parse(TimeSpanEnd);
                                         long endmillis = enddate.getTime();

                                        TimeZone tz = TimeZone.getDefault();
                                        long offset = tz.getRawOffset() + tz.getDSTSavings();

                                        startmillis +=offset;
                                        endmillis +=offset;

                                    kmlevent.setStartTime(startmillis);
                                    kmlevent.setEndTime(endmillis);

                                    //Get distance for notes
                                    String[] descriptionArray = description.split("Distance");
                                    String notes = "";

                                    //get coordinates
                                    String[] split = gxTrackFrom.split(" ");
                                    double lng = Double.valueOf(split[0]);
                                    double lat = Double.valueOf(split[1]);

                                        notes += descriptionArray[1].trim();
                                        if((descriptionArray[1].trim().equalsIgnoreCase("0m")) && (name.equalsIgnoreCase("Driving"))) {
                                            continue;
                                        }

                                        kmlevent.setLng(lng);
                                        kmlevent.setLat(lat);
                                        kmlevent.setName(name);
                                        kmlevent.setAddress(address);
                                        kmlevent.setNotes(notes);
                                        result.add(kmlevent);
                                }

                                }

                            }
                            catch(Exception ex)
                            {
                                Log.d("kmlParser", ex.getMessage());
                            }

                        }
                    }
                }
            }
            catch (ParserConfigurationException e) {
              //  Log.d("kmlParser", e.getMessage());
            }
            catch (IOException e) {
              //  Log.d("kmlParser", e.getMessage());
            }
            catch (SAXException e) {
             //   Log.d("kmlParser", e.getMessage());
            }
            catch (Exception e) {
               // Log.d("kmlParser", e.getMessage());
            }

            //if any userstay is crossing few days, slice it into multiple userstays per 24 hours
            ArrayList<KmlEvents> resultList = new ArrayList<>();
            for(int i = 0; i < result.size(); i++) {
               // for(KmlEvents llt: userstaySlicing(result.get(i))) {
                    resultList.add(userstaySlicing(result.get(i), kmlDate));
                //}
            }
            return result;
    }

    private KmlEvents userstaySlicing(KmlEvents kmlevent, DateTime kmlDate) {
        if(kmlDate !=null)
        {
            DateTime todayStart = kmlDate.withTimeAtStartOfDay();
            DateTime tomorrowStart = kmlDate.plusDays( 1 ).withTimeAtStartOfDay();
            if(kmlevent.getStartTime() < todayStart.getMillis())
            {
                kmlevent.setStartTime(todayStart.getMillis());
            }
            if(kmlevent.getEndTime() > (tomorrowStart.getMillis() - 60000))
            {
                kmlevent.setEndTime(tomorrowStart.getMillis() - 60000);
            }
        }
        return kmlevent;
    }

    /*
     * Calculate distance between two points in latitude and longitude
     * Uses Haversine method as its base.
     * lat1, lon1 Start point lat2, lon2 End point
     * @returns distance in Meters
    */
    private double distance(double lat1, double lon1, double lat2,
                            double lon2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters


        distance = Math.pow(distance, 2);

        return Math.sqrt(distance);
    }

}
