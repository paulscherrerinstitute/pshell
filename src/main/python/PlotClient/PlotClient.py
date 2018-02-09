import sys
import zmq
import json
import base64

class PlotClient:
    """
    A Python client to PShell plot server
    """
    def __init__(self, address = "localhost", port=7777, context = "0", timeout=None):        
        self.set_context(context)
        self.context = zmq.Context()      
        self.url = "tcp://" + address + (":%s" % port)
        self.debug = False        
        self.timeout = None if (timeout is None) else int(timeout * 1000)       
        self._create_socket()        

    def _create_socket(self):
        if self.debug: print ("Connecting to server: " + self.url)        
        self.socket = self.context.socket(zmq.REQ)
        if self.timeout is not None:
            self.socket.setsockopt(zmq.LINGER, 0)
            self.poller = zmq.Poller()
            self.poller.register(self.socket, zmq.POLLIN)                    
        self.socket.connect(self.url)

    def execute (self, cmd_name, cmd_data):
        if self.debug: print ("Sending: " +   cmd_name + "...")
        self.socket.send_string(cmd_name, flags = zmq.SNDMORE)        
        self.socket.send_string(json.dumps(cmd_data))
        if self.debug: print ("Receiving...")
         
        if not self.timeout is None:
            if not self.poller.poll(self.timeout):         
                self.socket.close()
                self._create_socket()
                raise TimeoutError("Communication timeout")
        rx = self.socket.recv_string()     
        if self.debug: print ("Done")
        response = json.loads(rx)   
        error = response["error"]
        if (error is not None) and (len(error)>0):
            raise Exception(error)
        return response["ret"]
    
    def close(self):
        if self.debug: print ("Closing...")
        self.socket.close() 
        self.context.term()

    def set_context(self, context):
        self.ctx = context

    def get_contexts(self):
        ret = self.execute("GetContexts", {})
        ret = json.loads(ret)
        return ret

    def set_context_attrs(self, quality=None, layout=None):
        """
            quality (str): Low, Medium, High or Maximum
            layout (str): Vertical, Horizontal, Grid
        """
        self.execute("SetContextAttrs", {"context":self.ctx, "quality":quality, "plotLayout":layout})

    def clear_contexts(self):
        self.execute("ClearContexts", {})

    def clear_plots(self):
        self.execute("ClearPlots",  {"context":self.ctx})

    def add_line_plot(self, title=None, style=None):
        """
            style = Normal (default), Step, Spline, ErrorX, ErrorY or ErrorXY
        """
        return self.execute("AddLinePlot",  {"context":self.ctx, "title":title, "style":style })

    def add_matrix_plot(self, title=None, style=None, colormap=None):
        """
            colormap = Grayscale, Red, Green, Blue, Inverted, Temperature (Default), Rainbow, Flame
            style = Normal (Default), Mesh, Image.
        """
        return self.execute("AddMatrixPlot",  {"context":self.ctx, "title":title, "style":style ,"colormap":colormap })

    def add_3d_plot(self, title=None, colormap=None):
        """
            colormap = Grayscale, Red, Green, Blue, Inverted, Temperature (Default), Rainbow, Flame
            style = Normal (Default), Mesh, Image.
        """
        return self.execute("Add3dPlot",  {"context":self.ctx, "title":title,"colormap":colormap})

    def add_time_plot(self, title=None, started=None, duration=None, markers=None):
        return self.execute("AddTimePlot",  {"context":self.ctx, "title":title, "started":started, "duration":duration, "markers":markers })

    def add_table(self, title=None):
        return self.execute("AddTable",  {"context":self.ctx, "title":title})
    
    def set_progress(self, progress):
        """
        0.0 < progress < 1.0 , or None for hiding it
        """
        self.execute("SetProgress", {"context":self.ctx, "progress":progress})

    def set_status(self, status):    
        self.execute("SetStatus", {"context":self.ctx, "status":status})
    
    def set_table_data(self, table, header, data):
        """
        header: list with the column titles
        data: list of lists holding the table contents
        """
        self.execute("SetTableData", {"table":table, "header":header, "data":data})

    def clear_plot(self, plot):
        self.execute("ClearPlot", {"plot":plot})

    def clear_series(self, series):
        self.execute("ClearSeries", {"series":series})

    def set_line_plot_attrs(self, plot, style):
        """
            style: see add_line_plot
        """
        self.execute("SetLinePlotAttrs", {"plot":plot, "style":style})

    def set_matrix_plot_attrs(self, plot, colormap):
        """
            colormap : see add_matrix_plot
        """
        self.execute("SetMatrixPlotAttrs",  {"plot":plot, "colormap":colormap})

    def set_time_plot_attrs(self, plot, started=None, duration=None, markers=None):
        """
            colormap : see add_matrix_plot
        """
        self.execute("SetTimePlotAttrs",  {"plot":plot, "started":started, "duration":duration, "markers":markers})        

    def set_plot_axis_attrs(self, axis, label=None, auto_range=None, min=None, max=None, inverted=None, logarithmic=None):
        self.execute("SetPlotAxisAttrs",  {"axis":axis, "label":label, "autoRange":auto_range, "min":min, "max":max, "inverted":inverted, "logarithmic":logarithmic})

    def add_text(self, plot, x, y, label, color):
        """
            color: string, color name or r,g,b -  e.g "red" or "255.0.0"
        """
        return self.execute("AddText",  {"plot":plot, "x":x, "y":y, "label":label, "color":color})

    def add_marker(self, axis, val, label, color):
        """
            color: see addText
        """
        return self.execute("AddMarker",  {"axis":axis, "val":val, "label":label, "color":color})

    def add_interval_marker(self, axis, start, end, label, color):
        """
            color: see addText
        """
        return self.execute("AddIntervalMarker",  {"axis":axis, "start":start, "end":end, "label":label, "color":color})
    
    def set_line_series_data(self, series, x, y, error=None, error_y=None):
        """
            x, y, error and error_y are arrays of floats. 
            Error arguments is disregarded if plot type is not error.
        """
        self.execute("SetLineSeriesData",  {"series":series, "x":x, "y":y, "error":error, "errorY":error_y})

    def set_matrix_series_data(self, series, data, x=None, y=None):
        """
            x, y: array of loats
            data: array of arrays
        """
        self.execute("SetMatrixSeriesData",  {"series":series, "x":x, "y":y, "data":data})

    def add_line_series(self, plot, name, color=None, axis_y=None, marker_size=None, line_width=None, max_count=None):
        return self.execute("AddLineSeries",  {"plot":plot, "name":name, "color":color, "axisY":axis_y, "markerSize":marker_size, "lineWidth":line_width, "maxCount":max_count})

    def add_matrix_series(self, plot, name, min_x=None, max_x=None, bins_x=None, min_y=None, max_y=None, bins_y=None):
        return self.execute("AddMatrixSeries",  {"plot":plot, "name":name, "minX": min_x, "maxX":max_x, "nX": bins_x, "minY":min_y, "maxY":max_y, "nY":bins_y})

    def add_3d_series(self, plot, name, min_x=None, max_x=None, bins_x=None, min_y=None, max_y=None, bins_y=None, min_z=None, max_z=None, bins_z=None):
        return self.execute("Add3dSeries",  {"plot":plot, "name":name, "minX": min_x, "maxX":max_x, "nX": bins_x, "minY":min_y, "maxY":max_y, "nY":bins_y, "minZ":min_z, "maxZ":max_z, "nZ":bins_z})            

    def add_time_series(self, plot, name, color=None, axis_y=None):
        return self.execute("AddTimeSeries",  {"plot":plot, "name":name, "color":color, "axisY":axis_y})

    def set_line_series_attrs(self, series, color=None, marker_size=None, line_width=None, max_count=None):
        self.execute("SetLineSeriesAttrs",  {"series":series, "color":color, "markerSize":marker_size, "lineWidth":line_width, "maxCount":max_count})

    def set_matrix_series_attrs(self, series, min_x=None, max_x=None, bins_x=None, min_y=None, max_y=None, bins_y=None):
        self.execute("SetMatrixSeriesAttrs",  {"series":series, "minX": min_x, "maxX":max_x, "nX": bins_x, "minY":min_y, "maxY":max_y, "nY":bins_y})

    def set_time_series_attrs(self, series, color):
        self.execute("SetTimeSeriesAttrs",  {"series":series, "color":color})


    def append_line_series_data(self, series, x, y, error=None):
        """
        x, y, error and error are floats. 
        Error argument is disregarded if plot type is not error.
        """
        self.execute("AppendLineSeriesData",  {"series":series, "x":x, "y":y, "error":error})

    def append_line_series_data_array(self, series, x, y, error=None):
        """
            x, y, error are arrays of floats. 
            Error argument is disregarded if plot type is not error.
        """
        self.execute("AppendLineSeriesDataArray",  {"series":series, "x":x, "y":y, "error":error})

    def append_matrix_series_data(self, series, x, y, z):
        """
            x, y, and z are floats. 
        """
        self.execute("AppendMatrixSeriesData",  {"series":series, "x":x, "y":y, "z":z})

    def append_matrix_series_data_array(self, series, x, y, z):
        """
            x, y, and z arrays of floats. 
        """
        self.execute("AppendMatrixSeriesDataArray",  {"series":series, "x":x, "y":y, "z":z})


    def append_3d_series_data(self, series, data):
        """
        """
        self.execute("Append3dSeriesData",  {"series":series, "data":data})


    def append_time_series_data(self, series, time = None, value = None):
        """
            x, y, and z are floats. 
        """        
        self.execute("AppendTimeSeriesData",  {"series":series, "time":time, "value":value})

    def remove_series(self, series):
        self.execute("RemoveSeries",  {"series":series})
    
    def remove_marker(self, marker):
        self.execute("RemoveMarker",  {"marker":marker})

    def remove_text(self, text):
        self.execute("RemoveText",  {"text":text})

     
    def get_plot_snapshot(self, plot, type="png"):
        """
            type (str): png, gif, bmp ot jpg
        """
        ret = self.execute("GetPlotSnapshot", {"plot":plot, "type":type})
        ret = json.loads(ret)
        ret = base64.b64decode(ret)
        return ret
    


if __name__ == "__main__":
  import time
  ps = PlotClient(context = "Py", timeout = 5.0)
  #while(True):    
  try:
    #Ckecking existing contexts
    print ("Contexts: " + str(ps.get_contexts()))

    #Initializing
    #ps.clear_contexts()
    ps.clear_plots()
    ps.set_context_attrs(quality=None, layout="Grid")
    ps.set_progress(0.2)
    ps.set_status("Processing")

    #Creating plotting widgets
    line_plot = ps.add_line_plot("Line Plot")
    matrix_plot = ps.add_matrix_plot("Matrix Plot", style = None, colormap = "Grayscale")
    line_plot_2 = ps.add_line_plot("Line Plot 2")
    matrix_plot_2 = ps.add_matrix_plot("Matrix Plot 2", style = "Mesh")
    line_plot_3 = ps.add_line_plot("Line Plot 3")    
    time_plot = ps.add_time_plot("Time Plot", True, 60000, True)
    table = ps.add_table("Table")
    plot_3d = ps.add_3d_plot("3d Plot", colormap = "Temperature")      

    #Setting plot attributes
    ps.set_line_plot_attrs(line_plot, style="ErrorY")
    ps.set_line_plot_attrs(line_plot_3, style="Spline")
    ps.set_matrix_plot_attrs(matrix_plot_2, colormap="Temperature")
    ps.set_plot_axis_attrs(line_plot+"/X", label="Domain", auto_range=None, min=None, max=None, inverted=None, logarithmic=None)
    ps.set_time_plot_attrs(time_plot, True, 1500, True)
    
    #Creating plot series
    ps.clear_plot(line_plot)
    line_series_1 = ps.add_line_series(line_plot, "Line Series 1" )
    matrix_series_1 =  ps.add_matrix_series(matrix_plot,  "Matrix Series 1")
    line_series_2 = ps.add_line_series(line_plot_2, "Line Series 2" )
    matrix_series_2 =  ps.add_matrix_series(matrix_plot_2,  "Matrix Series 2", min_x=0.0, max_x=100.0, bins_x=20, min_y= 10.0, max_y=20.0, bins_y=10)
    line_series_3 = ps.add_line_series(line_plot_3, "Line Series 3" )
    time_series_1 = ps.add_time_series( time_plot, "Series5", "green", 1)
    time_series_2 = ps.add_time_series(time_plot, "Series6", "blue", 2)
    series_3d = ps.add_3d_series(plot_3d, "Series8", None, None, None, None, None, None, None, None, None)

    #Setting series attributes
    ps.set_line_series_attrs(line_series_1, color=None, marker_size=3, line_width=None, max_count=None)
    ps.set_matrix_series_attrs(matrix_series_1, None, None, None, None, None, None)
    ps.set_time_series_attrs(time_series_1, color="orange")        

    #Adding plot objects
    marker = ps.add_marker(line_plot + "/X", 50.0, "Marker", "orange")
    text = ps.add_text(line_plot_2, 50.0, 15.0, "Test", "red")
    ps.set_table_data(table, ["Var", "Value"], [["a", 2], ["b",3]])
    #ps.remove_marker( marker)
    #ps.remove_text( text)

    #Manipulating series
    ps.clear_series(line_series_1)
    #ps.remove_series(line_series_1)

    x=[] 
    for i in range(100): x.append(float(i))
    y = [0.0] * 100; y[10] = 40.0; y[20] = 80.0
    ps.set_line_series_data(line_series_1, x, y, error = [2.0] * len(x))

    for i in range(len(x)):
        ps.append_line_series_data(line_series_2, x [i], y[i], None)

    ps.append_line_series_data_array(line_series_3, x, y, error=None)
    data = []
    for i in range(10): data.append([0.0,]*20)
    data[5][4] = 20; data[8][7] = 30
    ps.set_matrix_series_data(matrix_series_1,  data, None, None)

    xstep, ystep = 100.0/(20-1), (20.0-10.0)/(10-1) 
    for i in range(len(data)):
        for j in range(len(data[0])):
            ps.append_matrix_series_data(matrix_series_2, j* xstep, i*ystep+10, data[i][j])

    for i in  range(100):
        ps.append_time_series_data(time_series_1 , None, float(i))
        ps.append_time_series_data(time_series_2 , None, 10000.0-i)
        time.sleep(0.01)                

    #Reapeating the same thing to test append_matrix_series_data_array
    x =  [(j* xstep)  for j in range(len(data[0]))]
    for i in range(len(data)):
        y = [i*ystep,] * len(data[0])
        ps.append_matrix_series_data_array(matrix_series_2, x, y, data[i])

    for i in range(20):
        data[0][i] = i+1;
        ps.append_3d_series_data(series_3d, data)


    #Saving a plot snapshot
    #import time
    #time.sleep(0.5) # Sleeping because GUI update is not synchronous
    img = ps.get_plot_snapshot(matrix_plot_2)
    #f = open("Plot.png",  "wb")
    #f.write(img)
    #f.close()
    
    #Closing
    ps.set_status("Finished")
    ps.set_progress(None)
  except:
    import traceback, time
    traceback.print_exc(file=sys.stdout)
    time.sleep(1.0)
  ps.close()

