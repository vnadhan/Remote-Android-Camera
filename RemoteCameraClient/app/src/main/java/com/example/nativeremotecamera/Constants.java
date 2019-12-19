
/**
 * RemoteCameraServer that extends WebSocketServer to control multiple cameras via a socket connection
 *
 * @author  Vishnu Raveendra Nadhan
 * @version 1.0
 */

package com.example.nativeremotecamera;

public class Constants {

    public static final String IMAGE_DATA = "image_data";
    public static final String IMAGE_FILE_NAME = "image_filename";
    public static final String IMAGE_FILE_ABS_PATH = "image_file_abspath";
    public static final String UPLOAD_SERVER_URL = "uploadServerURL";
    public static final String STORE_ID = "storeId";
    public static final String CAMERA_ID = "cameraId";
    public static final String START_COMMAND = "start";
    public static final String STOP_COMMAND = "stop";

    public static final Boolean DEBUG_LOG = true;
}
