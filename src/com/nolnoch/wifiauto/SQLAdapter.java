package com.nolnoch.wifiauto;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLAdapter {
	
	public final static String KEY_LABEL = "label";
    public final static String KEY_LATITUDE = "latitude";
    public final static String KEY_LONGITUDE = "longitude";
    public final static String KEY_STATUS = "status";
    public final static String KEY_ROWID = "_id";

    private static final String TAG = "rainbowSQLAdapter";
    private SQLSetup dbSetup;
    private SQLiteDatabase sqlDb;
	
    private final Context dbContext;
	
	private static final String DATABASE_NAME = "PointsOfDesire";
	private static final String DATABASE_TABLE = "rainbow";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_CREATE =
			"create table rainbow (_id integer primary key autoincrement, "
			+ "label text not null, latitude real not null, "
			+ "longitude real not null, status integer not null);";
	
	private static class SQLSetup extends SQLiteOpenHelper {
		
		public SQLSetup(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS rainbow");
            onCreate(db);

		}
	}
	
	public SQLAdapter(Context context) {
		this.dbContext = context;
	}
	
	public SQLAdapter open() throws SQLException {
		dbSetup = new SQLSetup(dbContext);
		sqlDb = dbSetup.getWritableDatabase();
		return this;
	}
	
	public void close() {
		dbSetup.close();
	}
	
	public long createLocation(String label, double latitude, double longitude, int status) {
	    ContentValues initialValues = new ContentValues();
	    initialValues.put(KEY_LABEL, label);
	    initialValues.put(KEY_LATITUDE, latitude);
	    initialValues.put(KEY_LONGITUDE, longitude);
	    initialValues.put(KEY_STATUS, status);

        return sqlDb.insert(DATABASE_TABLE, null, initialValues);
    }
	
	public boolean deleteLocation(long rowId) {

        return sqlDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
	
	public Cursor fetchAllLocations() {

        return sqlDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_LABEL, KEY_LATITUDE, KEY_LONGITUDE, KEY_STATUS}, null, null, null, null, null);
    }
	
	public Cursor fetchLocation(long rowId) throws SQLException {

        Cursor mCursor =

            sqlDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_LABEL, KEY_LATITUDE, KEY_LONGITUDE, KEY_STATUS}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
	
	public boolean updateLocation(long rowId, String label, double latitude, double longitude, int status) {
        ContentValues args = new ContentValues();
        args.put(KEY_LABEL, label);
        args.put(KEY_LATITUDE, latitude);
        args.put(KEY_LONGITUDE, longitude);
        args.put(KEY_STATUS, status);

        return sqlDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

}
