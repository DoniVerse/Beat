package com.example.beat.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.data.entities.Artist;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.ui.LocalMusicPlayerActivity;
import com.example.beat.receivers.MediaControlReceiver;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY = "com.example.beat.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.beat.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.example.beat.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.example.beat.ACTION_PREVIOUS";

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();
    private LocalSong currentSong;
    private AppDatabase database;
    private MediaControlReceiver mediaControlReceiver;
    private boolean isPlaying = false;

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannel();
            database = AppDatabase.getInstance(this);
            
            // Register the broadcast receiver with error handling
            try {
                if (mediaControlReceiver != null) {
                    unregisterReceiver(mediaControlReceiver);
                }
            } catch (IllegalArgumentException e) {
                // Receiver not registered, ignore
            }
            
            mediaControlReceiver = new MediaControlReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_PLAY);
            intentFilter.addAction(ACTION_PAUSE);
            intentFilter.addAction(ACTION_NEXT);
            intentFilter.addAction(ACTION_PREVIOUS);
            registerReceiver(mediaControlReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
            
            Log.d(TAG, "MusicService created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player Service",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Controls for the music player");
                channel.setShowBadge(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created successfully");
                } else {
                    Log.e(TAG, "NotificationManager is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);
            
            if ("STOP_SERVICE".equals(action)) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            switch (action) {
                case ACTION_PLAY:
                    if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        Log.d(TAG, "Processing play action");
                        mediaPlayer.start();
                        updateNotification();
                        broadcastStatus(true);
                    }
                    break;
                    
                case ACTION_PAUSE:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        Log.d(TAG, "Processing pause action");
                        mediaPlayer.pause();
                        updateNotification();
                        broadcastStatus(false);
                    }
                    break;
                    
                case ACTION_NEXT:
                    Log.d(TAG, "Processing next action");
                    if (callback != null) {
                        callback.onNext();
                    }
                    break;
                    
                case ACTION_PREVIOUS:
                    Log.d(TAG, "Processing previous action");
                    if (callback != null) {
                        callback.onPrevious();
                    }
                    break;
            }
        }
        
        // Make the service sticky
        return START_STICKY;
    }

    public void playSong(LocalSong song) {
        if (song == null) {
            Log.e(TAG, "Attempted to play null song");
            return;
        }

        try {
            String filePath = song.getFilePath();
            Log.d(TAG, "Starting playback process for: " + song.getTitle());
            Log.d(TAG, "File path: " + filePath);
            
            // Notify ApiMusicService
            ApiMusicService.setLocalMusicPlaying(true);
            
            // Clean up existing MediaPlayer with error handling
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.reset();
                    mediaPlayer.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error cleaning up previous MediaPlayer: " + e.getMessage());
                }
                mediaPlayer = null;
            }

            // Verify file exists and is readable
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "File does not exist at path: " + filePath);
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!file.canRead()) {
                Log.e(TAG, "File is not readable: " + filePath);
                Toast.makeText(this, "Cannot read file", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "File exists and is readable");

            // Create new MediaPlayer with error handling
            Log.d(TAG, "Creating new MediaPlayer instance");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);

            // Set up error listener first to catch any initialization errors
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                String errorMessage;
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        errorMessage = "Server died";
                        break;
                    case MediaPlayer.MEDIA_ERROR_IO:
                        errorMessage = "IO error";
                        break;
                    case MediaPlayer.MEDIA_ERROR_MALFORMED:
                        errorMessage = "Malformed media";
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                        errorMessage = "Unsupported format";
                        break;
                    default:
                        errorMessage = "Unknown error (code: " + what + ", extra: " + extra + ")";
                        break;
                }
                Log.e(TAG, "MediaPlayer error: " + errorMessage);
                Toast.makeText(MusicService.this, "Playback error: " + errorMessage, Toast.LENGTH_SHORT).show();
                
                // Clean up on error
                try {
                    if (mp != null) {
                        mp.reset();
                        mp.release();
                    }
                    mediaPlayer = null;
                } catch (Exception e) {
                    Log.e(TAG, "Error cleaning up after MediaPlayer error: " + e.getMessage());
                }
                return true;
            });

            // Attempt to set the data source with better error handling
            Log.d(TAG, "Setting data source");
            try {
                if (filePath.startsWith("content://")) {
                    Uri contentUri = Uri.parse(filePath);
                    Log.d(TAG, "Using content URI: " + contentUri);
                    mediaPlayer.setDataSource(getApplicationContext(), contentUri);
                } else {
                    Log.d(TAG, "Using direct file path");
                    mediaPlayer.setDataSource(filePath);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to set data source: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(this, "Error loading audio file", Toast.LENGTH_SHORT).show();
                if (mediaPlayer != null) {
                    try {
                        mediaPlayer.release();
                    } catch (Exception releaseError) {
                        Log.e(TAG, "Error releasing MediaPlayer after data source error: " + releaseError.getMessage());
                    }
                    mediaPlayer = null;
                }
                return;
            }

            currentSong = song;
            
            // Set up completion listener with error handling
            mediaPlayer.setOnCompletionListener(mp -> {
                try {
                    Log.d(TAG, "Song completed: " + song.getTitle());
                    if (callback != null) {
                        callback.onNext();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in completion listener: " + e.getMessage());
                }
            });

            // Set up prepare listener with error handling
            mediaPlayer.setOnPreparedListener(mp -> {
                try {
                    Log.d(TAG, "MediaPlayer prepared, starting playback");
                    mp.start();
                    Notification notification = createNotification();
                    if (notification != null) {
                        startForeground(NOTIFICATION_ID, notification);
                        Log.d(TAG, "Started foreground service with notification");
                    }
                    Log.d(TAG, "Song started playing successfully: " + song.getTitle());
                    broadcastStatus(true);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting playback: " + e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(MusicService.this, "Error starting playback", Toast.LENGTH_SHORT).show();
                }
            });

            // Start preparing the MediaPlayer
            Log.d(TAG, "Starting async preparation");
            mediaPlayer.prepareAsync();
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing song", e);
            e.printStackTrace();
            Toast.makeText(this, "Error playing song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.release();
                } catch (Exception releaseError) {
                    Log.e(TAG, "Error releasing MediaPlayer after error: " + releaseError.getMessage());
                }
                mediaPlayer = null;
            }
        }
    }

    public void pauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                updateNotification();
                Log.d(TAG, "Song paused");
            } catch (Exception e) {
                Log.e(TAG, "Error pausing song: " + e.getMessage());
            }
        }
    }

    public void resumeSong() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.start();
                updateNotification();
                Log.d(TAG, "Song resumed");
            } catch (Exception e) {
                Log.e(TAG, "Error resuming song: " + e.getMessage());
            }
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                Log.e(TAG, "Error getting current position: " + e.getMessage());
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
                Log.e(TAG, "Error in seekTo: " + e.getMessage());
            }
        }
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    private String getArtistName(Integer artistId) {
        if (artistId == null) {
            Log.d(TAG, "No artist ID provided");
            return "Unknown Artist";
        }
        try {
            Artist artist = database.musicDao().getArtistById(artistId);
            return artist != null ? artist.name : "Unknown Artist";
        } catch (Exception e) {
            Log.e(TAG, "Error getting artist name: " + e.getMessage());
            return "Unknown Artist";
        }
    }

    private Notification createNotification() {
        if (currentSong == null) {
            return null;
        }

        try {
            // Create intent for when notification is clicked
            Intent notificationIntent = new Intent(this, LocalMusicPlayerActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationIntent.putExtra("FROM_NOTIFICATION", true);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create intents for buttons
            Intent playIntent = new Intent(ACTION_PLAY);
            Intent pauseIntent = new Intent(ACTION_PAUSE);
            Intent nextIntent = new Intent(ACTION_NEXT);
            Intent prevIntent = new Intent(ACTION_PREVIOUS);

            PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 1, playIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 2, pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 3, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 4, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create stop intent
            Intent stopIntent = new Intent(this, MusicService.class);
            stopIntent.setAction("STOP_SERVICE");
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 5, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create the notification builder
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_local)
                    .setContentTitle(currentSong.getTitle())
                    .setContentText(getArtistName(currentSong.getArtistId()))
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setContentIntent(contentIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setOngoing(mediaPlayer != null && mediaPlayer.isPlaying());

            // Add action buttons
            builder.addAction(R.drawable.ic_previous, "Previous", prevPendingIntent);
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                builder.addAction(R.drawable.ic_pause, "Pause", pausePendingIntent);
            } else {
                builder.addAction(R.drawable.ic_play, "Play", playPendingIntent);
            }
            builder.addAction(R.drawable.ic_next, "Next", nextPendingIntent);
            builder.addAction(R.drawable.ic_clear, "Stop", stopPendingIntent);

            // Set media style
            androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2);
            builder.setStyle(mediaStyle);

            // Set album art if available
            String albumArtUri = currentSong.getAlbumArtUri();
            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                try {
                    builder.setLargeIcon(android.graphics.BitmapFactory.decodeFile(albumArtUri));
                } catch (Exception e) {
                    Log.e(TAG, "Error setting album art: " + e.getMessage());
                }
            }

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification: " + e.getMessage());
            return null;
        }
    }

    private void updateNotification() {
        try {
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                Notification notification = createNotification();
                if (notification != null) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        startForeground(NOTIFICATION_ID, notification);
                    } else {
                        stopForeground(false);
                        notificationManager.notify(NOTIFICATION_ID, notification);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        try {
            // Clean up MediaPlayer
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing MediaPlayer: " + e.getMessage());
                }
                mediaPlayer = null;
            }

            // Notify ApiMusicService
            ApiMusicService.setLocalMusicPlaying(false);

            // Unregister receiver with error handling
            try {
                if (mediaControlReceiver != null) {
                    unregisterReceiver(mediaControlReceiver);
                    mediaControlReceiver = null;
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }

            // Remove notification
            stopForeground(true);
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
            }

            Log.d(TAG, "MusicService destroyed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
        super.onDestroy();
    }

    // Interface for callbacks
    public interface ServiceCallback {
        void onNext();
        void onPrevious();
    }

    private ServiceCallback callback;

    public void setCallback(ServiceCallback callback) {
        this.callback = callback;
    }

    private void broadcastStatus(boolean isPlaying) {
        Intent intent = new Intent("PLAYBACK_STATUS");
        intent.putExtra("isPlaying", isPlaying);
        sendBroadcast(intent);
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }
        // Notify ApiMusicService
        ApiMusicService.setLocalMusicPlaying(false);
        
        isPlaying = false;
        stopForeground(true);
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        stopSelf();
    }
} 