package com.example.beat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.beat.data.database.AppDatabase;
import com.example.beat.permissions.PermissionManager;
import com.example.beat.scanner.MediaStoreScanner;
import com.example.beat.services.MusicService;
import com.example.beat.ui.AlbumFragment;
import com.example.beat.ui.ArtistFragment;
import com.example.beat.ui.HomeFragment;
import com.example.beat.ui.LocalMusicPlayerActivity;
import com.example.beat.ui.LocalSongsFragment;
import com.example.beat.ui.PlaylistFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.EdgeToEdge;
import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {
    private int userId;  // class-level field
    private View miniPlayer;
    private ImageView miniAlbumArt;
    private TextView miniSongTitle;
    private TextView miniArtistName;
    private ImageButton miniPlayPauseButton;
    private ImageButton miniPrevButton;
    private ImageButton miniNextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Assign to field, NOT local variable
        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getInt("userId", -1);

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.navigation_local) {
                fragment = new LocalSongsFragment();
            } else if (itemId == R.id.navigation_playlist) {
                fragment = new PlaylistFragment();
            } else if (itemId == R.id.navigation_album) {
                fragment = new AlbumFragment();
            } else if (itemId == R.id.navigation_artist) {
                fragment = new ArtistFragment();
            }

            if (fragment != null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
            return true;
        });

        // Set initial fragment to HomeFragment
        navigateToFragment(new HomeFragment());
        
        // Select the home item in the bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // Request storage permissions and initialize media scanning
        if (PermissionManager.hasStoragePermissions(this)) {
            new Thread(() -> {
                AppDatabase database = AppDatabase.getInstance(this);
                MediaStoreScanner scanner = new MediaStoreScanner(this, database.musicDao(), userId);
                scanner.scanMusicFiles();
                
                // Update UI on main thread after scanning is complete
                runOnUiThread(() -> {
                    // Refresh the current fragment to show updated songs
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof LocalSongsFragment) {
                        ((LocalSongsFragment) currentFragment).refreshSongs();
                    }
                });
            }).start();
        } else {
            PermissionManager.requestStoragePermissions(this);
        }

        // Initialize mini player views
        miniPlayer = findViewById(R.id.mini_player_container);
        miniAlbumArt = findViewById(R.id.mini_album_art);
        miniSongTitle = findViewById(R.id.mini_song_title);
        miniArtistName = findViewById(R.id.mini_artist_name);
        miniPlayPauseButton = findViewById(R.id.mini_play_pause_button);
        miniPrevButton = findViewById(R.id.mini_prev_button);
        miniNextButton = findViewById(R.id.mini_next_button);

        // Register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("MINI_PLAYER_UPDATE");
        filter.addAction("PLAYBACK_STATUS");
        registerReceiver(playbackReceiver, filter);

        // Set up mini player click listeners
        miniPlayer.setOnClickListener(v -> {
            // Launch full player
            Intent intent = new Intent(this, LocalMusicPlayerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        miniPlayPauseButton.setOnClickListener(v -> {
            // Toggle play/pause
            Intent intent = new Intent(isPlaying() ? MusicService.ACTION_PAUSE : MusicService.ACTION_PLAY);
            sendBroadcast(intent);
        });

        miniPrevButton.setOnClickListener(v -> {
            Intent intent = new Intent(MusicService.ACTION_PREVIOUS);
            sendBroadcast(intent);
        });

        miniNextButton.setOnClickListener(v -> {
            Intent intent = new Intent(MusicService.ACTION_NEXT);
            sendBroadcast(intent);
        });
    }

    private BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;
            
            switch (intent.getAction()) {
                case "MINI_PLAYER_UPDATE":
                    updateMiniPlayer(intent);
                    break;
                case "PLAYBACK_STATUS":
                    boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                    updatePlayPauseButton(isPlaying);
                    break;
            }
        }
    };

    private void updatePlayPauseButton(boolean playing) {
        miniPlayPauseButton.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private boolean isPlaying() {
        return miniPlayPauseButton.getDrawable().getConstantState().equals(
            getResources().getDrawable(R.drawable.ic_pause).getConstantState()
        );
    }

    private void updateMiniPlayer(Intent intent) {
        String title = intent.getStringExtra("title");
        String artist = intent.getStringExtra("artist");
        String albumArtUri = intent.getStringExtra("albumArtUri");
        boolean show = intent.getBooleanExtra("show", false);
        boolean isPlaying = intent.getBooleanExtra("isPlaying", false);

        if (show) {
            miniPlayer.setVisibility(View.VISIBLE);
            miniSongTitle.setText(title);
            miniArtistName.setText(artist);
            updatePlayPauseButton(isPlaying);

            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                Glide.with(this)
                    .load(albumArtUri)
                    .error(R.drawable.default_artist)
                    .into(miniAlbumArt);
            } else {
                miniAlbumArt.setImageResource(R.drawable.default_artist);
            }
        } else {
            miniPlayer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(playbackReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionManager.STORAGE_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Storage permissions granted, start scanning
            new Thread(() -> {
                AppDatabase database = AppDatabase.getInstance(this);
                MediaStoreScanner scanner = new MediaStoreScanner(this, database.musicDao(), userId);
                scanner.scanMusicFiles();
                
                // Update UI on main thread after scanning is complete
                runOnUiThread(() -> {
                    // Refresh the current fragment to show updated songs
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof LocalSongsFragment) {
                        ((LocalSongsFragment) currentFragment).refreshSongs();
                    }
                });
            }).start();
        }
    }

    public void navigateToFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.STORAGE_PERMISSION_REQUEST_CODE) {
            if (PermissionManager.isPermissionGranted(permissions, grantResults)) {
                new Thread(() -> {
                    AppDatabase database = AppDatabase.getInstance(this);
                    MediaStoreScanner scanner = new MediaStoreScanner(this, database.musicDao(), userId);
                    scanner.scanMusicFiles();
                }).start();
            } else {
                // Optionally, show a message that storage permission is required
            }
        }
    }
}
