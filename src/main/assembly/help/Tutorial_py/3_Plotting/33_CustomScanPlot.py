################################################################################################### 
# Custom plot: Example of creating a 1D plot for a 2D scan where each scanned row is a series
################################################################################################### 


#Setting the 1d preference would create in the place of the matrix plot, a 1d plot where 
#each scanned column is a series
#set_exec_pars(line_plots = (ai1,))

p = plot(None, title="1d Plot")[0]
def AfterReadout(record, scan):
    if record.setpoints[1] == scan.getStart()[1]:
        p.addSeries(LinePlotSeries(str(record.positions[0])))
    p.getSeries(p.numberOfSeries-1).appendData(record.positions[1], record.values[0])    
        

ascan((ao1,ao2), (ai1), (0,10), (20,30), (20,20), 0.1, after_read=AfterReadout)