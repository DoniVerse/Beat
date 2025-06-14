package com.example.beat.ui;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.beat.R;
import com.example.beat.service.MusicService;
import com.example.beat.service.MusicServiceConnection;

public class MiniPlayerManager implements MusicServiceConnection.ServiceConnectionListener {
    private static MiniPlayerManager instance;
    private Context context;
    private View miniPlayerView;
    private ViewGroup parentContainer;
    private MusicServiceConnection musicServiceConnection;

    // Views
    private ImageView miniAlbumArt;
    private TextView miniTrackTitle;
    private TextView miniArtistName;
    private ImageButton miniPlayPauseButton;
    private ImageButton miniPreviousButton;
    private ImageButton miniNextButton;
    
    // Track info
    private String currentTitle = "";
    private String currentArtist = "";
    private String currentAlbumArt = "";
    private String currentStreamUrl = "";

    // Player state
    private boolean isShuffleEnabled = false;
    private boolean isRepeatEnabled = false;

    private MiniPlayerManager(Context context) {
        this.context = context.getApplicationContext();
        initializeMusicService();
    }

    public static MiniPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MiniPlayerManager(context);
        }
        return instance;
    }

    private void initializeMusicService() {
        musicServiceConnection = new MusicServiceConnection(context);
        musicServiceConnection.setServiceConnectionListener(this);
        musicServiceConnection.bindService();
    }

    public void showMiniPlayer(ViewGroup container) {
        Log.d("MiniPlayerManager", "showMiniPlayer called with container: " + container);

        if (miniPlayerView == null) {
            Log.d("MiniPlayerManager", "Creating mini player view");
            createMiniPlayerView();
        }

        Log.d("MiniPlayerManager", "Current parentContainer: " + parentContainer + ", new container: " + container);

        // Always remove from previous container and add to new one
        if (parentContainer != null && parentContainer != container) {
            Log.d("MiniPlayerManager", "Removing from previous container");
            parentContainer.removeView(miniPlayerView);
        }

        if (parentContainer != container) {
            Log.d("MiniPlayerManager", "Setting new container and adding view");
            parentContainer = container;

            // Make sure the view isn't already added
            if (miniPlayerView.getParent() != null) {
                ((ViewGroup) miniPlayerView.getParent()).removeView(miniPlayerView);
            }

            container.addView(miniPlayerView);
            android.util.Log.d("MiniPlayerManager", "View added to container. Container child count: " + container.getChildCount());
        }

        // Show both the mini player view and its container
        miniPlayerView.setVisibility(View.VISIBLE);
        container.setVisibility(View.VISIBLE);

        // Force layout update to ensure proper dimensions
        container.requestLayout();
        container.post(() -> {
            Log.d("MiniPlayerManager", "Container dimensions after layout: " +
                container.getWidth() + "x" + container.getHeight());
        });

        updateMiniPlayerInfo();

        Log.d("MiniPlayerManager", "Mini player shown - container visible: " + (container.getVisibility() == View.VISIBLE));
        Log.d("MiniPlayerManager", "Mini player view visible: " + (miniPlayerView.getVisibility() == View.VISIBLE));
        Log.d("MiniPlayerManager", "Container height: " + container.getHeight() + ", width: " + container.getWidth());
    }

    public void hideMiniPlayer() {
        if (miniPlayerView != null && parentContainer != null) {
            parentContainer.removeView(miniPlayerView);
            parentContainer.setVisibility(View.GONE);
            parentContainer = null;
            Log.d("MiniPlayerManager", "Mini player hidden");
        }
    }

    private void createMiniPlayerView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        miniPlayerView = inflater.inflate(R.layout.mini_player, null);
        
        // Initialize views
        miniAlbumArt = miniPlayerView.findViewById(R.id.miniAlbumArt);
        miniTrackTitle = miniPlayerView.findViewById(R.id.miniTrackTitle);
        miniArtistName = miniPlayerView.findViewById(R.id.miniArtistName);
        miniPlayPauseButton = miniPlayerView.findViewById(R.id.miniPlayPauseButton);
        miniPreviousButton = miniPlayerView.findViewById(R.id.miniPreviousButton);
        miniNextButton = miniPlayerView.findViewById(R.id.miniNextButton);
        
        // Set click listeners
        miniPlayPauseButton.setOnClickListener(v -> togglePlayPause());
        miniPreviousButton.setOnClickListener(v -> playPrevious());
        miniNextButton.setOnClickListener(v -> playNext());

        // Click on mini player opens full player
        miniPlayerView.setOnClickListener(v -> openFullPlayer());
    }

    private void togglePlayPause() {
        if (musicServiceConnection != null) {
            if (musicServiceConnection.isPlaying()) {
                musicServiceConnection.pauseMusic();
            } else {
                musicServiceConnection.resumeMusic();
            }
            updatePlayPauseButton();
        }
    }

    private void playPrevious() {
        try {
            // Use service command to trigger previous track
            android.content.Intent intent = new android.content.Intent(context, com.example.beat.service.MusicService.class);
            intent.setAction("PREVIOUS");
            context.startService(intent);
            android.util.Log.d("MiniPlayerManager", "Previous track service command sent");

            // Update button state after a short delay to ensure service has processed the command
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                updatePlayPauseButton();
            }, 500);
        } catch (Exception e) {
            android.util.Log.e("MiniPlayerManager", "Error sending previous track command", e);
            // Fallback: restart current song
            if (musicServiceConnection != null) {
                musicServiceConnection.seekTo(0);
            }
        }
    }

    private void playNext() {
        try {
            // Use service command to trigger next track
            android.content.Intent intent = new android.content.Intent(context, com.example.beat.service.MusicService.class);
            intent.setAction("NEXT");
            context.startService(intent);
            android.util.Log.d("MiniPlayerManager", "Next track service command sent");

            // Update button state after a short delay to ensure service has processed the command
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                updatePlayPauseButton();
            }, 500);
        } catch (Exception e) {
            android.util.Log.e("MiniPlayerManager", "Error sending next track command", e);
            // Fallback: restart current song
            if (musicServiceConnection != null) {
                musicServiceConnection.seekTo(0);
            }
        }
    }

    private void openFullPlayer() {
        android.content.Intent intent = new android.content.Intent(context, com.example.beat.ui.PlayerActivityWithService.class);
        intent.putExtra("title", currentTitle);
        intent.putExtra("artist", currentArtist);
        intent.putExtra("albumArtUrl", currentAlbumArt);
        intent.putExtra("streamUrl", currentStreamUrl);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void updateTrackInfo(String title, String artist, String albumArt) {
        currentTitle = title;
        currentArtist = artist;
        currentAlbumArt = albumArt;
        updateMiniPlayerInfo();
    }

    public void updateTrackInfo(String streamUrl, String title, String artist, String albumArt) {
        currentStreamUrl = streamUrl;
        currentTitle = title;
        currentArtist = artist;
        currentAlbumArt = albumArt;
        updateMiniPlayerInfo();
    }

    public boolean hasTrackInfo() {
        return currentTitle != null && !currentTitle.isEmpty();
    }

    private void updateMiniPlayerInfo() {
        if (miniPlayerView == null) return;
        
        miniTrackTitle.setText(currentTitle);
        miniArtistName.setText(currentArtist);
        
        // Load album art
        if (currentAlbumArt != null && !currentAlbumArt.isEmpty()) {
            Glide.with(context)
                    .load(currentAlbumArt)
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .into(miniAlbumArt);
        } else {
            miniAlbumArt.setImageResource(R.drawable.default_album_art);
        }
        
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {
        if (miniPlayPauseButton == null) return;

        boolean isPlaying = musicServiceConnection != null && musicServiceConnection.isPlaying();
        Log.d("MiniPlayerManager", "updatePlayPauseButton - isPlaying: " + isPlaying +
            ", currentTitle: " + currentTitle);

        if (isPlaying) {
            Log.d("MiniPlayerManager", "Setting mini pause icon - music is playing");
            miniPlayPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            Log.d("MiniPlayerManager", "Setting mini play icon - music is not playing");
            miniPlayPauseButton.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    @Override
    public void onServiceConnected(MusicService service) {
        // ✅ FIX: Add delay to ensure proper state sync for mini player
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Log.d("MiniPlayerManager", "Service connected - updating mini player button state");
            updatePlayPauseButton();

            // Additional check after another delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                updatePlayPauseButton();
            }, 1000);
        }, 500);
    }

    @Override
    public void onServiceDisconnected() {
        updatePlayPauseButton();
    }



    public boolean isPlaying() {
        return musicServiceConnection != null && musicServiceConnection.isPlaying();
    }
}
