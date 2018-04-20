################################################################################################### 
# Demonstrate configuring scan plot options
###################################################################################################

#Scan plots can be configured inline in the scan command or with set_exec_pars function.
#The keywords are the same.
#If inline, the option is valid for the current scan only. 
#If set_exec_pars is used, it is valid for all scans in the sequence.


################################################################################################### 
# Inline configiuration
###################################################################################################


#Scanning with custom plotting options: Providing the visible plots and displaying wf1 as 
#a 1d plot at each scan point, instead of a matrix plot
a= lscan(ao1, (ai1,ai2,wf1), 0, 40, 10, 0.01, plot_list = (ai1, wf1), line_plots = (wf1,))    

#Next scan have plotting options defaults restored
a= lscan(ao1, (ai1,ai2,wf1), 0, 40, 10, 0.01)    


################################################################################################### 
# Configuring with set_exec_pars
###################################################################################################


#This optional preference sets the displayed plots
set_exec_pars(plot_list = (ai1, wf1))

#This optional preference displays wf1 as a 1d plot at each scan point, instead of a matrix plot
set_exec_pars(line_plots = (wf1,)) 

#This optional preference disables displaying scan contents (on plot, table and console)
#set_exec_pars( display = False) 

#Execute the scan: 200 steps, a1 from 0 to 40
a= lscan(ao1, (ai1,ai2,wf1), 0, 40, 10, 0.01)                    

#The custom plotting options continue active
ascan((m1,ao1), (ai1,ai2,wf1), (0.0,0.0), (2.0,1.0), (4,4))


