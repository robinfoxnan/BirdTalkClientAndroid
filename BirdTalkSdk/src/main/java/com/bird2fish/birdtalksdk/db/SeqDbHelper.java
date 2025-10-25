package com.bird2fish.birdtalksdk.db;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SeqDbHelper {
    private static String TAG = "SeqDbHelper";
    private static final String TABLE_NAME = "msg_seq";
    private static final String COL_ID = "id";
    private static final String COL_SEQ = "seq";

    // 当前缓存的序列号范围 [cacheStart, cacheEnd]
    private static long cacheStart = -1;
    private static long cacheEnd = -1;

    private static  final  String defaultId = "1000";

    private static final int ALLOC_STEP = 20; // 每次取号的步长

    public SeqDbHelper() {
    }


    public static void onCreate(SQLiteDatabase db) {

        // 创建表
        try {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY CHECK (" + COL_ID + " = 1)," +
                COL_SEQ + " INTEGER NOT NULL" +
                ");");

            SeqDbHelper.ensureInitialized(db);

        }catch (SQLException e){
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * 检查表是否已初始化，如果没有就插入起始行
     */
    private static void ensureInitialized(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            // 检查表是否存在记录
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + COL_ID + " = 1", null);
            boolean hasRecord = false;
            if (cursor.moveToFirst()) {
                hasRecord = cursor.getLong(0) > 0;
            }
            if (!hasRecord) {
                // 插入起始行，初始seq从1000开始
                db.execSQL("INSERT OR REPLACE INTO " + TABLE_NAME + " (" + COL_ID + ", " + COL_SEQ + ") VALUES (1, "+ defaultId +")");
                Log.i(TAG, "msg_seq table initialized with start seq = " + defaultId);
            }
        } catch (Exception e) {
            Log.e(TAG, "ensureInitialized failed", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }


    /**
     * 获取下一个消息号（自动缓存一批号，加速）
     */
    public static synchronized long getNextSeq() {
        if (cacheStart < 0 || cacheStart > cacheEnd) {
            allocateBatch();
        }
        return cacheStart++;
    }

    /**
     * 从数据库一次申请一批消息号
     */
    private static void allocateBatch() {
        if (BaseDb.getInstance() == null){
            return;
        }
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        db.beginTransaction();
        try {
            long currentSeq = getCurrentSeq(db);
            long newSeq = currentSeq + ALLOC_STEP;
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COL_SEQ + " = ? WHERE " + COL_ID + " = 1",
                    new Object[]{newSeq});
            db.setTransactionSuccessful();

            cacheStart = currentSeq + 1;
            cacheEnd = newSeq;
            Log.d(TAG, "Allocated seq range: " + cacheStart + " ~ " + cacheEnd);
        } catch (Exception e) {
            Log.e(TAG, "allocateBatch failed", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 读取当前数据库中的序列号
     */
    private static long getCurrentSeq(SQLiteDatabase db) {
        long seq = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COL_SEQ + " FROM " + TABLE_NAME + " WHERE " + COL_ID + " = 1", null);
            if (cursor.moveToFirst()) {
                seq = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return seq;
    }

    /**
     * 重置序列号（用于调试或清空）
     */
    public static synchronized void resetSeq() {
        if (BaseDb.getInstance() == null){
            return;
        }
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();

        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COL_SEQ + " = " + defaultId +" WHERE " + COL_ID + " = 1");
        cacheStart = -1;
        cacheEnd = -1;
        Log.w(TAG, "Sequence reset to "+ defaultId);
    }
}
