package com.bird2fish.birdtalksdk.model;

import android.util.Base64;

public class User {
    private long id;
    private String name;
    private String nick;
    private String nick1;
    private String nick2;
    private String nick3;
    private String pwd;
    private long gid;
    private int age;
    private String gender;
    private String region;
    private String icon;
    private int follows;
    private int fans;
    private boolean isFollow;
    private boolean isFan;
    private String introduction;
    private String lastLoginTime;
    private boolean isOnline;
    private int mask;
    private String cryptKey;
    private String cryptType;
    private long sharedPrint;
    private byte[] sharedKey;

    private String phone;
    private String email;

    // Constructor
    public User() {
        // Default constructor
        id = 0;
        gid =0;
        age = 0;
        fans = 0;
        follows = 0;
        isFan = false;
        isFollow = false;
        mask = 0;
        sharedKey = null;

        this.name = "";
        this.nick = "";
        this.nick1 = "";
        this.nick2 = "";
        this.nick3 = "";
        this.pwd = "";
        this.gender = "";
        this.region = "";
        this.icon = "";
        this.introduction = "";
        this.lastLoginTime = "";
        this.cryptKey = "";
        this.cryptType = "";
        this.phone = "";
        this.email = "";
    }

    // Getters and setters for all fields

    public String getPhone() {return phone;}
    public void setPhone(String p) {this.phone = p;}
    public String getEmail() {return this.email;}
    public void setEmail(String e){this.email = e;}
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getNick1() {
        return nick1;
    }

    public void setNick1(String nick1) {
        this.nick1 = nick1;
    }

    public String getNick2() {
        return nick2;
    }

    public void setNick2(String nick2) {
        this.nick2 = nick2;
    }

    public String getNick3() {
        return nick3;
    }

    public void setNick3(String nick3) {
        this.nick3 = nick3;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getFollows() {
        return follows;
    }

    public void setFollows(int follows) {
        this.follows = follows;
    }

    public int getFans() {
        return fans;
    }

    public void setFans(int fans) {
        this.fans = fans;
    }

    public boolean isFollow() {
        return isFollow;
    }

    public void setFollow(boolean follow) {
        isFollow = follow;
    }

    public boolean isFan() {
        return isFan;
    }

    public void setFan(boolean fan) {
        isFan = fan;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public String getCryptKey() {
        return cryptKey;
    }

    public void setCryptKey(String cryptKey) {
        this.cryptKey = cryptKey;
    }

    public String getCryptType() {
        return cryptType;
    }

    public void setCryptType(String cryptType) {
        this.cryptType = cryptType;
    }

    public long getSharedPrint() {
        return sharedPrint;
    }

    public void setSharedPrint(long sharedPrint) {
        this.sharedPrint = sharedPrint;
    }

    public byte[] getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(byte[] sharedKey) {
        this.sharedKey = sharedKey;
    }

    private String nullToText(String value) {
        return value != null ?   value : "null";
    }

    // toString() method for debugging
    @Override
    public String toString() {
        String sharedKeyBase64 = "";
        if (sharedKey == null) {

        }else{
            sharedKeyBase64 = Base64.encodeToString(sharedKey, Base64.DEFAULT);
        }
        return "User{" +
                "id=" + id +
                ", name=" + nullToText(name) +
                ", nick=" + nullToText(nick) +
                ", nick1=" + nullToText(nick1) +
                ", nick2=" + nullToText(nick2) +
                ", nick3=" + nullToText(nick3) +
                ", pwd=" + nullToText(pwd) +
                ", gid=" + gid +
                ", age=" + age +
                ", gender=" + nullToText(gender) +
                ", region=" + nullToText(region) +
                ", icon=" + nullToText(icon) +
                ", follows=" + follows +
                ", fans=" + fans +
                ", isFollow=" + isFollow +
                ", isFan=" + isFan +
                ", introduction=" + nullToText(introduction) +
                ", lastLoginTime=" + nullToText(lastLoginTime) +
                ", isOnline=" + isOnline +
                ", mask=" + mask +
                ", cryptKey=" + nullToText(cryptKey) +
                ", cryptType=" + nullToText(cryptType) +
                ", sharedPrint=" + sharedPrint +
                ", sharedKey=" + nullToText(sharedKeyBase64) +
                '}';
    }
}
