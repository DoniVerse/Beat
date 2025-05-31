package com.example.beat.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.bumptech.glide.Glide;
import com.example.beat.R;
import com.example.beat.ui.PlayerActivity;

public class ApiMusicService extends Service {
    private static final String TAG = "ApiMusicService";
    private static final String CHANNEL_ID = "ApiMusicServiceChannel";
    private static final int NOTIFICATION_ID = 2;

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private String currentStreamUrl;
    private String currentTitle;
    private String currentArtist;
    private String currentAlbumArtUrl;
    private boolean isPlaying = false;
    private MediaSessionCompat mediaSession;
    private ServiceCallback callback;

    public interface ServiceCallback {
        void onNext();
        void onPrevious();
    }

    public class LocalBinder extends Binder {
        public ApiMusicService getService() {
            return ApiMusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            if (callback != null) {
                callback.onNext();
            }
        });

        // Initialize MediaSession
        mediaSession = new MediaSessionCompat(this, "ApiMusicService");

        // Register broadcast receiver for controls
        IntentFilter filter = new IntentFilter();
        filter.addAction("API_PLAY");
        filter.addAction("API_PAUSE");
        filter.addAction("API_NEXT");
        filter.addAction("API_PREVIOUS");
        registerReceiver(controlReceiver, filter);

        createNotificationChannel();
    }

    private final BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            switch (intent.getAction()) {
                case "API_PLAY":
                    resumeTrack();
                    break;
                case "API_PAUSE":
                    pauseTrack();
                    break;
                case "API_NEXT":
                    if (callback != null) callback.onNext();
                    break;
                case "API_PREVIOUS":
                    if (callback != null) callback.onPrevious();
                    break;
            }
        }
    };

    public void setCallback(ServiceCallback callback) {
        this.callback = callback;
    }

    public void playTrack(String streamUrl, String title, String artist, String albumArtUrl) {
        try {
            currentStreamUrl = streamUrl;
            currentTitle = title;
            currentArtist = artist;
            currentAlbumArtUrl = albumArtUrl;

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(streamUrl);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;

            updateNotification();
            broadcastStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error playing track: " + e.getMessage());
        }
    }

    public void pauseTrack() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            updateNotification();
            broadcastStatus();
        }
    }

    public void resumeTrack() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            updateNotification();
            broadcastStatus();
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "API Music Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for API music playback");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void updateNotification() {
        try {
            // Create pending intents for notification actions
            Intent notificationIntent = new Intent(this, PlayerActivity.class);
            notificationIntent.putExtra("FROM_NOTIFICATION", true);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent playIntent = new Intent("API_PLAY");
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent pauseIntent = new Intent("API_PAUSE");
            PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 0, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent nextIntent = new Intent("API_NEXT");
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent previousIntent = new Intent("API_PREVIOUS");
            PendingIntent previousPendingIntent = PendingIntent.getBroadcast(this, 0, previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Load album art
            Bitmap albumArt = null;
            try {
                if (currentAlbumArtUrl != null && !currentAlbumArtUrl.isEmpty()) {
                    albumArt = Glide.with(this)
                        .asBitmap()
                        .load(currentAlbumArtUrl)
                        .submit()
                        .get();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading album art: " + e.getMessage());
            }

            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_local)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setLargeIcon(albumArt)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_previous, "Previous", previousPendingIntent)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                    isPlaying ? "Pause" : "Play",
                    isPlaying ? pausePendingIntent : playPendingIntent)
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent);

            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: " + e.getMessage());
        }
    }

    private void broadcastStatus() {
        Intent intent = new Intent("API_PLAYBACK_STATUS");
        intent.putExtra("isPlaying", isPlaying);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        try {
            unregisterReceiver(controlReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered, ignore
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
    }
} 