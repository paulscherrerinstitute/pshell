///////////////////////////////////////////////////////////////////////////////////////////////////
//Resampling a scan record if a fail condition is met.
/////////////////////////////////////////////////////////////////////////////////////////////////// 


index=0 

function AfterReadout(rec){
    print (rec.getIndex() + " - " + to_array(rec.values))    
    //Only accept records if ai2 is positive
    if (ai2.take() < 0){
        sleep(1.0)
        print("Invalidating")
        rec.invalidate()
    }
}        
            

a= lscan(m1, [ai1,ai2], [0,], [0.4,], 20, 0.1, relative = undefined, passes = undefined, zigzag = undefined, before_read = undefined, after_read=AfterReadout)