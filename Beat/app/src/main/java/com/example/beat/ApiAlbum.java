package com.example.beat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ApiAlbum {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("cover")
    private String cover;

    @JsonProperty("cover_small")
    private String coverSmall;

    @JsonProperty("cover_medium")
    private String coverMedium;

    @JsonProperty("cover_big")
    private String coverBig;

    @JsonProperty("cover_xl")
    private String coverXl;

    @JsonProperty("md5_image")
    private String md5Image;

    @JsonProperty("tracklist")
    private String tracklist;

    @JsonProperty("type")
    private String type;
}
