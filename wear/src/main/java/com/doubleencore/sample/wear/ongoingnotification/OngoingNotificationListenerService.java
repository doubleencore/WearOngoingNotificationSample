package com.doubleencore.sample.wear.ongoingnotification;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by carlos on 7/15/14.
 */
public class OngoingNotificationListenerService extends WearableListenerService {
    private static final String TAG = OngoingNotificationListenerService.class.getSimpleName();

    // These values must match the values used in MainActivity
    private static final String PATH = "/ongoingnotification";
    private static final String KEY_TITLE = "title";
    private static final String KEY_IMAGE = "image";

    private GoogleApiClient mGoogleApiClient;
    private int notificationId = 100;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Service failed to connect to GoogleApiClient.");
                return;
            }
        }

        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (PATH.equals(path)) {
                    // Get the data out of the event
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    final String title = dataMapItem.getDataMap().getString(KEY_TITLE);
                    Asset asset = dataMapItem.getDataMap().getAsset(KEY_IMAGE);

                    // Build the intent to display our custom notification
                    Intent notificationIntent = new Intent(this, NotificationActivity.class);
                    notificationIntent.putExtra(NotificationActivity.EXTRA_TITLE, title);
                    notificationIntent.putExtra(NotificationActivity.EXTRA_IMAGE, asset);
                    PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                            this,
                            0,
                            notificationIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    // Create the ongoing notification
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                            .setOngoing(true)
                            .extend(new NotificationCompat.WearableExtender()
                                    .setDisplayIntent(notificationPendingIntent));
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                    // Build the notification and show it
                    notificationManager.notify(notificationId, notificationBuilder.build());
                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }
            }
        }
    }
}
