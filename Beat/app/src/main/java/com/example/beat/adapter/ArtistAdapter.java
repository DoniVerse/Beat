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
            
            // Load artist art if available, otherwise use default
            if (artist != null && artist.artistArtUri != null) {
                artistArt.setImageURI(Uri.parse(artist.artistArtUri));
            } else {
                artistArt.setImageResource(R.drawable.default_artist);
            }
        }
    }
}
