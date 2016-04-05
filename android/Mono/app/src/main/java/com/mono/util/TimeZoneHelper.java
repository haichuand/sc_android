package com.mono.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class TimeZoneHelper {

    private TimeZoneHelper() {}

    public static String getTimeZoneGMT(TimeZone timeZone, long time) {
        long hours = timeZone.getRawOffset() / Constants.HOUR_MS;
        long minutes = Math.abs(timeZone.getRawOffset() / Constants.MINUTE_MS - hours * 60);

        boolean dst = timeZone.inDaylightTime(new Date(time));
        if (dst) {
            hours++;
        }

        return String.format("(GMT" + (hours >= 0 ? "+" : "-") + "%02d:%02d)", Math.abs(hours),
            minutes);
    }

    public static String getTimeZoneName(TimeZone timeZone, long time) {
        boolean dst = timeZone.inDaylightTime(new Date(time));
        return timeZone.getDisplayName(dst, TimeZone.LONG, Locale.getDefault());
    }

    public static String getTimeZoneGMTName(TimeZone timeZone, long time) {
        return getTimeZoneGMT(timeZone, time) + " " + getTimeZoneName(timeZone, time);
    }

    public static List<String> getTimeZones(long time) {
        String[] ids = TimeZone.getAvailableIDs();

        Map<String, String> minus = new HashMap<>();
        Map<String, String> plus = new HashMap<>();

        for (String id : ids) {
            TimeZone timeZone = TimeZone.getTimeZone(id);

            String gmt = getTimeZoneGMT(timeZone, time);
            String name = getTimeZoneName(timeZone, time);

            if (!name.contains("GMT")) {
                name = getTimeZoneGMTName(timeZone, time);

                if (gmt.contains("-") && !minus.containsKey(gmt)) {
                    minus.put(gmt, name);
                } else if (gmt.contains("+") && !plus.containsKey(gmt)) {
                    plus.put(gmt, name);
                }
            }
        }

        List<String> minusList = new ArrayList<>(minus.values());
        Collections.sort(minusList);
        Collections.reverse(minusList);

        List<String> plusList = new ArrayList<>(plus.values());
        Collections.sort(plusList);

        minusList.addAll(plusList);

        return minusList;
    }
}
