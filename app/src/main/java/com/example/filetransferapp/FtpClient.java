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

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FtpClient extends AppCompatActivity {
    private static final String TAG = "FtpClient";
    private long startTime;

    private EditText editTextFtpUrl, editTextUserName, editTextPassWord, editTextFileName, editTextTransferCount;
    private TextView textViewStatus, textViewProgress;
    private ProgressBar progressBar;

    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp_client);

        // Initialize UI elements
        editTextFtpUrl = findViewById(R.id.editTextFtpUrl);
        editTextFileName = findViewById(R.id.editTextFileName);
        editTextPassWord = findViewById(R.id.editTextTextPassword);
        editTextUserName = findViewById(R.id.editTextUserName);
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
        FTPClient ftpClient = new FTPClient();
        FileOutputStream fileOutputStream = null;

        try {
            String ftpUrl = editTextFtpUrl.getText().toString().trim();
            Log.d(TAG, "FTP URL: " + ftpUrl);
            if (!ftpUrl.startsWith("ftp://")) {
                textViewStatus.setText("Invalid FTP URL");
                return;
            }
            ftpUrl = ftpUrl.substring(6); // Remove ftp://
            String[] urlParts = ftpUrl.split(":");
            if (urlParts.length < 2) {
                textViewStatus.setText("Invalid FTP URL format");
                return;
            }
            String serverAddress = urlParts[0];
            int port = Integer.parseInt(urlParts[1]);
            Log.d(TAG, "Server Address: " + serverAddress + ", Port: " + port);

            ftpClient.connect(serverAddress, port);
            boolean login = ftpClient.login(editTextUserName.getText().toString(),
                    editTextPassWord.getText().toString());
            if (!login) {
                textViewStatus.setText("Failed to login to FTP server");
                return;
            }
            Log.d(TAG, "Logged in successfully");

            ftpClient.changeWorkingDirectory("ftpfiles/");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            ftpClient.sendCommand("OPTS UTF8 ON");

            String fileName = editTextFileName.getText().toString();
            Log.d(TAG, "File Name: " + fileName);

            // Check if the file exists on the server
            FTPFile ftpFile = ftpClient.mlistFile(fileName);
            if (ftpFile == null) {
                Log.d(TAG, "File not found using listFile, attempting listFiles");
                FTPFile[] files = ftpClient.listFiles();
                boolean fileFound = false;
                for (FTPFile file : files) {
                    if (file.getName().equalsIgnoreCase(fileName)) {
                        fileFound = true;
                        ftpFile = file;
                        break;
                    }
                }

                if (!fileFound) {
                    textViewStatus.setText("File not found on the server");
                    Log.d(TAG, "File not found on the server");
                    return;
                }
            }

            long fileSize = ftpFile.getSize();
            progressBar.setMax((int) fileSize);

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

                InputStream inputStream = ftpClient.retrieveFileStream(fileName);
                byte[] buffer = new byte[2 * 1024 * 1024]; //size
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

                boolean booleanStatus = ftpClient.completePendingCommand();
                String stringText = "Downloaded File - " + booleanStatus;
                long elapsedTime = SystemClock.elapsedRealtime() - startTime; // Calculate elapsed time
                String elapsedTimeText = "Time taken: " + formatElapsedTime(elapsedTime); // Format the time
                textViewStatus.setText(stringText + "\n" + elapsedTimeText); // Display the status with time
                Log.d(TAG, stringText);

                fileOutputStream.flush();
                fileOutputStream.close();
            }

            ftpClient.logout();
            ftpClient.disconnect();
            Log.d(TAG, "FTP Client disconnected");

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
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
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
        //long milliseconds = elapsedTime % 1000;
        long seconds = (elapsedTime / 1000); //% 60;
        //long minutes = (elapsedTime / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d seconds", seconds);
    }
}
