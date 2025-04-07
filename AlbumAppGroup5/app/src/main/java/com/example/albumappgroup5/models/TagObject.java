package com.example.albumappgroup5.models;

public class TagObject {
    private final int tagID;
    private final String tagName;

    public TagObject (int tagID, String tagName) {
        this.tagID = tagID;
        this.tagName = tagName;
    }

    public int getTagID() {
        return tagID;
    }
    public String getTagName() {
        return tagName;
    }
}
