package com.example.albumappgroup5.models;

public class AlbumObject {
    int albumID;
    String albumName;

    public AlbumObject (int albumID, String albumName) {
        this.albumID = albumID;
        this.albumName = albumName;
    }

    public int getAlbumID() {
        return albumID;
    }
    public String getAlbumName() {
        return albumName;
    }
}
