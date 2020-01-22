package com.faust93.pushboard;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;


/**
 * Created by faust on 17.04.14.
 */
public class NotifyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null) {
            NotificationManager nMgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context);
            Intent myIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nBuilder.setAutoCancel(true);
            nBuilder.setSmallIcon(R.drawable.ic_launcher);
            nBuilder.setContentTitle(intent.getStringExtra("subj"));
            nBuilder.setContentText(intent.getStringExtra("msg"));
            nBuilder.setDefaults(Notification.DEFAULT_SOUND);
            nBuilder.setContentIntent(pendingIntent);
            nMgr.notify(123, nBuilder.build());
        }
        abortBroadcast();
    }
}
