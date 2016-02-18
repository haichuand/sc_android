package com.mono.parser;

/**
 * Created by xuejing on 2/17/16.
 */
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class KmlParser {
    DocumentBuilderFactory factory;
    DocumentBuilder builder;

    public KmlParser () {
        factory = DocumentBuilderFactory.newInstance();
    }

    public ArrayList parse(String fileName) {
        ArrayList<LatLngTime> result = new ArrayList<>();
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(fileName));
            List<String> name= new ArrayList<>();
            NodeList nodeList = document.getDocumentElement().getChildNodes();
            //get gx:Track layer
            Node node = nodeList.item(1);

            if(node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                NodeList whenList = elem.getElementsByTagName("when");
                NodeList locationList = elem.getElementsByTagName("gx:coord");

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
                        //System.out.println(time + ": " +millis);
                    }

                    Node locNode = locationList.item(i);
                    if(locNode.getNodeType() == Node.ELEMENT_NODE) {
                        String coordinate = locNode.getChildNodes().item(0).getNodeValue();
                        String[] split = coordinate.split(" ");
                        double lng = Double.valueOf(split[0]);
                        double lat = Double.valueOf(split[1]);
                        llt.setLat(lat);
                        llt.setLng(lng);
                        //System.out.println(split[0] + " " + lng + " "+ split[1] +" "+ lat);
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
        return getUserstay(result);
    }

    private ArrayList<LatLngTime> getUserstay(ArrayList<LatLngTime> inputList) {
        ArrayList<LatLngTime> outputList = new ArrayList<>();
        LatLngTime stay = null;
        if(!inputList.isEmpty()) {
            LatLngTime temp = inputList.get(0);
            stay = new LatLngTime(temp.getLat(), temp.getLng(), temp.getStartTime(), temp.getEndTime());
        }

        for(int i = 1; i < inputList.size(); i++) {
            LatLngTime temp = inputList.get(i);
            double tempLat = temp.getLat();
            double tempLng = temp.getLng();
            if(Math.abs(tempLat-stay.getLat()) < 0.001 && Math.abs(tempLng-stay.getLng()) < 0.001) {
                //System.out.println("start: "+stay.toString() + " temp: "+temp.toString());
                stay.setEndTime(temp.getEndTime());
            }
            else {
                //600000 milliseconds == 10 mins
                if(stay.getEndTime() - stay.getStartTime() >= 600000){
                    outputList.add(stay);
                }
                stay = new LatLngTime(temp.getLat(), temp.getLng(), temp.getStartTime(), temp.getEndTime());
            }
        }
        if(stay.getEndTime() - stay.getStartTime() >= 600000){
            outputList.add(stay);
        }
        //System.out.println("output: "+outputList.size()+" input: "+inputList.size());
        //recursion call
        if(outputList.size() == inputList.size())
            return outputList;
        else
            return getUserstay(outputList);

    }
}
