package com.bird2fish.birdtalksdk.model;

public class MessageData {
    private long id;
    private long tid;
    private long uid;
    private long sendId;
    private String devId;
    private int io;
    private String msgType;
    private byte[] data;
    private int isPlain;
    private long tm;
    private long tm1;
    private long tm2;
    private long tm3;
    private String cryptType;
    private int print;
    private String status;

    // Default constructor
    public MessageData() {
        this.msgType = "";
        this.devId = "";
        this.data = null;
    }

    // Constructor with all fields
    public MessageData(long id, long tid, long uid, long sendId, String devId, int io, String msgType, byte[] data,
                       int isPlain, long tm, long tm1, long tm2, long tm3, String cryptType, int print, String status) {
        this.id = id;
        this.tid = tid;
        this.uid = uid;
        this.sendId = sendId;
        this.devId = devId;
        this.io = io;
        this.msgType = msgType;
        this.data = data;
        this.isPlain = isPlain;
        this.tm = tm;
        this.tm1 = tm1;
        this.tm2 = tm2;
        this.tm3 = tm3;
        this.cryptType = cryptType;
        this.print = print;
        this.status = status;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTid() {
        return tid;
    }

    public void setTid(long tid) {
        this.tid = tid;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getSendId() {
        return sendId;
    }

    public void setSendId(long sendId) {
        this.sendId = sendId;
    }

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public int getIo() {
        return io;
    }

    public void setIo(int io) {
        this.io = io;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getIsPlain() {
        return isPlain;
    }

    public void setIsPlain(int isPlain) {
        this.isPlain = isPlain;
    }

    public long getTm() {
        return tm;
    }

    public void setTm(long tm) {
        this.tm = tm;
    }

    public long getTm1() {
        return tm1;
    }

    public void setTm1(long tm1) {
        this.tm1 = tm1;
    }

    public long getTm2() {
        return tm2;
    }

    public void setTm2(long tm2) {
        this.tm2 = tm2;
    }

    public long getTm3() {
        return tm3;
    }

    public void setTm3(long tm3) {
        this.tm3 = tm3;
    }

    public String getCryptType() {
        return cryptType;
    }

    public void setCryptType(String cryptType) {
        this.cryptType = cryptType;
    }

    public int getPrint() {
        return print;
    }

    public void setPrint(int print) {
        this.print = print;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "MessageData{" +
                "id=" + id +
                ", tid=" + tid +
                ", uid=" + uid +
                ", sendId=" + sendId +
                ", devId='" + devId + '\'' +
                ", io=" + io +
                ", msgType='" + msgType + '\'' +
                ", data=" + (data != null ? new String(data) : "null") +
                ", isPlain=" + isPlain +
                ", tm=" + tm +
                ", tm1=" + tm1 +
                ", tm2=" + tm2 +
                ", tm3=" + tm3 +
                ", cryptType='" + cryptType + '\'' +
                ", print=" + print +
                ", status='" + status + '\'' +
                '}';
    }
}

