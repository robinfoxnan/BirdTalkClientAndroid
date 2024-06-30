package com.bird2fish.birdtalksdk.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class BaseDb extends SQLiteOpenHelper {
    private static final String TAG = "BaseDb";

    private static final String DATABASE_NAME_PREFIX = "birdtalk";
    private static final int DATABASE_VERSION = 1;


    private static final Map<String, BaseDb> instances = new HashMap<>();
    private static String uid = "";
    private static Context context = null;

    private BaseDb(Context context, String uid) {
        super(context, DATABASE_NAME_PREFIX + uid + ".db", null, DATABASE_VERSION);
    }

    public static synchronized BaseDb changeToDB(Context context, String uid){
        if (!instances.containsKey(uid)) {
            instances.put(uid, new BaseDb(context.getApplicationContext(), uid));
            BaseDb.context = context;
        }
        BaseDb.uid = uid;
        return instances.get(uid);
    }

    public static synchronized BaseDb getInstance() {
        if ((BaseDb.uid == "") || (BaseDb.context == null)) {
            return null;
        }
        if (!instances.containsKey(BaseDb.uid)) {
            instances.put(BaseDb.uid, new BaseDb(BaseDb.context, BaseDb.uid));
        }
        return instances.get(BaseDb.uid);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Here you would typically handle any upgrade tasks if the schema changes.
        // For simplicity, we won't include it in this simplified version.
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Typically handled similar to onUpgrade.
        // For simplicity, we won't include it in this simplified version.
    }

    private void createTables(SQLiteDatabase db) {
        // Create your tables here
         UserDbHelper.onCreate(db);
    }
}
