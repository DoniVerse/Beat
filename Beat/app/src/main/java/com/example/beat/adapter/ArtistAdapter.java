package com.example.beat.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beat.R;
import com.example.beat.data.entities.Artist;
import com.google.android.material.imageview.ShapeableImageView;
import android.net.Uri;
import android.content.SharedPreferences;
import android.content.Context;
import android.app.Activity;
import com.bumptech.glide.Glide;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.LocalSong;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {
    private final List<Artist> artists;
    private final OnArtistClickListener listener;
    private OnArtistActionListener actionListener;

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    public interface OnArtistActionListener {
        void onDeleteArtist(Artist artist, int position);
        void onAddArtistToPlaylist(Artist artist);
    }

    public ArtistAdapter(List<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.listener = listener;
    }

    public void setOnArtistActionListener(OnArtistActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artists.get(position);
        holder.bind(artist);
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    class ArtistViewHolder extends RecyclerView.ViewHolder {
        private final TextView artistName;
        private final TextView songCount;
        private final ShapeableImageView artistArt;
        private final ImageButton btnOptions;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artist_name);
            songCount = itemView.findViewById(R.id.song_count);
            artistArt = itemView.findViewById(R.id.artist_art);
            btnOptions = itemView.findViewById(R.id.btn_options);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onArtistClick(artists.get(position));
                }
            });

            // Set up option menu click listener
            btnOptions.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showOptionsMenu(v, artists.get(position), position);
                }
            });
        }

        private void showOptionsMenu(View view, Artist artist, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.artist_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete_artist) {
                    if (actionListener != null) {
                        showDeleteConfirmation(artist, position);
                    } else {
                        // Show confirmation and actually delete the artist
                        showActualDeleteConfirmation(artist, position, view);
                    }
                    return true;
                } else if (item.getItemId() == R.id.action_add_artist_to_playlist) {
                    if (actionListener != null) {
                        actionListener.onAddArtistToPlaylist(artist);
                    } else {
                        // Show playlist selection dialog for all artist songs
                        showArtistPlaylistSelectionDialog(artist, view);
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        }

        private void showDeleteConfirmation(Artist artist, int position) {
            // Delete directly without confirmation
            if (actionListener != null) {
                actionListener.onDeleteArtist(artist, position);
            }
        }

        private void showActualDeleteConfirmation(Artist artist, int position, View view) {
            // Delete directly without confirmation
            deleteArtistFromDatabase(artist, position, view);
        }

        private void deleteArtistFromDatabase(Artist artist, int position, View view) {
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
                    db.musicDao().deleteArtistSongs(artist.artistId, userId);

                    // Delete the artist itself
                    db.musicDao().deleteArtist(artist);

                    // Update UI on main thread
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        // Remove from adapter list
                        artists.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, artists.size());
                    });
                } catch (Exception e) {
                    // Silent error handling - just log the error
                    android.util.Log.e("ArtistAdapter", "Error deleting artist: " + e.getMessage());
                }
            }).start();
        }

        private void showArtistPlaylistSelectionDialog(Artist artist, View view) {
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

                    // Get artist's songs
                    java.util.List<com.example.beat.data.entities.LocalSong> artistSongs =
                        db.musicDao().getSongsByArtistAndUser(artist.artistId, userId);

                    if (artistSongs == null || artistSongs.isEmpty()) {
                        ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                            new AlertDialog.Builder(view.getContext())
                                .setTitle("No Songs")
                                .setMessage("This artist has no songs to add to playlist.")
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
                            showCreatePlaylistForArtistDialog(artist, artistSongs, view, db, userId);
                        } else {
                            // Show playlist selection dialog
                            showExistingPlaylistsForArtistDialog(artist, artistSongs, view, db, playlists);
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

        private void showCreatePlaylistForArtistDialog(Artist artist,
                java.util.List<com.example.beat.data.entities.LocalSong> artistSongs, View view,
                com.example.beat.data.database.AppDatabase db, int userId) {
            android.widget.EditText editText = new android.widget.EditText(view.getContext());
            editText.setHint("Enter playlist name");

            new AlertDialog.Builder(view.getContext())
                .setTitle("Create New Playlist")
                .setMessage("You don't have any playlists yet. Create one to add all " + artistSongs.size() + " songs from this artist.")
                .setView(editText)
                .setPositiveButton("Create", (dialog, which) -> {
                    String playlistName = editText.getText().toString().trim();
                    if (!playlistName.isEmpty()) {
                        createPlaylistAndAddArtistSongs(artist, artistSongs, playlistName, view, db, userId);
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

        private void showExistingPlaylistsForArtistDialog(Artist artist,
                java.util.List<com.example.beat.data.entities.LocalSong> artistSongs, View view,
                com.example.beat.data.database.AppDatabase db,
                java.util.List<com.example.beat.data.entities.Playlist> playlists) {

            // Get user ID from SharedPreferences
            android.content.SharedPreferences prefs = view.getContext()
                .getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            int userId = prefs.getInt("userId", -1);

            String[] playlistNames = new String[playlists.size() + 1];
            for (int i = 0; i < playlists.size(); i++) {
                playlistNames[i] = playlists.get(i).getName();
            }
            playlistNames[playlists.size()] = "Create New Playlist...";

            new AlertDialog.Builder(view.getContext())
                .setTitle("Add Artist to Playlist")
                .setMessage("Add all " + artistSongs.size() + " songs from \"" + artist.name + "\" to:")
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
                                    createPlaylistAndAddArtistSongs(artist, artistSongs, playlistName, view, db, userId);
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
                        addArtistSongsToExistingPlaylist(artist, artistSongs, selectedPlaylist, view, db);
                    }
                })
                .show();
        }

        private void createPlaylistAndAddArtistSongs(Artist artist,
                java.util.List<com.example.beat.data.entities.LocalSong> artistSongs, String playlistName, View view,
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
                    for (com.example.beat.data.entities.LocalSong song : artistSongs) {
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
                            .setMessage("Created playlist \"" + playlistName + "\" and added " + finalAddedCount + " songs from artist \"" + artist.name + "\"")
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

        private void addArtistSongsToExistingPlaylist(Artist artist,
                java.util.List<com.example.beat.data.entities.LocalSong> artistSongs,
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
                    for (com.example.beat.data.entities.LocalSong song : artistSongs) {
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
                            message = "Added all " + finalAddedCount + " songs from artist \"" + artist.name + "\" to playlist \"" + playlist.getName() + "\"";
                        } else if (finalAddedCount == 0) {
                            message = "All " + finalSkippedCount + " songs from artist \"" + artist.name + "\" are already in playlist \"" + playlist.getName() + "\"";
                        } else {
                            message = "Added " + finalAddedCount + " new songs from artist \"" + artist.name + "\" to playlist \"" + playlist.getName() + "\". " + finalSkippedCount + " songs were already in the playlist.";
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

        public void bind(Artist artist) {
            if (artist != null) {
                artistName.setText(artist.name);
                songCount.setText(String.format("%d songs", artist.songCount));
            }

            // Load album art from first song of this artist, or use default
            loadArtistAlbumArt(artist);
        }

        private void loadArtistAlbumArt(Artist artist) {
            if (artist == null) {
                artistArt.setImageResource(R.drawable.default_artist);
                return;
            }

            // Get album art from first song of this artist in background thread
            new Thread(() -> {
                try {
                    com.example.beat.data.database.AppDatabase database =
                        com.example.beat.data.database.AppDatabase.getInstance(itemView.getContext());

                    // Get user ID from SharedPreferences
                    android.content.SharedPreferences prefs = itemView.getContext()
                        .getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                    int userId = prefs.getInt("userId", -1);

                    if (userId == -1) {
                        // Fallback to default if no user ID
                        ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                            artistArt.setImageResource(R.drawable.default_artist);
                        });
                        return;
                    }

                    // Get first song for this artist and try multiple album art methods
                    java.util.List<com.example.beat.data.entities.LocalSong> artistSongs =
                        database.musicDao().getSongsByArtistAndUser(artist.artistId, userId);

                    String albumArtUri = null;
                    com.example.beat.data.entities.LocalSong firstSongWithArt = null;

                    if (artistSongs != null && !artistSongs.isEmpty()) {
                        // First, try to find a song with MediaStore album art
                        for (com.example.beat.data.entities.LocalSong song : artistSongs) {
                            if (song.getAlbumArtUri() != null && !song.getAlbumArtUri().isEmpty()
                                && !song.getAlbumArtUri().startsWith("file://")) {
                                albumArtUri = song.getAlbumArtUri();
                                android.util.Log.d("ArtistAdapter", "✅ Found MediaStore album art for artist '" + artist.name + "': " + albumArtUri);
                                break;
                            }
                        }

                        // If no MediaStore album art found, try embedded extraction from first song
                        if (albumArtUri == null && !artistSongs.isEmpty()) {
                            firstSongWithArt = artistSongs.get(0); // Use first song for embedded extraction
                            android.util.Log.d("ArtistAdapter", "🔄 No MediaStore album art for artist '" + artist.name + "', trying embedded extraction from: " + firstSongWithArt.getTitle());
                        }
                    }

                    // Update UI on main thread
                    final String finalAlbumArtUri = albumArtUri;
                    final com.example.beat.data.entities.LocalSong finalFirstSong = firstSongWithArt;

                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        if (finalAlbumArtUri != null && !finalAlbumArtUri.isEmpty()) {
                            // Load MediaStore album art
                            loadArtistAlbumArt(finalAlbumArtUri, finalFirstSong, artist.name);
                        } else if (finalFirstSong != null) {
                            // No MediaStore album art, try embedded extraction
                            loadEmbeddedAlbumArtForArtist(finalFirstSong, artist.name);
                        } else {
                            android.util.Log.d("ArtistAdapter", "❌ No songs found for artist '" + artist.name + "', using default");
                            artistArt.setImageResource(R.drawable.default_album_art);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // Fallback to default on error
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        artistArt.setImageResource(R.drawable.default_album_art);
                    });
                }
            }).start();
        }

        private void loadArtistAlbumArt(String albumArtUri, com.example.beat.data.entities.LocalSong fallbackSong, String artistName) {
            com.bumptech.glide.Glide.with(itemView.getContext())
                    .load(albumArtUri)
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("ArtistAdapter", "❌ MediaStore album art failed for artist '" + artistName + "', trying embedded extraction");
                            // Try embedded extraction as fallback
                            if (fallbackSong != null) {
                                loadEmbeddedAlbumArtForArtist(fallbackSong, artistName);
                            } else {
                                artistArt.setImageResource(R.drawable.default_album_art);
                            }
                            return true; // We handle the error
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("ArtistAdapter", "✅ Successfully loaded MediaStore album art for artist '" + artistName + "'");
                            return false;
                        }
                    })
                    .into(artistArt);
        }

        private void loadEmbeddedAlbumArtForArtist(com.example.beat.data.entities.LocalSong song, String artistName) {
            // Try to extract embedded album art in background thread
            new Thread(() -> {
                try {
                    android.graphics.Bitmap albumArt = com.example.beat.utils.AlbumArtExtractor.extractAlbumArt(
                        itemView.getContext(), song.getFilePath());

                    // Update UI on main thread
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        if (albumArt != null) {
                            android.util.Log.d("ArtistAdapter", "✅ Successfully extracted embedded album art for artist '" + artistName + "'");
                            artistArt.setImageBitmap(albumArt);
                        } else {
                            android.util.Log.d("ArtistAdapter", "❌ No embedded album art found for artist '" + artistName + "', using default");
                            artistArt.setImageResource(R.drawable.default_album_art);
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("ArtistAdapter", "Error extracting embedded album art for artist '" + artistName + "': " + e.getMessage());
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        artistArt.setImageResource(R.drawable.default_album_art);
                    });
                }
            }).start();
        }
    }
}
