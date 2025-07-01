################################################################################################### 
# Demonstrate adding attributs to scan groups and datasets.
###################################################################################################


#Execute the scan: 200 steps, a1 from 0 to 40
a= lscan(ao1, (ai1,ai2), 0, 40, 10, 0.01)                    


#Setting attributes to the scan group
path = get_exec_pars().scanPath
set_attribute(path, "AttrString", "Value")
set_attribute(path, "AttrInteger", 1)
set_attribute(path, "AttrDouble", 2.0)
set_attribute(path, "AttrBoolean", True)

#Setting attributes to the scan datasets
#Only valid for default layout
set_attribute(path + ao1.name, "AttrInteger", 2)
set_attribute(path + ai1.name, "AttrInteger", 3)
set_attribute(path + ai2.name, "AttrInteger", 4)


