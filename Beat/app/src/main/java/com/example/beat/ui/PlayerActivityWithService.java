package com.example.beat.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.beat.R;
import com.example.beat.service.MusicService;
import com.example.beat.service.MusicServiceConnection;
import com.google.android.material.imageview.ShapeableImageView;

public class PlayerActivityWithService extends AppCompatActivity implements MusicServiceConnection.ServiceConnectionListener, com.example.beat.service.MusicService.TrackChangeListener {

    private MusicServiceConnection musicServiceConnection;
    private ShapeableImageView albumArtImageView;
    private TextView trackTitleTextView;
    private TextView artistNameTextView;
    private SeekBar seekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private ImageButton playPauseButton;
    private ImageButton backButton;
    private ImageButton shuffleButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton repeatButton;
    private ImageButton addToPlaylistButton;

    
    private String trackTitle;
    private String artistName;
    private String albumArtUrl;
    private String streamUrl;

    // Playlist navigation support
    private java.util.List<String> songList;
    private java.util.List<String> titleList;
    private java.util.List<String> artistList;
    private java.util.List<String> albumArtList;
    private int currentPosition = 0;
    
    private Handler handler = new Handler();
    private boolean isUserSeeking = false;
    private boolean isServiceReady = false;



    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (musicServiceConnection != null && musicServiceConnection.isPlaying() && !isUserSeeking) {
                int currentPosition = musicServiceConnection.getCurrentPosition();
                int duration = musicServiceConnection.getDuration();
                
                seekBar.setProgress(currentPosition);
                currentTimeTextView.setText(formatTime(currentPosition));
                
                if (duration > 0) {
                    seekBar.setMax(duration);
                    totalTimeTextView.setText(formatTime(duration));
                }
                
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initializeViews();
        extractIntentData();
        setupClickListeners();

        // Initialize button states
        updateShuffleButton();
        updateRepeatButton();

        // Initialize music service connection
        musicServiceConnection = new MusicServiceConnection(this);
        musicServiceConnection.setServiceConnectionListener(this);
        musicServiceConnection.bindService();


    }

    private void initializeViews() {
        albumArtImageView = findViewById(R.id.albumArtImageView);
        trackTitleTextView = findViewById(R.id.trackTitleTextView);
        artistNameTextView = findViewById(R.id.artistNameTextView);
        seekBar = findViewById(R.id.seekBar);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);
        playPauseButton = findViewById(R.id.playPauseButton);
        backButton = findViewById(R.id.backButton);
        shuffleButton = findViewById(R.id.shuffleButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        repeatButton = findViewById(R.id.repeatButton);
        addToPlaylistButton = findViewById(R.id.addToPlaylistButton);

        // Initialize play/pause button to show play icon
        if (playPauseButton != null) {
            playPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    private void extractIntentData() {
        try {
            Intent intent = getIntent();
            if (intent == null) {
                android.util.Log.e("PlayerActivity", "Intent is null");
                return;
            }

            trackTitle = intent.getStringExtra("title");
            artistName = intent.getStringExtra("artist");
            albumArtUrl = intent.getStringExtra("albumArtUrl");
            streamUrl = intent.getStringExtra("streamUrl");

            android.util.Log.d("PlayerActivity", "Basic data extracted - Title: " + trackTitle + ", URL: " + streamUrl);

            // Get playlist info and context - simplified to avoid serialization issues
            currentPosition = intent.getIntExtra("POSITION", 0);
            int totalSongs = intent.getIntExtra("TOTAL_SONGS", 0);
            String contextType = intent.getStringExtra("CONTEXT_TYPE");
            int contextId = intent.getIntExtra("CONTEXT_ID", -1);

            android.util.Log.d("PlayerActivity", "Position: " + currentPosition + ", Total songs: " + totalSongs +
                ", Context: " + contextType + ", ID: " + contextId);

            if (totalSongs > 1) {
                // Load playlist from database based on context
                loadPlaylistFromDatabase(contextType, contextId);
            } else {
                android.util.Log.d("PlayerActivity", "Single song mode");
                songList = null;
                titleList = null;
                artistList = null;
                albumArtList = null;
            }
        } catch (Exception e) {
            android.util.Log.e("PlayerActivity", "Error extracting intent data", e);
            // Fallback to safe defaults
            trackTitle = trackTitle != null ? trackTitle : "Unknown Track";
            artistName = artistName != null ? artistName : "Unknown Artist";
            albumArtUrl = "";
            songList = null;
            titleList = null;
            artistList = null;
            albumArtList = null;
            currentPosition = 0;
        }

        // Update UI with track info
        trackTitleTextView.setText(trackTitle != null ? trackTitle : "Unknown Track");
        artistNameTextView.setText(artistName != null ? artistName : "Unknown Artist");
        
        // Load album art
        if (albumArtUrl != null && !albumArtUrl.isEmpty()) {
            Glide.with(this)
                    .load(albumArtUrl)
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .into(albumArtImageView);
        } else {
            Glide.with(this)
                    .load(R.drawable.default_album_art)
                    .into(albumArtImageView);
        }
    }

    private void loadPlaylistFromDatabase(String contextType, int contextId) {
        new Thread(() -> {
            try {
                com.example.beat.data.database.AppDatabase db = com.example.beat.data.database.AppDatabase.getInstance(this);
                com.example.beat.data.dao.MusicDao musicDao = db.musicDao();

                java.util.List<com.example.beat.data.entities.LocalSong> songs = null;

                // Load songs based on context type
                if ("ARTIST_SONGS".equals(contextType) && contextId != -1) {
                    android.util.Log.d("PlayerActivity", "Loading songs for artist ID: " + contextId);
                    songs = musicDao.getSongsByArtistAndUser(contextId, 1); // Using userId = 1 for now
                } else if ("PLAYLIST_SONGS".equals(contextType) && contextId != -1) {
                    android.util.Log.d("PlayerActivity", "Loading songs for playlist ID: " + contextId);
                    // Get playlist with songs
                    com.example.beat.data.dao.PlaylistDao playlistDao = db.playlistDao();
                    com.example.beat.data.entities.PlaylistWithSongs playlistWithSongs = playlistDao.getPlaylistWithSongs(contextId);
                    if (playlistWithSongs != null) {
                        songs = playlistWithSongs.songs;
                    }
                } else if ("ALBUM_SONGS".equals(contextType) && contextId != -1) {
                    android.util.Log.d("PlayerActivity", "Loading songs for album ID: " + contextId);
                    songs = musicDao.getSongsByAlbumAndUser(contextId, 1); // Using userId = 1 for now
                    android.util.Log.d("PlayerActivity", "Found " + (songs != null ? songs.size() : "null") + " songs for album ID: " + contextId);
                } else {
                    // Default: load all songs for the user (LOCAL_SONGS context)
                    android.util.Log.d("PlayerActivity", "Loading all local songs for user");
                    songs = musicDao.getSongsByUser(1); // Using userId = 1 for now
                }

                if (songs != null && !songs.isEmpty()) {
                    // Create playlist arrays
                    songList = new java.util.ArrayList<>();
                    titleList = new java.util.ArrayList<>();
                    artistList = new java.util.ArrayList<>();
                    albumArtList = new java.util.ArrayList<>();

                    for (com.example.beat.data.entities.LocalSong song : songs) {
                        if (song.getFilePath() != null && !song.getFilePath().trim().isEmpty()) {
                            songList.add(song.getFilePath());
                            titleList.add(song.getTitle() != null ? song.getTitle() : "Unknown Track");
                            artistList.add("Local Artist");
                            albumArtList.add(song.getAlbumArtUri() != null ? song.getAlbumArtUri() : "");
                        }
                    }

                    android.util.Log.d("PlayerActivity", "Loaded " + contextType + " playlist from database: " + songList.size() + " songs");
                } else {
                    android.util.Log.d("PlayerActivity", "No songs found for context: " + contextType);
                }
            } catch (Exception e) {
                android.util.Log.e("PlayerActivity", "Error loading playlist from database", e);
            }
        }).start();
    }

    private void setupClickListeners() {
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        backButton.setOnClickListener(v -> onBackPressed()); // Use onBackPressed instead of finish

        // Add click listeners for new controls
        shuffleButton.setOnClickListener(v -> toggleShuffle());
        previousButton.setOnClickListener(v -> playPrevious());
        nextButton.setOnClickListener(v -> playNext());
        repeatButton.setOnClickListener(v -> toggleRepeat());
        addToPlaylistButton.setOnClickListener(v -> showAddToPlaylistDialog());


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userProgress = 0;
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    userProgress = progress;
                    currentTimeTextView.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (musicServiceConnection != null) {
                    musicServiceConnection.seekTo(userProgress);
                }
            }
        });
    }

    @Override
    public void onServiceConnected(MusicService service) {
        isServiceReady = true;

        // Set up track change listener
        service.setTrackChangeListener(this);

        // Setup playlist in PlaylistManager for next/previous functionality
        setupPlaylistManager();

        // Check if we need to start playing or just sync UI
        if (streamUrl != null) {
            android.util.Log.d("PlayerActivity", "Service connected - URL: " + streamUrl + ", Title: " + trackTitle);

            // Check if the same song is already playing
            boolean isSameSongPlaying = musicServiceConnection.getCurrentTrackTitle() != null &&
                                      musicServiceConnection.getCurrentTrackTitle().equals(trackTitle) &&
                                      musicServiceConnection.isPlaying();

            if (isSameSongPlaying) {
                android.util.Log.d("PlayerActivity", "Same song already playing, just syncing UI");
                // Just update UI without restarting playback
                updatePlayPauseButton();
                startSeekBarUpdate();

                // Ensure PlaylistManager is synced with current position
                syncPlaylistManagerPosition();
            } else {
                android.util.Log.d("PlayerActivity", "Starting new playback");
                try {
                    musicServiceConnection.playMusic(streamUrl, trackTitle, artistName, albumArtUrl);
                    // Don't set button state immediately - let updatePlayPauseButton handle it
                    // Add a small delay to allow service to start playing
                    handler.postDelayed(() -> {
                        updatePlayPauseButton();
                        startSeekBarUpdate();
                    }, 500); // 500ms delay to allow service to start
                } catch (Exception e) {
                    android.util.Log.e("PlayerActivity", "Error starting playback", e);
                    android.widget.Toast.makeText(this, "Error playing song: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            }

            // Always update mini player with current track info
            updateMiniPlayer();
        } else {
            android.util.Log.e("PlayerActivity", "streamUrl is null - cannot start playback");
            android.widget.Toast.makeText(this, "Error: No song URL provided", android.widget.Toast.LENGTH_SHORT).show();
        }

        // Note: Song completion is now handled by MusicService
    }



    @Override
    public void onServiceDisconnected() {
        isServiceReady = false;
        stopSeekBarUpdate();
    }

    private void togglePlayPause() {
        if (musicServiceConnection != null && isServiceReady) {
            if (musicServiceConnection.isPlaying()) {
                musicServiceConnection.pauseMusic();
            } else {
                musicServiceConnection.resumeMusic();
            }
            updatePlayPauseButton();
        } else {
            // Show debug message if service not ready
            android.widget.Toast.makeText(this, "Service not ready", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePlayPauseButton() {
        if (musicServiceConnection != null) {
            if (musicServiceConnection.isPlaying()) {
                playPauseButton.setImageResource(R.drawable.ic_pause);
                startSeekBarUpdate();
            } else {
                playPauseButton.setImageResource(R.drawable.ic_play);
                stopSeekBarUpdate();
            }
        } else {
            // If service is not connected, show play icon
            playPauseButton.setImageResource(R.drawable.ic_play);
            stopSeekBarUpdate();
        }
    }

    // Player state
    private boolean isShuffleEnabled = false;
    private boolean isRepeatEnabled = false;

    // Control methods for the new buttons
    private void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        updateShuffleButton();

        // Update PlaylistManager immediately
        com.example.beat.service.PlaylistManager playlistManager =
            com.example.beat.service.PlaylistManager.getInstance();
        playlistManager.setShuffleEnabled(isShuffleEnabled);

        android.util.Log.d("PlayerActivity", "Shuffle toggled: " + isShuffleEnabled);
        android.widget.Toast.makeText(this,
            isShuffleEnabled ? "Shuffle ON" : "Shuffle OFF",
            android.widget.Toast.LENGTH_SHORT).show();
    }

    private void playPrevious() {
        android.util.Log.d("PlayerActivity", "playPrevious called - using service command");
        try {
            // Use service command to trigger previous track (same as mini player)
            android.content.Intent intent = new android.content.Intent(this, com.example.beat.service.MusicService.class);
            intent.setAction("PREVIOUS");
            startService(intent);
            android.util.Log.d("PlayerActivity", "Previous track service command sent");
        } catch (Exception e) {
            android.util.Log.e("PlayerActivity", "Error sending previous track command", e);
            // Fallback: restart current song
            if (musicServiceConnection != null) {
                musicServiceConnection.seekTo(0);
            }
        }
    }

    private void playNext() {
        android.util.Log.d("PlayerActivity", "playNext called - using service command");
        try {
            // Use service command to trigger next track (same as mini player)
            android.content.Intent intent = new android.content.Intent(this, com.example.beat.service.MusicService.class);
            intent.setAction("NEXT");
            startService(intent);
            android.util.Log.d("PlayerActivity", "Next track service command sent");
        } catch (Exception e) {
            android.util.Log.e("PlayerActivity", "Error sending next track command", e);
            // Fallback: restart current song
            if (musicServiceConnection != null) {
                musicServiceConnection.seekTo(0);
            }
        }
    }

    // Setup playlist in PlaylistManager for service to use
    private void setupPlaylistManager() {
        if (songList != null && !songList.isEmpty()) {
            com.example.beat.service.PlaylistManager playlistManager =
                com.example.beat.service.PlaylistManager.getInstance();

            playlistManager.setPlaylist(songList, titleList, artistList, albumArtList, currentPosition);
            playlistManager.setShuffleEnabled(isShuffleEnabled);
            playlistManager.setRepeatEnabled(isRepeatEnabled);

            android.util.Log.d("PlayerActivity", "Playlist setup in PlaylistManager: " +
                songList.size() + " songs, position: " + currentPosition);
        }
    }

    // Sync PlaylistManager position when returning from mini player
    private void syncPlaylistManagerPosition() {
        if (songList != null && !songList.isEmpty() && streamUrl != null) {
            // Find current song position in the playlist
            int foundPosition = -1;
            for (int i = 0; i < songList.size(); i++) {
                if (streamUrl.equals(songList.get(i))) {
                    foundPosition = i;
                    break;
                }
            }

            if (foundPosition != -1) {
                currentPosition = foundPosition;
                com.example.beat.service.PlaylistManager playlistManager =
                    com.example.beat.service.PlaylistManager.getInstance();

                // Update the position in PlaylistManager
                playlistManager.setCurrentPosition(foundPosition);

                android.util.Log.d("PlayerActivity", "Synced PlaylistManager position to: " + foundPosition +
                    " for song: " + trackTitle);
            } else {
                android.util.Log.w("PlayerActivity", "Could not find current song in playlist: " + streamUrl);
            }
        }
    }

    private void loadCurrentSong() {
        if (songList != null && currentPosition >= 0 && currentPosition < songList.size()) {
            // Update current track info
            streamUrl = songList.get(currentPosition);
            trackTitle = titleList != null && currentPosition < titleList.size() ?
                        titleList.get(currentPosition) : "Unknown Track";
            artistName = artistList != null && currentPosition < artistList.size() ?
                        artistList.get(currentPosition) : "Unknown Artist";
            albumArtUrl = albumArtList != null && currentPosition < albumArtList.size() ?
                         albumArtList.get(currentPosition) : null;

            android.util.Log.d("PlayerActivity", "Loading song at position " + currentPosition +
                ": " + trackTitle + " - " + streamUrl);

            // Update UI
            trackTitleTextView.setText(trackTitle);
            artistNameTextView.setText(artistName);

            // Load album art
            if (albumArtUrl != null && !albumArtUrl.isEmpty()) {
                try {
                    com.bumptech.glide.Glide.with(this)
                            .load(albumArtUrl)
                            .placeholder(com.example.beat.R.drawable.default_album_art)
                            .error(com.example.beat.R.drawable.default_album_art)
                            .into(albumArtImageView);
                } catch (Exception e) {
                    albumArtImageView.setImageResource(com.example.beat.R.drawable.default_album_art);
                }
            } else {
                albumArtImageView.setImageResource(com.example.beat.R.drawable.default_album_art);
            }

            // Play the new song
            if (musicServiceConnection != null && streamUrl != null) {
                try {
                    // Check if file exists for local files
                    if (!streamUrl.startsWith("http")) {
                        java.io.File file = new java.io.File(streamUrl);
                        if (!file.exists()) {
                            android.util.Log.e("PlayerActivity", "File does not exist: " + streamUrl);
                            android.widget.Toast.makeText(this, "File not found: " + trackTitle, android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    musicServiceConnection.playMusic(streamUrl, trackTitle, artistName, albumArtUrl);

                    // Update mini player with new track info
                    updateMiniPlayer();

                    // Update button state after a short delay to allow service to start
                    handler.postDelayed(() -> updatePlayPauseButton(), 500);
                } catch (Exception e) {
                    android.util.Log.e("PlayerActivity", "Error playing song", e);
                    android.widget.Toast.makeText(this, "Error playing: " + trackTitle, android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                android.util.Log.e("PlayerActivity", "Cannot play song - service not ready or streamUrl is null");
            }
        }
    }

    private void toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled;
        updateRepeatButton();

        // Update PlaylistManager immediately
        com.example.beat.service.PlaylistManager playlistManager =
            com.example.beat.service.PlaylistManager.getInstance();
        playlistManager.setRepeatEnabled(isRepeatEnabled);

        android.util.Log.d("PlayerActivity", "Repeat toggled: " + isRepeatEnabled);
        android.widget.Toast.makeText(this,
            isRepeatEnabled ? "Repeat ON" : "Repeat OFF",
            android.widget.Toast.LENGTH_SHORT).show();
    }

    private void updateShuffleButton() {
        if (shuffleButton != null) {
            shuffleButton.setAlpha(isShuffleEnabled ? 1.0f : 0.5f);
        }
    }

    private void updateRepeatButton() {
        if (repeatButton != null) {
            repeatButton.setAlpha(isRepeatEnabled ? 1.0f : 0.5f);
        }
    }

    private void showAddToPlaylistDialog() {
        if (streamUrl == null) return;

        // Get all playlists for the current user
        new Thread(() -> {
            try {
                com.example.beat.data.database.AppDatabase db = com.example.beat.data.database.AppDatabase.getInstance(this);
                com.example.beat.data.dao.PlaylistDao playlistDao = db.playlistDao();
                java.util.List<com.example.beat.data.entities.Playlist> playlists = playlistDao.getPlaylistsByUser(1); // Using userId = 1 for now

                // Create dialog on UI thread
                runOnUiThread(() -> {
                    if (playlists == null || playlists.isEmpty()) {
                        showCreatePlaylistDialog();
                        return;
                    }

                    // Create dialog with playlist options
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("Add to Playlist");

                    String[] playlistNames = new String[playlists.size()];
                    for (int i = 0; i < playlists.size(); i++) {
                        playlistNames[i] = playlists.get(i).getName();
                    }

                    builder.setItems(playlistNames, (dialog, which) -> {
                        com.example.beat.data.entities.Playlist selectedPlaylist = playlists.get(which);
                        addToPlaylist(selectedPlaylist);
                    });

                    builder.setPositiveButton("Create New", (dialog, which) -> showCreatePlaylistDialog());
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Error loading playlists", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showCreatePlaylistDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Create New Playlist");

        // Set up input
        final android.widget.EditText input = new android.widget.EditText(this);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (playlistName.isEmpty()) {
                android.widget.Toast.makeText(this, "Please enter a playlist name", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // Create new playlist
            new Thread(() -> {
                try {
                    com.example.beat.data.database.AppDatabase db = com.example.beat.data.database.AppDatabase.getInstance(this);
                    com.example.beat.data.dao.PlaylistDao playlistDao = db.playlistDao();

                    com.example.beat.data.entities.Playlist newPlaylist = new com.example.beat.data.entities.Playlist();
                    newPlaylist.setName(playlistName);
                    newPlaylist.setUserId(1); // Using userId = 1 for now

                    long playlistId = playlistDao.insert(newPlaylist);

                    // Find current song in database and add to playlist
                    findCurrentSongAndAddToPlaylist((int) playlistId, playlistName);

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(this, "Error creating playlist", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addToPlaylist(com.example.beat.data.entities.Playlist playlist) {
        new Thread(() -> {
            try {
                com.example.beat.data.database.AppDatabase db = com.example.beat.data.database.AppDatabase.getInstance(this);
                com.example.beat.data.dao.MusicDao musicDao = db.musicDao();

                // Find the current song in database by file path
                com.example.beat.data.entities.LocalSong currentSong = musicDao.getSongByFilePath(streamUrl);

                if (currentSong != null) {
                    com.example.beat.data.dao.PlaylistDao playlistDao = db.playlistDao();

                    com.example.beat.data.entities.PlaylistSong playlistSong = new com.example.beat.data.entities.PlaylistSong();
                    playlistSong.setPlaylistId(playlist.getPlaylistId());
                    playlistSong.setSongId(currentSong.getSongId());

                    playlistDao.insertPlaylistSong(playlistSong);

                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(this, "Added to playlist: " + playlist.getName(), android.widget.Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(this, "Song not found in database", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Error adding to playlist", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void findCurrentSongAndAddToPlaylist(int playlistId, String playlistName) {
        try {
            com.example.beat.data.database.AppDatabase db = com.example.beat.data.database.AppDatabase.getInstance(this);
            com.example.beat.data.dao.MusicDao musicDao = db.musicDao();

            // Find the current song in database by file path
            com.example.beat.data.entities.LocalSong currentSong = musicDao.getSongByFilePath(streamUrl);

            if (currentSong != null) {
                com.example.beat.data.dao.PlaylistDao playlistDao = db.playlistDao();

                com.example.beat.data.entities.PlaylistSong playlistSong = new com.example.beat.data.entities.PlaylistSong();
                playlistSong.setPlaylistId(playlistId);
                playlistSong.setSongId(currentSong.getSongId());

                playlistDao.insertPlaylistSong(playlistSong);

                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Added to new playlist: " + playlistName, android.widget.Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Song not found in database", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, "Error adding to playlist", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }



    private void startSeekBarUpdate() {
        handler.removeCallbacks(updateSeekBarRunnable);
        handler.post(updateSeekBarRunnable);
    }

    private void stopSeekBarUpdate() {
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isServiceReady) {
            // Add a small delay to ensure service state is accurate
            handler.postDelayed(() -> {
                updatePlayPauseButton();
                if (musicServiceConnection != null && musicServiceConnection.isPlaying()) {
                    startSeekBarUpdate();
                }
            }, 100); // Small delay to ensure service state is current
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSeekBarUpdate();
    }

    private void updateTrackInfo() {
        // Update UI with track info
        if (trackTitleTextView != null) {
            trackTitleTextView.setText(trackTitle != null ? trackTitle : "Unknown Track");
        }
        if (artistNameTextView != null) {
            artistNameTextView.setText(artistName != null ? artistName : "Unknown Artist");
        }

        // Load album art
        if (albumArtUrl != null && !albumArtUrl.isEmpty()) {
            try {
                com.bumptech.glide.Glide.with(this)
                    .load(albumArtUrl)
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .into(albumArtImageView);
            } catch (Exception e) {
                android.util.Log.e("PlayerActivity", "Error loading album art", e);
                albumArtImageView.setImageResource(R.drawable.default_album_art);
            }
        } else {
            albumArtImageView.setImageResource(R.drawable.default_album_art);
        }
    }

    private void updateMiniPlayer() {
        try {
            if (trackTitle != null && artistName != null) {
                android.util.Log.d("PlayerActivity", "Updating mini player with: " + trackTitle);

                // Update MiniPlayerManager directly
                com.example.beat.ui.MiniPlayerManager.getInstance(this)
                    .updateTrackInfo(streamUrl, trackTitle, artistName, albumArtUrl);

                // Also trigger MainActivity to show mini player
                com.example.beat.MainActivity.showMiniPlayerStatic(trackTitle, artistName, albumArtUrl);

                android.util.Log.d("PlayerActivity", "Mini player updated and MainActivity notified");
            }
        } catch (Exception e) {
            android.util.Log.e("PlayerActivity", "Error updating mini player", e);
        }
    }

    @Override
    public void onBackPressed() {
        // Update mini player with current track info before going back
        updateMiniPlayer();

        // Just finish the activity - MainActivity will handle showing mini player
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSeekBarUpdate();



        // Don't unbind service on destroy to keep music playing
        // Only unbind when user explicitly stops music
    }

    // TrackChangeListener implementation
    @Override
    public void onTrackChanged(String streamUrl, String title, String artist, String albumArt) {
        android.util.Log.d("PlayerActivity", "Track changed notification received: " + title);

        // Update current track info
        this.streamUrl = streamUrl;
        this.trackTitle = title;
        this.artistName = artist;
        this.albumArtUrl = albumArt;

        // Update UI on main thread
        runOnUiThread(() -> {
            updateTrackInfo();
            updateMiniPlayer();
            android.util.Log.d("PlayerActivity", "UI updated for track change: " + title);
        });
    }
}
