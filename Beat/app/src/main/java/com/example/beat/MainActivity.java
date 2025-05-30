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
import com.example.beat.ui.HomeFragment;
import com.example.beat.ui.LocalSongsFragment;
import com.example.beat.ui.PlaylistFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.EdgeToEdge;

public class MainActivity extends AppCompatActivity {
    private int userId;  // class-level field

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
