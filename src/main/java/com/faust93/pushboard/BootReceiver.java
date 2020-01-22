package com.faust93.pushboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by faust on 05.05.14.
 */
public class BootReceiver extends BroadcastReceiver implements Const {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, 0);
            if (prefs.getBoolean("srvEnabled", false) && !PushBoardService.mStarted ) {
                PushBoardService.actionStart(context);
            }
        }
    }
}