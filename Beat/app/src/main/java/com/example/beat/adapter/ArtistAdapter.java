package com.example.beat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    public ArtistAdapter(List<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.listener = listener;
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

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artist_name);
            songCount = itemView.findViewById(R.id.song_count);
            artistArt = itemView.findViewById(R.id.artist_art);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onArtistClick(artists.get(position));
                }
            });
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
