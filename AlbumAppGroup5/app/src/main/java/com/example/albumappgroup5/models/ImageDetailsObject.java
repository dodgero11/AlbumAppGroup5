package com.example.albumappgroup5.models;

import android.os.Parcelable;
import android.os.Parcel;
import java.util.Date;

public class ImageDetailsObject implements Parcelable {
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

    public ImageDetailsObject(String imageID, String imageName, String description, Date timeAdded) {
        this.imageID = imageID;
        this.imageName = imageName;
        this.description = description;
        this.timeAdded = timeAdded;
    }

    public ImageDetailsObject(String imageID) {
        this.imageID = imageID;
    }

    protected ImageDetailsObject(Parcel in) {
        this.imageID = imageID;
        this.imageName = imageName;
        this.description = description;
        this.timeAdded = timeAdded;
        this.location = location;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageID);
        dest.writeString(imageName);
        dest.writeString(description);
        // If timeAdded is null, write 0 so that during reading we can get null back if needed.
        dest.writeLong(timeAdded != null ? timeAdded.getTime() : 0);
        dest.writeString(location);
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

    public static final Creator<ImageDetailsObject> CREATOR = new Creator<ImageDetailsObject>() {
        @Override
        public ImageDetailsObject createFromParcel(Parcel in) {
            return new ImageDetailsObject(in);
        }

        @Override
        public ImageDetailsObject[] newArray(int size) {
            return new ImageDetailsObject[size];
        }
    };
}
