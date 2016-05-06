package com.mono.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.mono.util.Common;

public class Database {

    private SQLiteDatabase database;

    public Database(SQLiteDatabase database) {
        this.database = database;
    }

    public void execSQL(String sql) throws SQLException {
        database.execSQL(sql);
    }

    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        database.execSQL(sql, bindArgs);
    }

    public Cursor rawQuery(String sql) {
        return rawQuery(sql, null);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return database.rawQuery(sql, selectionArgs);
    }

    public Cursor select(String table, String[] projection) {
        return select(table, projection, null, null, null, null, null, null);
    }

    public Cursor select(String table, String[] projection, String selection,
            String[] selectionArgs) {
        return select(table, projection, selection, selectionArgs, null, null, null, null);
    }

    public Cursor select(String table, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return select(table, projection, selection, selectionArgs, null, sortOrder, null, null);
    }

    public Cursor select(String table, String[] projection, String sortOrder) {
        return select(table, projection, null, null, null, sortOrder, null, null);
    }

    public Cursor select(String table, String[] projection, String selection,
            String[] selectionArgs, String groupBy, String sortOrder, Integer offset,
            Integer limit) {
        return rawQuery(
            "SELECT " +
            Common.implode(", ", projection) +
            " FROM " + table +
            (!Common.isEmpty(selection) ? " WHERE " + selection : "") +
            (!Common.isEmpty(groupBy) ? " GROUP BY " + groupBy : "") +
            (!Common.isEmpty(sortOrder) ? " ORDER BY " + sortOrder : "") +
            (limit != null ? " LIMIT " + limit : "") +
            (offset != null ? " OFFSET " + offset : ""),
            selectionArgs
        );
    }

    public long insert(String table, ContentValues values) throws SQLException {
        return database.insertOrThrow(table, null, values);
    }

    public int update(String table, ContentValues values, String selection,
            String[] selectionArgs) {
        return database.update(table, values, selection, selectionArgs);
    }

    public int delete(String table, String selection, String[] selectionArgs) {
        return database.delete(table, selection, selectionArgs);
    }
}
