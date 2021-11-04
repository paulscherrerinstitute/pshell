#!/usr/bin/env python
from setuptools import setup

setup(
    name='pshell',
    version="1.17.0",
    description="pshell is Python a client to PShell REST interface",
    author='Paul Scherrer Institute',
    requires=["requests"],
    packages=['pshell',
              ]
)
