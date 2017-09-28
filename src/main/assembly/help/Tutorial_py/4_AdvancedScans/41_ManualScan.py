################################################################################################### 
#Manual scan: Manually setting positioners and reading back sensors, but still using 
#the standard data handling and plotting of built-in scans.
################################################################################################### 


MOTOR_RANGE = (0.0, 8.0)
OUTPUT_SETPOINTS = (1.0, 2.0, 3.0)
FIXED_X = True


writables_names = [m1.getName(), ao1.getName()]
readable_names = [ai1.getName(), ai2.getName()]
start =  [ MOTOR_RANGE[0] if FIXED_X else -1, OUTPUT_SETPOINTS[0]] 
stop = [ MOTOR_RANGE[1] if FIXED_X else -1, OUTPUT_SETPOINTS[-1]]
steps = [int(MOTOR_RANGE[1]-MOTOR_RANGE[0]), len(OUTPUT_SETPOINTS)-1]

scan = ManualScan(writables_names, readable_names ,start, stop, steps)


#This option is to plot the foe each output value one 1D series, instead of all in a matrix plot
setup_plotting(line_plots = (ai1, ai2))


scan.start()
m1.setSpeed(10.0)
for setpoint1 in frange(MOTOR_RANGE[0], MOTOR_RANGE[1], 1.0, True):
    m1.move(setpoint1)
    for setpoint2 in OUTPUT_SETPOINTS:
        ao1.write(setpoint2)
        scan.append ([setpoint1, setpoint2], [m1.read(), ao1.read()], [ai1.read(), ai2.read()])


scan.end()
