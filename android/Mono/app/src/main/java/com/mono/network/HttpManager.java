package com.mono.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpManager {
    public static final String SERVER_URL = "";
    public static final String CREATE_USER_URL = SERVER_URL + "/SuperCaly/rest/user/createUser";
    public static final String GET_USER_URL = SERVER_URL + "/SuperCaly/rest/user/basicInfo/";
    public static final String UPDATE_USER_URL = SERVER_URL + "/SuperCaly/rest/user/basicInfo/";

    public static final String[] CREATE_USER_FIELDS = new String[] {
            "email",
            "firstName",
            "gcmId",
            "lastName",
            "mediaId",
            "phoneNumber",
            "userName"
    };

    public static final String[] UPDATE_USER_FIELDS = new String[] {
            "uId",
            "userName",
            "email",
            "firstName",
            "gcmId",
            "lastName",
            "mediaId",
            "phoneNumber"
    };

    private HttpManager() {}

    private static JSONObject queryServer(String[] fields, String[] values, String urlString, String method) throws JSONException, MalformedURLException, IOException {
        JSONObject jsonObject = null;
        if (fields != null && values != null) {
            int length = fields.length;
            if (length != values.length)
                return null;

            jsonObject = new JSONObject();
            for (int i = 0; i < length; i++) {
                jsonObject.put(fields[i], values[i]);
            }
        }

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        if ("POST".equals(method)) {
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            if (jsonObject != null) {
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(jsonObject.toString());
                writer.close();
            }
        }

        StringBuilder builder = new StringBuilder();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            return new JSONObject(builder.toString());
        } else {
            System.out.println(connection.getResponseMessage());
            return null;
        }
    }

    public static String sendRegister(String[] userInfo) {
        try {
            JSONObject responseJson = queryServer(CREATE_USER_FIELDS, userInfo, CREATE_USER_URL, "POST");
            if (responseJson != null && responseJson.has("uId")) {
                return responseJson.getString("uId");
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject sendLogin(String userId) {
        try {
            return queryServer(null, null, GET_USER_URL + "{" + userId + "}", "GET");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean UpdateUser(String[] userInfo) {
        try {
            JSONObject responseJson = queryServer(UPDATE_USER_FIELDS, userInfo, UPDATE_USER_URL, "POST");
            if (responseJson != null && responseJson.has("uId")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
