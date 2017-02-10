package com.mono.network;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mono.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class is used to communicate with the HTTP server using GET and POST requests. Responses
 * returned can either be in JSON, text, or bitmap format.
 *
 * @author Gary Ng
 */
public class NetworkManager {

    private static NetworkManager instance;

    private Context context;

    private RequestQueue requestQueue;
    private AsyncTask<Object, Void, Object[]> requestTask;

    private NetworkManager(Context context) {
        this.context = context;
    }

    public static NetworkManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkManager(context.getApplicationContext());
            instance.initialize();
        }

        return instance;
    }

    /**
     * Perform additional initialization such as creating the request queue used for Volley.
     */
    private void initialize() {
        requestQueue = Volley.newRequestQueue(context);
    }

    /**
     * Sends GET request that expects a JSON response. Uses Volley.
     *
     * @param url Server URL to send the request.
     * @param listener Listener that handles the response returned.
     */
    public void getJSON(String url, ResponseListener listener) {
        listener.onRequest();

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            listener,
            listener
        );

        requestQueue.add(request);
    }

    /**
     * Sends POST request that expects a JSON response. Uses Volley.
     *
     * @param url Server URL to send the request.
     * @param data Data to be sent in the request body.
     * @param listener Listener that handles the response returned.
     */
    public void postJSON(String url, JSONObject data, ResponseListener listener) {
        listener.onRequest();

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            url,
            data,
            listener,
            listener
        );

        requestQueue.add(request);
    }

    /**
     * Sends GET request that expects an image response. Uses HTTP URL Connection.
     *
     * @param url Server URL to send the request.
     * @param listener Listener that handles the response returned.
     */
    public void get(String url, ResponseListener listener) {
        listener.onRequest();

        requestTask = new AsyncTask<Object, Void, Object[]>() {
            private ResponseListener listener;

            @Override
            protected Object[] doInBackground(Object... params) {
                String spec = (String) params[0];
                listener = (ResponseListener) params[1];

                Object[] result = {0, null}; // {Response Code, Path}

                try {
                    URL url = new URL(spec);

                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setRequestMethod("GET");
                    // Handle Response
                    switch (connection.getResponseCode()) {
                        case HttpsURLConnection.HTTP_OK:
                            InputStream inputStream = connection.getInputStream();

                            String storage = Environment.getExternalStorageDirectory().getPath() + "/";
                            String filename = spec.substring(spec.lastIndexOf("/") + 1);
                            String path = storage + MainActivity.APP_DIR + filename;

                            OutputStream outputStream = new FileOutputStream(path);

                            int length;
                            byte[] buffer = new byte[1024];
                            while ((length = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, length);
                            }

                            outputStream.close();
                            inputStream.close();

                            result[0] = connection.getResponseCode();
                            result[1] = Uri.parse(path);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            protected void onPostExecute(Object[] result) {
                int responseCode = (int) result[0];

                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                        Uri uri = (Uri) result[1];
                        listener.onResponse(uri);
                        break;
                    default:
                        listener.onErrorResponse(null);
                        break;
                }

            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url, listener);
    }

    /**
     * Sends POST request that expects either a JSON or text response. Uses HTTP URL Connection.
     *
     * @param url Server URL to send the request.
     * @param path Path of file.
     * @param listener Listener that handles the response returned.
     */
    public void post(String url, String path, ResponseListener listener) {
        listener.onRequest();

        requestTask = new AsyncTask<Object, Void, Object[]>() {
            private ResponseListener listener;

            @Override
            protected Object[] doInBackground(Object... params) {
                String spec = (String) params[0];
                String path = (String) params[1];
                listener = (ResponseListener) params[2];

                Object[] result = {0, null}; // {Response Code, JSON}

                try {
                    URL url = new URL(spec);

                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setUseCaches(false);
                    // Handle File
                    if (path != null) {
                        String filename = path.substring(path.lastIndexOf("/") + 1);

                        String boundary = UUID.randomUUID().toString();
                        boundary = boundary.substring(boundary.lastIndexOf("-") + 1);

                        connection.setRequestProperty("Connection", "Keep-Alive");
                        connection.setRequestProperty("Cache-Control", "no-cache");
                        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                        DataOutputStream request = new DataOutputStream(connection.getOutputStream());

                        request.writeBytes("--" + boundary + "\r\n");
                        request.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + filename + "\"" + "\r\n");
                        request.writeBytes("\r\n");

                        InputStream input = new FileInputStream(path);

                        int length;
                        byte[] buffer = new byte[1024];
                        while ((length = input.read(buffer)) != -1) {
                            request.write(buffer, 0, length);
                        }

                        input.close();

                        request.writeBytes("\r\n");
                        request.writeBytes("--" + boundary + "--" + "\r\n");

                        request.flush();
                        request.close();
                    }

                    // Handle Response
                    switch (connection.getResponseCode()) {
                        case HttpURLConnection.HTTP_OK:
                            StringBuilder builder = new StringBuilder();
                            BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream())
                            );

                            String line;
                            while ((line = reader.readLine()) != null) {
                                builder.append(line).append('\n');
                            }

                            reader.close();

                            result[0] = connection.getResponseCode();

                            try {
                                result[1] = new JSONObject(builder.toString());
                            } catch (JSONException e) {
                                result[1] = builder.toString();
                            }
                            break;
                    }

                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            protected void onPostExecute(Object[] result) {
                int responseCode = (int) result[0];

                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                        Object response = result[1];

                        if (response instanceof JSONObject) {
                            listener.onResponse((JSONObject) response);
                        } else {
                            listener.onResponse((String) response);
                        }
                        break;
                    default:
                        listener.onErrorResponse(null);
                        break;
                }
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url, path, listener);
    }

    public static abstract class ResponseListener implements Response.Listener<JSONObject>,
            Response.ErrorListener {

        public void onRequest() {}

        public void onResponse(String response) {}

        public void onResponse(Uri uri) {}
    }
}
