# Built-in Functions

Built-in functions are defined in Lib/startup.py, which is loaded at the initialization of the shell.


These functions provide the script API to many aspects of PShell:

 * Standard scan commands:
    - lscan
    - vscan
    - ascan
    - rscan
    - cscan
    - hscan
    - oscan
    - bscan
    - tscan
    - mscan
    - escan
    - xscan
    - bsearch
    - hsearch

 * Data plotting:
    - plot
    - get_plots
    - get_plot_snapshots

 * Data access functions:
    - load_data
    - get_attributes
    - get_data_info
    - save_dataset
    - create_group
    - create_dataset
    - create_table
    - append_dataset
    - append_table
    - flush_data
    - set_attribute
    - set_exec_pars
    - get_exec_pars
    - log

 * EPICS channel access:
    - caget
    - cawait
    - camon
    - caput
    - caputq
    - create_channel_device

 * Concurrent execution:
    - fork
    - join
    - parallelize
    - invoke

 * Script evaluation and background task control.
    - run
    - abort
    - start_task
    - stop_task
    - set_return

 * Versioning tools:
    - commit
    - diff
    - checkout_tag
    - checkout_branch
    - pull_repository
    - push_repository
    - cleanup_repository

 * Device Pool functions:
    - add_device
    - remove_device
    - get_device
    - set_device_alias (deprecated, use "dev.set_alias" instead)
    - stop
    - update
    - reinit
    - create_device
    - create_averager

 * Mathematical functions:
    - arrmul, arradd, arrdiv, arrsub, arrabs, arroff
    - mean
    - variance
    - stdev  
    - center_of_mass
    - poly
    - histogram
    - convex_hull

 * Utilities:
    - get_setting
    - set_setting
    - exec_cmd
    - exec_cpython
    - bsget
    - frange
    - flatten
    - to_array       
    - to_list 
    - inject
    - notify
    - expand_path
    - help       

 * UI interaction:
    - set_status
    - set_preference
    - get_string
    - get_option
    - show_message
    - show_panel

 
__Note__: Installation-specific built-in functions can be added editing local.py.


