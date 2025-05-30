package com.example.beat.adapter;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.ui.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private List<LocalSong> songs;

    public SongAdapter() {
        this.songs = new ArrayList<>();
    }

    public SongAdapter(List<LocalSong> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
    }

    public void updateSongs(List<LocalSong> newSongs) {
        this.songs.clear();
        if (newSongs != null) {
            this.songs.addAll(newSongs);
        }
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
        private final TextView songArtist; // Changed from songAlbum
        private final ImageView songImage; // Added ImageView

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.song_title);
            songArtist = itemView.findViewById(R.id.song_artist); // Updated ID
            songImage = itemView.findViewById(R.id.song_image); // Added ID

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    LocalSong clickedSong = songs.get(position);
                    Intent intent = new Intent(itemView.getContext(), PlayerActivity.class);
                    intent.putParcelableArrayListExtra("SONG_LIST", (ArrayList<LocalSong>) songs);
                    intent.putExtra("POSITION", position);
                    itemView.getContext().startActivity(intent);
                }
            });
        }

        public void bind(LocalSong song) {
            songTitle.setText(song.getTitle());

            // Display Artist Name (or ID if name not available)
            // TODO: Fetch actual artist name based on artistId if possible
            if (song.getArtistId() != null && song.getArtistId() > 0) {
                // Ideally, fetch artist name from DB using ID
                // For now, displaying ID as placeholder
                songArtist.setText("Artist ID: " + song.getArtistId());
            } else {
                songArtist.setText("Unknown Artist");
            }

            // Load Album Art
            // TODO: Consider using Glide or Picasso for efficient image loading
            Uri albumArtUri = null;
            if (song.getAlbumArtUri() != null) {
                try {
                    albumArtUri = Uri.parse(song.getAlbumArtUri());
                } catch (Exception e) {
                    // Handle potential parsing error
                    albumArtUri = null;
                }
            }
            
            // Fallback using album ID if direct URI is not available or invalid
            if (albumArtUri == null && song.getAlbumId() != null && song.getAlbumId() > 0) {
                 Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                 albumArtUri = ContentUris.withAppendedId(sArtworkUri, song.getAlbumId());
            }

            if (albumArtUri != null) {
                try {
                    songImage.setImageURI(albumArtUri);
                    // Handle cases where setImageURI might fail (e.g., invalid URI, file not found)
                    if (songImage.getDrawable() == null) {
                         songImage.setImageResource(R.drawable.default_artist); // Fallback
                    }
                } catch (Exception e) {
                    songImage.setImageResource(R.drawable.default_artist); // Fallback on error
                }
            } else {
                songImage.setImageResource(R.drawable.default_artist); // Default image
            }
        }
    }
}

