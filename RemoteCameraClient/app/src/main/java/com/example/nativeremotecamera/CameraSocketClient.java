
/**
 * RemoteCameraServer that extends WebSocketServer to control multiple cameras via a socket connection
 *
 * @author  Vishnu Raveendra Nadhan
 * @version 1.0
 */

package com.example.nativeremotecamera;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class CameraSocketClient extends WebSocketClient {
    private MainActivity mainActivity;

    // Intent for starting camera as a service
    private Intent startCameraServiceIntent;

    // Constructor
    public CameraSocketClient(MainActivity mainActivity) throws URISyntaxException {
        super(new URI("ws://" + ApplicationData.websocket_server_host_ip + ":"  + ApplicationData.websocket_server_port));
        this.mainActivity = mainActivity;
        startCameraServiceIntent = new Intent(mainActivity, CameraAsAService.class);
    }

    // establishing connection to the camera service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(MainActivity.SERVICE_TAG, "Service connected and starting photo service.");

            // cast the IBinder and get CameraAsAService instance
            CameraAsAService.LocalBinder binder = (CameraAsAService.LocalBinder) service;
            mainActivity.cameraService = binder.getService(); // returns remoteCameraService
            // give the service access to MainActivity
            mainActivity.cameraService.setMainActivity(mainActivity);
            // start latch
            mainActivity.setLatch();
            // start the camera
            mainActivity.startCamera();
            // wait for the camera to finish loading
            mainActivity.awaitLatch();
            // start the process of taking pictures.
            mainActivity.cameraService.startProcess();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(MainActivity.SERVICE_TAG, "Service disconnected!");
        }
    };

    // when the socket establishes a connection with the socket server
    @Override
    public void onOpen(ServerHandshake handshake) {
        Log.d(MainActivity.SOCKET_CLIENT_TAG, "Connected with " + mainActivity.getLocalIpAddress());
        send("SUCCESSFUL_CONNECTION");
    }

    // when the socket connection is closed.
    @Override
    public void onClose(int code, String reason, boolean remote) {
        new Thread(new Runnable() {
            public void run() {
            // Try reconnection
            reconnect();
            }
        }).start();
    }

    // On receiving a message from the socket server
    @Override
    public void onMessage(String message) {
        Log.d(MainActivity.SOCKET_CLIENT_TAG, "Message from " + mainActivity.getLocalIpAddress() + ", " + message);

        String[] splits = null;
        CameraAttributes cameraAttributes = null;
        String outputMessage = null;
        Boolean isError = false;

        // Parse message received from the socket server
        if (message != null && message != "") {
            splits = message.split(";");

            // intent;cameraId;storeId;command;delay;iso;exposureTime;jpeg_quality;upload_url
            // Expecting 9 inputs in the string
            if (splits.length == 9) {
                // camera is asked to start taking pictures?
                if (splits[3].equalsIgnoreCase(Constants.START_COMMAND)) {
                    // Is the camera service already running and taking pictures?
                    // If no, then start. Else, do nothing.
                    if (this.mainActivity.cameraService == null || this.mainActivity.cameraService.getInstance() == null) {
                        // Get the camera attrs
                        cameraAttributes = new CameraAttributes(splits[1], splits[2], splits[3], splits[4], splits[5],splits[6], splits[7], splits[8]);

                        // https://stackoverflow.com/questions/8230606/android-run-thread-in-service-every-x-seconds
                        Log.d(MainActivity.SOCKET_CLIENT_TAG, "Calling service!");
                        startCameraServiceIntent.putExtra("cameraAttributes", cameraAttributes);
                        // Start camera service.
                        this.mainActivity.bindService(startCameraServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                        // Inform socket server
                        send("Photo Service started on " + mainActivity.getLocalIpAddress() + "!");
                    } else {
                        outputMessage = "Photo Service already running on " + mainActivity.getLocalIpAddress() + "!";
                    }

                } else if (splits[3].equalsIgnoreCase(Constants.STOP_COMMAND)) {
                    // Is the camera service running? If yes, then stop. Else, do nothing.
                    if (this.mainActivity.cameraService != null && this.mainActivity.cameraService.getInstance() != null) {
                        Log.d(MainActivity.SOCKET_CLIENT_TAG, "Stopping Photo service on " + mainActivity.getLocalIpAddress() + "!");

                        // Stop camera service
                        this.mainActivity.stopService(startCameraServiceIntent);  // not sure if this is required.
                        this.mainActivity.unbindService(serviceConnection); // this does the work!
                        // Close the camera
                        this.mainActivity.closeCamera();
                        send("Photo Service stopped on " + mainActivity.getLocalIpAddress() + "!");
                    } else {
                        // camera service not running.s
                        outputMessage = "Photo Service is not running on " + mainActivity.getLocalIpAddress() + "!" + " \n " + "Issue a start command to start the Photo Service!";
                    }
                } else {
                    // Error
                    isError = true;
                    outputMessage = "Invalid command received! Command should be either START or STOP";
                }
            } else {
                // Error
                isError = true;
                outputMessage = "Invalid input received! Input should be of the form intent;cameraId;storeId;command;delay;iso;exposureTime;jpeg_quality;upload_url";
            }
        }

        // Send Error / Status message to the socket server.
        if (outputMessage != null) {
            String[] messages = outputMessage.split("/n");
            if(isError) {
                for (String m: messages) {
                    Log.e(MainActivity.SOCKET_CLIENT_TAG, outputMessage);
                    send(outputMessage);
                }
            } else {
                for (String m: messages) {
                    Log.d(MainActivity.SOCKET_CLIENT_TAG, outputMessage);
                    send(outputMessage);
                }
            }
        }
    }

    // On an error that happened in the socket connection
    @Override
    public void onError(Exception ex) {
        if (this.isOpen())
            send("Error on " + mainActivity.getLocalIpAddress() + ", " + ex.getMessage());
    }
}