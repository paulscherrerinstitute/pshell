///////////////////////////////////////////////////////////////////////////////////////////////////
// Multiple Gaussians peak search with mathutils.py
/////////////////////////////////////////////////////////////////////////////////////////////////// 


run("mathutils")

start = 0
end = 50
step_size = 0.2

result= lscan(ao1,ai1,start,end,[step_size,])

readable = result.getReadable(0)
positions = result.getPositions(0)

min = Math.min.apply(null, readable) 
max = Math.max.apply(null, readable) 

threshold = (min + max)/2
min_peak_distance = 5.0

peaks = estimate_peak_indexes(readable, positions, threshold, min_peak_distance)
print ("Peak indexes: " + peaks)
print ("Peak x: " + peaks.map(function(x) {return positions[x]}))
print ("Peak y: " + peaks.map(function(x) {return readable[x]}))

gaussians = fit_gaussians(readable, positions, peaks)

plots = plot([readable],["sin"],[positions], undefined, title="Data" )
for (var i=0; i< peaks.length; i++){
    peak = peaks[i]    
    pars_gaussian = gaussians[i]
    normalization = pars_gaussian[0]
    mean_val = pars_gaussian[1]
    sigma = pars_gaussian [2]    
    if (Math.abs(mean_val - positions[peak]) < min_peak_distance){
        print ("Peak -> " + mean_val)
        plots[0].addMarker(mean_val, null, "N="+ Math.round(normalization,2), new Color(210,0,0))
    }else {
        print ("Invalid gaussian fit: " + mean_val)
    }
}