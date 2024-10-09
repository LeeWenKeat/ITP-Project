package com.example.filetransferapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Sender extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;

    private Button buttonOpenDialog, send;
    private TextView dataPath;
    private BluetoothAdapter btAdapter;
    private final ArrayList<Uri> fileUris = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        dataPath = findViewById(R.id.FilePath);
        buttonOpenDialog = findViewById(R.id.btnCFile);
        send = findViewById(R.id.btnTransfer);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Request necessary permissions
        requestPermissions();

        buttonOpenDialog.setOnClickListener(v -> openFilePicker());
        send.setOnClickListener(v -> sendViaBluetooth());
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    fileUris.clear();
                    StringBuilder filenames = new StringBuilder();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            fileUris.add(uri);
                            String filename = getFileName(uri);
                            filenames.append(filename).append("\n");
                        }
                    } else if (data.getData() != null) {
                        Uri uri = data.getData();
                        fileUris.add(uri);
                        String filename = getFileName(uri);
                        filenames.append(filename).append("\n");
                    }
                    dataPath.setText(filenames.toString().trim());
                }
            }
    );

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String path = uri.getPath();
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            return path.substring(cut + 1);
        }
        return path;
    }

    private void sendViaBluetooth() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        } else {
            enableBluetooth();
        }
    }

    private final ActivityResultLauncher<Intent> bluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    sendFile();
                }
            }
    );

    private void enableBluetooth() {
        if (!btAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothLauncher.launch(intent);
        } else {
            sendFile();
        }
    }

    private void sendFile() {
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> appsList = pm.queryIntentActivities(intent, 0);
        if (!appsList.isEmpty()) {
            for (ResolveInfo info : appsList) {
                if (info.activityInfo.packageName.equals("com.android.bluetooth")) {
                    intent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                    startActivity(intent);
                    return;
                }
            }
            Toast.makeText(this, "Bluetooth app not found", Toast.LENGTH_SHORT).show();
        }
    }
}