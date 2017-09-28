################################################################################################### 
# Setting script parameters and return value
################################################################################################### 


#Providing a map of global variables
#run ("52_ParametersAndReturn", {"start":10.0, "end":50.0, "step":40})

#Providing the locals dictionary 
# The parameters are not set as globals, and nor script definitions
#run ("52_ParametersAndReturn", locals={"start":10.0, "end":50.0, "step":40})

#Setting sys.argv:
#run ("52_ParametersAndReturn", [10.0, 50.0, 40])

#In this case the parameters would be parsed as:
#start = sys.argv[0]
#end = sys.argv[1]
#step = sys.argv[2]


a= lscan(ao1, ai1, start, end, step, 0.1)
a.getReadable(0)
set_return(a.getReadable(0))