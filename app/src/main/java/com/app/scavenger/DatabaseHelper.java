package com.app.scavenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper sInstance;
    //Database Version
    private static final int DB_VERSION = 1;

    //Database Name
    private static final String DATABASE_NAME = "scavengerLikes.db";

    //Table Names
    private static final String TABLE_LIKES = "table_itemIds";

    //Common Columns
    private static final String KEY_ID = "id";
    private static final String KEY_ITEM_ID = "itemId";

    static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    //Create View List Table
    private static final String CREATE_TABLE_LIKES = "CREATE TABLE IF NOT EXISTS " + TABLE_LIKES + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_ITEM_ID +
            " TEXT)";

    //------------------------------ ALL TABLES -------------------------------------------------------------------------------------------

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_LIKES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*switch (oldVersion) {
            case 1:

        }*/
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIKES);
        onCreate(db);
    }

    //Deletes all data from Likes Table
    public void clearData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_LIKES);
    }

    //------------------------------ LIST TABLE -------------------------------------------------------------------------------------------

    //Retrieve data from Likes Table
    public Cursor getListContents_View() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_LIKES, null);
    }

    //Updates Likes itemID
    public void updateListItem(String itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ITEM_ID, itemId);
        db.update(TABLE_LIKES, contentValues,KEY_ITEM_ID + " =?", new String[]{itemId});
    }

    //Add data to Likes Table
    public void addDataToView(String itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ITEM_ID, itemId);
        db.insert(TABLE_LIKES, null, contentValues);
    }

    //Remove item from Likes Table when item is removed from Likes on Firebase
    public void removeDataFromView(String itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LIKES, KEY_ITEM_ID + "=?", new String[]{itemId});
    }
}

