package com.bird2fish.birdtalksdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bird2fish.birdtalksdk.model.Group;

import java.util.ArrayList;
import java.util.List;

/*
group_id        群ID（主键）
owner_id        群主ID
name            群名称
brief           群简介
icon            群头像
members_count   成员数量
mask            状态掩码
chat_type       聊天类型（私密/普通/频道等）
visible_type    可见性
join_type       加群方式
question        入群问题
answer          入群答案

 */
public class GroupDbHelper {

    // 表名
    public static final String TABLE_GROUPS = "groups";

    // 列名
    public static final String COLUMN_GROUP_ID = "gid";
    public static final String COLUMN_OWNER_ID = "owner_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_BRIEF = "brief";
    public static final String COLUMN_ICON = "icon";
    public static final String COLUMN_MEMBERS_COUNT = "members_count";
    public static final String COLUMN_MASK = "mask";
    public static final String COLUMN_CHAT_TYPE = "chat_type";
    public static final String COLUMN_VISIBLE_TYPE = "visible_type";
    public static final String COLUMN_JOIN_TYPE = "join_type";
    public static final String COLUMN_QUESTION = "question";
    public static final String COLUMN_ANSWER = "answer";

    public static final String COLUMN_TAGS = "tags";

    // 创建 groups 表
    public static final String SQL_CREATE_GROUPS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_GROUPS + " (" +
                    COLUMN_GROUP_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_OWNER_ID + " INTEGER," +
                    COLUMN_NAME + " TEXT," +
                    COLUMN_BRIEF + " TEXT," +
                    COLUMN_ICON + " TEXT," +
                    COLUMN_TAGS + " TEXT," +
                    COLUMN_MEMBERS_COUNT + " INTEGER," +
                    COLUMN_MASK + " INTEGER," +
                    COLUMN_CHAT_TYPE + " TEXT," +
                    COLUMN_VISIBLE_TYPE + " TEXT," +
                    COLUMN_JOIN_TYPE + " TEXT," +
                    COLUMN_QUESTION + " TEXT," +
                    COLUMN_ANSWER + " TEXT" +
                    ");";

    // onCreate
    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_GROUPS);
    }

    /////////////////////////////////////////////////////////////////////////////////
    // 插入或更新群信息
    public static long insertOrUpdateGroup(Group group) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_GROUP_ID, group.getTid());
        values.put(COLUMN_OWNER_ID, group.getOwnerId());
        values.put(COLUMN_NAME, group.getTitle());
        values.put(COLUMN_BRIEF, group.getBrief());
        values.put(COLUMN_ICON, group.getIcon());
        values.put(COLUMN_TAGS, group.getTags());
        values.put(COLUMN_MEMBERS_COUNT, group.getMembersCount());
        values.put(COLUMN_MASK, group.getData());
        values.put(COLUMN_CHAT_TYPE, group.getChatType());
        values.put(COLUMN_VISIBLE_TYPE, group.getVisibleType());
        values.put(COLUMN_JOIN_TYPE, group.getJoinType());
        values.put(COLUMN_QUESTION, group.getQuestion());
        values.put(COLUMN_ANSWER, group.getAnswer());

        return BaseDb.getInstance()
                .getWritableDatabase()
                .insertWithOnConflict(
                        TABLE_GROUPS,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                );
    }

    public static int insertOrUpdateGroups(List<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            return 0;
        }

        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();
        int count = 0;

        db.beginTransaction();
        try {
            for (Group group : groups) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_GROUP_ID, group.getTid());
                values.put(COLUMN_OWNER_ID, group.getOwnerId());
                values.put(COLUMN_NAME, group.getTitle());
                values.put(COLUMN_BRIEF, group.getBrief());
                values.put(COLUMN_ICON, group.getIcon());
                values.put(COLUMN_TAGS, group.getTags());
                values.put(COLUMN_MEMBERS_COUNT, group.getMembersCount());
                values.put(COLUMN_MASK, group.getData());
                values.put(COLUMN_CHAT_TYPE, group.getChatType());
                values.put(COLUMN_VISIBLE_TYPE, group.getVisibleType());
                values.put(COLUMN_JOIN_TYPE, group.getJoinType());
                values.put(COLUMN_QUESTION, group.getQuestion());
                values.put(COLUMN_ANSWER, group.getAnswer());

                long rowId = db.insertWithOnConflict(
                        TABLE_GROUPS,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                );

                if (rowId != -1) {
                    count++;
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
    }

    // 根据 group_id 查询群
    public static Group getGroupById(long groupId) {
        Group group = null;

        String selection = COLUMN_GROUP_ID + "=?";
        String[] selectionArgs = { String.valueOf(groupId) };

        Cursor cursor = BaseDb.getInstance()
                .getWritableDatabase()
                .query(
                        TABLE_GROUPS,
                        null,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null
                );

        if (cursor != null && cursor.moveToFirst()) {
            group = parseGroup(cursor);
            cursor.close();
        }

        return group;
    }

    // 正向分页查询群
    public static List<Group> findGroupsFrom(long groupId, int limit) {
        List<Group> list = new ArrayList<>();

        String selection = COLUMN_GROUP_ID + ">=?";
        String[] selectionArgs = { String.valueOf(groupId) };
        String orderBy = COLUMN_GROUP_ID + " ASC";

        Cursor cursor = BaseDb.getInstance()
                .getWritableDatabase()
                .query(
                        TABLE_GROUPS,
                        null,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        orderBy,
                        String.valueOf(limit)
                );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(parseGroup(cursor));
            }
            cursor.close();
        }

        return list;
    }

    // 删除群
    public static void deleteGroup(long groupId) {
        BaseDb.getInstance()
                .getWritableDatabase()
                .delete(
                        TABLE_GROUPS,
                        COLUMN_GROUP_ID + "=?",
                        new String[]{ String.valueOf(groupId) }
                );
    }

    public static void resetGroupTable() {
        String tableName = TABLE_GROUPS;
        SQLiteDatabase db = BaseDb.getInstance().getWritableDatabase();

        String dropTableSql = "DROP TABLE IF EXISTS " + tableName;
        db.execSQL(dropTableSql);

        GroupDbHelper.onCreate(db);
    }


    // 更新成员数量
    public static void updateMemberCount(long groupId, int count) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEMBERS_COUNT, count);

        BaseDb.getInstance()
                .getWritableDatabase()
                .update(
                        TABLE_GROUPS,
                        values,
                        COLUMN_GROUP_ID + "=?",
                        new String[]{ String.valueOf(groupId) }
                );
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Cursor -> Group
    private static Group parseGroup(Cursor cursor) {
        Group group = new Group();
        group.setTid(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID)));
        group.setOwnerId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_OWNER_ID)));
        group.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
        group.setBrief(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BRIEF)));
        group.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ICON)));
        group.setTags(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAGS)));
        group.setMembersCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MEMBERS_COUNT)));
        group.setData(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASK)));
        group.setChatType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_TYPE)));
        group.setVisibleType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VISIBLE_TYPE)));
        group.setJoinType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_JOIN_TYPE)));
        group.setQuestion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION)));
        group.setAnswer(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER)));
        return group;
    }
}