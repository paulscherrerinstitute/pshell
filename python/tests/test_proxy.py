from pshell import PShellProxy
import socket

pshell = PShellProxy("http://" + socket.gethostname() + ":8080")
ret = pshell.lscan("ao1", ("ai1", "av1", "wf1"), 0, 40, 100, 0.01, meta=False)
print(ret["ai1"])
