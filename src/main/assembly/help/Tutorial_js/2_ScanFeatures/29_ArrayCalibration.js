///////////////////////////////////////////////////////////////////////////////////////////////////
// Calibrating array and matrix pseudo-devices
/////////////////////////////////////////////////////////////////////////////////////////////////// 


var ArrayCalibrated = Java.extend(ReadableArray, ReadableCalibratedArray)
var ac1 = new ArrayCalibrated() {	    
        read: function () {
        	return wf1.read()
        },
        getSize: function (value) {
            return wf1.size
        },
        getCalibration: function (value) {
            return new ArrayCalibration(5,1000)
        },
    }


var ac2 = new ArrayCalibrated() {	    
        read: function () {
        	return wf1.read()
        },
        getSize: function (value) {
            return wf1.size
        },
        getCalibration: function (value) {
            return new ArrayCalibration(5,1000)
        },
    }
    
        
var MatrixCalibrated = Java.extend(ReadableMatrix, ReadableCalibratedMatrix)
var mc1 = new MatrixCalibrated() {	  
        read: function () {
        	return im1.read()
        },
        getWidth: function (value) {
            return im1.width
        },
        getHeight: function (value) {
            return im1.height
        },
        getCalibration: function (value) {
            return new MatrixCalibration(2,4,100,200)   
        },
    }
    
set_device_alias(ac1, "wf1_calib")
set_device_alias(ac2, "wf1_calib_1d")
set_device_alias(mc1, "im1_calib")

set_preference(Preference.PLOT_TYPES, {"wf1_calib_1d":1})

a= lscan(ao1, [wf1, ac1, im1, mc1, ac2], 0, 40, 50, 0.02)         