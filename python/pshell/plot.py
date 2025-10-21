import sys
import zmq
import json
import base64
import threading


class PlotClient:
    """
    A Python client to PShell plot server
    """

    def __init__(self, address="localhost", port=7777, context=None, timeout=None):
        self.set_context(context)
        self.context = zmq.Context()
        self.url = "tcp://" + address + (":%s" % port)
        self.debug = False
        self.timeout = None if (timeout is None) else int(timeout * 1000)
        self._create_socket()
        self.lock = threading.Lock()

    def _create_socket(self):
        if self.debug: print("Connecting to server: " + self.url)
        self.socket = self.context.socket(zmq.REQ)
        if self.timeout is not None:
            self.socket.setsockopt(zmq.LINGER, 0)
            self.poller = zmq.Poller()
            self.poller.register(self.socket, zmq.POLLIN)
        self.socket.connect(self.url)

    def execute(self, cmd_name, cmd_data):
        with self.lock:
            if self.debug: print("Sending " + cmd_name + "...")
            json_str = json.dumps(cmd_data)
            self.socket.send_string(cmd_name, flags=zmq.SNDMORE)
            self.socket.send_string(json_str)
            if self.debug: print("Receiving...")

            if not self.timeout is None:
                if not self.poller.poll(self.timeout):
                    self.socket.close()
                    self._create_socket()
                    raise TimeoutError("Communication timeout")
            rx = self.socket.recv_string()
            if self.debug: print("Done")
            response = json.loads(rx)
            error = response["error"]
            if (error is not None) and (len(error) > 0):
                raise Exception(error)
            return response["ret"]
        
    def close(self):
        if self.debug: print("Closing...")
        self.socket.close()
        self.context.term()

    def set_context(self, context):
        self.ctx = context if context else "0"

    def get_contexts(self):
        ret = self.execute("GetContexts", {})
        ret = json.loads(ret)
        return ret

    def set_context_attrs(self, quality=None, layout=None):
        """
            quality (str): Low, Medium, High or Maximum
            layout (str): Vertical, Horizontal, Grid
        """
        self.execute("SetContextAttrs", {"context": self.ctx, "quality": quality, "plotLayout": layout})

    def clear_contexts(self):
        self.execute("ClearContexts", {})

    def clear_plots(self):
        self.execute("ClearPlots", {"context": self.ctx})

    def add_line_plot(self, title=None, style=None):
        """
            style = Normal (default), Step, Spline, ErrorX, ErrorY or ErrorXY
        """
        return self.execute("AddLinePlot", {"context": self.ctx, "title": title, "style": style})

    def add_matrix_plot(self, title=None, style=None, colormap=None):
        """
            colormap = Grayscale, Red, Green, Blue, Inverted, Temperature (Default), Rainbow, Flame
            style = Normal (Default), Mesh, Image.
        """
        return self.execute("AddMatrixPlot",
                            {"context": self.ctx, "title": title, "style": style, "colormap": colormap})

    def add_3d_plot(self, title=None, colormap=None):
        """
            colormap = Grayscale, Red, Green, Blue, Inverted, Temperature (Default), Rainbow, Flame
            style = Normal (Default), Mesh, Image.
        """
        return self.execute("Add3dPlot", {"context": self.ctx, "title": title, "colormap": colormap})

    def add_time_plot(self, title=None, started=None, duration=None, markers=None):
        return self.execute("AddTimePlot",
                            {"context": self.ctx, "title": title, "started": started, "duration": duration,
                             "markers": markers})

    def add_table(self, title=None):
        return self.execute("AddTable", {"context": self.ctx, "title": title})

    def set_progress(self, progress):
        """
        0.0 < progress < 1.0 , or None for hiding it
        """
        self.execute("SetProgress", {"context": self.ctx, "progress": progress})

    def set_status(self, status):
        self.execute("SetStatus", {"context": self.ctx, "status": status})

    def set_table_data(self, table, header, data):
        """
        header: list with the column titles
        data: list of lists holding the table contents
        """
        self.execute("SetTableData", {"table": table, "header": header, "data": data})

    def clear_plot(self, plot):
        self.execute("ClearPlot", {"plot": plot})

    def clear_series(self, series):
        self.execute("ClearSeries", {"series": series})

    def set_line_plot_attrs(self, plot, style):
        """
            style: see add_line_plot
        """
        self.execute("SetLinePlotAttrs", {"plot": plot, "style": style})

    def set_matrix_plot_attrs(self, plot, colormap):
        """
            colormap : see add_matrix_plot
        """
        self.execute("SetMatrixPlotAttrs", {"plot": plot, "colormap": colormap})

    def set_time_plot_attrs(self, plot, started=None, duration=None, markers=None):
        """
            colormap : see add_matrix_plot
        """
        self.execute("SetTimePlotAttrs", {"plot": plot, "started": started, "duration": duration, "markers": markers})

    def set_plot_axis_attrs(self, axis, label=None, auto_range=None, min=None, max=None, inverted=None,
                            logarithmic=None):
        self.execute("SetPlotAxisAttrs", {"axis": axis, "label": label, "autoRange": auto_range, "min": min, "max": max,
                                          "inverted": inverted, "logarithmic": logarithmic})

    def add_text(self, plot, x, y, label, color):
        """
            color: string, color name or r,g,b -  e.g "red" or "255.0.0"
        """
        return self.execute("AddText", {"plot": plot, "x": x, "y": y, "label": label, "color": color})

    def add_marker(self, axis, val, label, color):
        """
            color: see addText
        """
        return self.execute("AddMarker", {"axis": axis, "val": val, "label": label, "color": color})

    def add_interval_marker(self, axis, start, end, label, color):
        """
            color: see addText
        """
        return self.execute("AddIntervalMarker",
                            {"axis": axis, "start": start, "end": end, "label": label, "color": color})

    def set_line_series_data(self, series, x, y, error=None, error_y=None):
        """
            x, y, error and error_y are arrays of floats.
            Error arguments is disregarded if plot type is not error.
        """
        self.execute("SetLineSeriesData", {"series": series, "x": x, "y": y, "error": error, "errorY": error_y})

    def set_matrix_series_data(self, series, data, x=None, y=None):
        """
            x, y: array of loats
            data: array of arrays
        """
        self.execute("SetMatrixSeriesData", {"series": series, "x": x, "y": y, "data": data})

    def add_line_series(self, plot, name, color=None, axis_y=None, marker_size=None, line_width=None, max_count=None):
        return self.execute("AddLineSeries",
                            {"plot": plot, "name": name, "color": color, "axisY": axis_y, "markerSize": marker_size,
                             "lineWidth": line_width, "maxCount": max_count})

    def add_matrix_series(self, plot, name, min_x=None, max_x=None, bins_x=None, min_y=None, max_y=None, bins_y=None):
        return self.execute("AddMatrixSeries",
                            {"plot": plot, "name": name, "minX": min_x, "maxX": max_x, "nX": bins_x, "minY": min_y,
                             "maxY": max_y, "nY": bins_y})

    def add_3d_series(self, plot, name, min_x=None, max_x=None, bins_x=None, min_y=None, max_y=None, bins_y=None,
                      min_z=None, max_z=None, bins_z=None):
        return self.execute("Add3dSeries",
                            {"plot": plot, "name": name, "minX": min_x, "maxX": max_x, "nX": bins_x, "minY": min_y,
                             "maxY": max_y, "nY": bins_y, "minZ": min_z, "maxZ": max_z, "nZ": bins_z})

    def add_time_series(self, plot, name, color=None, axis_y=None):
        return self.execute("AddTimeSeries", {"plot": plot, "name": name, "color": color, "axisY": axis_y})

    def set_line_series_attrs(self, series, color=None, marker_size=None, line_width=None, max_count=None):
        self.execute("SetLineSeriesAttrs",
                     {"series": series, "color": color, "markerSize": marker_size, "lineWidth": line_width,
                      "maxCount": max_count})

    def set_matrix_series_attrs(self, series, min_x=None, max_x=None, bins_x=None, min_y=None, max_y=None, bins_y=None):
        self.execute("SetMatrixSeriesAttrs",
                     {"series": series, "minX": min_x, "maxX": max_x, "nX": bins_x, "minY": min_y, "maxY": max_y,
                      "nY": bins_y})

    def set_time_series_attrs(self, series, color):
        self.execute("SetTimeSeriesAttrs", {"series": series, "color": color})

    def append_line_series_data(self, series, x, y, error=None):
        """
        x, y, error and error are floats.
        Error argument is disregarded if plot type is not error.
        """
        self.execute("AppendLineSeriesData", {"series": series, "x": x, "y": y, "error": error})

    def append_line_series_data_array(self, series, x, y, error=None):
        """
            x, y, error are arrays of floats.
            Error argument is disregarded if plot type is not error.
        """
        self.execute("AppendLineSeriesDataArray", {"series": series, "x": x, "y": y, "error": error})

    def append_matrix_series_data(self, series, x, y, z):
        """
            x, y, and z are floats.
        """
        self.execute("AppendMatrixSeriesData", {"series": series, "x": x, "y": y, "z": z})

    def append_matrix_series_data_array(self, series, x, y, z):
        """
            x, y, and z arrays of floats.
        """
        self.execute("AppendMatrixSeriesDataArray", {"series": series, "x": x, "y": y, "z": z})

    def append_3d_series_data(self, series, data):
        """
        """
        self.execute("Append3dSeriesData", {"series": series, "data": data})

    def append_time_series_data(self, series, time=None, value=None):
        """
            x, y, and z are floats.
        """
        self.execute("AppendTimeSeriesData", {"series": series, "time": time, "value": value})

    def remove_series(self, series):
        self.execute("RemoveSeries", {"series": series})

    def remove_marker(self, marker):
        self.execute("RemoveMarker", {"marker": marker})

    def remove_text(self, text):
        self.execute("RemoveText", {"text": text})

    def get_plot_snapshot(self, plot, type="png", width=None, height=None):
        """
            type (str): png, gif, bmp ot jpg
        """
        ret = self.execute("GetPlotSnapshot", {"plot": plot, "type": type, "width": width, "height": height})
        ret = json.loads(ret)
        ret = base64.b64decode(ret)
        return ret
