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
                if (position != RecyclerView.NO_POSITION && actionListener != null) {
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
                    }
                    return true;
                } else if (item.getItemId() == R.id.action_add_album_to_playlist) {
                    if (actionListener != null) {
                        actionListener.onAddAlbumToPlaylist(album);
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
