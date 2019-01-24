# Interpreter


 - The interpreter is instantiated on the startup of the system. After initialization
   the builtin functions are injected to the global namespace and so are the global 
   devices.
 - If global names of functions or devices are overriten by scripts, they can be 
   restore using the _inject()_ builtin function (or the ":inject" control command)


# Console


 - The console panel sends live commands to the interpreter.
 - The position of the console can be set by menu "View" - "Console Location".
 - The console is composed by an input entry in the bottom and an output panel in the top.
 - One can navigate the command history with the arrow keys (up/down).
 - Auto-completion:
    - __Ctrl+space__ lists the global devices.
    - __Ctrl+shift+space__ lists the built-in functions
    - Typing '__.__' after an object lists its public methods.
    - Typing '__:__' in the beginning of the line lists the control commands.
 - The ":restart" command,  "Restart" button or "Shell"-"Restart" menu reinitializes 
   the system: all devices are disposed, the interpretor is reloaded, the builtin
   functions injected and the global device pool re-instantiated.


# Script Editor


 - A new script editor is opened by:
    - "New script file" button or "File" - "New" menu.
    - "Open file" button or "File" - "Open" menu.
    - Double-clicking a file in the "Scripts" tab of the status bar.
 - The editor features a syntax highlighting scheme:
    - Blue: reserved words
    - Brown: numbers
    - Orange: strings
    - Gray: commented code
    - Violet: built-in functions
    - Green: global devices
    - Black: everything else
 - Undo/Redo are triggered by __ctrl+z__ and __ctrl+y__.
 - __Ctrl+f__ performs a text search in the script, and __ctrl+shift+f__ searches in all script files.
 - __Ctrl+>__ indents a block and __ctrl+<__ unindents it.
 - __Ctrl+shift+>__ comments a block and __ctrl+shift+<__ uncomments it.
 - A script file can be detached from the workbench by double-clicking the title.
   The script can be sent back to the workbench by pressing ctrl while closing it.
   A script must be in the workbench to be debugged.
 - Auto-completion:
    - __Ctrl+space__ lists the global devices.
    - __Ctrl+shift+space__ lists the built-in functions
    - Typing '__.__' after an object lists its public methods.


# Running and Debugging Scripts


 - The "Run file" button or menu "Shell" - "Run" runs the file entirely.
   A script must be saved in order to be run. If it is not, the user will be asked to save it.
   The file will be executed completely by the interpretor and cannot be paused - only aborted.
 - A script is debugged with the "Debug script" button or menu "Shell" - "Debug".
   A debugged script does not need to be saved (the contents in the editor are debugged, 
   not the saved file). 
   The script is executed statement by statement and can be paused with the button 
   "Pause script" or menu "Shell" - "Pause".
 - A single statement can be executed with button "Step over" or menu "Shell" - "Step".
 - At any moment the execution can be aborted with the button "Abort execution".
 - The "Stop All" button or menu "Devices"-"Stop" aborts all movements.
   It calls a stop on all devices implementing the _Stoppable_ interface.
   This is only enabled if there is not a running script.