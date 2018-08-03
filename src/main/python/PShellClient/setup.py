#!/usr/bin/env python
from setuptools import setup

setup(
    name='pshell_client',
    version="1.0.2",
    description="pshell_client is Python a client to PShell REST interface",
    author='Paul Scherrer Institute',
    requires=["requests"],
    packages=['.',
              ]
)
