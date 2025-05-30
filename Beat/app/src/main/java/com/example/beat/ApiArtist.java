package com.example.beat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ApiArtist {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("link")
    private String link;

    @JsonProperty("picture")
    private String picture;

    @JsonProperty("picture_small")
    private String pictureSmall;

    @JsonProperty("picture_medium")
    private String pictureMedium;

    @JsonProperty("picture_big")
    private String pictureBig;

    @JsonProperty("picture_xl")
    private String pictureXl;

    @JsonProperty("tracklist")
    private String tracklist;

    @JsonProperty("type")
    private String type;

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    public String getPicture() {
        return picture;
    }

    public String getPictureSmall() {
        return pictureSmall;
    }

    public String getPictureMedium() {
        return pictureMedium;
    }

    public String getPictureBig() {
        return pictureBig;
    }

    public String getPictureXl() {
        return pictureXl;
    }

    public String getTracklist() {
        return tracklist;
    }

    public String getType() {
        return type;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setPictureSmall(String pictureSmall) {
        this.pictureSmall = pictureSmall;
    }

    public void setPictureMedium(String pictureMedium) {
        this.pictureMedium = pictureMedium;
    }

    public void setPictureBig(String pictureBig) {
        this.pictureBig = pictureBig;
    }

    public void setPictureXl(String pictureXl) {
        this.pictureXl = pictureXl;
    }

    public void setTracklist(String tracklist) {
        this.tracklist = tracklist;
    }

    public void setType(String type) {
        this.type = type;
    }
}
