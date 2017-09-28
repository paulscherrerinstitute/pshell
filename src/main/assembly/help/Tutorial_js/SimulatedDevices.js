///////////////////////////////////////////////////////////////////////////////////////////////////
// Simulated Devices
///////////////////////////////////////////////////////////////////////////////////////////////////

var AnalogOutput = Java.extend(RegisterBase)
var dev = new AnalogOutput("ao1") {	    
	    i:double = 0.0,
        doRead: function () {
        	return this.i
        },
        doWrite: function (value) {
            this.i=value
        },
    }
add_device(dev, true)


var AnalogOutput = Java.extend(RegisterBase)
var dev = new AnalogOutput("ao2") {	    
	    i:double = 0.0,
        doRead: function () {
        	return this.i
        },
        doWrite: function (value) {
            this.i=value
        },
    }
add_device(dev, true)

var SinusoidSample = Java.extend(ReadonlyRegisterBase)
var dev = new SinusoidSample("ai1") {	    
	    x:double = 0.0,
        doRead: function () {
            sleep(0.001)
            this.x += 0.1 
            var noise = (Math.random()  - 0.5) / 10.0
            return Math.sin(this.x)  + noise
        },
    }    
add_device(dev, true)


var SinusoidTime = Java.extend(ReadonlyRegisterBase)
var dev = new SinusoidTime("ai2") {	    
        doRead: function () {        
            sleep(0.001)
            var noise = (Math.random()  - 0.5) / 10.0
            return Math.sin(Date.now())  + noise
        },
    }    
add_device(dev, true)


var Random = Java.extend(ReadonlyRegisterBase)
var dev = new Random("ai3") {	    
        doRead: function () {
            sleep(0.001)
            return Math.random() 
        },
    }
    
add_device(dev, true)




var SinusoidWaveform = Java.extend(ReadonlyRegisterBase, ReadonlyRegisterArray)
var dev = new SinusoidWaveform("wf1") {	    
        doRead: function () {        
            sleep(0.001)
            var ret = []
            var x = Math.random()
            for (var i = 0; i < 20; i++) { 
                ret.push(Math.sin(x))
                x = x + 0.1            
            } 
           return to_array(ret, 'd')
        },
   
        getSize: function () {             
            //return wf1.take(-1).length    
            return SinusoidWaveformSuper.take(-1).length    
        },
        
    }    
var SinusoidWaveformSuper = Java.super(dev)    
add_device(dev, true)


var SinusoidImage = Java.extend(ReadonlyRegisterBase, ReadonlyRegisterMatrix)
var dev = new SinusoidImage("im1") {	    
        doRead: function () {        
            sleep(0.001)
            var ret = []
            var width = 200
            var height = 100
            var x = Math.random()
            var base = []
            for (var i = 0; i < width ; i++) { 
                base.push(Math.sin(x))
                x = x  + 0.05      
            } 
            for (var i = 0; i < height ; i++) { 
            	var noise = (Math.random() - 0.5)/5.0 
            	var row = []
            	for (var j = 0; j < width ; j++) { 
            		row.push(base[j]+noise)
            	}            	
                ret.push(row)	
            }
            return to_array(ret, 'd')
        },
   
        getWidth: function () {             
            return SinusoidSuper.take(-1)[0].length    
        },


        getHeight: function () {             
            return SinusoidSuper.take(-1).length    
        },
        
    }   
var SinusoidSuper = Java.super(dev)    
add_device(dev, true)|
        
              
        
//Defintion
add_device(new DummyMotor("m1"), true)
add_device(new DummyMotor("m2"), true)
add_device(new DummyRegister("reg1",3), true)
add_device(new DummyPositioner("p1"),true)
add_device(new MotorGroupBase("mg1", m1, m2), true)
add_device(new MotorGroupDiscretePositioner("dp1", mg1), true)


//Initial Configuration
if (p1.config.unit == null) {
    p1.config.minValue = 0.0 //Not persisted
    p1.config.maxValue = 1000.0
    p1.config.unit = "mm"
    p1.config.save()
    p1.initialize()
}

if (dp1.config.positions == null) {
    dp1.config.positions = ["Park","Ready","Out","Clear"]
    dp1.config.motor1 = ["0.0","4.0","8.0" ,"0.0"]
    dp1.config.motor2 = ["0.0","5.0","3.0" ,"NaN"]
    dp1.config.save()
    dp1.initialize()
}


//Update
m1.setMonitored(true)
m2.setMonitored(true)




///////////////////////////////////////////////////////////////////////////////////////////////////
// Simple Readable / Writable objects can be created and used in scans
///////////////////////////////////////////////////////////////////////////////////////////////////


var WritableScalar = Java.extend(Writable)
var ws1 = new WritableScalar() {	    
        write: function (value) {
        },
    }


var ReadableScalar = Java.extend(Readable)
var rs1 = new ReadableScalar() {	    
        read: function (value) {
        	return Math.random() 
        },
    }


var ReadableWaveform = Java.extend(ReadableArray)
var rw1 = new ReadableWaveform() {	    
	
        read: function (value) {
        	ret = []
        	for (var i=0; i< this.getSize(); i++){
        		ret.push(Math.random())
        	}
        	return to_array(ret, 'd')
        },
        
        getSize: function () {             
            return 20
        },

    }


var ReadableImage = Java.extend(ReadableMatrix)
var ri1 = new ReadableImage() {	    
        read: function (value) {
        	var ret = []        	
        	for (var i=0; i< this.getHeight(); i++){        		
        		var  row = []
        		for (var j=0; j< this.getWidth(); j++){
        			row.push(Math.random())
        		}
        		ret.push(row)
        	}        	
        	return to_array(ret, 'd')
        },
        
        getWidth: function () {             
            return 80
        },

        getHeight: function () {             
            return 40
        },
    }
   
    
    
///////////////////////////////////////////////////////////////////////////////////////////////////
// Imaging
///////////////////////////////////////////////////////////////////////////////////////////////////


GenericDevice = Java.type('ch.psi.pshell.device.GenericDevice')
File = Java.type('java.io.File')
var f = new File(GenericDevice.getConfigFileName("m1"))
configured = f.exists()

add_device(new RegisterMatrixSource("src1", im1), true)
add_device(new RegisterMatrixSource("src2", ri1), true)

src1.polling = 100
src2.polling = 100

//Some configuration for so the imaging will work out of the box
if (!configured){
	Colormap = Java.type('ch.psi.pshell.imaging.Colormap')
    src1.config.colormapAutomatic = true
    src1.config.colormap = configured.Temperature
    src1.config.save()
    src2.config.colormapAutomatic = true
    src2.config.save()
}


