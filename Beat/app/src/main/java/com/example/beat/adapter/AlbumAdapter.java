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

import com.example.beat.MainActivity;
import com.example.beat.R;
import com.example.beat.data.entities.AlbumWithSongs;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.ui.AlbumSongsFragment;
import com.google.android.material.imageview.ShapeableImageView;
import android.net.Uri;
import com.bumptech.glide.Glide;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private List<AlbumWithSongs> albums;
    private OnAlbumActionListener actionListener;

    public interface OnAlbumActionListener {
        void onDeleteAlbum(AlbumWithSongs album, int position);
        void onAddAlbumToPlaylist(AlbumWithSongs album);
    }

    public AlbumAdapter(List<AlbumWithSongs> albums) {
        this.albums = albums;
    }

    public void updateAlbums(List<AlbumWithSongs> albums) {
        this.albums = albums;
        notifyDataSetChanged();
    }

    public void setOnAlbumActionListener(OnAlbumActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        AlbumWithSongs album = albums.get(position);
        holder.albumName.setText(album.album.name);
        int songCount = album.songs != null ? album.songs.size() : 0;
        holder.songCount.setText(String.format("%d songs", songCount));
        holder.bind(album);
    }

    @Override
    public int getItemCount() {
        return albums != null ? albums.size() : 0;
    }

    class AlbumViewHolder extends RecyclerView.ViewHolder {
        private final TextView albumName;
        private final TextView songCount;
        private final ShapeableImageView albumArt;
        private final ImageButton btnOptions;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumName = itemView.findViewById(R.id.album_name);
            songCount = itemView.findViewById(R.id.song_count);
            albumArt = itemView.findViewById(R.id.album_art);
            btnOptions = itemView.findViewById(R.id.btn_options);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    AlbumWithSongs clickedAlbum = albums.get(position);
                    // Navigate to album songs fragment
                    AlbumSongsFragment fragment = AlbumSongsFragment.newInstance(clickedAlbum);
                    ((MainActivity) itemView.getContext()).navigateToFragment(fragment);
                }
            });

            // Set up option menu click listener
            btnOptions.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showOptionsMenu(v, albums.get(position), position);
                }
            });
        }

        private void showOptionsMenu(View view, AlbumWithSongs album, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.album_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete_album) {
                    if (actionListener != null) {
                        showDeleteConfirmation(album, position);
                    } else {
                        // Show confirmation and actually delete the album
                        showActualDeleteConfirmation(album, position, view);
                    }
                    return true;
                } else if (item.getItemId() == R.id.action_add_album_to_playlist) {
                    if (actionListener != null) {
                        actionListener.onAddAlbumToPlaylist(album);
                    } else {
                        // Show playlist selection dialog for all album songs
                        showAlbumPlaylistSelectionDialog(album, view);
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        }

        private void showDeleteConfirmation(AlbumWithSongs album, int position) {
            new AlertDialog.Builder(itemView.getContext())
                .setTitle("Delete Album")
                .setMessage("Are you sure you want to delete album \"" + album.album.name + "\" and all its songs?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onDeleteAlbum(album, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        private void showActualDeleteConfirmation(AlbumWithSongs album, int position, View view) {
            new AlertDialog.Builder(view.getContext())
                .setTitle("Delete Album")
                .setMessage("Are you sure you want to delete album \"" + album.album.name + "\" and all its " + album.songs.size() + " songs? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Actually delete the album and all its songs from database
                    deleteAlbumFromDatabase(album, position, view);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        private void deleteAlbumFromDatabase(AlbumWithSongs album, int position, View view) {
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

                    // Delete all songs in the album
                    db.musicDao().deleteAlbumSongs(album.album.albumId, userId);

                    // Delete the album itself
                    db.musicDao().deleteAlbum(album.album);

                    // Update UI on main thread
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        // Remove from adapter list
                        albums.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, albums.size());

                        // Show success message
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Success")
                            .setMessage("Album \"" + album.album.name + "\" and all its songs have been deleted successfully.")
                            .setPositiveButton("OK", null)
                            .show();
                    });
                } catch (Exception e) {
                    // Show error message on main thread
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Error")
                            .setMessage("Failed to delete album: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            }).start();
        }

        private void showAlbumPlaylistSelectionDialog(AlbumWithSongs album, View view) {
            if (album.songs == null || album.songs.isEmpty()) {
                new AlertDialog.Builder(view.getContext())
                    .setTitle("No Songs")
                    .setMessage("This album has no songs to add to playlist.")
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
                            showCreatePlaylistForAlbumDialog(album, view, db, userId);
                        } else {
                            // Show playlist selection dialog
                            showExistingPlaylistsForAlbumDialog(album, view, db, playlists);
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

        private void showCreatePlaylistForAlbumDialog(AlbumWithSongs album, View view,
                com.example.beat.data.database.AppDatabase db, int userId) {
            android.widget.EditText editText = new android.widget.EditText(view.getContext());
            editText.setHint("Enter playlist name");

            new AlertDialog.Builder(view.getContext())
                .setTitle("Create New Playlist")
                .setMessage("You don't have any playlists yet. Create one to add all " + album.songs.size() + " songs from this album.")
                .setView(editText)
                .setPositiveButton("Create", (dialog, which) -> {
                    String playlistName = editText.getText().toString().trim();
                    if (!playlistName.isEmpty()) {
                        createPlaylistAndAddAlbumSongs(album, playlistName, view, db, userId);
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

        private void showExistingPlaylistsForAlbumDialog(AlbumWithSongs album, View view,
                com.example.beat.data.database.AppDatabase db,
                java.util.List<com.example.beat.data.entities.Playlist> playlists) {

            String[] playlistNames = new String[playlists.size() + 1];
            for (int i = 0; i < playlists.size(); i++) {
                playlistNames[i] = playlists.get(i).getName();
            }
            playlistNames[playlists.size()] = "Create New Playlist...";

            new AlertDialog.Builder(view.getContext())
                .setTitle("Add to Playlist")
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
                                    android.content.SharedPreferences prefs = view.getContext()
                                        .getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                                    int userId = prefs.getInt("userId", -1);
                                    createPlaylistAndAddAlbumSongs(album, playlistName, view, db, userId);
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
                        addAlbumSongsToExistingPlaylist(album, selectedPlaylist, view, db);
                    }
                })
                .show();
        }



        private void createPlaylistAndAddAlbumSongs(AlbumWithSongs album, String playlistName, View view,
                com.example.beat.data.database.AppDatabase db, int userId) {
            new Thread(() -> {
                try {
                    // Create new playlist
                    com.example.beat.data.entities.Playlist newPlaylist = new com.example.beat.data.entities.Playlist();
                    newPlaylist.setName(playlistName);
                    newPlaylist.setUserId(userId);

                    long playlistId = db.playlistDao().insert(newPlaylist);

                    // Add all album songs to playlist
                    int addedCount = 0;
                    for (com.example.beat.data.entities.LocalSong song : album.songs) {
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
                            .setMessage("Created playlist \"" + playlistName + "\" and added " + finalAddedCount + " songs from album \"" + album.album.name + "\"")
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

        private void addAlbumSongsToExistingPlaylist(AlbumWithSongs album,
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
                    for (com.example.beat.data.entities.LocalSong song : album.songs) {
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
                            message = "Added all " + finalAddedCount + " songs from album \"" + album.album.name + "\" to playlist \"" + playlist.getName() + "\"";
                        } else if (finalAddedCount == 0) {
                            message = "All " + finalSkippedCount + " songs from album \"" + album.album.name + "\" are already in playlist \"" + playlist.getName() + "\"";
                        } else {
                            message = "Added " + finalAddedCount + " new songs from album \"" + album.album.name + "\" to playlist \"" + playlist.getName() + "\". " + finalSkippedCount + " songs were already in the playlist.";
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
                            .setMessage("Failed to add album songs to playlist: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            }).start();
        }

        public void bind(AlbumWithSongs album) {
            albumName.setText(album.album.name);
            songCount.setText(String.format("%d songs", album.songs.size()));

            // Get album art from first song with multiple fallback methods
            String albumArtUri = null;
            LocalSong firstSongWithArt = null;

            if (album.songs != null && !album.songs.isEmpty()) {
                // First, try to find a song with MediaStore album art
                for (LocalSong song : album.songs) {
                    if (song.getAlbumArtUri() != null && !song.getAlbumArtUri().isEmpty()
                        && !song.getAlbumArtUri().startsWith("file://")) {
                        albumArtUri = song.getAlbumArtUri();
                        android.util.Log.d("AlbumAdapter", "✅ Found MediaStore album art for album '" + album.album.name + "': " + albumArtUri);
                        break;
                    }
                }

                // If no MediaStore album art found, try embedded extraction from first song
                if (albumArtUri == null && !album.songs.isEmpty()) {
                    firstSongWithArt = album.songs.get(0); // Use first song for embedded extraction
                    android.util.Log.d("AlbumAdapter", "🔄 No MediaStore album art for album '" + album.album.name + "', trying embedded extraction from: " + firstSongWithArt.getTitle());
                }
            }

            // Load album art with fallback methods
            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                // Load MediaStore album art with fallback to embedded
                loadAlbumArt(albumArtUri, firstSongWithArt, album.album.name);
            } else if (firstSongWithArt != null) {
                // No MediaStore album art, try embedded extraction
                loadEmbeddedAlbumArtForAlbum(firstSongWithArt, album.album.name);
            } else {
                android.util.Log.d("AlbumAdapter", "❌ No songs found for album '" + album.album.name + "', using default");
                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(R.drawable.default_album_art)
                        .into(albumArt);
            }
        }

        private void loadAlbumArt(String albumArtUri, LocalSong fallbackSong, String albumName) {
            com.bumptech.glide.Glide.with(itemView.getContext())
                    .load(albumArtUri)
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("AlbumAdapter", "❌ MediaStore album art failed for album '" + albumName + "', trying embedded extraction");
                            // Try embedded extraction as fallback
                            if (fallbackSong != null) {
                                loadEmbeddedAlbumArtForAlbum(fallbackSong, albumName);
                            } else {
                                albumArt.setImageResource(R.drawable.default_album_art);
                            }
                            return true; // We handle the error
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("AlbumAdapter", "✅ Successfully loaded MediaStore album art for album '" + albumName + "'");
                            return false;
                        }
                    })
                    .into(albumArt);
        }

        private void loadEmbeddedAlbumArtForAlbum(LocalSong song, String albumName) {
            // Try to extract embedded album art in background thread
            new Thread(() -> {
                try {
                    android.graphics.Bitmap albumArt = com.example.beat.utils.AlbumArtExtractor.extractAlbumArt(
                        itemView.getContext(), song.getFilePath());

                    // Update UI on main thread
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        if (albumArt != null) {
                            android.util.Log.d("AlbumAdapter", "✅ Successfully extracted embedded album art for album '" + albumName + "'");
                            this.albumArt.setImageBitmap(albumArt);
                        } else {
                            android.util.Log.d("AlbumAdapter", "❌ No embedded album art found for album '" + albumName + "', using default");
                            this.albumArt.setImageResource(R.drawable.default_album_art);
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("AlbumAdapter", "Error extracting embedded album art for album '" + albumName + "': " + e.getMessage());
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        this.albumArt.setImageResource(R.drawable.default_album_art);
                    });
                }
            }).start();
        }
    }
}
