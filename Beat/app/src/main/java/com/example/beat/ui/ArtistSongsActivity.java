package com.example.beat.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beat.R;
import com.example.beat.adapter.SongAdapter;
import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.Artist;
import com.example.beat.data.entities.LocalSong;
import java.util.List;

public class ArtistSongsActivity extends AppCompatActivity {
    private MusicDao musicDao;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private Artist artist;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_songs);

        // Get artist from intent
        artist = getIntent().getParcelableExtra("artist");
        userId = getIntent().getIntExtra("userId", -1);
        
        if (artist == null || userId == -1) {
            finish();
            return;
        }

        // Initialize Room database
        musicDao = AppDatabase.getInstance(this).musicDao();

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Show loading indicator
        ProgressBar loadingProgress = findViewById(R.id.loading_progress);
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }

        // Move database operations to background thread
        new Thread(() -> {
            try {
                List<LocalSong> songs = musicDao.getSongsByArtistAndUser(artist.artistId, userId);
                runOnUiThread(() -> {
                    if (songs != null && !songs.isEmpty()) {
                        adapter = new SongAdapter(songs);
                        recyclerView.setAdapter(adapter);
                        loadingProgress.setVisibility(View.GONE);
                    } else {
                        // Show message if no songs found
                        TextView noSongs = findViewById(R.id.no_songs_text);
                        if (noSongs != null) {
                            noSongs.setVisibility(View.VISIBLE);
                        }
                        loadingProgress.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // Show error message
                    TextView errorText = findViewById(R.id.error_text);
                    if (errorText != null) {
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText("Error loading songs: " + e.getMessage());
                    }
                    loadingProgress.setVisibility(View.GONE);
                });
            }
        }).start();

        // Set artist name in title
        TextView artistName = findViewById(R.id.artist_name);
        artistName.setText(artist.name);
    }
}
