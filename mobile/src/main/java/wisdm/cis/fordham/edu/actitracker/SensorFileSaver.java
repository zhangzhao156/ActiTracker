package wisdm.cis.fordham.edu.actitracker;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Utility class to write sensor log files.
 */
public final class SensorFileSaver {

    private static final String TAG = "SensorFileSaver";

    private SensorFileSaver() {}

    // Make directory with path /User/Activity
    public static File getDirectory(Context context, String username, String activityName) {
        File directory = new File(context.getFilesDir() + File.separator + username +
                File.separator + activityName);
        if (!directory.isDirectory()) {
            Log.d(TAG, "Creating directory with path: " + directory.getPath());
            directory.mkdirs();
        }
        else {
            Log.d(TAG, "Directory already exists! Path: " + directory.getPath());
        }
        return directory;
    }

    // Make file in directory with name device_sensor_username_activityName_date_time.txt
    public static File createFile(File directory, String username, String activityName,
                                  String sensorName){

        String dateAndTime = new SimpleDateFormat("yyyyMMdd_HHmm")
                .format(Calendar.getInstance().getTime());
        return new File(directory, sensorName + "_" + username + "_" + activityName + "_" +
                dateAndTime + ".txt");
    }

    // Write data to file
    public static void writeFile(File fileName, ArrayList<SensorRecord> data) {
        Log.d(TAG, "Writing file. Name: " + fileName.getPath());
        Log.d(TAG, "Lines: " + data.size());
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));

            for (SensorRecord record : data) {
                bufferedWriter.write(record.toString());
                bufferedWriter.newLine();
            }

            bufferedWriter.close();
        }

        catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error writing files!");
        }
    }
}
