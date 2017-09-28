///////////////////////////////////////////////////////////////////////////////////////////////////
// Demonstrate the use of fft function in mathutils.py
///////////////////////////////////////////////////////////////////////////////////////////////////

run("mathutils")



//The signal is composed by 3 sinusoids (100Hz, 200Hz, 400Hz) and a background noise (a sin waveform is A.sin(2.pi.f.t))
function signal_generator(t){
    return  Math.sin(100*2*Math.PI*t)  + 0.5 * Math.sin(200*2*Math.PI*t) + 0.25 * Math.sin(400*2*Math.PI*t) + 0.5* Math.random()
}    

sampling_frequency = 1024.0         
number_of_samples = 1024


time_vector =  range(0, number_of_samples, 1).map(function(x) {return x / sampling_frequency})
signal =  time_vector.map(function(x) {return signal_generator(x)})
tranform = fft(signal)

two_side_spectrum =  get_modulus(tranform).map(function(x) {return x / number_of_samples})
spectrum = two_side_spectrum.slice(1,two_side_spectrum.length/2 + 1).map(function(x) {return (x * x)})
spectrum.splice(0,0,two_side_spectrum[0])

number_of_samples = tranform.length    // Signal may have been padded to next power of two
freq_vector = range(0, spectrum.length , 1).map(function(x) {return x * sampling_frequency / parseFloat(number_of_samples)})

plot([signal,spectrum], ["signal", "spectrum"],[time_vector, freq_vector])
    