# Control Commands

Control Commands are available only in the interactive console. They are preceded by the character ':' and are not sent to python interpreter. 
These commands are always processed asynchronously - they are not refused if there is an ongoing script or console command.
 
<br>
<table>
  <tr>
    <th align="left">Command</th>
    <th align="left">Description</th>
  </tr>
  <tr>
    <td>:history</td>
    <td>Print shell history. Optional following argument filters the output.</td>
  </tr>
  <tr>
    <td>:evalb</td>
    <td>Evaluate argument in the background (equivalent to following a statement by '&').</td>
  </tr>
  <tr>
    <td>:inject</td>
    <td>Restore definitions of startup global variables (as 'context' and device names).</td>
  </tr>
  <tr>
    <td>:reload</td>
    <td>Dispose started plugins and load all configured plugins.</td>
  </tr>
  <tr>
    <td>:login [name]</td>
    <td>Select current user.</td>
  </tr>
  <tr>
    <td>:reset</td>
    <td>Reset the shell interpretor, reloading startup modules.</td>
  </tr>
  <tr>
    <td>:run [script]</td>
    <td>Run a script. Argument can be the full file name or file prefix (if in the path).</td>
  </tr>
  <tr>
    <td>:abort</td>
    <td>Abort execution of running script.</td>
  </tr>
  <tr>
    <td>:tasks</td>
    <td>List background tasks.</td>
  </tr>
  <tr>
    <td>:devices</td>
    <td>List configured devices.</td>
  </tr>
  <tr>
    <td>:users</td>
    <td>List configured users.</td>
  </tr>
</table>


__Note__: When the ':' character is typed into the interactive console, an auto-completion list shows the control commands.