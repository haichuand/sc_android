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
        return Long.toString(random.nextLong()) + "_" + modelName + Long.toString(System.currentTimeMillis()) + Long.toString(random.nextLong());
    }

    public static String UniqueIdGenerator (String userId, String modelName) {
        return userId + "_" + modelName + Long.toString(System.currentTimeMillis()) + Long.toString(random.nextLong());
    }
}
