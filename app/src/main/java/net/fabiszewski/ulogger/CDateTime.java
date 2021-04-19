package net.fabiszewski.ulogger;


import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CDateTime
{
	public static Calendar getLastSundayInMonth(int Month) // Usually March BST or October GMT
	{
		Calendar calDayLightSavingMarchDate = Calendar.getInstance(Locale.UK);
		
		Calendar cal1 = Calendar.getInstance(Locale.UK);
		Calendar cal2 = Calendar.getInstance(Locale.UK);
		
        // Get last Sunday of March as a date
        calDayLightSavingMarchDate.set(Calendar.getInstance().get(Calendar.YEAR), Month, 1);// Calendar.MARCH, 1);
        int startIdx = 1;
        for(int i = 0; i < 31; i++)
        {
        	// Get the 1st Sunday
        	if(calDayLightSavingMarchDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
	        	break;
        	calDayLightSavingMarchDate.add(Calendar.DAY_OF_WEEK, 1);
        	startIdx++;
        }
        int lastSundayInMarch = startIdx;
        for(int i = startIdx; i < 31; i++)
        {
        	calDayLightSavingMarchDate.add(Calendar.DAY_OF_WEEK, 1);
        	if(calDayLightSavingMarchDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
        		lastSundayInMarch +=7;
        }
        calDayLightSavingMarchDate.set(Calendar.DAY_OF_MONTH, lastSundayInMarch);
        
        return calDayLightSavingMarchDate;
	}
	
	public static String getDateTimeString(long time, String formatStr)
    {
        // Return String eg. dd/MM/yyyy HH:mm
        String RC = "";
        try
        {
            final SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
            Date date = new Date(time);
            RC = sdf.format(date);
        }
        catch (Exception ex)
        {
            RC = "";
        }
        return RC;
    }
	
	public static long getLapsedSeconds(long dt1, long dt2)
	{
		long diff = dt1 - dt2;
                long diffSeconds = diff / 1000 % 60;
                long diffMinutes = diff / (60 * 1000) % 60;
                long diffHours = diff / (60 * 60 * 1000) % 24;
                long diffDays = diff / (24 * 60 * 60 * 1000);
                long elapsedSeconds = (diffDays * (1440 * 60)) + (diffHours * (60 * 60)) + ((diffMinutes * 60)) + diffSeconds;
		return elapsedSeconds;
	}

    public static String calculateTimeTaken(Date d1, Date d2, int addOrSubtractHour) {
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
            if (Logger.DEBUG) { Log.d(CGPSCoordinatesGPXHandler.TAG, "[export gpx write exception: " + e + "]"); };
        }
        return String.valueOf((int) diffDays) + " Days, " + String.valueOf((int) Math.rint(diffHours)) + " Hours, " + String.valueOf(diffMinutes) + " Mins, " + String.valueOf(diffSeconds) + " Secs";
    }

    public static int adjustDaylightSaving(Date firstDT, Date lastDT, Calendar calLastSunInMarch, Calendar calLastSunInOctober) {

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

