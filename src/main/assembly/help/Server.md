# Server

There are multiple ways to remote accessing the software.


 1. A REST interface provide generic access to PShell features. 
This functionality is enabled in menu "File"-"Configuration", items "Server Enabled" and "Server Port".
After change in configuration the application must be restarted. 
    The root for the REST interface is:

    ```
        http://[HOST]:[PORT]
    ```
    A Python client for the REST interface is available at:

    __https://github.com/paulscherrerinstitute/pshell/blob/master/src/main/python/PShellClient/PShellClient.py__



 2. A standard web client in included, which provides a user web interface to PShell through the REST interface.

    The URL of the initial page is:

    ```
        http://[HOST]:[PORT]/static/
    ```

 3. A raw TCP connection on a "terminal" port is enabled in menu "File"-"Configuration", items "Terminal Enabled" and "Terminal Port".
This server accepts commands with the same syntax as the console window.

    ```
        [HOST]:[TERMINAL_PORT]
    ```


 4. A ZMQ based server that provides access do the generated data, in a request-reply pattern. 
Each request is a full path data identifier. 

    Client code example:

    ```
        org.zeromq.ZMQ.Context context = org.zeromq.ZMQ.context(1);
        org.zeromq.ZMQ.Socket requester = context.socket(org.zeromq.ZMQ.REQ);
        requester.connect("tcp://localhost:5573");
        requester.send("2016_02/20160218/20160218_153900_test1 | scan 1/arr");
        Object rec = requester.recvStr();
        System.out.println(rec);       
        requester.close();
        context.term();
    ```

 5. A ZMQ based server stream our scan data in a publisher-subscriber pattern.

    Client code example:

    ```
        org.zeromq.ZMQ.Context context = org.zeromq.ZMQ.context(1);
        org.zeromq.ZMQ.Socket subscriber = context.socket(org.zeromq.ZMQ.SUB);
        subscriber.connect("tcp://localhost:5563");
        subscriber.subscribe(ScanStreamer.ENVELOPE_START.getBytes());
        subscriber.subscribe(ScanStreamer.ENVELOPE_RECORD.getBytes());
        subscriber.subscribe(ScanStreamer.ENVELOPE_END.getBytes());
        while (!Thread.currentThread().isInterrupted()) {
            String type = subscriber.recvStr();
            String contents = subscriber.recvStr();
            System.out.println(type + " : " + contents);
        }
        subscriber.close();
        context.term();
    ```

__Note__: Remote access rights can be defined in  the user management window.



