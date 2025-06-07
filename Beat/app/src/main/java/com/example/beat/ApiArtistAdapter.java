package com.example.beat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class ApiArtistAdapter extends RecyclerView.Adapter<ApiArtistAdapter.ArtistViewHolder> {
    private static final String TAG = "ArtistAdapter";
    private List<ApiArtist> artists;
    private OnArtistClickListener listener;

    public interface OnArtistClickListener {
        void onArtistClick(ApiArtist artist);
    }

    public ApiArtistAdapter(List<ApiArtist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.artist_item, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        ApiArtist artist = artists.get(position);
        holder.artistName.setText(artist.getName());

        String imageUrl = artist.getPictureMedium();
        Log.d(TAG, "Loading image for artist: " + artist.getName() + ", URL: " + imageUrl);

        // Load image using Glide
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_artist)
                    .error(R.drawable.default_artist)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Image load failed for " + artist.getName() + ": " + (e != null ? e.getMessage() : "unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "Image loaded successfully for " + artist.getName());
                            return false;
                        }
                    })
                    .into(holder.artistImage);
        } else {
            Log.w(TAG, "No image URL available for artist: " + artist.getName());
            holder.artistImage.setImageResource(R.drawable.default_artist);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onArtistClick(artist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return artists != null ? artists.size() : 0;
    }

    public void updateArtists(List<ApiArtist> newArtists) {
        this.artists = newArtists;
        notifyDataSetChanged();
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView artistImage;
        TextView artistName;

        ArtistViewHolder(View itemView) {
            super(itemView);
            artistImage = itemView.findViewById(R.id.artistImage);
            artistName = itemView.findViewById(R.id.artistName);
        }
    }
}