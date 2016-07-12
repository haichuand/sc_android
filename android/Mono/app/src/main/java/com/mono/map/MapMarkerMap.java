package com.mono.map;

import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to store markers using a combination of two hash maps to be used on the map.
 * Both hash maps work together to perform a reverse look up of the event ID using the marker
 * as the key.
 *
 * @author Gary Ng
 */
public class MapMarkerMap {

    private final Map<Marker, String> markers = new HashMap<>();
    private final Map<String, MapMarker> mapMarkers = new HashMap<>();

    public void clear() {
        markers.clear();

        for (MapMarker mapMarker : mapMarkers.values()) {
            mapMarker.remove();
        }
        mapMarkers.clear();
    }

    public MapMarker get(String id) {
        return mapMarkers.get(id);
    }

    public MapMarker get(Marker marker) {
        return get(markers.get(marker));
    }

    public Set<String> getIds() {
        return mapMarkers.keySet();
    }

    public void put(String id, MapMarker mapMarker) {
        markers.put(mapMarker.getActualMarker(), id);
        mapMarkers.put(id, mapMarker);
    }

    public boolean remove(String id) {
        MapMarker mapMarker = mapMarkers.remove(id);

        if (mapMarker != null) {
            mapMarker.remove();
            return true;
        }

        return false;
    }
}
