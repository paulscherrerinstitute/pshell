from pshell import PlotClient
import sys
import time

ps = PlotClient(context="Py", timeout=5.0)
# while(True):
try:
    # Ckecking existing contexts
    print("Contexts: " + str(ps.get_contexts()))

    # Initializing
    # ps.clear_contexts()
    ps.clear_plots()
    ps.set_context_attrs(quality=None, layout="Grid")
    ps.set_progress(0.2)
    ps.set_status("Processing")

    # Creating plotting widgets
    line_plot = ps.add_line_plot("Line Plot")
    matrix_plot = ps.add_matrix_plot("Matrix Plot", style=None, colormap="Grayscale")
    line_plot_2 = ps.add_line_plot("Line Plot 2")
    matrix_plot_2 = ps.add_matrix_plot("Matrix Plot 2", style="Mesh")
    line_plot_3 = ps.add_line_plot("Line Plot 3")
    time_plot = ps.add_time_plot("Time Plot", True, 60000, True)
    table = ps.add_table("Table")
    plot_3d = ps.add_3d_plot("3d Plot", colormap="Temperature")

    # Setting plot attributes
    ps.set_line_plot_attrs(line_plot, style="ErrorY")
    ps.set_line_plot_attrs(line_plot_3, style="Spline")
    ps.set_matrix_plot_attrs(matrix_plot_2, colormap="Temperature")
    ps.set_plot_axis_attrs(line_plot + "/X", label="Domain", auto_range=None, min=None, max=None, inverted=None,
                           logarithmic=None)
    ps.set_time_plot_attrs(time_plot, True, 1500, True)

    # Creating plot series
    ps.clear_plot(line_plot)
    line_series_1 = ps.add_line_series(line_plot, "Line Series 1")
    matrix_series_1 = ps.add_matrix_series(matrix_plot, "Matrix Series 1")
    line_series_2 = ps.add_line_series(line_plot_2, "Line Series 2")
    matrix_series_2 = ps.add_matrix_series(matrix_plot_2, "Matrix Series 2", min_x=0.0, max_x=100.0, bins_x=20,
                                           min_y=10.0, max_y=20.0, bins_y=10)
    line_series_3 = ps.add_line_series(line_plot_3, "Line Series 3")
    time_series_1 = ps.add_time_series(time_plot, "Series5", "green", 1)
    time_series_2 = ps.add_time_series(time_plot, "Series6", "blue", 2)
    series_3d = ps.add_3d_series(plot_3d, "Series8", None, None, None, None, None, None, None, None, None)

    # Setting series attributes
    ps.set_line_series_attrs(line_series_1, color=None, marker_size=3, line_width=None, max_count=None)
    ps.set_matrix_series_attrs(matrix_series_1, None, None, None, None, None, None)
    ps.set_time_series_attrs(time_series_1, color="orange")

    # Adding plot objects
    marker = ps.add_marker(line_plot + "/X", 50.0, "Marker", "orange")
    text = ps.add_text(line_plot_2, 50.0, 15.0, "Test", "red")
    ps.set_table_data(table, ["Var", "Value"], [["a", 2], ["b", 3]])
    # ps.remove_marker( marker)
    # ps.remove_text( text)

    # Manipulating series
    ps.clear_series(line_series_1)
    # ps.remove_series(line_series_1)

    x = []
    for i in range(100): x.append(float(i))
    y = [0.0] * 100;
    y[10] = 40.0;
    y[20] = 80.0
    ps.set_line_series_data(line_series_1, x, y, error=[2.0] * len(x))

    for i in range(len(x)):
        ps.append_line_series_data(line_series_2, x[i], y[i], None)

    ps.append_line_series_data_array(line_series_3, x, y, error=None)
    data = []
    for i in range(10): data.append([0.0, ] * 20)
    data[5][4] = 20;
    data[8][7] = 30
    ps.set_matrix_series_data(matrix_series_1, data, None, None)

    xstep, ystep = 100.0 / (20 - 1), (20.0 - 10.0) / (10 - 1)
    for i in range(len(data)):
        for j in range(len(data[0])):
            ps.append_matrix_series_data(matrix_series_2, j * xstep, i * ystep + 10, data[i][j])

    for i in range(100):
        ps.append_time_series_data(time_series_1, None, float(i))
        ps.append_time_series_data(time_series_2, None, 10000.0 - i)
        time.sleep(0.01)

        # Reapeating the same thing to test append_matrix_series_data_array
    x = [(j * xstep) for j in range(len(data[0]))]
    for i in range(len(data)):
        y = [i * ystep, ] * len(data[0])
        ps.append_matrix_series_data_array(matrix_series_2, x, y, data[i])

    for i in range(20):
        data[0][i] = i + 1;
        ps.append_3d_series_data(series_3d, data)

    # Saving a plot snapshot
    img = ps.get_plot_snapshot(line_plot)
    f = open("Plot.png",  "wb")
    f.write(img)
    f.close()

    # Closing
    ps.set_status("Finished")
    ps.set_progress(None)
except:
    import traceback, time
    traceback.print_exc(file=sys.stdout)
    time.sleep(1.0)
ps.close()
