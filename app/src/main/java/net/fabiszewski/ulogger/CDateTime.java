package net.fabiszewski.ulogger;


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
	
}

