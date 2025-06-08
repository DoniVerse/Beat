package com.example.beat.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
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
    private String currentAlbumArt = "";
    private String currentStreamUrl = "";

    private PlaylistManager playlistManager;

    // Track change listener interface
    public interface TrackChangeListener {
        void onTrackChanged(String streamUrl, String title, String artist, String albumArt);
    }

    private TrackChangeListener trackChangeListener;

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
        currentAlbumArt = albumArt != null ? albumArt : "";
        currentStreamUrl = streamUrl != null ? streamUrl : "";

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
                android.util.Log.d(TAG, "🎵 SONG COMPLETED! 🎵 - Title: " + currentTrackTitle);
                android.util.Log.d(TAG, "🔁 About to check repeat functionality...");
                handleSongCompletion();
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

    public String getCurrentStreamUrl() {
        return currentStreamUrl;
    }

    // Track change listener methods
    public void setTrackChangeListener(TrackChangeListener listener) {
        this.trackChangeListener = listener;
        Log.d(TAG, "Track change listener set");
    }

    public void removeTrackChangeListener() {
        this.trackChangeListener = null;
        Log.d(TAG, "Track change listener removed");
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

    private void handleSongCompletion() {
        android.util.Log.d(TAG, "Song completed, handling repeat/next");
        android.util.Log.d(TAG, "PlaylistManager null check: " + (playlistManager == null));

        if (playlistManager != null) {
            boolean repeatEnabled = playlistManager.isRepeatEnabled();
            android.util.Log.d(TAG, "Repeat enabled: " + repeatEnabled);

            if (repeatEnabled) {
                // Repeat current song - simple approach
                android.util.Log.d(TAG, "Repeat enabled - restarting current song: " + currentTrackTitle);
                if (mediaPlayer != null) {
                    try {
                        // Simple restart: just call playMusic again with same parameters
                        android.util.Log.d(TAG, "Restarting song for repeat");
                        playMusic(currentStreamUrl, currentTrackTitle, currentArtist, currentAlbumArt);

                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error restarting song for repeat", e);
                        // Fallback: try to go to next song
                        playNext();
                    }
                }
            } else {
                // Go to next song
                android.util.Log.d(TAG, "Repeat disabled - going to next song");
                playNext();
            }
        } else {
            android.util.Log.w(TAG, "PlaylistManager is null, cannot handle song completion");
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

        // Notify track change listener (PlayerActivityWithService)
        if (trackChangeListener != null) {
            trackChangeListener.onTrackChanged(streamUrl, title, artist, albumArt);
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

        // Add album art if available, otherwise use default
        Bitmap albumArtBitmap = null;
        if (currentAlbumArt != null && !currentAlbumArt.isEmpty()) {
            try {
                // Load album art bitmap for notification
                albumArtBitmap = loadAlbumArtBitmap(currentAlbumArt);
            } catch (Exception e) {
                Log.e(TAG, "Error loading album art for notification", e);
            }
        }

        // If no album art loaded, use default album art
        if (albumArtBitmap == null) {
            try {
                albumArtBitmap = loadDefaultAlbumArtBitmap();
            } catch (Exception e) {
                Log.e(TAG, "Error loading default album art for notification", e);
            }
        }

        // Set large icon if we have any bitmap (album art or default)
        if (albumArtBitmap != null) {
            builder.setLargeIcon(albumArtBitmap);
        }

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

    private Bitmap loadAlbumArtBitmap(String albumArtUri) {
        try {
            if (albumArtUri.startsWith("content://")) {
                // Load from content URI (MediaStore album art)
                return MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(albumArtUri));
            } else if (albumArtUri.startsWith("http://") || albumArtUri.startsWith("https://")) {
                // Load from URL (for online sources)
                // For now, return null - could implement URL loading later
                return null;
            } else {
                // Load from file path
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // Scale down to reduce memory usage
                return BitmapFactory.decodeFile(albumArtUri, options);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading album art bitmap", e);
            return null;
        }
    }

    private Bitmap loadDefaultAlbumArtBitmap() {
        try {
            Log.d(TAG, "Loading default album art bitmap for notification");

            // Create a beautiful gradient bitmap as default
            Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            // Create gradient background
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setAntiAlias(true);

            // Create a radial gradient from center
            android.graphics.RadialGradient gradient = new android.graphics.RadialGradient(
                64, 64, 64, // center x, y, radius
                new int[]{0xFF6200EA, 0xFF3700B3, 0xFF1A1A1A}, // purple to dark
                new float[]{0f, 0.7f, 1f}, // positions
                android.graphics.Shader.TileMode.CLAMP
            );
            paint.setShader(gradient);
            canvas.drawCircle(64, 64, 64, paint);

            // Draw a beautiful music note
            paint.setShader(null);
            paint.setColor(0xFFFFFFFF); // White icon
            paint.setAntiAlias(true);

            // Draw music note head (filled circle)
            canvas.drawCircle(45, 75, 12, paint);

            // Draw note stem
            paint.setStrokeWidth(6);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            canvas.drawLine(57, 75, 57, 35, paint);

            // Draw note flag (curved)
            android.graphics.Path flagPath = new android.graphics.Path();
            flagPath.moveTo(57, 35);
            flagPath.quadTo(75, 30, 85, 45);
            flagPath.quadTo(75, 50, 57, 45);
            flagPath.close();
            paint.setStyle(android.graphics.Paint.Style.FILL);
            canvas.drawPath(flagPath, paint);

            // Add a subtle glow effect
            paint.setColor(0x40FFFFFF);
            paint.setMaskFilter(new android.graphics.BlurMaskFilter(4, android.graphics.BlurMaskFilter.Blur.OUTER));
            canvas.drawCircle(45, 75, 12, paint);

            Log.d(TAG, "Beautiful default album art bitmap created successfully");
            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error loading default album art bitmap", e);
        }
        return null;
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
