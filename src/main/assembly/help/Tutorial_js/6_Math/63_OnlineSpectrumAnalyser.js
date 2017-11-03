///////////////////////////////////////////////////////////////////////////////////////////////////
// Create an online spectrum analyser uysing mathutils.py
///////////////////////////////////////////////////////////////////////////////////////////////////


run("mathutils")

function plot_spectrum(signal, sampling_freq){
	if (!is_defined(plots))    plots = null
    signal = to_array(signal) 
    number_of_samples = signal.length
    time_vector = range(0, number_of_samples, 1).map(function(x) {return x / sampling_freq})  
    tranform = fft(signal)
    two_side_spectrum =  get_modulus(tranform).map(function(x) {return x / number_of_samples})    
    spectrum = two_side_spectrum.slice(1,two_side_spectrum.length/2 + 1).map(function(x) {return x * x})
    spectrum.splice(0,0,two_side_spectrum[0])    
    number_of_samples = tranform.length    // Signal may have been padded to next power of two
    freq_vector = range(0, spectrum.length , 1).map(function(x) {return x * sampling_freq / parseFloat(number_of_samples)})  
    if (plots == null){
        plots = plot([signal,spectrum], ["signal", "spectrum"],[time_vector, freq_vector], undefined, title = "Spectrum")
        plots[0].getAxis(AxisId.Y).setRange(-1.5,2.5)
        plots[1].getAxis(AxisId.Y).setRange(0.0,1.1)
    } else {
        plots[0].getSeries(0).setData(time_vector,signal)
        plots[1].getSeries(0).setData(freq_vector,spectrum)
    }
    return plots
}    

function get_sample(samples, sampling_freq){
	return range(0,samples,1).map(function(t) {return Math.sin(200*2*Math.PI*t/sampling_freq) + Math.random()})
    //Simple example sampling a device:
    //set_exec_pars({"persist":false, "plot_disabled":true, "table_disabled":true})
    //return tscan(ai1, samples, 1/sampling_freq).getReadable(0)
}    

plots = null
samples = 1024
sampling_freq = 1024.0

while(true){
    signal = get_sample(samples, sampling_freq)
    plots = plot_spectrum(signal, sampling_freq, plots)
    sleep(0.1)
    if (plots[0].displayable == false){ 
        break
    }
}        
