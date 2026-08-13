/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.yanus171.feedexfork.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class StringUtils {

    private static final DateFormat TIME_FORMAT = android.text.format.DateFormat.getTimeFormat(MainApplication.getContext());
    private static final int SIX_HOURS = 21600000; // six hours in milliseconds
    public static DateFormat DATE_SHORT_FORMAT;
    public static DateFormat DATE_FORMAT;

    static {
        // getBestTimePattern() is only available in API 18 and up (Android 4.3 and better)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            DATE_SHORT_FORMAT = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(MainApplication.getContext().getResources().getConfiguration().locale, "d MMM"));
            DATE_FORMAT = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(MainApplication.getContext().getResources().getConfiguration().locale, "d MMMM"));
        } else {
            DATE_SHORT_FORMAT = android.text.format.DateFormat.getDateFormat(MainApplication.getContext());
        }
    }

    // -------------------------------------------------------------------------
    public static String MilliSecIntervalToString(final long ms, boolean shortCaption, boolean trimMinutesInBigHours) {
        String result = "";
        final Context context = MainApplication.getContext();

        long absMs = Math.abs(ms);

        final long MINUTE_DURATION = 60 * 1000;
        final long HOUR_DURATION = 60 * MINUTE_DURATION;
        final long DAY_DURATION = 24 * HOUR_DURATION;
        final long MONTH_DURATION = 30 * DAY_DURATION;
        final long YEAR_DURATION = 12 * MONTH_DURATION;
        final int MAX_LENGTH = 10;

        int years = (int) ((long) absMs / YEAR_DURATION);
        absMs = absMs % YEAR_DURATION;

        int months = (int) ((long) absMs / MONTH_DURATION);
        absMs = absMs % MONTH_DURATION;

        int days = (int) ((long) absMs / DAY_DURATION);
        absMs = absMs % DAY_DURATION;

        int hours = (int) ((long) absMs / HOUR_DURATION);
        absMs = absMs % HOUR_DURATION;

        int minutes = (int) ((long) absMs / MINUTE_DURATION);

        if (years > 0)
            result = result.trim() + String.format( " %d%s", years, context.getString(R.string.yearsShort) ) ;

        if (months > 0 && years < 3)
            result = result.trim() + String.format( " %d%s", months, context.getString(R.string.monthsShort) ) ;

        if ( result.length() < MAX_LENGTH && days > 0 && months < 3)
            result = result.trim() + " " + DaysToString(days, shortCaption);

        if ( result.length() < MAX_LENGTH && hours > 0 && (days < 2 || !trimMinutesInBigHours) )
            result = result.trim() + " " + HoursToString(hours, shortCaption);

        if ( result.length() < MAX_LENGTH && minutes > 0 && (hours < 2 || !trimMinutesInBigHours) && days == 0)
            result = result.trim() + " " + MinutesToString(minutes, shortCaption);

        return result.trim();
    }
    public static String DaysToString(int days, boolean shortCaption) {
        final Context context = MainApplication.getContext();
        String result;
        if (shortCaption) {
            result = context.getString(R.string.daysShort);
        } else if (Locale.getDefault().getLanguage().equals("en")) {
            result = context.getString(R.string.oneDay);
            if (days > 1) {
                result = context.getString(R.string.fiveDay);
            }
        } else {
            int modDays = days % 10;
            if ((modDays == 0) || ((days >= 5) && (days <= 20))) {
                result = context.getString(R.string.fiveDay);
            } else if (modDays == 1) {
                result = context.getString(R.string.oneDay);
            } else if ((modDays > 1) && (modDays <= 4)) {
                result = context.getString(R.string.twoFourDay);
            } else {
                result = context.getString(R.string.fiveDay);
            }
        }
        result = String.format("%d %s", days, result);
        return result;
    }
    // ---------------------------------------------------------------------------------
    private static String HoursToString(int hours, boolean shortCaption) {
        final Context context = MainApplication.getContext();

        String result;
        if (shortCaption) {
            result = Integer.toString(hours) + context.getString(R.string.hoursShort);
        } else {
            if (Locale.getDefault().getLanguage().equals("en")) {
                result = context.getString(R.string.oneHour);
                if (hours > 1) {
                    result = context.getString(R.string.fiveHour);
                }
            } else {
                int mod = hours % 10;
                if ((mod == 0) || ((hours >= 5) && (hours <= 20))) {
                    result = context.getString(R.string.fiveHour);
                } else if (mod == 1) {
                    result = context.getString(R.string.oneHour);
                } else if ((mod > 1) && (mod <= 4)) {
                    result = context.getString(R.string.twoFourHour);
                } else {
                    result = context.getString(R.string.fiveHour);
                }
            }
            result = String.format("%d %s", hours, result);
        }
        return result;
    }
    // ---------------------------------------------------------------------------------
    private static String MinutesToString(int minutes, boolean shortCaption) {
        final Context context = MainApplication.getContext();
        String result;
        if (shortCaption) {
            result = Integer.toString(minutes) + context.getString(R.string.minutesShort);
        } else {
            if (Locale.getDefault().getLanguage().equals("en")) {
                result = context.getString(R.string.oneMinute);
                if (minutes > 1) {
                    result = context.getString(R.string.fiveMinute);
                }
            } else {
                int mod = minutes % 10;
                if ((mod == 0) || ((minutes >= 5) && (minutes <= 20))) {
                    result = context.getString(R.string.fiveMinute);
                } else if (mod == 1) {
                    result = context.getString(R.string.oneMinute);
                } else if ((mod > 1) && (mod <= 4)) {
                    result = context.getString(R.string.twoFourMinute);
                } else {
                    result = context.getString(R.string.fiveMinute);
                }
            }

            result = String.format("%d %s", minutes, result);
        }
        return result;
    }

    private static String IntervalFromNowToStringInner(long ms1, boolean shortCaption, boolean trimMinutesInBigHours) {
        final Context context = MainApplication.getContext();
        if (shortCaption) {
            ms1 = Math.abs(ms1);
        }
        String result = MilliSecIntervalToString(ms1, shortCaption, trimMinutesInBigHours);
        if (result.length() == 0) {
            if (shortCaption) {
                result = MinutesToString(0, shortCaption);
            } else {
                result = context.getString(R.string.rightNow);
            }
        } else if (!shortCaption) {
            if (ms1 >= 1000 * 60) {
                result = context.getString(R.string.timeIntervalInFuture) + " " + result.trim();
            } else if (ms1 <= -1000 * 60) {
                result = result.trim() + " " + context.getString(R.string.timeIntervalInPast);
            }
        }
        return result;
    }

    static final String DEFAULT_DATETIME_FORMAT = "system";
    static public String getDateTimeString(long timestamp) {
        if ( PrefUtils.getString( "datetime_format", DEFAULT_DATETIME_FORMAT ).equals( "remaining" ) ) {
            return IntervalFromNowToStringInner(  timestamp - System.currentTimeMillis(), true, true );
        } else {
            String outString;

            Date date = new Date(timestamp);
            Calendar calTimestamp = Calendar.getInstance();
            calTimestamp.setTimeInMillis(timestamp);
            Calendar calCurrent = Calendar.getInstance();

            if (Math.abs(calCurrent.getTimeInMillis() - timestamp) < SIX_HOURS ||
                calCurrent.get(Calendar.DAY_OF_MONTH) == calTimestamp.get(Calendar.DAY_OF_MONTH)) {
                outString = TIME_FORMAT.format(date);
            } else {
                outString = DATE_SHORT_FORMAT.format(date) + ' ' + TIME_FORMAT.format(date);
            }

            if (calCurrent.get(Calendar.YEAR) != calTimestamp.get(Calendar.YEAR))
                outString = String.valueOf(calTimestamp.get(Calendar.YEAR)) + " " + outString;
            return outString;
        }
    }

    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return number.toString(16);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
