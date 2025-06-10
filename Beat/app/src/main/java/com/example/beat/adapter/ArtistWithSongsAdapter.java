package com.example.beat.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.MainActivity;
import com.example.beat.R;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.Artist;
import com.example.beat.data.entities.ArtistWithSongs;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.ui.ArtistSongsFragment;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class ArtistWithSongsAdapter extends RecyclerView.Adapter<ArtistWithSongsAdapter.ArtistViewHolder> {
    private List<ArtistWithSongs> artists;
    private int userId;

    public ArtistWithSongsAdapter() {
        this.artists = new ArrayList<>();
    }

    public ArtistWithSongsAdapter(List<ArtistWithSongs> artists) {
        this.artists = artists;
    }

    public void updateArtists(List<ArtistWithSongs> artists) {
        this.artists = artists;
        notifyDataSetChanged();
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        ArtistWithSongs artist = artists.get(position);
        holder.bind(artist, userId);
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    class ArtistViewHolder extends RecyclerView.ViewHolder {
        private final TextView artistName;
        private final TextView songCount;
        private final ShapeableImageView artistArt;
        private final ArtistWithSongsAdapter adapter;

        ArtistViewHolder(@NonNull View itemView, ArtistWithSongsAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            artistName = itemView.findViewById(R.id.artist_name);
            songCount = itemView.findViewById(R.id.song_count);
            artistArt = itemView.findViewById(R.id.artist_art);
            ImageButton btnOptions = itemView.findViewById(R.id.btn_options);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ArtistWithSongs artist = adapter.artists.get(position);
                    ArtistSongsFragment fragment = ArtistSongsFragment.newInstance(artist);
                    ((MainActivity) itemView.getContext()).navigateToFragment(fragment);
                }
            });

            // Set up option menu click listener
            btnOptions.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showOptionsMenu(v, adapter.artists.get(position), position);
                }
            });
        }

        private void showOptionsMenu(View view, ArtistWithSongs artist, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.artist_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete_artist) {
                    // Show confirmation and actually delete the artist
                    showActualDeleteConfirmation(artist, position, view);
                    return true;
                } else if (item.getItemId() == R.id.action_add_artist_to_playlist) {
                    // Show playlist selection dialog for all artist songs
                    showArtistPlaylistSelectionDialog(artist, view);
                    return true;
                }
                return false;
            });

            popup.show();
        }

        private void showActualDeleteConfirmation(ArtistWithSongs artist, int position, View view) {
            new AlertDialog.Builder(view.getContext())
                .setTitle("Delete Artist")
                .setMessage("Are you sure you want to delete artist \"" + artist.artist.name + "\" and all their " + artist.songs.size() + " songs? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Actually delete the artist and all their songs from database
                    deleteArtistFromDatabase(artist, position, view);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        private void deleteArtistFromDatabase(ArtistWithSongs artist, int position, View view) {
            new Thread(() -> {
                try {
                    // Get database instance
                    com.example.beat.data.database.AppDatabase db =
                        com.example.beat.data.database.AppDatabase.getInstance(view.getContext());

                    // Get user ID from SharedPreferences
                    android.content.SharedPreferences prefs = view.getContext()
                        .getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                    int userId = prefs.getInt("userId", -1);

                    if (userId == -1) {
                        ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                            new AlertDialog.Builder(view.getContext())
                                .setTitle("Error")
                                .setMessage("User not logged in")
                                .setPositiveButton("OK", null)
                                .show();
                        });
                        return;
                    }

                    // Delete all songs by this artist
                    db.musicDao().deleteArtistSongs(artist.artist.artistId, userId);

                    // Delete the artist itself
                    db.musicDao().deleteArtist(artist.artist);

                    // Update UI on main thread
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        // Remove from adapter list
                        adapter.artists.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, adapter.artists.size());

                        // Show success message
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Success")
                            .setMessage("Artist \"" + artist.artist.name + "\" and all their songs have been deleted successfully.")
                            .setPositiveButton("OK", null)
                            .show();
                    });
                } catch (Exception e) {
                    // Show error message on main thread
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Error")
                            .setMessage("Failed to delete artist: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            }).start();
        }

        private void showArtistPlaylistSelectionDialog(ArtistWithSongs artist, View view) {
            if (artist.songs == null || artist.songs.isEmpty()) {
                new AlertDialog.Builder(view.getContext())
                    .setTitle("No Songs")
                    .setMessage("This artist has no songs to add to playlist.")
                    .setPositiveButton("OK", null)
                    .show();
                return;
            }

            new Thread(() -> {
                try {
                    // Get database instance
                    com.example.beat.data.database.AppDatabase db =
                        com.example.beat.data.database.AppDatabase.getInstance(view.getContext());

                    // Get user ID from SharedPreferences
                    android.content.SharedPreferences prefs = view.getContext()
                        .getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                    int userId = prefs.getInt("userId", -1);

                    if (userId == -1) {
                        ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                            new AlertDialog.Builder(view.getContext())
                                .setTitle("Error")
                                .setMessage("User not logged in")
                                .setPositiveButton("OK", null)
                                .show();
                        });
                        return;
                    }

                    // Get user's playlists
                    java.util.List<com.example.beat.data.entities.Playlist> playlists =
                        db.playlistDao().getPlaylistsByUser(userId);

                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        if (playlists.isEmpty()) {
                            // No playlists exist, offer to create one
                            showCreatePlaylistForArtistDialog(artist, view, db, userId);
                        } else {
                            // Show playlist selection dialog
                            showExistingPlaylistsForArtistDialog(artist, view, db, playlists);
                        }
                    });
                } catch (Exception e) {
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Error")
                            .setMessage("Failed to load playlists: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            }).start();
        }

        private void showCreatePlaylistForArtistDialog(ArtistWithSongs artist, View view,
                com.example.beat.data.database.AppDatabase db, int userId) {
            android.widget.EditText editText = new android.widget.EditText(view.getContext());
            editText.setHint("Enter playlist name");

            new AlertDialog.Builder(view.getContext())
                .setTitle("Create New Playlist")
                .setMessage("You don't have any playlists yet. Create one to add all " + artist.songs.size() + " songs from this artist.")
                .setView(editText)
                .setPositiveButton("Create", (dialog, which) -> {
                    String playlistName = editText.getText().toString().trim();
                    if (!playlistName.isEmpty()) {
                        createPlaylistAndAddArtistSongs(artist, playlistName, view, db, userId);
                    } else {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Error")
                            .setMessage("Please enter a playlist name")
                            .setPositiveButton("OK", null)
                            .show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        private void showExistingPlaylistsForArtistDialog(ArtistWithSongs artist, View view,
                com.example.beat.data.database.AppDatabase db,
                java.util.List<com.example.beat.data.entities.Playlist> playlists) {

            String[] playlistNames = new String[playlists.size() + 1];
            for (int i = 0; i < playlists.size(); i++) {
                playlistNames[i] = playlists.get(i).getName();
            }
            playlistNames[playlists.size()] = "Create New Playlist...";

            new AlertDialog.Builder(view.getContext())
                .setTitle("Add Artist to Playlist")
                .setMessage("Add all " + artist.songs.size() + " songs from \"" + artist.artist.name + "\" to:")
                .setItems(playlistNames, (dialog, which) -> {
                    if (which == playlists.size()) {
                        // Create new playlist option selected
                        android.widget.EditText editText = new android.widget.EditText(view.getContext());
                        editText.setHint("Enter playlist name");

                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Create New Playlist")
                            .setView(editText)
                            .setPositiveButton("Create", (d, w) -> {
                                String playlistName = editText.getText().toString().trim();
                                if (!playlistName.isEmpty()) {
                                    // Get user ID from SharedPreferences
                                    android.content.SharedPreferences prefs = view.getContext()
                                        .getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                                    int userId = prefs.getInt("userId", -1);
                                    createPlaylistAndAddArtistSongs(artist, playlistName, view, db, userId);
                                } else {
                                    new AlertDialog.Builder(view.getContext())
                                        .setTitle("Error")
                                        .setMessage("Please enter a playlist name")
                                        .setPositiveButton("OK", null)
                                        .show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    } else {
                        // Existing playlist selected
                        com.example.beat.data.entities.Playlist selectedPlaylist = playlists.get(which);
                        addArtistSongsToExistingPlaylist(artist, selectedPlaylist, view, db);
                    }
                })
                .show();
        }

        private void createPlaylistAndAddArtistSongs(ArtistWithSongs artist, String playlistName, View view,
                com.example.beat.data.database.AppDatabase db, int userId) {
            new Thread(() -> {
                try {
                    // Create new playlist
                    com.example.beat.data.entities.Playlist newPlaylist = new com.example.beat.data.entities.Playlist();
                    newPlaylist.setName(playlistName);
                    newPlaylist.setUserId(userId);

                    long playlistId = db.playlistDao().insert(newPlaylist);

                    // Add all artist songs to playlist
                    int addedCount = 0;
                    for (com.example.beat.data.entities.LocalSong song : artist.songs) {
                        com.example.beat.data.entities.PlaylistSong playlistSong = new com.example.beat.data.entities.PlaylistSong();
                        playlistSong.setPlaylistId((int) playlistId);
                        playlistSong.setSongId(song.getSongId());

                        db.playlistDao().insertPlaylistSong(playlistSong);
                        addedCount++;
                    }

                    // Show success message
                    final int finalAddedCount = addedCount;
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Success")
                            .setMessage("Created playlist \"" + playlistName + "\" and added " + finalAddedCount + " songs from artist \"" + artist.artist.name + "\"")
                            .setPositiveButton("OK", null)
                            .show();
                    });
                } catch (Exception e) {
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Error")
                            .setMessage("Failed to create playlist: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            }).start();
        }

        private void addArtistSongsToExistingPlaylist(ArtistWithSongs artist,
                com.example.beat.data.entities.Playlist playlist, View view,
                com.example.beat.data.database.AppDatabase db) {
            new Thread(() -> {
                try {
                    // Get existing songs in playlist
                    java.util.List<com.example.beat.data.entities.PlaylistSong> existingSongs =
                        db.playlistDao().getPlaylistSongs(playlist.getPlaylistId());

                    // Create set of existing song IDs for quick lookup
                    java.util.Set<Integer> existingSongIds = new java.util.HashSet<>();
                    for (com.example.beat.data.entities.PlaylistSong ps : existingSongs) {
                        existingSongIds.add(ps.getSongId());
                    }

                    // Add songs that aren't already in playlist
                    int addedCount = 0;
                    int skippedCount = 0;
                    for (com.example.beat.data.entities.LocalSong song : artist.songs) {
                        if (!existingSongIds.contains(song.getSongId())) {
                            com.example.beat.data.entities.PlaylistSong playlistSong = new com.example.beat.data.entities.PlaylistSong();
                            playlistSong.setPlaylistId(playlist.getPlaylistId());
                            playlistSong.setSongId(song.getSongId());

                            db.playlistDao().insertPlaylistSong(playlistSong);
                            addedCount++;
                        } else {
                            skippedCount++;
                        }
                    }

                    // Show success message
                    final int finalAddedCount = addedCount;
                    final int finalSkippedCount = skippedCount;
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        String message;
                        if (finalSkippedCount == 0) {
                            message = "Added all " + finalAddedCount + " songs from artist \"" + artist.artist.name + "\" to playlist \"" + playlist.getName() + "\"";
                        } else if (finalAddedCount == 0) {
                            message = "All " + finalSkippedCount + " songs from artist \"" + artist.artist.name + "\" are already in playlist \"" + playlist.getName() + "\"";
                        } else {
                            message = "Added " + finalAddedCount + " new songs from artist \"" + artist.artist.name + "\" to playlist \"" + playlist.getName() + "\". " + finalSkippedCount + " songs were already in the playlist.";
                        }

                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Success")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();
                    });
                } catch (Exception e) {
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Error")
                            .setMessage("Failed to add artist songs to playlist: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            }).start();
        }

        void bind(ArtistWithSongs artist, int userId) {
            artistName.setText(artist.artist.name);
            songCount.setText(String.format("%d songs", artist.songs.size()));

            // Load album art from first song of this artist, or use default
            loadArtistAlbumArt(artist);
        }

        private void loadArtistAlbumArt(ArtistWithSongs artist) {
            if (artist == null || artist.songs == null || artist.songs.isEmpty()) {
                artistArt.setImageResource(R.drawable.default_album_art);
                return;
            }

            // Get album art from first song with album art
            String albumArtUri = null;
            for (LocalSong song : artist.songs) {
                if (song.getAlbumArtUri() != null && !song.getAlbumArtUri().isEmpty()) {
                    albumArtUri = song.getAlbumArtUri();
                    break;
                }
            }

            // Load album art using Glide
            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(albumArtUri)
                        .placeholder(R.drawable.default_album_art)
                        .error(R.drawable.default_album_art)
                        .into(artistArt);
            } else {
                artistArt.setImageResource(R.drawable.default_album_art);
            }
        }
    }
}
