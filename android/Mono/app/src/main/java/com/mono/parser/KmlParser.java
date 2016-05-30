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
    private static final String TAG = "KmlParser";
    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;

    public KmlParser () {
        factory = DocumentBuilderFactory.newInstance();
    }

    public ArrayList<LatLngTime> parse(String fileName1, String fileName2) {
        ArrayList<LatLngTime> result = parse(fileName1);
        result.addAll(parse(fileName2));
        return result;
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

            return getUserstay(result);

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
            if(Math.abs(tempLat-stay.getLat()) < 0.0012 && Math.abs(tempLng-stay.getLng()) < 0.0015) {
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
        if(stay!=null && stay.getEndTime() - stay.getStartTime() >= 600000){
            outputList.add(stay);
        }
        //System.out.println("output: "+outputList.size()+" input: "+inputList.size());
        //recursion call
        if(outputList.size() == inputList.size())
            return outputList;
        else
            return getUserstay(outputList);

    }
    // for test purpose
    private void outputFile(String path) {
        try {
            StringBuilder builder = new StringBuilder(path + "\n\n");
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String nextLine;

            while ((nextLine = reader.readLine()) != null) {
                builder.append(nextLine);
                builder.append('\n');
            }

            Log.d(TAG, builder.toString());

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
