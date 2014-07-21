package com.doubleencore.sample.wear.ongoingnotification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by carlos on 7/15/14.
 */
public class OngoingNotificationListenerService extends WearableListenerService {
    private static final String TAG = OngoingNotificationListenerService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 100;

    private GoogleApiClient mGoogleApiClient;

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
                if (Constants.PATH_NOTIFICATION.equals(path)) {
                    // Get the data out of the event
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    final String title = dataMapItem.getDataMap().getString(Constants.KEY_TITLE);
                    Asset asset = dataMapItem.getDataMap().getAsset(Constants.KEY_IMAGE);

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
                    Notification.Builder notificationBuilder = new Notification.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                            .setOngoing(true)
                            .extend(new Notification.WearableExtender()
                                    .setDisplayIntent(notificationPendingIntent));

                    // Build the notification and show it
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Constants.PATH_DISMISS)) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
}
