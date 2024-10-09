package com.example.filetransferapp;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;

public class TftpServer extends AppCompatActivity {

    private static final int SERVER_PORT = 6969;
    private static final int DEFAULT_BLOCK_SIZE = 512;  // Default TFTP block size
    private static final int MAX_BLOCK_SIZE = 65464;    // Maximum block size (negotiated)
    private int blockSize = DEFAULT_BLOCK_SIZE;         // Negotiated block size
    private TextView serverStatus;
    private TextView serverIp;
    private Button startServerBtn;
    private Button stopServerBtn;
    private boolean serverRunning = false;
    private DatagramSocket socket;
    private int blockNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tftp_server);

        serverStatus = findViewById(R.id.serverStatus);
        serverIp = findViewById(R.id.serverIp);
        startServerBtn = findViewById(R.id.startServerBtn);
        stopServerBtn = findViewById(R.id.stopServerBtn);

        // Create the testfile.txt in the internal storage directory
        try {
            File file = new File(getFilesDir(), "testfile.txt");
            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write("This is a test file.".getBytes());
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        startServerBtn.setOnClickListener(v -> {
            if (!serverRunning) {
                new Thread(this::startTftpServer).start();
            } else {
                Toast.makeText(this, "Server is already running", Toast.LENGTH_SHORT).show();
            }
        });

        stopServerBtn.setOnClickListener(v -> {
            if (serverRunning) {
                stopTftpServer();
            } else {
                Toast.makeText(this, "Server is not running", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTftpServer() {
        try {
            runOnUiThread(() -> {
                serverStatus.setText("Starting server...");
                startServerBtn.setEnabled(false);
                stopServerBtn.setEnabled(true);
            });

            socket = new DatagramSocket(SERVER_PORT);
            serverRunning = true;

            // Get the Wi-Fi IP address and generate the TFTP link
            String tftpLink = generateTftpLink();

            runOnUiThread(() -> {
                serverStatus.setText("Server running on port " + SERVER_PORT);
                serverIp.setText("TFTP Link: " + tftpLink);
            });

            byte[] buffer = new byte[DEFAULT_BLOCK_SIZE + 4];  // TFTP header (4 bytes) + block size
            while (serverRunning) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                if (isReadRequest(packet.getData())) {
                    handleReadRequest(socket, clientAddress, clientPort);
                } else if (isWriteRequest(packet.getData())) {
                    handleWriteRequest(socket, clientAddress, clientPort);
                } else if (isBlocksizeOption(packet.getData())) {
                    negotiateBlocksize(packet.getData(), socket, clientAddress, clientPort);
                }
            }
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "TFTP Server: " + e.getMessage(), Toast.LENGTH_LONG).show();
                serverStatus.setText("Server closed");
                startServerBtn.setEnabled(true);
                stopServerBtn.setEnabled(false);
            });
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private String generateTftpLink() {
        // Get the Wi-Fi IP address
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        String ipString = Formatter.formatIpAddress(ipAddress);

        // Construct the TFTP link
        return "tftp://" + ipString + ":" + SERVER_PORT;
    }

    private void stopTftpServer() {
        serverRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        runOnUiThread(() -> {
            serverStatus.setText("Server stopped");
            startServerBtn.setEnabled(true);
            stopServerBtn.setEnabled(false);
            serverIp.setText("TFTP Link: N/A");
        });
    }

    private boolean isReadRequest(byte[] data) {
        return data[0] == 0 && data[1] == 1; // Opcode for RRQ
    }

    private boolean isWriteRequest(byte[] data) {
        return data[0] == 0 && data[1] == 2; // Opcode for WRQ
    }

    private boolean isBlocksizeOption(byte[] data) {
        // Parse for blocksize option, typically starts after filename in RRQ/WRQ
        String request = new String(data);
        return request.contains("blksize");
    }

    private void negotiateBlocksize(byte[] data, DatagramSocket socket, InetAddress clientAddress, int clientPort) throws IOException {
        // Extract blocksize from the option request and negotiate it
        String request = new String(data);
        String[] options = request.split("\0");
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals("blksize")) {
                blockSize = Math.min(Integer.parseInt(options[i + 1]), MAX_BLOCK_SIZE);
                break;
            }
        }
        // Send ACK with the negotiated blocksize
        byte[] ackPacket = {0, 6, 'b', 'l', 'k', 's', 'i', 'z', 'e', 0, (byte) (blockSize >> 8), (byte) blockSize, 0};
        socket.send(new DatagramPacket(ackPacket, ackPacket.length, clientAddress, clientPort));
    }

    private byte[] getNextBlockNumber() {
        blockNumber++;
        byte[] block = new byte[2];
        block[0] = (byte) ((blockNumber >> 8) & 0xFF);
        block[1] = (byte) (blockNumber & 0xFF);
        return block;
    }

    private void handleReadRequest(DatagramSocket socket, InetAddress clientAddress, int clientPort) throws IOException {
        File file = new File(getFilesDir(), "largefile.txt");
        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[blockSize];
        int bytesRead;
        blockNumber = 0;

        while ((bytesRead = fis.read(buffer)) != -1) {
            byte[] dataPacket = new byte[4 + bytesRead];
            byte[] block = getNextBlockNumber();

            dataPacket[0] = 0;
            dataPacket[1] = 3;  // DATA Opcode
            dataPacket[2] = block[0];
            dataPacket[3] = block[1];

            System.arraycopy(buffer, 0, dataPacket, 4, bytesRead);

            DatagramPacket sendPacket = new DatagramPacket(dataPacket, dataPacket.length, clientAddress, clientPort);
            socket.send(sendPacket);

            // Wait for ACK
            DatagramPacket ackPacket = new DatagramPacket(new byte[4], 4);
            socket.receive(ackPacket);

            blockNumber = (blockNumber == 65535) ? 0 : blockNumber;
        }
        fis.close();
    }

    private void handleWriteRequest(DatagramSocket socket, InetAddress clientAddress, int clientPort) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), "uploaded_large.txt"));

        byte[] ackPacket = {0, 4, 0, 0};  // ACK for block 0
        socket.send(new DatagramPacket(ackPacket, ackPacket.length, clientAddress, clientPort));

        byte[] buffer = new byte[blockSize + 4];
        blockNumber = 0;

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            int receivedBlockNumber = ((buffer[2] & 0xff) << 8) | (buffer[3] & 0xff);
            if (buffer[0] == 0 && buffer[1] == 3 && receivedBlockNumber == blockNumber + 1) {
                fos.write(buffer, 4, packet.getLength() - 4);
                blockNumber++;

                ackPacket[2] = buffer[2];
                ackPacket[3] = buffer[3];
                socket.send(new DatagramPacket(ackPacket, ackPacket.length, clientAddress, clientPort));

                if (packet.getLength() < blockSize + 4) {
                    break;  // End of transfer
                }

                blockNumber = (blockNumber == 65535) ? 0 : blockNumber;
            }
        }
        fos.close();
    }
}
