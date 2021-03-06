package com.mono;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.mono.chat.ConversationManager;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.Event;
import com.mono.model.Message;
import com.mono.network.ChatServerManager;
import com.mono.network.FCMHelper;
import com.mono.network.HttpServerManager;
import com.mono.parser.KmlDownloadingService;
import com.mono.parser.SupercalyAlarmManager;
import com.mono.settings.Settings;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * This activity is used to handle the login process that includes displaying the fragment to
 * accept user input and communicating with the server to log in.
 *
 * @author Gary Ng, Haichuan Duan
 */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT = "account";
    private HttpServerManager httpServerManager;
    private EventManager eventManager;
    private ConversationManager conversationManager;
    private int uid;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private ConversationDataSource conversationDataSource;
    SharedPreferences sharedpreferences;
    static String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        showLogin();
    }

    /**
     * Encapsulates the steps required to load any fragment into this activity.
     *
     * @param fragment The fragment to be shown.
     * @param tag The tag value to later retrieve the fragment.
     * @param addToBackStack The value used to enable back stack usage.
     */
    public void showFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentManager manager = getSupportFragmentManager();

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);

        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }

        transaction.commit();
    }

    /**
     * Display the fragment holding the login view.
     */
    public void showLogin() {
        String tag = getString(R.string.fragment_login);
        showFragment(new LoginFragment(), tag, false);
    }
    public void resetPassword(String username, String email)
    {
        JSONObject responseJson;
            if (httpServerManager == null) {
                httpServerManager = HttpServerManager.getInstance(this);
            }

            if (username.contains("@")) {
                responseJson = httpServerManager.getUserByEmail(username);
            } else {
                responseJson = httpServerManager.getUserByPhone(username);
            }
            if(!email.contains("@"))
            {
                Toast.makeText(getApplicationContext(), "Please enter a valid email id", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                if (responseJson != null && responseJson.has(HttpServerManager.STATUS)) {
                    if(responseJson.getInt(HttpServerManager.STATUS) == 1)
                    {
                        Toast.makeText(getApplicationContext(), "Please enter a valid registered email id or phone number", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                else
                {
                    Account account = new Account(responseJson.getInt(HttpServerManager.UID));
                    account.firstName = responseJson.getString(HttpServerManager.FIRST_NAME);
                    account.lastName = responseJson.getString(HttpServerManager.LAST_NAME);
                    account.username = responseJson.getString(HttpServerManager.USER_NAME);
                    account.mediaId = responseJson.getString(HttpServerManager.MEDIA_ID);
                    account.email = responseJson.getString(HttpServerManager.EMAIL);
                    account.phone = responseJson.getString(HttpServerManager.PHONE_NUMBER);
                    String fcmId = responseJson.getString(HttpServerManager.FCM_ID);
                    uid = responseJson.getInt(HttpServerManager.UID);
                    String newPass = generateRandomPass();
                    new sendEmail().execute(email, newPass);
                    httpServerManager.editUser(uid,account.username, account.email,account.firstName,fcmId,account.lastName,account.mediaId, account.phone, Common.md5(newPass));
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        showLogin();
    }

    public String generateRandomPass()
    {
        StringBuilder sb = new StringBuilder( 7 );
        for( int i = 0; i < 7; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    private class sendEmail extends  AsyncTask <Object , Void, Void>{
        @Override
        protected Void doInBackground(Object... params) {

            String email = (String) params[0].toString().trim();
            String newPass = (String) params[1];
            String emailbody = Constants.SUPERCALY_EMAIL_MESSAGE + newPass;


            //Creating properties
            Properties props = new Properties();
            //Configuring properties for gmail
            //If you are not using gmail you may need to change the values
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "465");

            //Creating a new session
            Session session = Session.getDefaultInstance(props,
                    new javax.mail.Authenticator() {
                        //Authenticating the password
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(Constants.SUPERCALY_EMAIL_TEST_ID, Constants.SUPERCALY_EMAIL_PASS);
                        }
                    });

            try {
                //Creating MimeMessage object
                MimeMessage mm = new MimeMessage(session);

                //Setting sender address
                mm.setFrom(new InternetAddress(Constants.SUPERCALY_EMAIL_TEST_ID));
                //Adding receiver
                mm.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(email));
                //Adding subject
                mm.setSubject(Constants.SUPERCALY_EMAIL_SUBJECT);
                //Adding message
                mm.setText(emailbody);
                //Sending email
                Transport.send(mm);
                Toast.makeText(getApplicationContext(), "New Password sent to your email address!", Toast.LENGTH_SHORT).show();
            } catch (MessagingException e) {
                Toast.makeText(getApplicationContext(), "Invalid address!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            catch (Exception ex)
            {
                Toast.makeText(getApplicationContext(), "Invalid address!", Toast.LENGTH_SHORT).show();
            }
            return null;


        }
    }

    public void showForgotPass()
    {
        String tag = getString(R.string.fragment_forgotpassword);
        showFragment(new ForgotPasswordFragment(), tag, false);
    }
    /**
     * Handle the action of submitting user credentials to log in.
     *
     * @param emailOrPhone The email or phone to be used to log in.
     * @param password The value of the password.
     * @param remember Remember email or phone.
     */
    public void submitLogin(String emailOrPhone, String password, boolean remember) {
        if (!Common.isConnectedToInternet(this)) {
            Toast.makeText(this, "No network connection. Cannot log in", Toast.LENGTH_SHORT).show();
            return;
        }

        httpServerManager = HttpServerManager.getInstance(this);
        eventManager = EventManager.getInstance(this);
        conversationManager = ConversationManager.getInstance(this);
        conversationDataSource = DatabaseHelper.getDataSource(getApplicationContext(), ConversationDataSource.class);
        String toastMessage = null;
        switch (httpServerManager.loginUser(emailOrPhone, password)) {
            case 0:
                Settings.getInstance(this).setRememberMe(remember);

                getUserInfoAndSetAccount(emailOrPhone);
//                resetUserTable(httpServerManager);
                startKMLService(this);
                Toast.makeText(this, "Login successful", Toast.LENGTH_LONG).show();
                sharedpreferences = getSharedPreferences(SuperCalyPreferences.FIRST_TIME_LOGIN, Context.MODE_PRIVATE);
                if(sharedpreferences.getString(SuperCalyPreferences.FIRST_TIME_LOGIN, "default").equalsIgnoreCase("default")) {
                    sharedpreferences.edit().putString(SuperCalyPreferences.FIRST_TIME_LOGIN, "yes").apply();
                    checkforChats();
                }
                finish();
                break;
            case 1:
            case 2:
                toastMessage = "Incorrect login information";
                break;
            case -1:
                toastMessage = "Server error. Please try again";
                break;
        }
        if (toastMessage != null) {
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void getUserInfoAndSetAccount(String emailOrPhone) {
        JSONObject responseJson;
        if (emailOrPhone.contains("@")) {
            responseJson = httpServerManager.getUserByEmail(emailOrPhone);
        } else {
            responseJson = httpServerManager.getUserByPhone(emailOrPhone);
        }

        try {
            Account account = new Account(responseJson.getInt(HttpServerManager.UID));
            account.firstName = responseJson.getString(HttpServerManager.FIRST_NAME);
            account.lastName = responseJson.getString(HttpServerManager.LAST_NAME);
            account.username = responseJson.getString(HttpServerManager.USER_NAME);
            account.mediaId = responseJson.getString(HttpServerManager.MEDIA_ID);
            account.email = responseJson.getString(HttpServerManager.EMAIL);
            account.phone = responseJson.getString(HttpServerManager.PHONE_NUMBER);
            AccountManager accountManager = AccountManager.getInstance(this);
            accountManager.login(account);
            uid = responseJson.getInt(HttpServerManager.UID);
            //refresh FCM tokens on http and chat servers
            String token = accountManager.getFcmToken();
            if (httpServerManager.updateUserFcmId((int) account.id, token) != 0) {
                Toast.makeText(this, "Error updating FCM token on http server", Toast.LENGTH_LONG).show();
            }
            ChatServerManager.getInstance(this).updateUserFcmId(account.id, token);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void checkforChats()
    {
        HashMap<String, String> cidEidMap = new HashMap<>();
            try {
                JSONObject messageObj = httpServerManager.getConversationMessages(uid);
                if(!messageObj.isNull(HttpServerManager.MESSAGES)) {
                    JSONArray messageArray = messageObj.getJSONArray(HttpServerManager.MESSAGES);
                    if (messageArray.length() > 0) {
                        for (int i = 0; i < messageArray.length(); i++) {
                            JSONObject obj = messageArray.getJSONObject(i);
                            String conversationId = obj.getJSONObject(HttpServerManager.MESSAGE_KEY).getString(HttpServerManager.CONVERSATION_ID);
                            JSONObject eventObj = httpServerManager.getEventByconversation(conversationId);
                            JSONArray eventArr = eventObj.getJSONArray(HttpServerManager.EVENT_ID);
                            if (eventArr.length() > 0) {
                                String eventId = eventArr.getString(0);
                                cidEidMap.put(conversationId, eventId);
                            } else {
                                cidEidMap.put(conversationId, "");
                            }
                        }
                        for (HashMap.Entry<String, String> entry : cidEidMap.entrySet()) {
                            String convId = entry.getKey();
                            String eventId = entry.getValue();
                            JSONObject eventObject = null;
                            if(!eventId.equals(""))
                            {
                                eventObject = httpServerManager.getEvent(eventId);
                            }
                            new addEventConversationTodb().execute(eventObject,convId, messageArray);
                        }

                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

    }

    private class addMessageTodb extends  AsyncTask <Object , Void, Void>{

        @Override
        protected Void doInBackground(Object... params) {
            try {
                JSONArray messageArr = (JSONArray) params[0];
                String convId = (String) params[1];
                for (int i = 0; i < messageArr.length(); i++) {

                    JSONObject obj = messageArr.getJSONObject(i);
                    String conversationId = obj.getJSONObject(HttpServerManager.MESSAGE_KEY).getString(HttpServerManager.CONVERSATION_ID);
                    if(conversationId.equals(convId)) {
                        String senderId = String.valueOf(obj.getJSONObject(HttpServerManager.MESSAGE_KEY).getInt(HttpServerManager.SENDER_ID));
                        String messageText = obj.getString(HttpServerManager.TEXT_CONTENT);
                        long timestamp = obj.getJSONObject(HttpServerManager.MESSAGE_KEY).getLong(HttpServerManager.TIMESTAMP);
                        com.mono.model.Message message = new com.mono.model.Message(senderId, conversationId, messageText, timestamp);
                        message.ack = true;
                        conversationDataSource.addMessageToConversation(message);
                    }

                }

            }
            catch(Exception ex){
                Log.d("", ex.getMessage());
                ex.printStackTrace();
            }

            return null;
        }
    }


    private class addEventConversationTodb extends AsyncTask<Object, Void, Void> {

        long startTime;
        long endTime;
        String eventTitle;
        List<String> eventAttendeesId = new ArrayList<>();
        String conversationId;
        int eventCreatorId;
        String eventId;
        JSONArray messagearr;
        @Override
        protected Void doInBackground(Object... params) {
            JSONObject eventObj = (JSONObject) params[0];
            conversationId = (String) params[1];
            messagearr = (JSONArray) params[2];
            eventId = "";
            if(eventObj != null) {
                //find events with matching start, end times & title in local database
                try {
                    eventId = eventObj.getString(HttpServerManager.EVENT_ID);
                    startTime = eventObj.getLong(FCMHelper.START_TIME);
                    endTime = eventObj.getLong(FCMHelper.END_TIME);
                    eventTitle = eventObj.getString(FCMHelper.TITLE);
                    conversationId = eventObj.getString(FCMHelper.CONVERSATION_ID);
                    eventCreatorId = eventObj.getInt(FCMHelper.CREATOR_ID);
                    JSONArray attendeesId = eventObj.getJSONArray(HttpServerManager.ATTENDEES_ID);
                    for (int i = 0; i < attendeesId.length(); i++) {
                        eventAttendeesId.add(attendeesId.get(i).toString());
                    }
                    JSONArray attendeesDetail = eventObj.getJSONArray(httpServerManager.ATTENDEES);

                    //create event on local database
                    List<Event> events = eventManager.getLocalEvents(startTime, endTime);
                    if (events.isEmpty()) {  //create new event
                        if (!createEvent(eventId, startTime, endTime, eventTitle, eventAttendeesId, attendeesDetail)) {
                            return null;
                        }
                    } else {  //find matching event, if any
                        Event localEvent = null;
                        for (Event event : events) {
                            if (event.startTime == startTime && event.endTime == endTime && eventTitle.equals(event.title)) {
                                localEvent = event;
                                break;
                            }
                        }
                        if (localEvent == null) {
                            if (!createEvent(eventId, startTime, endTime, eventTitle, eventAttendeesId, attendeesDetail)) {
                                return null;
                            }
                        } else { //update eventId
                            eventManager.updateEventId(localEvent.id, eventId);
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
                JSONObject conversationObj = httpServerManager.getConversation(conversationId);
                new addConversationTodb().execute(conversationObj, conversationId, eventId, messagearr);

        }
    }


    private class addConversationTodb extends AsyncTask<Object, Void, Void> {

        String conversationTitle;
        String creatorId;
        String creatorName = "";
        List<String> attendeesIdList = new ArrayList<>();
        String conversationId;
        JSONArray messageArr;

        @Override
        protected Void doInBackground(Object... params) {

            JSONObject conversationObj = (JSONObject) params[0];
            conversationId = (String) params[1];
            String eventId = (String) params[2];
            messageArr = (JSONArray) params[3];

            try {
                conversationTitle = conversationObj.getString(HttpServerManager.TITLE);
                creatorId = conversationObj.getString(HttpServerManager.CREATOR_ID);
                JSONArray attendeesArray = conversationObj.getJSONArray(HttpServerManager.ATTENDEES);
                if (attendeesArray != null) {
                    for (int i = 0; i < attendeesArray.length(); i++) {
                        JSONObject attendeeObj = (JSONObject) attendeesArray.get(i);
                        String id = String.valueOf(attendeeObj.getInt(HttpServerManager.UID));
                        // save attendee if not in local database
                        Attendee attendee = null;
                        if (!conversationManager.hasUser(id)) {
                            attendee = new Attendee(
                                    id,
                                    attendeeObj.getString(HttpServerManager.MEDIA_ID),
                                    attendeeObj.getString(HttpServerManager.EMAIL),
                                    attendeeObj.getString(HttpServerManager.PHONE_NUMBER),
                                    attendeeObj.getString(HttpServerManager.FIRST_NAME),
                                    attendeeObj.getString(HttpServerManager.LAST_NAME),
                                    attendeeObj.getString(HttpServerManager.USER_NAME),
                                    false,
                                    true
                            );
                            conversationManager.saveUserToDB(attendee);
                        } else {
                            attendee = conversationManager.getUserById(id);
                        }
                        attendeesIdList.add(id);
                       // attendeesNameList.add(attendee.toString());
                        if (id.equals(creatorId)) {
                            creatorName = attendee.toString();
                        }
                    }
                }

                //create conversation in local database
                if(eventId != "") {
                    conversationDataSource.createEventConversation(eventId, conversationId, conversationTitle, creatorId, attendeesIdList, false, 0);
                }
                else
                {
                    conversationDataSource.createConversation(conversationId, conversationTitle, creatorId, attendeesIdList, false, 0);
                }
            } catch (JSONException e) {
                System.out.println(e.getMessage());
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new addMessageTodb().execute(messageArr, conversationId);
        }
    }



    private boolean createEvent(String eventId, long startTime, long endTime, String title, List<String> attendeeIds, JSONArray attendeesDetail)
    {
        List<Attendee> attendees = new ArrayList<>();
        int i =0;
        for (String id : attendeeIds) {
            Attendee attendee = conversationManager.getUserById(id);
            if (attendee == null) {
                JSONObject obj  = null;
                try {
                    obj = attendeesDetail.getJSONObject(i);
                attendee = new Attendee(
                        id,
                        obj.getString(HttpServerManager.MEDIA_ID),
                        obj.getString(HttpServerManager.EMAIL),
                        obj.getString(HttpServerManager.PHONE_NUMBER),
                        obj.getString(HttpServerManager.FIRST_NAME),
                        obj.getString(HttpServerManager.LAST_NAME),
                        obj.getString(HttpServerManager.USER_NAME),
                        false,
                        true
                );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (!conversationManager.saveUserToDB(attendee)) {
                    return false;
                }
            }
            attendees.add(attendee);
            i++;
        }
        boolean isAllDay = (startTime == endTime);

        Event event = new Event(eventId, 0, null, Event.TYPE_CALENDAR);
        event.title = title;
        event.color = Colors.getColor(this, R.color.green);
        event.startTime = startTime;
        event.endTime = endTime;
        event.allDay = isAllDay;
        event.attendees = attendees;

        String id = eventManager.createEvent(EventManager.EventAction.ACTOR_SELF, event, null);
        return (id != null);
    }



    /*//temporary during development: add all users on server database to client database
    private void resetUserTable(HttpServerManager httpServerManager) {
        AttendeeDataSource attendeeDataSource = DatabaseHelper.getDataSource(this, AttendeeDataSource.class);
        attendeeDataSource.clearAttendeeTable();
        httpServerManager.addAllRegisteredUsersToUserTable(attendeeDataSource);
    }*/

    /**
     * Start KML service to handle location data, if logged in with Google.
     */
    public static void startKMLService(Context context) {
        if (!Settings.getInstance(context).getGoogleHasCookie()) {
            return;
        }
        Intent intent = new Intent(context, KmlDownloadingService.class);
        intent.putExtra(KmlDownloadingService.TYPE, KmlDownloadingService.FIRST_TIME);
        context.startService(intent);

        SupercalyAlarmManager manager = SupercalyAlarmManager.getInstance(context);
        manager.scheduleAlarm(1); // schedule an alarm for every 1-hour
    }

    public void onLogin(Bundle data) {
//        Log.d("LoginActivity", "onLogin");
        int status = Integer.parseInt(data.getString("status"));
        String username = data.getString("username");

        if (status == 0) {
            Account account = new Account(-1);
            account.username = username;

            Intent intent = getIntent();
            if (intent == null) {
                intent = new Intent();
            }
            intent.putExtra(EXTRA_ACCOUNT, account);

            setResult(RESULT_OK, intent);
            finish();
        }
    }

    /**
     * Display the fragment holding the registration view.
     */
    public void showRegister() {
        String tag = getString(R.string.fragment_register);
        showFragment(new RegisterFragment(), tag, true);
    }

    /**
     * Handle the action of submitting user credentials to register.
     *
     * @param email The email to be used.
     * @param phone The phone to be used.
     * @param firstName The first name of the user.
     * @param lastName The last name of the user.
     * @param userName The username to be used.
     * @param password The password to be used.
     */
    public void submitRegister(String email, String phone, String firstName, String lastName,
            String userName, String password) {
        String token = AccountManager.getInstance(this).getFcmToken();
        HttpServerManager httpServerManager = HttpServerManager.getInstance(this);
        String toastMessage;
        int uId = httpServerManager.registerMe(email, firstName, token, lastName, null, phone, userName, password);
        if (uId > 0) {
            ChatServerManager chatServerManager = ChatServerManager.getInstance(this);
            chatServerManager.sendRegister(uId, token);
            toastMessage = "Registration successful.";
            finish();
        } else if (uId == 0){
            toastMessage = "The email address or phone number has already been used.";
        } else {
            toastMessage = "Registration unsuccessful. Please try again.";
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability instance = GoogleApiAvailability.getInstance();
        int resultCode = instance.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (instance.isUserResolvableError(resultCode)) {
                instance.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                finish();
            }

            return false;
        }

        return true;
    }
}