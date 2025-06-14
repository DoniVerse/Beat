package com.example.beat.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.beat.R;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.data.entities.Playlist;
import com.example.beat.data.dao.PlaylistDao;
import com.example.beat.data.entities.PlaylistSong;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LocalMusicPlayerActivity extends AppCompatActivity {
    private TextView songTitle;
    private ImageView albumArtImageView;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private SeekBar seekBar;
    private ImageButton playPauseBtn, prevBtn, nextBtn, shuffleBtn, repeatBtn, addToPlaylistBtn;

    private LocalSong currentSong;
    private List<LocalSong> songList;
    private int currentPosition = 0;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                currentTimeTextView.setText(formatTime(mediaPlayer.getCurrentPosition()));
                handler.postDelayed(this, 1000);
            }
        }
    };

    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_local);

        // Get user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        songTitle = findViewById(R.id.song_title);
        albumArtImageView = findViewById(R.id.album_art);
        currentTimeTextView = findViewById(R.id.current_time);
        totalTimeTextView = findViewById(R.id.total_time);
        seekBar = findViewById(R.id.seekBar);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        prevBtn = findViewById(R.id.prev_btn);
        nextBtn = findViewById(R.id.next_btn);
        shuffleBtn = findViewById(R.id.shuffle_btn);

        addToPlaylistBtn = findViewById(R.id.add_to_playlist_btn);

        Intent intent = getIntent();
        if (intent != null) {
            ArrayList<LocalSong> songs = intent.getParcelableArrayListExtra("SONG_LIST");
            if (songs != null && !songs.isEmpty()) {
                int position = intent.getIntExtra("POSITION", 0);
                currentSong = songs.get(position);
                songList = songs;
                currentPosition = position;
                updateSongInfo();
                setupPlayer();
            }
        }

        prevBtn.setOnClickListener(v -> playPrevious());
        nextBtn.setOnClickListener(v -> playNext());
        playPauseBtn.setOnClickListener(v -> togglePlayPause());
        shuffleBtn.setOnClickListener(v -> toggleShuffle());

        
        addToPlaylistBtn.setOnClickListener(v -> showAddToPlaylistDialog());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.post(updateSeekBarRunnable);
            }
        });
    }

    private void updateSongInfo() {
        if (currentSong != null) {
            songTitle.setText(currentSong.getTitle());

            // Load album art using Glide
            String albumArtUri = currentSong.getAlbumArtUri();
            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                try {
                    Glide.with(this)
                            .load(albumArtUri)
                            .placeholder(R.drawable.default_album_art)
                            .error(R.drawable.default_album_art)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(albumArtImageView);
                } catch (Exception e) {
                    e.printStackTrace();
                    albumArtImageView.setImageResource(R.drawable.default_album_art);
                }
            } else {
                albumArtImageView.setImageResource(R.drawable.default_album_art);
            }

            if (mediaPlayer != null) {
                seekBar.setMax(mediaPlayer.getDuration());
                totalTimeTextView.setText(formatTime(mediaPlayer.getDuration()));
                currentTimeTextView.setText("0:00");
            }
        }
    }

    private void setupPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        if (currentSong != null) {
            String filePath = currentSong.getFilePath();

            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, Uri.parse(filePath));
                mediaPlayer.prepare();
                mediaPlayer.start();
                isPlaying = true;
                playPauseBtn.setImageResource(R.drawable.ic_pause);
                updateSongInfo();
                seekBar.setProgress(0);
                handler.post(updateSeekBarRunnable);

                mediaPlayer.setOnCompletionListener(mp -> {
                    if (isRepeat) {
                        setupPlayer();
                    } else {
                        playNext();
                    }
                });

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer != null) {
                            mediaPlayer.seekTo(progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        handler.removeCallbacks(updateSeekBarRunnable);
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        handler.post(updateSeekBarRunnable);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                songTitle.setText("Error playing song");
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                playPauseBtn.setImageResource(R.drawable.ic_play);
            } else {
                mediaPlayer.start();
                isPlaying = true;
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            }
        } else {
            // If no song is playing, start the current song
            setupPlayer();
        }
    }

    private void playPrevious() {
        if (songList != null && !songList.isEmpty()) {
            currentPosition = (currentPosition - 1 + songList.size()) % songList.size();
            currentSong = songList.get(currentPosition);
            setupPlayer();
        }
    }

    private void playNext() {
        if (songList != null && !songList.isEmpty()) {
            if (isShuffle) {
                currentPosition = new Random().nextInt(songList.size());
            } else {
                currentPosition = (currentPosition + 1) % songList.size();
            }
            currentSong = songList.get(currentPosition);
            setupPlayer();
        }
    }

    private void toggleShuffle() {
        isShuffle = !isShuffle;
        shuffleBtn.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle);
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

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }
}
