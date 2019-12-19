
/**
 * RemoteCameraServer that extends WebSocketServer to control multiple cameras via a socket connection
 *
 * @author  Vishnu Raveendra Nadhan
 * @version 1.0
 */

package com.example.nativeremotecamera;

import java.io.FileOutputStream;
import java.io.IOException;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ImageUploadQueue extends IntentService {

    public static final String IMAGE_UPLOAD_TAG = "ImageUpload";

    public ImageUploadQueue() {
        super("ImageUploadQueue IntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        byte[] bytes = intent.getByteArrayExtra(Constants.IMAGE_DATA);
        byte[] bytes = ApplicationData.image_data;
        String imageFileName = intent.getStringExtra(Constants.IMAGE_FILE_NAME);
        String imageFileAbsPath = intent.getStringExtra(Constants.IMAGE_FILE_ABS_PATH);
        String cameraId = intent.getStringExtra(Constants.STORE_ID);
        String storeId = intent.getStringExtra(Constants.CAMERA_ID);
        String uploadServerURL = intent.getStringExtra(Constants.UPLOAD_SERVER_URL);
//        String image_upload_server_hostname = "http://data.molnify.com:7001/imageconsumer";
        String outputMessage = null;

        if (imageFileName == null || imageFileName == "") {
            return;
        }

        if (cameraId == null || cameraId == "") {
            return;
        }

        if (storeId == null || storeId == "") {
            return;
        }

        if (bytes == null) {
            return;
        }

        FileOutputStream output = null;
        //            output = new FileOutputStream(imageFileAbsPath);
        //            YuvImage yuv = new YuvImage(bytes, ImageFormat.NV21, 4160, 3120, null);
        //            yuv.compressToJpeg(new Rect(0, 0, 4160, 3120), 100, output);

        try {
            output = new FileOutputStream(imageFileAbsPath);
            output.write(bytes);


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Upload image on server
//        try {
//            Log.d(IMAGE_UPLOAD_TAG, "Image file size : " + bytes.length);
//            Log.d(IMAGE_UPLOAD_TAG, "imageFileAbsPath : " + imageFileAbsPath);
//            CloseableHttpClient client = HttpClients.createDefault();
//            HttpPost httpPost = new HttpPost(uploadServerURL);
//
//            MultipartEntityBuilder mpEntity = MultipartEntityBuilder.create();
//            mpEntity.addPart("data", new ByteArrayBody(bytes, imageFileName));
//
//            httpPost.setEntity(mpEntity.build());
//
//            httpPost.addHeader("wwimage-cameraId", cameraId);
//            httpPost.setHeader("wwimage-storeId", storeId);
//
//            CloseableHttpResponse httpResponse = client.execute(httpPost);
//
//            Log.d(IMAGE_UPLOAD_TAG, Integer.toString(httpResponse.getStatusLine().getStatusCode()));
//            Log.d(IMAGE_UPLOAD_TAG, httpResponse.toString());
//
//            if (httpResponse.getStatusLine().getStatusCode() == 200) {
//                outputMessage = "Successfully uploaded file : " + imageFileName;
//            } else {
//                outputMessage = httpResponse.getStatusLine().getReasonPhrase();
//            }
//            client.close();
//        } catch (IOException e) {
//            Log.e(IMAGE_UPLOAD_TAG, "Error in uploading picture to the server");
//            outputMessage = "Error: Failed to upload image to server due to " + e.getMessage();
//            e.printStackTrace();
//        }

        if (outputMessage != null)
            Log.d(IMAGE_UPLOAD_TAG, "Image upload status : " + outputMessage);
    }
}
