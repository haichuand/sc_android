package com.mono.map;

import android.content.Context;
import android.graphics.PorterDuff;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.EventDataSource;
import com.mono.map.MapMenuBar.MapMenuBarListener;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.settings.Settings;
import com.mono.util.Colors;
import com.mono.util.Constants;
import com.mono.util.LocationHelper;
import com.mono.util.LocationHelper.LocationCallback;
import com.mono.util.Pixels;
import com.mono.util.SimpleTabLayout.TabPagerCallback;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A fragment that displays a map showing location events between a specified time period. Events
 * selected can be viewed for more details or edited as well.
 *
 * @author Gary Ng
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, OnCameraIdleListener,
        OnMapClickListener, OnMarkerClickListener, InfoWindowAdapter, OnInfoWindowClickListener,
        EventBroadcastListener, TabPagerCallback, MapMenuBarListener {

    private static final SimpleDateFormat DATE_TIME_FORMAT;

    private static final float DEFAULT_ZOOM_LEVEL = 12.5f;
    private static final float DEFAULT_TAP_ZOOM_LEVEL = 14f;

    private static final int MODE_NONE = 0;
    private static final int MODE_TIMELINE = 1;

    private static final int OPTION_DIMENSION_DP = 24;
    private static final int OPTION_MARGIN_DP = 2;

    private static final float BASE_ALPHA = 0.4f;
    private static final int MARKER_COLOR_ID = R.color.colorPrimary;
    private static final int LINE_WIDTH = 5;

    private static final int TIMELINE_LIMIT = 100;
    private static final int LIMIT = 30;

    private static final float BOUNDS_PADDING_DP = 30f;

    private MainInterface mainInterface;
    private EventManager eventManager;

    private MapMenuBar menuBar;
    private GoogleMap map;

    private MapMarkerMap markers = new MapMarkerMap();

    private int mode;
    private MapMarker currentMarker;
    private int mapType;

    private AsyncTask<Object, Void, Map<String, List<Event>>> task;

    private long startTime;
    private long endTime;

    static {
        DATE_TIME_FORMAT = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        eventManager = EventManager.getInstance(context);

        if (context instanceof MainInterface) {
            mainInterface = (MainInterface) context;
        }

        mapType = Settings.getInstance(context).getMapType();
        setMode(MODE_TIMELINE);
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
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        menuBar = new MapMenuBar(getContext(), this);
        menuBar.onCreateView(view);

        String tag = getString(R.string.fragment_map);

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
                return true;
        }

        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(mapType);
        map.setOnCameraIdleListener(this);
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setInfoWindowAdapter(this);
        map.setOnInfoWindowClickListener(this);

        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);

        centerLastKnownLocation(false);

        startTime = System.currentTimeMillis() - Constants.DAY_MS;
        endTime = System.currentTimeMillis();
        refresh();
    }

    @Override
    public void onCameraIdle() {
        if (mode == MODE_NONE) {
            refresh();
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (currentMarker != null) {
            currentMarker.onMarkerClick(false);
            currentMarker = null;

            setZoomLevel(DEFAULT_ZOOM_LEVEL, true);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (currentMarker != null) {
            currentMarker.onMarkerClick(false);
            currentMarker = null;
        }

        currentMarker = markers.get(marker);
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
        MapMarker mapMarker = markers.get(marker);
        Event event = mapMarker.events.get(0);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.map_marker_info_event, null, false);

        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setColorFilter(event.color | 0xFF000000);

        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(event.title);

        String text = DATE_TIME_FORMAT.format(event.startTime) + " to " +
            DATE_TIME_FORMAT.format(event.endTime);
        TextView description = (TextView) view.findViewById(R.id.description);
        description.setText(text);

        ViewGroup options = (ViewGroup) view.findViewById(R.id.options);

        if (event.photos != null && !event.photos.isEmpty()) {
            createOption(options, R.drawable.ic_camera);
        }

        return view;
    }

    private void createOption(ViewGroup viewGroup, int resId) {
        Context context = getContext();

        ImageView image = new ImageView(context);
        image.setImageResource(resId);

        int color = Colors.getColor(context, R.color.lavender);
        image.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        int dimension = Pixels.pxFromDp(context, OPTION_DIMENSION_DP);
        int margin = Pixels.pxFromDp(context, OPTION_MARGIN_DP);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dimension, dimension);
        params.setMargins(margin, 0, margin, 0);

        viewGroup.addView(image, params);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        MapMarker mapMarker = markers.get(marker);
        Event event = mapMarker.events.get(0);

        mainInterface.showEventDetails(event);
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

    /**
     * Set map mode.
     *
     * @param mode Map mode to determine how content is displayed.
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Refresh the map with markers around current centered position.
     */
    public void refresh() {
        refresh(map.getCameraPosition().target);
    }

    /**
     * Refresh the map with markers around the specified position.
     *
     * @param position Position in latitude and longitude.
     */
    public void refresh(LatLng position) {
        double[] bounds = null;
        int limit;

        if (mode == MODE_TIMELINE) {
            clear();
            limit = TIMELINE_LIMIT;
        } else {
            LatLngBounds latLngBounds = map.getProjection().getVisibleRegion().latLngBounds;

            LatLng lowerLeft = latLngBounds.southwest;
            LatLng upperRight = latLngBounds.northeast;

            bounds = new double[]{
                lowerLeft.latitude,
                lowerLeft.longitude,
                upperRight.latitude,
                upperRight.longitude
            };

            limit = LIMIT;
        };

        if (task != null) {
            task.cancel(true);
        }

        task = new AsyncTask<Object, Void, Map<String, List<Event>>>() {
            @Override
            protected Map<String, List<Event>> doInBackground(Object... params) {
                LatLng position = (LatLng) params[0];
                double[] bounds = (double[]) params[1];
                int limit = (int) params[2];

                double[] latLng = {
                    position.latitude,
                    position.longitude
                };

                Map<String, List<Event>> result = new LinkedHashMap<>();
                // Retrieve Event IDs
                EventDataSource dataSource =
                    DatabaseHelper.getDataSource(getContext(), EventDataSource.class);
                List<String> eventIds =
                    dataSource.getEventIds(startTime, endTime, latLng, bounds, 0, limit);

                if (!eventIds.isEmpty()) {
                    eventIds.removeAll(markers.getIds());
                    // Group Events by Days
                    for (int i = 0; i < eventIds.size(); i++) {
                        String id = eventIds.get(i);
                        Event event = eventManager.getEvent(id, false);
                        // Only Locations with Latitude and Longitude
                        if (event.location.containsLatLng()) {
                            LocalDate date = new LocalDate(event.startTime);
                            String key = date.getYear() + "/" + date.getMonthOfYear() + "/" +
                                date.getDayOfMonth();

                            List<Event> events = result.get(key);
                            if (events == null) {
                                result.put(key, events = new ArrayList<>());
                            }

                            events.add(event);
                        }
                    }
                }

                return result;
            }

            @Override
            protected void onPostExecute(Map<String, List<Event>> result) {
                if (result.isEmpty()) {
                    task = null;
                    return;
                }

                int tempColor = Colors.getColor(getContext(), MARKER_COLOR_ID);

                List<List<Event>> values = new ArrayList<>(result.values());

                Event firstDayEvent = values.get(0).get(0);
                Event lastDayEvent = values.get(values.size() - 1).get(0);
                LocalDate start = new LocalDate(lastDayEvent.startTime);
                LocalDate end = new LocalDate(firstDayEvent.startTime);

                int days = Days.daysBetween(start, end).getDays();

                float base = BASE_ALPHA;

                for (int i = values.size() - 1; i >= 0; i--) {
                    List<Event> events = values.get(i);

                    Event tempEvent = events.get(0);
                    LocalDate date = new LocalDate(tempEvent.startTime);

                    float delta = Days.daysBetween(start, date).getDays();
                    float alpha = days == 0 ? 1 : base + (1 - base) * delta / days;

                    // Color for Marker and Path
                    final int color = Colors.setAlpha(tempColor, alpha);

                    List<LatLng> positions = new ArrayList<>();

                    for (Event event : events) {
                        LatLng position = new LatLng(
                            event.location.getLatitude(),
                            event.location.getLongitude()
                        );
                        // Draw Marker for Event
                        MapMarker marker = new MapMarker(event.id, event, position);

                        if (mode == MODE_TIMELINE) {
                            marker.drawPointMarker(getContext(), map, color, true);
                        } else {
                            int iconResId = R.drawable.ic_place;
                            marker.drawPlaceMarker(getContext(), map, iconResId, color, true);
                        }

                        markers.put(event.id, marker);
                        // Keep Unique Consecutive Positions
                        if (positions.isEmpty() || !position.equals(positions.get(positions.size() - 1))) {
                            positions.add(position);
                        }
                    }
                    // Center Camera w/ All Locations
                    centerLocations(positions, true);
                    // Retrieve Path to Draw
                    int year = date.getYear();
                    int month = date.getMonthOfYear() - 1;
                    int day = date.getDayOfMonth();

                    new TimelineTask(new TimelineTask.TimelineListener() {
                        @Override
                        public void onFinish(List<LatLng> result) {
                            if (result.isEmpty()) {
                                return;
                            }

                            PolylineOptions options = new PolylineOptions();
                            options.addAll(result);
                            options.color(color);
                            options.width(LINE_WIDTH);

                            map.addPolyline(options);
                        }
                    }).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, year, month, day);
                }

                task = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, position, bounds, limit);
    }

    /**
     * Change the current map type.
     *
     * @param type Map type used to render.
     */
    public void setMapType(int type) {
        map.setMapType(mapType = type);
        Settings.getInstance(getContext()).setMapType(type);
    }

    /**
     * Center camera at given position.
     *
     * @param latLng Position to center at.
     * @param zoom Camera zoom level after centering.
     * @param animate Enable interpolation for camera movement.
     */
    public void centerLocation(LatLng latLng, float zoom, boolean animate) {
        CameraUpdate update;

        if (zoom > 0) {
            update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        } else {
            update = CameraUpdateFactory.newLatLng(latLng);
        }

        moveCamera(update, animate);
    }

    /**
     * Center camera to fit all given positions.
     *
     * @param positions Positions used to create a bounding box.
     * @param animate Enable interpolation for camera movement.
     */
    public void centerLocations(List<LatLng> positions, boolean animate) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : positions) {
            builder.include(latLng);
        }

        LatLngBounds bounds = builder.build();
        int padding = Pixels.pxFromDp(getContext(), BOUNDS_PADDING_DP);

        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        moveCamera(update, animate);
    }

    /**
     * Adjust camera position.
     *
     * @param cameraUpdate Values to adjust camera.
     * @param animate Enable interpolation for camera movement.
     */
    public void moveCamera(CameraUpdate cameraUpdate, boolean animate) {
        if (animate) {
            map.animateCamera(cameraUpdate);
        } else {
            map.moveCamera(cameraUpdate);
        }
    }

    /**
     * Center camera at current position.
     *
     * @param animate Enable interpolation for camera movement.
     */
    public void centerLastKnownLocation(final boolean animate) {
        LocationHelper.getLastKnownLatLng(getActivity(), new LocationCallback() {
            @Override
            public void onFinish(Location location) {
                if (location != null) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    centerLocation(latLng, DEFAULT_ZOOM_LEVEL, animate);
                }
            }
        });
    }

    /**
     * Change camera zoom level.
     *
     * @param zoom Camera zoom level.
     * @param animate Enable interpolation for camera movement.
     */
    public void setZoomLevel(float zoom, boolean animate) {
        CameraUpdate update = CameraUpdateFactory.zoomTo(zoom);
        moveCamera(update, animate);
    }

    /**
     * Remove all existing markers from the map.
     */
    public void clear() {
        map.clear();
        markers.clear();

        currentMarker = null;
    }

    @Override
    public void onTimeSelected(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        refresh();
    }
}
