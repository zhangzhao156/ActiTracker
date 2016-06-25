package wisdm.cis.fordham.edu.actitracker;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Listens for sensor logging data from watch (asset byte stream) and unpacks into
 * ArrayList so it can be written to a file.
 */
public class PhoneListenerService extends WearableListenerService {

    private static final String TAG = "PhoneListenerService";
    private static final String ACCEL_ASSET = "ACCEL_ASSET";
    private static final String GYRO_ASSET = "GYRO_ASSET";
    private static final String USERNAME = "USERNAME";
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    private static final String DATA = "/data";
    private static final String WATCH_SENSORS = "/watch_sensors";
    private static final String SENSOR_LIST_STRING = "SENSOR_LIST";
    private static final String SENSOR_CODES = "SENSOR_CODES";
    private static final String WATCH_ACCEL = "watch_accel";
    private static final String WATCH_GYRO = "watch_gyro";


    public PhoneListenerService() {
    }

    /**
     * Gets sensor data from watch and unpacks from asset.
     * Then writes watch files to disk.
     * @param dataEvents
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged called");

        for (DataEvent event : dataEvents) {
            /**
             * Sensor data from phone
             */
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(DATA)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset watchAccelAsset = dataMapItem.getDataMap().getAsset(ACCEL_ASSET);
                Asset watchGyroAsset = dataMapItem.getDataMap().getAsset(GYRO_ASSET);
                String username = dataMapItem.getDataMap().getString(USERNAME);
                String activityName = dataMapItem.getDataMap().getString(ACTIVITY_NAME);

                ArrayList<SensorRecord> watchAccelData = loadDataFromAsset(watchAccelAsset);
                ArrayList<SensorRecord> watchGyroData = loadDataFromAsset(watchGyroAsset);

                writeFiles(watchAccelData, watchGyroData, username, activityName);
            }

            /**
             * Sensor list from phone
             */
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(WATCH_SENSORS)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                DataMap dataMap = dataMapItem.getDataMap();

                ArrayList<String> sensorListString = dataMap.getStringArrayList(SENSOR_LIST_STRING);
                ArrayList<Integer> sensorCodes = dataMap.getIntegerArrayList(SENSOR_CODES);

                for (int i = 0; i < sensorListString.size(); i++) {
                    Log.d(TAG, "Name: " + sensorListString.get(i) + " Code: " + sensorCodes.get(i));
                }
            }
        }
    }

    private void writeFiles(ArrayList<SensorRecord> watchAccelRecords,
                            ArrayList<SensorRecord> watchGyroRecords,
                            String username, String activityName) {
        File directory = SensorFileSaver.getDirectory(this, username, activityName);
        File watchAccelFile = SensorFileSaver.createFile(directory, username, activityName, WATCH_ACCEL);
        File watchGyroFile = SensorFileSaver.createFile(directory, username, activityName, WATCH_GYRO);
        SensorFileSaver.writeFile(watchAccelFile, watchAccelRecords);
        SensorFileSaver.writeFile(watchGyroFile, watchGyroRecords);
    }

    /**
     * Helper function to unpack ArrayList of records from byte stream.
     * @param asset
     * @return
     */
    private ArrayList<SensorRecord> loadDataFromAsset(Asset asset) {
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

        return (ArrayList<SensorRecord>) SerializationUtils.deserialize(assetInputStream);
    }
}