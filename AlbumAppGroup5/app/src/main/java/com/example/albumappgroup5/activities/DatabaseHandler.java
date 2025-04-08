package com.example.albumappgroup5.activities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.example.albumappgroup5.models.AlbumObject;
import com.example.albumappgroup5.models.ImageDetailsObject;
import com.example.albumappgroup5.models.TagObject;

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
            database.execSQL("PRAGMA foreign_key = ON");
        }
        catch (SQLiteException e) {
            Log.e("error", "cannot access database, " + e);
            database = null;
        }
    }

    //-------- Required operations --------//
    public static DatabaseHandler getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHandler(context);
            if (instance.database == null) {
                instance = null;
            }
        }
        return instance;
    }

    public void closeDatabase () {
    /* after closing, a new instance is required to work with database again
    call when the program is about to exit */
        if (database != null) {
            database.close();
            instance = null;
        }
    }

    //-------- Database creation/deletion --------//
    public boolean createDatabase() { // run on first access to db only
    // return true on successful table creation, false if error
        boolean success = true; // return value
        database.beginTransaction();
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS Image (" +
                    "imageID text NOT NULL PRIMARY KEY," + // draft - change to different type if necessary
                    "imageName text," +
                    "description text," +
                    "timeAdded integer," + // unix time (milliseconds since 1970-01-01 00:00:00)
                    "location text" +
                    ");");

            database.execSQL("CREATE TABLE IF NOT EXISTS Tag (" +
                    "tagID integer PRIMARY KEY AUTOINCREMENT," +
                    "tagName text NOT NULL" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS ImageTag (" +
                    "imageID text NOT NULL," +
                    "tagID integer," +
                    "PRIMARY KEY (imageID, tagID)," +
                    "FOREIGN KEY (imageID) REFERENCES Image(imageID) ON DELETE CASCADE," +
                    "FOREIGN KEY (tagID) REFERENCES Tag(tagID) ON DELETE CASCADE" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS Album (" +
                    "albumID integer PRIMARY KEY AUTOINCREMENT," +
                    "albumName text NOT NULL" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS ImageAlbum (" +
                    "imageID text NOT NULL," +
                    "albumID integer," +
                    "PRIMARY KEY (imageID, albumID)," +
                    "FOREIGN KEY (imageID) REFERENCES Image(imageID) ON DELETE CASCADE," +
                    "FOREIGN KEY (albumID) REFERENCES Album(albumID) ON DELETE CASCADE" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS ImagePassword (" +
                    "imageID text NOT NULL PRIMARY KEY," +
                    "password text NOT NULL," + // password also cannot be empty
                    "FOREIGN KEY (imageID) REFERENCES Image(imageID) ON DELETE CASCADE" +
                    ")");

            database.execSQL("CREATE TABLE IF NOT EXISTS AlbumPassword (" +
                    "albumID integer PRIMARY KEY," +
                    "password text NOT NULL," + // password also cannot be empty
                    "FOREIGN KEY (albumID) REFERENCES Album(albumID) ON DELETE CASCADE" +
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

    //-------- Queries --------//
    public ImageDetailsObject getImageDetails (String imageID) {
    // return object with image details, null on error, if image is not found then return object with null values
        ImageDetailsObject result = null;

        try (Cursor data = database.rawQuery("SELECT imageName, description, timeAdded, location " +
                        "FROM Image " +
                        "WHERE imageID = ?",
                new String[]{imageID})) {

            if (data.moveToFirst()) {
                String imageName = data.getString(data.getColumnIndexOrThrow("imageName"));
                String description = data.getString(data.getColumnIndexOrThrow("description"));
                long timeAddedString = data.getLong(data.getColumnIndexOrThrow("timeAdded"));
                String location = data.getString(data.getColumnIndexOrThrow("location"));

                return new ImageDetailsObject(imageID, imageName, description, new Date(timeAddedString), location);
            }
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        } catch (IllegalArgumentException e) {
            Log.e("error", "table error, column not found - " + e);
            return null;
        }

        return new ImageDetailsObject(imageID, null, null, null, null);
    }

    public List<String> searchImages (String searchString) {
    // return list of imageID
        List<String> result = new ArrayList<>();

        searchString = "%" + searchString + "%";
        try (Cursor data = database.rawQuery("SELECT imageID FROM Image " +
                "WHERE imageName LIKE ? OR imageDescription LIKE ?", new String[]{searchString, searchString})) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                result.add(data.getString(0));
            }
            return result;
        }
        catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }

    public List<String> getAlbumImages (int albumID) {
    // return list of imageID
        List<String> result = new ArrayList<>();

        try (Cursor data = database.rawQuery("SELECT imageID FROM ImageAlbum WHERE albumID = ?", new String[]{String.valueOf(albumID)})) {
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
                        "FROM Image " +
                        "INNER JOIN ImageAlbum ON Image.imageID = ImageAlbum.imageID " +
                        "WHERE ImageAlbum.albumID = ? " +
                        "ORDER BY Image.? " + (ascending ? "ASC" : "DESC"),
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

    public List<String> getImagesByTag (int tagID) {
        List<String> result = new ArrayList<>();

        try (Cursor data = database.rawQuery("SELECT imageID FROM ImageTag WHERE tagID = ?",
                new String[]{String.valueOf(tagID)})) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                result.add(data.getString(0));
            }
            return result;
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }

    public List<String> getImagesByTag (String tagName) {
    // return list of imageID, or empty list if tag name not found
        try (Cursor data = database.rawQuery("SELECT tagID FROM Tag WHERE tagName = ?",
                new String[]{tagName})) {
            if (data.moveToFirst()) {
                return getImagesByTag(data.getInt(0));
            }
            return new ArrayList<>(); // empty list
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }

    public List<String> getImagesByTag (int tagID, String sortCriteria, boolean ascending) {
    /* sortCriteria is column name of Image
     ascending is sort order (true for ascending, false for descending) */
        List<String> result = new ArrayList<>();

        try (Cursor data = database.rawQuery("SELECT Tag.imageID " +
                        "FROM ImageTag " +
                        "JOIN Image ON Tag.imageID = Image.imageID " +
                        "WHERE tagID = ? " +
                        "ORDER BY Image.? " + (ascending ? "ASC" : "DESC"),
                new String[]{String.valueOf(tagID), sortCriteria})) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                result.add(data.getString(0));
            }
            return result;
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }

    public List<String> getImagesByTag (String tagName, String sortCriteria, boolean ascending) {
        // return list of imageID, or empty list if tag name not found
        try (Cursor data = database.rawQuery("SELECT tagID FROM Tag WHERE tagName = ?",
                new String[]{tagName})) {
            if (data.moveToFirst()) {
                return getImagesByTag(data.getInt(0), sortCriteria, ascending);
            }
            return new ArrayList<>(); // empty list
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }

    public List<TagObject> getImageTagList (String imageID) {
        List<TagObject> result = new ArrayList<>();

        try (Cursor data = database.rawQuery("SELECT Tag.tagID, Tag.tagName " +
                "FROM Tag " +
                "JOIN ImageTag ON ImageTag.tagID = Tag.tagID " +
                "WHERE ImageTag.imageID = ? " +
                "ORDER BY Tag.TagName ASC",
                new String[]{imageID}
                )) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                result.add(new TagObject(data.getInt(0), data.getString(1)));
            }
            return result;
        }
        catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }

    public List<String> getTagNames () {
        List<String> result = new ArrayList<>();

        try (Cursor data = database.rawQuery("SELECT tagName from Tag", null)) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                result.add(data.getString(0));
            }
            return result;
        }
        catch (SQLiteException e) {
            Log.e("error", e.toString());
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

    public List<AlbumObject> getAlbums () {
        List<AlbumObject> result = new ArrayList<>();
        try (Cursor data = database.rawQuery("SELECT albumID, albumName FROM Album", null)) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                result.add(new AlbumObject(data.getInt(0), data.getString(1)));
            }
            return result;
        }
        catch (SQLiteException e) {
            Log.e("error", e.toString());
            return null;
        }
    }

    //-------- Insertions --------//

    public boolean insertImage (String imageID) {
    // imageID may need to reference image file name
        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("imageID", imageID);
            item.put("timeAdded", System.currentTimeMillis());
            if (database.insert("Image", "imageName", item) != -1)
                database.setTransactionSuccessful();
            else
                success = false;
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
    public boolean insertImage (ImageDetailsObject data) {
        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("imageID", data.getImageID());
            item.put("imageName", data.getImageName());
            item.put("description", data.getDescription());
            item.put("location", data.getLocation());
            if (data.getTimeAdded() != null)
                item.put("timeAdded", data.getTimeAdded().getTime());
            else
                item.put("timeAdded", System.currentTimeMillis());
            if (database.insert("Image", "imageName", item) != -1)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean insertAlbum (String albumName) {
    // allowing duplicates name for album for now
        if (albumName == null || albumName.isEmpty())
            return false;

        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("albumName", albumName);
            if (database.insert("Album", null, item) != -1)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean insertTag (String tagName) {
    // do not allow duplicate tag name (case insensitive)
    // note: sqlite only work correctly with case sensitivity for English characters
        if (tagName == null || tagName.isEmpty())
            return false;

        boolean success = true;
        database.beginTransaction();
        try (Cursor check = database.rawQuery("SELECT 1 FROM Tag WHERE tagName LIKE ?", new String[]{tagName})) {
            if (check.getCount() != 0)
                success = false;
            else {
                ContentValues item = new ContentValues();
                item.put("tagName", tagName);
                if (database.insert("Tag", null, item) != -1)
                    database.setTransactionSuccessful();
                else
                    success = false;
            }
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

    public boolean addToAlbum (String imageID, int albumID) {
        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("imageID", imageID);
            item.put("albumID", albumID);
            if (database.insert("ImageAlbum", null, item) != -1)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean tagImage (String imageID, int tagID) {
        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("imageID", imageID);
            item.put("tagID", tagID);
            if (database.insert("ImageTag", null, item) != -1) {
                database.setTransactionSuccessful();
            }
            else
                success = false;
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
    public boolean tagImage (String imageID, String tagName) {
        boolean success = true;
        database.beginTransaction();
        try {
            insertTag(tagName);
            Cursor id = database.rawQuery("SELECT tagID FROM Tag WHERE tagName LIKE ?", new String[]{tagName});
            if (id.moveToFirst() && tagImage(imageID, id.getInt(0))) {
                 database.setTransactionSuccessful();
            }
            else
                success = false;
            id.close();
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

    public boolean insertImagePassword (String imageID, String password) {
        if (password == null || password.isEmpty())
            return false;

        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("imageID", imageID);
            item.put("password", password);
            if (database.insert("ImagePassword", null, item) != -1)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean insertAlbumPassword (int albumID, String password) {
        if (password == null || password.isEmpty())
            return false;

        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("albumID", albumID);
            item.put("password", password);
            if (database.insert("AlbumPassword", null, item) != -1)
                database.setTransactionSuccessful();
            else
                success = false;
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

    //-------- Deletions --------//
    // cascade delete should delete connected data on other tables
    // if not change to manually delete first
    public boolean deleteImage (String imageID) {
        boolean success = true;
        database.beginTransaction();
        try {
            if (database.delete("Image", "imageID = ?", new String[]{imageID}) > 0)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean deleteAlbum (int albumID) {
        boolean success = true;
        database.beginTransaction();
        try {
            if (database.delete("Album", "albumID = ?", new String[]{String.valueOf(albumID)}) > 0)
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

    public boolean deleteTag (int tagID) {
        boolean success = true;
        database.beginTransaction();
        try {
            if (database.delete("Tag", "tagID = ?", new String[]{String.valueOf(tagID)}) > 0)
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

    public boolean removeFromAlbum (String imageID, int albumID) {
        boolean success = true;
        database.beginTransaction();
        try {
            if (database.delete("ImageAlbum", "imageID = ? AND albumID = ?", new String[]{imageID, String.valueOf(albumID)}) > 0)
                database.setTransactionSuccessful();
            else
                success = false;
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            success = false;
        } finally {
            database.endTransaction();
        }
        return success;
    }

    public boolean removeImageTag (String imageID, int tagID) {
        boolean success = true;
        database.beginTransaction();
        try {
            if (database.delete("ImageTag", "imageID = ? AND tagID = ?", new String[]{imageID, String.valueOf(tagID)}) > 0)
                database.setTransactionSuccessful();
            else
                success = false;
        } catch (SQLiteException e) {
            Log.e("error", e.toString());
            success = false;
        } finally {
            database.endTransaction();
        }
        return success;
    }

    public boolean deleteImagePassword (String imageID) {
        boolean success = true;
        database.beginTransaction();
        try {
            if (database.delete("ImagePassword", "imageID = ?", new String[]{imageID}) > 0)
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

    public boolean deleteAlbumPassword (int albumID) {
        boolean success = true;
        database.beginTransaction();
        try {
            if (database.delete("AlbumPassword", "albumID = ?", new String[]{String.valueOf(albumID)}) > 0)
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

    //-------- Updates --------//
    public boolean updateImage (ImageDetailsObject image) {
        if (image.getImageID() == null)
            return false;

        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            if (image.getImageName() != null)
                item.put("imageName", image.getImageName());
            if (image.getDescription() != null)
                item.put("description", image.getDescription());
            if (image.getLocation() != null)
                item.put("location", image.getLocation());
            if (database.update("Image", item, "imageID = ?", new String[]{image.getImageID()}) > 0)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean updateAlbum (int albumID, String albumName) {
        if (albumName == null || albumName.isEmpty())
            return false;

        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("albumName", albumName);
            if (database.update("Album", item, "albumID = ?", new String[]{String.valueOf(albumID)}) > 0)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean updateTag (int tagID, String tagName) {
        if (tagName == null || tagName.isEmpty())
            return false;

        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("tagName", tagName);
            if (database.update("Tag", item, "tagID = ?", new String[]{String.valueOf(tagID)}) > 0)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean updateImagePassword (String imageID, String password) {
        if (password == null || password.isEmpty())
            return false;

        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("password", password);
            if (database.update("ImagePassword", item, "imageID = ?", new String[]{imageID}) > 0)
                database.setTransactionSuccessful();
            else
                success = false;
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

    public boolean updateAlbumPassword (int albumID, String password) {
        if (password == null || password.isEmpty())
            return false;

        boolean success = true;
        database.beginTransaction();
        try {
            ContentValues item = new ContentValues();
            item.put("password", password);
            if (database.update("AlbumPassword", item, "albumID = ?", new String[]{String.valueOf(albumID)}) > 0)
                database.setTransactionSuccessful();
            else
                success = false;
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
