package com.example.beat;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.beat.ui.AlbumFragment;
import com.example.beat.ui.ArtistFragment;
//import com.example.beat.ui.HomeFragment;
import com.example.beat.ui.HomeFragment;
import com.example.beat.ui.LocalSongsFragment;
import com.example.beat.ui.PlaylistFragment;
import com.example.beat.ui.MiniPlayerManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.EdgeToEdge;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {
    private int userId;  // class-level field
    private FrameLayout miniPlayerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set current instance for static access
        currentInstance = this;

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
//        navigateToFragment(new HomeFragment());
        
        // Select the home item in the bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // Initialize mini player container
        miniPlayerContainer = findViewById(R.id.mini_player_container);

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

    public void showMiniPlayer() {
        if (miniPlayerContainer != null) {
            MiniPlayerManager.getInstance(this).showMiniPlayer(miniPlayerContainer);
        }
    }

    public void hideMiniPlayer() {
        if (miniPlayerContainer != null) {
            MiniPlayerManager.getInstance(this).hideMiniPlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if mini player should be shown when returning to MainActivity
        showMiniPlayerIfNeeded();
    }

    private void showMiniPlayerIfNeeded() {
        try {
            MiniPlayerManager miniPlayerManager = MiniPlayerManager.getInstance(this);
            if (miniPlayerManager.hasTrackInfo() && miniPlayerContainer != null) {
                miniPlayerManager.showMiniPlayer(miniPlayerContainer);
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    // Method to manually show mini player with track info
    public void showMiniPlayerWithTrack(String title, String artist, String albumArt) {
        try {
            android.util.Log.d("MainActivity", "showMiniPlayerWithTrack called with: " + title);

            // Ensure we have the container
            if (miniPlayerContainer == null) {
                miniPlayerContainer = findViewById(R.id.mini_player_container);
                android.util.Log.d("MainActivity", "Found container: " + miniPlayerContainer);
            }

            if (miniPlayerContainer != null) {
                // Force layout update
                miniPlayerContainer.requestLayout();

                android.util.Log.d("MainActivity", "Container dimensions before: " +
                    miniPlayerContainer.getWidth() + "x" + miniPlayerContainer.getHeight());

                MiniPlayerManager miniPlayerManager = MiniPlayerManager.getInstance(this);
                miniPlayerManager.updateTrackInfo(title, artist, albumArt);
                miniPlayerManager.showMiniPlayer(miniPlayerContainer);

                android.util.Log.d("MainActivity", "Mini player shown successfully");
            } else {
                android.util.Log.e("MainActivity", "Mini player container is null!");
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error showing mini player", e);
        }
    }

    // Static reference to current MainActivity instance
    private static MainActivity currentInstance;

    // Static method to show mini player from any activity
    public static void showMiniPlayerStatic(String title, String artist, String albumArt) {
        if (currentInstance != null) {
            currentInstance.runOnUiThread(() -> {
                // Add a small delay to ensure layout is complete
                currentInstance.findViewById(android.R.id.content).post(() -> {
                    currentInstance.showMiniPlayerWithTrack(title, artist, albumArt);
                });
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear static reference to prevent memory leaks
        if (currentInstance == this) {
            currentInstance = null;
        }
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
