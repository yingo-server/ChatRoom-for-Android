package com.motan.chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {
    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String generateId() {
        return System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
}
