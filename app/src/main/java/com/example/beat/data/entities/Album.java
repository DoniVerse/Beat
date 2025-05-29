package com.example.beat.data.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "album")
public class Album implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int albumId;

    @NonNull
    public String name;

    public String releaseYear;

    public int artistId;

    public Album() {
        // Required empty constructor for Room
    }

    protected Album(Parcel in) {
        albumId = in.readInt();
        name = in.readString();
        releaseYear = in.readString();
        artistId = in.readInt();
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(albumId);
        dest.writeString(name);
        dest.writeString(releaseYear);
        dest.writeInt(artistId);
    }
}
