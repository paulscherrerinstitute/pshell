################################################################################################### 
# Direct creation of plots
################################################################################################### 

data_1d = [10.0, 20.0, 30.0, 40.0, 50.0]
data_2d = [ data_1d, data_1d, data_1d, data_1d, data_1d]
data_3d = [ data_2d, data_2d , data_2d, data_2d, data_2d]
data_x = [1.0, 2.0, 3.0, 4.0, 5.0]
data_y = [2.0, 4.0, 6.0, 8.0, 10.0]

#1d-plot with optional xdata
plot(data_1d, title = "1d", xdata = data_x)

#2d-plot with optional xdata and ydata
plot(data_2d, title = "2d", xdata = data_x, ydata = data_y)

#3d-plot
plot(data_3d, title = "3d")
 
#3 plots in the save panel
plot([data_1d, data_2d, data_3d], ["1d", "2d", "3d"])

