################################################################################################### 
#Data Manipulation: Using the data access API to generate and retrieve data
################################################################################################### 


#Creating a 1D dataset from an array
path="group/data1"
data1d = [1.0, 2.0, 3.0, 4.0, 5.0]
save_dataset(path, data1d)
#Reading ii back
read =load_data(path)
print read.tolist()
assert data1d==read.tolist()
plot(read)

#Creating a 2D dataset from an array with some attributes
data2d = [ [1.0, 2.0, 3.0, 4.0, 5.0],  [2.0, 3.0, 4.0, 5.0, 6.0, ],  [3.0, 4.0, 5.0, 6.0, 7.0]]
path="group/data2"
save_dataset(path, data2d)
set_attribute(path, "AttrString", "Value")
set_attribute(path, "AttrInteger", 1)
set_attribute(path, "AttrDouble", 2.0)
set_attribute(path, "AttrBoolean", True)
#Reading it back
read =load_data(path)
print read.tolist()
plot(read)

#Creating a 3D dataset from an array
data3d = [ [ [1,2,3,4,5],  [2,3,4,5,6],  [3,4,5,6,7]],   [ [3,2,3,4,5],  [4,3,4,5,6],  [5,4,5,6,7]]]
path="group/data3"
save_dataset(path, data3d)
#Reading it back
read =load_data(path,0)
print read.tolist()
read =load_data(path,1)
print read.tolist()

#Creating a INT dataset adding elements one by one
path = "group/data4"
create_dataset(path, 'i')
for i in range(10):
    append_dataset(path,i)    


#Creating a 2D data FLOAT dataset adding lines one by one
path = "group/data5"
create_dataset(path, 'd', False, (0,0))
for row in data2d:
    append_dataset(path, row)    


#Creating a Table (compund type)
path = "group/data6"
names = ["a", "b", "c", "d"]
types = ["d", "d", "d", "[d"]
lenghts = [0,0,0,5]
table = [    [1,2,3,[0,1,2,3,4]],  
            [2,3,4,[3,4,5,6,7]],  
            [3,4,5,[6,7,8,9,4]]    ]
create_table(path, names, types, lenghts)
for row in table:
    append_table(path, row)   
flush_data() 
#Read it back
read =load_data(path)
print read


#Writing scalars (datasets with rank 0)
save_dataset("group/val1", 1)
save_dataset("group/val2", 3.14)
save_dataset("group/val3", "test")
print load_data("group/val1")
print load_data("group/val2")
print load_data("group/val3")


