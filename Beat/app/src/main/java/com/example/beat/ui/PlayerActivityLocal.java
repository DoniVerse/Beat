package com.example.beat.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide; // Using Glide for image loading
import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.service.MediaPlayerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerActivityLocal extends AppCompatActivity {

    private static final String TAG = "PlayerActivityLocal";

    private TextView titleView, artistView, currentTimeView, totalTimeView;
    private ImageView albumArtView;
    private ImageButton playPauseBtn, nextBtn, prevBtn, shuffleBtn, repeatBtn;
    private SeekBar seekBar;

    private MediaPlayerService musicService;
    private boolean isBound = false;
    private List<LocalSong> songs;
    private int currentPosition = -1;
    private Handler handler = new Handler();

    // Service Connection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MusicBinder binder = (MediaPlayerService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            Log.d(TAG, "Service Bound");
            updateUI(); // Update UI once service is connected
            startSeekBarUpdate();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
            Log.d(TAG, "Service Unbound");
        }
    };
    
    // Broadcast Receiver for Service Updates (e.g., song prepared)
    private BroadcastReceiver songPreparedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isBound && musicService != null && "com.example.beat.ACTION_SONG_PREPARED".equals(intent.getAction())) {
                 Log.d(TAG, "Received SONG_PREPARED broadcast");
                 updateUI(); // Update UI when song is prepared
                 startSeekBarUpdate();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_local);
        Log.d(TAG, "onCreate");

        // Initialize Views
        titleView = findViewById(R.id.song_title);
        // artistView = findViewById(R.id.song_artist); // Assuming you add an artist TextView
        albumArtView = findViewById(R.id.album_art);
        currentTimeView = findViewById(R.id.current_time);
        totalTimeView = findViewById(R.id.total_time);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        nextBtn = findViewById(R.id.next_btn);
        prevBtn = findViewById(R.id.prev_btn);
        shuffleBtn = findViewById(R.id.shuffle_btn);
        repeatBtn = findViewById(R.id.repeat_btn);
        seekBar = findViewById(R.id.seekBar);

        // Get data from Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("SONG_LIST") && intent.hasExtra("POSITION")) {
            songs = intent.getParcelableArrayListExtra("SONG_LIST");
            currentPosition = intent.getIntExtra("POSITION", -1);
            Log.d(TAG, "Received song list size: " + (songs != null ? songs.size() : 0) + ", position: " + currentPosition);

            // Start and bind to the service
            startAndBindService();
        } else {
            Log.e(TAG, "Intent data missing. Cannot initialize player.");
            Toast.makeText(this, "Error loading song.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if data is missing
            return;
        }

        setupButtonClickListeners();
        setupSeekBarListener();
        
        // Register receiver for service updates

    }

    private void startAndBindService() {
        Intent serviceIntent = new Intent(this, MediaPlayerService.class);
        serviceIntent.setAction("ACTION_PLAY"); // Tell service to play
        serviceIntent.putParcelableArrayListExtra("SONG_LIST", (ArrayList<LocalSong>) songs);
        serviceIntent.putExtra("POSITION", currentPosition);

        // Start the service (ensures it runs in foreground even if activity is unbound)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Bind to the service (to interact with it)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Starting and binding service");
    }

    private void setupButtonClickListeners() {
        playPauseBtn.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pausePlayer();
                    playPauseBtn.setImageResource(R.drawable.ic_play);
                } else {
                    musicService.resumePlayer();
                    playPauseBtn.setImageResource(R.drawable.ic_pause);
                }
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.playNext();
                // UI will update via onServiceConnected or broadcast receiver
            }
        });

        prevBtn.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.playPrevious();
                // UI will update via onServiceConnected or broadcast receiver
            }
        });

        // TODO: Implement shuffle and repeat logic in the service and update buttons here
        shuffleBtn.setOnClickListener(v -> Toast.makeText(this, "Shuffle Toggled (Not Implemented)", Toast.LENGTH_SHORT).show());
        repeatBtn.setOnClickListener(v -> Toast.makeText(this, "Repeat Toggled (Not Implemented)", Toast.LENGTH_SHORT).show());
    }

    private void setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && musicService != null) {
                    musicService.seekTo(progress);
                    currentTimeView.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBarRunnable); // Pause updates while dragging
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startSeekBarUpdate(); // Resume updates after dragging
            }
        });
    }

    private void updateUI() {
        if (!isBound || musicService == null) {
             Log.w(TAG, "updateUI called but service not bound or null");
             return; // Don't update if service isn't ready
        }

        LocalSong currentSong = musicService.getCurrentSong();
        if (currentSong != null) {
            Log.d(TAG, "Updating UI for song: " + currentSong.getTitle());
            titleView.setText(currentSong.getTitle());
            // if (artistView != null) artistView.setText("Artist ID: " + currentSong.getArtistId()); // TODO: Get actual artist name

            // Load Album Art using Glide
            Uri albumArtUri = null;
            if (currentSong.getAlbumArtUri() != null) {
                try {
                    albumArtUri = Uri.parse(currentSong.getAlbumArtUri());
                } catch (Exception e) { albumArtUri = null; }
            }
            if (albumArtUri == null && currentSong.getAlbumId() != null && currentSong.getAlbumId() > 0) {
                 Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                 albumArtUri = android.content.ContentUris.withAppendedId(sArtworkUri, currentSong.getAlbumId());
            }

            Glide.with(this)
                 .load(albumArtUri)
                 .placeholder(R.drawable.default_artist) // Placeholder image
                 .error(R.drawable.default_artist) // Error image
                 .into(albumArtView);

            int duration = musicService.getDuration();
            seekBar.setMax(duration);
            totalTimeView.setText(formatTime(duration));
            
            if (musicService.isPlaying()) {
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_play);
            }

        } else {
             Log.w(TAG, "Current song is null, cannot update UI fully");
             // Reset UI elements if no song is playing
             titleView.setText("No Song Playing");
             albumArtView.setImageResource(R.drawable.default_artist);
             seekBar.setMax(0);
             seekBar.setProgress(0);
             currentTimeView.setText("0:00");
             totalTimeView.setText("0:00");
             playPauseBtn.setImageResource(R.drawable.ic_play);
        }
    }

    private void startSeekBarUpdate() {
        handler.post(updateSeekBarRunnable);
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && musicService != null && musicService.isPlaying()) {
                int currentMediaPosition = musicService.getCurrentSongPosition();
                seekBar.setProgress(currentMediaPosition);
                currentTimeView.setText(formatTime(currentMediaPosition));
                handler.postDelayed(this, 500); // Update every 500ms
            } else {
                 // Stop updating if not playing or not bound
                 handler.removeCallbacks(this);
            }
        }
    };

    private String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // Re-bind if necessary (e.g., activity restarted)
        if (!isBound && songs != null) {
             Intent serviceIntent = new Intent(this, MediaPlayerService.class);
             bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // Unbind from the service to allow it to run in the background
        // Do NOT stop the service here
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
            Log.d(TAG, "Unbinding service in onStop");
        }
        handler.removeCallbacks(updateSeekBarRunnable); // Stop UI updates when activity is not visible
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        // Unregister receiver
        try {
             unregisterReceiver(songPreparedReceiver);
        } catch (IllegalArgumentException e) {
             Log.w(TAG, "Receiver not registered or already unregistered.");
        }
        // Service continues running in background if playing
    }
    
    // No need to override onBackPressed, default behavior finishes activity
    // Service keeps running due to startForegroundService
}

