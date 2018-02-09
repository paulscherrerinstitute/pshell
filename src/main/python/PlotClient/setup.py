#!/usr/bin/env python
from setuptools import setup

setup(
    name='pplot',
    version="1.0.1",
    description="pplot is Python a client to PShell plot server",
    author='Paul Scherrer Institute',
    requires=["zmq"],
    packages=['.',
              ]
)
