/////////////////////////////////////////////////////////////////////////////////////////////////// 
//// Using Parallelization API to execute tasks concurrently
/////////////////////////////////////////////////////////////////////////////////////////////////// 


//Simple parallization
function task1(){
    m1.moveRel(1.0)
    return m1.getPosition()
}    

function task2(){
    m2.moveRel(1.0)
    return m1.getPosition()
}    

function task3(){
    return ai1.read()
}    

ret = parallelize(task1, task2, task3)
print (ret)


//Fork amd join
ret = fork(task1, task2, task3)
print (ai1.read())
ret = join(ret)
print (ret)



//Functions with parameters
function moveRelative(motor, step){
    motor.moveRel(step)
    return motor.getPosition()
}    

ret = parallelize([moveRelative,[m1,-1]], [moveRelative,[m2,-1]])
print (ret)


//Exception in parallel task is thrown back to script
try{
    parallelize([moveRelative,[m1,1]], [moveRelative,[m2,1]])
}catch (ex){
    print ("Ok, caught exception:")
    print(ex)
}    
    




