package com.example.albumappgroup5.models;

import android.os.Parcelable;
import android.os.Parcel;

public class ImageModel implements Parcelable {
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

    protected ImageModel(Parcel in) {
        imagePath = in.readString();
        name = in.readString();
        fileSize = in.readLong();
        dateTaken = in.readString();
    }

    public static final Creator<ImageModel> CREATOR = new Creator<ImageModel>() {
        @Override
        public ImageModel createFromParcel(Parcel in) {
            return new ImageModel(in);
        }

        @Override
        public ImageModel[] newArray(int size) {
            return new ImageModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imagePath);
        dest.writeString(name);
        dest.writeLong(fileSize);
        dest.writeString(dateTaken);
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
