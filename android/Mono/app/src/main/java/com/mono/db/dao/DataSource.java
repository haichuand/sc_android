package com.mono.db.dao;

import com.mono.db.Database;

import java.util.Random;

public abstract class DataSource {

    protected Database database;
    private static final Random random = new Random();
    public DataSource(Database database) {
        this.database = database;
    }

    public static String UniqueIdGenerator (String modelName) {
        //todo: the userid should be changed to the real userid of thie device
        return "userid_" + modelName + Long.toString(System.currentTimeMillis()) + Long.toString(random.nextLong());
    }
}
