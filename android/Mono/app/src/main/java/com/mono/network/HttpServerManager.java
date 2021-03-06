package com.mono.network;

import android.content.Context;
import android.os.AsyncTask;

import com.mono.AccountManager;
import com.mono.model.Account;
import com.mono.model.Contact;
import com.mono.model.Message;
import com.mono.util.Common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class HttpServerManager {

    public static final String SERVER_URL = "https://www.supercaly.com:8443/";
    public static final String REST_URL = SERVER_URL + "SuperCaly/rest/";

    public static final String CREATE_USER_URL = REST_URL + "user/createUser";
    public static final String GET_USER_URL = REST_URL + "user/basicInfo/";
    public static final String GET_USER_BY_EMAIL_URL = REST_URL + "user/getUserByEmail/";
    public static final String GET_USER_BY_PHONE_URL = REST_URL + "user/getUserByPhoneNumber/";
    public static final String VERIFY_USER_BY_EMAIL_URL = REST_URL + "user/verifyUserByEmail";
    public static final String VERIFY_USER_BY_PHONE_URL = REST_URL + "user/verifyUserByPhoneNumber";
    public static final String EDIT_USER_URL = REST_URL + "user/editUser";
    public static final String GET_USER_EVENTS_URL = REST_URL + "user/userEvents/";
    public static final String GET_USER_CONVERSATIONS_URL = REST_URL + "user/userConversations/";
    public static final String CREATE_EVENT_URL = REST_URL + "event/createEvent";
    public static final String GET_EVENT_URL = REST_URL + "event/getEvent/";
    public static final String CREATE_EVENT_CONVERSATION_URL = REST_URL + "event/addConversation/";
    public static final String UPDATE_FCM_ID_URL = REST_URL + "user/updateUserFcmId/";
    public static final String GET_ALL_USER_ID_URL = REST_URL + "user/getAllUserId";
    public static final String CREATE_CONVERSATION_URL = REST_URL + "conversation/createConversation";
    public static final String GET_CONVERSATION_URL = REST_URL + "conversation/getConversation/";
    public static final String ADD_CONVERSATION_ATTENDEES_URL = REST_URL + "conversation/addAttendees";
    public static final String UPDATE_CONVERSATION_TITLE_URL = REST_URL + "conversation/updateTitle";
    public static final String DROP_CONVERSATION_ATTENDEES_URL = REST_URL + "conversation/dropAttendees";
    public static final String SUGGEST_CONTACTS_URL = REST_URL + "user/suggestContacts";

    public static final String GET_CONVID_BYUSER = REST_URL + "conversation/getconversationIds/";
    public static final String GET_EVENT_BYCONVID = REST_URL + "event/getEventbyConvId/";
    public static final String GET_MESSAGE_BYCONVID = REST_URL + "conversation/conversationMessages/";
    public static final String GET_MESSAGE_BYUSERID = REST_URL + "conversation/conversationMessagesByUser/";

    public static final String ADD_MESSAGE_URL = REST_URL + "conversation/addMessage";
    public static final String BACKUP_MESSAGE_URL = REST_URL + "conversation/backupMessages";
    public static final String DELETE_CONVERSATION_MESSAGES_URL = REST_URL + "conversation/deleteMessages/";

    public static final String UPLOAD_IMAGE_URL = REST_URL + "file/upload/image";
    public static final String UPLOAD_VIDEO_URL = REST_URL + "file/upload/video";
    public static final String DOWNLOAD_URL = REST_URL + "file/download/";

    //http server return codes
    public static final int STATUS_OK = 0;
    public static final int STATUS_NO_USER = 1;
    public static final int STATUS_WRONG_PASSWORD = 2;
    public static final int STATUS_ERROR = 3;
    public static final int STATUS_NO_CONVERSATION = 4;
    public static final int STATUS_NO_EVENT = 5;
    public static final int STATUS_EXCEPTION = -1;

    //server methods
    public static final String POST = "POST";
    public static final String GET = "GET";

    //JSON key names
    public static final String EMAIL = "email";
    public static final String FIRST_NAME = "firstName";
    public static final String FCM_ID = "fcmId";
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
    public static final String ATTENDEES = "attendees";
    public static final String STATUS = "status";
    public static final String CONVERSATION_ID = "cId";
    public static final String SENDER_ID = "senderId";
    public static final String MESSAGE_KEY = "messageKey";
    public static final String TIMESTAMP = "timestamp";
    public static final String TEXT_CONTENT = "textContent";
    public static final String MESSAGES = "messages";


    private Context context;
    private static HttpServerManager instance;

    private HttpServerManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static HttpServerManager getInstance (Context context) {
        if (instance == null) {
            instance = new HttpServerManager(context);
        }
        return instance;
    }

    public int registerMe(String email, String firstName, String fcmId, String lastName, String mediaId, String phoneNum, String userName, String password) {
        try {
            JSONObject userInfo = getJSONObject(
                    new String[]{EMAIL, FIRST_NAME, FCM_ID, LAST_NAME, MEDIA_ID, PHONE_NUMBER, USER_NAME, PASSWORD},
                    new String[]{email, firstName, fcmId, lastName, mediaId, phoneNum, userName, password}
            );
            JSONObject responseJson = new JSONObject(queryServer(userInfo.toString(), CREATE_USER_URL, POST));
            if (responseJson.has(UID)) {
                int uId = responseJson.getInt(UID);
                Account account = new Account(uId);
                account.firstName = firstName;
                account.lastName = lastName;
                account.username = userName;
                account.email = email;
                account.phone = phoneNum;
                AccountManager.getInstance(context).login(account);
                return uId;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    public int createUser (String email, String firstName, String fcmId, String lastName, String mediaId, String phoneNum, String userName, String password) {
        try {
            JSONObject userInfo = getJSONObject(
                    new String[]{EMAIL, FIRST_NAME, FCM_ID, LAST_NAME, MEDIA_ID, PHONE_NUMBER, USER_NAME, PASSWORD},
                    new String[]{email, firstName, fcmId, lastName, mediaId, phoneNum, userName, password}
            );
            JSONObject responseJson = new JSONObject(queryServer(userInfo.toString(), CREATE_USER_URL, POST));
            if (responseJson.has(UID)) {
                return responseJson.getInt(UID);
            }
            return responseJson.getInt(STATUS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    public int loginUser(String emailOrPhone, String password) {
        JSONObject jsonObject;
        String url;
        try {
            if (emailOrPhone.contains("@")) {
                url = VERIFY_USER_BY_EMAIL_URL;
                jsonObject = getJSONObject(
                        new String[]{EMAIL, PASSWORD},
                        new String[]{emailOrPhone, password}
                );
            } else {
                url = VERIFY_USER_BY_PHONE_URL;
                jsonObject = getJSONObject(
                        new String[]{PHONE_NUMBER, PASSWORD},
                        new String[]{emailOrPhone, password}
                );
            }
            JSONObject responseJson = new JSONObject(queryServer(jsonObject.toString(), url, POST));
            if (responseJson != null && responseJson.has(STATUS)) {
                return responseJson.getInt(STATUS);
            } else if (responseJson.has(UID)) {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    public int updateUserFcmId(int userId, String fcmId) {
        try {
            JSONObject responseJson = new JSONObject(queryServer(null, UPDATE_FCM_ID_URL + userId + "/" + fcmId, POST));
            if (responseJson != null && responseJson.has(STATUS)) {
                return responseJson.getInt(STATUS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    /*public int addAllRegisteredUsersToUserTable(AttendeeDataSource attendeeDataSource) {
        JSONObject allUserIds = getAllRegisteredUserIds();
        if (allUserIds == null)
            return 0;
        int count = 0;
        try {
            JSONArray userIdArray = allUserIds.getJSONArray("allUserId");
            int length = userIdArray.length();
            for (int i=0; i<length; i++) {
                JSONObject userInfo = getUserInfo(Integer.valueOf(userIdArray.get(i).toString()));
                attendeeDataSource.createAttendeeWithAttendeeId(userInfo.getString(UID), userInfo.getString(MEDIA_ID),
                        userInfo.getString(EMAIL), userInfo.getString(PHONE_NUMBER), userInfo.getString(FIRST_NAME),
                        userInfo.getString(LAST_NAME), userInfo.getString(USER_NAME), false, true);
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
    }*/

    public JSONObject getUserInfo(int userId) {
        try {
            return new JSONObject(queryServer(null, GET_USER_URL + userId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int editUser(int uId, String userName, String email, String firstName, String fcmId, String lastName, String mediaId, String phoneNumber, String password) {
        try {
            JSONObject userInfo = getJSONObject(
                    new String[]{UID, USER_NAME, EMAIL, FIRST_NAME, FCM_ID, LAST_NAME, MEDIA_ID, PHONE_NUMBER, PASSWORD},
                    new Object[]{uId, userName, email, firstName, fcmId, lastName, mediaId, phoneNumber, password}
            );
            JSONObject responseJson = new JSONObject(queryServer(userInfo.toString(), EDIT_USER_URL, POST));
            if (responseJson != null)
                return responseJson.getInt(STATUS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    public JSONObject getUserEvents(int userId) {
        try {
            return new JSONObject(queryServer(null, GET_USER_EVENTS_URL + userId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getUserConversations(int userId) {
        try {
            return new JSONObject(queryServer(null, GET_USER_CONVERSATIONS_URL + userId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getUserByEmail(String email) {
        try {
            return new JSONObject(queryServer(null, GET_USER_BY_EMAIL_URL + email, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getUserByPhone(String phoneNum) {
        try {
            return new JSONObject(queryServer(null, GET_USER_BY_PHONE_URL + phoneNum, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param eventId
     * @param eventType
     * @param title
     * @param location
     * @param startTime
     * @param endTime
     * @param creatorId
     * @param createTime
     * @param attendeesId
     * @return Server-generated event id
     */
    public String createEvent(String eventId, String eventType, String title, String location,
                                  long startTime, long endTime, Integer creatorId, long createTime, List<String> attendeesId) {
        try {
            JSONObject jsonObject = getJSONObject(
                    new String[]{EVENT_ID, EVENT_TYPE, TITLE, LOCATION, START_TIME, END_TIME, CREATOR_ID, CREATE_TIME, ATTENDEES_ID},
                    new Object[]{eventId, eventType, title, location, startTime, endTime, creatorId, createTime, new JSONArray(Common.convertIdList(attendeesId))}
            );
            JSONObject responseJson = new JSONObject(queryServer(jsonObject.toString(), CREATE_EVENT_URL, POST));
            if (responseJson != null && responseJson.has(EVENT_ID)) {
                return  responseJson.getString(EVENT_ID);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public JSONObject getEvent (String eventId) {
        try {
            return new JSONObject(queryServer(null, GET_EVENT_URL + eventId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createConversation(String conversationId, String title, Integer creatorId, List<String> attendeesId) {
        try {
            JSONObject conversationInfo = getJSONObject(
                    new String[] {CONVERSATION_ID, TITLE, CREATOR_ID, ATTENDEES_ID},
                    new Object[] {conversationId, title, creatorId, new JSONArray(Common.convertIdList(attendeesId))}
            );
            JSONObject responseJson = new JSONObject(queryServer(conversationInfo.toString(), CREATE_CONVERSATION_URL, POST));
            if (responseJson != null && responseJson.has(STATUS) && responseJson.getInt(STATUS) == 0) {
               return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Possible status: OK = 0, NO_EVENT = 5;
     * @param eventId
     * @param conversationId
     * @param title
     * @param creatorId
     * @param attendeesId
     * @return True if successful, false if not
     */
    public boolean createEventConversation (String eventId, String conversationId, String title, Integer creatorId, List<String> attendeesId) {
        try {
            JSONObject conversationInfo = getJSONObject(
                    new String[] {CONVERSATION_ID, TITLE, CREATOR_ID, ATTENDEES_ID},
                    new Object[] {conversationId, title, creatorId, new JSONArray(Common.convertIdList(attendeesId))}
            );
            String result = queryServer(conversationInfo.toString(), CREATE_EVENT_CONVERSATION_URL + eventId, POST);
            JSONObject responseJson = new JSONObject(result);
            if (responseJson != null && responseJson.has(STATUS) && responseJson.getInt(STATUS) == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public JSONObject getConversation(String conversationId) {
        try {
            return new JSONObject(queryServer(null, GET_CONVERSATION_URL + conversationId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }




    public JSONObject getConversationMessages(String conversationId) {
        try {
            return new JSONObject(queryServer(null, GET_MESSAGE_BYCONVID + conversationId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getConversationMessages(int userId) {
        try {
            return new JSONObject(queryServer(null, GET_MESSAGE_BYUSERID + userId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getConversationByuser(int userId) {
        try {
            return new JSONObject(queryServer(null, GET_CONVID_BYUSER + userId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getEventByconversation(String conversationId) {
        try {
            return new JSONObject(queryServer(null, GET_EVENT_BYCONVID + conversationId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject deleteMessagesByConversation(String conversationId) {
        try {
            return new JSONObject(queryServer(null, DELETE_CONVERSATION_MESSAGES_URL + conversationId, GET));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param conversationId
     * @param newAttendeesId
     * @return NO_CONVERSATION = 4, OK=3, NO_USER=1, Error=STATUS_EXCEPTION
     */
    public int addConversationAttendees(String conversationId, List<String> newAttendeesId) {
        try {
            JSONObject jsonObject = getJSONObject(
                    new String[]{CONVERSATION_ID, ATTENDEES_ID},
                    new Object[]{conversationId, new JSONArray(Common.convertIdList(newAttendeesId))}
            );
            JSONObject responseJson = new JSONObject(queryServer(jsonObject.toString(), ADD_CONVERSATION_ATTENDEES_URL, POST));
            if (responseJson != null && responseJson.has(STATUS)) {
                return  responseJson.getInt(STATUS);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    /**
     *
     * @param conversationId
     * @param attendeesId
     * @return NO_CONVERSATION = 4, OK=3, NO_USER=1, EXCEPTION=STATUS_EXCEPTION
     */
    public int dropConversationAttendees(String conversationId, List<String> attendeesId) {
        try {
            JSONObject jsonObject = getJSONObject(
                    new String[]{CONVERSATION_ID, ATTENDEES_ID},
                    new Object[]{conversationId, new JSONArray(Common.convertIdList(attendeesId))}
            );
            JSONObject responseJson = new JSONObject(queryServer(jsonObject.toString(), DROP_CONVERSATION_ATTENDEES_URL, POST));
            if (responseJson != null && responseJson.has(STATUS)) {
                return  responseJson.getInt(STATUS);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    /**
     *
     * @param conversationId
     * @param newTitle
     * @return NO_CONVERSATION = 4, OK=3, EXCEPTION=STATUS_EXCEPTION
     */
    public int updateConversationTitle(String conversationId, String newTitle) {
        try {
            JSONObject jsonObject = getJSONObject(
                    new String[]{CONVERSATION_ID, TITLE},
                    new String[]{conversationId, newTitle}
            );
            JSONObject responseJson = new JSONObject(queryServer(jsonObject.toString(), UPDATE_CONVERSATION_TITLE_URL, POST));
            if (responseJson != null && responseJson.has(STATUS)) {
                return  responseJson.getInt(STATUS);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    public int addMessage(Message msg) {

        try {
            JSONObject messagekey = getJSONObject(new String[]{CONVERSATION_ID,"senderId", "timestamp"}, new Object[]{msg.getConversationId(), Integer.parseInt(msg.getSenderId()), msg.getTimestamp()});
            JSONObject userInfo = getUserInfo(Integer.parseInt(msg.getSenderId()));
            JSONObject jsonObject = getJSONObject(
                    new String[]{"messageKey", MEDIA_ID, "textContent"},
                    new Object[]{messagekey, userInfo.getString(MEDIA_ID), msg.getMessageText()}
            );
            JSONObject responseJson = new JSONObject(queryServer(jsonObject.toString(), ADD_MESSAGE_URL, POST));
            if (responseJson != null && responseJson.has(STATUS)) {
                return  responseJson.getInt(STATUS);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return STATUS_EXCEPTION;
    }

    public List<Contact> getSuggestedContacts(List<Contact> contactList) {
        JSONArray emailArray = new JSONArray();
        JSONArray phoneArray = new JSONArray();
        List<Contact> suggestedContacts = new ArrayList<>();
        try {
            for (Contact contact : contactList) {
                String[] emails = contact.getEmails();
                if (emails != null) {
                    for (String email : emails) {
                        emailArray.put(email);
                    }
                }
                String[] phones = contact.getPhones();
                if (phones != null) {
                    for (String phone : phones) {
                        phoneArray.put(phone.replaceAll("\\D", ""));
                    }
                }
            }
            if (emailArray.length() == 0 && phoneArray.length() == 0) {
                return suggestedContacts;
            }

            JSONObject object = new JSONObject();
            object.put(EMAIL, emailArray);
            object.put(PHONE_NUMBER, phoneArray);
            JSONArray response = new JSONArray(queryServer(object.toString(), SUGGEST_CONTACTS_URL, POST));

            for (int i = 0; i < response.length(); i++) {
                JSONObject obj = response.getJSONObject(i);
                Contact contact = new Contact(obj.getInt(UID), Contact.TYPE_USER);
                contact.visible = true;
                contact.mediaId = obj.getString(MEDIA_ID);
                contact.setEmail(obj.getString(EMAIL));
                contact.setPhone(obj.getString(PHONE_NUMBER));
                contact.firstName = obj.getString(FIRST_NAME);
                contact.lastName = obj.getString(LAST_NAME);
                contact.userName = obj.getString(USER_NAME);
                suggestedContacts.add(contact);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return suggestedContacts;
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

    private Object[] queryServer(HttpsURLConnection connection, String data) {
        Object[] result = new Object[2];

        try {
            if (connection.getRequestMethod().equalsIgnoreCase("POST") && data != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(data);
                writer.close();
            }

            int responseCode = connection.getResponseCode();
            result[0] = responseCode;

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                reader.close();

                result[1] = builder.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private String queryServer(String data, String urlString, String method) throws
            ExecutionException, InterruptedException {
        AsyncTask<Object, Void, String> task = new AsyncTask<Object, Void, String>() {

            private HttpsURLConnection connection;
            private int responseCode;

            @Override
            protected String doInBackground(Object... params) {
                String result = null;

                String data = (String) params[0];
                String urlString = (String) params[1];
                String method = (String) params[2];

                try {
                    URL url = new URL(urlString);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod(method);

                    Object[] values = queryServer(connection, data);
                    responseCode = (Integer) values[0];

                    if (values[1] != null) {
                        result = (String) values[1];
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                try {
                    System.out.format(
                        "Server response code & message: %d %s\n",
                        responseCode,
                        connection.getResponseMessage()
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.execute(data, urlString, method);

        return task.get();
    }

    public JSONObject send(JSONObject data, String urlString, String method) {
        JSONObject result = null;

        try {
            URL url = new URL(urlString);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod(method);

            Object[] values = queryServer(connection, data.toString());
            if (values[1] != null) {
                result = (JSONObject) values[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
