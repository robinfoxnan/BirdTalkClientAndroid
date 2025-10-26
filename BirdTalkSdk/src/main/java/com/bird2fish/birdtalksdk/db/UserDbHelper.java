package com.bird2fish.birdtalksdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.bird2fish.birdtalksdk.model.User;
import java.util.ArrayList;
import java.util.List;

public class UserDbHelper {

    // 表名
    public static final String TABLE_USERS = "users";
    public static final String TABLE_FOLLOWS = "follows";
    public static final String TABLE_FANS = "fans";
    public static final String TABLE_GROUP_MEMBERS = "group_members";
    public static final String TABLE_ACCOUNT = "account";

    // 视图
    public static final String VIEW_FOLLOWS = "user_follows_view";
    public static final String VIEW_FANS = "user_fans_view";
    public static final String VIEW_MUTUAL = "mutual_follows_view";
    public static final String VIEW_GROUP_MEMS = "group_mems_view";

    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_NICK = "nick";
    public static final String COLUMN_AGE = "age";
    public static final String COLUMN_GENDER = "gender";
    public static final String COLUMN_REGION = "region";
    public static final String COLUMN_ICON = "icon";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_FOLLOWS = "follows";
    public static final String COLUMN_FANS = "fans";
    public static final String COLUMN_INTRODUCTION = "introduction";
    public static final String COLUMN_LAST_LOGIN_TIME = "last_login_time";
    public static final String COLUMN_IS_ONLINE = "is_online";
    public static final String COLUMN_MASK = "mask";
    public static final String COLUMN_CRYPT_KEY = "crypt_key";
    public static final String COLUMN_CRYPT_TYPE = "crypt_type";

    public static final String COLUMN_NICK1 = "nick1";   // 关注昵称
    public static final String COLUMN_NICK2 = "nick2";   // 粉丝昵称
    public static final String COLUMN_NICK3 = "nick3";   // 群昵称

    // 列名
    public static final String COLUMN_GID = "gid";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_ROLE = "role";

    // 个人账号使用的
    public static final String COLUMN_PRINT = "print";
    public static final String COLUMN_SHARED_KEY = "shared_key";
    public static final String COLUMN_PWD = "pwd";

    // 创建用户表的 SQL 语句
    public static final String SQL_CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME + " TEXT," +
                    COLUMN_NICK + " TEXT," +
                    COLUMN_AGE + " INTEGER," +
                    COLUMN_GENDER + " TEXT," +
                    COLUMN_REGION + " TEXT," +
                    COLUMN_ICON + " TEXT," +
                    COLUMN_PHONE + " TEXT," +
                    COLUMN_EMAIL + " TEXT," +
                    COLUMN_FOLLOWS + " INTEGER," +
                    COLUMN_FANS + " INTEGER," +
                    COLUMN_INTRODUCTION + " TEXT," +
                    COLUMN_LAST_LOGIN_TIME + " TEXT," +
                    COLUMN_IS_ONLINE + " INTEGER," +
                    COLUMN_MASK + " INTEGER," +
                    COLUMN_CRYPT_KEY + " TEXT," +
                    COLUMN_CRYPT_TYPE + " TEXT" +
                    ");";


    public static final String SQL_CREATE_FOLLOWS = "CREATE TABLE IF NOT EXISTS " + TABLE_FOLLOWS  +" (" +
            COLUMN_ID + " INTEGER PRIMARY KEY," +
            COLUMN_NICK1 + " TEXT" +
            " );";

    public static final String SQL_CREATE_FANS = "CREATE TABLE IF NOT EXISTS " + TABLE_FANS  +" (" +
            COLUMN_ID + " INTEGER PRIMARY KEY," +
            COLUMN_NICK2 + " TEXT" +
            " );";




    // 创建 group_members 表的 SQL 语句
    public static final String SQL_CREATE_GROUP_MEMBERS = "CREATE TABLE IF NOT EXISTS " + TABLE_GROUP_MEMBERS + " (" +
                    COLUMN_GID + " INTEGER NOT NULL," +
                    COLUMN_UID + " INTEGER NOT NULL," +
                    COLUMN_ROLE + " INTEGER," +
                    COLUMN_NICK3 + " TEXT," +
                    "PRIMARY KEY (" + COLUMN_GID + ", " + COLUMN_UID + ")" +
                    ");";


    // 创建 account 表的 SQL 语句
    public static final String SQL_CREATE_ACCOUNT =
            "CREATE TABLE IF NOT EXISTS " + TABLE_ACCOUNT + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NICK + " TEXT," +
                    COLUMN_ICON + " TEXT," +
                    COLUMN_PWD + " INTEGER," +
                    COLUMN_EMAIL + " INTEGER," +
                    COLUMN_PHONE + " TEXT," +
                    COLUMN_PRINT + " INTEGER," +
                    COLUMN_SHARED_KEY + " BLOB," +
                    COLUMN_CRYPT_TYPE + " TEXT" +
                    ");";


    // 创建 user_follow_view 视图的 SQL 语句
    public static final String SQL_CREATE_FOLLOW_VIEW = "CREATE VIEW IF NOT EXISTS " + VIEW_FOLLOWS +" AS " +
                    "SELECT " +
                    TABLE_FOLLOWS + "." + COLUMN_ID + " AS id, " +
                    TABLE_FOLLOWS + "." + COLUMN_NICK1 + " AS nick1, " +
                    TABLE_USERS + "." + COLUMN_NAME + " AS name, " +
                    TABLE_USERS + "." + COLUMN_NICK + " AS nick, " +
                    TABLE_USERS + "." + COLUMN_AGE + " AS age, " +
                    TABLE_USERS + "." + COLUMN_GENDER + " AS gender, " +
                    TABLE_USERS + "." + COLUMN_REGION + " AS region, " +
                    TABLE_USERS + "." + COLUMN_ICON + " AS icon, " +
                    TABLE_USERS + "." + COLUMN_PHONE + " AS phone, " +
                    TABLE_USERS + "." + COLUMN_EMAIL + " AS email, " +
                    TABLE_USERS + "." + COLUMN_FOLLOWS + " AS follows, " +
                    TABLE_USERS + "." + COLUMN_FANS + " AS fans, " +
                    TABLE_USERS + "." + COLUMN_INTRODUCTION + " AS introduction, " +
                    TABLE_USERS + "." + COLUMN_LAST_LOGIN_TIME + " AS last_login_time, " +
                    TABLE_USERS + "." + COLUMN_IS_ONLINE + " AS is_online, " +
                    TABLE_USERS + "." + COLUMN_CRYPT_KEY + " AS crypt_key, " +
                    TABLE_USERS + "." + COLUMN_CRYPT_TYPE + " AS crypt_type, " +
                    TABLE_USERS + "." + COLUMN_MASK + " AS mask, " +
                    "CASE WHEN " + TABLE_FANS + "." + COLUMN_ID + " IS NOT NULL THEN 1 ELSE 0 END AS is_fan " +
                    "FROM " + TABLE_FOLLOWS +
                    " LEFT JOIN " + TABLE_USERS + " ON " + TABLE_FOLLOWS + "." + COLUMN_ID + " = " + TABLE_USERS + "." + COLUMN_ID + " " +
                    " LEFT JOIN " + TABLE_FANS + " ON " + TABLE_FOLLOWS  + "." + COLUMN_ID + " = " + TABLE_FANS + "." + COLUMN_ID + ";";


    // 创建 user_fan_view 视图的 SQL 语句
    public static final String SQL_CREATE_FAN_VIEW =  "CREATE VIEW IF NOT EXISTS " +VIEW_FANS+" AS " +
                    "SELECT " +
                    TABLE_FANS + "." + COLUMN_ID + " AS id, " +
                    TABLE_FANS + "." + COLUMN_NICK2 + " AS nick2, " +
                    TABLE_USERS + "." + COLUMN_NAME + " AS name, " +
                    TABLE_USERS + "." + COLUMN_NICK + " AS nick, " +
                    TABLE_USERS + "." + COLUMN_AGE + " AS age, " +
                    TABLE_USERS + "." + COLUMN_GENDER + " AS gender, " +
                    TABLE_USERS + "." + COLUMN_REGION + " AS region, " +
                    TABLE_USERS + "." + COLUMN_ICON + " AS icon, " +
                    TABLE_USERS + "." + COLUMN_PHONE + " AS phone, " +
                    TABLE_USERS + "." + COLUMN_EMAIL + " AS email, " +
                    TABLE_USERS + "." + COLUMN_FOLLOWS + " AS follows, " +
                    TABLE_USERS + "." + COLUMN_FANS + " AS fans, " +
                    TABLE_USERS + "." + COLUMN_INTRODUCTION + " AS introduction, " +
                    TABLE_USERS + "." + COLUMN_LAST_LOGIN_TIME + " AS last_login_time, " +
                    TABLE_USERS + "." + COLUMN_IS_ONLINE + " AS is_online, " +
                    TABLE_USERS + "." + COLUMN_CRYPT_KEY + " AS crypt_key, " +
                    TABLE_USERS + "." + COLUMN_CRYPT_TYPE + " AS crypt_type, " +
                    TABLE_USERS + "." + COLUMN_MASK + " AS mask, " +
                    "CASE WHEN " + TABLE_FOLLOWS + "." + COLUMN_ID + " IS NOT NULL THEN 1 ELSE 0 END AS is_followed " +
                    "FROM " + TABLE_FANS + " " +
                    "LEFT JOIN " + TABLE_USERS + " ON " + TABLE_FANS + "." + COLUMN_ID + " = " + TABLE_USERS + "." + COLUMN_ID + " " +
                    "LEFT JOIN " + TABLE_FOLLOWS + " ON " + TABLE_FANS + "." + COLUMN_ID + " = " + TABLE_FOLLOWS + "." + COLUMN_ID + ";";

    // 创建 mutual_follows_view 视图的 SQL 语句
    public static final String SQL_CREATE_MUTUAL_VIEW = "CREATE VIEW IF NOT EXISTS " + VIEW_MUTUAL +" AS " +
                    "SELECT " +
                    TABLE_FOLLOWS + "." + COLUMN_ID + " AS id, " +
                    TABLE_FOLLOWS + "." + COLUMN_NICK1 + " AS nick1, " +
                    TABLE_FANS + "." + COLUMN_NICK2 + " AS nick2, " +
                    TABLE_USERS + "." + COLUMN_NAME + " AS name, " +
                    TABLE_USERS + "." + COLUMN_NICK + " AS nick, " +
                    TABLE_USERS + "." + COLUMN_AGE + " AS age, " +
                    TABLE_USERS + "." + COLUMN_GENDER + " AS gender, " +
                    TABLE_USERS + "." + COLUMN_REGION + " AS region, " +
                    TABLE_USERS + "." + COLUMN_ICON + " AS icon, " +
                    TABLE_USERS + "." + COLUMN_PHONE + " AS phone, " +
                    TABLE_USERS + "." + COLUMN_EMAIL + " AS email, " +
                    TABLE_USERS + "." + COLUMN_FOLLOWS + " AS follows, " +
                    TABLE_USERS + "." + COLUMN_FANS + " AS fans, " +
                    TABLE_USERS + "." + COLUMN_INTRODUCTION + " AS introduction, " +
                    TABLE_USERS + "." + COLUMN_LAST_LOGIN_TIME + " AS last_login_time, " +
                    TABLE_USERS + "." + COLUMN_IS_ONLINE + " AS is_online, " +
                    TABLE_USERS + "." + COLUMN_CRYPT_KEY + " AS crypt_key, " +
                    TABLE_USERS + "." + COLUMN_CRYPT_TYPE + " AS crypt_type, " +
                    TABLE_USERS + "." + COLUMN_MASK + " AS mask " +
                    "FROM " + TABLE_FOLLOWS + " " +
                    "INNER JOIN " + TABLE_FANS + " ON " + TABLE_FOLLOWS + "." + COLUMN_ID + " = " + TABLE_FANS + "." + COLUMN_ID + " " +
                    "LEFT JOIN " + TABLE_USERS + " ON " + TABLE_FOLLOWS + "." + COLUMN_ID + " = " + TABLE_USERS + "." + COLUMN_ID + ";";

    // 创建 group_mem_view 视图的 SQL 语句
    public static final String SQL_CREATE_GROUP_MEM_VIEW = "CREATE VIEW IF NOT EXISTS " + VIEW_GROUP_MEMS +" AS " +
                    "SELECT " +
                    TABLE_GROUP_MEMBERS + "." + COLUMN_GID + " AS gid, " +
                    TABLE_GROUP_MEMBERS + "." + COLUMN_UID + " AS uid, " +
                    TABLE_GROUP_MEMBERS + "." + COLUMN_NICK3 + " AS nick3, " +

                    TABLE_USERS + "." + COLUMN_NAME + " AS name, " +
                    TABLE_USERS + "." + COLUMN_NICK + " AS nick, " +
                    TABLE_USERS + "." + COLUMN_AGE + " AS age, " +
                    TABLE_USERS + "." + COLUMN_GENDER + " AS gender, " +
                    TABLE_USERS + "." + COLUMN_REGION + " AS region, " +
                    TABLE_USERS + "." + COLUMN_ICON + " AS icon, " +
                    TABLE_USERS + "." + COLUMN_PHONE + " AS phone, " +
                    TABLE_USERS + "." + COLUMN_EMAIL + " AS email, " +
                    TABLE_USERS + "." + COLUMN_FOLLOWS + " AS follows, " +
                    TABLE_USERS + "." + COLUMN_FANS + " AS fans, " +
                    TABLE_USERS + "." + COLUMN_INTRODUCTION + " AS introduction, " +
                    TABLE_USERS + "." + COLUMN_LAST_LOGIN_TIME + " AS last_login_time, " +
                    TABLE_USERS + "." + COLUMN_IS_ONLINE + " AS is_online, " +
                    TABLE_USERS + "." + COLUMN_CRYPT_KEY + " AS crypt_key, " +
                    TABLE_USERS + "." + COLUMN_CRYPT_TYPE + " AS crypt_type, " +
                    TABLE_USERS + "." + COLUMN_MASK + " AS mask " +
                    "FROM " + TABLE_GROUP_MEMBERS + " " +
                    "LEFT JOIN " + TABLE_USERS + " ON " + TABLE_GROUP_MEMBERS + "." + COLUMN_UID + " = " + TABLE_USERS + "." + COLUMN_ID + ";";


    public static void onCreate(SQLiteDatabase db) {
        // 创建表
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_FOLLOWS);
        db.execSQL(SQL_CREATE_FANS);
        db.execSQL(SQL_CREATE_GROUP_MEMBERS);
        db.execSQL(SQL_CREATE_ACCOUNT);

        // 创建视图
        db.execSQL(SQL_CREATE_FOLLOW_VIEW);
        db.execSQL(SQL_CREATE_FAN_VIEW);
        db.execSQL(SQL_CREATE_MUTUAL_VIEW);
        db.execSQL(SQL_CREATE_GROUP_MEM_VIEW);
    }
    /////////////////////////////////////////////////////////////////////////////////


    // 插入联系人
    // 函数用于插入或更新数据
    public static long insertOrUpdateUser(User user)  {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, user.getId());
        values.put(COLUMN_NAME, user.getName());
        values.put(COLUMN_NICK, user.getNick());
        values.put(COLUMN_AGE, user.getAge());
        values.put(COLUMN_GENDER, user.getGender());
        values.put(COLUMN_REGION, user.getRegion());
        values.put(COLUMN_ICON, user.getIcon());
        values.put(COLUMN_FOLLOWS, user.getFollows());
        values.put(COLUMN_FANS, user.getFans());
        values.put(COLUMN_INTRODUCTION, user.getIntroduction());
        values.put(COLUMN_LAST_LOGIN_TIME, user.getLastLoginTime());
        values.put(COLUMN_IS_ONLINE, user.isOnline() ? 1 : 0);
        values.put(COLUMN_MASK, user.getMask());
        values.put(COLUMN_CRYPT_KEY, user.getCryptKey());
        values.put(COLUMN_CRYPT_TYPE, user.getCryptType());

        // 插入或更新操作
        long rowId = BaseDb.getInstance().getWritableDatabase().insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            // 插入失败
            // 可以根据需要进行错误处理
        } else {
            // 插入或更新成功
        }
        return rowId;
    }

    // 查询所有联系人
    // 查询所有用户
    // 根据 ID 查询用户
    // 根据 id 查询用户
    public static User getUserById(long userId) {
        User user = null;

        // 定义需要查询的列
        String[] projection = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_NICK,
                COLUMN_AGE,
                COLUMN_GENDER,
                COLUMN_REGION,
                COLUMN_ICON,
                COLUMN_FOLLOWS,
                COLUMN_FANS,
                COLUMN_INTRODUCTION,
                COLUMN_LAST_LOGIN_TIME,
                COLUMN_IS_ONLINE,
                COLUMN_MASK,
                COLUMN_CRYPT_KEY,
                COLUMN_CRYPT_TYPE
                // 添加其他列
        };

        // 查询条件
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(userId) };

        // 查询操作
        Cursor cursor = BaseDb.getInstance().getWritableDatabase().query(
                TABLE_USERS,         // 表名
                projection,          // 返回的列
                selection,           // WHERE 子句
                selectionArgs,       // WHERE 子句的参数
                null,                // GROUP BY 子句
                null,                // HAVING 子句
                null                 // ORDER BY 子句
        );

        // 解析查询结果
        if (cursor != null && cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            user.setNick(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK)));
            user.setAge(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE)));
            user.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)));
            user.setRegion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION)));
            user.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON)));
            user.setFollows(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS)));
            user.setFans(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS)));
            user.setIntroduction(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTRODUCTION)));
            user.setLastLoginTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN_TIME)));
            user.setOnline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1);
            user.setMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK)));
            user.setCryptKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY)));
            user.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE)));
            // 解析其他列

            cursor.close();
        }

        return user;
    }


    // 更新所有的字段


    // 正向查找若干条用户数据
    public static  List<User> findUserFrom(int userId, int limit) {
        List<User> userList = new ArrayList<>();

        // 定义需要查询的列
        String[] projection = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_NICK,
                COLUMN_AGE,
                COLUMN_GENDER,
                COLUMN_REGION,
                COLUMN_ICON,
                COLUMN_FOLLOWS,
                COLUMN_FANS,
                COLUMN_INTRODUCTION,
                COLUMN_LAST_LOGIN_TIME,
                COLUMN_IS_ONLINE,
                COLUMN_MASK,
                COLUMN_CRYPT_KEY,
                COLUMN_CRYPT_TYPE
                // 添加其他列
        };

        // 查询条件
        String selection = COLUMN_ID + " >= ?";
        String[] selectionArgs = { String.valueOf(userId) };

        // 排序方式
        String sortOrder = COLUMN_ID + " ASC";

        // 查询操作
        Cursor cursor = BaseDb.getInstance().getWritableDatabase().query(
                TABLE_USERS,         // 表名
                projection,          // 返回的列
                selection,           // WHERE 子句
                selectionArgs,       // WHERE 子句的参数
                null,                // GROUP BY 子句
                null,                // HAVING 子句
                sortOrder,           // ORDER BY 子句
                String.valueOf(limit) // LIMIT 子句，限制返回行数
        );

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                User user = new User();
                user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                user.setNick(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK)));
                user.setAge(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE)));
                user.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)));
                user.setRegion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION)));
                user.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON)));
                user.setFollows(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS)));
                user.setFans(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS)));
                user.setIntroduction(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTRODUCTION)));
                user.setLastLoginTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN_TIME)));
                user.setOnline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1);
                user.setMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK)));
                user.setCryptKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY)));
                user.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE)));
                // 解析其他列

                userList.add(user);
            }
            cursor.close();
        }

        return userList;
    }

    // 插入粉丝关系
    public static  long insertFan(long userId, String nick) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, userId);  // 这里假设粉丝表的用户 id 列名为 "id"
        values.put(COLUMN_NICK2, nick); // 假设粉丝表的昵称列名为 "nick2"

        // 插入操作
        long rowId = BaseDb.getInstance().getWritableDatabase().insertWithOnConflict(TABLE_FANS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            // 插入失败
            // 可以根据需要进行错误处理
        } else {
            // 插入成功
        }
        return rowId;
    }

    // 插入关注关系
    public static long insertFollow(long userId, String nick) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, userId);  // 这里假设粉丝表的用户 id 列名为 "id"
        values.put(COLUMN_NICK1, nick); // 假设粉丝表的昵称列名为 "nick2"

        // 插入操作
        long rowId = BaseDb.getInstance().getWritableDatabase().insertWithOnConflict(TABLE_FOLLOWS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            // 插入失败
            // 可以根据需要进行错误处理
        } else {
            // 插入成功
        }
        return rowId;
    }

    // 从粉丝表中删除指定 id 的用户
    public static  void deleteFromFans(int userId) {
        // 删除操作
        int rowsDeleted =  BaseDb.getInstance().getWritableDatabase().delete(TABLE_FANS, "id=?", new String[] { String.valueOf(userId) });
        if (rowsDeleted > 0) {
            // 删除成功
        } else {
            // 删除失败或未找到匹配的行
        }
    }

    // 从关注表中删除指定 id 的用户
    public static  void deleteFromFollows(int userId) {
        // 删除操作
        int rowsDeleted = BaseDb.getInstance().getWritableDatabase().delete(TABLE_FOLLOWS, "id=?", new String[] { String.valueOf(userId) });
        if (rowsDeleted > 0) {
            // 删除成功
        } else {
            // 删除失败或未找到匹配的行
        }
    }

    // 查询视图中从某个 id 开始的若干条 User 记录
    public static  List<User> queryFollowsFromView(long userId, int limit) {
        List<User> userList = new ArrayList<>();

        // 查询条件
        //String selection = COLUMN_ID + " >= ?";
        //String[] selectionArgs = { String.valueOf(userId) };

        // 排序方式
        String sortOrder = COLUMN_ID + " ASC";

        // 构建视图的查询语句
        String viewQuery = "SELECT * FROM " + VIEW_FOLLOWS +
                //" WHERE " + selection +
                " ORDER BY " + sortOrder +
                " LIMIT " + limit;

        // 执行查询操作
        Cursor cursor = BaseDb.getInstance().getWritableDatabase().rawQuery(viewQuery, null);

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                User user = new User();
                user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                user.setNick(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK)));
                user.setNick1(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK1)));
                user.setAge(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE)));
                user.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)));
                user.setRegion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION)));
                user.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON)));
                user.setFollows(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS)));
                user.setFans(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS)));
                user.setIntroduction(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTRODUCTION)));
                user.setLastLoginTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN_TIME)));
                user.setOnline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1);
                user.setMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK)));
                user.setCryptKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY)));
                user.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE)));
                // 解析其他列
                int is_fan = cursor.getInt(cursor.getColumnIndexOrThrow("is_fan"));
                user.setFan(is_fan == 1);

                userList.add(user);
            }
            cursor.close();
        }

        return userList;
    }

    // 查询视图中从某个 id 开始的若干条 User 记录
    public static  List<User> queryFansFromView(long userId, int limit) {
        List<User> userList = new ArrayList<>();

        // 查询条件
        //String selection = COLUMN_ID + " >= ?";
        //String[] selectionArgs = { String.valueOf(userId) };

        // 排序方式
        String sortOrder = COLUMN_ID + " ASC";

        // 构建视图的查询语句
        String viewQuery = "SELECT * FROM " + VIEW_FANS +
                //" WHERE " + selection +
                " ORDER BY " + sortOrder +
                " LIMIT " + limit;

        // 执行查询操作
        Cursor cursor = BaseDb.getInstance().getWritableDatabase().rawQuery(viewQuery, null);

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                User user = new User();
                user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                user.setNick(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK)));
                user.setNick2(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK2)));
                user.setAge(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE)));
                user.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)));
                user.setRegion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION)));
                user.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON)));
                user.setFollows(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS)));
                user.setFans(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS)));
                user.setIntroduction(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTRODUCTION)));
                user.setLastLoginTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN_TIME)));
                user.setOnline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1);
                user.setMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK)));
                user.setCryptKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY)));
                user.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE)));
                int is_following = cursor.getInt(cursor.getColumnIndexOrThrow("is_followed"));
                user.setFollow(is_following == 1);
                // 解析其他列

                userList.add(user);
            }
            cursor.close();
        }

        return userList;
    }

    public static   List<User> queryMutualFromView(long userId, int limit) {
        List<User> userList = new ArrayList<>();

        // 查询条件
        //String selection = COLUMN_ID + " >= ?";
        //String[] selectionArgs = { String.valueOf(userId) };

        // 排序方式
        String sortOrder = COLUMN_ID + " ASC";

        // 构建视图的查询语句
        String viewQuery = "SELECT * FROM " + VIEW_MUTUAL +
                //" WHERE " + selection +
                " ORDER BY " + sortOrder +
                " LIMIT " + limit;

        // 执行查询操作
        Cursor cursor = BaseDb.getInstance().getWritableDatabase().rawQuery(viewQuery, null);

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                User user = new User();
                user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                user.setNick(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK)));
                user.setNick1(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK1)));
                user.setNick2(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK2)));
                user.setAge(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE)));
                user.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)));
                user.setRegion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION)));
                user.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON)));
                user.setFollows(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS)));
                user.setFans(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS)));
                user.setIntroduction(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTRODUCTION)));
                user.setLastLoginTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN_TIME)));
                user.setOnline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1);
                user.setMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK)));
                user.setCryptKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY)));
                user.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE)));
                // 解析其他列

                userList.add(user);
            }
            cursor.close();
        }

        return userList;
    }

    // 群组成员
    public static long insertOrReplaceGroupMember(long gid, long uid, int role, String nick) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_GID, gid);
        values.put(COLUMN_UID, uid);
        values.put(COLUMN_ROLE, role);
        values.put(COLUMN_NICK3, nick);

        // 使用 insertWithOnConflict() 方法实现插入或替换
        long result = BaseDb.getInstance().getWritableDatabase().insertWithOnConflict(TABLE_GROUP_MEMBERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        if (result == -1) {
            // 插入或替换失败
        } else {
            // 插入或替换成功
        }
        return result;
    }

    // 从 group_members 表中删除特定的记录
    public static void deleteGroupMember(long gid, long uid) {
        // 定义删除条件
        String whereClause = "gid=? AND uid=?";
        String[] whereArgs = { String.valueOf(gid), String.valueOf(uid) };

        // 执行删除操作
        int rowsDeleted = BaseDb.getInstance().getWritableDatabase().delete(TABLE_GROUP_MEMBERS, whereClause, whereArgs);

        if (rowsDeleted > 0) {
            // 删除成功
        } else {
            // 删除失败或未找到匹配的行
        }
    }

    // 查询视图中从某个 uid 开始的若干条 User 记录
    public static List<User> queryUsersFromGroupMemsView(long gid, long uid, int limit) {
        List<User> userList = new ArrayList<>();
        // 查询条件
        String selection = "gid = ? AND uid >= ?";
        String[] selectionArgs = { String.valueOf(gid), String.valueOf(uid) };

        // 排序方式
        String sortOrder = COLUMN_UID + " ASC";

        // 构建视图的查询语句
        String viewQuery = "SELECT * FROM " + VIEW_GROUP_MEMS +
                " WHERE " + selection +
                " ORDER BY " + sortOrder +
                " LIMIT " + limit;

        // 执行查询操作
        Cursor cursor = BaseDb.getInstance().getWritableDatabase().rawQuery(viewQuery, selectionArgs);

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                User user = new User();
                user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UID)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                user.setNick(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK)));
                user.setNick3(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK3)));
                user.setAge(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE)));
                user.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)));
                user.setRegion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION)));
                user.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON)));
                user.setFollows(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS)));
                user.setFans(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS)));
                user.setIntroduction(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTRODUCTION)));
                user.setLastLoginTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN_TIME)));
                user.setOnline(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1);
                user.setMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK)));
                user.setCryptKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY)));
                user.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE)));
                // 解析其他列

                userList.add(user);
            }
            cursor.close();
        }

        return userList;
    }


    // 向 TABLE_ACCOUNT 表中插入或替换数据
    public static Boolean insertOrReplaceAccount(User user){
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, user.getId());
        values.put(COLUMN_NICK, user.getNick());
        values.put(COLUMN_ICON, user.getIcon());
        values.put(COLUMN_PWD, user.getPwd());
        values.put(COLUMN_EMAIL, user.getEmail());
        values.put(COLUMN_PHONE, user.getPhone());
        values.put(COLUMN_PRINT, user.getSharedPrint());
        values.put(COLUMN_SHARED_KEY, user.getSharedKey());
        values.put(COLUMN_CRYPT_TYPE, user.getCryptType());

        // 使用 insertWithOnConflict() 方法实现插入或替换
        long result = BaseDb.getInstance().getWritableDatabase().insertWithOnConflict(TABLE_ACCOUNT, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        if (result == -1) {
            // 插入或替换失败
            return false;
        } else {
            // 插入或替换成功
        }
        return true;
    }

    // 根据 id 查询 User
    public static User getAccount(long id) {
        User user = null;

        // 查询条件
        String selection = "id=?";
        String[] selectionArgs = { String.valueOf(id) };

        // 执行查询操作
        Cursor cursor = BaseDb.getInstance().getWritableDatabase().query(
                TABLE_ACCOUNT, // 表名
                null,          // 返回所有列
                selection,     // 查询条件
                selectionArgs, // 查询条件的参数值
                null,          // 不需要 group by
                null,          // 不需要 having
                null           // 不需要 order by
        );

        // 解析查询结果
        if (cursor != null && cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            user.setNick(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK)));
            user.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON)));
            user.setPwd(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PWD)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
            user.setPhone(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)));
            user.setSharedPrint(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PRINT)));
            user.setSharedKey(cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_SHARED_KEY)));
            user.setCryptType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE)));

            // 解析其他列
        }

        // 关闭游标
        if (cursor != null) {
            cursor.close();
        }

        return user;
    }


}
