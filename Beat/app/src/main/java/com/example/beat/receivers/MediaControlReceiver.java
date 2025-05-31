package com.example.beat.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MediaControlReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaControlReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        Log.d(TAG, "Received action: " + intent.getAction());
        
        Intent serviceIntent = new Intent(context, com.example.beat.services.MusicService.class);
        serviceIntent.setAction(intent.getAction());
        
        try {
            context.startService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error forwarding media control action to service", e);
        }
    }
} 