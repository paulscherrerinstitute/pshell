import time

from pshell import PShellClient, TimeoutException
import socket


with PShellClient("http://" + socket.gethostname() + ":8080") as ps:
    print(ps.get_state())


    #ps.eval_then("print 'OK'")
    #ps.eval_then("run('test_scan2'),{'a':2}")
    #print(ps.get_state())


    def on_event(name, value):
        print(f"--> event: {name}={value}")
        #if name == "state":
        #    print ("State: ",  value)
        #elif name == "run":
        #    print ("Run: ", value)

    ps.subscribe(on_event, ["state", "run"])
    #print(ps.run("test_scan2", background=True))
    #print (ps.run("test_scan2"))
    #print (ps.run("test_scan2", background=True))
    #ps.wait_state("Ready")
    #print (ps.start_run("test_scan2"))
    print (ps.start_run("test_scan2", background=True))

    #while True:
    #    ev_name, ev_val = ps.wait_events({"state":None, "run":None})
    #    print (ev_name, ev_val)


    id = ps.start_eval("run('test_scan2',{'a':2})")
    ps.wait_state("Busy", 0.5)

    ps.wait_state("Ready")

    ps.start_run('test_scan2', {'a': 2})
    #ps.wait_events({"state": None})
    ps.wait_state("Busy")
    #ps.eval_then("print ('Success')")
    #ps.run_then("test_sleep", {'a': 2, 'b': "asdasd", 'c':True, 'd': 3.14})
    ps.run_then("test_sleep", [1, 3.12, False, "asdads"], on_success=True)

    try:
        ev_name,ev_val = ps.wait_events({"state":["Ready", "Error"], "run": ["test", "test_sleep"]}, timeout=5.0)
        if (ev_name, ev_val) == ("run","test_sleep"):
            ps.eval_then("print ('Success'); set_status ('Success')")
    except TimeoutException as e:
        print(e)

    ev_name,ev_val =ps.wait_events({"shell": 'Success'}, timeout=5.0)
    #ps.wait_state_not("Busy")
    print (ev_name,ev_val)
    ev_name, ev_val = ps.wait_events({"state": None}, timeout=5.0)

time.sleep(1.0)


