# CommandInfo

The CommandInfo class is passed to the command execution callbacks, 
(on_command_started and on_command_finished), containing information on the 
execution of the script or statement.


Methods:
  * boolean isRunning() : return True if command is still running.
  * boolean isAborted(): return True if the command was aborted.
  * Object getResult(): return the command result (an exception object if an exception has taken place).
  * boolean isError(): return True if an exception has been thrown in the command execution.
  * void abort(): abort the command.
  * void join(): wait the end of execution of the command.

Attributes:
  * CommandSource source
  * String script: name of the script (or None if evaluating a console statement).
  * String command: console statement (or one if valuating a script).
  * boolean background: true if run in the background.
  * Object args: arguments to the command (list or dictionary).
  * Thread thread: running thread.
  * long id: unique identifier.
  * long start: start timestamp.
  * long end: finish timestamp.