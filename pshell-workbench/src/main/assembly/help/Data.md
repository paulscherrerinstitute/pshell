# Data


PShell aims to abstract data saving from DAQ scripts. Scripts should, ideally, be unaware of data saving, or, at least, unaffected by the choice of data formats.
 
Scans save data automatically by default, and all data created by a _run_ (the execution of a DAQ script) is wrapped in a single HDF5 file.

But other data formats are supported and new ones can be implemented,  with the assumption they are hierarchical.


The automatic data creation by scans is governed by:

- Scan options: how to translate scan records into to data files data.

- A template for the name of the path to the data.

- A provider of a hierarchal file storage: the __file format__.

- An object implementing how scan data and metadata are organised inside the file storage: the __data layout__.


The global configuration of these parameters is done in the workbench with __Menu File -> Data Setup...__ .

These settings can be overridden:
- In the scope of a script with the command __set_exec_pars__.
- For a single scan with arguments of the scan method (same arguments as __set_exec_pars__).


A number of file formats and data layouts are available, but customized ones can be implemented in extensions.



## File Format

| Format         | Description |
| :------------------ | :---------- |
| h5                  | HDF5 file. Can store any dimensionality.|
| txt                 | Data is stored in text files. A folder structure is created to mimic HDF5 hierarchy. Attributes are stored in hidden files or in data file headers. Each file can store up to three dimensional data.|
| csv                 | Similar to __txt__ but in a simpler format that allows files to be diretly open by spreadsheet applications. Each file can store up to two dimensional data.|
| fda                 | Text file format used by FDA application.| 


## Data Layout


| Layout         | Description |
| :------------------ | :---------- |
| default             | Creates one group per scan, and, in each scan group, one dataset per device (writable or readable) with the dimensionality of the device plus one. For each scan record one new value is added in each device dataset.|
| table               | The data of a scan is is stored as single dataset - a table or composite type in HDF5.
| sf                  | SwissFEL specific format - recording pulse id and machine information.|
| fda                 | Layout used by FDA application - similar to __table__.|
| nx                  | Preliminary implementation of a Nexus file format.|


The tools for data visualization work identically, whatever are the file format and data layout.

Scripts can choose also to disable automatic data saving, and save data manually. 
There are commands is the scripting API to directly handling data, which are independent of the file format.