package com.mono;

import android.content.Context;

import com.mono.db.DatabaseHelper;
import com.mono.db.dao.LocationDataSource;
import com.mono.model.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * This manager class is used to centralize all location related actions such as retrieve location
 * from the database. Location retrieved are also cached here to improve efficiency.
 *
 * @author Gary Ng
 */
public class LocationManager {

    private static LocationManager instance;

    private Context context;

    private final Map<Long, Location> cache = new HashMap<>();

    private LocationManager(Context context) {
        this.context = context;
    }

    public static LocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocationManager(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Insert a location into the cache.
     *
     * @param location The instance of a location.
     */
    public void add(Location location) {
        cache.put(location.id, location);
    }

    /**
     * Retrieve a location using the ID.
     *
     * @param id The value of the location ID.
     * @return an instance of the location.
     */
    public Location getLocation(long id) {
        Location location;

        if (cache.containsKey(id)) {
            location = cache.get(id);
        } else {
            LocationDataSource dataSource =
                DatabaseHelper.getDataSource(context, LocationDataSource.class);
            location = dataSource.getLocation(id);
            // Cache Location
            if (location != null) {
                add(location);
            }
        }

        return location;
    }
}
