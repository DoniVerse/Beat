package com.example.beat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ArtistWithSongs artist = adapter.artists.get(position);
                    ArtistSongsFragment fragment = ArtistSongsFragment.newInstance(artist);
                    ((MainActivity) itemView.getContext()).navigateToFragment(fragment);
                }
            });
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
