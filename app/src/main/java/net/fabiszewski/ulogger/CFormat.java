package net.fabiszewski.ulogger;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;

/**
 * Created by steve on 18/09/2018.
 */

public class CFormat
{
	public static void removeAmpFromKMLFile(Uri uri, String filename, Context context)
	{
		try
		{
			File directory = context.getExternalFilesDir(null);
			if (directory.exists() == true) {
				File file = new File(directory, filename + ".kml");
				FileOutputStream fos = null;
				fos = new FileOutputStream(file);
				try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fos)) {
					InputStream instream = context.getContentResolver().openInputStream(uri);
					if (instream == null) {
						throw new IOException(context.getString(R.string.e_open_out_stream));
					}
					BufferedInputStream bufferedInputStream = new BufferedInputStream(instream);
					BufferedWriter buffWriter = new BufferedWriter(new OutputStreamWriter(bufferedOutputStream));
					BufferedReader buffReader = new BufferedReader(new InputStreamReader(bufferedInputStream));
					for(;;)
					{
						try
						{
							String line = buffReader.readLine();
							if(line == null)
								break;
							// &amp;lt;a href="https://www.google.com/maps?q=-36.0851437761,146.917419491&amp;amp;layer=c&amp;amp;cbll=-36.0851437761,146.917419491"&amp;gt;&amp;lt;p&amp;gt;&amp;lt;font color="red"&amp;gt;View in Google Maps SV&amp;lt;/font&amp;gt;&amp;lt;/p&amp;gt;
							// into
							// &lt;a href="https://www.google.com/maps?q=-36.0851437761,146.917419491&amp;layer=c&amp;cbll=-36.0851437761,146.917419491"&gt;&lt;p&gt;&lt;font color="red"&gt;View in Google Maps&lt;/font&gt;&lt;/p&gt;
							if(line.contains("&amp;lt;"))
							{
								line = line.replace("&amp;lt;", "&lt;");
							}
							if(line.contains("&amp;gt;"))
							{
								line = line.replace("&amp;gt;", "&gt;");
							}
							buffWriter.write(line);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
					buffWriter.flush();
					buffWriter.close();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		catch(FileNotFoundException ignored)
		{
		}
	}
}
