package com.mono.db.dao;

import com.mono.db.Database;

import java.util.Random;

public abstract class DataSource {

    protected Database database;
    
    public DataSource(Database database) {
        this.database = database;
    }
}
