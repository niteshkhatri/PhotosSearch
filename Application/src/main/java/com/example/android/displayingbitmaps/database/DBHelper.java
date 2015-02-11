package com.example.android.displayingbitmaps.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.displayingbitmaps.ui.PhotoBean;

import java.util.ArrayList;

/**
 * Created by Nitesh on 26/1/15.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "PhotoSearch.db";
    public static final String PHOTO_TABLE_NAME = "photo";

    // Photo table fields
    public static final String PHOTO_COLUMN_ID = "id";
    public static final String PHOTO_COLUMN_PHOTO_ID = "photoid";
    public static final String PHOTO_COLUMN_URL = "photourl";
    public static final String PHOTO_COLUMN_SEARCH = "photosearch";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL( "create table " + PHOTO_TABLE_NAME + " (" +
                        PHOTO_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        PHOTO_COLUMN_PHOTO_ID + " TEXT," +
                        PHOTO_COLUMN_URL + " TEXT," +
                        PHOTO_COLUMN_SEARCH + " TEXT" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }

    /**
     * Get all photos
     * @return
     */
    public ArrayList<PhotoBean> getAllPhotos(String search) {
        ArrayList<PhotoBean> array_list = new ArrayList<PhotoBean>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor =  db.rawQuery( "select * from  " + PHOTO_TABLE_NAME + " where " + PHOTO_COLUMN_SEARCH + "='" + search + "'", null );
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            while(cursor.isAfterLast() == false) {
                String id = cursor.getString(cursor.getColumnIndex(PHOTO_COLUMN_ID));
                String photoid = cursor.getString(cursor.getColumnIndex(PHOTO_COLUMN_PHOTO_ID));
                String photourl = cursor.getString(cursor.getColumnIndex(PHOTO_COLUMN_URL));
                String name = cursor.getString(cursor.getColumnIndex(PHOTO_COLUMN_SEARCH));
                PhotoBean entity = new PhotoBean();
                entity.setId(id);
                entity.setPhotoid(photoid);
                entity.setPhotourl(photourl);
                entity.setPhotosearch(name);
                array_list.add(entity);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return array_list;
    }

    public long insertPhotoRecord(String photoId, String photoUrl, String search){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(PHOTO_COLUMN_PHOTO_ID, photoId);
        contentValues.put(PHOTO_COLUMN_URL, photoUrl);
        contentValues.put(PHOTO_COLUMN_SEARCH, search);

        long rowid = db.insert(PHOTO_TABLE_NAME, null, contentValues);

        db.close();

        return rowid;
    }

    public long insertPhotoRecordIfNotExist(String photoId, String photoUrl, String search){
        boolean isExist = false;
        ArrayList<PhotoBean> avatars = getAllPhotos(search);
        for(int i=0; i<avatars.size(); i++) {
            if(avatars.get(i).getPhotoid().equalsIgnoreCase(search)) {
                isExist = true;
                break;
            }
        }

        if(!isExist) {
            return insertPhotoRecord(photoId, photoUrl, search);
        }

        return 0;
    }

    public boolean deleteAllPhotoRecords() {
        int doneDelete = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        doneDelete = db.delete(PHOTO_TABLE_NAME, null , null);
        db.close();
        return doneDelete > 0;
    }

}
