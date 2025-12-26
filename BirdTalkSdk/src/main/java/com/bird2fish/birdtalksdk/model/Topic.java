package com.bird2fish.birdtalksdk.model;

import com.bird2fish.birdtalksdk.db.TopicFlag;

public class Topic {
    private long tid;
    private long syncId;
    private long readId;
    private int type;
    private int data;
    private String title;
    private String icon;

    private long unReadCount;
    private MessageContent lastMsg;
    private boolean mute;  // 小写首字母，符合Java规范
    private boolean showHide; // 是否显示
    private boolean pinned;     // 置顶

    // 默认构造函数
    public Topic() {
        this(0L, 0L, 0L, 0, 1, "", "sys:11");
    }

    // 主构造函数
    public Topic(long tid, long syncId, long readId, int type, int data, String title, String icon) {
        this.tid = tid;
        this.syncId = syncId;
        this.readId = readId;
        this.type = type;
        this.data = data;
        this.title = title != null ? title : "";
        this.icon = icon != null ? icon : "sys:11";
        this.unReadCount = 0;
        this.lastMsg = null;
        this.mute = false;  // 默认不静音
        this.showHide = true;
        this.pinned = false;
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

    public int getData() {
        return data;
    }

    public void setData(int mask) {
        this.data = mask;
        this.mute = (mask & TopicFlag.MUTE) != 0;
        this.showHide = (mask & TopicFlag.VISIBLE) != 0;
        this.pinned = (mask & TopicFlag.PINNED) != 0;
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

    public boolean getMute() {
        return this.mute;
    }

    public boolean getPinned(){
        return this.pinned;
    }

    public boolean getShowHide(){
        return this.showHide;
    }
    public void setMute(boolean mute) {
        this.mute = mute;
        updateFlag(TopicFlag.MUTE, mute);
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        updateFlag(TopicFlag.PINNED, pinned);
    }

    public void setShowHide(boolean visible) {
        this.showHide = visible;
        updateFlag(TopicFlag.VISIBLE, visible);
    }

    private void updateFlag(int flag, boolean enable) {
        if (enable) {
            data |= flag;
        } else {
            data &= ~flag;
        }
    }

    public long getTm(){
        if (this.lastMsg == null){
            return 0L;
        }

        return this.lastMsg.getTm();
    }

    @Override
    public String toString() {
        return "Topic{" +
                "tid=" + tid +
                ", syncId=" + syncId +
                ", readId=" + readId +
                ", type=" + type +
                ", visible=" + data +
                ", title='" + title + '\'' +
                ", icon='" + icon + '\'' +
                ", unReadCount=" + unReadCount +
                ", lastMsg=" + (lastMsg != null ? lastMsg.toString() : "null") +
                ", mute=" + mute +
                '}';
    }
}
