package com.example.albumappgroup5.activities;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.example.albumappgroup5.models.ImageDetailsModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// note: queries return null on error
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
            Log.e("error", "cannot access database, " + e);
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
    // return true on successful table creation, false if error
        boolean success = true; // return value
        database.beginTransaction();
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS Image (" +
                    "imageID text NOT NULL PRIMARY KEY," + // draft - change to different type if necessary
                    "imageName text," +
                    "description text," +
                    "timeAdded integer," + // unix time (seconds since 1970-01-01 00:00:00)
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
                    "password text NOT NULL," + // password also cannot be empty
                    "FOREIGN KEY (imageID) REFERENCES Image(imageID)" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS AlbumPassword (" +
                    "albumID integer PRIMARY KEY," +
                    "password text NOT NULL," + // password also cannot be empty
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

    public boolean resetDatabase () { // DANGER, will delete all data
    // return true on successful deletion, false if error
        boolean success = true;
        database.beginTransaction();
        try {
            database.execSQL("DROP TABLE IF EXISTS Image");
            database.execSQL("DROP TABLE IF EXISTS Tag");
            database.execSQL("DROP TABLE IF EXISTS ImageTag");
            database.execSQL("DROP TABLE IF EXISTS Album");
            database.execSQL("DROP TABLE IF EXISTS ImageAlbum");
            database.execSQL("DROP TABLE IF EXISTS ImagePassword");
            database.execSQL("DROP TABLE IF EXISTS AlbumPassword");

            database.setTransactionSuccessful();
        }
        catch (IllegalStateException e) {
            Log.e("error", "transaction lost before commit");
            success = false;
        }
        catch (SQLException e) {
            Log.e("error", e.toString());
            success = false;
        }
        finally {
            database.endTransaction();
        }
        return success;
    }

    public ImageDetailsModel getImageDetails (String imageID) {
    // return object with image details, null on error AND if image is not found
        ImageDetailsModel result = null;

        try (Cursor data = database.rawQuery("SELECT imageName, description, timeAdded, location " +
                        "FROM Image " +
                        "WHERE imageID = ?",
                new String[]{imageID})) {

            if (data.moveToFirst()) {
                String imageName = data.getString(data.getColumnIndexOrThrow("imageName"));
                String description = data.getString(data.getColumnIndexOrThrow("description"));
                long timeAddedString = data.getLong(data.getColumnIndexOrThrow("timeAdded"));
                String location = data.getString(data.getColumnIndexOrThrow("location"));

                result = new ImageDetailsModel(imageName, description, new Date(timeAddedString * 1000), location);
            }
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
        } catch (IllegalArgumentException e) {
            Log.e("error", "table error, column not found - " + e);
        }

        return result;
    }

    public List<String> getAlbumImages (int albumID) {
    // return list of imageID
        List<String> result = new ArrayList<>();

        try (Cursor data = database.query("ImageAlbum", new String[]{"imageID"}, "albumID = ?", new String[]{String.valueOf(albumID)}, null, null, null)) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                result.add(data.getString(data.getColumnIndexOrThrow("imageID")));
            }
            return result;
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        } catch (IllegalArgumentException e) {
            Log.e("error", "table error, column not found - " + e);
            return null;
        }

    }

    public List<String> getAlbumImages (int albumID, String sortCriteria, boolean ascending) {
    /* sortCriteria is column name of Image
     ascending is sort order (true for ascending, false for descending) */
        List<String> result = new ArrayList<>();

        try (Cursor data = database.rawQuery("SELECT Image.imageID " +
                        "FROM Image, ImageAlbum " +
                        "WHERE ImageAlbum.albumID = ? AND Image.imageID = ImageAlbum.imageID " +
                        "ORDER BY ? " + (ascending ? "ASC" : "DESC"),
                new String[]{String.valueOf(albumID), sortCriteria})) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                result.add(data.getString(data.getColumnIndexOrThrow("imageID")));
            }
            return result;
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        } catch (IllegalArgumentException e) {
            Log.e("error", "table error, column not found - " + e);
            return null;
        }
    }

    public String getImagePassword (String imageID) {
    // return null on error, empty string ("") if no password found, and password string otherwise
        try (Cursor data = database.rawQuery("SELECT password FROM ImagePassword WHERE imageID = ?",
                new String[]{imageID})) {
            if (!data.moveToFirst())
                return "";
            else
                return data.getString(0);
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }

    public String getAlbumPassword (int albumID) {
    // return null on error, empty string ("") if no password found, and password string otherwise
        try (Cursor data = database.rawQuery("SELECT password FROM AlbumPassword WHERE albumID = ?",
                new String[]{String.valueOf(albumID)})) {
            if (!data.moveToFirst())
                return "";
            else
                return data.getString(0);
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }
}
