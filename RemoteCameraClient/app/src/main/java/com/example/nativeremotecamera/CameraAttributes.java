
/**
 * RemoteCameraServer that extends WebSocketServer to control multiple cameras via a socket connection
 *
 * @author  Vishnu Raveendra Nadhan
 * @version 1.0
 */

package com.example.nativeremotecamera;

import java.io.Serializable;

// Serializable class to be able to send an instance of this class as IntentExtra
public class CameraAttributes implements Serializable {

    private String cameraId;
    private String storeId;
    private String command;
    private int delay;
    private int iso;
    private Long exposureTime;
    private int jpeg_quality;
    // URL for server onto which the clicked images will be uploaded
    private String upload_url;


    public CameraAttributes(String cameraId, String storeId, String command, String delay, String iso, String exposureTime, String jpeg_quality, String upload_url) {
        this.cameraId = cameraId;
        this.storeId = storeId;
        this.command = command;
        this.upload_url = upload_url;

        // Convert Strings to numericals
        try {
            this.delay = Integer.parseInt(delay);
        } catch(Exception e) {
            this.delay = 5000;
        }

        try {
            this.iso = Integer.parseInt(iso);
        } catch(Exception e) {
            this.iso = 230;
        }

        try {
            this.exposureTime = Long.parseLong(exposureTime);
        } catch(Exception e) {
            this.exposureTime = 1000000000l/30;
        }

        try {
            this.jpeg_quality = Integer.parseInt(jpeg_quality);
        } catch(Exception e) {
            this.jpeg_quality = 70;
        }
    }

    public int getIso() {
        return iso;
    }

    public Long getExposureTime() {
        return exposureTime;
    }

    public int getJpeg_quality() {
        return jpeg_quality;
    }

    public String getUpload_url() {
        return upload_url;
    }

    public String getCommand() {
        return command;
    }

    public int getDelay() {
        return delay;
    }

    public String getCameraId() {
        return cameraId;
    }

    public String getStoreId() {
        return storeId;
    }

}
