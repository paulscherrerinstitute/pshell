################################################################################################### 
# Multiple Gaussians peak search with mathutils.py
################################################################################################### 


from mathutils import estimate_peak_indexes, fit_gaussians, create_fit_point_list

start = 0
end = 50
step_size = 0.2

result= lscan(ao1,ai1,start,end,[step_size,])

readable = result.getReadable(0)
positions = result.getPositions(0)

threshold = (min(readable) + max(readable))/2
min_peak_distance = 5.0

peaks = estimate_peak_indexes(readable, positions, threshold, min_peak_distance)
print "Peak indexes: " + str(peaks)
print "Peak x: " + str(map(lambda x:positions[x], peaks))
print "Peak y: " + str(map(lambda x:readable[x], peaks))

gaussians = fit_gaussians(readable, positions, peaks)

plots = plot([readable],["sin"],[positions], title="Data" )
for i in range(len(peaks)):
    peak = peaks[i]
    (normalization, mean_val, sigma) = gaussians[i]
    if abs(mean_val - positions[peak]) < min_peak_distance:
        print "Peak -> " +  str(mean_val)
        plots[0].addMarker(mean_val, None, "N="+str(round(normalization,2)), Color(210,0,0))
    else:
        print "Invalid gaussian fit: " +  str(mean_val)
