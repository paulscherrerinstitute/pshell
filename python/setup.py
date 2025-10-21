import os
from setuptools import setup

def read(fname):
    return open(os.path.join(os.path.dirname(__file__), fname)).read()

setup(
    name='psi-pshell',
    version="2.0.1",
    author='Paul Scherrer Institute',
    author_email="daq@psi.ch",
    description="Python client to PShell REST API",
    license="GPLv3",
    keywords="",
    url="https://github.com/paulscherrerinstitute/pshell/tree/master/python",
    install_requires=['requests', 'sseclient', 'pyzmq'],
    packages=['pshell'],
    long_description=read('Readme.md'),
    long_description_content_type="text/markdown"
)
