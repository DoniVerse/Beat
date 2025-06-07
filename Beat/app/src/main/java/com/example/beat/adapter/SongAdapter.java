package com.example.beat.adapter;

import android.content.Intent;
import android.net.Uri;
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

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.song_title);
            songArtist = itemView.findViewById(R.id.song_artist);
            songArt = itemView.findViewById(R.id.song_art);
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
        }

        public void bind(LocalSong song) {
            songTitle.setText(song.getTitle());
            if (song.getArtistId() != null) {
                songArtist.setText("Artist: " + song.getArtistId());
            } else {
                songArtist.setText("No Artist");
            }
            
            // Load album art with multiple fallback methods
            String albumArtUri = song.getAlbumArtUri();
            android.util.Log.d("SongAdapter", "Loading album art for '" + song.getTitle() + "': " + albumArtUri);

            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                // Check if it's a file path (our fallback method)
                if (albumArtUri.startsWith("file://")) {
                    // Try to extract embedded album art from the file
                    loadEmbeddedAlbumArt(song, songArt);
                } else {
                    // Try to load from MediaStore content URI
                    loadMediaStoreAlbumArt(song, albumArtUri, songArt);
                }
            } else {
                android.util.Log.d("SongAdapter", "No album art URI for '" + song.getTitle() + "', using default");
                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(R.drawable.default_album_art)
                        .into(songArt);
            }
        }

        private void loadMediaStoreAlbumArt(LocalSong song, String albumArtUri, android.widget.ImageView songArt) {
            com.bumptech.glide.Glide.with(itemView.getContext())
                    .load(albumArtUri)
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("SongAdapter", "❌ MediaStore album art failed for '" + song.getTitle() + "': " + albumArtUri);
                            // Try embedded album art as fallback
                            loadEmbeddedAlbumArt(song, songArt);
                            return true; // We handle the error ourselves
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("SongAdapter", "✅ Successfully loaded MediaStore album art for '" + song.getTitle() + "'");
                            return false; // Let Glide handle the success
                        }
                    })
                    .into(songArt);
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
