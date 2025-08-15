///////////////////////////////////////////////////////////////////////////////////////////////////
// Use of HardwareScan 
///////////////////////////////////////////////////////////////////////////////////////////////////


CrlogicPositioner = Java.type('ch.psi.pshell.crlogic.CrlogicPositioner')
CrlogicSensor = Java.type('ch.psi.pshell.crlogic.CrlogicSensor')

//sc1.stop()
//sc1.setOneShot()
//sc1.channels[0].setPreset(false)
//sc1.channels[0].setPresetValue(0)
//sc1.start()

config = {}
config["class"] = "ch.psi.pshell.crlogic.CrlogicScan"
config["prefix"] = "MTEST-HW3-CRL"
config["ioc"] = "MTEST-VME-HW3.psi.ch"
config["integrationTime"] = 0.01
config["additionalBacklash"] = 0.0

pos = new CrlogicPositioner("CrlogicPositioner", "MTEST-HW3:MOT1", null)

sensors = [
    new CrlogicSensor("Trigger0", "TRIGGER0"),
    new CrlogicSensor("Trigger1", "TRIGGER1"),    
    new CrlogicSensor("Scaler0", "SCALER0", true),    
    new CrlogicSensor("Scaler1", "SCALER1", true),    
    new CrlogicSensor("Timestamp", "TIMESTAMP"),   
    ]



//pos.initialize()      
//pos.move(0.0)

try {
    r1 = hscan(config, pos, sensors,0.0, 10.0, 0.1,1, false)
}
catch(err) {
    
} 
finally {
    pos.close()     
}

