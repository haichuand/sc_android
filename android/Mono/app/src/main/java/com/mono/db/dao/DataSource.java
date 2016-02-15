package com.mono.db.dao;

import com.mono.db.Database;

public abstract class DataSource {

    protected Database database;

    public DataSource(Database database) {
        this.database = database;
    }
}
