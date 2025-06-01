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
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.bumptech.glide.Glide;
import com.example.beat.R;
import com.example.beat.ui.PlayerActivity;

public class ApiMusicService extends Service {
    private static final String TAG = "ApiMusicService";
    private static final String CHANNEL_ID = "ApiMusicServiceChannel";
    private static final int NOTIFICATION_ID = 2;
    public static final String ACTION_STOP = "com.example.beat.ACTION_STOP";
    private static boolean isLocalMusicPlaying = false;

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

    public static void setLocalMusicPlaying(boolean playing) {
        isLocalMusicPlaying = playing;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);
            
            if (ACTION_STOP.equals(action)) {
                stopPlayback();
                return START_NOT_STICKY;
            }

            switch (action) {
                case "API_PLAY":
                    if (isLocalMusicPlaying) {
                        Toast.makeText(this, "Please stop local music playback first", Toast.LENGTH_SHORT).show();
                        return START_STICKY;
                    }
                    if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        resumeTrack();
                    }
                    break;
                case "API_PAUSE":
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        pauseTrack();
                    }
                    break;
                case "API_NEXT":
                    if (callback != null) callback.onNext();
                    break;
                case "API_PREVIOUS":
                    if (callback != null) callback.onPrevious();
                    break;
            }
        }
        return START_STICKY;
    }

    public void playTrack(String streamUrl, String title, String artist, String albumArtUrl) {
        if (isLocalMusicPlaying) {
            Toast.makeText(this, "Please stop local music playback first", Toast.LENGTH_SHORT).show();
            return;
        }

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
            Toast.makeText(this, "Error playing track", Toast.LENGTH_SHORT).show();
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
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationIntent.putExtra("FROM_NOTIFICATION", true);
            notificationIntent.putExtra("title", currentTitle);
            notificationIntent.putExtra("artist", currentArtist);
            notificationIntent.putExtra("albumArtUrl", currentAlbumArtUrl);
            notificationIntent.putExtra("streamUrl", currentStreamUrl);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create control intents
            Intent playIntent = new Intent("API_PLAY");
            Intent pauseIntent = new Intent("API_PAUSE");
            Intent nextIntent = new Intent("API_NEXT");
            Intent previousIntent = new Intent("API_PREVIOUS");
            Intent stopIntent = new Intent(this, ApiMusicService.class);
            stopIntent.setAction(ACTION_STOP);

            PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 1, playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 2, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent previousPendingIntent = PendingIntent.getBroadcast(this, 4, previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 5, stopIntent,
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
                .setAutoCancel(true)
                .setOngoing(isPlaying)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_previous, "Previous", previousPendingIntent)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                    isPlaying ? "Pause" : "Play",
                    isPlaying ? pausePendingIntent : playPendingIntent)
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
                .addAction(R.drawable.ic_clear, "Stop", stopPendingIntent);

            // Set media style
            androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2);
            builder.setStyle(mediaStyle);

            Notification notification = builder.build();
            
            if (isPlaying) {
                startForeground(NOTIFICATION_ID, notification);
            } else {
                stopForeground(false);
                NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
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

    private void stopPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }
        isPlaying = false;
        stopForeground(true);
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayback();
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