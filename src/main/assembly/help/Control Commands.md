# Control Commands

Control Commands are available only in the interactive console. They are preceded by the character ':' and are not sent to python interpreter. 
These commands are always processed asynchronously - they are not refused if there is an ongoing script or console command.

<br/>

| Command       | Description |
| :------------ | :---------- |
| :history      | Print shell history. Optional following argument filters the output.|
| :evalb        | Evaluate argument in the background (equivalent to following a statement by '&'). |
| :inject       | Restore definitions of startup global variables (as 'context' and device names). |
| :reload       | Dispose started plugins and load all configured plugins. |
| :login [name] | Select the current user. |
| :reset        | Reset the shell interpretor, reloading startup modules. |
| :run [script] | Run a script. Argument can be the full file name or file prefix (if in the path). |
| :abort        | Abort execution of running script. |
| :tasks        | List background tasks. |
| :devices      | List configured devices. |
| :users        | List configured users. |

 

__Note__: When the ':' character is typed into the interactive console, an auto-completion list shows the control commands.