import threading
import time
import sys
import requests
import json

try:
    from sseclient import SSEClient
except:
    SSEClient = None


class SSEReceiver:
    def __init__(self, url, subscribed_events):
        if SSEClient is None:
            raise Exception("sseclient library is not installed: server events are not available")
        self.url = url
        self.events = subscribed_events
        self._lock = threading.Lock()
        self._stop = threading.Event()
        self.session = None
        self.client = None
        self.debug = False
        self._subscribers = {}
        self.thread = threading.Thread(target=self.task, kwargs={})
        self.thread.daemon = True
        self.thread.start()

    def task(self):
        try:
            while not self._stop.is_set():
                try:
                    self.session = requests.Session()
                    self.client = SSEClient(self.url, session=self.session)
                    for msg in self.client:
                        if self.is_closed():
                            break
                        event_name = msg.event or "message"

                        if (self.events is None) or (event_name in self.events):
                            try:
                                data = json.loads(msg.data)
                            except:
                                data = str(msg.data)
                            #if self.debug:
                            #    print (event_name, data)
                            with self._lock:
                                subs = list(self._subscribers.values())

                            for events, callback in subs:
                                if events is None or event_name in events:
                                    try:
                                        callback(event_name, data)
                                    except Exception as e:
                                        if self.debug:
                                            print(f"[SSEManager] Error in callback {callback}: {e}")

                except IOError as e:
                    # print(e)
                    pass
                except:
                    if self.debug:
                        print("Error:", sys.exc_info()[1])
                finally:
                    self._close_client()
                if self.is_closed():
                    break
                else:
                    time.sleep(1.0)
        finally:
            if self.debug:
                print("Exit SSE loop task")

    def subscribe(self, callback, events=None):
        """
        Subscribe to SSE events.

        Args:
            callback: function(event_name, data)
            events: None (all events), str (one event), or list[str] (multiple events)
        """
        if isinstance(events, str):
            events = [events]
        if events is not None:
            events = set(events)

        with self._lock:
            self._subscribers[id(callback)] = (events, callback)

    def unsubscribe(self, callback):
        """Unsubscribe a previously subscribed callback."""
        with self._lock:
            self._subscribers.pop(id(callback), None)

    def wait_events(self, events={}, timeout=-1):
        """Wait any of the events matching the value (value None for any).

        Args:
            events (dict event name->value)
            timeout:
        Returns:
            (event, value) or None if timeout
        """
        rx = {}
        condition = threading.Condition()
        def callback(name, value):
            with condition:
                rx[name] = value
                condition.notify_all()

        self.subscribe(callback,  events.keys())
        try:
            start = time.time()
            with condition:
                while True:
                    for name in events.keys():
                        if name in rx.keys():
                            values, rx_value = events[name], rx[name]
                            if values is not None and type(values) is not list:
                                values = [values]
                            if values is None or rx_value in values:
                                return name,rx_value

                    remaining = None
                    if timeout >= 0:
                        remaining = max(0, timeout - (time.time() - start))
                        if remaining <= 0:
                            return None
                    condition.wait(timeout=remaining)
        finally:
            self.unsubscribe(callback)


    def _close_client(self):
        self.client = None
        """
        if self.client is not None:
            try:
                if hasattr(self.client.resp, "raw"):
                    conn = getattr(self.client.resp.raw, "_connection", None)
                    if conn and hasattr(conn, "sock") and conn.sock:
                        conn.sock.shutdown(2)
                        conn.sock.close()
                self.client.resp.close()
                self.client = None
            except:
                pass
        """
        if self.session is not None:
            try:
                self.session.close()
                self.session = None
            except:
                pass

    def close(self):
        self._stop.set()
        self._close_client()
        if self.debug:
            print("closed")

    def is_closed(self):
        return self._stop.is_set()
