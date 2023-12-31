package com.rakibofc.downloadfiledemo;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadHelper {

    public static void downloadImage(Context context, String downloadUrl, final DownloadCompleteListener listener) {

        // Get download service and create download manager request
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));

        String downloadPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/Android/data/" + context.getPackageName() + "/files/mushaf/qaloon/";
        String fileName = generateFileName(downloadUrl);

        // Set the destination directory to Android/data/package_name/files directory
        // String destination = Environment.getExternalStorageDirectory() + "/Android/data/" + context.getPackageName() + "/files/mushaf/qaloon/";
        request.setTitle("My File");
        request.setDescription("Downloading");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(false);
        request.setDestinationInExternalFilesDir(context, "/mushaf/qaloon/", fileName);

        // Get download service and enqueue the download
        final long downloadId = downloadManager.enqueue(request);

        // Create a BroadcastReceiver to listen to the download completion event
        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                if (downloadId == id) {

                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

                    // Get the download status
                    Cursor cursor = dm.query(query);

                    if (cursor.moveToFirst()) {

                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = cursor.getInt(statusIndex);

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            // Download completed successfully
                            if (listener != null) {
                                try {
                                    UnpackZip.extract(new File(downloadPath + fileName), new File(downloadPath));
                                    listener.onDownloadComplete();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        } else {
                            // Download failed
                            if (listener != null) {
                                listener.onDownloadFailed();
                            }
                        }
                    } else {
                        // Download failed
                        if (listener != null) {
                            listener.onDownloadFailed();
                        }
                    }
                    // Unregister the BroadcastReceiver
                    context.unregisterReceiver(this);
                }
            }
        };
        // Register the BroadcastReceiver to receive the download completion event
        context.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private static String generateFileName(String url) {

        try {
            URL fileUrl = new URL(url);
            String path = fileUrl.getPath();

            return path.substring(path.lastIndexOf('/') + 1);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}