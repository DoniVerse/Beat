package com.example.beat;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Track {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("readable")
    private Boolean readable;

    @JsonProperty("title")
    private String title;

    @JsonProperty("title_short")
    private String titleShort;

    @JsonProperty("title_version")
    private String titleVersion;

    @JsonProperty("link")
    private String link;

    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("rank")
    private Integer rank;

    @JsonProperty("explicit_lyrics")
    private Boolean explicitLyrics;

    @JsonProperty("explicit_content_lyrics")
    private Integer explicitContentLyrics;

    @JsonProperty("explicit_content_cover")
    private Integer explicitContentCover;

    @JsonProperty("preview")
    private String preview;

    @JsonProperty("md5_image")
    private String md5Image;

    @JsonProperty("artist")
    private ApiArtist artist;

    @JsonProperty("album")
    private ApiAlbum album;

    @JsonProperty("type")
    private String type;

    // Getters
    public Long getId() {
        return id;
    }

    public Boolean getReadable() {
        return readable;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleShort() {
        return titleShort;
    }

    public String getTitleVersion() {
        return titleVersion;
    }

    public String getLink() {
        return link;
    }

    public Integer getDuration() {
        return duration;
    }

    public Integer getRank() {
        return rank;
    }

    public Boolean getExplicitLyrics() {
        return explicitLyrics;
    }

    public Integer getExplicitContentLyrics() {
        return explicitContentLyrics;
    }

    public Integer getExplicitContentCover() {
        return explicitContentCover;
    }

    public String getPreview() {
        return preview;
    }

    public String getMd5Image() {
        return md5Image;
    }

    public ApiArtist getArtist() {
        return artist;
    }

    public ApiAlbum getAlbum() {
        return album;
    }

    public String getType() {
        return type;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setReadable(Boolean readable) {
        this.readable = readable;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitleShort(String titleShort) {
        this.titleShort = titleShort;
    }

    public void setTitleVersion(String titleVersion) {
        this.titleVersion = titleVersion;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public void setExplicitLyrics(Boolean explicitLyrics) {
        this.explicitLyrics = explicitLyrics;
    }

    public void setExplicitContentLyrics(Integer explicitContentLyrics) {
        this.explicitContentLyrics = explicitContentLyrics;
    }

    public void setExplicitContentCover(Integer explicitContentCover) {
        this.explicitContentCover = explicitContentCover;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public void setMd5Image(String md5Image) {
        this.md5Image = md5Image;
    }

    public void setArtist(ApiArtist artist) {
        this.artist = artist;
    }

    public void setAlbum(ApiAlbum album) {
        this.album = album;
    }

    public void setType(String type) {
        this.type = type;
    }
}