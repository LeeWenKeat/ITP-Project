package com.example.filetransferapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {

    // Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainFragment.
     */
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Setup Edge-to-Edge display
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup button click listener
        //Button btnSend = view.findViewById(R.id.btnSend); // button to next page
        //btnSend.setOnClickListener(v -> {
        //    Intent intent = new Intent(getActivity(), Sender.class);
        //    startActivity(intent);
        //});

        //Button btnReceive = view.findViewById(R.id.btnReceive); // button to next page
        //btnReceive.setOnClickListener(v -> {
        //    Intent intent = new Intent(getActivity(), Receiver.class);
        //    startActivity(intent);
        //});

        Button btntftpserver= view.findViewById(R.id.btntftpserver); // button to next page
        btntftpserver.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TftpServer.class);
            startActivity(intent);
        });

        Button btnFTP= view.findViewById(R.id.btnFTP); // button to next page
        btnFTP.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FtpClient.class);
            startActivity(intent);
        });

        Button btntftpclient= view.findViewById(R.id.btntftpclient); // button to next page
        btntftpclient.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TftpClient.class);
            startActivity(intent);
        });


        //Button btnhttpclient= view.findViewById(R.id.btnHttp); // button to next page
        //btnhttpclient.setOnClickListener(v -> {
        //    Intent intent = new Intent(getActivity(), HttpClient.class);
        //    startActivity(intent);
        //});

        return view;
    }
}
