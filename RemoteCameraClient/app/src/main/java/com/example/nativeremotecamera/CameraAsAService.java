
/**
 * RemoteCameraServer that extends WebSocketServer to control multiple cameras via a socket connection
 *
 * @author  Vishnu Raveendra Nadhan
 * @version 1.0
 */

package com.example.nativeremotecamera;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public class CameraAsAService extends Service {

    //    public static final int notify = 5000;  //interval between two services(Here Service run every 5 seconds)
    int count = 0;  //number of times a picture is clicked
    private Handler mHandler = new Handler();   //run on another Thread to avoid crash
    private Timer mTimer = null;    //timer handling
    private final String TAG = "CameraService";
    private CountDownLatch latch = null;
    private MainActivity mainActivity;
    private final IBinder binder = new LocalBinder();
    private CameraAttributes cameraAttributes;

    private static CameraAsAService instance = null;

    public CameraAsAService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.cameraAttributes = (CameraAttributes) intent.getSerializableExtra("cameraAttributes");
        instance = this;
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Photo Service destroyed!");
        instance = null;
        mTimer.cancel();
//        Toast.makeText(this, "Service is Destroyed", Toast.LENGTH_SHORT).show();
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.mainActivity.setCameraAttributes(this.cameraAttributes);
    }

    public class LocalBinder extends Binder {
        public CameraAsAService getService() {
            return CameraAsAService.this;
        }
    }

    //class TimeDisplay for handling task
    class TimeDisplay extends TimerTask {
        CameraAttributes cameraAttributes;

        public TimeDisplay(CameraAttributes cameraAttributes) {
            this.cameraAttributes = cameraAttributes;
        }

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Photo Service task runs, Click a picture");
                    mainActivity.takePicture();
                }
            });
        }
    }

    public void startProcess() {
        if (mTimer != null) // Cancel if already existed
            mTimer.cancel();
        else
            mTimer = new Timer();   //recreate new
        mTimer.scheduleAtFixedRate(new TimeDisplay(this.cameraAttributes), 0, this.cameraAttributes.getDelay());   //Schedule task
        instance = this;
    }
}
