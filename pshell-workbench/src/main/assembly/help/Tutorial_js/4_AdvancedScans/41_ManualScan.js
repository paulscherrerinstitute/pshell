///////////////////////////////////////////////////////////////////////////////////////////////////
// Manual scan: Manually setting positioners and reading back sensors, but still using 
// the standard data handling and plotting of built-in scans.
///////////////////////////////////////////////////////////////////////////////////////////////////


var MOTOR_RANGE = [0.0, 8.0]
var OUTPUT_SETPOINTS = [1.0, 2.0, 3.0]
var FIXED_X = true


writables_names = to_array([m1.getName(), ao1.getName()], 's')
readable_names = to_array([ai1.getName(), ai2.getName()], 's')
start = to_array( [ FIXED_X ? MOTOR_RANGE[0] : -1, OUTPUT_SETPOINTS[0]] , 'd')
stop =to_array([ FIXED_X ? MOTOR_RANGE[1] : -1, OUTPUT_SETPOINTS[OUTPUT_SETPOINTS.length-1]] , 'd')
steps = to_array([Math.round(MOTOR_RANGE[1]-MOTOR_RANGE[0]), OUTPUT_SETPOINTS.length-1] , 'i')

print (to_array(writables_names))
print (to_array(readable_names))
print (to_array(start))
print (to_array(stop))
print (to_array(steps))

scan = new ManualScan(writables_names, readable_names, start, stop,  steps, false)


//This option is to plot the foe each output value one 1D series, instead of all in a matrix plot
set_preference(Preference.PLOT_TYPES, {"ai1":1, "ai2":1})


scan.start()
m1.setSpeed(10.0)
for (var setpoint1  = MOTOR_RANGE[0]; setpoint1 <=MOTOR_RANGE[1]; setpoint1+=1.0){
    m1.move(setpoint1)
    for (var setpoint2 in OUTPUT_SETPOINTS){
        ao1.write(setpoint2)
        scan.append (to_array([setpoint1, setpoint2], 'd'), to_array([m1.read(), ao1.read()], 'd'), to_array([ai1.read(), ai2.read()], 'd'))
    }
}

scan.end()
