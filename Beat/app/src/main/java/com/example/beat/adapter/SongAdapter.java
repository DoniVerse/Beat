package com.example.beat.adapter;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.google.android.material.imageview.ShapeableImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.ui.LocalMusicPlayerActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private List<LocalSong> songs;
    private String contextType = "LOCAL_SONGS"; // Default context
    private int contextId = -1; // For artist ID, playlist ID, etc.
    private OnSongActionListener actionListener;

    public interface OnSongActionListener {
        void onDeleteSong(LocalSong song, int position);
        void onAddSongToPlaylist(LocalSong song);
    }

    public SongAdapter() {
        this.songs = new ArrayList<>();
    }

    public SongAdapter(List<LocalSong> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
    }

    public SongAdapter(List<LocalSong> songs, String contextType) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        this.contextType = contextType;
    }

    public SongAdapter(List<LocalSong> songs, String contextType, int contextId) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        this.contextType = contextType;
        this.contextId = contextId;
    }

    public void updateSongs(List<LocalSong> newSongs) {
        this.songs.clear();
        this.songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    public void setOnSongActionListener(OnSongActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        LocalSong song = songs.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        private final TextView songTitle;
        private final TextView songArtist;
        private final ShapeableImageView songArt;
        private final ImageButton btnOptions;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.song_title);
            songArtist = itemView.findViewById(R.id.song_artist);
            songArt = itemView.findViewById(R.id.song_art);
            btnOptions = itemView.findViewById(R.id.btn_options);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    LocalSong clickedSong = songs.get(position);
                    // Use PlayerActivityWithService for background playback
                    Intent intent = new Intent(itemView.getContext(), com.example.beat.ui.PlayerActivityWithService.class);
                    intent.putExtra("title", clickedSong.getTitle());
                    intent.putExtra("artist", "Local Artist"); // You can improve this by getting actual artist name
                    intent.putExtra("albumArtUrl", clickedSong.getAlbumArtUri() != null ? clickedSong.getAlbumArtUri() : "");
                    intent.putExtra("streamUrl", clickedSong.getFilePath()); // Use file path for local songs

                    // Pass context information to load the correct playlist
                    intent.putExtra("POSITION", position);
                    intent.putExtra("TOTAL_SONGS", songs.size());
                    intent.putExtra("CONTEXT_TYPE", contextType);
                    if (contextId != -1) {
                        intent.putExtra("CONTEXT_ID", contextId);
                    }

                    itemView.getContext().startActivity(intent);
                }
            });

            // Set up option menu click listener
            btnOptions.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showOptionsMenu(v, songs.get(position), position);
                }
            });
        }

        private void showOptionsMenu(View view, LocalSong song, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.song_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete_song) {
                    if (actionListener != null) {
                        showDeleteConfirmation(song, position);
                    } else {
                        // Show confirmation and actually delete the song
                        showActualDeleteConfirmation(song, position, view);
                    }
                    return true;
                } else if (item.getItemId() == R.id.action_add_to_playlist) {
                    if (actionListener != null) {
                        actionListener.onAddSongToPlaylist(song);
                    } else {
                        // Show playlist selection dialog
                        showPlaylistSelectionDialog(song, view);
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        }

        private void showDeleteConfirmation(LocalSong song, int position) {
            // Delete directly without confirmation
            if (actionListener != null) {
                actionListener.onDeleteSong(song, position);
            }
        }

        private void showActualDeleteConfirmation(LocalSong song, int position, View view) {
            // Delete directly without confirmation
            deleteSongFromDatabase(song, position, view);
        }

        private void deleteSongFromDatabase(LocalSong song, int position, View view) {
            new Thread(() -> {
                try {
                    // Get database instance
                    com.example.beat.data.database.AppDatabase db =
                        com.example.beat.data.database.AppDatabase.getInstance(view.getContext());

                    // Delete the song
                    db.musicDao().deleteSong(song);

                    // Update UI on main thread
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        // Remove from adapter list
                        songs.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, songs.size());
                    });
                } catch (Exception e) {
                    // Silent error handling - just log the error
                    android.util.Log.e("SongAdapter", "Error deleting song: " + e.getMessage());
                }
            }).start();
        }

        private void showPlaylistSelectionDialog(LocalSong song, View view) {
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
                            showCreatePlaylistDialog(song, view, db, userId);
                        } else {
                            // Show playlist selection dialog
                            showExistingPlaylistsDialog(song, view, db, playlists);
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

        private void showCreatePlaylistDialog(LocalSong song, View view,
                com.example.beat.data.database.AppDatabase db, int userId) {
            android.widget.EditText editText = new android.widget.EditText(view.getContext());
            editText.setHint("Enter playlist name");

            new AlertDialog.Builder(view.getContext())
                .setTitle("Create New Playlist")
                .setMessage("You don't have any playlists yet. Create one to add this song.")
                .setView(editText)
                .setPositiveButton("Create", (dialog, which) -> {
                    String playlistName = editText.getText().toString().trim();
                    if (!playlistName.isEmpty()) {
                        createPlaylistAndAddSong(song, playlistName, view, db, userId);
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

        private void showExistingPlaylistsDialog(LocalSong song, View view,
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
                                    createPlaylistAndAddSong(song, playlistName, view, db, userId);
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
                        addSongToExistingPlaylist(song, selectedPlaylist, view, db);
                    }
                })
                .show();
        }

        private void createPlaylistAndAddSong(LocalSong song, String playlistName, View view,
                com.example.beat.data.database.AppDatabase db, int userId) {
            new Thread(() -> {
                try {
                    // Create new playlist
                    com.example.beat.data.entities.Playlist newPlaylist = new com.example.beat.data.entities.Playlist();
                    newPlaylist.setName(playlistName);
                    newPlaylist.setUserId(userId);

                    long playlistId = db.playlistDao().insert(newPlaylist);

                    // Add song to playlist
                    com.example.beat.data.entities.PlaylistSong playlistSong = new com.example.beat.data.entities.PlaylistSong();
                    playlistSong.setPlaylistId((int) playlistId);
                    playlistSong.setSongId(song.getSongId());

                    db.playlistDao().insertPlaylistSong(playlistSong);

                    // Show success message
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Success")
                            .setMessage("Created playlist \"" + playlistName + "\" and added \"" + song.getTitle() + "\"")
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

        private void addSongToExistingPlaylist(LocalSong song,
                com.example.beat.data.entities.Playlist playlist, View view,
                com.example.beat.data.database.AppDatabase db) {
            new Thread(() -> {
                try {
                    // Check if song is already in playlist
                    java.util.List<com.example.beat.data.entities.PlaylistSong> existingSongs =
                        db.playlistDao().getPlaylistSongs(playlist.getPlaylistId());

                    boolean songExists = false;
                    for (com.example.beat.data.entities.PlaylistSong ps : existingSongs) {
                        if (ps.getSongId() == song.getSongId()) {
                            songExists = true;
                            break;
                        }
                    }

                    if (songExists) {
                        ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                            new AlertDialog.Builder(view.getContext())
                                .setTitle("Already Added")
                                .setMessage("\"" + song.getTitle() + "\" is already in playlist \"" + playlist.getName() + "\"")
                                .setPositiveButton("OK", null)
                                .show();
                        });
                        return;
                    }

                    // Add song to playlist
                    com.example.beat.data.entities.PlaylistSong playlistSong = new com.example.beat.data.entities.PlaylistSong();
                    playlistSong.setPlaylistId(playlist.getPlaylistId());
                    playlistSong.setSongId(song.getSongId());

                    db.playlistDao().insertPlaylistSong(playlistSong);

                    // Show success message
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Success")
                            .setMessage("Added \"" + song.getTitle() + "\" to playlist \"" + playlist.getName() + "\"")
                            .setPositiveButton("OK", null)
                            .show();
                    });
                } catch (Exception e) {
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Error")
                            .setMessage("Failed to add song to playlist: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            }).start();
        }

        public void bind(LocalSong song) {
            songTitle.setText(song.getTitle());
            if (song.getArtistId() != null) {
                songArtist.setText("Artist: " + song.getArtistId());
            } else {
                songArtist.setText("No Artist");
            }
            
            // Load album art with fallback logic (like AlbumAdapter)
            String albumArtUri = song.getAlbumArtUri();
            android.util.Log.d("SongAdapter", "🔍 Song: '" + song.getTitle() + "', AlbumArtUri: '" + albumArtUri + "'");

            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                if (albumArtUri.startsWith("file://")) {
                    // File-based URI - try embedded art extraction
                    android.util.Log.d("SongAdapter", "🔄 Extracting embedded album art for: " + song.getTitle());
                    loadEmbeddedAlbumArt(song, songArt);
                } else {
                    // MediaStore URI - use Glide directly
                    android.util.Log.d("SongAdapter", "✅ Loading MediaStore album art for: " + song.getTitle());
                    com.bumptech.glide.Glide.with(itemView.getContext())
                            .load(albumArtUri)
                            .placeholder(R.drawable.default_album_art)
                            .error(R.drawable.default_album_art)
                            .into(songArt);
                }
            } else {
                android.util.Log.d("SongAdapter", "❌ No album art URI for '" + song.getTitle() + "', using default");
                songArt.setImageResource(R.drawable.default_album_art);
            }
        }

        private void loadEmbeddedAlbumArt(LocalSong song, android.widget.ImageView songArt) {
            // Try to extract embedded album art in background thread
            new Thread(() -> {
                try {
                    android.graphics.Bitmap albumArt = com.example.beat.utils.AlbumArtExtractor.extractAlbumArt(
                        itemView.getContext(), song.getFilePath());

                    // Update UI on main thread
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        if (albumArt != null) {
                            android.util.Log.d("SongAdapter", "✅ Successfully extracted embedded album art for '" + song.getTitle() + "'");
                            songArt.setImageBitmap(albumArt);
                        } else {
                            android.util.Log.d("SongAdapter", "❌ No embedded album art found for '" + song.getTitle() + "', using default");
                            songArt.setImageResource(R.drawable.default_album_art);
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("SongAdapter", "Error extracting embedded album art for '" + song.getTitle() + "': " + e.getMessage());
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        songArt.setImageResource(R.drawable.default_album_art);
                    });
                }
            }).start();
        }

    }
}
