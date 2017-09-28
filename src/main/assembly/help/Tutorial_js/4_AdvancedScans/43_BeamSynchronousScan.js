///////////////////////////////////////////////////////////////////////////////////////////////////
// Scans with beam synchronous streams
/////////////////////////////////////////////////////////////////////////////////////////////////// 

//Creating a stream, assuming a provided  named "dispatcher".
st1 = new Stream("st1", dispatcher)

//Adding channels to that stream
s1=st1.addScalar("Int8Scalar", "Int8Scalar", 10, 0)
s2=st1.addScalar("Float64Scalar", "Float64Scalar", 10, 0)
w1=st1.addWaveform("Int32Waveform", "Int32Waveform", 10, 0)
mt1=st1.addMatrix("Int16Waveform", "Int16Waveform", 10, 0, 64, 32)
st1.initialize()

try{
    //The stream can be used on any conventional scan. The next stream value is sampled.
    //Readable values belong to same pulse id.
    tscan (st1.getReadables(), 10 , 0.2)

    //The bscan command performs samples every stream element 
    //Readable values belong to same pulse id.
    bscan (st1, 10)    //Sampling 10 elements

    //An individual stream channel can be used in a conventional scan, but in this case the stream
    //must be explicitly started, and there is no guarantee the values belong to the same PulseID 
    //(likely they are since as only cached values are taken).
    st1.start(true)
    tscan ([s1,s2], 10 , 0.2)

    //If waveform individual stream channel is used, and no size is provided in constructor/config,
    //then it must be read previously to the scan to update the size value.
    st1.start(true)
    w1.update()
    tscan ([s1,w1,mt1], 10 , 0.2)
} finally{    
    st1.close()    
}    
