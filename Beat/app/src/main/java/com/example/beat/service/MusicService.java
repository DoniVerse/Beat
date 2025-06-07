package com.example.beat.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.beat.MainActivity;
import com.example.beat.R;

import java.io.IOException;

public class MusicService extends Service implements PlaylistManager.PlaylistListener {
    private static final String TAG = "MusicService";
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private String currentTrackTitle = "";
    private String currentArtist = "";

    private PlaylistManager playlistManager;

    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Initialize PlaylistManager
        playlistManager = PlaylistManager.getInstance();
        playlistManager.setPlaylistListener(this);

        Log.d(TAG, "MusicService created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case "PLAY_PAUSE":
                    if (isPlaying()) {
                        pauseMusic();
                    } else {
                        resumeMusic();
                    }
                    showNotification(); // Update notification
                    break;
                case "PREVIOUS":
                    playPrevious();
                    break;
                case "NEXT":
                    playNext();
                    break;
                case "STOP_MUSIC":
                    // User dismissed the notification - stop music
                    stopMusic();
                    break;
            }
        }
        return START_STICKY; // Service will be restarted if killed
    }

    public void playMusic(String streamUrl, String title, String artist, String albumArt) {
        currentTrackTitle = title != null ? title : "Unknown Track";
        currentArtist = artist != null ? artist : "Unknown Artist";

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            
            // Handle both local files and URLs
            if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
                // For URLs (like Deezer previews)
                mediaPlayer.setDataSource(streamUrl);
            } else {
                // For local files
                mediaPlayer.setDataSource(streamUrl);
            }
            
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                showNotification();
                Log.d(TAG, "Music started playing: " + currentTrackTitle);
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                Log.d(TAG, "Music completed");
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                isPlaying = false;
                return true;
            });

        } catch (IOException e) {
            Log.e(TAG, "Error setting up MediaPlayer", e);
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            showNotification(); // Update notification to make it clearable
            Log.d(TAG, "Music paused");
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            showNotification(); // Update notification to make it ongoing again
            Log.d(TAG, "Music resumed");
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        clearNotification(); // Clear notification when music is stopped
        Log.d(TAG, "Music stopped");
    }

    public boolean isPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                Log.e(TAG, "Error getting current position", e);
                return 0;
            }
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (Exception e) {
                Log.e(TAG, "Error getting duration", e);
                return 0;
            }
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(position);
            } catch (Exception e) {
                Log.e(TAG, "Error seeking", e);
            }
        }
    }

    public String getCurrentTrackTitle() {
        return currentTrackTitle;
    }

    public String getCurrentArtist() {
        return currentArtist;
    }

    public void playNext() {
        Log.d(TAG, "Next song requested");
        if (playlistManager != null) {
            playlistManager.playNext();
        }
    }

    public void playPrevious() {
        Log.d(TAG, "Previous song requested");
        if (playlistManager != null) {
            playlistManager.playPrevious();
        }
    }

    // PlaylistManager.PlaylistListener implementation
    @Override
    public void onTrackChanged(String streamUrl, String title, String artist, String albumArt) {
        Log.d(TAG, "Track changed to: " + title);
        playMusic(streamUrl, title, artist, albumArt);

        // Also notify MiniPlayerManager about the track change
        try {
            com.example.beat.ui.MiniPlayerManager.getInstance(this)
                .updateTrackInfo(streamUrl, title, artist, albumArt);
        } catch (Exception e) {
            Log.e(TAG, "Error updating mini player", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create media control intents
        Intent playPauseIntent = new Intent(this, MusicService.class);
        playPauseIntent.setAction("PLAY_PAUSE");
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction("PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction("NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create delete intent for when notification is dismissed
        Intent deleteIntent = new Intent(this, MusicService.class);
        deleteIntent.setAction("STOP_MUSIC");
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_IMMUTABLE);

        // Build rich notification with media controls (always clearable)
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(currentTrackTitle != null ? currentTrackTitle : "Playing Music")
                .setContentText(currentArtist != null ? currentArtist : "Unknown Artist")
                .setSmallIcon(17301540) // Built-in music icon
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent) // Handle notification dismissal
                .setOngoing(false) // Always clearable
                .setShowWhen(false);

        // Add media control actions
        builder.addAction(17301539, "Previous", previousPendingIntent); // Built-in previous icon
        builder.addAction(isPlaying ? 17301539 : 17301540, isPlaying ? "Pause" : "Play", playPausePendingIntent);
        builder.addAction(17301540, "Next", nextPendingIntent); // Built-in next icon

        Notification notification = builder.build();

        // Always use regular notification (clearable) - never foreground service
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void clearNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d(TAG, "MusicService destroyed");
    }
}
