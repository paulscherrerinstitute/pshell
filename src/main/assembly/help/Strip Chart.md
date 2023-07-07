# Strip Chart

Strip Chart is a tool for displaying streaming data in time history plots.
- A new Strip Chart window is created in the workbench with menu "Devices - Strip Chart".
- Strip Chart can also be executed detached from the workbench using the command line option "-strp".


<br/>

## Startup Options

There are some startup options specific for Strip Chart when detached:

<br/>

| Option                    | Description |
| :------------------------ | :---------- |
| -f=<...>                  | Open a StripChart configuration file (.scd).|
| -config=<...>             | JSON configuration string (as in .scd file) or list of channel names.|
| -start                    | Start the data displaying immediately.|
| -v                        | Create a StripChart server.|
| -attach                   | Shared mode: try connecting to existing server, or create one if not available.|
| -background_color=<...>   | Set default plot background color.|
| -grid_color=<...>         | Set default plot grid color.|
| -tick_label_font=name:size| Set font for time plot tick labels.|
| -alarm_interval=<...>     | Set the alarm timer interval (default 1000ms).|
| -alarm_file=<...>         | Set alarm sound file (default use system beep).|


Note: The general startup options also apply, see document __Startup Options__.


<br/>

## Config Tab

The Strip Chart window is composed by two tabs: __Config__ and __Plot__.
The first tab allows configuring the data and the plotting. 
The configuration can be saved and loaded with "Save" and "Open" buttons.

The tab is divided in the the following panels:


### Series

The __Series__ table holds the definition of the streamed data. 
Each series must be assigned to a Y axis of one of the five available charts.
After the plotting starts, the series properties cannot change dynamically, with the exception of "Color" and "Alarm".

<br/>

| Column       | Description |
| :------------| :---------- |
| Enable       | Enables/Disables plotting.|
| Name         | The name of the data source, and type-specific optional parameters (see below).|
| Type         | The type of data source: EPICS channel, BSREAD stream, Device or CamServer stream.|
| Plot         | Plot number for this series (1-5).|
| YAxis        | Y axis for this series (1-2).|
| Color        | Color for the series.|
| Alarm        | Enables alarm for the series, opening a window for setting alarm limits.|

<br/>

The optional parameters on the __Name__ column are dependent on the type of data. The value is formed by:
- A mandatory name.
- An optional list of values corresponding to type-specific parameters.
- An optional __alias__, surrounded by the delimiters: < >. If defined, will will be used in the plot legend.

<br/>

| Data Type               | Name and Optional Parameters |
| :-----------------------| :---------- |
| Channel                 | __ChannelName [Polling(ms)=-1 Precision=-1] \<Alias\>__|
|                         |     CamServer uses EPICS channel monitors by default. If the data volume is to big for the the plotting, it can be lowered by specifying the __Polling__ parameter.|
|                         |     Instead of appending to the chart on the monitor event, the channel will be read in the given interval (in milliseconds.)|
|                         |     |
| Stream                  | __ChannelName [Modulo=1 Offset=0] \<Alias\>__|
|                         |     |
| Device                  | __DeviceName \<Alias\>__|
|                         |     Where DeviceName can be:|
|                         |     - A global device.|
|                         |     - A inline device string, as defined in __Devices__ document, section __Inline devices__.|
|                         |     - A statement, if the interpreter is enabled (e.g. for accessing a sub-device).|
|                         |     |
| CamServer               | __InstanceName ChannelName \<Alias\>__|
|                         |     - Alternativelly, the stream URL can be provided instead of the instance name.|
|                         |     |



### Charts

The __Charts__ table allows defining the properties of each chart. Up to 5 charts can be used in Strip Chart.
Each chart can have two Y axis (Y1 and Y2).
After the plotting starts, the chart properties can be changed dynamically, with the exception of "Local Time".

<br/>

| Column       | Description |
| :------------| :---------- |
| Y1min, Y1max | If both are specified, sets a fixed Y1 range (otherwise range is automatic).|
| Y1max, Y2max | If both are specified, sets a fixed Y2 range (otherwise range is automatic).|
| Duration     | The time window of each history plot (default 60s).|
| Markers      | If set, plots the markers of each data point over the series line.|
| Local Time   | If set, adds data points based on the computer local time, instead of the channel time.|



### Graphics

- The __Background__ and __Grid__ buttons allow configuring the chart colors for background and grid. 
  The __Defaults__ button restores default values, which depend on the look and feel.

- The spinner __Plot Update__ configures, in milliseconds, how long data events are buffered before sent to the plots (default is 100ms).


### Settings 

- The field __Stream filter__ can hold an expression to filter stream data. An expression consist of condition on a channel, or a set of conditions separated by 'AND':

  ```
    C1 [ AND C2 ] [ AND C3 ]...
  ```

  Each conditions if formed with a stream channel name, an operator and a value:
  ```
    <channel name> <operator> <value>
  ```

  The operators can be: == , !=, <, >, <= or >=.

  The values can be numeric, boolean ('true' or 'false'), or else a string (surrounded by quotes).

- The spinner __Drag Interval__ configures, in milliseconds, when the time history plots are scrolled if no new data is received, 
  so that the plot doesn't show indefinitely data which is not updated (default is 1000ms).



### Persistence

If the checkbox __Save to__ is checked then all received data is saved using the file name template, and the given data format and layout (see __Data__ document).

If the checkbox is not set before starting the plotting then, while plotting, it is replaced by a __Save__ button, 
which saves the data currently displayed also using the file name template, and the given data format and layout.

<br/>

## Plots Tab

After the configuration is done, acquisition and plotting starts by clicking the "Start" button - or simply by switching to the "Plots" panel.

- Acquisition can be paused/resumed with the pause button in the to top right corner.

- If an alarm is set and active, the relative chart will flash and a beep will sound every second.
  if an alarm sound file is defined in command line arguments, then it will be used instead of the standard beep. When alarming, 
  a button displayed on the side of the __Pause__ button silences the beep for 30 minutes.

- The chart's context menu allows saving snapshots, setting plot attributes and changing the visibility of the series.