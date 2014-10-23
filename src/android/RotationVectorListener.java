/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.grumpysailor.cordova.devicerotationvector;

import java.util.List;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Handler;
import android.os.Looper;

/**
 * This class listens to the rotation-vector sensor and stores the latest
 * acceleration values x,y,z.
 */
public class RotationVectorListener extends CordovaPlugin implements SensorEventListener {

    public static int STOPPED = 0;
    public static int STARTING = 1;
    public static int RUNNING = 2;
    public static int ERROR_FAILED_TO_START = 3;
   
    private double alpha,beta,gamma;                                // most recent acceleration values
    private long timestamp;                         // time of most recent value
    private int status;                                 // status of listener
    private int accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;

    private SensorManager sensorManager;    // Sensor manager
    private Sensor mSensor;                           // Acceleration sensor returned by sensor manager

    private CallbackContext callbackContext;              // Keeps track of the JS callback context.

    private Handler mainHandler=null;
    private Runnable mainRunnable =new Runnable() {
        public void run() {
            RotationVectorListener.this.timeout();
        }
    };

    /**
     * Create an rotation-vector listener.
     */
    public RotationVectorListener() {
        this.alpha = 0;
        this.beta = 0;
        this.gamma = 0;
        this.timestamp = 0;
        this.setStatus(RotationVectorListener.STOPPED);
     }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * Executes the request.
     *
     * @param action        The action to execute.
     * @param args          The exec() arguments.
     * @param callbackId    The callback id used when calling back into JavaScript.
     * @return              Whether the action was valid.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (action.equals("start")) {
            this.callbackContext = callbackContext;
            if (this.status != RotationVectorListener.RUNNING) {
                // If not running, then this is an async call, so don't worry about waiting
                // We drop the callback onto our stack, call start, and let start and the sensor callback fire off the callback down the road
                this.start();
            }
        }
        else if (action.equals("stop")) {
            if (this.status == RotationVectorListener.RUNNING) {
                this.stop();
            }
        } else {
          // Unsupported action
            return false;
        }

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT, "");
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        return true;
    }

    /**
     * Called by AccelBroker when listener is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        this.stop();
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------
    //
    /**
     * Start listening for acceleration sensor.
     * 
     * @return          status of listener
    */
    private int start() {
        // If already starting or running, then just return
        if ((this.status == RotationVectorListener.RUNNING) || (this.status == RotationVectorListener.STARTING)) {
            return this.status;
        }

        this.setStatus(RotationVectorListener.STARTING);

        // Get rotation-vector from sensor manager
        List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_GAME_ROTATION_VECTOR);

        // If found, then register as listener
        if ((list != null) && (list.size() > 0)) {
          this.mSensor = list.get(0);
          this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_UI);
          this.setStatus(RotationVectorListener.STARTING);
        } else {
          this.setStatus(RotationVectorListener.ERROR_FAILED_TO_START);
          this.fail(RotationVectorListener.ERROR_FAILED_TO_START, "No sensors found to register rotation-vector listening to.");
          return this.status;
        }

        // Set a timeout callback on the main thread.
        stopTimeout();
        mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.postDelayed(mainRunnable, 2000);

        return this.status;
    }
    private void stopTimeout() {
        if(mainHandler!=null){
            mainHandler.removeCallbacks(mainRunnable);
        }
    }
    /**
     * Stop listening to acceleration sensor.
     */
    private void stop() {
        stopTimeout();
        if (this.status != RotationVectorListener.STOPPED) {
            this.sensorManager.unregisterListener(this);
        }
        this.setStatus(RotationVectorListener.STOPPED);
        this.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    }

    /**
     * Returns an error if the sensor hasn't started.
     *
     * Called two seconds after starting the listener.
     */
    private void timeout() {
        if (this.status == RotationVectorListener.STARTING) {
            this.setStatus(RotationVectorListener.ERROR_FAILED_TO_START);
            this.fail(RotationVectorListener.ERROR_FAILED_TO_START, "rotation-vector could not be started.");
        }
    }

    /**
     * Called when the accuracy of the sensor has changed.
     *
     * @param sensor
     * @param accuracy
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Only look at rotation-vector events
        if (sensor.getType() != Sensor.TYPE_GAME_ROTATION_VECTOR) {
            return;
        }

        // If not running, then just return
        if (this.status == RotationVectorListener.STOPPED) {
            return;
        }
        this.accuracy = accuracy;
    }

    /**
     * Sensor listener event.
     *
     * @param SensorEvent event
     */
    public void onSensorChanged(SensorEvent event) {
        float[] deviceRotationMatrix = new float[9];
        // Only look at accelerometer events
        if (event.sensor.getType() != Sensor.TYPE_GAME_ROTATION_VECTOR) {
            return;
        }

        // If not running, then just return
        if (this.status == RotationVectorListener.STOPPED) {
            return;
        }
        this.setStatus(RotationVectorListener.RUNNING);

        if (this.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {

            // Save time that event was received
            this.timestamp = System.currentTimeMillis();

            SensorManager.getRotationMatrixFromVector(deviceRotationMatrix, event.values);
            double[] rotationAngles = new double[3];
            computeDeviceOrientationFromRotationMatrix(deviceRotationMatrix, rotationAngles);

            this.alpha = Math.toDegrees(rotationAngles[0]);
            this.beta = Math.toDegrees(rotationAngles[1]);
            this.gamma = Math.toDegrees(rotationAngles[2]);

            this.win();
        }
    }

    private static double[] computeDeviceOrientationFromRotationMatrix(float[] R, double[] values) {
        if (R.length != 9)
            return values;
 
         if (R[8] > 0) {  // cos(beta) > 0
             values[0] = Math.atan2(-R[1], R[4]);
             values[1] = Math.asin(R[7]);           // beta (-pi/2, pi/2)
             values[2] = Math.atan2(-R[6], R[8]);   // gamma (-pi/2, pi/2)
         } else if (R[8] < 0) {  // cos(beta) < 0
             values[0] = Math.atan2(R[1], -R[4]);
             values[1] = -Math.asin(R[7]);
             values[1] += (values[1] >= 0) ? -Math.PI : Math.PI; // beta [-pi,-pi/2) U (pi/2,pi)
             values[2] = Math.atan2(R[6], -R[8]);   // gamma (-pi/2, pi/2)
         } else { // R[8] == 0
             if (R[6] > 0) {  // cos(gamma) == 0, cos(beta) > 0
                 values[0] = Math.atan2(-R[1], R[4]);
                 values[1] = Math.asin(R[7]);       // beta [-pi/2, pi/2]
                 values[2] = -Math.PI / 2;          // gamma = -pi/2
          } else if (R[6] < 0) { // cos(gamma) == 0, cos(beta) < 0
          values[0] = Math.atan2(R[1], -R[4]);
             values[1] = -Math.asin(R[7]);
             values[1] += (values[1] >= 0) ? -Math.PI : Math.PI; // beta [-pi,-pi/2) U (pi/2,pi)
             values[2] = -Math.PI / 2;          // gamma = -pi/2
         } else { // R[6] == 0, cos(beta) == 0
             // gimbal lock discontinuity
             values[0] = Math.atan2(R[3], R[0]);
             values[1] = (R[7] > 0) ? Math.PI / 2 : -Math.PI / 2;  // beta = +-pi/2
             values[2] = 0;                                        // gamma = 0
         }
     }

        // alpha is in [-pi, pi], make sure it is in [0, 2*pi).
        if (values[0] < 0)
            values[0] += 2 * Math.PI; // alpha [0, 2*pi)

        return values;
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        if (this.status == RotationVectorListener.RUNNING) {
            this.stop();
        }
    }

    // Sends an error back to JS
    private void fail(int code, String message) {
        // Error object
        JSONObject errorObj = new JSONObject();
        try {
            errorObj.put("code", code);
            errorObj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PluginResult err = new PluginResult(PluginResult.Status.ERROR, errorObj);
        err.setKeepCallback(true);
        callbackContext.sendPluginResult(err);
    }

    private void win() {
        // Success return object
        PluginResult result = new PluginResult(PluginResult.Status.OK, this.getRotationJSON());
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void setStatus(int status) {
        this.status = status;
    }
    private JSONObject getRotationJSON() {
        JSONObject r = new JSONObject();
        try {
            r.put("alpha", this.alpha);
            r.put("gamma", this.gamma);
            r.put("beta", this.beta);
            r.put("timestamp", this.timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return r;
    }
}
