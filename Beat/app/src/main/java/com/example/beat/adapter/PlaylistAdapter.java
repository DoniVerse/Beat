package com.example.beat.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.MainActivity;
import com.example.beat.R;
import com.example.beat.data.entities.PlaylistWithSongs;
import com.example.beat.ui.PlaylistSongsFragment;
import com.example.beat.ui.LocalMusicPlayerActivity;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private List<PlaylistWithSongs> playlists;
    private int userId;

    public PlaylistAdapter() {
        this.playlists = new ArrayList<>();
    }

    public PlaylistAdapter(List<PlaylistWithSongs> playlists) {
        this.playlists = playlists;
    }

    public void updatePlaylists(List<PlaylistWithSongs> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistWithSongs playlist = playlists.get(position);
        holder.bind(playlist, userId);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final TextView playlistName;
        private final TextView songCount;
        private final PlaylistAdapter adapter;

        PlaylistViewHolder(@NonNull View itemView, PlaylistAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            playlistName = itemView.findViewById(R.id.playlist_name);
            songCount = itemView.findViewById(R.id.song_count);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    PlaylistWithSongs playlist = adapter.playlists.get(position);
                    if (playlist.songs != null && !playlist.songs.isEmpty()) {
                        Intent intent = new Intent(itemView.getContext(), LocalMusicPlayerActivity.class);
                        intent.putParcelableArrayListExtra("SONG_LIST", new ArrayList<>(playlist.songs));
                        intent.putExtra("POSITION", 0);
                        itemView.getContext().startActivity(intent);
                    }
                }
            });
        }

        void bind(PlaylistWithSongs playlist, int userId) {
            playlistName.setText(playlist.playlist.name);
            songCount.setText(String.format("%d songs", playlist.songs.size()));
        }
    }
}
