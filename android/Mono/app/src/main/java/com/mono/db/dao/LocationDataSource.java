package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Location;

/**
 * Created by xuejing on 2/20/16.
 */
public class LocationDataSource extends DataSource {

    private LocationDataSource(Database database) {
        super(database);
    }

    public long createLocation(String name, String googlePlaceId, Double latitude,
            Double longitude, String address) {
        long id = -1;

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Location.NAME, name);
        values.put(DatabaseValues.Location.GOOGLE_PLACE_ID, googlePlaceId);
        values.put(DatabaseValues.Location.LATITUDE, latitude);
        values.put(DatabaseValues.Location.LONGITUDE, longitude);
        values.put(DatabaseValues.Location.ADDRESS, address);
        values.put(DatabaseValues.Location.BEEN_THERE, 0); // when fisrt time write a location to database, we assume user never been there before

        try {
            long rowId = database.insert(DatabaseValues.Location.TABLE, values);

            Cursor cursor = database.select(
                DatabaseValues.Location.TABLE,
                new String[]{
                    DatabaseValues.Location.ID
                },
                "`rowid` = ?",
                new String[]{
                    String.valueOf(rowId)
                }
            );

            if (cursor.moveToNext()) {
                id = cursor.getLong(0);
            }

            cursor.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Log.d("LocationDataSource", " create location: " + id + " name: " + name + " address: " + address);

        return id;
    }

    public void createLocationAsCandidates (String name, String googlePlaceId, Double latitude, Double longitude, String address, long eventId) {
        long locId = createLocation(name, googlePlaceId, latitude, longitude, address);
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.EventLocationCandidates.EVENT_ID, eventId);
        values.put(DatabaseValues.EventLocationCandidates.LOC_ID, locId);

        try {
            database.insert(DatabaseValues.EventLocationCandidates.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Location getLocation(long id) {
        Location location = null;

        Cursor cursor = database.select(
            DatabaseValues.Location.TABLE,
            DatabaseValues.Location.PROJECTION,
            DatabaseValues.Location.ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );

        if (cursor.moveToNext()) {
            location = cursorToLocation(cursor);
        }

        cursor.close();

        return location;
    }

    public Location getLocationByGooglePlaceId(String googlePlaceId) {
        Location location = null;

        Cursor cursor = database.select(
                DatabaseValues.Location.TABLE,
                DatabaseValues.Location.PROJECTION,
                DatabaseValues.Location.GOOGLE_PLACE_ID + " = ?",
                new String[]{
                        String.valueOf(googlePlaceId)
                }
        );

        if (cursor.moveToNext()) {
            location = cursorToLocation(cursor);
        }

        cursor.close();

        return location;
    }

    public int updateValues(String id, ContentValues values) {
        Log.d("LLocationDataSource", "updateing location with id " + id);
        return database.update(
                DatabaseValues.Location.TABLE,
                values,
                DatabaseValues.Location.ID + " = ?",
                new String[]{
                        String.valueOf(id)
                }
        );
    }

    public int updateLocationAddress(String id, String newAddress) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Location.ADDRESS, newAddress);

        return updateValues(id, values);
    }

    public int removeLocation(String id) {
        return database.delete(
                DatabaseValues.Location.TABLE,
                DatabaseValues.Location.ID + " = ?",
                new String[]{
                        String.valueOf(id)
                }
        );
    }

    /**
     * For PROJECTION only.
     */
    private Location cursorToLocation(Cursor cursor) {
        Location location = new Location(cursor.getLong(DatabaseValues.Location.INDEX_ID));
        location.name = cursor.getString(DatabaseValues.Location.INDEX_NAME);
        location.googlePlaceId = cursor.getString(DatabaseValues.Location.INDEX_GOOGLE_PLACE_ID);

        if (!cursor.isNull(DatabaseValues.Location.INDEX_LATITUDE) &&
                !cursor.isNull(DatabaseValues.Location.INDEX_LONGITUDE)) {
            double latitude = cursor.getDouble(DatabaseValues.Location.INDEX_LATITUDE);
            double longitude = cursor.getDouble(DatabaseValues.Location.INDEX_LONGITUDE);

            location.setLatLng(latitude, longitude);
        }

        String address = cursor.getString(DatabaseValues.Location.INDEX_ADDRESS);
        if (address != null) {
            location.address = address.split(",");
        }

        return location;
    }
}
