package com.example.albumappgroup5.models;

import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumModel extends ViewModel {
    private List<AlbumObject> albumList = new ArrayList<>();
    private Map<String, List<ImageDetailsObject>> albumImages = new HashMap<>();
    private Map<String, String> albumThumbnails = new HashMap<>();


    public void addAlbum(AlbumObject album) {
        if (!albumList.contains(album)) {
            albumList.add(album);
            albumImages.put(album.getAlbumName(), new ArrayList<>());
        }
    }

    public void removeAlbum(AlbumObject albumName) {
        if (albumList.contains(albumName)) {
            albumList.remove(albumName);
            albumImages.remove(albumName);
        }
    }

    public Map<String, List<ImageDetailsObject>> getAlbumImages() {
        return albumImages;
    }

    public List<AlbumObject> getAlbumList() {
        return albumList;
    }

    public AlbumObject getAlbumByName(String albumName) {
        for (AlbumObject album : albumList) {
            if (album.getAlbumName().equals(albumName)) {
                return album;
            }
        }
        return null;
    }

    public void setAlbumThumbnail(String albumName, String imagePath) {
        albumThumbnails.put(albumName, imagePath);
    }
    public String getAlbumThumbnail(String albumName) {
        return albumThumbnails.getOrDefault(albumName, null);
    }

    public void clearAlbumList() {
        albumList.clear();
    }
}
