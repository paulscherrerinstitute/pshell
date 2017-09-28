################################################################################################### 
# This moddule is called by demo scripts to execute and embed CPython.
# Must be put in the scripts folder, or else in the python path.
################################################################################################### 

import sys
import os

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

try:
    import tkinter as tk 
except:
    import Tkinter as tk
   

def calc(array):    
    return np.transpose(array + array)


def test_pandas():
    s = pd.Series([1,3,5,np.nan,6,8])
    print (s)
    dates = pd.date_range('20130101', periods=6)
    print (dates)
    df = pd.DataFrame(np.random.randn(6,4), index=dates, columns=list('ABCD'))
    print (df)
    df2 = pd.DataFrame({ 'A' : 1.,
        'B' : pd.Timestamp('20130102'),
        'C' : pd.Series(1,index=list(range(4)),dtype='float32'),
        'D' : np.array([3] * 4,dtype='int32'),
        'E' : pd.Categorical(["test","train","test","train"]),
        'F' : 'foo' })
    print (df2)
    print (df2.dtypes)
    print (df.head())
    print (df.tail(3))
    print (df.values)
    print (df.describe())
    print (df.T)
    print (df.sort_index(axis=1, ascending=False))
    #print (df.sort_values(by='B'))
    print (df['A'])
    print (df[0:3])
    print (df.mean())
    return str(df.mean())


def test_tkinter():
    root = tk.Tk()
    listb = tk.Listbox(root)           
    for item in ["Hello", "World"]:               
        listb.insert(0,item)    
    listb.pack()                  
    root.mainloop()


def test_matplotlib(start,stop,step):
    import threading   
    x = np.arange(start,stop,step)
    y = np.exp(-x)

    # example variable error bar values
    yerr = 0.1 + 0.2*np.sqrt(x)
    xerr = 0.1 + yerr

    # First illustrate basic pyplot interface, using defaults where possible.
    plt.figure()
    plt.errorbar(x, y, xerr=0.2, yerr=0.4)
    plt.title("Simplest errorbars, 0.2 in x, 0.4 in y")

    # Now switch to a more OO interface to exercise more features.
    fig, axs = plt.subplots(nrows=2, ncols=2, sharex=True)
    ax = axs[0,0]
    ax.errorbar(x, y, yerr=yerr, fmt='o')
    ax.set_title('Vert. symmetric')

    # With 4 subplots, reduce the number of axis ticks to avoid crowding.
    ax.locator_params(nbins=4)

    ax = axs[0,1]
    ax.errorbar(x, y, xerr=xerr, fmt='o')
    ax.set_title('Hor. symmetric')

    ax = axs[1,0]
    ax.errorbar(x, y, yerr=[yerr, 2*yerr], xerr=[xerr, 2*xerr], fmt='--o')
    ax.set_title('H, V asymmetric')

    ax = axs[1,1]
    ax.set_yscale('log')
    # Here we have to be careful to keep all y values positive:
    ylower = np.maximum(1e-2, y - yerr)
    yerr_lower = y - ylower

    ax.errorbar(x, y, yerr=[yerr_lower, 2*yerr], xerr=xerr,
                fmt='o', ecolor='g', capthick=2)
    ax.set_title('Mixed sym., log y')

    fig.suptitle('Variable errorbars')
    
    plt.show()
    return [start,stop,step]


if __name__ == "__main__":
    x = np.arange(0, 5, 0.1)
    y = np.sin(x)
    plt.plot(x, y)
    plt.show()

  
