package com.example.beat.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.Serializable;

@Entity(tableName = "local_song",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Artist.class, parentColumns = "artistId", childColumns = "artistId", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = Album.class, parentColumns = "albumId", childColumns = "albumId", onDelete = ForeignKey.SET_NULL)
        },
        indices = {
                @Index("userId"),
                @Index("artistId"),
                @Index("albumId"),
                @Index(value = {"filePath", "userId"}, unique = true)
        }
)
public class LocalSong implements Parcelable, Serializable {
    private static final long serialVersionUID = 1L;
    protected LocalSong(Parcel in) {
        songId = in.readInt();
        title = in.readString();
        filePath = in.readString();
        userId = in.readInt();
        artistId = in.readInt();
        albumId = in.readInt();
        albumArtUri = in.readString();  // read album art URI from parcel
    }

    public static final Creator<LocalSong> CREATOR = new Creator<LocalSong>() {
        @Override
        public LocalSong createFromParcel(Parcel in) {
            return new LocalSong(in);
        }

        @Override
        public LocalSong[] newArray(int size) {
            return new LocalSong[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(songId);
        dest.writeString(title);
        dest.writeString(filePath);
        dest.writeInt(userId);
        dest.writeInt(artistId != null ? artistId : 0);
        dest.writeInt(albumId != null ? albumId : 0);
        dest.writeString(albumArtUri);  // write album art URI to parcel
    }

    @PrimaryKey(autoGenerate = true)
    private int songId;

    private String title;
    private String filePath;
    private int userId;
    private Integer artistId;
    private Integer albumId;

    // New field for album art URI
    private String albumArtUri;

    public LocalSong() {
        // Default constructor required by Room
    }

    public LocalSong(String title, String filePath) {
        this.title = title;
        this.filePath = filePath;
    }

    // Getters and setters...

    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getArtistId() {
        return artistId;
    }

    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    public Integer getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Integer albumId) {
        this.albumId = albumId;
    }

    public String getAlbumArtUri() {
        return albumArtUri;
    }

    public void setAlbumArtUri(String albumArtUri) {
        this.albumArtUri = albumArtUri;
    }
}
