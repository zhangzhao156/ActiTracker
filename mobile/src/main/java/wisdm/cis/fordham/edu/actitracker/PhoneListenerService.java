package wisdm.cis.fordham.edu.actitracker;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.commons.lang3.SerializationUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PhoneListenerService extends WearableListenerService {

    private static final String TAG = "PhoneListenerService";

    public PhoneListenerService() {
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged called");

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/data")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset watchAccelAsset = dataMapItem.getDataMap().getAsset("ACCEL_ASSET");
                Asset watchGyroAsset = dataMapItem.getDataMap().getAsset("GYRO_ASSET");

                ArrayList<ThreeTupleRecord> watchAccelData = loadDataFromAsset(watchAccelAsset);
                ArrayList<ThreeTupleRecord> watchGyroData = loadDataFromAsset(watchGyroAsset);

                Log.d(TAG, "Size of accel: " + watchAccelData.size() + "Size of gyro: " + watchGyroData.size());
                Log.d(TAG, "Writing watch files...");

                writeFiles(watchAccelData, watchGyroData);
            }
        }
    }

    private void writeFiles(ArrayList<ThreeTupleRecord> watchAccelRecords, ArrayList<ThreeTupleRecord> watchGyroRecords) {
        Log.d(TAG, "Writing files. Size of Accel: " + watchAccelRecords.size() +
                "Size of Gyro: " + watchGyroRecords.size());

        File accelFile = new File(getFilesDir(), "watchAccel.txt");
        File gyroFile = new File(getFilesDir(), "watchGyro.txt");

        try {
            BufferedWriter accelBufferedWriter = new BufferedWriter(new FileWriter(accelFile));
            BufferedWriter gyroBufferedWriter = new BufferedWriter(new FileWriter(gyroFile));

            for (ThreeTupleRecord record : watchAccelRecords) {
                accelBufferedWriter.write(record.toString());
                accelBufferedWriter.newLine();
            }

            for (ThreeTupleRecord record : watchGyroRecords) {
                gyroBufferedWriter.write(record.toString());
                gyroBufferedWriter.newLine();
            }

            accelBufferedWriter.close();
            gyroBufferedWriter.close();
        }

        catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error writing files!");
        }
    }

    private ArrayList<ThreeTupleRecord> loadDataFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult result = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!result.isSuccess()) {
            return null;
        }

        InputStream assetInputStream = Wearable.DataApi
                .getFdForAsset(googleApiClient, asset).await().getInputStream();
        googleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
        }

        return (ArrayList<ThreeTupleRecord>) SerializationUtils.deserialize(assetInputStream);
    }
}