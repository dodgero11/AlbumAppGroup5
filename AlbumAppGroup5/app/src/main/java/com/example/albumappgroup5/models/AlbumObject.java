package com.example.albumappgroup5.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
