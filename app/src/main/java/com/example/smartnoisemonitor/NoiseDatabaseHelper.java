package com.example.smartnoisemonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NoiseDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "noise_data.db";
    private static final int DATABASE_VERSION = 1;
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";

    public static final String TABLE_NAME = "noise";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_DECIBEL = "decibel";

    public NoiseDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_TIMESTAMP + " TEXT, " +
                        COLUMN_DECIBEL + " REAL, " +
                        COLUMN_LATITUDE + " REAL, " +
                        COLUMN_LONGITUDE + " REAL)";

        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertNoiseData(String timestamp, double decibel, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_DECIBEL, decibel);
        values.put(COLUMN_LATITUDE, lat);
        values.put(COLUMN_LONGITUDE, lng);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

}
