# Command-line startup options

<table>
  <tr>
    <th align="left">Option</th>
    <th align="left">Description</th>
  </tr>
  <tr>
    <td>-h</td>
    <td>Print the help message.</td>
  </tr>
  <tr>
    <td>-c</td>
    <td>Start command line interface.</td>
  </tr>
  <tr>
    <td>-x</td>
    <td>Start GUI with plots only.</td>
  </tr>
  <tr>
    <td>-v</td>
    <td>Start in server mode.</td>
  </tr>
  <tr>
    <td>-w</td>
    <td>Start the GUI shell console window only.</td>
  </tr>
  <tr>
    <td>-l</td>
    <td>Execution in local mode: no lock(exclusive-mode), no servers, no versioning, no context persistence, no startup tasks.</td>
  </tr>
  <tr>
    <td>-d</td>
    <td>Detached mode: no Workbench, Panel plugins are instantiated in a private frames.</td>
  </tr>
  <tr>
    <td>-k</td>
    <td>Persist state of  detached frames.</td>
  </tr>
  <tr>
    <td>-i</td>
    <td>No file locks (locks are not supported by some Linux systems).</td>
  </tr>
  <tr>
    <td>-b</td>
    <td>Bare mode: no plugin is loaded.</td>
  </tr>
  <tr>
    <td>-e</td>
    <td>Empty mode: device pool is not loaded.</td>
  </tr>
  <tr>
    <td>-g</td>
    <td>Local initialization script is not executed in startup.</td>
  </tr>
  <tr>
    <td>-j</td>
    <td>Disable session management.</td>
  </tr>
  <tr>
    <td>-h</td>
    <td>Hide graphical user interface at startup.</td>
  </tr>
  <tr>
    <td>-r</td>
    <td>Redirect standard output to Output window (if GUI is instantiated).</td>
  </tr>
  <tr>
    <td>-o</td>
    <td>Start in offline mode: data access only.</td>
  </tr>
  <tr>
    <td>-s</td>
    <td>All devices are simulated.</td>
  </tr>
  <tr>
    <td>-n</td>
    <td>Interpreter is not started.</td>
  </tr>
  <tr>
    <td>-q</td>
    <td>Quiet mode.</td>
  </tr>
  <tr>
    <td>-a</td>
    <td>Auto close after executing file.</td>
  </tr>
    <tr>
      <td>-y</td>
      <td>Force headless mode.</td>
    </tr>
  <tr>
    <td>-z</td>
    <td>.</td>
  </tr>
  <tr>
    <td>-home=[path]</td>
    <td>Set the home folder (default is ./home).<br>The token '~', is replaced by the system home folder.</td>
  </tr>
  <tr>
    <td>-outp=[path]</td>
    <td>Set the output folder (default is {home}).<br>The token '~', is replaced by the system home folder.</td>
  </tr>
  <tr>
    <td>-data=[path]</td>
    <td>Set the data folder (default is {home}/data).<br>The token '~', is replaced by the system home folder.</td>
  </tr>
  <tr>
    <td>-scpt=[path]</td>
    <td>Set the script folder (default is {home}/script).<br>The token '~', is replaced by the system home folder.</td>
  </tr>
  <tr>
    <td>-devp=[path]</td>
    <td>Set the devices configuration folder (default is {home}/devices).</td>
  </tr>  
  <tr>
    <td>-plgp=[path]</td>
    <td>Set the plugin folder (default is {home}/plugins).</td>
  </tr> 
  <tr>
    <td>-extp=[path]</td>
    <td>Set the extensions folder (default is {home}/extensions)r.</td>
  </tr>  
  <tr>
    <td>-logp=[path]</td>
    <td>Set the log folder (default is {home}/logs).</td>
  </tr>  
  <tr>
    <td>-ctxp=[path]</td>
    <td>ctcp=<path>\tSet the context folder (default is {home}/context).</td>
  </tr>  
  <tr>
    <td>-imgp=[path]</td>
    <td>Set the image folder (default is {home}/image).</td>
  </tr>  <tr>
    <td>-sesp=[path]</td>
    <td>Set the  sessions folder (default is {home}/sessions).</td>
  </tr>
  <tr>
    <td>-setp=[path]</td>
    <td>Override the setup file, which contains all path definitions (default is {config}/setup.properties).</td>
  </tr>
  <tr>
    <td>-conf=[path]</td>
    <td>Override the config file, which stors user options (default is {config}/config.properties).</td>
  </tr>
  <tr>
    <td>-pref=[path]</td>
    <td>Override the view preferences file.</td>
  </tr>
  <tr>
    <td>-pool=[path]</td>
    <td>Set the device pool configuration file (default is {config}/devices.properties).</td>
  </tr>
  <tr>
    <td>-plug=[path]</td>
    <td>Override the plugin definition file, which lists plugins loaded at startup (default is {config}/plugins.properties).</td>
  </tr>
  <tr>
    <td>-task=[path]</td>
    <td>Override the task definition file,  which lists tasks loaded at startup (default is {config}/tasks.properties).</td>
  </tr>
  <tr>
    <td>-sets=[path]</td>
    <td>Override the settings file - persisted script properties (default is {config}/settings.properties).</td>
  </tr>
  <tr>
    <td>-pini=[value]</td>
    <td>Override config flag (true or false)for parallel initialization.</td>
  </tr>
  <tr>
    <td>-user=[name]</td>
    <td>Set the startup user.</td>
  </tr>
  <tr>
    <td>-clog=[level]</td>
    <td>Set the console logging level: OFF, FINEST, FINER, FINE, INFO, WARNING or SEVERE.</td>
  </tr>
  <tr>
    <td>-type=[ext]</td>
    <td>Set the script type, overriding the setup. Allowed values:  py, js or groovy.</td>
  </tr>
  <tr>
    <td>-loca=[file]</td>
    <td>Set local startup file.</td>
  </tr>
  <tr>
    <td>-dfmt=[format]</td>
    <td>Set the data format, overriding the configuration: h5, txt, csv or fda.</td>
  </tr>
  <tr>
    <td>-dlay=[format]</td>
    <td>Set the data layout, overriding the configuration: default, table, sf or fda.</td>
  </tr>
  <tr>
    <td>-dspt</td>
    <td>Disable scan plots.</td>
  </tr>
  <tr>
    <td>-dspr</td>
    <td>Disable scan printing to output.</td>
  </tr>
  <tr>
    <td>-sbar</td>
    <td>Append status bar to detached windows.</td>
  </tr>
  <tr>
    <td>-dplt</td>
    <td>Create plots for detached windows.</td>
  </tr>
  <tr>
    <td>-strp</td>
    <td>Show strip chart window (can be used together with -f).</td>
  </tr>
  <tr>
    <td>-dtpn</td>
    <td>Show data panel window only (can be used together with -f).</td>
  </tr>
  <tr>
    <td>-dtpn</td>
    <td>Show CamServer viewer.</td>
  </tr>
  <tr>
    <td>-help</td>
    <td>Start the GUI help window.</td>
  </tr>
  <tr>
    <td>-full</td>
    <td>Start in full screen mode.</td>
  </tr>
  <tr>
    <td>-dual</td>
    <td>Start GUI and command line terminal: cannot be used if running in the background.</td>
  </tr>
  <tr>
    <td>-dvpn=[cls, ,,,]</td>
    <td>Show a device panel, giving a class name and arguments, if it has a main method.</td>
  </tr>    
  <tr>
    <td>-extr=[value]</td>
    <td>Force (value=true) or disable (value=false) extraction of startup and utility scrips (default disabled in local mode).</td>
  </tr>    
  <tr>
    <td>-vers=[value]</td>
    <td>Force versioning enabled (value=true) or disabled (value=false) (default disabled in local mode)</td>
  </tr>
  <tr>
    <td>-nbcf=[value]</td>
    <td>Force disabling (true) or enabling (false) the use of bytecode files (default defined in configuration)</td>
  </tr>
  <tr>
    <td>-strh=[path]</td>
    <td>Strip chart default configuration folder.</td>
  </tr>
  <tr>
    <td>-libp=[path]</td>
    <td>Add to library path.</td>
  </tr>
  <tr>
    <td>-clsp=[path]</td>
    <td>Add to class path.</td>
  </tr>
  <tr>
    <td>-scrp=[path]</td>
    <td>Add to script path.</td>
  </tr>
  <tr>
    <td>-jcae=[path]</td>
    <td>Force EPICS configuration file (or, in volatile mode, EPICS configuration options separated by '|').</td>
  </tr>
  <tr>
    <td>-uisc=[value]</td>
    <td>UI Scale factor as factor (e.g 0.75, 75% or 72dpi).</td>
  </tr>
  <tr>
    <td>-laf=[name]</td>
    <td>Set application Look and Feel: system (s), metal (m), nimbus (n), darcula (d), 
        flat (f), or dark (b).</td>
  </tr>
    <td>-size=WxH</td>
    <td>Set application window size if GUI state not persisted.</td>
  </tr>
  <tr>
    <td>-args=id:val,...</td>
    <td>Provide arguments to interpreter.</td>
  </tr>
  <tr>
    <td>-f=[name]</td>
    <td>File to run (together with -c option) or open in file in editor.</td>
  </tr>
  <tr>
    <td>-t=[name,delay,interval]</td>
    <td>Start a task in the background.</td>
  </tr>
  <tr>
    <td>-p=[name]</td>
    <td>Load a plugin at startup.</td>
  </tr>
  <tr>
    <td>-m=[path]</td>
    <td>Load a package at startup.</td>
  </tr>
</table>

<!--
TODO: txtmark is not rendering tables:
| Option   | Description |
| ---------|-------------|    
| -help    | Print help message |
| -c       | Start command line interface only (no GUI) |
| -t       | Start GUI and command line terminal: cannot be used if running in the background |
| -w       | Start the GUI shell console window only |
| -l       | Execution in local mode (no exclusive-mode, no servers, no versioning, no context persistence) |
| -b       | Execution in bare mode (no plugin is loaded) |
| -h       | Hide graphical user interface at startup |
| -r       | Redirect standard output to Output window (if GUI is instantiated) |
| -o       | Start in offline mode: no devices configuration and data access only |
| -s       | All devices are simulated |
| -home=<> | Set the home folder (default is ./home |
| -outp=<> | Set the output folder (default is {home} |
| -user=<> | Set the startup user |
| -type=<> |Set the script type, overriding the setup |
| -mlaf    | Use Metal look and feel (cross platform) |
| -slaf    | Use System look and feel (or Metal if no System look and feel is found) |
| -nlaf    | Use Nimbus look and feel (cross platform) |
| -dlaf    | Use a dark variation of the Nimbus look and feel |
| -f=<...> | Runs a file instead of entering interactive shell (together with -c option) |
| -p=<...> | Loads a given plugin at startup |

-->
