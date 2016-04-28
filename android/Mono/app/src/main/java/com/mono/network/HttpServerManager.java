package com.mono.network;

import android.content.Context;
import android.os.AsyncTask;

import com.mono.AccountManager;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.model.Account;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class HttpServerManager {
    public static final String SERVER_URL = "http://52.25.71.19:8080";
    public static final String CREATE_USER_URL = SERVER_URL + "/SuperCaly/rest/user/createUser";
    public static final String GET_USER_URL = SERVER_URL + "/SuperCaly/rest/user/basicInfo/";
    public static final String GET_USER_BY_EMAIL_URL = SERVER_URL + "/SuperCaly/rest/user/getUserByEmail/";
    public static final String GET_USER_BY_PHONE_URL = SERVER_URL + "/SuperCaly/rest/user/getUserByPhoneNumber/";
    public static final String VERIFY_USER_BY_EMAIL_URL = SERVER_URL + "/SuperCaly/rest/user/verifyUserByEmail/";
    public static final String VERIFY_USER_BY_PHONE_URL = SERVER_URL + "/SuperCaly/rest/user/verifyUserByPhoneNumber/";
    public static final String EDIT_USER_URL = SERVER_URL + "/SuperCaly/rest/user/basicInfo/";
    public static final String GET_USER_EVENTS_URL = SERVER_URL + "/SuperCaly/rest/user/userEvents/";
    public static final String GET_USER_CONVERSATIONS_URL = SERVER_URL + "/SuperCaly/rest/user/userConversations/";
    public static final String CREATE_EVENT_URL = SERVER_URL + "/SuperCaly/rest/event/createEvent";
    public static final String UPDATE_GCM_ID_URL = SERVER_URL + "/SuperCaly/rest/user/updateUserGcmId/";
    public static final String GET_ALL_USER_ID_URL = SERVER_URL + "/SuperCaly/rest/user/getAllUserId";
    public static final String CREATE_CONVERSATION_URL = SERVER_URL + "/SuperCaly/rest/conversation/createConversation";

    //http server return codes
    public static final int STATUS_OK = 0;
    public static final int STATUS_NO_USER = 1;
    public static final int STATUS_WRONG_PASSWORD = 2;
    public static final int STATUS_ERROR = 3;
    public static final int STATUS_NO_CONVERSATION = 4;
    public static final int STATUS_NO_EVENT = 5;
    //server methods
    public static final String POST = "POST";
    public static final String GET = "GET";

    //JSON key names
    public static final String EMAIL = "email";
    public static final String FIRST_NAME = "firstName";
    public static final String GCM_ID = "gcmId";
    public static final String LAST_NAME = "lastName";
    public static final String MEDIA_ID = "mediaId";
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String USER_NAME = "userName";
    public static final String PASSWORD = "password";
    public static final String UID = "uId";
    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TYPE = "eventType";
    public static final String TITLE = "title";
    public static final String LOCATION = "location";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String CREATOR_ID = "creatorId";
    public static final String CREATE_TIME = "createTime";
    public static final String ATTENDEES_ID = "attendeesId";
    public static final String STATUS = "status";
    public static final String CONVERSATION_ID = "cId";

    private Context context;

    public HttpServerManager(Context context) {
        this.context = context;
    }

    public int createUser(String email, String firstName, String gcmId, String lastName, String mediaId, String phoneNum, String userName, String password) {
        try {
            JSONObject userInfo = getJSONObject(
                    new String[]{EMAIL, FIRST_NAME, GCM_ID, LAST_NAME, MEDIA_ID, PHONE_NUMBER, USER_NAME, PASSWORD},
                    new String[]{email, firstName, gcmId, lastName, mediaId, phoneNum, userName, password}
            );
            JSONObject responseJson = queryServer(userInfo, CREATE_USER_URL, POST);
            if (responseJson.has(UID)) {
                int uId = responseJson.getInt(UID);
                Account account = new Account(uId);
                account.firstName = firstName;
                account.lastName = lastName;
                account.username = userName;
                account.email = email;
                AccountManager.getInstance(context).setAccount(account);
                return uId;
            } else if (responseJson.getInt(STATUS) == STATUS_ERROR) {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int loginUser(String emailOrPhone, String password) {
        String url;
        if (emailOrPhone.contains("@")) {
//            url = VERIFY_USER_BY_EMAIL_URL + "{" + emailOrPhone + "}/{" + password + "}";
            url = VERIFY_USER_BY_EMAIL_URL + emailOrPhone + "/" + password;
        } else {
            url = VERIFY_USER_BY_PHONE_URL + emailOrPhone + "/" + password;
        }

        try {
            JSONObject responseJson = queryServer(null, url, POST);
            if (responseJson != null && responseJson.has(STATUS)) {
                return responseJson.getInt(STATUS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int updateUserGcmId(long userId, String gcmId) {
        try {
            JSONObject responseJson = queryServer(null, UPDATE_GCM_ID_URL + userId + "/" + gcmId, POST);
            if (responseJson != null && responseJson.has(STATUS)) {
                return responseJson.getInt(STATUS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int addAllRegisteredUsersToUserTable(AttendeeDataSource attendeeDataSource) {
        JSONObject allUserIds = getAllRegisteredUserIds();
        if (allUserIds == null)
            return 0;
        int count = 0;
        try {
            JSONArray userIdArray = allUserIds.getJSONArray("allUserId");
            int length = userIdArray.length();
            for (int i=0; i<length; i++) {
                String userId = userIdArray.get(i).toString();
                JSONObject userInfo = getUserInfo(userId);
                attendeeDataSource.createAttendeeWithAttendeeId(userInfo.getString(UID), userInfo.getString(MEDIA_ID),
                        userInfo.getString(EMAIL), userInfo.getString(PHONE_NUMBER), userInfo.getString(FIRST_NAME),
                        userInfo.getString(LAST_NAME), userInfo.getString(USER_NAME), true);
                count ++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public JSONObject getAllRegisteredUserIds() {
        try {
            return queryServer(null, GET_ALL_USER_ID_URL, GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getUserInfo(int userId) {
        try {
            return queryServer(null, GET_USER_URL + userId, GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getUserInfo(String userId) {
        try {
            return queryServer(null, GET_USER_URL + userId, GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int editUser(int uId, String userName, String email, String firstName, String gcmId, String lastName, String mediaId, String phoneNumber, String password) {
        try {
            JSONObject userInfo = getJSONObject(
                    new String[]{UID, USER_NAME, EMAIL, FIRST_NAME, GCM_ID, LAST_NAME, MEDIA_ID, PHONE_NUMBER, PASSWORD},
                    new Object[]{uId, userName, email, firstName, gcmId, lastName, mediaId, phoneNumber, password}
            );
            JSONObject responseJson = queryServer(userInfo, EDIT_USER_URL, POST);
            if (responseJson != null)
                return responseJson.getInt(STATUS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public JSONObject getUserEvents(int userId) {
        try {
            return queryServer(null, GET_USER_EVENTS_URL + userId, GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getUserConversations(int userId) {
        try {
            return queryServer(null, GET_USER_CONVERSATIONS_URL + userId, GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getUserByEmail(String email) {
        try {
            return queryServer(null, GET_USER_BY_EMAIL_URL + email, GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getUserByPhone(String phoneNum) {
        try {
            return queryServer(null, GET_USER_BY_PHONE_URL + phoneNum, GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public JSONObject createEvent(JSONObject eventInfo) {
        try {
            return queryServer(eventInfo, CREATE_EVENT_URL, "POST");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int createConversation(String conversationId, String title, long creatorId, long[] attendeesId) {
        try {
            JSONObject conversationInfo = getJSONObject(
                    new String[] {CONVERSATION_ID, TITLE, CREATOR_ID, ATTENDEES_ID},
                    new Object[] {conversationId, title, creatorId, attendeesId}
            );
            JSONObject responseJson = queryServer(conversationInfo, CREATE_CONVERSATION_URL, POST);
            if (responseJson.has(STATUS)) {
                return responseJson.getInt(STATUS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }


    public JSONObject getJSONObject(String[] keys, Object[] values) throws JSONException {
        if (keys.length != values.length) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < keys.length; i++) {
            if (values[i] == null) {
                jsonObject.put(keys[i], "");
            } else {
                jsonObject.put(keys[i], values[i]);
            }
        }
        return jsonObject;
    }

    private JSONObject queryServer(final JSONObject data, final String urlString, final String method) throws JSONException, MalformedURLException, IOException, ExecutionException, InterruptedException {

        AsyncTask<Void, Void, JSONObject> serverTask = new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod(method);
                    if ("POST".equals(method) && data != null) {
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);
                        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                        String string = data.toString();
                        writer.write(string);
                        writer.close();
                    }

                    StringBuilder builder = new StringBuilder();
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line).append("\n");
                        }
                        reader.close();
                        return new JSONObject(builder.toString());
                    } else {
                        System.out.println(connection.getResponseMessage());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        }.execute();

        return serverTask.get();
    }

}
