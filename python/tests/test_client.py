from pshell import PShellClient
import socket

ps = PShellClient("http://" + socket.gethostname() + ":8080")
print(ps.get_state())



