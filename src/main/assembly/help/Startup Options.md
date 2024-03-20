# Command-line startup options

<br>

| Option                   | Description |
| :----------------------- | :---------- |
| -h                       | Print the help message.|
| -c                       | Start command line interface.|
| -x                       | Start GUI with plots only.|
| -v                       | Start in server mode.|
| -w                       | Start the GUI shell console window only.|
| -l                       | Execution in local mode: no lock(exclusive-mode), no servers, no versioning, no context persistence, no startup tasks.|
| -d                       | Detached mode: no Workbench, Panel plugins are instantiated in a private frames.|
| -k                       | Persist state of  detached frames.|
| -i                       | No file locks (locks are not supported by some Linux systems).|
| -b                       | Bare mode: no plugin is loaded.|
| -e                       | Empty mode: device pool is not loaded.|
| -g                       | Local initialization script is not executed in startup.|
| -j                       | Disable session management.|
| -h                       | Hide graphical user interface at startup.|
| -r                       | Redirect standard output to Output window (if GUI is instantiated).|
| -o                       | Start in offline mode: data access only.|
| -s                       | All devices are simulated.|
| -n                       | Interpreter is not started.|
| -q                       | Quiet mode.|
| -a                       | Auto close after executing file.|
| -y                       | Force headless mode.|
| -z                       | Home folder is volatile (created in tmp folder).|
| -home=[path]             | Set the home folder (default is ./home).<br>The token '~', is replaced by the system home folder.|
| -outp=[path]             | Set the output folder (default is {home}).<br>The token '~', is replaced by the system home folder.|
| -data=[path]             | Set the data folder (default is {home}/data).<br>The token '~', is replaced by the system home folder.|
| -scpt=[path]             | Set the script folder (default is {home}/script).<br>The token '~', is replaced by the system home folder.|
| -devp=[path]             | Set the devices configuration folder (default is {home}/devices).|
| -plgp=[path]             | Set the plugin folder (default is {home}/plugins).|
| -extp=[path]             | Set the extensions folder (default is {home}/extensions).|
| -logp=[path]             | Set the log folder (default is {home}/logs).|
| -ctxp=[path]             | Set the log folder (default is {home}/logs).|
| -imgp=[path]             | Set the image folder (default is {data}).|
| -sesp=[path]             | Set the sessions folder (default is {home}/sessions).|
| -setp=[path]             | Override the setup file, which contains all path definitions (default is {config}/setup.properties).|
| -conf=[path]             | Override the config file, which stors user options (default is {config}/config.properties).|
| -pref=[path]             | Override the view preferences file.|
| -pool=[path]             | Set the device pool configuration file (default is {config}/devices.properties).|
| -plug=[path]             | Override the plugin definition file, which lists plugins loaded at startup (default is {config}/plugins.properties).|
| -task=[path]             | Override the task definition file,  which lists tasks loaded at startup (default is {config}/tasks.properties).|
| -sets=[path]             | Override the settings file - persisted script properties (default is {config}/settings.properties).|
| -pyhm=[path]             | Set the CPython home, overriding the configuration (default is PYTHONHOME variable).|
| -pini=[value]            | Override config flag (true or false)for parallel initialization.|
| -user=[name]             | Set the startup user.|
| -clog=[level]            | Set the console logging level: OFF, FINEST, FINER, FINE, INFO, WARNING or SEVERE.|
| -type=[ext]              | Set the script type, overriding the setup. Allowed values:  py, js or groovy.|
| -loca=[file]             | Set local startup file.|
| -dfmt=[format]           | Set the data format, overriding the configuration: h5, txt, csv or fda.|
| -dlay=[layout]           | Set the data layout, overriding the configuration: default, table, sf or fda.|
| -dspt                    | Disable scan plots.|
| -dspr                    | Disable scan printing to output.|
| -sbar                    | Append status bar to detached windows.|
| -dplt                    | Create plots for detached windows.|
| -strp                    | Show strip chart window (can be used together with -f).|
| -dtpn                    | Show data panel window only (can be used together with -f).|
| -help                    | Start the GUI help window.|
| -full                    | Start in full screen mode.|
| -dual                    | Start GUI and command line terminal: cannot be used if running in the background.|
| -dvpn=[cls, ...]         | Show a device panel, giving a class name and arguments (if the class has a main method) or else an inline device string.|
| -psrv=[url]              | URL of a plot server (plots are externalized adding '-dspt -hide=tabPlots').|
| -plot=[port]             | Start a plot server on the given port.|
| -extr=[value]            | Force (value=true) or disable (value=false) extraction of startup and utility scrips (default disabled in local mode).|
| -vers=[value]            | Force versioning enabled (value=true) or disabled (value=false) (default disabled in local mode).|
| -nbcf=[value]            | Force disabling (true) or enabling (false) the use of bytecode files (default defined in configuration).|
| -strh=[path]             | Strip chart default configuration folder.|
| -libp=[path]             | Add to library path.|
| -clsp=[path]             | Add to class path.|
| -scrp=[path]             | Add to script path.|
| -jcae=[path]             | Force EPICS configuration file (or, in volatile mode, EPICS configuration options separated by '|').|
| -uisc=[value]            | UI Scale factor as factor (e.g 0.75, 75% or 72dpi).|
| -laf=[name]              | Set application Look and Feel: system (s), metal (m), nimbus (n), darcula (d), flat (f), or dark (b).|
| -size=WxH                | Set application window size if GUI state not persisted. |
| -args=id:val, ...        | Provide arguments to interpreter.|
| -eval=...                | Evaluate the script statement.|
| -f=[name]                | File to run (together with -c option) or open in file in editor.|
| -t=[name,delay,interval] | Start a task in the background.|
| -p=[name]                | Load a plugin at startup.|
| -m=[path]                | Load a package at startup.|
