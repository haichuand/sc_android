package com.mono.parser;

/**
 * Created by xuejing on 2/17/16.
 */
import android.os.Environment;
import android.util.Log;

import com.mono.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
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

    public ArrayList<LatLngTime> parse(String fileName) {

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)){
            String storage = Environment.getExternalStorageDirectory().getPath() + "/";
            Log.d(TAG, "parse(): file: "+ storage+MainActivity.APP_DIR+fileName);
            File file = new File(storage + MainActivity.APP_DIR + fileName);
            ArrayList<LatLngTime> result = new ArrayList<>();
            try {
                builder = factory.newDocumentBuilder();
                Document document = builder.parse(file);
                NodeList nodeList = document.getDocumentElement().getChildNodes();
                //get gx:Track layer
                Node node = nodeList.item(0);

                if(node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    NodeList whenList = elem.getElementsByTagName("when");
                    NodeList locationList = elem.getElementsByTagName("gx:coord");
                    NodeList placemarksList = elem.getElementsByTagName("Placemark");
                    if(whenList == null || locationList == null) {
                        return null;
                    }
                    String format = "yyyy-MM-dd'T'HH:mm:ss.SSS";

                    for(int i = 0; i < whenList.getLength(); i++) {
                        LatLngTime llt = new LatLngTime ();
                        Node whenNode = whenList.item(i);
                        if(whenNode.getNodeType() == Node.ELEMENT_NODE) {
                            String time = whenNode.getChildNodes().item(0).getNodeValue();
                            SimpleDateFormat formatter = new SimpleDateFormat(format);
                            Date date = formatter.parse(time);
                            long millis = date.getTime();
                            llt.setStartTime(millis);
                            llt.setEndTime(millis);
                        }

                        Node locNode = locationList.item(i);
                        if(locNode.getNodeType() == Node.ELEMENT_NODE) {
                            String coordinate = locNode.getChildNodes().item(0).getNodeValue();
                            String[] split = coordinate.split(" ");
                            double lng = Double.valueOf(split[0]);
                            double lat = Double.valueOf(split[1]);
                            llt.setLat(lat);
                            llt.setLng(lng);
                        }
                        result.add(llt);
                    }
                }
            }
            catch (ParserConfigurationException e) {
                Log.d("kmlParser", e.getMessage());
            }
            catch (IOException e) {
                Log.d("kmlParser", e.getMessage());
            }
            catch (SAXException e) {
                Log.d("kmlParser", e.getMessage());
            }
            catch (Exception e) {
                Log.d("kmlParser", e.getMessage());
            }
            ArrayList<LatLngTime> userstayList = getUserstay(result);
            //if any userstay is crossing few days, slice it into multiple userstays per 24 hours
            ArrayList<LatLngTime> resultList = new ArrayList<>();
            for(int i = 0; i < userstayList.size(); i++) {
                for(LatLngTime llt: userstaySlicing(userstayList.get(i))) {
                    resultList.add(llt);
                }
            }
            return resultList;

        }
        return null;
    }

    public ArrayList<KmlEvents> newKmlParse(String fileName)
    {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)){
            String storage = Environment.getExternalStorageDirectory().getPath() + "/";
            Log.d(TAG, "parse(): file: "+ storage+MainActivity.APP_DIR+fileName);
            File file = new File(storage + MainActivity.APP_DIR + fileName);
            ArrayList<KmlEvents> result = new ArrayList<>();
            try {
                builder = factory.newDocumentBuilder();
                Document document = builder.parse(file);
                NodeList nodeList = document.getDocumentElement().getChildNodes();
                Node node = nodeList.item(0);

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
                                    String gxTrackTo = "";
                                    String name = placemarkNode.getChildNodes().item(0).getChildNodes().item(0).getNodeValue();

                                    if (!((i == 0)&&(name.equalsIgnoreCase("Driving")))) // ignore days when only event is driving
                                    {
                                          if(!(name.equalsIgnoreCase("Driving"))) //Non Driving events
                                         {
                                                     address = placemarkNode.getChildNodes().item(1).getChildNodes().item(0).getNodeValue();
                                         }
                                        else
                                          {
                                              // if Driving get destination lat long
                                              gxTrackTo = placemarkNode.getChildNodes().item(5).getLastChild().getChildNodes().item(0).getNodeValue();

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

                                        if(gxTrackTo != "")
                                        {
                                            String[] split2 = gxTrackTo.split(" ");
                                            double lng2 = Double.valueOf(split2[0]);
                                            double lat2 = Double.valueOf(split2[1]);
                                            notes += lng2 + " " +lat2 + " ";
                                        }
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
                Log.d("kmlParser", e.getMessage());
            }
            catch (IOException e) {
                Log.d("kmlParser", e.getMessage());
            }
            catch (SAXException e) {
                Log.d("kmlParser", e.getMessage());
            }
            catch (Exception e) {
                Log.d("kmlParser", e.getMessage());
            }

            //if any userstay is crossing few days, slice it into multiple userstays per 24 hours
            ArrayList<KmlEvents> resultList = new ArrayList<>();
            for(int i = 0; i < result.size(); i++) {
                for(KmlEvents llt: userstaySlicing(result.get(i))) {
                    resultList.add(llt);
                }
            }
            return resultList;

        }
        return null;

    }
    /**
     *
     * @param inputList
     * @return the information of places that user had stayed more than 10 mins
     */
    private ArrayList<LatLngTime> getUserstay(ArrayList<LatLngTime> inputList) {
        ArrayList<LatLngTime> outputList = new ArrayList<>();
        LatLngTime stay = null;
        if(!inputList.isEmpty()) {
            LatLngTime temp = inputList.get(0);
            stay = new LatLngTime(temp.getLat(), temp.getLng(), temp.getStartTime(), temp.getEndTime());
        }
        else {
            return outputList;
        }

        for(int i = 1; i < inputList.size(); i++) {
            LatLngTime temp = inputList.get(i);
            double tempLat = temp.getLat();
            double tempLng = temp.getLng();

            if(distance(tempLat, tempLng, stay.getLat(), stay.getLng()) <= 100) {
                stay.setEndTime(temp.getEndTime());
            }
            else {
                //1200000 milliseconds == 20 mins
                if(stay.getEndTime() - stay.getStartTime() >= 600000){
                    outputList.add(stay);
                }
                //this if-block handles special case of a standalone userstay in first round of iteration
                //standalone userstay: one userstay records but represents a long period of stay
                if(temp.getStartTime() - stay.getEndTime() >= 2400000 && temp.getStartTime()- temp.getEndTime() == 0) {
                    long startTime = stay.getEndTime();
                    //the new userstay will start 10 mins after the previous one(assume 10-mins moving)
                    stay = new LatLngTime(temp.getLat(), temp.getLng(),startTime+600000, temp.getEndTime());
                    outputList.add(stay);
                    if(i < inputList.size()-1) {
                        stay = new LatLngTime(inputList.get(i+1).getLat(), inputList.get(i+1).getLng(),
                                inputList.get(i+1).getStartTime(), inputList.get(i+1).getEndTime());
                        i++;
                    }
                    continue;
                }

                stay = new LatLngTime(temp.getLat(), temp.getLng(),temp.getStartTime(), temp.getEndTime());
            }
        }
        if(stay!=null && stay.getEndTime() - stay.getStartTime() >= 600000){
            outputList.add(stay);
        }
        //recursion call
        if(outputList.size() == inputList.size())
            return outputList;
        else
            return getUserstay(outputList);

    }

    private ArrayList<LatLngTime> userstaySlicing(LatLngTime llt) {
        ArrayList<LatLngTime> slices = new ArrayList<>();
        long endTime = llt.getEndTime();
        long curTime = llt.getStartTime();
        while(curTime < endTime) {
            LatLngTime slice = new LatLngTime();
            slice.setLat(llt.getLat());
            slice.setLng(llt.getLng());
            slice.setStartTime(curTime);
            //get the nextend time(the end of current day) as per pacific time zone, GMT is 7 hours ahead Pacific time
            long nextend = ((curTime-7*3600*1000) /(24*3600*1000)+1)*24*3600*1000 + 7*3600*1000 - 60*1000;
            long sliceEnd = nextend > endTime ? endTime : nextend;
            slice.setEndTime(sliceEnd);
            slices.add(slice);
            curTime = sliceEnd + 60*1000;
        }
        return slices;
    }

    private ArrayList<KmlEvents> userstaySlicing(KmlEvents kmlevent) {
        ArrayList<KmlEvents> slices = new ArrayList<>();
        long endTime = kmlevent.getEndTime();
        long curTime = kmlevent.getStartTime();
        while(curTime < endTime) {
            KmlEvents slice = new KmlEvents();
            slice.setLat(kmlevent.getLat());
            slice.setLng(kmlevent.getLng());
            slice.setStartTime(curTime);
            //get the nextend time(the end of current day) as per pacific time zone, GMT is 7 hours ahead Pacific time
            long nextend = ((curTime-7*3600*1000) /(24*3600*1000)+1)*24*3600*1000 + 7*3600*1000 - 60*1000;
            long sliceEnd = nextend > endTime ? endTime : nextend;
            slice.setEndTime(sliceEnd);
            slice.setName(kmlevent.getName());
            slice.setAddress(kmlevent.getAddress());
            slice.setNotes(kmlevent.getNotes());
            slices.add(slice);
            curTime = sliceEnd + 60*1000;
        }
        return slices;
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
