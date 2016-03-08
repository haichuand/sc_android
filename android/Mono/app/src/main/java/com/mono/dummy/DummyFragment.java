package com.mono.dummy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.RequestCodes;
import com.mono.chat.GcmMessage;
import com.mono.dummy.KML.DownloadListener;
import com.mono.util.SimpleTabLayout.TabPagerCallback;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DummyFragment extends Fragment implements TabPagerCallback {

    private MainInterface mainInterface;
    private KML kml;

    private TextView text;
    private Button send_button;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RequestCodes.Activity.DUMMY_WEB:
                if (resultCode == Activity.RESULT_OK) {
                    kml.getLastKML();
                }
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainInterface) {
            mainInterface = (MainInterface) context;
            kml = new KML(context, mainInterface);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kml, container, false);

        text = (TextView) view.findViewById(R.id.text);
        send_button  = (Button) view.findViewById(R.id.button_send);

        send_button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                sendMessage(view);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        kml.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        kml.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.dummy, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_login:
                mainInterface.showWebActivity(null, RequestCodes.Activity.WEB);
                break;
            case R.id.action_logout:
                KML.clearCookies(getContext());
                break;
        }

        return false;
    }

    @Override
    public void onPageSelected() {

    }

    @Override
    public ViewPager getTabLayoutViewPager() {
        return null;
    }

    @Override
    public ActionButton getActionButton() {
        return new ActionButton(R.drawable.ic_star_border_white, 0, new OnClickListener() {
            @Override
            public void onClick(View view) {
                kml.getKML(getParentFragment(), RequestCodes.Activity.DUMMY_WEB,
                    new DownloadListener() {
                        @Override
                        public void onFinish(int status, Uri uri) {
                            if (uri != null) {
                                outputFile(uri.getPath());
                            }
                        }
                    }
                );
            }
        });
    }

    public void outputFile(String path) {
        try {
            StringBuilder builder = new StringBuilder(path + "\n\n");
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String nextLine;

            while ((nextLine = reader.readLine()) != null) {
                builder.append(nextLine);
                builder.append('\n');
            }

            text.setText(builder.toString());

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(View view) {
        String message = "Hello World";
        String sender_id = "theUserIdOfThisDevice";
        String conversation_id = "conversationId";
        String action = "MESSAGE";
        List<String> recipients = new ArrayList<>();
        //use the google gcm token to test the messaging function
        //TODO: change all the token to user_id generated by our own server
        recipients.add("faDH8E5Uzfc:APA91bH5-wo_-pKLJ3oqJtniHmOXX6mGZK_kVZqNqbKC8n5EY_X397p2nd0vRUtfkA8TeOO04RalvCLa7sZTVJSNapzD4EHQuAJh8uh_CFGXwFknsMcZMJizOQHM9HLj-qcaAY4isRYR");
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this.getContext());
        GcmMessage gcmMessage = GcmMessage.getInstance(this.getContext());
        gcmMessage.sendMessage(sender_id,conversation_id,message,action,recipients,gcm);
    }
}
