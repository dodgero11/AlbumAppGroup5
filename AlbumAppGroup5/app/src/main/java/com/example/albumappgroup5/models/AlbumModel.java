package com.example.albumappgroup5.models;

import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumModel extends ViewModel {
    private List<String> albumList = new ArrayList<>();
    private Map<String, List<ImageModel>> albumImages = new HashMap<>();

    public List<String> getAlbumList() {
        return albumList;
    }

    public Map<String, List<ImageModel>> getAlbumImages() {
        return albumImages;
    }

    public void addAlbum(String albumName) {
        if (!albumList.contains(albumName)) {
            albumList.add(albumName);
            albumImages.put(albumName, new ArrayList<>());
        }
    }

    public void removeAlbum(String albumName) {
        if (albumList.contains(albumName)) {
            albumList.remove(albumName);
            albumImages.remove(albumName);
        }
    }
}
