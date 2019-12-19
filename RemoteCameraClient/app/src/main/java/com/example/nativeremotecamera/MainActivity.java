
/**
 * RemoteCameraServer that extends WebSocketServer to control multiple cameras via a socket connection
 *
 * @author  Vishnu Raveendra Nadhan
 * @version 1.0
 */

package com.example.nativeremotecamera;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;

//AppCompatActivity
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    public static final String SERVICE_TAG = "CameraService";
    public static final String SOCKET_CLIENT_TAG = "WebSocketsClient";

    CameraAsAService cameraService;
    CameraSocketClient socket = null;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private ImageReader imageReader;
    private File file;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraManager cameraManager;
    private CountDownLatch latch = null;
    View decorView = null;
    private CameraAttributes cameraAttributes;
    private CameraCharacteristics cameraCharacteristics;

    // On create of application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setContentView(R.layout.activity_main);

        // Make the app full screen and hide navigations
        decorView = getWindow().getDecorView();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // View listener methods
        // To hide the bottom navigation if it is displayed upon touch on the screen
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                            // TODO: The system bars are visible. Make any desired
                            // adjustments to your UI, such as showing the action bar or
                            // other navigational controls.
                            MainActivity.this.decorView.setSystemUiVisibility(uiOptions);
                        } else {
                            // TODO: The system bars are NOT visible. Make any desired
                            // adjustments to your UI, such as hiding the action bar or
                            // other navigational controls.
                        }
                    }
                });


        // Start the WebSocketClient
        new Thread(new Runnable() {
            public void run() {
                try {
                    Log.d(TAG, "Starting WebSocketCLient");

                    socket = new CameraSocketClient(MainActivity.this);
                    socket.connect();

                } catch (Exception e) {
                    Log.e(TAG, "Error in creating Web Sockets!" + e);
                }
            }
        }).start();
    }

    private CameraAttributes getCameraAttributes() {
        return cameraAttributes;
    }

    public void setCameraAttributes(CameraAttributes cameraAttributes) {
        this.cameraAttributes = cameraAttributes;
    }

    public void setLatch() {
        latch = new CountDownLatch(1);
    }

    public void awaitLatch() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Latch Interruption!");
        }

    }

    public void decrementLatch() {
        latch.countDown();
    }

    public void startCamera() {
        try {
            Log.d(TAG, "starting camera ...");

            // check permissions
            // Request permissions if absent.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 0);
            }

            // check permissions
            // Request permissions if absent.
            // only for testing.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }

            // Manager
            cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            try {
//                for (String cameraId : cameraManager.getCameraIdList()) {
//                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
//
//                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
//                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
//                        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                        if (map == null)
//                            continue;
//
//                        // For still image captures, we use the largest available size.
//                        Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
//
//                        Log.d(TAG, cameraId);
//
//                        Log.d(TAG, largest.getWidth() + " - " + largest.getHeight());
//                        imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
//                        imageReader.setOnImageAvailableListener(mOnImageAvailableListener, backgroundHandler);
//
//                        mCameraId = cameraId;
////                        Log.d(TAG, "camera Id : " + cameraId);
////                        Log.d(TAG, "camera rotation : " + characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
////                        Log.d(TAG, "camera  : " + getWindowManager().getDefaultDisplay().getRotation());
//                        cameraCharacteristics = characteristics;
//                    }
//                }

                String mCameraId = "0";
                cameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);

                if (cameraCharacteristics == null)
                    return;

                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null)
                    return;

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

                Log.d(TAG, mCameraId);
                Log.d(TAG, largest.getWidth() + " - " + largest.getHeight());

                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
                imageReader.setOnImageAvailableListener(mOnImageAvailableListener, backgroundHandler);

                Log.i(TAG, "Photo Width : " + imageReader.getWidth());
                Log.i(TAG, "Photo Height " + imageReader.getHeight());

            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                // Currently an NPE is thrown when the Camera2API is used but not supported on the
                // device this code runs.
                e.printStackTrace();
            }


            startBackgroundThread();
            cameraManager.openCamera(mCameraId, mStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "A new image is available for upload!");

            // this code will not work if the app is in the background
            Image image = reader.acquireNextImage();
            if (image != null) {

                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);


                // Call Upload Image IntentService
                Intent intent = new Intent(MainActivity.this, ImageUploadQueue.class);
                // App Static data
                ApplicationData.image_data = bytes;
                // Intent Extras
                intent.putExtra(Constants.IMAGE_FILE_ABS_PATH, file.getAbsolutePath());
                intent.putExtra(Constants.IMAGE_FILE_NAME, file.getName());
                intent.putExtra(Constants.STORE_ID, MainActivity.this.getCameraAttributes().getStoreId());
                intent.putExtra(Constants.CAMERA_ID, MainActivity.this.getCameraAttributes().getCameraId());
                intent.putExtra(Constants.UPLOAD_SERVER_URL, MainActivity.this.getCameraAttributes().getUpload_url());
                startService(intent);

                image.close();
            } else {
                Log.e(TAG, "Fatal error: The clicked image is null!");
            }
        }
    };

    private File getOutputMediaFile() {
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaFile;
        mediaFile = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera/", "IMG_" + timeStamp + ".jpg");

        Log.d(TAG, "FILE :" + mediaFile.getAbsolutePath());
        return mediaFile;
    }


    public void takePicture() {
        try {
            if (null == mCameraDevice) {
                Log.e(TAG, "cameraDevice is null when taking picture ...");
                // bring app into the foreground and wait for next takePicture() call.
                Intent intent = new Intent("android.intent.category.LAUNCHER");
                intent.setClassName("com.example.nativeremotecamera", "com.example.nativeremotecamera.MainActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }

            // Create new file.
            file = getOutputMediaFile();
            Log.d(TAG, "filename : " + file.getName());

            // This is the CaptureRequest.Builder that we use to take a picture.
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(imageReader.getSurface());

            // Picture Settings..
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mCaptureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, cameraAttributes.getExposureTime());
            mCaptureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, cameraAttributes.getIso());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) cameraAttributes.getJpeg_quality()); // add this line and set your own quality

            // to follow sensor orientation.
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Log.d(TAG, "new image captured.... " + result.toString());

                    // DEBUG
//                    Integer iso = result.get(result.SENSOR_SENSITIVITY);
//                    long timeExposure = result.get(result.SENSOR_EXPOSURE_TIME);
//                    Log.i(TAG, "[mHdrCaptureCallback][HDR] Photo: "  + " Exposure: " + timeExposure);
//                    Log.i(TAG, "[mHdrCaptureCallback][HDR] Photo: " + " ISO " + iso);
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(mCaptureRequestBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Toast.makeText(MainActivity.this, "new picture", Toast.LENGTH_SHORT).show();
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraDevice = cameraDevice;
            Log.d(TAG, "camera device " + cameraDevice);

            try {

                // Here, we create a CameraCaptureSession for camera preview.
                mCameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    Log.e(TAG, "cameraDevice is null");
                                    return;
                                }

                                // When the session is ready, we start displaying the preview.
                                mCaptureSession = cameraCaptureSession;
                                decrementLatch();
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                Log.d(TAG, "Configuration Failed");
                            }
                        }, null
                );
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "mStateCallback ondisconnect");
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.d(TAG, "mStateCallback onError" + error);
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "mStateCallback onClosed");
        }
    };

    public String getLocalIpAddress() {
        String resultIpv6 = "";
        String resultIpv4 = "";

        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {

                NetworkInterface intf = (NetworkInterface) en.nextElement();

                for (Enumeration enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements(); ) {

                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) {
                            resultIpv4 = inetAddress.getHostAddress().toString();
                        } else if (inetAddress instanceof Inet6Address) {
                            resultIpv6 = inetAddress.getHostAddress().toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((resultIpv4.length() > 0) ? resultIpv4 : resultIpv6);
    }
}


