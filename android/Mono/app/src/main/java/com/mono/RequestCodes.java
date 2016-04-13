package com.mono;

public class RequestCodes {

    private RequestCodes() {}

    public static class Activity {

        public static final int SETTINGS = 1;
        public static final int EVENT_DETAILS = 2;
        public static final int WEB = 3;
        public static final int DUMMY_WEB = 4;
        public static final int ALARM_RECEIVER = 5;
        public static final int CHAT = 6;
        public static final int LOGIN = 7;
        public static final int LOGIN_CHAT = 8;
    }

    public static class Permission {

        public static final int PERMISSION_CHECK = 1;
    }
}
