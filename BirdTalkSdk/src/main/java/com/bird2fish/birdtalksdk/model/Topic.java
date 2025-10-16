package com.bird2fish.birdtalksdk.model;

public class Topic {
    private long tid;
    private long syncId;
    private long readId;
    private int type;
    private int visible;
    private String title;
    private String icon;

    private long unReadCount;
    private MessageContent lastMsg;
    private int mute;  // 小写首字母，符合Java规范

    // 默认构造函数
    public Topic() {
        this(0L, 0L, 0L, 0, 1, "", "sys:11");
    }

    // 主构造函数
    public Topic(long tid, long syncId, long readId, int type, int visible, String title, String icon) {
        this.tid = tid;
        this.syncId = syncId;
        this.readId = readId;
        this.type = type;
        this.visible = visible;
        this.title = title != null ? title : "";
        this.icon = icon != null ? icon : "sys:11";
        this.unReadCount = 0;
        this.lastMsg = null;
        this.mute = 0;  // 默认不静音
    }

    // getter/setter
    public long getTid() {
        return tid;
    }

    public void setTid(long tid) {
        this.tid = tid;
    }

    public long getSyncId() {
        return syncId;
    }

    public void setSyncId(long syncId) {
        this.syncId = syncId;
    }

    public long getReadId() {
        return readId;
    }

    public void setReadId(long readId) {
        this.readId = readId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getVisible() {
        return visible;
    }

    public void setVisible(int visible) {
        this.visible = visible;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon != null ? icon : "sys:11";
    }

    public long getUnReadCount() {
        return unReadCount;
    }

    public void setUnReadCount(long unReadCount) {
        this.unReadCount = unReadCount;
    }

    public MessageContent getLastMsg() {
        return lastMsg;
    }

    public void setLastMsg(MessageContent lastMsg) {
        this.lastMsg = lastMsg;
    }

    public int getMute() {
        return mute;
    }

    public void setMute(int mute) {
        this.mute = mute;
    }

    @Override
    public String toString() {
        return "Topic{" +
                "tid=" + tid +
                ", syncId=" + syncId +
                ", readId=" + readId +
                ", type=" + type +
                ", visible=" + visible +
                ", title='" + title + '\'' +
                ", icon='" + icon + '\'' +
                ", unReadCount=" + unReadCount +
                ", lastMsg=" + (lastMsg != null ? lastMsg.toString() : "null") +
                ", mute=" + mute +
                '}';
    }
}
