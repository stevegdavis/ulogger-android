package net.fabiszewski.ulogger;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.util.Xml;

import androidx.preference.PreferenceManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.fabiszewski.ulogger.SettingsActivity.KEY_DEVICENAME;


public class CGPSCoordinatesKMLHandler
{
    //public String KML_FILENAME = "";
    public static String ns = "http://www.opengis.net/kml/2.2";

    private final String loggedAtStr = "Logged at: ";
    private final String commentStr = "Comment: ";

    //private ProgressDialog progress = null;

    //public void setKMLFilename(String filename) {
        //KML_FILENAME = filename;
    //}

    //public String getKMLFilename() {
        //return KML_FILENAME;
    //}


//	<?xml version="1.0" encoding="UTF-8"?>
//	<kml xmlns="http://www.opengis.net/kml/2.2">
//	  <Placemark>
//	    <name>Simple placemark</name>
//	    <description>Attached to the ground. Intelligently places itself 
//	       at the height of the underlying terrain.</description>
//	    <Point>
//	      <coordinates>-122.0822035425683,37.42228990140251,0</coordinates>
//	    </Point>
//	  </Placemark>
//	</kml>

    public void GPSCoordinatesKMLStreamWriter(OutputStream outputStream, Cursor cursor, Cursor startCursor, Context context)
    {
        final String newLine = System.getProperty("line.separator");
        try
        {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            //StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(outputStream, "UTF-8");
            //start Document
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.setPrefix("", ns);
            xmlSerializer.startTag(ns, "kml");
            xmlSerializer.startTag("", "Document");        // <Document>
            ////xmlSerializer.text(newLine);
            xmlSerializer.startTag("", "name");             // Name usually for MAPS.ME import of logged route
            //xmlSerializer.text("µLogger " + filename);
            xmlSerializer.endTag("", "name");
            Calendar calM = CDateTime.getLastSundayInMonth(Calendar.MARCH);
            Calendar calO = CDateTime.getLastSundayInMonth(Calendar.OCTOBER);
            // Get last Sunday of March as a Calendar
            // Get the last Sunday in October as a Calendar
            int pointNumber = 0;
            // Add line colour
            xmlSerializer.startTag("", "Style");// id='lineStyleTripLogger'");
            xmlSerializer.attribute("", "id", "lineStyleTripLogger");
            xmlSerializer.startTag("", "LineStyle");
            xmlSerializer.startTag("", "color");
            xmlSerializer.text("64F00014");
            xmlSerializer.endTag("", "color");
            ////xmlSerializer.text(newLine);
            xmlSerializer.startTag("", "width");
            xmlSerializer.text("3");
            xmlSerializer.endTag("", "width");
            ////xmlSerializer.text(newLine);
            //<gx:labelVisibility>1</gx:labelVisibility>
            xmlSerializer.endTag("", "LineStyle");
            ////xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "Style");
            ////xmlSerializer.text(newLine);
            // Add Icon(s)
            xmlSerializer.startTag("", "Style");
            xmlSerializer.attribute("", "id", "iconTripLogger");
            xmlSerializer.startTag("", "IconStyle");
            xmlSerializer.startTag("", "hotSpot");
            xmlSerializer.attribute("", "x", "0.5");
            xmlSerializer.attribute("", "y", "0.0");
            xmlSerializer.attribute("", "xunits", "fraction");
            xmlSerializer.attribute("", "yunits", "fraction");
            xmlSerializer.endTag("", "hotSpot");
            xmlSerializer.startTag("", "Icon");
            xmlSerializer.startTag("", "href");
            xmlSerializer.text("http://maps.google.com/mapfiles/kml/paddle/blu-circle.png");
            xmlSerializer.endTag("", "href");
            ////xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "Icon");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "IconStyle");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "Style");
            xmlSerializer.text(newLine);
            xmlSerializer.startTag("", "Style");
            xmlSerializer.attribute("", "id", "iconStartTripLogger");
            xmlSerializer.startTag("", "IconStyle");
            xmlSerializer.startTag("", "hotSpot");
            xmlSerializer.attribute("", "x", "0.5");
            xmlSerializer.attribute("", "y", "0.0");
            xmlSerializer.attribute("", "xunits", "fraction");
            xmlSerializer.attribute("", "yunits", "fraction");
            xmlSerializer.endTag("", "hotSpot");
            xmlSerializer.startTag("", "Icon");
            xmlSerializer.startTag("", "href");
            xmlSerializer.text("http://maps.google.com/mapfiles/kml/paddle/go.png");
            xmlSerializer.endTag("", "href");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "Icon");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "IconStyle");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "Style");
            xmlSerializer.text(newLine);
            xmlSerializer.startTag("", "Style");
            xmlSerializer.attribute("", "id", "iconStopTripLogger");
            xmlSerializer.startTag("", "IconStyle");
            xmlSerializer.startTag("", "hotSpot");
            xmlSerializer.attribute("", "x", "0.5");// y=\"0.0\" xunits=\"fraction\" yunits=\"fraction\"");
            xmlSerializer.attribute("", "y", "0.0");
            xmlSerializer.attribute("", "xunits", "fraction");
            xmlSerializer.attribute("", "yunits", "fraction");
            xmlSerializer.endTag("", "hotSpot");

            xmlSerializer.startTag("", "Icon");
            xmlSerializer.startTag("", "href");
            xmlSerializer.text("http://maps.google.com/mapfiles/kml/paddle/red-circle.png");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "href");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "Icon");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "IconStyle");
            xmlSerializer.text(newLine);
            xmlSerializer.endTag("", "Style");
            xmlSerializer.text(newLine);
            int Idx = 0;
            startCursor.moveToFirst();
            while (cursor.moveToNext()) {
                //Placemark tag
                xmlSerializer.startTag("", "Placemark");        // <Placemark>
                if(Idx == 0)
                {
                    xmlSerializer.startTag("", "styleUrl");
                    xmlSerializer.text("#iconStartTripLogger");
                    xmlSerializer.endTag("", "styleUrl");
                    xmlSerializer.text(newLine);
                }
                else if(Idx >= cursor.getCount() - 1)
                {
                    xmlSerializer.startTag("", "styleUrl");
                    xmlSerializer.text("#iconStopTripLogger");
                    //xmlSerializer.text(newLine);
                    xmlSerializer.endTag("", "styleUrl");
                    xmlSerializer.text(newLine);
                }
                else
                {
                    xmlSerializer.startTag("", "styleUrl");
                    xmlSerializer.text("#iconTripLogger");
                    xmlSerializer.endTag("", "styleUrl");
                    xmlSerializer.text(newLine);
                }
                Idx++;
                xmlSerializer.startTag("", "name");                // <name>
                xmlSerializer.text("µLogger");
                xmlSerializer.endTag("", "name");                // </name>
                //
                String timeTakenString = CDateTime.calculateTimeTaken(DbAccess.getDate(startCursor), DbAccess.getDate(cursor), CDateTime.adjustDaylightSaving(DbAccess.getDate(cursor), DbAccess.getDate(startCursor), calM, calO)); // 0 = no add or subtract, 1 = add, -1 = subtract 1 hour
                // TEST ONLY TODO
                //<Placemark><name>kmlpoint</name><Point><coordinates>-2.253992166370153,51.7475611763075,0.0</coordinates></Point></Placemark>target=_blank | sametab
                //xmlSerializer.text("Point #" + String.valueOf(++pointNumber) + "<br/>" + loggedAtStr + CDateTime.getDateTimeString(data.getLocation().getTime(), "dd/MM/yyyy HH:mm:ss") + " " + "<br/>" + "Altitude: " + String.format("%.1f", data.getLocation().getAltitude()) + "M<br/>" + "Journey time to here: " + timeTakenString + "<br/>" + ((data.getComment().length() == 0) ? "" : commentStr + data.getComment()));
                xmlSerializer.startTag("", "description");        // <description>

                String latLonS = DbAccess.getLatitude(cursor) + "," + DbAccess.getLongitude(cursor);
                //target="_blank | sametab"
                String hyperLink = "&lt;a href=\"https://www.google.com/maps?q=" + latLonS + "&amp;layer=c&amp;cbll=" + latLonS + "&amp;cbp=11\" target=\"_blank | sametab\"&gt;&lt;font color=\"red\"&gt;Street View&lt;/font&gt;";
                xmlSerializer.text("Device: " + getDeviceName(context) + "<br/>" + "Point: " + String.valueOf(++pointNumber) + " of " + String.valueOf(cursor.getCount()) + "<br/>" + loggedAtStr + DbAccess.getDateString(cursor) + " " + "<br/>" + "Latitude,Longitude: " + latLonS + "<br/>" + "Accuracy within: " + DbAccess.getAccuracy(cursor) + "M<br/>" + "Altitude: " + DbAccess.getAltitude(cursor) + "M<br/>" + "Journey time to here: " + timeTakenString + "<br/>" + ((DbAccess.getComment(cursor) == null || DbAccess.getComment(cursor).length() == 0) ? "" : commentStr + DbAccess.getComment(cursor) + "<br/>") + hyperLink);
                xmlSerializer.endTag("", "description");
                xmlSerializer.text(newLine);

                xmlSerializer.startTag("", "Point");            // <Point>
                xmlSerializer.startTag("", "coordinates");        // <coordinates>
                xmlSerializer.text(DbAccess.getLongitude(cursor) + "," + DbAccess.getLatitude(cursor) + "," + DbAccess.getAltitude(cursor));
                xmlSerializer.endTag("", "coordinates");        // </coordinates>
                //xmlSerializer.text(newLine);
                //
                xmlSerializer.endTag("", "Point");
                //xmlSerializer.text(newLine);
                xmlSerializer.endTag("", "Placemark");          // </Placemark>
            }
            // Add lines if more than 1 point
            if(cursor.getCount() > 1)
            {
                cursor.moveToFirst();
                xmlSerializer.startTag("", "Placemark");        // <Placemark>

                //<styleUrl>#linestyleExample</styleUrl>
                xmlSerializer.startTag("", "styleUrl");
                xmlSerializer.text("#lineStyleTripLogger");
                xmlSerializer.endTag("", "styleUrl");
                //xmlSerializer.text(newLine);
                xmlSerializer.startTag("", "LineString");
                xmlSerializer.startTag("", "coordinates");      // <coordinates>
                cursor.moveToFirst();
                xmlSerializer.text(DbAccess.getLongitude(cursor) + "," + DbAccess.getLatitude(cursor) + "," + DbAccess.getAltitude(cursor) + " ");
                while (cursor.moveToNext()) {
                    xmlSerializer.text(DbAccess.getLongitude(cursor) + "," + DbAccess.getLatitude(cursor) + "," + DbAccess.getAltitude(cursor) + " ");
                }
                xmlSerializer.endTag("", "coordinates");        // </coordinates>
                //xmlSerializer.text(newLine);
                xmlSerializer.endTag("", "LineString");
                //xmlSerializer.text(newLine);
                xmlSerializer.endTag("", "Placemark");          // </Placemark>
                //xmlSerializer.text(newLine);
                // Finish add lines
            }

            xmlSerializer.endTag("", "Document");
            //xmlSerializer.text(newLine);
            xmlSerializer.endTag(ns, "kml");
            //xmlSerializer.text(newLine);
            xmlSerializer.endDocument();
        }
        catch (Exception ex)
        {
            ex = ex;
        }
        finally
        {
            try
            {
                outputStream.flush();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            try
            {
                outputStream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
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

    private String updatePointNumber(String description, int pointNumber)
    {
        String tmp = "";
        int Idx = description.indexOf("<br/>");
        if(Idx > -1)
        {
            String formatStr = "Point #";
            int Idx2 = description.indexOf(formatStr);
            if (Idx2 > -1)
            {
                int Idx3 = description.indexOf("<br/>", Idx2);

                if (Idx3 > -1)
                {
                    tmp = description.substring(Idx3);
                    tmp = description.substring(0, Idx) + "<br/>" + formatStr + String.valueOf(pointNumber) + tmp;
                }
            }
        }
        return  tmp;
    }
}

