/*
 * Copyright (c) 2017 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of μlogger-android.
 * Licensed under GPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

package net.fabiszewski.ulogger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Export track to KML format
 */
public class KmlExportService extends JobIntentService {

    private static final String TAG = KmlExportService.class.getSimpleName();

    public static final String KML_BROADCAST_EXPORT_FAILED = "net.fabiszewski.ulogger.broadcast.kml_write_failed";
    public static final String KML_BROADCAST_EXPORT_DONE = "net.fabiszewski.ulogger.broadcast.kml_write_ok";
    public static final String KML_EXTENSION = ".kml";
    //public static final String GPX_MIME = "application/gpx+xml";

    private static String mFilename = null;

    private DbAccess db;

    private Context mContext;

    static final int JOB_ID = 1001;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work, String fn) {
        mFilename = fn;
        enqueueWork(context, KmlExportService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Logger.DEBUG) { Log.d(TAG, "[kml export create]"); }

        db = DbAccess.getOpenInstance(this);
    }

    /**
     * Cleanup
     */
    @Override
    public void onDestroy() {
        if (Logger.DEBUG) { Log.d(TAG, "[kml export stop]"); }
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    /**
     * Handle intent
     *
     * @param intent Intent
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (intent.getData() != null) {
            try {
                write(intent.getData());
                CFormat.removeAmpFromKMLFile(intent.getData(), mFilename, getApplicationContext());
                sendBroadcast(KML_BROADCAST_EXPORT_DONE, null);
            } catch (IOException e) {
                sendBroadcast(KML_BROADCAST_EXPORT_FAILED, e.getMessage());
            }
        }
    }

    /**
     * Write serialized track to URI
     * @param uri Target URI
     * @throws IOException Exception
     */
    private void write(@NonNull Uri uri) throws IOException {
        OutputStream stream = getContentResolver().openOutputStream(uri);
        if (stream == null) {
            throw new IOException(getString(R.string.e_open_out_stream));
        }
        try (BufferedOutputStream bufferedStream = new BufferedOutputStream(stream)) {
            CGPSCoordinatesKMLHandler handler = new CGPSCoordinatesKMLHandler();
            handler.GPSCoordinatesKMLStreamWriter(bufferedStream, db.getPositions(), db.getPositions(), getApplicationContext());
            if (Logger.DEBUG) { Log.d(TAG, "[export kml file written to " + uri); }
        } catch (IOException|IllegalArgumentException|IllegalStateException e) {
            if (Logger.DEBUG) { Log.d(TAG, "[export kml write exception: " + e + "]"); }
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Send broadcast message
     * @param broadcast Broadcast intent
     * @param message Optional extra message
     */
    private void sendBroadcast(String broadcast, String message) {
        Intent intent = new Intent(broadcast);
        if (message != null) {
            intent.putExtra("message", message);
        }
        sendBroadcast(intent);
    }
}
