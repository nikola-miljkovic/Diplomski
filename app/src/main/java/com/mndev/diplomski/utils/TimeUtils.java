package com.mndev.diplomski.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class TimeUtils {
    public static long[] getTimeVector(Date date, int interval, int iterations) {
        long[] times = new long[iterations];
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        for (int i = 0; i < iterations; i += 1) {
            times[i] = calendar.getTimeInMillis();
            calendar.add(Calendar.MILLISECOND, interval);
        }

        return times;
    }

    public static Date textToDateTime(CharSequence inputTime) {
        DateFormat formatter = new SimpleDateFormat("H:mm");
        try {
            Date date = formatter.parse(inputTime.toString());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.set(Calendar.MINUTE, date.getMinutes());
            calendar.set(Calendar.HOUR_OF_DAY, date.getHours());
            calendar.set(Calendar.SECOND, 0);
            return calendar.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
