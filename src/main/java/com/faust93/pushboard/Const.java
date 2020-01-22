package com.faust93.pushboard;

/**
 * Created by faust on 17.04.14.
 */
public interface Const {
    public static final String PREF_FILE = "pushboard_prefs";
    public static final String DEVICE_ID_FORMAT = "andr_%s";
    public static final String NOTIFY_ACTION = "com.faust93.pushboard.NOTIFY";
    public static final String NOTIFY_PERMISSION ="com.faust93.pushboard.permission.NP";
    public static int MSG_TYPE_OK = 0;
    public static int MSG_TYPE_WARN = 1;
    public static int MSG_TYPE_CRITICAL = 2;
}
