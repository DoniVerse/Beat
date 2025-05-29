package com.example.beat.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.ui.LocalMusicPlayerActivity;

import java.util.List;

public class MusicService extends Service {
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();
    private List<LocalSong> songList;
    private int currentPosition;
    private boolean isPlaying = false;
    private MediaSessionCompat mediaSession;

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "BeatPlayer");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PLAY_PAUSE":
                    togglePlayPause();
                    break;
                case "NEXT":
                    playNext();
                    break;
                case "PREVIOUS":
                    playPrevious();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    public void setupPlayer(List<LocalSong> songs, int position) {
        this.songList = songs;
        this.currentPosition = position;
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        playCurrentSong();
    }

    private void playCurrentSong() {
        if (songList == null || songList.isEmpty() || currentPosition >= songList.size()) {
            return;
        }

        LocalSong currentSong = songList.get(currentPosition);
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, Uri.parse(currentSong.getFilePath()));
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            mediaPlayer.setOnCompletionListener(mp -> playNext());
            updateNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
            } else {
                mediaPlayer.start();
                isPlaying = true;
            }
            updateNotification();
        }
    }

    public void playNext() {
        if (songList != null && !songList.isEmpty()) {
            currentPosition = (currentPosition + 1) % songList.size();
            playCurrentSong();
        }
    }

    public void playPrevious() {
        if (songList != null && !songList.isEmpty()) {
            currentPosition = (currentPosition - 1 + songList.size()) % songList.size();
            playCurrentSong();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows the currently playing music");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        if (songList == null || songList.isEmpty()) return;

        LocalSong currentSong = songList.get(currentPosition);
        
        // Create pending intents for notification actions
        Intent notificationIntent = new Intent(this, LocalMusicPlayerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, MusicService.class);
        prevIntent.setAction("PREVIOUS");
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 1, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MusicService.class);
        playPauseIntent.setAction("PLAY_PAUSE");
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 2, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction("NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(currentSong.getTitle())
                .setContentText("Now Playing")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_previous, "Previous", prevPendingIntent)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, 
                        isPlaying ? "Pause" : "Play", playPausePendingIntent)
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }
} 