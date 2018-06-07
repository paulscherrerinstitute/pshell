# Command-line startup options

<table>
  <tr>
    <th align="left">Option</th>
    <th align="left">Description</th>
  </tr>
  <tr>
    <td>-?</td>
    <td>Print the help message.</td>
  </tr>
  <tr>
    <td>-h</td>
    <td>Start the GUI help window.</td>
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
    <td>-t</td>
    <td>Start GUI and command line terminal: cannot be used if running in the background.</td>
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
    <td>tAuto close after executing file.</td>
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
    <td>-setp=[path]</td>
    <td>Override the setup file, which contains all path definitions (default is {config}/setup.properties).</td>
  </tr>
  <tr>
    <td>-conf=[path]</td>
    <td>Override the config file, which stors user options (default is {config}/config.properties).</td>
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
    <td>-extr</td>
    <td>Force extract startup and utility scrips (must not be in local mode).</td>
  </tr>    
  <tr>
    <td>-strp</td>
    <td>Show strip chart window (can be used together with -f).</td>
  </tr>
  <tr>
    <td>-strc=[path]</td>
    <td>Strip chart default configuration folder.</td>
  </tr>
  <tr>
    <td>-dtpn=[path]</td>
    <td>Show data panel window only (can be used together with -f).</td>
  </tr>
  <tr>
    <td>-mlaf</td>
    <td>Use Metal look and feel (cross platform).</td>
  </tr>
  <tr>
    <td>-slaf</td>
    <td>Use System look and feel (or Metal if no System look and feel is found).</td>
  </tr>
  <tr>
    <td>-nlaf</td>
    <td>Use Nimbus look and feel (cross platform).</td>
  </tr>
  <tr>
    <td>-dlaf</td>
    <td>Use a dark look and feel (cross platform).</td>
  </tr>
  <tr>
    <td>-args=id:val,...</td>
    <td>Provide arguments to interpreter.</td>
  </tr>
  <tr>
    <td>-f=[name]</td>
    <td>Runs a file instead of entering interactive shell (together with -c option).</td>
  </tr>
  <tr>
    <td>-p=[name]</td>
    <td>Loads a given plugin at startup .</td>
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
