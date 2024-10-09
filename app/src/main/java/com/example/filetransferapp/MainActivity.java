package com.example.filetransferapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 124;

    //bluetooth
    //private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Request necessary permissions
        requestPermissions();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // Bottom notification
            //Toast.makeText(MainActivity.this, "This is a BETA version application", Toast.LENGTH_LONG).show();

            return insets;
        });

        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

        findViewById(R.id.menu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navView);
        navigationView.setItemIconTintList(null);

        NavController navController = Navigation.findNavController(this, R.id.navHostFragment);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Create necessary directories
        createRequiredDirectories();

        //bluetooth request
        //btAdapter = BluetoothAdapter.getDefaultAdapter();
        //enableBluetooth();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.INTERNET
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
            }
        }
    }

    //private void enableBluetooth() {
    //    if (!btAdapter.isEnabled()) {
    //        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
     //       startActivityForResult(intent, 1);
    //    }
    //}

    private void createRequiredDirectories() {
        File ftpFilesDir = new File(Environment.getExternalStorageDirectory(), "ftpfiles");
        File downloadFtpFilesDir = new File(Environment.getExternalStorageDirectory(), "Download/ftpfiles");

        if (!ftpFilesDir.exists()) {
            boolean result = ftpFilesDir.mkdirs();
            if (result) {
                Toast.makeText(this, "Directory created: " + ftpFilesDir.getPath(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to create directory: " + ftpFilesDir.getPath(), Toast.LENGTH_SHORT).show();
            }
        }

        if (!downloadFtpFilesDir.exists()) {
            boolean result = downloadFtpFilesDir.mkdirs();
            if (result) {
                Toast.makeText(this, "Directory created: " + downloadFtpFilesDir.getPath(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to create directory: " + downloadFtpFilesDir.getPath(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Permission " + permissions[i] + " denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Permission to manage external storage denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
