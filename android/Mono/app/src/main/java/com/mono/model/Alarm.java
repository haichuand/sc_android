package com.mono.model;

/**
 * Created by xuejing on 3/10/16.
 */
public class Alarm {
    String id;
    long alarmTime;
    long createTime;
    boolean enabled;

    public Alarm(String id, long alarmTime, long createTime) {
        this.id = id;
        this.alarmTime = alarmTime;
        this.createTime = createTime;
    }

    public Alarm (String id, long alarmTime, long createTime, boolean enabled) {
        this.id = id;
        this.alarmTime = alarmTime;
        this.createTime = createTime;
        this.enabled = enabled;
    }

    public String getAlarmId() {
        return this.id;
    }

    public long getAlarmTime() {
        return this.alarmTime;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setAlarmTime(long alarmTime) {
        this.alarmTime = alarmTime;
    }

    public void enableAlarm() {
        this.enabled = true;
    }

    public void disableAlarm() {
        this.enabled = false;
    }
    //for testing purpose
    public String toString() {
        return "Alarm id: " + this.id + ", alarmtime: " + this.alarmTime + ", createTime: "+ this.createTime + ", enabled: "+ this.enabled;
    }
 }
