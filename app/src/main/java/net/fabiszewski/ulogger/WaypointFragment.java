/*
 * Copyright (c) 2019 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of μlogger-android.
 * Licensed under GPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

package net.fabiszewski.ulogger;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.what3words.androidwrapper.What3WordsV3;
import com.what3words.javawrapper.request.Coordinates;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

//import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
//import io.reactivex.rxjava3.core.Observable;
//import io.reactivex.rxjava3.schedulers.Schedulers;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.EXTRA_LOCAL_ONLY;
import static android.content.Intent.EXTRA_MIME_TYPES;
import static android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class WaypointFragment extends Fragment implements LoggerTask.LoggerTaskCallback, ImageTask.ImageTaskCallback {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_OPEN = 2;
    private static final int PERMISSION_WRITE = 1;
    private static final int PERMISSION_LOCATION = 2;
    private static final String KEY_URI = "keyPhotoUri";
    private static final String KEY_THUMB = "keyPhotoThumb";
    private static final String KEY_LOCATION = "keyLocation";

    public static final String LatlonIndentifier = "q=";
    public static final String GoogleMapsLinkStr = "http://maps.google.com/maps?" + LatlonIndentifier;

    private static final String TAG = WaypointFragment.class.getSimpleName();

    private TextView locationNotFoundTextView;
    private TextView locationTextView;
    private TextView locationDetailsTextView;
    private EditText commentEditText;
    private Button saveButton;
    private Button shareCurrentLocationButton;
    private ImageView thumbnailImageView;
    private SwipeRefreshLayout swipe;

    private LoggerTask loggerTask;
    private ImageTask imageTask;

    private Location location = null;
    private Uri photoUri = null;
    private Bitmap photoThumb = null;

    private final ExecutorService executor = newCachedThreadPool();

    public WaypointFragment() {
    }

    static WaypointFragment newInstance() {
        return new WaypointFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_waypoint, container, false);

        locationNotFoundTextView = layout.findViewById(R.id.waypointLocationNotFound);
        locationTextView = layout.findViewById(R.id.waypointLocation);
        locationDetailsTextView = layout.findViewById(R.id.waypointLocationDetails);
        commentEditText = layout.findViewById(R.id.waypointComment);
        saveButton = layout.findViewById(R.id.waypointButton);
        shareCurrentLocationButton = layout.findViewById(R.id.shareCurrentLocationButton);
        thumbnailImageView = layout.findViewById(R.id.waypointThumbnail);
        swipe = (SwipeRefreshLayout) layout;
        swipe.setOnRefreshListener(this::reloadTask);

        saveButton.setOnClickListener(this::saveWaypoint);
        shareCurrentLocationButton.setOnClickListener(this::shareCurrentLocation);
        thumbnailImageView.setOnClickListener(this::addImage);
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
        return layout;
    }

    private void shareCurrentLocation(View view) {
        try {
            final Context context = getActivity().getBaseContext();
            final String latitudeStr = String.valueOf(location.getLatitude());
            final String longitudeStr = String.valueOf(location.getLongitude());
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Network activeNetwork = cm.getActiveNetwork();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/html");
                boolean isConnected = true;
                if (activeNetwork == null) {
                    isConnected = false;
                }
                else {
                    NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);
                    if (networkCapabilities == null || !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        isConnected = false;
                    }
                }
                if(!isConnected)    // If no internet connection so use SMS (Android > 9)
                    intent.setData(Uri.parse("smsto:"));  // This ensures only SMS apps respond
                intent.putExtra(Intent.EXTRA_EMAIL, "emailaddress@emailaddress.com");
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_current_location_title));
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_current_location_link_google) + GoogleMapsLinkStr + latitudeStr + "," + longitudeStr);
                startActivity(Intent.createChooser(intent, getString(R.string.share_current_location_send_title)));
            }
            else
            {
                // Create a choice intent
                // "Share", "SMS"
                final CharSequence choiceList[] = {getString(R.string.share_current_location_choice_share), getString(R.string.share_current_location_choice_sms)};
                final boolean bl[] = new boolean[choiceList.length];
                bl[0] = true; // Default setting
                PackageManager pm = context.getPackageManager();

                final android.app.AlertDialog.Builder ad = new android.app.AlertDialog.Builder(getActivity());//MainActivity.this);
                ad.setTitle(getString(R.string.share_current_location_dialog_action_title));
                ad.setSingleChoiceItems(choiceList, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        bl[arg1] = true;
                        if (arg1 == 0) {
                            bl[1] = false;
                        } else if (arg1 == 1) {
                            bl[0] = false;
                        }
                    }
                });
                ad.setPositiveButton(getString(R.string.dialogBoxOK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TextView tv = null;
                        int Idx = -1;
                        Date date = new Date(System.currentTimeMillis());
                        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        String message = getString(R.string.share_current_location_link_google) + GoogleMapsLinkStr + latitudeStr + "," + longitudeStr;
                        if (bl[0]) {
                            // Share intent
                            try {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/html");
                                intent.putExtra(Intent.EXTRA_EMAIL, "emailaddress@emailaddress.com");
                                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_current_location_title));
                                intent.putExtra(Intent.EXTRA_TEXT, message);
                                startActivity(Intent.createChooser(intent, getString(R.string.share_current_location_send_title)));
                                finish();
                            } catch (android.content.ActivityNotFoundException e) {
                                if (Logger.DEBUG) {
                                    Log.d(TAG, e.getMessage());
                                }
                            } catch (Exception e) {
                                if (Logger.DEBUG) {
                                    Log.d(TAG, e.getMessage());
                                }
                            }
                        } else if (bl[1]) {
                            // Send SMS
                            try {
                                Intent intent = new Intent(Intent.ACTION_SENDTO);
                                intent.setType("text/plain");
                                intent.setData(Uri.parse("smsto:"));  // This ensures only SMS apps respond
                                intent.putExtra("sms_body", message);
                                //intent.putExtra(Intent.EXTRA_STREAM, attachment);
                                if (intent.resolveActivity(context.getPackageManager()) != null) {
                                    startActivity(intent);
                                }
                            } catch (android.content.ActivityNotFoundException e) {
                                if (Logger.DEBUG) {
                                    Log.d(TAG, e.getMessage());
                                }

                            } catch (Exception e) {
                                if (Logger.DEBUG) {
                                    Log.d(TAG, e.getMessage());
                                }
                            }
                        }
                    }
                });
                ad.setNegativeButton(getString(R.string.dialogBoxCancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                ad.show();
            }
        } catch (Exception e) {
            if (Logger.DEBUG) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    private void shareCurrentLocationWhat3Words(View view) {
        try
        {
            final Context context = getActivity().getBaseContext();
            What3WordsV3 wrapper = new What3WordsV3("U75NR3E0", context);

            Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            Observable.fromCallable(() -> wrapper.convertTo3wa(new Coordinates(location.getLatitude(), location.getLongitude())).execute())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result.isSuccessful()) {
                            //Log.i("MainActivity", String.format("3 word address: %s", result.getWords()));
                            String message = getString(R.string.share_current_location_link_w3w) + " https://w3w.co/" + result.getWords() + " " + getString(R.string.share_current_location_accuracy) + " " + String.valueOf(location.getAccuracy()) + " metres).";
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.button_share_current_location));
                            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
                            startActivity(Intent.createChooser(shareIntent, getString(R.string.button_share_current_location)));
                        } else {
                            showToast("Unable to get access to what3words server - please check internet connection");
                            if (Logger.DEBUG) { Log.d(TAG, result.getError().getMessage()); }
                        }
                    });
        }
        catch (Exception e)
        {
            if (Logger.DEBUG) { Log.d(TAG, e.getMessage()); }
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(KEY_URI)) {
            photoUri = savedInstanceState.getParcelable(KEY_URI);
            setThumbnail(photoThumb);
        }
        if (savedInstanceState.containsKey(KEY_THUMB)) {
            photoThumb = savedInstanceState.getParcelable(KEY_THUMB);
            setThumbnail(photoThumb);
        }
        if (savedInstanceState.containsKey(KEY_LOCATION)) {
            location = savedInstanceState.getParcelable(KEY_LOCATION);
            setLocationText();
            saveButton.setEnabled(true);
            shareCurrentLocationButton.setEnabled(true);
        }
    }

    private void reloadTask() {
        cancelLoggerTask();
        runLoggerTask();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUri != null) {
            outState.putParcelable(KEY_URI, photoUri);
        }
        if (photoThumb != null) {
            outState.putParcelable(KEY_THUMB, photoThumb);
        }
        if (location != null) {
            outState.putParcelable(KEY_LOCATION, location);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasLocation()) {
            runLoggerTask();
        }
    }

    /**
     * Start logger task
     */
    private void runLoggerTask() {
        if (loggerTask == null || !loggerTask.isRunning()) {
            saveButton.setEnabled(false);
            shareCurrentLocationButton.setEnabled(false);
            location = null;
            clearLocationText();
            loggerTask = new LoggerTask(this);
            executor.execute(loggerTask);
            setRefreshing(true);
        }
    }

    /**
     * Stop logger task
     */
    private void cancelLoggerTask() {
        if (Logger.DEBUG) { Log.d(TAG, "[cancelLoggerTask]"); }
        if (loggerTask != null && loggerTask.isRunning()) {
            if (Logger.DEBUG) { Log.d(TAG, "[cancelLoggerTask effective]"); }
            loggerTask.cancel();
            loggerTask = null;
            if (imageTask == null || !imageTask.isRunning()) {
                setRefreshing(false);
            }
        }
    }

    private void cancelImageTask() {
        if (Logger.DEBUG) { Log.d(TAG, "[cancelImageTask]"); }
        if (imageTask != null && imageTask.isRunning()) {
            if (Logger.DEBUG) { Log.d(TAG, "[cancelImageTask effective]"); }
            imageTask.cancel();
            imageTask = null;
            if (loggerTask == null || !loggerTask.isRunning()) {
                setRefreshing(false);
            }
        }
    }

    /**
     * Start image task
     */
    private void runImageTask(@NonNull Uri uri) {
        if (imageTask == null || !imageTask.isRunning()) {
            clearImage();
            saveButton.setEnabled(false);
            shareCurrentLocationButton.setEnabled(false);
            imageTask = new ImageTask(uri, this);
            executor.execute(imageTask);
            setRefreshing(true);
        }
    }

    private void setRefreshing(boolean refreshing) {
        swipe.setRefreshing(refreshing);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cancelLoggerTask();
        cancelImageTask();
    }

    @Override
    public void onDestroy() {
        ImageHelper.clearImageCache(requireContext());
        super.onDestroy();
    }

    /**
     * Display location details
     */
    private void setLocationText() {
        LocationFormatter formatter = new LocationFormatter(location);
        locationNotFoundTextView.setVisibility(View.GONE);
        locationTextView.setText(String.format("%s\n—\n%s", formatter.getLongitudeDMS(), formatter.getLatitudeDMS()));
        locationTextView.setVisibility(View.VISIBLE);
        locationDetailsTextView.setText(formatter.getDetails(requireContext()));
    }


    private void clearLocationText() {
        locationNotFoundTextView.setVisibility(View.GONE);
        locationTextView.setVisibility(View.VISIBLE);
        locationTextView.setText("");
        locationDetailsTextView.setText("");
    }

    /**
     * Save waypoint action
     * @param view View
     */
    private void saveWaypoint(View view) {
        if (hasLocation()) {
            if (photoUri != null) {
                photoUri = ImageHelper.moveCachedToAppStorage(view.getContext(), photoUri);
            }
            String comment = commentEditText.getText().toString();
            String uri = (photoUri == null) ? null : photoUri.toString();
            DbAccess.writeLocation(view.getContext(), location, comment, uri);
            photoUri = null;
            if (Logger.DEBUG) { Log.d(TAG, "[saveWaypoint: " + location + ", " + comment + ", " + uri + "]"); }
        }
        finish();
    }

    /**
     * Go back to main fragment
     */
    private void finish() {
        requireActivity().getSupportFragmentManager().popBackStackImmediate();
    }

    private boolean hasLocation() {
        return location != null;
    }

    private void takePhoto() {
        if (!hasStoragePermission()) {
            return;
        }
        requestImageCapture();
    }

    private void requestImageCapture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            photoUri = ImageHelper.createImageUri(requireContext());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            int flags = FLAG_GRANT_WRITE_URI_PERMISSION|FLAG_GRANT_READ_URI_PERMISSION|FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
            takePictureIntent.addFlags(flags);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            showToast("You must accept permission for writing photo to external storage");
            ActivityCompat.requestPermissions(requireActivity(), new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_WRITE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_WRITE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                requestImageCapture();
            }
        } else if (requestCode == PERMISSION_LOCATION) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                runLoggerTask();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_OPEN:
                    if (resultData != null && resultData.getData() != null) {
                        photoUri = resultData.getData();
                        runImageTask(photoUri);
                    }
                    break;

                case REQUEST_IMAGE_CAPTURE:
                    if (photoUri != null) {
                        ImageHelper.galleryAdd(requireContext(), photoUri);
                        runImageTask(photoUri);
                    }
                    break;
            }
        }
    }

    /**
     * Set thumbnail on ImageView
     * @param thumbnail Thumbnail bitmap, default placeholder if null
     */
    private void setThumbnail(@Nullable Bitmap thumbnail) {
        if (thumbnail == null) {
            thumbnailImageView.setImageResource(R.drawable.ic_photo_camera_gray_24dp);
        } else {
            thumbnailImageView.setImageBitmap(thumbnail);
        }
    }

    /**
     * Display toast message
     * FIXME: duplicated method
     * @param text Message
     */
    private void showToast(CharSequence text) {
        Context context = getContext();
        if (context != null) {
            Toast toast = Toast.makeText(requireContext(), text, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * Add image action
     * @param view View
     */
    private void addImage(View view) {
        clearImage();
        if (requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            View dialogView = View.inflate(getContext(), R.layout.image_dialog, null);
            builder.setView(dialogView);
            builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

            TextView photoTextView = dialogView.findViewById(R.id.action_photo);
            TextView libraryTextView = dialogView.findViewById(R.id.action_library);

            final AlertDialog dialog = builder.create();
            photoTextView.setOnClickListener(v -> {
                takePhoto();
                dialog.dismiss();
            });
            libraryTextView.setOnClickListener(v -> {
                pickImage();
                dialog.dismiss();
            });
            dialog.show();
        } else {
            pickImage();
        }

    }

    /**
     * Show file picker
     */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = { "image/jpeg", "image/gif", "image/png", "image/x-ms-bmp" };
        intent.putExtra(EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(EXTRA_LOCAL_ONLY, true);
        int flags = FLAG_GRANT_READ_URI_PERMISSION|FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        intent.addFlags(flags);
        try {
            startActivityForResult(intent, REQUEST_IMAGE_OPEN);
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.cannot_open_picker));
        }
    }

    @Override
    public void onLoggerTaskCompleted(Location location) {
        if (Logger.DEBUG) { Log.d(TAG, "[onLoggerTaskCompleted: " + location + "]"); }
        this.location = location;
        if (imageTask == null || !imageTask.isRunning()) {
            setRefreshing(false);
        }
        setLocationText();
        saveButton.setEnabled(true);
        shareCurrentLocationButton.setEnabled(true);
    }

    @Override
    public void onLoggerTaskFailure(int reason) {
        if (Logger.DEBUG) { Log.d(TAG, "[onLoggerTaskFailure: " + reason + "]"); }
        if (imageTask == null || !imageTask.isRunning()) {
            setRefreshing(false);
        }
        locationTextView.setVisibility(View.GONE);
        locationNotFoundTextView.setVisibility(View.VISIBLE);
        if ((reason & LoggerTask.E_PERMISSION) != 0) {
            showToast(getString(R.string.location_permission_denied));
            Activity activity = getActivity();
            if (activity != null) {
                ActivityCompat.requestPermissions(activity, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSION_LOCATION);
            }
        }
        if ((reason & LoggerTask.E_DISABLED) != 0) {
            showToast(getString(R.string.location_disabled));
        }
    }

    @Override
    public void onImageTaskCompleted(@NonNull Uri uri, @NonNull Bitmap thumbnail) {
        if (Logger.DEBUG) { Log.d(TAG, "[onImageTaskCompleted: " + uri + "]"); }
        photoUri = uri;
        photoThumb = thumbnail;
        setThumbnail(thumbnail);
        if (loggerTask == null || !loggerTask.isRunning()) {
            setRefreshing(false);
        }
        if (this.location != null) {
            saveButton.setEnabled(true);
            shareCurrentLocationButton.setEnabled(true);
        }
    }

    @Override
    public void onImageTaskFailure(@NonNull String error) {
        if (Logger.DEBUG) { Log.d(TAG, "[onImageTaskFailure: " + error + "]"); }
        clearImage();
        if (loggerTask == null || !loggerTask.isRunning()) {
            setRefreshing(false);
        }
        String message = getString(R.string.image_task_failed);
        if (!error.isEmpty()) {
            message += ": " + error;
        }
        showToast(message);
        if (this.location != null) {
            saveButton.setEnabled(true);
            shareCurrentLocationButton.setEnabled(true);
        }
    }

    private void clearImage() {
        if (photoUri != null) {
            ImageHelper.clearImageCache(requireContext());
            photoUri = null;
        }
        if (photoThumb != null) {
            photoThumb = null;
            setThumbnail(null);
        }
    }
}
