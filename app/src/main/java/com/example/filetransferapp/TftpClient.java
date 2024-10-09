package com.example.filetransferapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TftpClient extends AppCompatActivity {

    private static final int SERVER_PORT = 6969;
    private static final int BUFFER_SIZE = 516; // 512 bytes for data + 4 bytes for header

    private EditText ipAddressInput, remoteFileInput, transferCountInput;
    private Button connectBtn, downloadBtn;
    private TextView clientStatus;
    private ProgressBar progressBar;
    private String serverIp;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tftp_client);

        ipAddressInput = findViewById(R.id.ipAddressInput);
        remoteFileInput = findViewById(R.id.remoteFileInput);
        transferCountInput = findViewById(R.id.transferCountInput);
        connectBtn = findViewById(R.id.connectBtn);
        downloadBtn = findViewById(R.id.downloadBtn);
        clientStatus = findViewById(R.id.clientStatus);
        progressBar = findViewById(R.id.progressBar);

        downloadBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        connectBtn.setOnClickListener(v -> {
            serverIp = ipAddressInput.getText().toString().trim();
            if (!serverIp.isEmpty()) {
                isConnected = true;
                clientStatus.setText("Connected to Server: " + serverIp);
                downloadBtn.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(TftpClient.this, "Please enter the server IP address", Toast.LENGTH_SHORT).show();
            }
        });

        downloadBtn.setOnClickListener(v -> {
            String remoteFile = remoteFileInput.getText().toString().trim();
            if (!remoteFile.isEmpty()) {
                downloadFile(remoteFile);
            } else {
                Toast.makeText(TftpClient.this, "Please enter the remote file path", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFile(String remoteFile) {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();  // Start the timer
            int transferCount = Integer.parseInt(transferCountInput.getText().toString().trim());

            try {
                runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
                DatagramSocket socket = new DatagramSocket();
                InetAddress serverAddress = InetAddress.getByName(serverIp);

                // Define the directory to save
                File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ftpfiles");
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs(); // Create the directory if it doesn't exist
                }

                for (int i = 1; i <= transferCount; i++) {
                    // Create Read Request (RRQ)
                    byte[] rrq = createRequestPacket((byte) 1, remoteFile);  // 1 is the opcode for RRQ
                    DatagramPacket sendPacket = new DatagramPacket(rrq, rrq.length, serverAddress, SERVER_PORT);
                    socket.send(sendPacket);

                    byte[] receiveBuffer = new byte[BUFFER_SIZE];

                    // Generate a new file name with a timestamp
                    String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HHmmss", Locale.getDefault()).format(new Date());
                    String newFileName = timeStamp + "_" + i + "_" + remoteFile;
                    File outputFile = new File(downloadDir, newFileName);
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    int blockNumber = 1;

                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(receivePacket);

                        byte[] data = receivePacket.getData();
                        int dataLength = receivePacket.getLength();

                        // Check if the received block number matches the expected block number
                        int receivedBlockNumber = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
                        if (receivedBlockNumber == blockNumber) {
                            fos.write(data, 4, dataLength - 4);  // Write data to file (excluding TFTP header)

                            // Send ACK for the received block
                            byte[] ack = createAckPacket(blockNumber);
                            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, serverAddress, receivePacket.getPort());
                            socket.send(ackPacket);

                            blockNumber++;

                            // If data length is less than BUFFER_SIZE, it indicates the last packet
                            if (dataLength < BUFFER_SIZE) {
                                break;
                            }
                        }
                    }

                    fos.close();
                }

                long endTime = System.currentTimeMillis();  // Stop the timer
                long elapsedTime = endTime - startTime;

                runOnUiThread(() -> {
                    String timeFormatted = formatElapsedTime(elapsedTime);
                    clientStatus.setText("Download complete: " + transferCount + " file(s) downloaded to " + downloadDir.getAbsolutePath() + "\nTime used: " + timeFormatted);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(TftpClient.this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

    private String formatElapsedTime(long elapsedTime) {
        long seconds = (elapsedTime / 1000); // Convert milliseconds to seconds
        return String.format(Locale.getDefault(), "%d seconds", seconds);
    }

    private byte[] createRequestPacket(byte opcode, String fileName) {
        byte[] fileNameBytes = fileName.getBytes();
        byte[] modeBytes = "octet".getBytes();
        byte[] requestPacket = new byte[2 + fileNameBytes.length + 1 + modeBytes.length + 1];

        requestPacket[0] = 0;
        requestPacket[1] = opcode;
        System.arraycopy(fileNameBytes, 0, requestPacket, 2, fileNameBytes.length);
        requestPacket[2 + fileNameBytes.length] = 0;
        System.arraycopy(modeBytes, 0, requestPacket, 3 + fileNameBytes.length, modeBytes.length);
        requestPacket[requestPacket.length - 1] = 0;

        return requestPacket;
    }

    private byte[] createAckPacket(int blockNumber) {
        byte[] ackPacket = new byte[4];
        ackPacket[0] = 0;
        ackPacket[1] = 4;  // ACK opcode
        ackPacket[2] = (byte) (blockNumber >> 8);
        ackPacket[3] = (byte) (blockNumber);

        return ackPacket;
    }
}
