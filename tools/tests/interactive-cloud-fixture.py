"""Loopback-only production-server fixture; all data belongs to this test run."""
import importlib.util
import os
from pathlib import Path
import ssl
import sys
import json
import threading
from http.server import ThreadingHTTPServer

root = Path(__file__).resolve().parents[2]
output = Path(sys.argv[1]).resolve()
output.mkdir(parents=True, exist_ok=True)
os.environ.update(PAYKITODO_CLOUD_DB=str(output / "cloud.sqlite3"),
                  PAYKITODO_CLOUD_HOST="127.0.0.1", PAYKITODO_CLOUD_PORT="19443",
                  PAYKITODO_PASSWORD_PEPPER="interactive-fixture-only",
                  PAYKITODO_TURNSTILE_SITE_KEY="", PAYKITODO_TURNSTILE_SECRET_KEY="",
                  PAYKITODO_ADMIN_USER="", PAYKITODO_ADMIN_PASSWORD="")
spec = importlib.util.spec_from_file_location("fixture_cloud", root / "server/paykitodo_cloud_server.py")
module = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = module
spec.loader.exec_module(module)
module.DB.initialize()
state = {"outage": False, "holdUpload": False, "holdDownload": False, "entered": False}
release = threading.Event()


class FixtureHandler(module.PaykiTodoCloudHandler):
    def do_POST(self):
        if self.path == "/fixture/control":
            values = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))))
            if values.get("release"):
                release.set()
            else:
                release.clear()
                state.update(entered=False)
            state.update({key: value for key, value in values.items() if key in state})
            self.send_json(200, state)
            return
        super().do_POST()

    def do_GET(self):
        if self.path == "/fixture/state":
            self.send_json(200, state)
            return
        if self.path.startswith("/v2/sync/") and state["outage"]:
            self.send_json(503, {"message": "local fixture network outage"})
            return
        if self.path == "/v2/sync/snapshot" and state["holdDownload"]:
            state["entered"] = True
            release.wait(20)
        super().do_GET()

    def do_PUT(self):
        if self.path == "/v2/sync/snapshot" and state["holdUpload"]:
            state["entered"] = True
            release.wait(20)
        super().do_PUT()


server = ThreadingHTTPServer(("127.0.0.1", 19443), FixtureHandler)
context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
context.load_cert_chain(output / "localhost.crt", output / "localhost.key")
server.socket = context.wrap_socket(server.socket, server_side=True)
server.daemon_threads = True
server.serve_forever()
