package com.example.albumappgroup5.activities;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class DatabaseHandler {
    private static DatabaseHandler instance = null;

    private SQLiteDatabase database;

    // constructor
    private DatabaseHandler(Context context) {
        try {
//            database = SQLiteDatabase.openDatabase(context.getFilesDir().getPath() + Global.DATABASE_NAME, null, SQLiteDatabase.CREATE_IF_NECESSARY);
            database = context.openOrCreateDatabase(Global.DATABASE_NAME, Context.MODE_PRIVATE, null);
        }
        catch (SQLiteException e) {
            Log.e("error", "cannot access database, " + e.toString());
            database = null;
        }
    }

    public static DatabaseHandler getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHandler(context);
            if (instance.database == null) {
                instance = null;
            }
        }
        return instance;
    }

    public boolean createDatabase() { // run on first access to db only
        boolean success = true; // return value
        database.beginTransaction();
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS Image (" +
                    "imageID text NOT NULL PRIMARY KEY," + // draft - change to different type if necessary
                    "imageName text," +
                    "description text," +
                    "timeAdded text," + // datetime text (YYYY-MM-DD HH:MM:SS.SSS)
                    "location text" +
                    ");");

            database.execSQL("CREATE TABLE IF NOT EXISTS Tag (" +
                    "tagID integer PRIMARY KEY AUTOINCREMENT," +
                    "tagName text NOT NULL" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS ImageTag (" +
                    "imageID text NOT NULL PRIMARY KEY," +
                    "tagID integer PRIMARY KEY," +
                    "FOREIGN KEY (imageID) REFERENCES Image(imageID)," +
                    "FOREIGN KEY (tagID) REFERENCES Tag(tagID)" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS Album (" +
                    "albumID integer PRIMARY KEY AUTOINCREMENT," +
                    "albumName text NOT NULL" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS ImageAlbum (" +
                    "imageID text NOT NULL PRIMARY KEY," +
                    "albumID integer PRIMARY KEY," +
                    "FOREIGN KEY (imageID) REFERENCES Image(imageID)," +
                    "FOREIGN KEY (albumID) REFERENCES Album(albumID)" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS ImagePassword (" +
                    "imageID text NOT NULL PRIMARY KEY," +
                    "password text NOT NULL," +
                    "FOREIGN KEY (imageID) REFERENCES Image(imageID)" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS AlbumPassword (" +
                    "albumID integer PRIMARY KEY," +
                    "password text NOT NULL," +
                    "FOREIGN KEY (albumID) REFERENCES Album(albumID)" +
                    ")");

            database.setTransactionSuccessful();
        }
        catch (SQLiteException e) {
            Log.e("error", e.toString());
            success = false;
        }
        finally {
            database.endTransaction();
        }
        return success;
    }
}
