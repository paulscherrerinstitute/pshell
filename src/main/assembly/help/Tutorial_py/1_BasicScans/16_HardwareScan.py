################################################################################################### 
# Use of HardwareScan 
################################################################################################### 

import ch.psi.pshell.crlogic.CrlogicPositioner as CrlogicPositioner
import ch.psi.pshell.crlogic.CrlogicSensor as CrlogicSensor

#sc1.stop()
#sc1.setOneShot()
#sc1.channels[0].setPreset(False)
#sc1.channels[0].setPresetValue(0)
#sc1.start()

config = {}
config["class"] = "ch.psi.pshell.crlogic.CrlogicScan"
config["prefix"] = "MTEST-HW3-CRL"
config["ioc"] = "MTEST-VME-HW3.psi.ch"
config["integrationTime"] = 0.01
config["additionalBacklash"] = 0.0

pos = CrlogicPositioner("CrlogicPositioner", "MTEST-HW3:MOT1", None);

sensors = [
    CrlogicSensor("Trigger0", "TRIGGER0"),
    CrlogicSensor("Trigger1", "TRIGGER1"),    
    CrlogicSensor("Scaler0", "SCALER0", True),    
    CrlogicSensor("Scaler1", "SCALER1", True),    
    CrlogicSensor("Timestamp", "TIMESTAMP"),   
    ]



pos.initialize()      
pos.move(0.0)
try:
    r1 = hscan(config, pos, sensors,0.0, 10.0, 0.1,1, False)
finally:    
    pos.close()     
    