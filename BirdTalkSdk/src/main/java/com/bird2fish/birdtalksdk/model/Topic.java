package com.bird2fish.birdtalksdk.model;

public class Topic {
    private long tid;
    private int syncId;
    private int readId;
    private int type;
    private int visible;
    private String title;
    private String icon;

    public Topic() {
        // Default constructor required by SQLite
        title = "";
        icon = "";
    }

    public Topic(long tid, int syncId, int readId, int type, int visible, String title, String icon) {
        this.tid = tid;
        this.syncId = syncId;
        this.readId = readId;
        this.type = type;
        this.visible = visible;
        this.title = title;
        this.icon = icon;
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
                '}';
    }


    public long getTid() {
        return tid;
    }

    public void setTid(long tid) {
        this.tid = tid;
    }

    public int getSyncId() {
        return syncId;
    }

    public void setSyncId(int syncId) {
        this.syncId = syncId;
    }

    public int getReadId() {
        return readId;
    }

    public void setReadId(int readId) {
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
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
