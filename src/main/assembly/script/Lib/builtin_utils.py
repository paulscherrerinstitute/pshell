import sys
import time
import math
import os.path
from operator import add, mul, sub, truediv
from time import sleep
from array import array
import jarray

import java.lang.Class as Class
import java.lang.Object as Object
import java.beans.PropertyChangeListener
import java.util.concurrent.Callable
import java.util.List
import java.lang.reflect.Array
import java.lang.Thread
import java.awt.image.BufferedImage as BufferedImage
import java.awt.Color as Color
import java.awt.Point as Point
import java.awt.Dimension as Dimension
import java.awt.Font as Font
import org.python.core.PyArray as PyArray
import org.python.core.PyFunction as PyFunction
import org.python.core.PyMethod as PyMethod
import org.python.core.PyGenerator as PyGenerator

import java.lang.Boolean
import java.lang.Integer
import java.lang.Float
import java.lang.Double
import java.lang.Short
import java.lang.Byte
import java.lang.Long
import java.lang.String

import ch.psi.pshell.core.Context
import ch.psi.pshell.scripting.ScriptUtils as ScriptUtils
import ch.psi.utils.Convert as Convert
import ch.psi.utils.Arr as Arr


###################################################################################################
#Type conversion and checking
###################################################################################################

def to_array(obj, type = 'o'):
    """Convert Python list to Java array.

    Args:
        obj(list): Original data.
        type(str): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 'd' = double,
                              'c' = char, 'z' = boolean, 's' = String,  'o' = Object
    Returns:
        Java array.
    """
    if type[0] == '[':
        type = type[1:]
    arrayType = ScriptUtils.getType("["+type)

    if obj is None:
        return None
    if isinstance(obj,java.util.List):
        obj = obj.toArray()
        if type != 'o':
            obj = Convert.toPrimitiveArray(obj, ScriptUtils.getType(type))
    if isinstance(obj,PyArray):
        if type != 'o':
            if (Arr.getRank(obj)== 1) and (obj.typecode != type):
                ret = java.lang.reflect.Array.newInstance(ScriptUtils.getType(type), len(obj))
                if type == 's':
                    for i in range(len(obj)): ret[i] = str(obj[i])
                elif type == 'c':
                    for i in range(len(obj)): ret[i] = chr(obj[i])
                else:
                    for i in range(len(obj)): ret[i] = obj[i]
                obj = ret
            if type not in ['o', 's']:
                obj = Convert.toPrimitiveArray(obj)
        return obj
    if is_list(obj):
        if type=='o' or  type== 's':
            ret = java.lang.reflect.Array.newInstance(ScriptUtils.getType(type), len(obj))
            for i in range (len(obj)):
                if is_list(obj[i]):
                    ret[i] = to_array(obj[i],type)
                elif type == 's':
                    ret[i] = str(obj[i])
                else:
                    ret[i] = obj[i]
            return ret

        if len(obj)>0 and is_list(obj[0]):
            if  len(obj[0])>0 and is_list(obj[0][0]):
                    ret = java.lang.reflect.Array.newInstance(arrayType,len(obj),len(obj[0]))
                    for i in range(len(obj)):
                        ret[i]=to_array(obj[i], type)
                    return ret
            else:
                ret = java.lang.reflect.Array.newInstance(arrayType,len(obj))
                for i in range(len(obj)):
                    ret[i]=to_array(obj[i], type)
                return ret
        return jarray.array(obj,type)
    return obj

def to_list(obj):
    """Convert an object into a Python List.

    Args:
        obj(tuple or array or ArrayList): Original data.
    
    Returns:
        List.
    """
    if obj is None:
        return None
    if isinstance(obj,tuple) or isinstance(obj,java.util.ArrayList) :
        return list(obj)
    #if isinstance(obj,PyArray):
    #    return obj.tolist()
    if not isinstance(obj,list):
        return [obj,]
    return obj

def is_list(obj):
    return isinstance(obj,tuple) or isinstance(obj,list) or isinstance (obj, java.util.ArrayList)

def is_string(obj):
    return (type(obj) is str) or (type(obj) is unicode)


def is_interpreter_thread():
    return java.lang.Thread.currentThread().name == "Interpreter Thread"

###################################################################################################
#Access to context singleton
###################################################################################################
def get_context():
    return ch.psi.pshell.core.Context.getInstance()



