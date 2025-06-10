package com.example.beat.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.MainActivity;
import com.example.beat.R;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.PlaylistWithSongs;
import com.example.beat.ui.PlaylistSongsFragment;

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
        private final ImageView optionMenu;
        private final PlaylistAdapter adapter;

        PlaylistViewHolder(@NonNull View itemView, PlaylistAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            playlistName = itemView.findViewById(R.id.playlist_name);
            songCount = itemView.findViewById(R.id.song_count);
            optionMenu = itemView.findViewById(R.id.playlist_option_menu);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    PlaylistWithSongs playlist = adapter.playlists.get(position);
                    PlaylistSongsFragment fragment = PlaylistSongsFragment.newInstance(playlist);
                    ((MainActivity) itemView.getContext()).navigateToFragment(fragment);
                }
            });

            optionMenu.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showPlaylistOptionsMenu(adapter.playlists.get(position), position, v);
                }
            });
        }

        void bind(PlaylistWithSongs playlist, int userId) {
            playlistName.setText(playlist.playlist.name);
            songCount.setText(String.format("%d songs", playlist.songs.size()));
        }

        private void showPlaylistOptionsMenu(PlaylistWithSongs playlist, int position, View view) {
            String[] options = {"Delete Playlist"};

            new AlertDialog.Builder(view.getContext())
                .setTitle("Playlist Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Delete playlist
                        showDeleteConfirmationDialog(playlist, position, view);
                    }
                })
                .show();
        }

        private void showDeleteConfirmationDialog(PlaylistWithSongs playlist, int position, View view) {
            new AlertDialog.Builder(view.getContext())
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete \"" + playlist.playlist.name + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deletePlaylist(playlist, position, view);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        private void deletePlaylist(PlaylistWithSongs playlist, int position, View view) {
            new Thread(() -> {
                try {
                    com.example.beat.data.database.AppDatabase db = com.example.beat.data.database.AppDatabase.getInstance(view.getContext());

                    // Delete all playlist songs first (foreign key constraint)
                    db.playlistDao().deletePlaylistSongs(playlist.playlist.getPlaylistId());

                    // Delete the playlist
                    db.playlistDao().delete(playlist.playlist);

                    // Update UI on main thread
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        // Remove from list and notify adapter
                        adapter.playlists.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, adapter.playlists.size());

                        Toast.makeText(view.getContext(), "Playlist deleted successfully", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    ((android.app.Activity) view.getContext()).runOnUiThread(() -> {
                        Toast.makeText(view.getContext(), "Error deleting playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }
}
