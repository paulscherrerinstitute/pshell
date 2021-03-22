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

def to_array(obj, type = None, primitive = True):
    """Convert Python list to Java array.

    Args:
        obj(list): Original data.
        type(str): array type 'b' = byte, 'h' = short, 'i' = int, 'l' = long,  'f' = float, 'd' = double,
                              'c' = char, 'z' = boolean, 's' = String,  'o' = Object
    Returns:
        Java array.
    """
    if obj is None:
        return None
    if type is None:
        type = 'o'
        enforceArrayType=False
    else:
        enforceArrayType=True
    if type[0] == '[':
        type = type[1:]        
    element_type = ScriptUtils.getPrimitiveType(type) if primitive else ScriptUtils.getType(type)

    def convert_1d_array(obj):
        try:
            return jarray.array(obj,type)
        except:
            if type == 'c':
                ret = java.lang.reflect.Array.newInstance(element_type,len(obj))
                for i in range(len(obj)): ret[i] = chr(obj[i])
                return ret
            if type == 'z':
                ret = java.lang.reflect.Array.newInstance(element_type,len(obj))
                for i in range(len(obj)):
                    ret[i]= True if obj[i] else False
                return ret
            if type == 'o':
                ret = java.lang.reflect.Array.newInstance(element_type,len(obj))
                for i in range(len(obj)):
                    ret[i]= obj[i] 
                return ret
            if type == "s":
                return Convert.toStringArray(obj)   
            if primitive:   
                ret = Convert.toPrimitiveArray(obj, element_type)  
            else:
                ret = java.lang.reflect.Array.newInstance(element_type,len(obj))
                for i in range(len(obj)): ret[i] = Convert.toType(obj[i],element_type)
            return ret
                
    if isinstance(obj,PyArray):
        if enforceArrayType:
            if Arr.getComponentType(obj) != element_type:
                rank = Arr.getRank(obj)
                if (rank== 1):
                    obj=convert_1d_array(obj)
                elif (rank>1):  
                    pars, aux = [element_type], obj
                    for i  in range(rank):
                        pars.append(len(aux))
                        aux = aux[0]
                    ret = java.lang.reflect.Array.newInstance(*pars)    
                    for i in range(len(obj)):
                        ret[i]=to_array(obj[i], type)                        
                    obj = ret
    elif is_list(obj):        
        if type=='o':
            ret = java.lang.reflect.Array.newInstance(element_type, len(obj))
            for i in range (len(obj)):
                if is_list(obj[i]) or isinstance(obj[i],PyArray):
                    ret[i] = to_array(obj[i],type)
                else:
                    ret[i] = obj[i]
            obj=ret
        elif len(obj)>0 and (is_list(obj[0]) or isinstance(obj[0],PyArray)):
            pars, aux = [element_type], obj
            while len(aux)>0 and (is_list(aux[0]) or isinstance(aux[0],PyArray)):
                pars.append(len(aux))
                aux = aux[0]
            pars.append(0)
            ret = java.lang.reflect.Array.newInstance(*pars)
            for i in range(len(obj)):
                ret[i]=to_array(obj[i], type)
            obj=ret
        else:
         obj= convert_1d_array(obj)
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



