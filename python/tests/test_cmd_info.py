import time

from pshell import PShellClient
import socket
from threading import Thread
import os
from requests import HTTPError
import requests
one_quit = False
ps = PShellClient("http://" + socket.gethostname() + ":8080")
print(ps.get_state())

try:
    ps.eval("1/0")
except requests.HTTPError as e:
    print(str(e))  # ZeroDivisionError: integer division or modulo by zero \n 500 Server Error: Internal Server Error for url: http://ag.local:8080/eval/1/0
    print(e.response.status_code)  # 500
    print(e.response.text)  # ZeroDivisionError: integer division or modulo by zero



def create_task(duration=0.1):
    return ps.start_eval("time.sleep("+str(duration)+")&")

def run(index):
    global one_quit
    print ("Enter: ", os.getpid(), index)
    count = 0
    while (True):
        count = count+1
        id = create_task()
        sts = ps.get_result(id)
        if sts['status'] == "unlaunched":
            break
        if one_quit:
            break
    print("Quit: ", os.getpid(), index, ":", count)
    one_quit = True


run_no = 0
while True:
    run_no = run_no + 1
    one_quit = False
    threads = []
    for index in range(run_no*1000, run_no*1000 + 10):
        thread = Thread(target=run, args=(index,))
        thread.start()
        threads.append(thread)
    while one_quit == False:
        time.sleep(0.1)
    for thread in threads:
        thread.join()
    time.sleep(1.0)

    break