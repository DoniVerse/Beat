package com.example.beat.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.beat.R;
import com.example.beat.services.ApiMusicService;

public class PlayerActivity extends AppCompatActivity {
    private static final String TAG = "PlayerActivity";

    private ImageView albumArtImageView;
    private TextView trackTitleTextView;
    private TextView artistNameTextView;
    private SeekBar seekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton shuffleButton;
    private ImageButton repeatButton;
    private boolean isUserSeeking = false;
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private Handler handler;
    private ApiMusicService musicService;
    private boolean serviceBound = false;
    private boolean isActivityVisible = true;

    private String currentTitle;
    private String currentArtist;
    private String currentAlbumArtUrl;
    private String currentStreamUrl;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ApiMusicService.LocalBinder binder = (ApiMusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;

            musicService.setCallback(new ApiMusicService.ServiceCallback() {
                @Override
                public void onNext() {
                    // Handle next track
                }

                @Override
                public void onPrevious() {
                    // Handle previous track
                }
            });

            if (currentStreamUrl != null) {
                musicService.playTrack(currentStreamUrl, currentTitle, currentArtist, currentAlbumArtUrl);
                isPlaying = true;
                updatePlayPauseButton();
                startProgressUpdates();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            serviceBound = false;
        }
    };

    private BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("API_PLAYBACK_STATUS")) {
                boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                updatePlayPauseButton();
            }
        }
    };

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (musicService != null && isPlaying && isActivityVisible) {
                int currentPosition = musicService.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                currentTimeTextView.setText(formatTime(currentPosition));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Handle launch from notification
        if (getIntent().getBooleanExtra("FROM_NOTIFICATION", false)) {
            if (savedInstanceState != null) {
                return;
            }
        }

        handler = new Handler();
        initializeViews();
        setupClickListeners();

        // Register broadcast receiver
        IntentFilter filter = new IntentFilter("API_PLAYBACK_STATUS");
        registerReceiver(playbackReceiver, filter);

        // Get track data from intent
        Intent intent = getIntent();
        if (intent != null) {
            currentTitle = intent.getStringExtra("title");
            currentArtist = intent.getStringExtra("artist");
            currentAlbumArtUrl = intent.getStringExtra("albumArtUrl");
            currentStreamUrl = intent.getStringExtra("streamUrl");

            if (currentStreamUrl != null) {
                updateTrackInfo();
                bindToService();
            }
        }
    }

    private void initializeViews() {
        albumArtImageView = findViewById(R.id.albumArtImageView);
        trackTitleTextView = findViewById(R.id.trackTitleTextView);
        artistNameTextView = findViewById(R.id.artistNameTextView);
        seekBar = findViewById(R.id.seekBar);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);
        playPauseButton = findViewById(R.id.playPauseButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        shuffleButton = findViewById(R.id.shuffleButton);
        repeatButton = findViewById(R.id.repeatButton);
    }

    private void setupClickListeners() {
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        previousButton.setOnClickListener(v -> playPrevious());
        nextButton.setOnClickListener(v -> playNext());
        shuffleButton.setOnClickListener(v -> toggleShuffle());
        repeatButton.setOnClickListener(v -> toggleRepeat());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) {
                    musicService.seekTo(progress);
                    currentTimeTextView.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
                stopProgressUpdates();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (isPlaying) {
                    startProgressUpdates();
                }
            }
        });
    }

    private void bindToService() {
        Intent serviceIntent = new Intent(this, ApiMusicService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(serviceIntent);
    }

    private void updateTrackInfo() {
        trackTitleTextView.setText(currentTitle);
        artistNameTextView.setText(currentArtist);

        if (currentAlbumArtUrl != null && !currentAlbumArtUrl.isEmpty()) {
            Glide.with(this)
                .load(currentAlbumArtUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.default_album_art)
                .into(albumArtImageView);
        } else {
            albumArtImageView.setImageResource(R.drawable.default_album_art);
        }

        if (musicService != null) {
            seekBar.setMax(musicService.getDuration());
            totalTimeTextView.setText(formatTime(musicService.getDuration()));
        }
    }

    private void togglePlayPause() {
        if (musicService != null) {
            if (isPlaying) {
                musicService.pauseTrack();
                stopProgressUpdates();
            } else {
                musicService.resumeTrack();
                startProgressUpdates();
            }
            isPlaying = !isPlaying;
            updatePlayPauseButton();
        }
    }

    private void playPrevious() {
        // Implement previous track logic
    }

    private void playNext() {
        // Implement next track logic
    }

    private void toggleShuffle() {
        isShuffle = !isShuffle;
        shuffleButton.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle);
    }

    private void toggleRepeat() {
        isRepeat = !isRepeat;
        repeatButton.setImageResource(isRepeat ? R.drawable.ic_repeat_on : R.drawable.ic_repeat);
    }

    private void updatePlayPauseButton() {
        if (isActivityVisible) {
            playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    private void startProgressUpdates() {
        handler.removeCallbacks(updateSeekBarRunnable);
        handler.post(updateSeekBarRunnable);
    }

    private void stopProgressUpdates() {
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public void onBackPressed() {
        // Send broadcast to show mini player
        Intent updateIntent = new Intent("MINI_PLAYER_UPDATE");
        updateIntent.putExtra("title", currentTitle);
        updateIntent.putExtra("artist", currentArtist);
        updateIntent.putExtra("albumArtUri", currentAlbumArtUrl);
        updateIntent.putExtra("show", true);
        sendBroadcast(updateIntent);

        // Minimize the activity
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        if (isPlaying) {
            startProgressUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
        stopProgressUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();
        
        if (serviceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding service: " + e.getMessage());
            }
            serviceBound = false;
        }
        
        try {
            unregisterReceiver(playbackReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered, ignore
        }
        
        handler = null;
    }
}

