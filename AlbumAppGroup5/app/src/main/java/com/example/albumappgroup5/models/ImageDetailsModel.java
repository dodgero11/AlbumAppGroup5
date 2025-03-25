package com.example.albumappgroup5.models;

import java.util.Date;

public class ImageDetailsModel {
    String imageName;
    String description;
    Date timeAdded;
    String location;

    public ImageDetailsModel (String imageName, String description, Date timeAdded, String location) {
        this.imageName = imageName;
        this.description = description;
        this.timeAdded = timeAdded;
        this.location = location;
    }

    public String getImageName() {
        return imageName;
    }
    public String getDescription() {
        return description;
    }
    public Date getTimeAdded() {
        return timeAdded;
    }
    public String getLocation() {
        return location;
    }
}
