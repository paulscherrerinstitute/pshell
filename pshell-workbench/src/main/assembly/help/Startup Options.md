# Command-line options and Environment Variables

<br>

| Command-line Option           | Environment Variable          | Description |
| :---------------------------- | :---------------------------- | :---------- |
| -h,--help                     |                               | Show help|
| -vr,--version                 |                               | Show version|
| -db,--debug                   | PSHELL_DEBUG                  | Set debug mode|
| -lf,--laf                     | PSHELL_LAF                    | Look and feel: system, metal, nimbus, flat, dark or black|
| -sz,--size                    | PSHELL_SIZE                   | Set application window size|
| -fs,--full_screen             | PSHELL_FULL_SCREEN            | Start in full screen mode|
| -us,--ui_scale                | PSHERLL_UI_SCALE              | UI Scale factor as factor (e.g 0.75, 75% or 72dpi)|
| -cl,--console_log             | PSHELL_CONSOLE_LOG            | Set the console logging level|
| -qt,--quality                 | PSHELL_QUALITY                | Set plot rendering quality: Low, Medium, High, Maximum|
| -ti,--title                   | PSHELL_TITLE                  | Application title|
| -st,--start                   | PSHELL_START                  | Execute command line command on startup|
| -hd,--hide                    | PSHELL_HIDE                   | Hide graphical user interface at startup|
| -l,--local                    | PSHELL_LOCAL                  | Local  mode (multiple instance) : no servers, no lock, no context persistence|
| -ec,--epics_config            | PSHELL_EPICS_CONFIG           | EPICS config file or EPICS options separated by '|'|
| -cs,--camera_server <url>     | PSHELL_CAMERA_SERVER          | Address of CamServer camera proxy|
| -ps,--pipeline_server <url>   | PSHELL_PIPELINE_SERVER        | Address of CamServer pipeline proxy|
| -ar,--archiver <url>          | PSHELL_ARCHIVER               | Set the address of the Daqbuf server (othrerwise defined by DAQBUF_DEFAULT_URL)|
| -be,--backend <name>          | PSHELL_BACKEND                | Set the default backend (othrerwise defined by DAQBUF_DEFAULT_BACKEND)|
| -s,--simulation               | PSHELL_SIMULATION             | All devices are simulated|
| -k,--empty                    | PSHELL_EMPTY                  | Empty mode: device pool is not loaded at startup|
| -u,--parallel                 | PSHELL_PARALLEL               | Parallel initialization of devices (values: true (default)or false)|
| -f,--file <path>              | PSHELL_FILE                   | File to run or open in file in editor|
| -e,--eval <code>              | PSHELL_EVAL                   | Evaluate the script statement|
| -t,--task                     | PSHELL_TASK                   | Start a task using the format script,delay,interval|
| -p,--plugin                   | PSHELL_PLUGIN                 | Load a plugin|
| -m,--package                  | PSHELL_PACKAGE                | Load a package|
| -c,--cli                      | PSHELL_CLI                    | Start command line interface|
| -x,--plot_only                | PSHELL_PLOT_ONLY              | Start GUI with plots only|
| -v,--server                   | PSHELL_SERVER                 | Start in server mode|
| -y,--headless                 | PSHELL_HEADLESS               | Force headless mode|
| -w,--shell                    | PSHELL_SHELL                  | Start the shell window only|
| -d,--detached                 | PSHELL_DETACHED               | Detached mode: Panel plugins are instantiated in private frames|
| -b,--bare                     | PSHELL_BARE                   | Bare mode: no plugin is loaded at startup|
| -g,--generic                  | PSHELL_GENERIC                | Local initialization script is not executed at startup|
| -r,--redirect                 | PSHELL_REDIRECT               | Redirect standard output to Output window|
| -o,--offline                  | PHELL_OFFLINE                 | Start in offline mode: data access only|
| -n,--disabled                 | PSHELL_DISABLED               | Interpreter is not started|
| -q,--quiet                    | PSHELL_QUIET                  | Quiet mode|
| -a,--auto_close               | PSHELL_AUTO_CLOSE             | Auto close after executing file|
| -z,--volatile                 | PSHELL_VOLATILE               | Home folder is volatile (created in tmp folder)|
| -home,--home_path <path>      | PSHELL_HOME_PATH              | Set home folder (default is ~/pshell/home)|
| -outp,--output_path <path>    | PSHELL_OUTPUT_PATH            | Set output folder (default is ~/pshell/home)|
| -scpt,--scripts_path <path>   | PSHELL_SCRIPT_PATH            | Set script folder (default is {home}/script)|
| -devp,--devices_path <path>   | PSHELL_DEVICES_PATH           | Set devices config folder (default is {home}/devices)|
| -plgp,--plugins_path <path>   | PSHELL_PLUGINS_PATH           | Set plugin folder (default is {home}/plugin)|
| -cfgp,--config_path <path>    | PSHELL_CONFIG_PATH            | Set config folder (default is {home}/config)|
| -extp,--extensions_path <path>| PSHELL_EXENSIONS_PATH         | Set extensions folder (default is {home}/extensions)|
| -wwwp,--www_path <path>       | PSHELL_WWW_PATH               | Set www folder (default is {home}/www)|
| -data,--data_path <path>      | PSHELL_DATA_PATH              | Set data folder (default is {outp}/data)|
| -imgp,--images_path <path>    | PSHELL_IMAGE_PATH             | Set image folder (default is {data})|
| -logp,--logs_path <path>      | PSHELL_LOGS_PATH              | Set log folder (default is {outp}/logs)|
| -ctxp,--context_path <path>   | PSHELL_CONTEXT_PATH           | Set context folder (default is {outp}/context)|
| -sesp,--sesssions_path <path> | PSHELL_SESSIONS_PATH          | Set sessions folder (default is {outp}/sessions)|
| -quep,--queues_path <path>    | PSHELL_QUEUES_PATH            | Set default location for queue files (default is {script})
| -sclp,--script_lib <path>     | PSHELL_SCRIPT_LIB_PATH        | Script library path (default is {script}/Lib)|
| -xscp,--xscan_path <path>     | PSHELL_XSCAN_PATH             | Set the xscan file folder (default is {script})|
| -pyhm,--python_home <path>    | PSHELL_PYTHON_HOME            | Set CPython home (default is PYTHONHOME)|
| -pool,--device_pool <path>    | PSHELL_DEVICE_POOL           | Set devices device pool config file|
| -plug,--plugin_config <path>  | PSHELL_PLUGIN_CONFIG          | Set plugins definition file|
| -tskc,--task_config <path>    | PSHELL_TASK_CONFIG             | Set task definition file|
| -sets,--settings <path>       | PSHELL_SETTINGS               | Set settings file|
| -loca,--local_startup <path>  | PSHELL_LOCAL_STARTUP          | Set local startup script (default is {script}/local)|
| -user,--user                  | PSHELL_USER                   | Set the startup user|
| -type,--type                  | PSHELL_SCRIPT_TYPE            | Set the script type, overriding the setup: py, cpy, jv or groovy|
| -dfmt,--data_format           | PSHELL_DATA_FORMAT            | Set the data format, overriding the configuration: h5, txt, csv or fda|
| -dlay,--data_layout           | PSHELL_DATA_LAYOUT            | Set the data layout, overriding the configuration: default, table, sf or fda|
| -dspl,--disable_plot          | PSHELL_DISABLE_PLOT           | Disable scan plots|
| -dspr,--disable_print         | PSHELL_DISABLE_PRINT          | Disable printing scans to console|
| -extr,--extract               | PSHELL_EXTRACT                | Force (true) or disable (false) extraction of startup and utility scrips|
| -vers,--versioning            | PSHELL_VERSIONING             | Force versioning enabled (true) or disabled (false)|
| -nbcf,--no_bytecode           | PSHELL_NO_BYTECODE            | Force disabling (true) or enabling (false) the use of bytecode files|
| -lock,--lock_mode             | PSHELL_LOCK_MODE              | Lock protection for exclusive (not local) mode: none, user (default) or global|
| -libp,--library_path <path>   | PSHELL_LIBRARY_PATH           | Add to library path|
| -clsp,--class_path <path>     | PSHELL_CLASS_PATH             | Add to class path|
| -args,--interp_args <args>    | PSHELL_INTERP_ARGS            | Provide arguments to interpreter|
| -sbar,--statusbar             | PSHELL_STATUSBAR              | Append status bar to detached windows|
| -tbar,--toolbar               | PSHELL_TOOLBAR                | Append tool bar to detached processors|
| -dplt,--detached_plots        | PSHELL_DETACHED_PLOTS         | Create plots for detached windows|
| -hcmp,--hide_component        | PSHELL_HIDE_COMPONENTS        | Hide component in GUI given its name|
| -pers,--persist               | PROPERTY_DETACHED_PERSISTED   | Persist state of frames|
| -psrv,--plot_server <url>     | PSHELL_PLOT_SERVER            | URL of a plot server (plots are externalized adding '-hcmp=tabPlots')|
| -diss,--disable_sessions      | PSHELL_DISABLE_SESSIONS       | Disable session management|
| -dual, --stdio                | PSHELL_STDIO                  | GUI with command line interface (not allowed if running in the background)|
| -pref,--preferences <path>    | PSHELL_PREFERENCES            | Override the view preferences file|
| -cfg,--config                 | PSHELL_CONFIG                 | Set config file (default is {config}/config.properties)|


<p>

The startup scripts of application RPMs use the additional environmet variables:

<br>

| Environment Variable               | Description |
| :--------------------------------- | :---------- |
| PSHELL_JVM_OPTIONS                 | JVM options used for all PShell applications|
| APP_<APP_NAME>_JVM_OPTIONS         | JVM options for the application <APP_NAME>|
| PSHELL_ARGS                        | Startup options used for all PShell applications|
| APP_<APP_NAME>_ARGS                | Startup options used for the application <APP_NAME>|


<p>

The priority when resolving startup options is the following, from lower to higher:
- PSHELL_ARGS environment variable.
- APP_<APP_NAME>_ARGS environment variable.
- PSHELL_<OPTION_NAME> environment variables.
- Command-line option.
    - If given multiple times - as when chaining startup scripts - the last value of a command-line option has higher proiprity.



