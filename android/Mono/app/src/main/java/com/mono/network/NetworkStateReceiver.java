package com.mono.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by haichuand on 9/22/2016.
 * Listens for network state changes and initiates server syncing
 */

public class NetworkStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ServerSyncManager manager = ServerSyncManager.getInstance(context);
        manager.processServerSyncItems();
    }
}
