################################################################################################### 
#Resampling a scan record if a fail condition is met.
###################################################################################################


index=0 

def after_read(rec):
    if rec[ai2] < 0:
        time.sleep(1.0)
        rec.invalidate()


a= lscan((m1), (ai1,ai2), (0,), (0.4,), 20, 0.1, after_read=after_read)