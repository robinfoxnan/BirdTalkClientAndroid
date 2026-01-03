package com.bird2fish.birdtalksdk.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.bird2fish.birdtalksdk.model.User

object UserDbHelper {
    // 表名
    const val TABLE_USERS: String = "users"
    const val TABLE_FOLLOWS: String = "follows"
    const val TABLE_FANS: String = "fans"
    const val TABLE_GROUP_MEMBERS: String = "group_members"
    const val TABLE_ACCOUNT: String = "account"

    // 视图
    const val VIEW_FOLLOWS: String = "user_follows_view"
    const val VIEW_FANS: String = "user_fans_view"
    const val VIEW_MUTUAL: String = "mutual_follows_view"
    const val VIEW_GROUP_MEMS: String = "group_mems_view"

    // 列名
    const val COLUMN_ID: String = "id"
    const val COLUMN_NAME: String = "name"
    const val COLUMN_NICK: String = "nick"
    const val COLUMN_AGE: String = "age"
    const val COLUMN_GENDER: String = "gender"
    const val COLUMN_REGION: String = "region"
    const val COLUMN_ICON: String = "icon"
    const val COLUMN_PHONE: String = "phone"
    const val COLUMN_EMAIL: String = "email"
    const val COLUMN_FOLLOWS: String = "follows"
    const val COLUMN_FANS: String = "fans"
    const val COLUMN_INTRODUCTION: String = "introduction"
    const val COLUMN_LAST_LOGIN_TIME: String = "last_login_time"
    const val COLUMN_IS_ONLINE: String = "is_online"
    const val COLUMN_MASK: String = "mask"
    const val COLUMN_CRYPT_KEY: String = "crypt_key"
    const val COLUMN_CRYPT_TYPE: String = "crypt_type"

    const val COLUMN_NICK1: String = "nick1" // 关注昵称
    const val COLUMN_NICK2: String = "nick2" // 粉丝昵称
    const val COLUMN_NICK3: String = "nick3" // 群昵称

    // 列名
    const val COLUMN_GID: String = "gid"
    const val COLUMN_UID: String = "uid"
    const val COLUMN_ROLE: String = "role"

    // 个人账号使用的
    const val COLUMN_PRINT: String = "print"
    const val COLUMN_SHARED_KEY: String = "shared_key"
    const val COLUMN_PWD: String = "pwd"

    // 创建用户表的 SQL 语句
    const val SQL_CREATE_USERS: String = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
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
            ");"


    const val SQL_CREATE_FOLLOWS: String = "CREATE TABLE IF NOT EXISTS " + TABLE_FOLLOWS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY," +
            COLUMN_NICK1 + " TEXT" +
            " );"

    const val SQL_CREATE_FANS: String = "CREATE TABLE IF NOT EXISTS " + TABLE_FANS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY," +
            COLUMN_NICK2 + " TEXT" +
            " );"


    // 创建 group_members 表的 SQL 语句
    const val SQL_CREATE_GROUP_MEMBERS: String =
        "CREATE TABLE IF NOT EXISTS " + TABLE_GROUP_MEMBERS + " (" +
                COLUMN_GID + " INTEGER NOT NULL," +
                COLUMN_UID + " INTEGER NOT NULL," +
                COLUMN_ROLE + " INTEGER," +
                COLUMN_NICK3 + " TEXT," +
                "PRIMARY KEY (" + COLUMN_GID + ", " + COLUMN_UID + ")" +
                ");"


    // 创建 account 表的 SQL 语句
    const val SQL_CREATE_ACCOUNT: String = "CREATE TABLE IF NOT EXISTS " + TABLE_ACCOUNT + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY," +
            COLUMN_NICK + " TEXT," +
            COLUMN_ICON + " TEXT," +
            COLUMN_PWD + " INTEGER," +
            COLUMN_EMAIL + " INTEGER," +
            COLUMN_PHONE + " TEXT," +
            COLUMN_PRINT + " INTEGER," +
            COLUMN_SHARED_KEY + " BLOB," +
            COLUMN_CRYPT_TYPE + " TEXT" +
            ");"


    // 创建 user_follow_view 视图的 SQL 语句
    const val SQL_CREATE_FOLLOW_VIEW: String =
        "CREATE VIEW IF NOT EXISTS " + VIEW_FOLLOWS + " AS " +
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
                " LEFT JOIN " + TABLE_FANS + " ON " + TABLE_FOLLOWS + "." + COLUMN_ID + " = " + TABLE_FANS + "." + COLUMN_ID + ";"


    // 创建 user_fan_view 视图的 SQL 语句
    const val SQL_CREATE_FAN_VIEW: String = "CREATE VIEW IF NOT EXISTS " + VIEW_FANS + " AS " +
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
            "LEFT JOIN " + TABLE_FOLLOWS + " ON " + TABLE_FANS + "." + COLUMN_ID + " = " + TABLE_FOLLOWS + "." + COLUMN_ID + ";"

    // 创建 mutual_follows_view 视图的 SQL 语句
    const val SQL_CREATE_MUTUAL_VIEW: String = "CREATE VIEW IF NOT EXISTS " + VIEW_MUTUAL + " AS " +
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
            "LEFT JOIN " + TABLE_USERS + " ON " + TABLE_FOLLOWS + "." + COLUMN_ID + " = " + TABLE_USERS + "." + COLUMN_ID + ";"

    // 创建 group_mem_view 视图的 SQL 语句
    const val SQL_CREATE_GROUP_MEM_VIEW: String =
        "CREATE VIEW IF NOT EXISTS " + VIEW_GROUP_MEMS + " AS " +
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
                "LEFT JOIN " + TABLE_USERS + " ON " + TABLE_GROUP_MEMBERS + "." + COLUMN_UID + " = " + TABLE_USERS + "." + COLUMN_ID + ";"


    @JvmStatic
    fun onCreate(db: SQLiteDatabase) {
        //SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        // 创建表
        db.execSQL(SQL_CREATE_USERS)
        db.execSQL(SQL_CREATE_FOLLOWS)
        db.execSQL(SQL_CREATE_FANS)
        db.execSQL(SQL_CREATE_GROUP_MEMBERS)
        db.execSQL(SQL_CREATE_ACCOUNT)

        // 创建视图
        db.execSQL(SQL_CREATE_FOLLOW_VIEW)
        db.execSQL(SQL_CREATE_FAN_VIEW)
        db.execSQL(SQL_CREATE_MUTUAL_VIEW)
        db.execSQL(SQL_CREATE_GROUP_MEM_VIEW)
    }


    /////////////////////////////////////////////////////////////////////////////////
    // 插入联系人
    // 函数用于插入或更新数据
    fun insertOrUpdateUser(user: User): Long {
        val values = ContentValues()
        values.put(COLUMN_ID, user.id)
        values.put(COLUMN_NAME, user.name)
        values.put(COLUMN_NICK, user.nick)
        values.put(COLUMN_AGE, user.age)
        values.put(COLUMN_GENDER, user.gender)
        values.put(COLUMN_REGION, user.region)
        values.put(COLUMN_ICON, user.icon)
        values.put(COLUMN_FOLLOWS, user.follows)
        values.put(COLUMN_FANS, user.fans)
        values.put(COLUMN_INTRODUCTION, user.introduction)
        values.put(COLUMN_LAST_LOGIN_TIME, user.lastLoginTime)
        values.put(COLUMN_IS_ONLINE, if (user.isOnline) 1 else 0)
        values.put(COLUMN_MASK, user.mask)
        values.put(COLUMN_CRYPT_KEY, user.cryptKey)
        values.put(COLUMN_CRYPT_TYPE, user.cryptType)

        // 插入或更新操作
        val rowId = BaseDb.getInstance().writableDatabase.insertWithOnConflict(
            TABLE_USERS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        if (rowId == -1L) {
            // 插入失败
            // 可以根据需要进行错误处理
        } else {
            // 插入或更新成功
        }
        return rowId
    }

    // 查询所有联系人
    // 查询所有用户
    // 根据 ID 查询用户
    // 根据 id 查询用户
    fun getUserById(userId: Long): User? {
        var user: User? = null

        // 定义需要查询的列
        val projection = arrayOf(
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
            COLUMN_CRYPT_TYPE // 添加其他列
        )

        // 查询条件
        val selection = COLUMN_ID + " = ?"
        val selectionArgs = arrayOf(userId.toString())

        // 查询操作
        val cursor = BaseDb.getInstance().writableDatabase.query(
            TABLE_USERS,  // 表名
            projection,  // 返回的列
            selection,  // WHERE 子句
            selectionArgs,  // WHERE 子句的参数
            null,  // GROUP BY 子句
            null,  // HAVING 子句
            null // ORDER BY 子句
        )

        // 解析查询结果
        if (cursor != null && cursor.moveToFirst()) {
            user = User()
            user.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            user.name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            user.nick = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK))
            user.age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE))
            user.gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER))
            user.region = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION))
            user.icon = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON))
            user.follows = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS))
            user.fans = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS))
            user.introduction = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INTRODUCTION))
            user.lastLoginTime = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    COLUMN_LAST_LOGIN_TIME
                )
            )
            user.isOnline = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1L
            user.mask = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK))
            user.cryptKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY))
            user.cryptType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE))

            // 解析其他列
            cursor.close()
        }

        return user
    }


    // 更新所有的字段
    // 正向查找若干条用户数据
    fun findUserFrom(userId: Int, limit: Int): List<User> {
        val userList: MutableList<User> = ArrayList()

        // 定义需要查询的列
        val projection = arrayOf(
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
            COLUMN_CRYPT_TYPE // 添加其他列
        )

        // 查询条件
        val selection = COLUMN_ID + " >= ?"
        val selectionArgs = arrayOf(userId.toString())

        // 排序方式
        val sortOrder = COLUMN_ID + " ASC"

        // 查询操作
        val cursor = BaseDb.getInstance().writableDatabase.query(
            TABLE_USERS,  // 表名
            projection,  // 返回的列
            selection,  // WHERE 子句
            selectionArgs,  // WHERE 子句的参数
            null,  // GROUP BY 子句
            null,  // HAVING 子句
            sortOrder,  // ORDER BY 子句
            limit.toString() // LIMIT 子句，限制返回行数
        )

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val user = User()
                user.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                user.name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                user.nick = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK))
                user.age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE))
                user.gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER))
                user.region = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION))
                user.icon = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON))
                user.follows = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS))
                user.fans = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS))
                user.introduction = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_INTRODUCTION
                    )
                )
                user.lastLoginTime = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_LAST_LOGIN_TIME
                    )
                )
                user.isOnline = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1L
                user.mask = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK))
                user.cryptKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY))
                user.cryptType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE))

                // 解析其他列
                userList.add(user)
            }
            cursor.close()
        }

        return userList
    }

    // 插入粉丝关系
    fun insertFan(userId: Long, nick: String?): Long {
        val values = ContentValues()
        values.put(COLUMN_ID, userId) // 这里假设粉丝表的用户 id 列名为 "id"
        values.put(COLUMN_NICK2, nick) // 假设粉丝表的昵称列名为 "nick2"

        // 插入操作
        val rowId = BaseDb.getInstance().writableDatabase.insertWithOnConflict(
            TABLE_FANS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        if (rowId == -1L) {
            // 插入失败
            // 可以根据需要进行错误处理
        } else {
            // 插入成功
        }
        return rowId
    }

    // 插入关注关系
    fun insertFollow(userId: Long, nick: String?): Long {
        val values = ContentValues()
        values.put(COLUMN_ID, userId) // 这里假设粉丝表的用户 id 列名为 "id"
        values.put(COLUMN_NICK1, nick) // 假设粉丝表的昵称列名为 "nick2"

        // 插入操作
        val rowId = BaseDb.getInstance().writableDatabase.insertWithOnConflict(
            TABLE_FOLLOWS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        if (rowId == -1L) {
            // 插入失败
            // 可以根据需要进行错误处理
        } else {
            // 插入成功
        }
        return rowId
    }

    // 从粉丝表中删除指定 id 的用户
    fun deleteFromFans(userId: Long) {
        // 删除操作
        val rowsDeleted = BaseDb.getInstance().writableDatabase.delete(
            TABLE_FANS,
            "id=?",
            arrayOf(userId.toString())
        )
        if (rowsDeleted > 0) {
            // 删除成功
        } else {
            // 删除失败或未找到匹配的行
        }
    }

    // 从关注表中删除指定 id 的用户
    fun deleteFromFollows(userId: Long) {
        // 删除操作
        val rowsDeleted = BaseDb.getInstance().writableDatabase.delete(
            TABLE_FOLLOWS,
            "id=?",
            arrayOf(userId.toString())
        )
        if (rowsDeleted > 0) {
            // 删除成功
        } else {
            // 删除失败或未找到匹配的行
        }
    }

    // 查询视图中从某个 id 开始的若干条 User 记录
    fun queryFollowsFromView(userId: Long, limit: Int): List<User> {
        val userList: MutableList<User> = ArrayList()

        // 查询条件
        //String selection = COLUMN_ID + " >= ?";
        //String[] selectionArgs = { String.valueOf(userId) };

        // 排序方式
        val sortOrder = COLUMN_ID + " ASC"

        // 构建视图的查询语句
        val viewQuery = "SELECT * FROM " + VIEW_FOLLOWS +  //" WHERE " + selection +
                " ORDER BY " + sortOrder +
                " LIMIT " + limit

        // 执行查询操作
        val cursor = BaseDb.getInstance().writableDatabase.rawQuery(viewQuery, null)

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val user = User()
                user.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                user.name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                user.nick = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK))
                user.nick1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK1))
                user.age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE))
                user.gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER))
                user.region = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION))
                user.icon = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON))
                user.follows = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS))
                user.fans = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS))
                user.introduction = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_INTRODUCTION
                    )
                )
                user.lastLoginTime = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_LAST_LOGIN_TIME
                    )
                )
                user.isOnline = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1L
                user.mask = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK))
                user.cryptKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY))
                user.cryptType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE))
                // 解析其他列
                val is_fan = cursor.getInt(cursor.getColumnIndexOrThrow("is_fan"))
                user.isFan = is_fan == 1

                userList.add(user)
            }
            cursor.close()
        }

        return userList
    }

    // 查询视图中从某个 id 开始的若干条 User 记录
    fun queryFansFromView(userId: Long, limit: Int): List<User> {
        val userList: MutableList<User> = ArrayList()

        // 查询条件
        //String selection = COLUMN_ID + " >= ?";
        //String[] selectionArgs = { String.valueOf(userId) };

        // 排序方式
        val sortOrder = COLUMN_ID + " ASC"

        // 构建视图的查询语句
        val viewQuery = "SELECT * FROM " + VIEW_FANS +  //" WHERE " + selection +
                " ORDER BY " + sortOrder +
                " LIMIT " + limit

        // 执行查询操作
        val cursor = BaseDb.getInstance().writableDatabase.rawQuery(viewQuery, null)

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val user = User()
                user.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                user.name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                user.nick = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK))
                user.nick2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK2))
                user.age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE))
                user.gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER))
                user.region = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION))
                user.icon = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON))
                user.follows = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS))
                user.fans = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS))
                user.introduction = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_INTRODUCTION
                    )
                )
                user.lastLoginTime = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_LAST_LOGIN_TIME
                    )
                )
                user.isOnline = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1L
                user.mask = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK))
                user.cryptKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY))
                user.cryptType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE))
                val is_following = cursor.getInt(cursor.getColumnIndexOrThrow("is_followed"))
                user.isFollow = is_following == 1

                // 解析其他列
                userList.add(user)
            }
            cursor.close()
        }

        return userList
    }

    fun queryMutualFromView(userId: Long, limit: Int): List<User> {
        val userList: MutableList<User> = ArrayList()

        // 查询条件
        //String selection = COLUMN_ID + " >= ?";
        //String[] selectionArgs = { String.valueOf(userId) };

        // 排序方式
        val sortOrder = COLUMN_ID + " ASC"

        // 构建视图的查询语句
        val viewQuery = "SELECT * FROM " + VIEW_MUTUAL +  //" WHERE " + selection +
                " ORDER BY " + sortOrder +
                " LIMIT " + limit

        // 执行查询操作
        val cursor = BaseDb.getInstance().writableDatabase.rawQuery(viewQuery, null)

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val user = User()
                user.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                user.name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                user.nick = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK))
                user.nick1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK1))
                user.nick2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK2))
                user.age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE))
                user.gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER))
                user.region = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION))
                user.icon = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON))
                user.follows = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS))
                user.fans = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS))
                user.introduction = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_INTRODUCTION
                    )
                )
                user.lastLoginTime = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_LAST_LOGIN_TIME
                    )
                )
                user.isOnline = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1L
                user.mask = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK))
                user.cryptKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY))
                user.cryptType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE))

                // 解析其他列
                userList.add(user)
            }
            cursor.close()
        }

        return userList
    }

    // 群组成员
    fun insertOrReplaceGroupMember(gid: Long, uid: Long, role: Int, nick: String?): Long {
        val values = ContentValues()
        values.put(COLUMN_GID, gid)
        values.put(COLUMN_UID, uid)
        values.put(COLUMN_ROLE, role)
        values.put(COLUMN_NICK3, nick)

        // 使用 insertWithOnConflict() 方法实现插入或替换
        val result = BaseDb.getInstance().writableDatabase.insertWithOnConflict(
            TABLE_GROUP_MEMBERS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        if (result == -1L) {
            // 插入或替换失败
        } else {
            // 插入或替换成功
        }
        return result
    }

    // 从 group_members 表中删除特定的记录
    fun deleteGroupMember(gid: Long, uid: Long) {
        // 定义删除条件
        val whereClause = "gid=? AND uid=?"
        val whereArgs = arrayOf(gid.toString(), uid.toString())

        // 执行删除操作
        val rowsDeleted = BaseDb.getInstance().writableDatabase.delete(
            TABLE_GROUP_MEMBERS,
            whereClause,
            whereArgs
        )

        if (rowsDeleted > 0) {
            // 删除成功
        } else {
            // 删除失败或未找到匹配的行
        }
    }

    // 查询视图中从某个 uid 开始的若干条 User 记录
    fun queryUsersFromGroupMemsView(gid: Long, uid: Long, limit: Int): List<User> {
        val userList: MutableList<User> = ArrayList()
        // 查询条件
        val selection = "gid = ? AND uid >= ?"
        val selectionArgs = arrayOf(gid.toString(), uid.toString())

        // 排序方式
        val sortOrder = COLUMN_UID + " ASC"

        // 构建视图的查询语句
        val viewQuery = "SELECT * FROM " + VIEW_GROUP_MEMS +
                " WHERE " + selection +
                " ORDER BY " + sortOrder +
                " LIMIT " + limit

        // 执行查询操作
        val cursor = BaseDb.getInstance().writableDatabase.rawQuery(viewQuery, selectionArgs)

        // 解析查询结果
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val user = User()
                user.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UID))
                user.name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                user.nick = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK))
                user.nick3 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK3))
                user.age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE))
                user.gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER))
                user.region = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION))
                user.icon = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON))
                user.follows = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOLLOWS))
                user.fans = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FANS))
                user.introduction = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_INTRODUCTION
                    )
                )
                user.lastLoginTime = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        COLUMN_LAST_LOGIN_TIME
                    )
                )
                user.isOnline = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_IS_ONLINE)) == 1L
                user.mask = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK))
                user.cryptKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_KEY))
                user.cryptType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE))

                // 解析其他列
                userList.add(user)
            }
            cursor.close()
        }

        return userList
    }


    // 向 TABLE_ACCOUNT 表中插入或替换数据
    fun insertOrReplaceAccount(user: User): Boolean {
        val values = ContentValues()
        values.put(COLUMN_ID, user.id)
        values.put(COLUMN_NICK, user.nick)
        values.put(COLUMN_ICON, user.icon)
        values.put(COLUMN_PWD, user.pwd)
        values.put(COLUMN_EMAIL, user.email)
        values.put(COLUMN_PHONE, user.phone)
        values.put(COLUMN_PRINT, user.sharedPrint)
        values.put(COLUMN_SHARED_KEY, user.sharedKey)
        values.put(COLUMN_CRYPT_TYPE, user.cryptType)

        // 使用 insertWithOnConflict() 方法实现插入或替换
        val result = BaseDb.getInstance().writableDatabase.insertWithOnConflict(
            TABLE_ACCOUNT,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        if (result == -1L) {
            // 插入或替换失败
            return false
        } else {
            // 插入或替换成功
        }
        return true
    }

    // 根据 id 查询 User
    fun getAccount(id: Long): User? {
        var user: User? = null

        // 查询条件
        val selection = "id=?"
        val selectionArgs = arrayOf(id.toString())

        // 执行查询操作
        val cursor = BaseDb.getInstance().writableDatabase.query(
            TABLE_ACCOUNT,  // 表名
            null,  // 返回所有列
            selection,  // 查询条件
            selectionArgs,  // 查询条件的参数值
            null,  // 不需要 group by
            null,  // 不需要 having
            null // 不需要 order by
        )

        // 解析查询结果
        if (cursor != null && cursor.moveToFirst()) {
            user = User()
            user.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            user.nick = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK))
            user.icon = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON))
            user.pwd = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PWD))
            user.email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
            user.phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE))
            user.sharedPrint = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PRINT))
            user.sharedKey = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_SHARED_KEY))
            user.cryptType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CRYPT_TYPE))

            // 解析其他列
        }

        // 关闭游标
        cursor?.close()

        return user
    }
}
