package net.fabiszewski.ulogger;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.preference.Preference;
import android.content.SharedPreferences;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import static net.fabiszewski.ulogger.SettingsActivity.KEY_DEVICENAME;

public class CGPSCoordinatesGPXHandler
{
    private static final String TAG = GpxExportService.class.getSimpleName();
    // namespace for use with uLogger-server(from ulogger)
	public static String ns = "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd https://github.com/stevegdavis/ulogger-android/1 https://raw.githubusercontent.com/stevegdavis/uLogger-server/master/scripts/gpx_extensions1.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ulogger=\"https://github.com/stevegdavis/uLogger-android/1\" creator=\"uLogger-server 1.0-beta\" version=\"1.0\">";
    private ProgressDialog progress = null;

    private final String loggedAtStr = "Logged at: ";
    private final String commentStr = "Comment: ";

    public void GPSCoordinatesGPXStreamWriter(OutputStream outputStream, Cursor cursor, Cursor startCursor, Context context)
    {
        try
        {
            outputStream.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n").getBytes());
            outputStream.write((ns + "\n").getBytes());
            double totalDistance = 0.00;

            outputStream.write(("<metadata><device>" + getDeviceName(context) + "</device></metadata>").getBytes());
            outputStream.write(("\n").getBytes());
            //
            Calendar calM = CDateTime.getLastSundayInMonth(Calendar.MARCH);
            Calendar calO = CDateTime.getLastSundayInMonth(Calendar.OCTOBER);
            // Get last Sunday of March as a Calendar
            // Get the last Sunday in October as a Calendar
            int pointNumber = 0;
            int Idx = 0;
            startCursor.moveToFirst();
            while (cursor.moveToNext()) {
                String timeTakenString = calculateTimeTaken(DbAccess.getDate(startCursor), DbAccess.getDate(cursor), adjustDaylightSaving(DbAccess.getDate(cursor), DbAccess.getDate(startCursor), calM, calO)); // 0 = no add or subtract, 1 = add, -1 = subtract 1 hour
                outputStream.write(("<wpt lat=" + "\"" + DbAccess.getLatitude(cursor) + "\"" + " lon=" + "\"" + DbAccess.getLongitude(cursor) + "\"" + ">").getBytes());
                // <ele>88.4000015258789</ele>
                outputStream.write(("<ele>" + String.valueOf(DbAccess.getAltitude(cursor)) + "</ele>").getBytes());
                outputStream.write(("<name>" + "µLogger" + "</name>").getBytes());
                String latLonS = DbAccess.getLatitude(cursor) + "," + DbAccess.getLongitude(cursor);
                String hyperLink = "&lt;a href=\"https://www.google.com/maps?q=" + latLonS + "&amp;layer=c&amp;cbll=" + latLonS + "&amp;cbp=11\" target=\"_blank | sametab\"&gt;&lt;font color=\"red\"&gt;Street View&lt;/font&gt;";
                outputStream.write(("<desc>" + "Device: " + getDeviceName(context) + "&lt;br/&gt;" + "Point: " + String.valueOf(++pointNumber) + " of " + String.valueOf(cursor.getCount()) + "&lt;br/&gt;" + loggedAtStr + DbAccess.getDateString(cursor) + "&lt;br/&gt;" + "Latitude,Longitude: " + DbAccess.getLatitude(cursor) + "," + DbAccess.getLongitude(cursor) + "&lt;br/&gt;" + "Accuracy within: " + DbAccess.getAccuracy(cursor) + "M" + "&lt;br/&gt;" + "Altitude: " + DbAccess.getAltitude(cursor) + "M" + "&lt;br/&gt;" + "Journey time to here: " + timeTakenString + "&lt;br/&gt;" + ((DbAccess.getComment(cursor) == null || DbAccess.getComment(cursor).length() == 0) ? "" : commentStr + DbAccess.getComment(cursor)) +hyperLink + "</desc>").getBytes());
                outputStream.write(("</wpt>\n").getBytes());
            }

            outputStream.write(("<trk>\n").getBytes());
            outputStream.write(("<trkseg>\n").getBytes());
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
				String dt = String.format("yyyy-MM-dd HH:mm:ss.SSS", DbAccess.getTime(cursor));
                dt = (dt.replace(" ", "T")) + "Z";
				outputStream.write(("<trkpt lat=" + "\"" + DbAccess.getLatitude(cursor) + "\"" + " lon=" + "\"" + DbAccess.getLongitude(cursor) + "\"" + "><ele>" + DbAccess.getAltitude(startCursor) + "</ele><time>" + dt + "</time><src>gps</src>" + "<extensions>" + "<ulogger:accuracy>" + DbAccess.getAccuracy(startCursor) + "</ulogger:accuracy>" + "<ulogger:speed>" + DbAccess.getSpeed(startCursor) + "</ulogger:speed>" + "<ulogger:bearing>" + DbAccess.getBearing(startCursor) + "</ulogger:bearing>" + "<ulogger:device_name>" + getDeviceName(context) + "</ulogger:device_name>" + "</extensions>" + "</trkpt>\n").getBytes());
			}

            outputStream.write(("</trkseg>\n").getBytes());
            outputStream.write(("</trk>\n").getBytes());
            outputStream.write(("</gpx>\n").getBytes());
            outputStream.flush();
            outputStream.close();
        }
        catch (Exception e)
        {
            if (Logger.DEBUG) { Log.d(TAG, "[export gpx write exception: " + e + "]"); };
			//CUtils.getLoggerInstance().info("Exception: " + e.getMessage());
        }
    }

    private String getDeviceName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(KEY_DEVICENAME, "");
    }

    private String capitalise(String s)
    {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String calculateTimeTaken(Date d1, Date d2, int addOrSubtractHour) {
        //HH converts hour in 24 hours format (0-23), day calculation

        //Date d1 = new Date(date1);
        //Date d2 = new Date(date2);

        long diff;
        long diffSeconds = 0;
        long diffMinutes = 0;
        long diffHours = 0;
        long diffDays = 0;
        try {
            // TEST ONLY
            //String dateStart = "31/12/2013 11:01";
            //String dateStop = "01/08/2014 11:10";
            //d1 = format.parse(dateStart);
            //d2 = format.parse(dateStop);
            //
            // May need to calculate daylight saving time add or subtract 1 hour GMT/BST
            //d1 = format.parse(firstData.getTimeStamp());
            //d2 = format.parse(lastData.getTimeStamp());

            //in milliseconds
            diff = d2.getTime() - d1.getTime();
            diffSeconds = diff / 1000;
            diffMinutes = diffSeconds / 60;// % 60;
            diffSeconds = diffSeconds % 60;

            diffMinutes = diff / (60 * 1000) % 60;
            diffHours = diff / (60 * 60 * 1000) % 24;
            diffDays = diff / (24 * 60 * 60 * 1000);

            switch (addOrSubtractHour) {
                case 1:
                    diffHours++;
                    if (diffHours > 23) {
                        diffDays++;
                        diffHours = 0;
                    }
                    break;
                case -1:
                    if (diffHours > 0)
                        diffHours--;
                    else {
                        diffHours = 23;
                        diffDays--;
                    }
                    break;
            }
        }
        catch (Exception e) {
            if (Logger.DEBUG) { Log.d(TAG, "[export gpx write exception: " + e + "]"); };
        }
        return String.valueOf((int) diffDays) + " Days, " + String.valueOf((int) Math.rint(diffHours)) + " Hours, " + String.valueOf(diffMinutes) + " Mins, " + String.valueOf(diffSeconds) + " Secs";
    }

    private int adjustDaylightSaving(Date firstDT, Date lastDT, Calendar calLastSunInMarch, Calendar calLastSunInOctober) {

        //Date firstDT = new Date(firstTime);
        //Date lastDT = new Date(lastTime);
        //SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        Calendar cal1 = Calendar.getInstance(Locale.UK);
        Calendar cal2 = Calendar.getInstance(Locale.UK);
        cal1.setTime(firstDT);
        cal2.setTime(lastDT);
        int result = 0;
        if (cal1.before(calLastSunInMarch) && (cal2.after(calLastSunInMarch) && (cal2.before(calLastSunInOctober))))
            result = -1;  // GMT to BST
        else if (cal1.before(calLastSunInMarch) && cal2.after(calLastSunInOctober))
            result = 0;   // GMT to GMT
        else if ((cal1.after(calLastSunInMarch) && cal1.before(calLastSunInOctober)) && cal2.after(calLastSunInOctober))
            result = 1;   // BST to GMT
        return result;
    }
}

