package com.example.beat.data.entities;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class AlbumWithSongs implements Parcelable {
    @Embedded
    public Album album;

    @Relation(
        parentColumn = "albumId",
        entityColumn = "albumId"
    )
    public List<LocalSong> songs;

    public AlbumWithSongs() {
        // Required empty constructor for Room
    }

    protected AlbumWithSongs(Parcel in) {
        album = in.readParcelable(Album.class.getClassLoader());
        songs = in.createTypedArrayList(LocalSong.CREATOR);
    }

    public static final Creator<AlbumWithSongs> CREATOR = new Creator<AlbumWithSongs>() {
        @Override
        public AlbumWithSongs createFromParcel(Parcel in) {
            return new AlbumWithSongs(in);
        }

        @Override
        public AlbumWithSongs[] newArray(int size) {
            return new AlbumWithSongs[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(album, flags);
        dest.writeTypedList(songs);
    }
}
