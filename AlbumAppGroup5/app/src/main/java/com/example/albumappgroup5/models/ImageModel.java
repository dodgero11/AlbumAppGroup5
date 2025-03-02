package com.example.albumappgroup5.models;

public class ImageModel {
    private String imagePath;
    private String name;
    private long fileSize;
    private String dateTaken;

    public ImageModel(String imagePath, String name, long fileSize, String dateTaken) {
        this.imagePath = imagePath;
        this.name = name;
        this.fileSize = fileSize;
        this.dateTaken = dateTaken;
    }

    public String getImagePath() {
        return imagePath;
    }
    public String getName() {
        return name;
    }
    public long getFileSize() {
        return fileSize;
    }
    public String getDateTaken() {
        return dateTaken;
    }

}
