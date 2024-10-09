package com.example.filetransferapp;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HttpClient extends AppCompatActivity {
    private static final String TAG = "HttpClient";
    private long startTime;

    private EditText editTextHttpUrl, editTextFileName, editTextTransferCount;
    private TextView textViewStatus, textViewProgress;
    private ProgressBar progressBar;

    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_client);

        // Initialize UI elements
        editTextHttpUrl = findViewById(R.id.editTextHttpUrl);
        editTextFileName = findViewById(R.id.editTextFileName);
        editTextTransferCount = findViewById(R.id.editTextTransferCount);

        textViewStatus = findViewById(R.id.status);
        progressBar = findViewById(R.id.progressBar);
        textViewProgress = findViewById(R.id.textViewProgress);

        ActivityCompat.requestPermissions(this,
                new String[]{READ_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);

        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(threadPolicy);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Acquire wake lock to prevent the screen from going to sleep
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        wakeLock.acquire();  // Acquire the wake lock
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the wake lock when the activity is destroyed
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void buttonDownloadFTPFile(View view) {
        Log.d(TAG, "buttonDownloadFTPFile: Started");
        startTime = SystemClock.elapsedRealtime(); // Start the timer

        StorageManager storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
        StorageVolume storageVolume = storageManager.getStorageVolumes().get(0); // 0 for internal storage
        FileOutputStream fileOutputStream = null;

        try {
            String httpUrl = editTextHttpUrl.getText().toString().trim();
            Log.d(TAG, "HTTP URL: " + httpUrl);
            if (!httpUrl.startsWith("http://") && !httpUrl.startsWith("https://")) {
                textViewStatus.setText("Invalid HTTP URL");
                return;
            }

            URL url = new URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                textViewStatus.setText("Failed to connect to server");
                return;
            }
            Log.d(TAG, "Connected successfully");

            int fileSize = connection.getContentLength();
            progressBar.setMax(fileSize);

            String fileName = editTextFileName.getText().toString();
            Log.d(TAG, "File Name: " + fileName);

            String transferCountString = editTextTransferCount.getText().toString().trim();
            int transferCount = transferCountString.isEmpty() ? 1 : Integer.parseInt(transferCountString);

            for (int i = 0; i < transferCount; i++) {
                // Add a timestamp to the filename
                String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HHmmss", Locale.getDefault()).format(new Date());
                String newFileName = timeStamp + "_" + fileName;

                String localFilePath = storageVolume.getDirectory().getPath() +
                        "/Download/ftpfiles/" + newFileName;
                Log.d(TAG, "Local File Path: " + localFilePath);

                File localFile = new File(localFilePath);
                fileOutputStream = new FileOutputStream(localFile);

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                byte[] buffer = new byte[2 * 1024 * 1024]; // buffer size
                long bytesRead = 0;
                int count;
                while ((count = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, count);
                    bytesRead += count;
                    final long finalBytesRead = bytesRead;
                    runOnUiThread(() -> {
                        progressBar.setProgress((int) finalBytesRead);
                        textViewProgress.setText(
                                formatFileSize(finalBytesRead) + " / " + formatFileSize(fileSize));
                    });
                }

                String stringText = "Downloaded File";
                long elapsedTime = SystemClock.elapsedRealtime() - startTime; // Calculate elapsed time
                String elapsedTimeText = "Time taken: " + formatElapsedTime(elapsedTime); // Format the time
                textViewStatus.setText(stringText + "\n" + elapsedTimeText); // Display the status with time
                Log.d(TAG, stringText);

                fileOutputStream.flush();
                fileOutputStream.close();
            }

            connection.disconnect();
            Log.d(TAG, "HTTP Client disconnected");

            Toast.makeText(this, "File(s) downloaded successfully", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "File downloaded successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error: ", e);
            textViewStatus.setText(e.toString());
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams: ", e);
            }
        }
        Log.d(TAG, "buttonDownloadFTPFile: Ended");
    }

    private String formatFileSize(long size) {
        if (size >= 1024 * 1024 * 1024) {
            return String.format("%.2f GB", (double) size / (1024 * 1024 * 1024));
        } else if (size >= 1024 * 1024) {
            return String.format("%.2f MB", (double) size / (1024 * 1024));
        } else if (size >= 1024) {
            return String.format("%.2f KB", (double) size / 1024);
        } else {
            return size + " B";
        }
    }

    private String formatElapsedTime(long elapsedTime) {
        long seconds = (elapsedTime / 1000);
        return String.format(Locale.getDefault(), "%d seconds", seconds);
    }
}
