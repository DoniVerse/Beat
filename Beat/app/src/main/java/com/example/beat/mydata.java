package com.example.beat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class mydata {
    @JsonProperty("data")
    private List<Track> data;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("next")
    private String next;

    // Explicit getters
    public List<Track> getData() {
        return data;
    }

    public Integer getTotal() {
        return total;
    }

    public String getNext() {
        return next;
    }

    // Explicit setters
    public void setData(List<Track> data) {
        this.data = data;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public void setNext(String next) {
        this.next = next;
    }
}