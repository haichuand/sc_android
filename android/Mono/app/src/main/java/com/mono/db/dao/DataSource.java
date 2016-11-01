package com.mono.db.dao;

import com.mono.db.Database;

/**
 * This abstract class is used to store a reference of the database and to be extended to include
 * methods to handle CRUD functions.
 *
 * @author Gary Ng
 */
public abstract class DataSource {

    protected Database database;

    public DataSource(Database database) {
        this.database = database;
    }
}
