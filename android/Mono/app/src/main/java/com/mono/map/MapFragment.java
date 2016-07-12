package com.mono.map;

import android.app.DatePickerDialog;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.EventDataSource;
import com.mono.details.EventDetailsActivity.DateTimePickerCallback;
import com.mono.map.DirectionsTask.DirectionListener;
import com.mono.map.MenuPagerAdapter.MenuPagerListener;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.settings.Settings;
import com.mono.util.Colors;
import com.mono.util.Constants;
import com.mono.util.LocationHelper;
import com.mono.util.LocationHelper.LocationCallback;
import com.mono.util.Pixels;
import com.mono.util.SimpleTabLayout.TabPagerCallback;
import com.mono.util.SimpleViewPager;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A fragment that displays a map showing location events between a specified time period. Events
 * selected can be viewed for more details or edited as well.
 *
 * @author Gary Ng
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, OnCameraChangeListener,
        OnMapClickListener, OnMarkerClickListener, InfoWindowAdapter, OnInfoWindowClickListener,
        EventBroadcastListener, TabPagerCallback, OnItemSelectedListener {

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATE_TIME_FORMAT;

    private static final float DEFAULT_ZOOM_LEVEL = 12.5f;
    private static final float DEFAULT_TAP_ZOOM_LEVEL = 14f;
    private static final int CIRCLE_STROKE_WIDTH = 4;

    private static final int MODE_NONE = 0;
    private static final int MODE_TIMELINE = 1;

    private static final CharSequence[] MENU_ITEMS = {"Day", "Week", "Custom"};
    private static final int MENU_DAY = 0;
    private static final int MENU_WEEK = 1;
    private static final int MENU_CUSTOM = 2;

    private static final float MENU_TEXT_SIZE_SP = 16;
    private static final int OPTION_DIMENSION_DP = 24;
    private static final int OPTION_MARGIN_DP = 2;

    private static final int MARKER_COLOR_ID = R.color.colorPrimary;
    private static final int LINE_WIDTH = 5;

    private static final int LIMIT = 30;

    private MainInterface mainInterface;
    private EventManager eventManager;

    private Spinner menu;
    private ViewGroup menuContainer;
    private GoogleMap map;

    private MapMarkerMap markers = new MapMarkerMap();

    private int mode;
    private MapMarker currentMarker;
    private int mapType;

    private AsyncTask<Object, Void, Map<String, List<Event>>> task;

    private long startTime;
    private long endTime;

    static {
        DATE_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
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

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(getContext(),
            R.layout.simple_spinner_item, MENU_ITEMS);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        menu = (Spinner) view.findViewById(R.id.menu);
        menu.setAdapter(adapter);
        menu.setOnItemSelectedListener(this);

        menuContainer = (ViewGroup) view.findViewById(R.id.menu_container);

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
        map.setOnCameraChangeListener(this);
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
    public void onCameraChange(CameraPosition cameraPosition) {
        if (mode == MODE_NONE) {
            LatLng target = cameraPosition.target;
            refresh(target);
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
     * @param mode The value of the mode.
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Refresh the map with markers around current centered position.
     */
    public void refresh() {
        if (mode == MODE_TIMELINE) {
            clear();
        }

        refresh(map.getCameraPosition().target);
    }

    /**
     * Refresh the map with markers around the specified position.
     *
     * @param position The position in latitude and longitude.
     */
    public void refresh(LatLng position) {
        if (mode == MODE_TIMELINE) {
            clear();
        }

        LatLngBounds latLngBounds = map.getProjection().getVisibleRegion().latLngBounds;

        LatLng lowerLeft = latLngBounds.southwest;
        LatLng upperRight = latLngBounds.northeast;

        double[] latLng = {
            position.latitude,
            position.longitude
        };

        double[] bounds = {
            lowerLeft.latitude,
            lowerLeft.longitude,
            upperRight.latitude,
            upperRight.longitude
        };

        int limit = LIMIT;

        if (mode == MODE_TIMELINE) {
            bounds = null;
            limit = 100;
        }

        if (task != null) {
            task.cancel(true);
        }

        task = new AsyncTask<Object, Void, Map<String, List<Event>>>() {
            @Override
            protected Map<String, List<Event>> doInBackground(Object... params) {
                double[] latLng = (double[]) params[0];
                double[] bounds = (double[]) params[1];
                int limit = (int) params[2];

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
                int tempColor = Colors.getColor(getContext(), MARKER_COLOR_ID);

                int i = result.size() - 1;

                for (List<Event> events : result.values()) {
                    // Color for Marker and Path
                    final int color = Colors.getLighter(tempColor, Math.min(0.05f * i--, 0.5f));

                    List<LatLng> positions = new ArrayList<>();

                    for (Event event : events) {
                        LatLng position = new LatLng(
                            event.location.getLatitude(),
                            event.location.getLongitude()
                        );
                        // Draw Marker for Event
                        MapMarker marker = new MapMarker(event.id, event, position);

                        int iconResId = R.drawable.ic_place;

                        if (mode == MODE_TIMELINE) {
                            marker.drawPointMarker(getContext(), map, color, true);
                        } else {
                            marker.drawPlaceMarker(getContext(), map, iconResId, color, true);
                        }

                        markers.put(event.id, marker);
                        // Keep Unique Consecutive Positions
                        if (positions.isEmpty() || !position.equals(positions.get(positions.size() - 1))) {
                            positions.add(position);
                        }
                    }
                    // Retrieve Path to Draw
                    new DirectionsTask(positions, new DirectionListener() {
                        @Override
                        public void onFinish(List<LatLng> result) {
                            PolylineOptions options = new PolylineOptions();
                            options.addAll(result);
                            options.color(color);
                            options.width(LINE_WIDTH);

                            map.addPolyline(options);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                task = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, latLng, bounds, limit);
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
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
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

    public void clear() {
        map.clear();
        markers.clear();

        currentMarker = null;
    }

    @Override
    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        ((TextView) parent.getChildAt(0)).setTextSize(MENU_TEXT_SIZE_SP);

        switch (position) {
            case MENU_DAY:
                onMenuDay();
                break;
            case MENU_WEEK:
                onMenuWeek();
                break;
            case MENU_CUSTOM:
                onMenuCustom();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView parent) {

    }

    private void showMenuPager(int type, MenuPagerListener listener) {
        menuContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.map_menu_pager, null, false);

        int count = 100;
        SimpleViewPager viewPager = (SimpleViewPager) view.findViewById(R.id.container);
        viewPager.setAdapter(new MenuPagerAdapter(type, count, listener));

        viewPager.setCurrentItem(count - 1);
        viewPager.setOffscreenPageLimit(10);
        viewPager.setSwipeEnabled(true);
        viewPager.setPageMargin(Pixels.pxFromDp(getContext(), 1));
        viewPager.setPageMarginDrawable(android.R.color.white);

        menuContainer.addView(view);
    }

    private void onMenuDay() {
        showMenuPager(MenuPagerAdapter.TYPE_DAY, new MenuPagerListener() {
            @Override
            public void onDaySelected(int year, int month, int day) {
                DateTime dateTime = new DateTime(year, month, day, 0, 0);

                startTime = dateTime.millisOfDay().withMinimumValue().getMillis();
                endTime = dateTime.millisOfDay().withMaximumValue().getMillis();

                refresh();
            }
        });
    }

    private void onMenuWeek() {
        showMenuPager(MenuPagerAdapter.TYPE_WEEK, new MenuPagerListener() {
            @Override
            public void onWeekSelected(int year, int week) {
                DateTime dateTime = new DateTime().withYear(year).withWeekOfWeekyear(week);

                DateTime startDateTime = dateTime.dayOfWeek().withMinimumValue().minusDays(1);
                startTime = startDateTime.millisOfDay().withMinimumValue().getMillis();

                DateTime endDateTime = dateTime.dayOfWeek().withMaximumValue().minusDays(1);
                endTime = endDateTime.millisOfDay().withMaximumValue().getMillis();

                refresh();
            }
        });
    }

    private void onMenuCustom() {
        menuContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.map_menu_range, null, false);

        final TextView startDate = (TextView) view.findViewById(R.id.start_date);
        startDate.setText(DATE_FORMAT.format(startTime));
        startDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeZone = TimeZone.getDefault().getID();
                onDateClick(startTime, timeZone, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        startDate.setText(DATE_FORMAT.format(startTime = date.getTime()));
                        refresh();
                    }
                });
            }
        });

        final TextView endDate = (TextView) view.findViewById(R.id.end_date);
        endDate.setText(DATE_FORMAT.format(endTime));
        endDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeZone = TimeZone.getDefault().getID();
                onDateClick(endTime, timeZone, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        endDate.setText(DATE_FORMAT.format(endTime = date.getTime()));
                        refresh();
                    }
                });
            }
        });

        menuContainer.addView(view);
    }

    public void onDateClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final DateTime dateTime = new DateTime(milliseconds, DateTimeZone.forID(timeZone));

        DatePickerDialog dialog = new DatePickerDialog(
            getContext(),
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    DateTime newDateTime = dateTime.withDate(year, monthOfYear + 1, dayOfMonth);
                    callback.onSet(newDateTime.toDate());
                }
            },
            dateTime.getYear(),
            dateTime.getMonthOfYear() - 1,
            dateTime.getDayOfMonth()
        );

        dialog.show();
    }
}
