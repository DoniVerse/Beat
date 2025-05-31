package com.example.beat.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.beat.R;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.Artist;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.data.entities.Playlist;
import com.example.beat.data.dao.PlaylistDao;
import com.example.beat.data.entities.PlaylistSong;
import com.example.beat.services.MusicService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LocalMusicPlayerActivity extends AppCompatActivity {
    private static final String TAG = "LocalMusicPlayer";

    private TextView songTitle;
    private ImageView albumArtImageView;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private SeekBar seekBar;
    private ImageButton playPauseBtn, prevBtn, nextBtn, shuffleBtn, repeatBtn, addToPlaylistBtn;

    private LocalSong currentSong;
    private List<LocalSong> songList;
    private int currentPosition = 0;
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private Handler handler;
    private int userId;

    private MusicService musicService;
    private boolean serviceBound = false;
    private boolean isActivityVisible = true;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
                musicService = binder.getService();
                serviceBound = true;
                Log.d(TAG, "Service connected successfully");
                
                // Set up callback for notification controls
                musicService.setCallback(new MusicService.ServiceCallback() {
                    @Override
                    public void onNext() {
                        playNext();
                    }

                    @Override
                    public void onPrevious() {
                        playPrevious();
                    }
                });
                
                // Start playing the current song if one is loaded
                if (currentSong != null) {
                    musicService.playSong(currentSong);
                    updatePlayPauseButton(true);
                    isPlaying = true;
                    startProgressUpdates();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to service: " + e.getMessage());
                Toast.makeText(LocalMusicPlayerActivity.this, 
                    "Error connecting to music service", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            serviceBound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    private BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getAction() != null && intent.getAction().equals("PLAYBACK_STATUS")) {
                    boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                    updatePlayPauseButton(isPlaying);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in playback receiver: " + e.getMessage());
            }
        }
    };

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (musicService != null && isPlaying && isActivityVisible) {
                    int currentPosition = musicService.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentTimeTextView.setText(formatTime(currentPosition));
                    handler.postDelayed(this, 1000);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating seekbar: " + e.getMessage());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_music_player);
        
        // Handle launch from notification
        if (getIntent().getBooleanExtra("FROM_NOTIFICATION", false)) {
            // Just restore the activity, don't recreate the player
            if (savedInstanceState != null) {
                return;
            }
        }

        try {
            handler = new Handler();

            // Register broadcast receiver with error handling
            try {
                unregisterReceiver(playbackReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered, ignore
            }
            IntentFilter filter = new IntentFilter("PLAYBACK_STATUS");
            registerReceiver(playbackReceiver, filter);

            // Get user ID
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getInt("userId", -1);

            if (userId == -1) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            initializeViews();
            setupClickListeners();

            Intent intent = getIntent();
            if (intent != null) {
                ArrayList<LocalSong> songs = intent.getParcelableArrayListExtra("SONG_LIST");
                if (songs != null && !songs.isEmpty()) {
                    songList = songs;
                    currentPosition = intent.getIntExtra("POSITION", 0);
                    if (currentPosition >= 0 && currentPosition < songList.size()) {
                        currentSong = songList.get(currentPosition);
                        updateSongInfo();
                        
                        // Bind to the music service with error handling
                        try {
                            Intent serviceIntent = new Intent(this, MusicService.class);
                            if (!bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)) {
                                Log.e(TAG, "Failed to bind to service");
                                Toast.makeText(this, "Failed to start music service", Toast.LENGTH_SHORT).show();
                            }
                            startService(serviceIntent);
                        } catch (Exception e) {
                            Log.e(TAG, "Error binding to service: " + e.getMessage());
                            Toast.makeText(this, "Error starting music service", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid song position");
                    }
                } else {
                    throw new IllegalArgumentException("No songs provided");
                }
            } else {
                throw new IllegalArgumentException("No intent data provided");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error starting player: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            songTitle = findViewById(R.id.song_title);
            albumArtImageView = findViewById(R.id.album_art);
            currentTimeTextView = findViewById(R.id.current_time);
            totalTimeTextView = findViewById(R.id.total_time);
            seekBar = findViewById(R.id.seekBar);
            playPauseBtn = findViewById(R.id.play_pause_btn);
            prevBtn = findViewById(R.id.prev_btn);
            nextBtn = findViewById(R.id.next_btn);
            shuffleBtn = findViewById(R.id.shuffle_btn);
            repeatBtn = findViewById(R.id.repeat_btn);
            addToPlaylistBtn = findViewById(R.id.add_to_playlist_btn);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            throw e;
        }
    }

    private void setupClickListeners() {
        try {
            playPauseBtn.setOnClickListener(v -> togglePlayPause());
            prevBtn.setOnClickListener(v -> playPrevious());
            nextBtn.setOnClickListener(v -> playNext());
            shuffleBtn.setOnClickListener(v -> toggleShuffle());
            repeatBtn.setOnClickListener(v -> toggleRepeat());
            addToPlaylistBtn.setOnClickListener(v -> showAddToPlaylistDialog());

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
                    stopProgressUpdates();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (isPlaying) {
                        startProgressUpdates();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage());
            throw e;
        }
    }

    private void updateSongInfo() {
        if (currentSong == null) {
            Log.e(TAG, "Attempted to update info for null song");
            return;
        }

        try {
            songTitle.setText(currentSong.getTitle());

            String albumArtUri = currentSong.getAlbumArtUri();
            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                Glide.with(this)
                        .load(albumArtUri)
                        .error(R.drawable.default_artist)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, 
                                    @Nullable Transition<? super Drawable> transition) {
                                if (isActivityVisible) {
                                    albumArtImageView.setImageDrawable(resource);
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                if (isActivityVisible) {
                                    albumArtImageView.setImageResource(R.drawable.default_artist);
                                }
                            }
                        });
            } else {
                albumArtImageView.setImageResource(R.drawable.default_artist);
            }

            if (musicService != null) {
                int duration = musicService.getDuration();
                seekBar.setMax(duration);
                totalTimeTextView.setText(formatTime(duration));
                currentTimeTextView.setText(formatTime(musicService.getCurrentPosition()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating song info: " + e.getMessage());
            Toast.makeText(this, "Error updating song information", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        try {
            if (musicService != null) {
                if (isPlaying) {
                    musicService.pauseSong();
                    stopProgressUpdates();
                } else {
                    musicService.resumeSong();
                    startProgressUpdates();
                }
                isPlaying = !isPlaying;
                updatePlayPauseButton(isPlaying);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling playback: " + e.getMessage());
            Toast.makeText(this, "Error controlling playback", Toast.LENGTH_SHORT).show();
        }
    }

    private void startProgressUpdates() {
        handler.removeCallbacks(updateSeekBarRunnable);
        handler.post(updateSeekBarRunnable);
    }

    private void stopProgressUpdates() {
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    private void updatePlayPauseButton(boolean playing) {
        if (isActivityVisible) {
            playPauseBtn.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    private void playPrevious() {
        try {
            if (songList != null && !songList.isEmpty()) {
                currentPosition = (currentPosition - 1 + songList.size()) % songList.size();
                currentSong = songList.get(currentPosition);
                updateSongInfo();
                if (musicService != null) {
                    musicService.playSong(currentSong);
                    isPlaying = true;
                    updatePlayPauseButton(true);
                    startProgressUpdates();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing previous song: " + e.getMessage());
            Toast.makeText(this, "Error playing previous song", Toast.LENGTH_SHORT).show();
        }
    }

    private void playNext() {
        try {
            if (songList != null && !songList.isEmpty()) {
                if (isShuffle) {
                    currentPosition = new Random().nextInt(songList.size());
                } else {
                    currentPosition = (currentPosition + 1) % songList.size();
                }
                currentSong = songList.get(currentPosition);
                updateSongInfo();
                if (musicService != null) {
                    musicService.playSong(currentSong);
                    isPlaying = true;
                    updatePlayPauseButton(true);
                    startProgressUpdates();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing next song: " + e.getMessage());
            Toast.makeText(this, "Error playing next song", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleShuffle() {
        isShuffle = !isShuffle;
        shuffleBtn.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle);
        Toast.makeText(this, "Shuffle " + (isShuffle ? "enabled" : "disabled"), 
            Toast.LENGTH_SHORT).show();
    }

    private void toggleRepeat() {
        isRepeat = !isRepeat;
        repeatBtn.setImageResource(isRepeat ? R.drawable.ic_repeat_on : R.drawable.ic_repeat);
        Toast.makeText(this, "Repeat " + (isRepeat ? "enabled" : "disabled"), 
            Toast.LENGTH_SHORT).show();
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
        try {
            super.onDestroy();
            stopProgressUpdates();
            
            // Hide mini player when activity is destroyed
            Intent updateIntent = new Intent("MINI_PLAYER_UPDATE");
            updateIntent.putExtra("show", false);
            sendBroadcast(updateIntent);
            
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
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        // Send broadcast to show mini player
        Intent updateIntent = new Intent("MINI_PLAYER_UPDATE");
        updateIntent.putExtra("title", currentSong.getTitle());
        updateIntent.putExtra("artist", getArtistName(currentSong.getArtistId()));
        updateIntent.putExtra("albumArtUri", currentSong.getAlbumArtUri());
        updateIntent.putExtra("show", true);
        sendBroadcast(updateIntent);

        // Minimize the activity
        moveTaskToBack(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Glide.with(this).clear(albumArtImageView);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // If launched from notification, bring activity to front
        if (intent.getBooleanExtra("FROM_NOTIFICATION", false)) {
            if (musicService != null) {
                updateSongInfo();
                updatePlayPauseButton(musicService.isPlaying());
            }
        }
    }
 
    private String formatTime(int milliseconds) {
        try {
            int seconds = milliseconds / 1000;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time: " + e.getMessage());
            return "0:00";
        }
    }

    private void showAddToPlaylistDialog() {
        if (currentSong == null) return;

        // Get all playlists for the current user
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                PlaylistDao playlistDao = db.playlistDao();
                List<Playlist> playlists = playlistDao.getPlaylistsByUser(userId);

                // Create dialog on UI thread
                runOnUiThread(() -> {
                    if (playlists == null || playlists.isEmpty()) {
                        showCreatePlaylistDialog();
                        return;
                    }

                    // Create dialog with playlist options
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Add to Playlist");
                    
                    String[] playlistNames = new String[playlists.size()];
                    for (int i = 0; i < playlists.size(); i++) {
                        playlistNames[i] = playlists.get(i).getName();
                    }

                    builder.setItems(playlistNames, (dialog, which) -> {
                        Playlist selectedPlaylist = playlists.get(which);
                        addToPlaylist(selectedPlaylist);
                    });

                    builder.setPositiveButton("Create New", (dialog, which) -> showCreatePlaylistDialog());
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading playlists", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Playlist");

        // Set up input
        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (playlistName.isEmpty()) {
                Toast.makeText(this, "Please enter a playlist name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create new playlist
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(this);
                    PlaylistDao playlistDao = db.playlistDao();
                    
                    Playlist newPlaylist = new Playlist();
                    newPlaylist.setName(playlistName);
                    newPlaylist.setUserId(userId);
                    
                    long playlistId = playlistDao.insert(newPlaylist);
                    
                    // Add current song to playlist
                    PlaylistSong playlistSong = new PlaylistSong();
                    playlistSong.setPlaylistId((int) playlistId);
                    playlistSong.setSongId(currentSong.getSongId());
                    playlistDao.insertPlaylistSong(playlistSong);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Added to new playlist: " + playlistName, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error creating playlist", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addToPlaylist(Playlist playlist) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                PlaylistDao playlistDao = db.playlistDao();
                
                PlaylistSong playlistSong = new PlaylistSong();
                playlistSong.setPlaylistId(playlist.getPlaylistId());
                playlistSong.setSongId(currentSong.getSongId());
                
                playlistDao.insertPlaylistSong(playlistSong);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Added to playlist: " + playlist.getName(), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error adding to playlist", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private boolean isPlaying() {
        return musicService != null && musicService.isPlaying();
    }

    private String getArtistName(Integer artistId) {
        if (artistId == null) {
            return "Unknown Artist";
        }
        try {
            AppDatabase db = AppDatabase.getInstance(this);
            Artist artist = db.musicDao().getArtistById(artistId);
            return artist != null ? artist.name : "Unknown Artist";
        } catch (Exception e) {
            Log.e(TAG, "Error getting artist name: " + e.getMessage());
            return "Unknown Artist";
        }
    }
}
