# Overview

This package implements a Python client to PShell REST API.

- Project URL: https://github.com/paulscherrerinstitute/pshell

# Installation

Install via pip:

```
pip install psi-pshell
```

Dependencies:
  - requests
  - pyzmq

# Classes
  - PShellClient: client to PShell sequencer API, providing access to the interpreter, event receiving, control of the state machine, data retrieval and plot visualization.
  - PShellProxy: PShellClient extension adding functions to run scans and control devices as in PShell console.
  - PlotClient: client to PShell plotting API, allowing externalize plots to PShell's Plotter application.
