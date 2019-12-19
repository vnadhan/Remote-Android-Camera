
/**
 * RemoteCameraServer that extends WebSocketServer to control multiple cameras via a socket connection
 *
 * @author  Vishnu Raveendra Nadhan
 * @version 1.0
 */

package com.example.nativeremotecamera;

import android.app.Application;

public class ApplicationData extends Application {
    public static byte[] image_data = null;
    public static String websocket_server_host_ip = null;
    public static String websocket_server_port = null;
}
