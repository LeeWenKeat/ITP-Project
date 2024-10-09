package com.example.filetransferapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private static final int REQUEST_CODE_PERMISSIONS = 100;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
        } else {
            loadFiles(recyclerView);
        }

        return view;
    }

    private void loadFiles(RecyclerView recyclerView) {
        // Use app-specific directory in external storage
        File directory = getContext().getExternalFilesDir("ftpfiles");
        if (directory != null) {
            String path = directory.getAbsolutePath();
            Log.d(TAG, "Directory Path: " + path);

            List<File> files = getFilesFromDirectory(directory);
            if (files.isEmpty()) {
                Log.d(TAG, "No files found in the directory");
            } else {
                Log.d(TAG, "Files found: " + files.size());
            }

            FileAdapter adapter = new FileAdapter(files);
            recyclerView.setAdapter(adapter);
        } else {
            Log.e(TAG, "Failed to get external files directory");
        }
    }

    private List<File> getFilesFromDirectory(File directory) {
        List<File> files = new ArrayList<>();
        if (directory.exists()) {
            Log.d(TAG, "Directory exists: " + directory.getAbsolutePath());
            File[] filesArray = directory.listFiles();
            if (filesArray != null && filesArray.length > 0) {
                for (File file : filesArray) {
                    if (file.isFile()) {
                        Log.d(TAG, "File found: " + file.getName());
                        files.add(file);
                    }
                }
            } else {
                Log.d(TAG, "No files in the directory or directory is empty");
            }
        } else {
            Log.e(TAG, "Directory does not exist");
        }
        return files;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                View view = getView();
                if (view != null) {
                    RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
                    loadFiles(recyclerView);
                }
            } else {
                Log.e(TAG, "Permission denied by user");
            }
        }
    }
}
