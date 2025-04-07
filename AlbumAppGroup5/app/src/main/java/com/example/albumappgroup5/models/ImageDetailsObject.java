package com.example.albumappgroup5.models;

import java.util.Date;

public class ImageDetailsObject {
    String imageID;
    String imageName;
    String description;
    Date timeAdded;
    String location;

    public ImageDetailsObject(String imageID, String imageName, String description, Date timeAdded, String location) {
        this.imageID = imageID;
        this.imageName = imageName;
        this.description = description;
        this.timeAdded = timeAdded;
        this.location = location;
    }

    public String getImageID() {
        return imageID;
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

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setTimeAdded(Date timeAdded) {
        this.timeAdded = timeAdded;
    }
    public void setLocation(String location) {
        this.location = location;
    }
}
