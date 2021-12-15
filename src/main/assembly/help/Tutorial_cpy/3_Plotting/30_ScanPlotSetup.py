################################################################################################### 
# Demonstrate configuring scan plot options
###################################################################################################

#Scan plots can be configured inline in the scan command or with set_exec_pars function.
#The keywords are the same.
#If inline, the option is valid for the current scan only. 
#If set_exec_pars is used, it is valid for all scans in the sequence.


################################################################################################### 
# Inline configuration
###################################################################################################

 
#This optional preference sets the displayed plots for the scan command
a= lscan(ao1, (ai1,ai2,wf1), 0, 40, 10, 0.01, plot_list = ("ai1", "wf1"))      

#This optional preference disables displaying scan contents (on plot, table and console)
a= lscan(ao1, (ai1,ai2,wf1), 0, 40, 10, 0.01, display = False)                    
print (a.getReadable("ai1"))


################################################################################################### 
# Configuring with set_exec_pars
###################################################################################################

#Scanning with custom plotting options: Providing the visible plots and displaying wf1 as 
#a 1d plot at each scan point, instead of a matrix plot
set_exec_pars(line_plots = ("wf1",)) 
#Next scan have plotting options defaults restored
a= lscan(ao1, (ai1,ai2,wf1), 0, 40, 10, 0.1)    


#This optional preference sets the displayed plots for the remaining of the script
set_exec_pars(plot_list = ("ai1", "wf1"))
a= lscan(ao1, (ai1,ai2,wf1), 0, 40, 10, 0.01)   




