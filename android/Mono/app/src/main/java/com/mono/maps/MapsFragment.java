package com.mono.maps;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.settings.Settings;
import com.mono.util.Colors;
import com.mono.util.LocationHelper;
import com.mono.util.LocationHelper.LocationCallback;
import com.mono.util.SimpleTabLayout.TabPagerCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsFragment extends Fragment implements OnMapReadyCallback, OnMapClickListener,
        OnMarkerClickListener, InfoWindowAdapter, OnInfoWindowClickListener,
        EventBroadcastListener, TabPagerCallback {

    private static final float DEFAULT_ZOOM_LEVEL = 12.5f;
    private static final float DEFAULT_TAP_ZOOM_LEVEL = 14f;
    private static final int CIRCLE_STROKE_WIDTH = 4;
    private static final int LINE_WIDTH = 4;

    private static final int NUM_DAYS = 3;

    private MainInterface mainInterface;
    private EventManager eventManager;

    private GoogleMap map;

    private final Map<Marker, String> markers = new HashMap<>();
    private final Map<String, MapMarker> mapMarkers = new HashMap<>();
    private final Map<Integer, BitmapDescriptor> markerBitmapCache = new HashMap<>();

    private MapMarker currentMarker;
    private int mapType;

    private AsyncTask<Long, Void, List<? extends Event>> task;
    private final List<MapMarker> sortedMarkers = new ArrayList<>();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        eventManager = EventManager.getInstance(context);

        if (context instanceof MainInterface) {
            mainInterface = (MainInterface) context;
        }

        mapType = Settings.getInstance(context).getMapType();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        eventManager.addEventBroadcastListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        String tag = getString(R.string.fragment_maps);

        FragmentManager manager = getChildFragmentManager();
        SupportMapFragment fragment = (SupportMapFragment) manager.findFragmentByTag(tag);

        if (fragment == null) {
            fragment = SupportMapFragment.newInstance();
            fragment.getMapAsync(this);
        }

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.map_container, fragment, tag);
        transaction.commit();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        eventManager.removeEventBroadcastListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.maps, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_clear:
                clear();
                return true;
        }

        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(mapType);
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setInfoWindowAdapter(this);
        map.setOnInfoWindowClickListener(this);

        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);

        centerLastKnownLocation(false);

        refresh();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (currentMarker != null) {
            currentMarker.onMarkerClick(false);
            currentMarker = null;

            setZoomLevel(DEFAULT_ZOOM_LEVEL, true);
        }

        createDummyCircle(latLng);
    }

    private void createDummyCircle(LatLng latLng) {
        int[] colors = {Colors.BROWN, Colors.BROWN_LIGHT, Colors.LAVENDAR};
        int color = colors[(int) (Math.random() * colors.length) % colors.length];
        drawCircle(latLng, 500, color | 0x80000000, color | 0xFF000000);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (currentMarker != null) {
            currentMarker.onMarkerClick(false);
            currentMarker = null;
        }

        currentMarker = getMapMarker(marker);
        currentMarker.onMarkerClick(true);

        marker.showInfoWindow();
        centerLocation(marker.getPosition(), DEFAULT_TAP_ZOOM_LEVEL, true);

        return true;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        MapMarker mapMarker = getMapMarker(marker);
        return null;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        MapMarker mapMarker = getMapMarker(marker);
    }

    @Override
    public void onEventBroadcast(EventAction data) {
        switch (data.getAction()) {
            case EventAction.ACTION_CREATE:
                break;
            case EventAction.ACTION_UPDATE:
                break;
            case EventAction.ACTION_REMOVE:
                break;
        }
    }

    @Override
    public int getPageTitle() {
        return R.string.map;
    }

    @Override
    public ViewPager getTabLayoutViewPager() {
        return null;
    }

    @Override
    public ActionButton getActionButton() {
        return new ActionButton(R.drawable.ic_location, 0, new OnClickListener() {
            @Override
            public void onClick(View view) {
                centerLastKnownLocation(true);
            }
        });
    }

    @Override
    public void onPageSelected() {

    }

    private MapMarker getMapMarker(Marker marker) {
        return mapMarkers.get(markers.get(marker));
    }

    private void putMarkers(String id, Marker marker, MapMarker mapMarker) {
        markers.put(marker, id);
        mapMarkers.put(id, mapMarker);
    }

    private BitmapDescriptor getMarkerIcon(int resId) {
        if (markerBitmapCache.containsKey(resId)) {
            return markerBitmapCache.get(resId);
        }

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(resId);
        markerBitmapCache.put(resId, icon);

        return icon;
    }

    private Circle drawCircle(LatLng position, double radius, int color, int strokeColor) {
        CircleOptions options = new CircleOptions();
        options.center(position);
        options.radius(radius);
        options.fillColor(color);
        options.strokeColor(strokeColor);
        options.strokeWidth(CIRCLE_STROKE_WIDTH);

        return map.addCircle(options);
    }

    private Marker drawMarker(LatLng position, int iconResId) {
        MarkerOptions options = new MarkerOptions();
        options.anchor(0.5f, 0.5f);
        options.position(position);

        if (iconResId == 0) {
            iconResId = R.drawable.ic_star_border;
        }
        options.icon(getMarkerIcon(iconResId));

        return map.addMarker(options);
    }

    private Polyline drawLine(LatLng from, LatLng to, int color) {
        PolylineOptions options = new PolylineOptions();
        options.add(from, to);
        options.color(color);
        options.width(LINE_WIDTH);

        return map.addPolyline(options);
    }

    public void setMapType(int type) {
        map.setMapType(mapType = type);
        Settings.getInstance(getContext()).setMapType(type);
    }

    public void centerLocation(LatLng latLng, float zoom, boolean animate) {
        CameraUpdate update;

        if (zoom > 0) {
            update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        } else {
            update = CameraUpdateFactory.newLatLng(latLng);
        }

        if (animate) {
            map.animateCamera(update);
        } else {
            map.moveCamera(update);
        }
    }

    public void centerLastKnownLocation(final boolean animate) {
        LocationHelper.getLastKnownLatLng(getActivity(), new LocationCallback() {
            @Override
            public void onFinish(Location location) {
                if (location != null) {
                    LatLng latLng = new LatLng(location.latitude, location.longitude);
                    centerLocation(latLng, DEFAULT_ZOOM_LEVEL, animate);
                }
            }
        });
    }

    public void setZoomLevel(float zoom, boolean animate) {
        CameraUpdate update = CameraUpdateFactory.zoomTo(zoom);

        if (animate) {
            map.animateCamera(update);
        } else {
            map.moveCamera(update);
        }
    }

    public void refresh() {
        if (task != null) {
            task.cancel(true);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -NUM_DAYS);

        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        task = new AsyncTask<Long, Void, List<? extends Event>>() {
            @Override
            protected List<? extends Event> doInBackground(Long... params) {
                long startTime = params[0];
                long endTime = params[1];

                return eventManager.getEvents(startTime, endTime);
            }

            @Override
            protected void onPostExecute(List<? extends Event> result) {
                showAll(result, true);
                task = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, startTime, endTime);
    }

    public void clear() {
        map.clear();
        markers.clear();

        for (MapMarker mapMarker : mapMarkers.values()) {
            mapMarker.clear();
        }
        mapMarkers.clear();

        currentMarker = null;
    }

    public void show(String id, double latitude, double longitude, int iconResId, double radius,
            int color, int strokeColor, Event event) {
        if (mapMarkers.containsKey(id)) {
            return;
        }

        LatLng position = new LatLng(latitude, longitude);
        Marker marker = drawMarker(position, iconResId);

        MapMarker<Event> mapMarker = new MapMarker<>(id, marker, event);

        Circle circle = drawCircle(position, radius, color, strokeColor);
        mapMarker.setCircle(circle);

        putMarkers(id, mapMarker.marker, mapMarker);
    }

    public void showAll(List<? extends Event> events, boolean clear) {
        if (clear) {
            clear();
        }

        for (Event event : events) {
            Location location = event.location;
            if (location != null && location.containsLatLng()) {
                show(event.id, location.latitude, location.longitude, 0, 0, 0, 0, event);
            }
        }

        sortMarkersByTime();
    }

    public void sortMarkersByTime() {
        Collection<MapMarker> markers = mapMarkers.values();
        sortedMarkers.clear();

        for (MapMarker marker : markers) {
            if (marker.marker != null) {
                sortedMarkers.add(marker);
            }
        }

        Collections.sort(sortedMarkers, new Comparator<MapMarker>() {
            @Override
            public int compare(MapMarker m1, MapMarker m2) {
                return Long.compare(m1.getStartTime(), m2.getStartTime());
            }
        });
    }
}
