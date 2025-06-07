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
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private OnSongSelectedListener listener;
    private List<LocalSong> songs;

    public SongAdapter() {
        this.songs = new ArrayList<>();
    }

    public SongAdapter(List<LocalSong> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
    }

    public void setOnSongSelectedListener(OnSongSelectedListener listener) {
        this.listener = listener;
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
                    if (listener != null) {
                        listener.onSongSelected(position);
                    } else {
                        // Fallback to starting a new activity if current activity is not set
                        Intent intent = new Intent(itemView.getContext(), LocalMusicPlayerActivity.class);
                        intent.putParcelableArrayListExtra("SONG_LIST", (ArrayList<LocalSong>) songs);
                        intent.putExtra("POSITION", position);
                        itemView.getContext().startActivity(intent);
                    }
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
            
            // Load album art if available, otherwise use default
            if (song.getAlbumArtUri() != null) {
                songArt.setImageURI(Uri.parse(song.getAlbumArtUri()));
            } else {
                songArt.setImageResource(R.drawable.default_artist);
            }
        }
    }
}
