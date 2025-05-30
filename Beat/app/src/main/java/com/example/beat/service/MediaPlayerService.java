package com.example.beat.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.ui.PlayerActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = "MediaPlayerService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MediaPlayerServiceChannel";

    private MediaPlayer mediaPlayer;
    private List<LocalSong> songList;
    private int currentPosition = -1;
    private final IBinder binder = new MusicBinder();
    private boolean isPrepared = false;

    public class MusicBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        initMediaPlayer();
        createNotificationChannel();
        Log.d(TAG, "Service Created");
    }

    private void initMediaPlayer() {
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand received");
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                Log.d(TAG, "Action: " + action);
                switch (action) {
                    case "ACTION_PLAY":
                        if (intent.hasExtra("SONG_LIST") && intent.hasExtra("POSITION")) {
                            songList = intent.getParcelableArrayListExtra("SONG_LIST");
                            currentPosition = intent.getIntExtra("POSITION", -1);
                            if (songList != null && !songList.isEmpty() && currentPosition >= 0 && currentPosition < songList.size()) {
                                playSong();
                            } else {
                                Log.e(TAG, "Invalid song list or position");
                                stopSelf(); // Stop if data is invalid
                            }
                        } else {
                             Log.w(TAG, "PLAY action without song list/position, resuming?");
                             if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
                                 resumePlayer();
                             }
                        }
                        break;
                    case "ACTION_PAUSE":
                        pausePlayer();
                        break;
                    case "ACTION_NEXT":
                        playNext();
                        break;
                    case "ACTION_PREVIOUS":
                        playPrevious();
                        break;
                    case "ACTION_STOP_FOREGROUND":
                         Log.d(TAG, "Stopping foreground service");
                         stopForeground(true);
                         // Optionally stop playback or just remove notification
                         // pausePlayer(); 
                         break;
                }
            }
        }
        // If the service is killed, restart it, but only if it was started with START_STICKY
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service Bound");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service Unbound");
        // If we want onRebind to be called, return true
        return true; 
    }
     @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "Service Rebound");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true); // Ensure notification is removed
        Log.d(TAG, "Service Destroyed");
    }

    // Playback Control Methods
    public void playSong() {
        if (songList == null || songList.isEmpty() || currentPosition < 0 || currentPosition >= songList.size()) {
            Log.e(TAG, "Cannot play song, invalid state");
            return;
        }

        mediaPlayer.reset();
        isPrepared = false;
        LocalSong playSong = songList.get(currentPosition);
        String filePath = playSong.getFilePath();
        Log.d(TAG, "Playing song: " + playSong.getTitle() + " at path: " + filePath);

        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepareAsync(); // Prepare asynchronously
            startForeground(NOTIFICATION_ID, buildNotification(playSong, true)); // Start foreground immediately
        } catch (IOException e) {
            Log.e(TAG, "Error setting data source", e);
            stopSelf(); // Stop service if song cannot be loaded
        } catch (IllegalStateException e) {
             Log.e(TAG, "Error preparing media player (IllegalStateException)", e);
             stopSelf();
        }
    }

    public void pausePlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateNotification(getCurrentSong(), false); // Update notification to show paused state
            Log.d(TAG, "Player Paused");
        }
    }

    public void resumePlayer() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && isPrepared) {
            mediaPlayer.start();
            updateNotification(getCurrentSong(), true); // Update notification to show playing state
            Log.d(TAG, "Player Resumed");
        }
    }

    public void stopPlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            isPrepared = false;
            stopForeground(true); // Remove notification when stopped
            Log.d(TAG, "Player Stopped");
        }
    }

    public void playNext() {
        if (songList != null && !songList.isEmpty()) {
            currentPosition = (currentPosition + 1) % songList.size();
            playSong();
            Log.d(TAG, "Playing Next");
        }
    }

    public void playPrevious() {
        if (songList != null && !songList.isEmpty()) {
            currentPosition = (currentPosition > 0) ? currentPosition - 1 : songList.size() - 1;
            playSong();
            Log.d(TAG, "Playing Previous");
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentSongPosition() {
        if (mediaPlayer != null && isPrepared) {
            try {
                 return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                 Log.e(TAG, "Error getting current position", e);
                 return 0;
            }
        } 
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
             try {
                 return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                 Log.e(TAG, "Error getting duration", e);
                 return 0;
            }
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
             try {
                mediaPlayer.seekTo(position);
            } catch (IllegalStateException e) {
                 Log.e(TAG, "Error seeking", e);
            }
        }
    }
    
    public LocalSong getCurrentSong() {
        if (songList != null && currentPosition >= 0 && currentPosition < songList.size()) {
            return songList.get(currentPosition);
        }
        return null;
    }

    // MediaPlayer Listeners
    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        mp.start();
        Log.d(TAG, "MediaPlayer Prepared and Started");
        // Optionally, broadcast an event or update UI via bound activity
        Intent intent = new Intent("com.example.beat.ACTION_SONG_PREPARED");
        sendBroadcast(intent);
        updateNotification(getCurrentSong(), true);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "Song Completed");
        // Implement repeat/shuffle logic here if needed
        // For now, just play the next song
        playNext();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer Error - what: " + what + " extra: " + extra);
        mp.reset(); // Reset the player on error
        isPrepared = false;
        stopForeground(true); // Stop foreground and remove notification on error
        // Optionally, notify the user or try playing the next song
        return true; // Indicates the error was handled
    }

    // Notification Handling
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player Service Channel",
                    NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound/vibration
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                Log.d(TAG, "Notification Channel Created");
            } else {
                 Log.e(TAG, "Failed to get NotificationManager");
            }
        }
    }

    private Notification buildNotification(LocalSong song, boolean isPlaying) {
        if (song == null) {
             Log.e(TAG, "Cannot build notification, song is null");
             // Return a minimal notification or handle error
             return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Beat Music Player")
                .setContentText("Playback error")
                .setSmallIcon(R.drawable.ic_play) // Placeholder icon
                .build();
        }
        
        Log.d(TAG, "Building notification for: " + song.getTitle() + ", isPlaying: " + isPlaying);

        // Intent to open the player activity when notification is clicked
        Intent notificationIntent = new Intent(this, PlayerActivity.class);
        // Pass necessary data to reopen the player in the correct state
        notificationIntent.putParcelableArrayListExtra("SONG_LIST", (ArrayList<LocalSong>) songList);
        notificationIntent.putExtra("POSITION", currentPosition);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        // Create PendingIntents for notification actions
        PendingIntent playPausePendingIntent = createActionIntent(isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY", 1);
        PendingIntent nextPendingIntent = createActionIntent("ACTION_NEXT", 2);
        PendingIntent prevPendingIntent = createActionIntent("ACTION_PREVIOUS", 3);
        PendingIntent stopPendingIntent = createActionIntent("ACTION_STOP_FOREGROUND", 4); // To close notification

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.getTitle())
                .setContentText("Artist ID: " + song.getArtistId()) // TODO: Get actual artist name
                .setSmallIcon(R.drawable.ic_play) // TODO: Use a proper app icon
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Makes the notification non-dismissible while playing
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Add actions
                .addAction(R.drawable.ic_previous, "Previous", prevPendingIntent)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, isPlaying ? "Pause" : "Play", playPausePendingIntent)
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent) // Simple stop/close action
                // Apply media style
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        // .setMediaSession(mediaSession.getSessionToken()) // TODO: Integrate MediaSession
                        .setShowActionsInCompactView(1, 2)); // Indices of actions to show in compact view (Play/Pause, Next)

        // TODO: Load album art for notification
        // builder.setLargeIcon(albumArtBitmap);

        return builder.build();
    }
    
    private PendingIntent createActionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, MediaPlayerService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    private void updateNotification(LocalSong song, boolean isPlaying) {
        Notification notification = buildNotification(song, isPlaying);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
            Log.d(TAG, "Notification Updated");
        } else {
             Log.e(TAG, "Failed to get NotificationManager for update");
        }
    }
}

