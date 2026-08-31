#!/usr/bin/env python3
"""Small HTTPS-behind-Nginx relay for encrypted PaykiTodo snapshots.

The service never parses the payload. TLS terminates at Nginx and this process
stores only the encrypted envelope uploaded by the Android client.
"""

from __future__ import annotations

import hashlib
import os
import re
import tempfile
import threading
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit


MAX_PAYLOAD_BYTES = 32 * 1024 * 1024
DEVICE_PATTERN = re.compile(r"^[A-Za-z0-9_-]{1,128}$")
DATA_DIR = Path(os.environ.get("PAYKITODO_SYNC_DATA", "/home/azureuser/.local/share/paykitodo-sync"))
LOCK = threading.Lock()


def device_name(raw: str) -> str | None:
    value = unquote(raw)
    return value if DEVICE_PATTERN.fullmatch(value) else None


def device_paths(device_id: str) -> tuple[Path, Path]:
    return DATA_DIR / f"{device_id}.bin", DATA_DIR / f"{device_id}.auth"


def bearer_token(handler: BaseHTTPRequestHandler) -> str:
    value = handler.headers.get("Authorization", "")
    prefix = "Bearer "
    return value[len(prefix):].strip() if value.startswith(prefix) else ""


def auth_hash(secret: str) -> str:
    return hashlib.sha256(secret.encode("utf-8")).hexdigest()


class SyncHandler(BaseHTTPRequestHandler):
    server_version = "PaykiTodoSync/1"

    def do_GET(self) -> None:  # noqa: N802
        path = urlsplit(self.path).path
        if path == "/healthz":
            self.send_json(HTTPStatus.OK, b'{"ok":true}')
            return
        device_id = self.extract_device(path)
        if device_id is None:
            self.send_json(HTTPStatus.NOT_FOUND, b'{"error":"not found"}')
            return
        payload_path, auth_path = device_paths(device_id)
        if not self.authorized(auth_path):
            return
        if not payload_path.is_file():
            self.send_json(HTTPStatus.NOT_FOUND, b'{"error":"no snapshot"}')
            return
        payload = payload_path.read_bytes()
        self.send_bytes(HTTPStatus.OK, payload, "application/octet-stream")

    def do_PUT(self) -> None:  # noqa: N802
        path = urlsplit(self.path).path
        device_id = self.extract_device(path)
        if device_id is None:
            self.send_json(HTTPStatus.NOT_FOUND, b'{"error":"not found"}')
            return
        payload_path, auth_path = device_paths(device_id)
        token = bearer_token(self)
        if not token:
            self.send_json(HTTPStatus.UNAUTHORIZED, b'{"error":"missing authorization"}')
            return
        if not self.authorized(auth_path, token, allow_enrollment=True):
            return
        length_text = self.headers.get("Content-Length", "")
        try:
            length = int(length_text)
        except ValueError:
            self.send_json(HTTPStatus.LENGTH_REQUIRED, b'{"error":"invalid content length"}')
            return
        if length <= 0 or length > MAX_PAYLOAD_BYTES:
            self.send_json(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, b'{"error":"payload too large"}')
            return
        payload = self.rfile.read(length)
        if len(payload) != length or not payload.startswith(b"PKSYNC1"):
            self.send_json(HTTPStatus.BAD_REQUEST, b'{"error":"invalid encrypted envelope"}')
            return

        DATA_DIR.mkdir(parents=True, exist_ok=True)
        with LOCK:
            with tempfile.NamedTemporaryFile(dir=DATA_DIR, prefix=f".{device_id}.", delete=False) as temp:
                temp.write(payload)
                temp.flush()
                os.fsync(temp.fileno())
                temp_name = Path(temp.name)
            os.replace(temp_name, payload_path)
        self.send_json(HTTPStatus.OK, b'{"ok":true}')

    def extract_device(self, path: str) -> str | None:
        prefix = "/v1/sync/"
        if not path.startswith(prefix):
            return None
        suffix = path[len(prefix):]
        if "/" in suffix or not suffix:
            return None
        return device_name(suffix)

    def authorized(self, auth_path: Path, token: str | None = None, allow_enrollment: bool = False) -> bool:
        provided = token if token is not None else bearer_token(self)
        if not provided:
            self.send_json(HTTPStatus.UNAUTHORIZED, b'{"error":"missing authorization"}')
            return False
        expected = auth_path.read_text(encoding="ascii").strip() if auth_path.is_file() else ""
        if not expected and allow_enrollment:
            DATA_DIR.mkdir(parents=True, exist_ok=True)
            with LOCK:
                if not auth_path.exists():
                    auth_path.write_text(auth_hash(provided) + "\n", encoding="ascii")
                expected = auth_path.read_text(encoding="ascii").strip()
        if expected != auth_hash(provided):
            self.send_json(HTTPStatus.UNAUTHORIZED, b'{"error":"invalid authorization"}')
            return False
        return True

    def send_json(self, status: HTTPStatus, body: bytes) -> None:
        self.send_bytes(status, body, "application/json")

    def send_bytes(self, status: HTTPStatus, body: bytes, content_type: str) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args: object) -> None:
        # Keep access logs free of Authorization headers and payload content.
        super().log_message("%s", format % args)


def main() -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    server = ThreadingHTTPServer(("127.0.0.1", int(os.environ.get("PAYKITODO_SYNC_PORT", "36568"))), SyncHandler)
    server.daemon_threads = True
    server.serve_forever()


if __name__ == "__main__":
    main()
