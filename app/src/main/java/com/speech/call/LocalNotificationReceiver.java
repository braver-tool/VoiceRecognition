package com.speech.call;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.speech.call.AppService.ACTION_SPEECH_RECOGNIZER_DESTROY;
import static com.speech.call.AppService.IS_STOP_SERVICE;
import static com.speech.call.AppService.NOTIFICATION_ID;

public class LocalNotificationReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            if (intent.getBooleanExtra(IS_STOP_SERVICE, false)) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ID);
                Intent navIntent = new Intent(ACTION_SPEECH_RECOGNIZER_DESTROY);
                context.sendBroadcast(navIntent);
            }

        }
    }

}