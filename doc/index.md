<!---
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
-->

# com.grumpysailor.cordova.device-rotation-vector

This plugin provides access to the device's [Rotation Vector Sensor](http://developer.android.com/guide/topics/sensors/sensors_motion.html#sensors-motion-rotate) using TYPE_GAME_ROTATION_VECTOR. The Game Rotation Vector represents the orientation of the device (without using the earth's geomagnetic field) as a combination of an angle and an axis, in which the device has rotated through an angle θ around an axis (x, y, or z) 




## Installation

    cordova plugin add https://github.com/marciopuga/cordova-plugin-device-rotation-vector.git

## Supported Platforms

- Android

## Methods

- navigator.rotationvector.getCurrentRotationVector
- navigator.rotationvector.watchRotationVector
- navigator.rotationvector.clearWatch

## Objects

- RotationVector

## navigator.rotationvector.getCurrentRotationVector

Get the current orientation with _alpha_, _beta_, and _gamma_.

These orientation values are returned to the `rotationVectorSuccess`
callback function.

    navigator.rotationvector.getCurrentRotationVector(rotationVectorSuccess, rotationVectorError);


### Example

    function onSuccess(rotationvector) {
        alert('rotationvector alpha: ' + rotationvector.alpha + '\n' +
              'rotationvector beta: ' + rotationvector.beta + '\n' +
              'rotationvector gamma: ' + rotationvector.gamma + '\n' +
              'Timestamp: '      + rotationvector.timestamp + '\n');
    };

    function onError() {
        alert('onError!');
    };

    navigator.rotationvector.getCurrentRotationVector(onSuccess, onError);


## navigator.rotationvector.watchRotationVector

Retrieves the device's current `Orientation` at a regular interval, executing
the `rotationVectorSuccess` callback function each time. Specify the interval in
milliseconds via the `rotationVectorOptions` object's `frequency` parameter.

The returned watch ID references the RotationVector's watch interval,
and can be used with `navigator.rotationvector.clearWatch` to stop watching the
rotation vector.

    var watchID = navigator.rotationvector.watchRotationVector(rotationVectorSuccess,
                                                           rotationVectorError,
                                                           rotationVectorOptions);

- rotationVectorOptions: An object with the following optional keys:
  - __period__: requested period of calls to rotationVectorSuccess with Rotation Vector  data in Milliseconds. _(Number)_ (Default: 10000)


###  Example

    function onSuccess(rotationvector) {
        alert('rotationvector alpha: ' + rotationvector.alpha + '\n' +
              'rotationvector beta: ' + rotationvector.beta + '\n' +
              'rotationvector gamma: ' + rotationvector.gamma + '\n' +
              'Timestamp: '      + rotationvector.timestamp + '\n');
    };

    function onError() {
        alert('onError!');
    };

    var options = { frequency: 3000 };  // Update every 3 seconds

    var watchID = navigator.rotationvector.watchRotationVector(onSuccess, onError, options);


## navigator.rotationvector.clearWatch

Stop watching the `Orientation` referenced by the `watchID` parameter.

    navigator.rotationvector.clearWatch(watchID);

- __watchID__: The ID returned by `navigator.rotationvector.watchRotationVector`.

###  Example

    var watchID = navigator.rotationvector.watchRotationVector(onSuccess, onError, options);

    // ... later on ...

    navigator.rotationvector.clearWatch(watchID);
