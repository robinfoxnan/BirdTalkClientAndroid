package com.bird2fish.birdtalksdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.bird2fish.birdtalksdk.model.MessageContent;
import com.bird2fish.birdtalksdk.model.MessageData;
import com.bird2fish.birdtalksdk.model.MessageStatus;
import com.bird2fish.birdtalksdk.model.Topic;
import com.bird2fish.birdtalksdk.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TopicDbHelper {
    // 表名
    public static final String TABLE_PCHAT = "pchat";
    public static final String TABLE_PCHAT_UNREAD  = "pchat_unread";
    public static final String TABLE_PTOPIC = "p_topic";
    public static final String TABLE_GTOPIC = "g_topic";

    // 列
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TID = "tid";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_SEND_ID = "send_id";
    public static final String COLUMN_DEV_ID = "dev_id";
    public static final String COLUMN_IO = "io";
    public static final String COLUMN_MSG_TYPE = "msg_type";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_IS_PLAIN = "is_plain";
    public static final String COLUMN_TM = "tm";
    public static final String COLUMN_TM1 = "tm1";
    public static final String COLUMN_TM2 = "tm2";
    public static final String COLUMN_TM3 = "tm3";
    public static final String COLUMN_CRYPT_TYPE = "crypt_type";
    public static final String COLUMN_PRINT = "print";
    public static final String COLUMN_STATUS = "status";

    // topic 使用

    public static final String COLUMN_SYNC_ID = "sync_id";
    public static final String COLUMN_READ_ID = "read_id";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_VISIBLE = "visible";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_ICON = "icon";

    private static final String SQL_CREATE_PTOPIC_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PTOPIC + " (" +
                    COLUMN_TID + " INTEGER PRIMARY KEY," +
                    COLUMN_SYNC_ID + " INTEGER," +
                    COLUMN_READ_ID + " INTEGER," +
                    COLUMN_TYPE + " INTEGER," +
                    COLUMN_VISIBLE + " INTEGER," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_ICON + " TEXT);";

    private static final String SQL_CREATE_GTOPIC_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_GTOPIC + " (" +
                    COLUMN_TID + " INTEGER PRIMARY KEY," +
                    COLUMN_SYNC_ID + " INTEGER," +
                    COLUMN_READ_ID + " INTEGER," +
                    COLUMN_TYPE + " INTEGER," +
                    COLUMN_VISIBLE + " INTEGER," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_ICON + " TEXT);";


    private static final String SQL_CREATE_PCHAT_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PCHAT + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_TID + " INTEGER," +
                    COLUMN_UID + " INTEGER," +
                    COLUMN_SEND_ID + " INTEGER," +
                    COLUMN_DEV_ID + " TEXT," +
                    COLUMN_IO + " INTEGER," +
                    COLUMN_MSG_TYPE + " TEXT," +
                    COLUMN_DATA + " BLOB," +
                    COLUMN_IS_PLAIN + " INTEGER," +
                    COLUMN_TM + " INTEGER," +
                    COLUMN_TM1 + " INTEGER," +
                    COLUMN_TM2 + " INTEGER," +
                    COLUMN_TM3 + " INTEGER," +
                    COLUMN_CRYPT_TYPE + " TEXT," +
                    COLUMN_PRINT + " INTEGER," +
                    COLUMN_STATUS + " TEXT," +  // 注意这里多一个逗号，因为后面要加索引
                    "INDEX idx_tm (" + COLUMN_TM + "));"; // 为 TM 字段创建名为 idx_tm 的索引

    private static final String SQL_CREATE_PCHAT_UNREAD_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PCHAT_UNREAD + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_UID + " INTEGER," +
                    COLUMN_IO + " INTEGER);";

    public static void onCreate(SQLiteDatabase db) {
        // 创建表
        try {
            db.execSQL(SQL_CREATE_PCHAT_TABLE);
            db.execSQL(SQL_CREATE_PCHAT_UNREAD_TABLE);
            db.execSQL(SQL_CREATE_PTOPIC_TABLE);
            db.execSQL(SQL_CREATE_GTOPIC_TABLE);
        }catch (SQLException e){
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    public static String getPChatName(long fid){
        String tableName = "pchat_" + fid;
        return tableName;
    }

    public static String getGChatName(long gid){
        String tableName = "gchat_" + gid;
        return tableName;
    }

    public static void createPChatTable(long fid){

        // 0 标记一个特殊的用户，是私聊队列的同步位置信息
        if (fid == 0){
            return;
        }
        String tableName = getPChatName(fid);
        boolean has = BaseDb.getInstance().hasPTable(tableName);
        if (has){
            return;
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY);";

        BaseDb.getInstance().getWritableDatabase().execSQL(sql);
        BaseDb.getInstance().setPTable(tableName);
    }

    public static void createGChatTable(long gid){
        String tableName = getGChatName(gid);
        boolean has = BaseDb.getInstance().hasGTable(tableName);
        if (has){
            return;
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_TID + " INTEGER," +
                COLUMN_UID + " INTEGER," +
                COLUMN_SEND_ID + " INTEGER," +
                COLUMN_DEV_ID + " TEXT," +
                COLUMN_IO + " INTEGER," +
                COLUMN_MSG_TYPE + " TEXT," +
                COLUMN_DATA + " BLOB," +
                COLUMN_IS_PLAIN + " INTEGER," +
                COLUMN_TM + " INTEGER," +
                COLUMN_TM1 + " INTEGER," +
                COLUMN_TM2 + " INTEGER," +
                COLUMN_TM3 + " INTEGER," +
                COLUMN_CRYPT_TYPE + " TEXT," +
                COLUMN_PRINT + " INTEGER," +
                COLUMN_STATUS + " TEXT);";

        BaseDb.getInstance().getWritableDatabase().execSQL(sql);
        BaseDb.getInstance().setGTable(tableName);
    }
    /////////////////////////////////////////////////////////////////////////////////
    // 第二次插入的时候，需要删除之前的消息
    public static boolean  insertPChatMsgAgain(MessageData messageData) {
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        ContentValues values = new ContentValues();

        // 基础数据表；
        values.put("id", messageData.getId());
        values.put("tid", messageData.getTid());
        values.put("uid", messageData.getUid());
        values.put("send_id", messageData.getSendId());
        values.put("dev_id", messageData.getDevId());
        values.put("io", messageData.getIo());
        values.put("msg_type", messageData.getMsgType());
        values.put("data", messageData.getData());
        values.put("is_plain", messageData.getIsPlain());
        values.put("tm", messageData.getTm());
        values.put("tm1", messageData.getTm1());
        values.put("tm2", messageData.getTm2());
        values.put("tm3", messageData.getTm3());
        values.put("crypt_type", messageData.getCryptType());
        values.put("print", messageData.getPrint());
        values.put("status", messageData.getStatus());

        // 会话表
        ContentValues values2 = new ContentValues();
        values2.put("id", messageData.getId());

        // 对方未读，或者自己未读
        ContentValues values3 = new ContentValues();
        values3.put("id", messageData.getId());
        values3.put("uid", messageData.getUid());
        values3.put("io", messageData.getIo());

        long row1 = 0;
        long row2 = 0;
        long row3 = 0;

        boolean ret = false;
        try {
            db.beginTransaction();

            row1 = db.insertWithOnConflict(TABLE_PCHAT, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            long tid = messageData.getTid();
            createPChatTable(tid);
            row2 = db.insertWithOnConflict(getPChatName(tid), null, values2, SQLiteDatabase.CONFLICT_REPLACE);

            if (messageData.getTm3() == 0L)
            {
                row3 = db.insertWithOnConflict(TABLE_PCHAT_UNREAD, null, values3, SQLiteDatabase.CONFLICT_REPLACE);
            }
            // 删除之前的行
            String selection = "id = ?";
            String[] selectionArgs = { String.valueOf(messageData.getSendId()) };
            int deletedRows = db.delete(TABLE_PCHAT, selection, selectionArgs);

            deletedRows = db.delete(getPChatName(tid), selection, selectionArgs);

            deletedRows = db.delete(TABLE_PCHAT_UNREAD, selection, selectionArgs);

            // 所有操作成功
            db.setTransactionSuccessful();

            if (row1 != -1 && row2 != -1 && row3 != -1) {
                ret = true;
            } else {
                // 发生了至少一个插入失败的情况
                //Log.e("Transaction", "Failed to insert or replace data");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            db.endTransaction();
        }

        return ret;
    }
    // 收发P2P数据，同时写3个表，pchat, pchat..., pchat_unread
    public static boolean  insertPChatMsg(MessageData messageData) {
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        ContentValues values = new ContentValues();

        // 基础数据表；
        values.put("id", messageData.getId());
        values.put("tid", messageData.getTid());
        values.put("uid", messageData.getUid());
        values.put("send_id", messageData.getSendId());
        values.put("dev_id", messageData.getDevId());
        values.put("io", messageData.getIo());
        values.put("msg_type", messageData.getMsgType());
        values.put("data", messageData.getData());
        values.put("is_plain", messageData.getIsPlain());
        values.put("tm", messageData.getTm());
        values.put("tm1", messageData.getTm1());
        values.put("tm2", messageData.getTm2());
        values.put("tm3", messageData.getTm3());
        values.put("crypt_type", messageData.getCryptType());
        values.put("print", messageData.getPrint());
        values.put("status", messageData.getStatus());

        // 会话表
        ContentValues values2 = new ContentValues();
        values2.put("id", messageData.getId());

        // 对方未读，或者自己未读
        ContentValues values3 = new ContentValues();
        values3.put("id", messageData.getId());
        values3.put("uid", messageData.getUid());
        values3.put("io", messageData.getIo());

        long row1 = 0;
        long row2 = 0;
        long row3 = 0;

        boolean ret = false;
        try {
            db.beginTransaction();

            row1 = db.insertWithOnConflict(TABLE_PCHAT, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            long tid = messageData.getTid();
            createPChatTable(tid);
            row2 = db.insertWithOnConflict(getPChatName(tid), null, values2, SQLiteDatabase.CONFLICT_REPLACE);

            row3 = db.insertWithOnConflict(TABLE_PCHAT_UNREAD, null, values3, SQLiteDatabase.CONFLICT_REPLACE);

            if (row1 != -1 && row2 != -1 && row3 != -1) {
                // 所有操作成功
                db.setTransactionSuccessful();
                ret = true;
            } else {
                // 发生了至少一个插入失败的情况
                //Log.e("Transaction", "Failed to insert or replace data");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            db.endTransaction();
        }

        return ret;
    }

    public static boolean  insertPChatMsgBatch(List<MessageData> messageDataList, LinkedList<MessageData> ret) {
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();


        // 件检查表是否存在
        HashMap<Long,Long> tempMap = new HashMap<Long,Long>();
        for (MessageData item : messageDataList){
            if (!tempMap.containsKey(item.getTid())){
                tempMap.put(item.getTid(), 1L);
            }
        }
        for (Long tid :tempMap.keySet())
        {
            createPChatTable(tid);
        }


        long row1 = 0;
        long row2 = 0;
        long row3 = 0;

        boolean isAllOk = true;
        try {
            db.beginTransaction();
            for (MessageData messageData :  messageDataList){
                // 基础数据表；
                ContentValues values = new ContentValues();
                values.put("id", messageData.getId());
                values.put("tid", messageData.getTid());
                values.put("uid", messageData.getUid());
                values.put("send_id", messageData.getSendId());
                values.put("dev_id", messageData.getDevId());
                values.put("io", messageData.getIo());
                values.put("msg_type", messageData.getMsgType());
                values.put("data", messageData.getData());
                values.put("is_plain", messageData.getIsPlain());
                values.put("tm", messageData.getTm());
                values.put("tm1", messageData.getTm1());
                values.put("tm2", messageData.getTm2());
                values.put("tm3", messageData.getTm3());
                values.put("crypt_type", messageData.getCryptType());
                values.put("print", messageData.getPrint());
                values.put("status", messageData.getStatus());

                // 会话表
                ContentValues values2 = new ContentValues();
                values2.put("id", messageData.getId());

                // 对方未读，或者自己未读
                ContentValues values3 = new ContentValues();
                values3.put("id", messageData.getId());
                values3.put("uid", messageData.getUid());
                values3.put("io", messageData.getIo());

                row1 = db.insertWithOnConflict(TABLE_PCHAT, null, values, SQLiteDatabase.CONFLICT_REPLACE);

                long tid = messageData.getTid();

                row2 = db.insertWithOnConflict(getPChatName(tid), null, values2, SQLiteDatabase.CONFLICT_REPLACE);

                row3 = db.insertWithOnConflict(TABLE_PCHAT_UNREAD, null, values3, SQLiteDatabase.CONFLICT_REPLACE);

                if (row1 != -1 && row2 != -1 && row3 != -1) {
                    // 所有操作成功
                } else {
                    // 发生了至少一个插入失败的情况
                    //Log.e("Transaction", "Failed to insert or replace data");
                    isAllOk = false;
                    ret.add(messageData);
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            isAllOk = false;
            //ret.add(currentMessageData);
            e.printStackTrace();
        }finally {
            db.endTransaction();
        }

        return isAllOk;
    }


    // 方法用于从数据库中获取从指定 id 开始的若干条消息数据，并返回 MessageData 对象列表
    public  static List<MessageData> getPChatFromTableId(int startId, int limit, boolean forward) {
        List<MessageData> messages = new ArrayList<>();
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        Cursor cursor = null;

        try {
            // SQL 查询语句
            String sql = "";
            if (forward){
                sql = "SELECT * FROM pchat WHERE id >= ? ORDER BY id ASC LIMIT ?";
            }else{
                sql = "SELECT * FROM pchat WHERE id <= ? ORDER BY id ASC LIMIT ?";
            }
            String[] selectionArgs = {String.valueOf(startId), String.valueOf(limit)};
            cursor = db.rawQuery(sql, selectionArgs);

            // 遍历 Cursor 并映射到 MessageData 对象列表
            if (cursor != null) {
                while (cursor.moveToNext()) {

                    // 从 Cursor 中提取数据并创建 MessageData 对象
                    MessageData message = new MessageData();
                    message.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    message.setTid(cursor.getLong(cursor.getColumnIndexOrThrow("tid")));
                    message.setUid(cursor.getLong(cursor.getColumnIndexOrThrow("uid")));
                    message.setSendId(cursor.getLong(cursor.getColumnIndexOrThrow("send_id")));
                    message.setDevId(cursor.getString(cursor.getColumnIndexOrThrow("dev_id")));
                    message.setIo(cursor.getInt(cursor.getColumnIndexOrThrow("io")));
                    message.setMsgType(cursor.getString(cursor.getColumnIndexOrThrow("msg_type")));
                    message.setData(cursor.getBlob(cursor.getColumnIndexOrThrow("data")));
                    message.setIsPlain(cursor.getInt(cursor.getColumnIndexOrThrow("is_plain")));
                    message.setTm(cursor.getLong(cursor.getColumnIndexOrThrow("tm")));
                    message.setTm1(cursor.getLong(cursor.getColumnIndexOrThrow("tm1")));
                    message.setTm2(cursor.getLong(cursor.getColumnIndexOrThrow("tm2")));
                    message.setTm3(cursor.getLong(cursor.getColumnIndexOrThrow("tm3")));
                    message.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow("crypt_type")));
                    message.setPrint(cursor.getInt(cursor.getColumnIndexOrThrow("print")));
                    message.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));

                    // 将 MessageData 对象添加到列表中
                    messages.add(message);
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭 cursor
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    // 方法用于从数据库中获取各个 uid 的未读消息数，并以 Map 形式返回
    public  static Map<Long, Long> getUnreadCountsByUid() {
        Map<Long, Long> unreadCounts = new HashMap<>();
        SQLiteDatabase db = BaseDb.getInstance().getReadableDatabase();
        Cursor cursor = null;

        try {
            // SQL 查询语句
            String query = "SELECT uid, COUNT(*) AS count FROM pchat_unread WHERE io = 1 GROUP BY uid";
            cursor = db.rawQuery(query, null);

            // 遍历结果集并添加到 Map 中
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Long uid = cursor.getLong(cursor.getColumnIndexOrThrow("uid"));
                    Long count = cursor.getLong(cursor.getColumnIndexOrThrow("count"));
                    unreadCounts.put(uid, count);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭 cursor
            if (cursor != null) {
                cursor.close();
            }
        }

        return unreadCounts;
    }

    // 从未读数据中查询所有没有回执的消息
    public static  List<MessageData> getMessagesUnReply() {
        List<MessageData> messages = new ArrayList<>();
        SQLiteDatabase db = BaseDb.getInstance().getReadableDatabase();
        Cursor cursor = null;

        try {
            String sqlQuery = "SELECT pchat.* " +
                    "FROM pchat_unread " +
                    "LEFT JOIN pchat ON pchat_unread.id = pchat.id " +
                    "WHERE pchat_unread.io = ?  ORDER BY pchat.id ASC ";

            String[] selectionArgs = { "0" };

            cursor = db.rawQuery(sqlQuery, selectionArgs);

            // Iterate through the cursor and populate the MessageData list
            while (cursor.moveToNext()) {
                MessageData message = new MessageData();
                message.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                message.setTid(cursor.getLong(cursor.getColumnIndexOrThrow("tid")));
                message.setUid(cursor.getLong(cursor.getColumnIndexOrThrow("uid")));
                message.setSendId(cursor.getLong(cursor.getColumnIndexOrThrow("send_id")));
                message.setDevId(cursor.getString(cursor.getColumnIndexOrThrow("dev_id")));
                message.setIo(cursor.getInt(cursor.getColumnIndexOrThrow("io")));
                message.setMsgType(cursor.getString(cursor.getColumnIndexOrThrow("msg_type")));
                message.setData(cursor.getBlob(cursor.getColumnIndexOrThrow("data")));
                message.setIsPlain(cursor.getInt(cursor.getColumnIndexOrThrow("is_plain")));
                message.setTm(cursor.getLong(cursor.getColumnIndexOrThrow("tm")));
                message.setTm1(cursor.getLong(cursor.getColumnIndexOrThrow("tm1")));
                message.setTm2(cursor.getLong(cursor.getColumnIndexOrThrow("tm2")));
                message.setTm3(cursor.getLong(cursor.getColumnIndexOrThrow("tm3")));
                message.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow("crypt_type")));
                message.setPrint(cursor.getInt(cursor.getColumnIndexOrThrow("print")));
                message.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));

                messages.add(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }


    // 查询当前所有消息最后一条数据
    public static long getPChatLastTm(){
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        Cursor cursor = null;
        String sqlQuery = "SELECT pchat.* LIMIT";

        cursor = db.rawQuery("SELECT MAX(tm) AS max_tm FROM pchat", null);
        long maxTm = 0; // 默认值

        if (cursor.moveToFirst()) {
            // 读取 max_tm 列的值，若为 NULL 则返回 -1（可根据需求调整默认值）
            maxTm = cursor.getLong(cursor.getColumnIndexOrThrow("max_tm"));
        }

        cursor.close(); // 记得关闭 Cursor

        return maxTm;

    }

    public static long getPChatLastId(){
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        Cursor cursor = null;

        cursor = db.rawQuery("SELECT MAX(id) AS max_id FROM pchat", null);
        long maxTm = 0; // 默认值

        if (cursor.moveToFirst()) {
            // 读取 max_tm 列的值，若为 NULL 则返回 -1（可根据需求调整默认值）
            maxTm = cursor.getLong(cursor.getColumnIndexOrThrow("max_id"));
        }

        cursor.close(); // 记得关闭 Cursor

        return maxTm;

    }
    // 查询某个会话中的数据
    public  static List<MessageData> getPChatMessagesById(long fid, long startId, int limit, boolean forward) {
        List<MessageData> messages = new ArrayList<>();

        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        Cursor cursor = null;
        String tableName = getPChatName(fid);
        createPChatTable(fid);

        try {

            // 反向
            String sqlQuery = "SELECT pchat.* " +
                    " FROM " + tableName +
                    " LEFT JOIN pchat ON " + tableName + ".id = pchat.id " +
                    " WHERE " + tableName + ".id <= ? " +
                    "ORDER BY pchat.tm ASC " +
                    " LIMIT ?";

            if (forward){
                sqlQuery = "SELECT pchat.* " +
                        " FROM " + tableName +
                        " LEFT JOIN pchat ON " + tableName + ".id = pchat.id " +
                        " WHERE " + tableName + ".id >= ? " +
                        "ORDER BY pchat.tm ASC " +
                        " LIMIT ?";
            }


            String[] selectionArgs = { String.valueOf(startId), String.valueOf(limit) };

            cursor = db.rawQuery(sqlQuery, selectionArgs);

            // Iterate through the cursor and populate the MessageData list
            while (cursor.moveToNext()) {
                MessageData message = new MessageData();
                message.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                message.setTid(cursor.getLong(cursor.getColumnIndexOrThrow("tid")));
                message.setUid(cursor.getLong(cursor.getColumnIndexOrThrow("uid")));
                message.setSendId(cursor.getLong(cursor.getColumnIndexOrThrow("send_id")));
                message.setDevId(cursor.getString(cursor.getColumnIndexOrThrow("dev_id")));
                message.setIo(cursor.getInt(cursor.getColumnIndexOrThrow("io")));
                message.setMsgType(cursor.getString(cursor.getColumnIndexOrThrow("msg_type")));
                message.setData(cursor.getBlob(cursor.getColumnIndexOrThrow("data")));
                message.setIsPlain(cursor.getInt(cursor.getColumnIndexOrThrow("is_plain")));
                message.setTm(cursor.getLong(cursor.getColumnIndexOrThrow("tm")));
                message.setTm1(cursor.getLong(cursor.getColumnIndexOrThrow("tm1")));
                message.setTm2(cursor.getLong(cursor.getColumnIndexOrThrow("tm2")));
                message.setTm3(cursor.getLong(cursor.getColumnIndexOrThrow("tm3")));
                message.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow("crypt_type")));
                message.setPrint(cursor.getInt(cursor.getColumnIndexOrThrow("print")));
                message.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));

                messages.add(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }


    // 从未读表中删除一行；
    public  static void deleteFromPChatUnread(long id) {
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();

        try {
            // 删除操作
            db.delete(TABLE_PCHAT_UNREAD, "id = ?", new String[]{String.valueOf(id)});

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 结束事务
        }
    }

    // 清理历史数据
    public static void cleanPChatHistory(long fid, long id){

        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        Cursor cursor = null;
        String tableName = getPChatName(fid);

        String whereClause = "id < ?";
        String[] whereArgs = { String.valueOf(id) };

        // 会话表
        createPChatTable(fid);
        int rowsDeleted = db.delete(tableName, whereClause, whereArgs);

        // 大表，
        String whereClause1 = "id < ? AND tid = ?";
        String[] whereArgs1 = { String.valueOf(id), String.valueOf(fid) };
        rowsDeleted = db.delete(TABLE_PCHAT, whereClause, whereArgs);

        // 未读表
        rowsDeleted = db.delete(TABLE_PCHAT_UNREAD, whereClause, whereArgs);

    }

    // 更新私聊中回执等信息
//    public static boolean updatePChatReply(MessageData msg){
//        ContentValues values = new ContentValues();
//        String status = MessageStatus.SENDING.name();
//        values.put("tm1", msg.getTm1());
//        values.put("tm2", msg.getTm2());
//        values.put("tm3", msg.getTm3());
//
//        if (msg.getTm1() > 0){
//            status = MessageStatus.OK.name();
//        }
//
//        if (msg.getTm2() > 0){
//            status = MessageStatus.RECV.name();
//        }
//
//        if (msg.getTm3() > 0){
//            status = MessageStatus.SEEN.name();
//        }
//        values.put("status", status);
//
//        String whereClause = "id = ?";
//        String[] whereArgs = { String.valueOf(msg.getId()) };
//
//        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
//        int rowsUpdated = db.update(TABLE_PCHAT, values, whereClause, whereArgs);
//        if (rowsUpdated > 0){
//            return true;
//        }else{
//            return false;
//        }
//    }

    // 批量的处理同步得到的数据
    public static boolean updatePChatReplyBatch(List<MessageContent> messageContentList){
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();

        long row1 = 0;
        long row2 = 0;
        long row3 = 0;

        boolean isAllOk = true;
        try {
            db.beginTransaction();

            for (MessageContent message : messageContentList) {

                ContentValues values = new ContentValues();
                String status = MessageStatus.SENDING.name();

                if (message.getTm1() > 0){
                    status = MessageStatus.OK.name();
                    values.put("tm1", message.getTm1());
                }

                if (message.getTm2() > 0){
                    status = MessageStatus.RECV.name();
                    values.put("tm2", message.getTm2());
                }

                if (message.getTm3() > 0){
                    status = MessageStatus.SEEN.name();
                    values.put("tm3", message.getTm3());
                }
                values.put("status", status);

                String whereClause = "id = ?";
                String[] whereArgs = { String.valueOf(message.getMsgId()) };

                int rowsUpdated = db.update(TABLE_PCHAT, values, whereClause, whereArgs);
                if (rowsUpdated > 0){

                }else{
                    isAllOk = false;
                }
            }

            db.setTransactionSuccessful();
        }catch (Exception e){
            isAllOk = false;
        }finally {
            db.endTransaction();
        }
        return isAllOk;
    }

    public static boolean updatePChatReply(long msgId, long tm1, long tm2, long tm3){
        ContentValues values = new ContentValues();
        String status = MessageStatus.SENDING.name();

        if (tm1 > 0){
            status = MessageStatus.OK.name();
            values.put("tm1", tm1);
        }

        if (tm2 > 0){
            status = MessageStatus.RECV.name();
            values.put("tm2", tm2);
        }

        if (tm3 > 0){
            status = MessageStatus.SEEN.name();
            values.put("tm3", tm3);
        }
        values.put("status", status);

        String whereClause = "id = ?";
        String[] whereArgs = { String.valueOf(msgId) };

        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        int rowsUpdated = db.update(TABLE_PCHAT, values, whereClause, whereArgs);
        if (rowsUpdated > 0){
            return true;
        }else{
            return false;
        }
    }

    // fail 之类的，timeout
    public static boolean updatePChatStatus(MessageData msg){
        ContentValues values = new ContentValues();
        values.put("status", msg.getStatus());

        String whereClause = "id = ?";
        String[] whereArgs = { String.valueOf(msg.getId()) };

        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        int rowsUpdated = db.update(TABLE_PCHAT, values, whereClause, whereArgs);
        if (rowsUpdated > 0){
            return true;
        }else{
            return false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    public static boolean  insertGChatMsg(MessageData messageData) {
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        ContentValues values = new ContentValues();

        createGChatTable(messageData.getTid());
        String tableName = getGChatName(messageData.getTid());
        // 基础数据表；
        values.put("id", messageData.getId());
        values.put("tid", messageData.getTid());
        values.put("uid", messageData.getUid());
        values.put("send_id", messageData.getSendId());
        values.put("dev_id", messageData.getDevId());
        values.put("io", messageData.getIo());
        values.put("msg_type", messageData.getMsgType());
        values.put("data", messageData.getData());
        values.put("is_plain", messageData.getIsPlain());
        values.put("tm", messageData.getTm());
        values.put("tm1", messageData.getTm1());
        values.put("tm2", messageData.getTm2());
        values.put("tm3", messageData.getTm3());
        values.put("crypt_type", messageData.getCryptType());
        values.put("print", messageData.getPrint());
        values.put("status", messageData.getStatus());

        long row = 0;

        boolean ret = false;
        try {
            row = db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            if (row != -1) {
                ret = true;
            } else {
                // 发生了至少一个插入失败的情况
                //Log.e("Transaction", "Failed to insert or replace data");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    // 针对单条的群聊数据，需要识别是否存在
    public static boolean insertOrUpdateGChatMsg(MessageData messageData) {
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        ContentValues values = new ContentValues();

        createGChatTable(messageData.getTid());
        String tableName = getGChatName(messageData.getTid());
        // 基础数据表；
        values.put("id", messageData.getId());
        values.put("tid", messageData.getTid());
        values.put("uid", messageData.getUid());
        values.put("send_id", messageData.getSendId());
        values.put("dev_id", messageData.getDevId());
        values.put("io", messageData.getIo());
        values.put("msg_type", messageData.getMsgType());
        values.put("data", messageData.getData());
        values.put("is_plain", messageData.getIsPlain());
        values.put("tm", messageData.getTm());
        values.put("tm1", messageData.getTm1());
        values.put("tm2", messageData.getTm2());
        values.put("tm3", messageData.getTm3());
        values.put("crypt_type", messageData.getCryptType());
        values.put("print", messageData.getPrint());
        values.put("status", messageData.getStatus());

        long row = 0;

        boolean ret = false;

        try {
            // 1. 先查询数据是否已存在（假设id是唯一标识）
            String query = "SELECT id FROM " + tableName + " WHERE id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messageData.getId())});
            boolean exists = cursor.moveToFirst();
            cursor.close();

            if (exists)
            {
                messageData.setStatus(MessageStatus.CONTINUOUS.name());
            }else{
                messageData.setStatus(MessageStatus.FORWARD.name());
            }
            // 2. 执行插入或替换
            row = db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            if (row != -1) {
                ret = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }


    // 查询返回的数据，除了第一条以外都是一起批量插入的
    public static boolean insertGChatDataBatch(long tid, List<MessageData> lst, List<MessageData> ret){

        if (lst == null || lst.isEmpty()) {
            return true; // 空列表无需处理，返回成功
        }

        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();

        createGChatTable(tid);
        String tableName = getGChatName(tid);

        boolean isAllSuccess = true; // 标记是否全部成功

        long row = 0;
        db.beginTransaction();
        try {
            for (MessageData messageData : lst) {
                // 执行查询+插入逻辑
                ContentValues values = new ContentValues();

                // 基础数据表；
                values.put("id", messageData.getId());
                values.put("tid", messageData.getTid());
                values.put("uid", messageData.getUid());
                values.put("send_id", messageData.getSendId());
                values.put("dev_id", messageData.getDevId());
                values.put("io", messageData.getIo());
                values.put("msg_type", messageData.getMsgType());
                values.put("data", messageData.getData());
                values.put("is_plain", messageData.getIsPlain());
                values.put("tm", messageData.getTm());
                values.put("tm1", messageData.getTm1());
                values.put("tm2", messageData.getTm2());
                values.put("tm3", messageData.getTm3());
                values.put("crypt_type", messageData.getCryptType());
                values.put("print", messageData.getPrint());
                values.put("status", messageData.getStatus());
                row = db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);

                if (row == -1) {
                    isAllSuccess = false;
                    ret.add(messageData);
                }
                // 只有全部成功时，才标记事务成功
                //if (isAllSuccess)
                {
                    db.setTransactionSuccessful();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            isAllSuccess = false; // 异常时标记失败
        } finally {
            db.endTransaction();
        }

        return isAllSuccess;
    }
    public static boolean updateGChatReply(long msgId, long tm1){
        ContentValues values = new ContentValues();
        String status = MessageStatus.SENDING.name();

        if (tm1 > 0){
            status = MessageStatus.OK.name();
            values.put("tm1", tm1);
        }

//        if (tm2 > 0){
//            status = MessageStatus.RECV.name();
//            values.put("tm2", tm2);
//        }
//
//        if (tm3 > 0){
//            status = MessageStatus.SEEN.name();
//            values.put("tm3", tm3);
//        }
        values.put("status", status);

        String whereClause = "id = ?";
        String[] whereArgs = { String.valueOf(msgId) };

        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        int rowsUpdated = db.update(TABLE_GTOPIC, values, whereClause, whereArgs);
        if (rowsUpdated > 0){
            return true;
        }else{
            return false;
        }
    }

    // 更新组中某一条的状态标记
    // forward_loading： 需要正向加载
    // loaded: 加载完毕
    // backward_loading：需要反向加载
    // sending:发送中，服务器未回执；
    // fail：超时了，网络故障，或者其他原因
    // "" 发送正常的，或者接收连续的消息
    public static boolean updateGChatStatus(long gid, long id, String status){
        ContentValues values = new ContentValues();
        values.put("status", status);

        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(id) };

        String tableName = getGChatName(gid);
        createGChatTable(gid);

        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        int rowsUpdated = db.update(tableName, values, selection, selectionArgs);
        if (rowsUpdated > 0){
            return true;
        }

        return false;
    }


    public static int getGChatCount(long gid) {
        int count = 0;
        Cursor cursor = null;
        try {
            String query = "SELECT COUNT(*) FROM " + getGChatName(gid);
            cursor = BaseDb.getInstance().getWritableDatabase().rawQuery(query, null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    // 正向或者反向查找数据
    public  static List<MessageData> getGChatMessagesById(long gid, long startId, int limit, boolean forward) {
        List<MessageData> messages = new ArrayList<>();
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        Cursor cursor = null;
        String tableName = getGChatName(gid);
        createGChatTable(gid);

        try {
            // SQL 查询语句
            String sql = "";
            if (forward){
                sql = "SELECT * FROM " + tableName + " WHERE id >= ? ORDER BY id ASC LIMIT ?";
            }else{
                sql = "SELECT * FROM " + tableName + " WHERE id <= ? ORDER BY id DESC LIMIT ?";
            }
            String[] selectionArgs = {String.valueOf(startId), String.valueOf(limit)};
            cursor = db.rawQuery(sql, selectionArgs);

            // 遍历 Cursor 并映射到 MessageData 对象列表
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // 从 Cursor 中提取数据并创建 MessageData 对象
                    MessageData message = new MessageData();
                    message.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    message.setTid(cursor.getLong(cursor.getColumnIndexOrThrow("tid")));
                    message.setUid(cursor.getLong(cursor.getColumnIndexOrThrow("uid")));
                    message.setSendId(cursor.getLong(cursor.getColumnIndexOrThrow("send_id")));
                    message.setDevId(cursor.getString(cursor.getColumnIndexOrThrow("dev_id")));
                    message.setIo(cursor.getInt(cursor.getColumnIndexOrThrow("io")));
                    message.setMsgType(cursor.getString(cursor.getColumnIndexOrThrow("msg_type")));
                    message.setData(cursor.getBlob(cursor.getColumnIndexOrThrow("data")));
                    message.setIsPlain(cursor.getInt(cursor.getColumnIndexOrThrow("is_plain")));
                    message.setTm(cursor.getLong(cursor.getColumnIndexOrThrow("tm")));
                    message.setTm1(cursor.getLong(cursor.getColumnIndexOrThrow("tm1")));
                    message.setTm2(cursor.getLong(cursor.getColumnIndexOrThrow("tm2")));
                    message.setTm3(cursor.getLong(cursor.getColumnIndexOrThrow("tm3")));
                    message.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow("crypt_type")));
                    message.setPrint(cursor.getInt(cursor.getColumnIndexOrThrow("print")));
                    message.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));

                    // 将 MessageData 对象添加到列表中
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭 cursor
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    // 清理历史数据
    public static void cleanGChatHistory(long gid, long id){

        SQLiteDatabase db = BaseDb.getInstance().getReadableDatabase();
        Cursor cursor = null;
        String tableName = getGChatName(gid);
        createGChatTable(gid);


        String whereClause = "id < ?";
        String[] whereArgs = { String.valueOf(id) };

        int rowsDeleted = db.delete(tableName, whereClause, whereArgs);
    }

    // 删除某个表
    public static boolean removeGChat(long gid){

        String tableName = getGChatName(gid);
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();

        String dropTableSql = "DROP TABLE IF EXISTS " + tableName;
        db.execSQL(dropTableSql);

        // 重新创建空表
        createGChatTable(gid);
        return true;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    private static  boolean insertOrReplaceTopic(Topic topic, String tableName) {
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tid", topic.getTid());
        values.put("sync_id", topic.getSyncId());
        values.put("read_id", topic.getReadId());
        values.put("type", topic.getType());
        values.put("visible", topic.getVisible());
        values.put("title", topic.getTitle());
        values.put("icon", topic.getIcon());

        long rowId = 0;
        try {
            rowId = db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }catch (Exception e){
            e.printStackTrace();
        }

        if (rowId == -1) {
            // 插入或替换失败
            return false;
        } else {
            // 插入或替换成功
        }
        return true;
    }

    // 插入时候自动建表
    public  static  boolean insertOrReplacePTopic(Topic topic) {
        createPChatTable(topic.getTid());
        return insertOrReplaceTopic(topic, TABLE_PTOPIC);
    }

    // 自动建表
    public  static boolean insertOrReplaceGTopic(Topic topic) {
        createGChatTable(topic.getTid());
        return insertOrReplaceTopic(topic, TABLE_GTOPIC);
    }


    // 删除数据
    private static  boolean deleteTopic(long tid, String tableName) {
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        String selection = "tid = ?";
        String[] selectionArgs = { String.valueOf(tid) };
        int deletedRows = db.delete(tableName, selection, selectionArgs);

        if (deletedRows > 0) {
            // 删除成功
            return true;
        } else {
            // 没有匹配的行被删除
        }
        return false;
    }

    public  static boolean deleteFromPTopic(long tid){
        return deleteTopic(tid, TABLE_PTOPIC);
    }

    public  static void deleteFromPTopic(){
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        String sql = "delete from " + TABLE_PTOPIC + " where 1";

        db.execSQL(sql);
    }

    public static void clearPChatData(Long tid){
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        String tableName = getPChatName(tid);
        String sql = "delete from " + tableName + " where 1";

        db.execSQL(sql);
    }

    public static void clearPChatData(){
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        String sql = "delete from " + TABLE_PCHAT + " where 1";
        db.execSQL(sql);

        sql = "delete from " + TABLE_PCHAT_UNREAD + " where 1";
        db.execSQL(sql);
    }

    public  static boolean deleteFromGTopic(long tid){
        return deleteTopic(tid, TABLE_PTOPIC);
    }

    private  static List<Topic> getAllTopics(String tableName) {
        List<Topic> topics = new ArrayList<>();
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        String sql = "SELECT * FROM " + tableName;
        Cursor cursor = db.rawQuery(sql, null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    Topic topic = new Topic();
                    topic.setTid(cursor.getLong(cursor.getColumnIndexOrThrow("tid")));
                    topic.setSyncId(cursor.getLong(cursor.getColumnIndexOrThrow("sync_id")));
                    topic.setReadId(cursor.getLong(cursor.getColumnIndexOrThrow("read_id")));
                    topic.setType(cursor.getInt(cursor.getColumnIndexOrThrow("type")));
                    topic.setVisible(cursor.getInt(cursor.getColumnIndexOrThrow("visible")));
                    topic.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                    topic.setIcon(cursor.getString(cursor.getColumnIndexOrThrow("icon")));

                    topics.add(topic);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return topics;
    }

    public  static List<Topic> getAllPTopics(){
        return getAllTopics(TABLE_PTOPIC);
    }

    public  static List<Topic> getAllGTopics(){
        return getAllTopics(TABLE_GTOPIC);
    }

    // 建表
    public  static void InitGTables(List<Topic> gList, List<Topic> pList){

        for (Topic topic : gList) {
            // 处理每个 Topic 对象，例如打印信息
            TopicDbHelper.createGChatTable(topic.getTid());
        }

        for (Topic topic : pList) {
            // 处理每个 Topic 对象，例如打印信息
            TopicDbHelper.createPChatTable(topic.getTid());
        }

    }

//    public static void reCreateTable(){
//        // 创建表
//        try {
//            BaseDb.getInstance().getWritableDatabase().execSQL(SQL_CREATE_PCHAT_TABLE);
//            BaseDb.getInstance().getWritableDatabase().execSQL(SQL_CREATE_PCHAT_UNREAD_TABLE);
//            BaseDb.getInstance().getWritableDatabase().execSQL(SQL_CREATE_PTOPIC_TABLE);
//            BaseDb.getInstance().getWritableDatabase().execSQL(SQL_CREATE_GTOPIC_TABLE);
//        }catch (SQLException e){
//            e.printStackTrace();
//            System.out.println(e.toString());
//        }
//    }

}
