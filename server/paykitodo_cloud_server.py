#!/usr/bin/env python3
"""PaykiTodo account, encrypted snapshot sync, and administrator service.

The service is intended to listen on loopback behind an HTTPS reverse proxy.
Clients keep the authoritative working copy locally and upload an encrypted
PaykiTodo migration archive. The server never decrypts snapshot payloads.
"""

from __future__ import annotations

import base64
from contextlib import contextmanager
import hashlib
import hmac
import html
import json
import os
import re
import secrets
import sqlite3
import threading
import time
import uuid
from dataclasses import dataclass
from http import HTTPStatus
from http.cookies import SimpleCookie
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, quote, urlsplit


MAX_JSON_BYTES = 128 * 1024
MAX_SNAPSHOT_BYTES = 32 * 1024 * 1024
ACCESS_TOKEN_SECONDS = 15 * 60
REFRESH_TOKEN_SECONDS = 30 * 24 * 60 * 60
USERNAME_PATTERN = re.compile(r"^[A-Za-z0-9_.-]{3,64}$")
DEVICE_ID_PATTERN = re.compile(r"^[A-Za-z0-9_-]{8,128}$")
OPERATION_ID_PATTERN = re.compile(r"^[A-Za-z0-9_.:-]{8,160}$")
RECOVERY_CODE_PATTERN = re.compile(r"^[A-F0-9]{16}$")
ADMIN_WEB_SESSION_SECONDS = 8 * 60 * 60
DB_PATH = Path(os.environ.get("PAYKITODO_CLOUD_DB", "./paykitodo-cloud.sqlite3"))
LISTEN_HOST = os.environ.get("PAYKITODO_CLOUD_HOST", "127.0.0.1")
LISTEN_PORT = int(os.environ.get("PAYKITODO_CLOUD_PORT", "18766"))
ADMIN_USER = os.environ.get("PAYKITODO_ADMIN_USER", "")
ADMIN_PASSWORD = os.environ.get("PAYKITODO_ADMIN_PASSWORD", "")
PASSWORD_PEPPER = os.environ.get("PAYKITODO_PASSWORD_PEPPER", "").encode("utf-8")
ADMIN_WEB_SESSIONS: dict[str, tuple[int, str]] = {}
ADMIN_WEB_SESSIONS_LOCK = threading.Lock()


def now_seconds() -> int:
    return int(time.time())


def token_hash(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def password_hash(password: str, salt: bytes) -> bytes:
    material = password.encode("utf-8") + PASSWORD_PEPPER
    return hashlib.scrypt(material, salt=salt, n=2**14, r=8, p=1, dklen=32)


def generate_recovery_code() -> str:
    raw = secrets.token_hex(8).upper()
    return "-".join(raw[index:index + 4] for index in range(0, len(raw), 4))


def canonical_recovery_code(value: str) -> str:
    return re.sub(r"[-\s]", "", value).upper()


def canonical_username(value: str) -> str:
    return value.strip().lower()


def public_user(row: sqlite3.Row) -> dict[str, object]:
    return {
        "id": row["id"],
        "username": row["username"],
        "displayName": row["display_name"],
        "createdAt": row["created_at"],
        "lastLoginAt": row["last_login_at"],
        "disabled": bool(row["disabled"]),
    }


class RateLimiter:
    def __init__(self) -> None:
        self._attempts: dict[str, list[float]] = {}
        self._lock = threading.Lock()

    def allow(self, key: str, limit: int, window_seconds: int) -> bool:
        cutoff = time.monotonic() - window_seconds
        with self._lock:
            attempts = [stamp for stamp in self._attempts.get(key, []) if stamp >= cutoff]
            if len(attempts) >= limit:
                self._attempts[key] = attempts
                return False
            attempts.append(time.monotonic())
            self._attempts[key] = attempts
            return True


RATE_LIMITER = RateLimiter()


class CloudDatabase:
    def __init__(self, path: Path) -> None:
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.initialize()

    def connect(self) -> sqlite3.Connection:
        connection = sqlite3.connect(self.path, timeout=15, isolation_level=None)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA foreign_keys = ON")
        connection.execute("PRAGMA busy_timeout = 15000")
        return connection

    @contextmanager
    def session(self):
        connection = self.connect()
        try:
            yield connection
        finally:
            connection.close()

    def initialize(self) -> None:
        with self.session() as connection:
            connection.executescript(
                """
                PRAGMA journal_mode = WAL;
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    display_name TEXT NOT NULL,
                    password_salt BLOB NOT NULL,
                    password_hash BLOB NOT NULL,
                    recovery_salt BLOB,
                    recovery_hash BLOB,
                    created_at INTEGER NOT NULL,
                    last_login_at INTEGER,
                    disabled INTEGER NOT NULL DEFAULT 0
                );
                CREATE TABLE IF NOT EXISTS devices (
                    id TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    name TEXT NOT NULL,
                    platform TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    last_seen_at INTEGER NOT NULL,
                    revoked_at INTEGER
                );
                CREATE INDEX IF NOT EXISTS idx_devices_user ON devices(user_id, last_seen_at DESC);
                CREATE TABLE IF NOT EXISTS sessions (
                    access_hash TEXT PRIMARY KEY,
                    refresh_hash TEXT NOT NULL UNIQUE,
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    device_id TEXT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
                    access_expires_at INTEGER NOT NULL,
                    refresh_expires_at INTEGER NOT NULL,
                    session_expires_at INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                );
                CREATE INDEX IF NOT EXISTS idx_sessions_refresh ON sessions(refresh_hash);
                CREATE TABLE IF NOT EXISTS snapshots (
                    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                    revision INTEGER NOT NULL,
                    payload BLOB NOT NULL,
                    payload_sha256 TEXT NOT NULL,
                    updated_at INTEGER NOT NULL,
                    updated_by_device TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS operations (
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    operation_id TEXT NOT NULL,
                    resulting_revision INTEGER NOT NULL,
                    payload_sha256 TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    PRIMARY KEY (user_id, operation_id)
                );
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    remote_ip TEXT NOT NULL,
                    detail TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                );
                CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at DESC);
                """
            )
            columns = {row["name"] for row in connection.execute("PRAGMA table_info(users)").fetchall()}
            if "recovery_salt" not in columns:
                connection.execute("ALTER TABLE users ADD COLUMN recovery_salt BLOB")
            if "recovery_hash" not in columns:
                connection.execute("ALTER TABLE users ADD COLUMN recovery_hash BLOB")
            session_columns = {row["name"] for row in connection.execute("PRAGMA table_info(sessions)").fetchall()}
            if "session_expires_at" not in session_columns:
                connection.execute("ALTER TABLE sessions ADD COLUMN session_expires_at INTEGER")
                connection.execute("UPDATE sessions SET session_expires_at=refresh_expires_at WHERE session_expires_at IS NULL")


DB = CloudDatabase(DB_PATH)


@dataclass(frozen=True)
class AuthenticatedSession:
    user_id: int
    username: str
    device_id: str


class ApiError(Exception):
    def __init__(self, status: HTTPStatus, code: str, message: str) -> None:
        super().__init__(message)
        self.status = status
        self.code = code
        self.message = message


class PaykiTodoCloudHandler(BaseHTTPRequestHandler):
    server_version = "PaykiTodoCloud/2"

    def do_GET(self) -> None:  # noqa: N802
        self.dispatch("GET")

    def do_POST(self) -> None:  # noqa: N802
        self.dispatch("POST")

    def do_PUT(self) -> None:  # noqa: N802
        self.dispatch("PUT")

    def do_DELETE(self) -> None:  # noqa: N802
        self.dispatch("DELETE")

    def dispatch(self, method: str) -> None:
        try:
            path = urlsplit(self.path).path.rstrip("/") or "/"
            if method == "GET" and path == "/":
                self.redirect("/admin")
            elif method == "GET" and path == "/healthz":
                self.send_json(HTTPStatus.OK, {"ok": True, "service": "paykitodo-cloud", "apiVersion": 2})
            elif path == "/v2/auth/register" and method == "POST":
                self.register()
            elif path == "/v2/auth/login" and method == "POST":
                self.login()
            elif path == "/v2/auth/recover" and method == "POST":
                self.recover_password()
            elif path == "/v2/auth/refresh" and method == "POST":
                self.refresh_session()
            elif path == "/v2/auth/logout" and method == "POST":
                self.logout()
            elif path == "/v2/account" and method == "GET":
                self.account()
            elif path == "/v2/devices" and method == "GET":
                self.devices()
            elif path.startswith("/v2/devices/") and method == "DELETE":
                self.revoke_device(path.rsplit("/", 1)[-1])
            elif path == "/v2/sync/status" and method == "GET":
                self.sync_status()
            elif path == "/v2/sync/snapshot" and method == "GET":
                self.download_snapshot()
            elif path == "/v2/sync/snapshot" and method == "PUT":
                self.upload_snapshot()
            elif path == "/admin" and method == "GET":
                self.admin_page()
            elif path == "/admin/login" and method == "POST":
                self.admin_login()
            elif path == "/admin/logout" and method == "POST":
                self.admin_logout()
            elif path.startswith("/admin/users/") and path.endswith("/toggle") and method == "POST":
                self.admin_toggle_user(path.split("/")[3])
            elif path.startswith("/admin/devices/") and path.endswith("/revoke") and method == "POST":
                self.admin_revoke_device(path.split("/")[3])
            else:
                raise ApiError(HTTPStatus.NOT_FOUND, "not_found", "接口不存在")
        except ApiError as error:
            self.send_json(error.status, {"error": error.code, "message": error.message})
        except (BrokenPipeError, ConnectionResetError):
            return
        except Exception as error:  # Keep internal details out of public responses.
            self.log_error("Unhandled request error: %r", error)
            self.send_json(HTTPStatus.INTERNAL_SERVER_ERROR, {"error": "internal_error", "message": "服务器处理失败"})

    @property
    def remote_ip(self) -> str:
        forwarded = self.headers.get("X-Forwarded-For", "").split(",", 1)[0].strip()
        return forwarded or self.client_address[0]

    def read_json(self) -> dict[str, object]:
        raw = self.read_body(MAX_JSON_BYTES)
        try:
            value = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_json", "请求 JSON 无效") from error
        if not isinstance(value, dict):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_json", "请求必须是 JSON 对象")
        return value

    def read_body(self, limit: int) -> bytes:
        try:
            length = int(self.headers.get("Content-Length", ""))
        except ValueError as error:
            raise ApiError(HTTPStatus.LENGTH_REQUIRED, "invalid_length", "Content-Length 无效") from error
        if length <= 0:
            raise ApiError(HTTPStatus.BAD_REQUEST, "empty_body", "请求内容不能为空")
        if length > limit:
            raise ApiError(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, "payload_too_large", "请求内容过大")
        payload = self.rfile.read(length)
        if len(payload) != length:
            raise ApiError(HTTPStatus.BAD_REQUEST, "incomplete_body", "请求内容不完整")
        return payload

    def register(self) -> None:
        if not RATE_LIMITER.allow(f"register:{self.remote_ip}", 8, 15 * 60):
            raise ApiError(HTTPStatus.TOO_MANY_REQUESTS, "rate_limited", "注册请求过于频繁")
        body = self.read_json()
        username, display_name, password, device_id, device_name, platform = self.validate_credentials(body)
        salt = secrets.token_bytes(16)
        recovery_code = generate_recovery_code()
        recovery_salt = secrets.token_bytes(16)
        timestamp = now_seconds()
        try:
            with DB.session() as connection:
                connection.execute("BEGIN IMMEDIATE")
                cursor = connection.execute(
                    "INSERT INTO users(username, display_name, password_salt, password_hash, recovery_salt, recovery_hash, created_at) VALUES(?,?,?,?,?,?,?)",
                    (username, display_name, salt, password_hash(password, salt), recovery_salt, password_hash(canonical_recovery_code(recovery_code), recovery_salt), timestamp),
                )
                user_id = int(cursor.lastrowid)
                connection.execute(
                    "INSERT INTO devices(id,user_id,name,platform,created_at,last_seen_at) VALUES(?,?,?,?,?,?)",
                    (device_id, user_id, device_name, platform, timestamp, timestamp),
                )
                tokens = self.create_session(connection, user_id, device_id)
                self.audit(connection, user_id, "register", device_name)
                connection.commit()
        except sqlite3.IntegrityError as error:
            raise ApiError(HTTPStatus.CONFLICT, "username_exists", "该账号已存在") from error
        self.send_json(HTTPStatus.CREATED, {**tokens, "user": {"username": username, "displayName": display_name}, "deviceId": device_id, "snapshotRevision": 0, "recoveryCode": recovery_code})

    def login(self) -> None:
        if not RATE_LIMITER.allow(f"login:{self.remote_ip}", 12, 10 * 60):
            raise ApiError(HTTPStatus.TOO_MANY_REQUESTS, "rate_limited", "登录尝试过于频繁")
        body = self.read_json()
        username, _, password, device_id, device_name, platform = self.validate_credentials(body, registering=False)
        timestamp = now_seconds()
        with DB.session() as connection:
            row = connection.execute("SELECT * FROM users WHERE username = ?", (username,)).fetchone()
            if row is None or not hmac.compare_digest(bytes(row["password_hash"]), password_hash(password, bytes(row["password_salt"]))):
                raise ApiError(HTTPStatus.UNAUTHORIZED, "invalid_credentials", "账号或密码错误")
            if row["disabled"]:
                raise ApiError(HTTPStatus.FORBIDDEN, "account_disabled", "账号已被停用")
            connection.execute("BEGIN IMMEDIATE")
            existing = connection.execute("SELECT user_id FROM devices WHERE id = ?", (device_id,)).fetchone()
            if existing is not None and int(existing["user_id"]) != int(row["id"]):
                device_id = uuid.uuid4().hex
            connection.execute(
                "INSERT INTO devices(id,user_id,name,platform,created_at,last_seen_at,revoked_at) VALUES(?,?,?,?,?,?,NULL) "
                "ON CONFLICT(id) DO UPDATE SET name=excluded.name,platform=excluded.platform,last_seen_at=excluded.last_seen_at,revoked_at=NULL",
                (device_id, row["id"], device_name, platform, timestamp, timestamp),
            )
            connection.execute("UPDATE users SET last_login_at = ? WHERE id = ?", (timestamp, row["id"]))
            recovery_code = None
            if row["recovery_hash"] is None:
                recovery_code = generate_recovery_code()
                recovery_salt = secrets.token_bytes(16)
                connection.execute(
                    "UPDATE users SET recovery_salt=?, recovery_hash=? WHERE id=?",
                    (recovery_salt, password_hash(canonical_recovery_code(recovery_code), recovery_salt), row["id"]),
                )
            tokens = self.create_session(connection, int(row["id"]), device_id)
            revision_row = connection.execute("SELECT revision FROM snapshots WHERE user_id = ?", (row["id"],)).fetchone()
            self.audit(connection, int(row["id"]), "login", f"{platform}:{device_name}")
            connection.commit()
        response = {**tokens, "user": {"username": username, "displayName": row["display_name"]}, "deviceId": device_id, "snapshotRevision": int(revision_row["revision"]) if revision_row else 0}
        if recovery_code is not None:
            response["recoveryCode"] = recovery_code
        self.send_json(HTTPStatus.OK, response)

    def recover_password(self) -> None:
        if not RATE_LIMITER.allow(f"recover:{self.remote_ip}", 8, 15 * 60):
            raise ApiError(HTTPStatus.TOO_MANY_REQUESTS, "rate_limited", "找回密码请求过于频繁")
        body = self.read_json()
        username = canonical_username(str(body.get("username", "")))
        recovery_code = canonical_recovery_code(str(body.get("recoveryCode", "")))
        new_password = str(body.get("newPassword", ""))
        if not USERNAME_PATTERN.fullmatch(username):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_username", "账号格式无效")
        if not RECOVERY_CODE_PATTERN.fullmatch(recovery_code):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_recovery_code", "恢复码格式无效")
        if len(new_password) < 10 or len(new_password) > 256:
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_password", "新密码至少 10 位且不超过 256 位")
        with DB.session() as connection:
            connection.execute("BEGIN IMMEDIATE")
            row = connection.execute(
                "SELECT id,recovery_salt,recovery_hash,disabled FROM users WHERE username=?",
                (username,),
            ).fetchone()
            valid = (
                row is not None
                and not row["disabled"]
                and row["recovery_salt"] is not None
                and row["recovery_hash"] is not None
                and hmac.compare_digest(
                    bytes(row["recovery_hash"]),
                    password_hash(recovery_code, bytes(row["recovery_salt"])),
                )
            )
            if not valid:
                connection.rollback()
                raise ApiError(HTTPStatus.UNAUTHORIZED, "invalid_recovery", "账号或恢复码错误")
            salt = secrets.token_bytes(16)
            connection.execute(
                "UPDATE users SET password_salt=?,password_hash=? WHERE id=?",
                (salt, password_hash(new_password, salt), row["id"]),
            )
            connection.execute("DELETE FROM sessions WHERE user_id=?", (row["id"],))
            self.audit(connection, int(row["id"]), "password_recovered", "password reset with recovery code")
            connection.commit()
        self.send_json(HTTPStatus.OK, {"ok": True, "message": "密码已重置，请使用新密码登录"})

    def refresh_session(self) -> None:
        body = self.read_json()
        refresh_token = str(body.get("refreshToken", "")).strip()
        if not refresh_token:
            raise ApiError(HTTPStatus.BAD_REQUEST, "missing_refresh_token", "缺少刷新令牌")
        timestamp = now_seconds()
        with DB.session() as connection:
            row = connection.execute(
                "SELECT s.*,u.disabled,d.revoked_at FROM sessions s JOIN users u ON u.id=s.user_id "
                "JOIN devices d ON d.id=s.device_id WHERE s.refresh_hash=?",
                (token_hash(refresh_token),),
            ).fetchone()
            if row is None or row["refresh_expires_at"] <= timestamp or row["disabled"] or row["revoked_at"] is not None:
                raise ApiError(HTTPStatus.UNAUTHORIZED, "invalid_refresh_token", "登录已过期，请重新登录")
            connection.execute("BEGIN IMMEDIATE")
            connection.execute("DELETE FROM sessions WHERE refresh_hash=?", (row["refresh_hash"],))
            tokens = self.create_session(
                connection,
                int(row["user_id"]),
                row["device_id"],
                int(row["session_expires_at"]),
            )
            connection.execute("UPDATE devices SET last_seen_at=? WHERE id=?", (timestamp, row["device_id"]))
            connection.commit()
        self.send_json(HTTPStatus.OK, tokens)

    def logout(self) -> None:
        token = self.bearer_token()
        if token:
            with DB.session() as connection:
                connection.execute("DELETE FROM sessions WHERE access_hash=?", (token_hash(token),))
        self.send_json(HTTPStatus.OK, {"ok": True})

    def account(self) -> None:
        session = self.require_session()
        with DB.session() as connection:
            row = connection.execute("SELECT * FROM users WHERE id=?", (session.user_id,)).fetchone()
            revision = connection.execute("SELECT revision FROM snapshots WHERE user_id=?", (session.user_id,)).fetchone()
        self.send_json(HTTPStatus.OK, {"user": public_user(row), "deviceId": session.device_id, "snapshotRevision": int(revision["revision"]) if revision else 0})

    def devices(self) -> None:
        session = self.require_session()
        with DB.session() as connection:
            rows = connection.execute(
                "SELECT id,name,platform,created_at,last_seen_at,revoked_at FROM devices WHERE user_id=? ORDER BY last_seen_at DESC",
                (session.user_id,),
            ).fetchall()
        self.send_json(HTTPStatus.OK, {"devices": [{"id": row["id"], "name": row["name"], "platform": row["platform"], "createdAt": row["created_at"], "lastSeenAt": row["last_seen_at"], "revoked": row["revoked_at"] is not None, "current": row["id"] == session.device_id} for row in rows]})

    def revoke_device(self, device_id: str) -> None:
        session = self.require_session()
        if not DEVICE_ID_PATTERN.fullmatch(device_id):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_device", "设备 ID 无效")
        with DB.session() as connection:
            connection.execute("BEGIN IMMEDIATE")
            cursor = connection.execute("UPDATE devices SET revoked_at=? WHERE id=? AND user_id=?", (now_seconds(), device_id, session.user_id))
            if cursor.rowcount == 0:
                raise ApiError(HTTPStatus.NOT_FOUND, "device_not_found", "设备不存在")
            connection.execute("DELETE FROM sessions WHERE device_id=? AND user_id=?", (device_id, session.user_id))
            self.audit(connection, session.user_id, "device_revoke", device_id)
            connection.commit()
        self.send_json(HTTPStatus.OK, {"ok": True})

    def sync_status(self) -> None:
        session = self.require_session()
        with DB.session() as connection:
            row = connection.execute("SELECT revision,updated_at,updated_by_device,length(payload) AS payload_bytes FROM snapshots WHERE user_id=?", (session.user_id,)).fetchone()
        self.send_json(HTTPStatus.OK, {"revision": int(row["revision"]) if row else 0, "updatedAt": int(row["updated_at"]) if row else None, "updatedByDevice": row["updated_by_device"] if row else None, "payloadBytes": int(row["payload_bytes"]) if row else 0})

    def download_snapshot(self) -> None:
        session = self.require_session()
        with DB.session() as connection:
            row = connection.execute("SELECT * FROM snapshots WHERE user_id=?", (session.user_id,)).fetchone()
        if row is None:
            raise ApiError(HTTPStatus.NOT_FOUND, "snapshot_not_found", "云端暂无数据")
        self.send_bytes(HTTPStatus.OK, bytes(row["payload"]), "application/octet-stream", {"ETag": f'"{row["revision"]}"', "X-Payki-Revision": str(row["revision"]), "X-Payki-Updated-At": str(row["updated_at"])})

    def upload_snapshot(self) -> None:
        session = self.require_session()
        payload = self.read_body(MAX_SNAPSHOT_BYTES)
        if not payload.startswith(b"PKSYNC1"):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_snapshot", "快照加密格式无效")
        operation_id = self.headers.get("X-Payki-Operation-Id", "").strip()
        if not OPERATION_ID_PATTERN.fullmatch(operation_id):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_operation_id", "缺少有效的幂等操作 ID")
        match_header = self.headers.get("If-Match", "").strip().strip('"')
        try:
            expected_revision = int(match_header)
        except ValueError as error:
            raise ApiError(HTTPStatus.PRECONDITION_REQUIRED, "revision_required", "上传必须携带 If-Match 版本号") from error
        digest = hashlib.sha256(payload).hexdigest()
        timestamp = now_seconds()
        with DB.session() as connection:
            connection.execute("BEGIN IMMEDIATE")
            previous_operation = connection.execute("SELECT resulting_revision,payload_sha256 FROM operations WHERE user_id=? AND operation_id=?", (session.user_id, operation_id)).fetchone()
            if previous_operation is not None:
                if previous_operation["payload_sha256"] != digest:
                    raise ApiError(HTTPStatus.CONFLICT, "operation_reused", "同一操作 ID 不能上传不同内容")
                connection.commit()
                self.send_json(HTTPStatus.OK, {"ok": True, "revision": int(previous_operation["resulting_revision"]), "idempotent": True})
                return
            current = connection.execute("SELECT revision FROM snapshots WHERE user_id=?", (session.user_id,)).fetchone()
            current_revision = int(current["revision"]) if current else 0
            if expected_revision != current_revision:
                connection.rollback()
                self.send_json(HTTPStatus.CONFLICT, {"error": "revision_conflict", "message": "云端已有更新，请先同步", "currentRevision": current_revision})
                return
            next_revision = current_revision + 1
            connection.execute(
                "INSERT INTO snapshots(user_id,revision,payload,payload_sha256,updated_at,updated_by_device) VALUES(?,?,?,?,?,?) "
                "ON CONFLICT(user_id) DO UPDATE SET revision=excluded.revision,payload=excluded.payload,payload_sha256=excluded.payload_sha256,updated_at=excluded.updated_at,updated_by_device=excluded.updated_by_device",
                (session.user_id, next_revision, payload, digest, timestamp, session.device_id),
            )
            connection.execute("INSERT INTO operations(user_id,operation_id,resulting_revision,payload_sha256,created_at) VALUES(?,?,?,?,?)", (session.user_id, operation_id, next_revision, digest, timestamp))
            connection.execute("DELETE FROM operations WHERE user_id=? AND created_at<?", (session.user_id, timestamp - 7 * 24 * 60 * 60))
            self.audit(connection, session.user_id, "snapshot_upload", f"revision={next_revision};bytes={len(payload)}")
            connection.commit()
        self.send_json(HTTPStatus.OK, {"ok": True, "revision": next_revision, "updatedAt": timestamp})

    def validate_credentials(self, body: dict[str, object], registering: bool = True) -> tuple[str, str, str, str, str, str]:
        username = canonical_username(str(body.get("username", "")))
        display_name = str(body.get("displayName", username)).strip()[:80] or username
        password = str(body.get("password", ""))
        device_id = str(body.get("deviceId", "")).strip() or uuid.uuid4().hex
        device_name = str(body.get("deviceName", "设备")).strip()[:100] or "设备"
        platform = str(body.get("platform", "unknown")).strip().lower()[:32] or "unknown"
        if not USERNAME_PATTERN.fullmatch(username):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_username", "账号需为 3-64 位字母、数字、点、横线或下划线")
        minimum = 10 if registering else 1
        if len(password) < minimum or len(password) > 256:
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_password", "密码至少 10 位且不超过 256 位")
        if not DEVICE_ID_PATTERN.fullmatch(device_id):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_device", "设备 ID 无效")
        return username, display_name, password, device_id, device_name, platform

    def create_session(
        self,
        connection: sqlite3.Connection,
        user_id: int,
        device_id: str,
        session_expires_at: int | None = None,
    ) -> dict[str, object]:
        access_token = secrets.token_urlsafe(32)
        refresh_token = secrets.token_urlsafe(48)
        timestamp = now_seconds()
        connection.execute("DELETE FROM sessions WHERE device_id=?", (device_id,))
        session_deadline = session_expires_at or (timestamp + REFRESH_TOKEN_SECONDS)
        access_deadline = min(timestamp + ACCESS_TOKEN_SECONDS, session_deadline)
        connection.execute(
            "INSERT INTO sessions(access_hash,refresh_hash,user_id,device_id,access_expires_at,refresh_expires_at,session_expires_at,created_at) VALUES(?,?,?,?,?,?,?,?)",
            (token_hash(access_token), token_hash(refresh_token), user_id, device_id, access_deadline, session_deadline, session_deadline, timestamp),
        )
        return {"accessToken": access_token, "accessExpiresAt": access_deadline, "refreshToken": refresh_token, "refreshExpiresAt": session_deadline}

    def bearer_token(self) -> str:
        value = self.headers.get("Authorization", "")
        return value[7:].strip() if value.startswith("Bearer ") else ""

    def require_session(self) -> AuthenticatedSession:
        token = self.bearer_token()
        if not token:
            raise ApiError(HTTPStatus.UNAUTHORIZED, "missing_token", "请先登录")
        timestamp = now_seconds()
        with DB.session() as connection:
            row = connection.execute(
                "SELECT s.user_id,s.device_id,u.username,u.disabled,d.revoked_at FROM sessions s "
                "JOIN users u ON u.id=s.user_id JOIN devices d ON d.id=s.device_id "
                "WHERE s.access_hash=? AND s.access_expires_at>?",
                (token_hash(token), timestamp),
            ).fetchone()
            if row is None or row["disabled"] or row["revoked_at"] is not None:
                raise ApiError(HTTPStatus.UNAUTHORIZED, "invalid_token", "登录已过期，请重新登录")
            connection.execute("UPDATE devices SET last_seen_at=? WHERE id=?", (timestamp, row["device_id"]))
        return AuthenticatedSession(int(row["user_id"]), row["username"], row["device_id"])

    def audit(self, connection: sqlite3.Connection, user_id: int | None, action: str, detail: str) -> None:
        connection.execute("INSERT INTO audit_log(user_id,action,remote_ip,detail,created_at) VALUES(?,?,?,?,?)", (user_id, action, self.remote_ip[:80], detail[:500], now_seconds()))

    def require_admin(self) -> bool:
        if not ADMIN_USER or not ADMIN_PASSWORD:
            raise ApiError(HTTPStatus.SERVICE_UNAVAILABLE, "admin_not_configured", "管理员后台尚未配置")
        value = self.headers.get("Authorization", "")
        if value.startswith("Basic "):
            try:
                username, password = base64.b64decode(value[6:]).decode("utf-8").split(":", 1)
            except (ValueError, UnicodeDecodeError):
                return False
            return hmac.compare_digest(username, ADMIN_USER) and hmac.compare_digest(password, ADMIN_PASSWORD)
        return self.admin_session() is not None

    def admin_session(self) -> tuple[str, str] | None:
        cookie = SimpleCookie()
        try:
            cookie.load(self.headers.get("Cookie", ""))
        except ValueError:
            return None
        token = cookie.get("paykitodo_admin_session")
        if token is None or not token.value:
            return None
        now = now_seconds()
        with ADMIN_WEB_SESSIONS_LOCK:
            session = ADMIN_WEB_SESSIONS.get(token.value)
            if session is None:
                return None
            expires_at, csrf = session
            if expires_at <= now:
                ADMIN_WEB_SESSIONS.pop(token.value, None)
                return None
            return token.value, csrf

    def create_admin_session(self) -> tuple[str, str]:
        token = secrets.token_urlsafe(32)
        csrf = secrets.token_urlsafe(32)
        expires_at = now_seconds() + ADMIN_WEB_SESSION_SECONDS
        with ADMIN_WEB_SESSIONS_LOCK:
            ADMIN_WEB_SESSIONS[token] = (expires_at, csrf)
            if len(ADMIN_WEB_SESSIONS) > 256:
                now = now_seconds()
                for key, (expiry, _) in list(ADMIN_WEB_SESSIONS.items()):
                    if expiry <= now:
                        ADMIN_WEB_SESSIONS.pop(key, None)
        return token, csrf

    def remove_admin_session(self) -> None:
        session = self.admin_session()
        if session is not None:
            with ADMIN_WEB_SESSIONS_LOCK:
                ADMIN_WEB_SESSIONS.pop(session[0], None)

    def send_admin_login_page(self, error: str | None = None, status: HTTPStatus = HTTPStatus.OK) -> None:
        error_html = f"<div class='error'>{html.escape(error)}</div>" if error else ""
        body = f"<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width'><title>登录 PaykiTodo 管理后台</title><style>body{{font-family:Segoe UI,Microsoft YaHei,sans-serif;margin:0;min-height:100vh;display:grid;place-items:center;background:#eef2f4;color:#1f2933}}main{{width:min(420px,calc(100vw - 32px));background:#fff;border:1px solid #dfe3e8;border-radius:10px;padding:28px;box-sizing:border-box;box-shadow:0 12px 30px rgba(31,41,51,.08)}}h1{{font-size:22px;margin:0 0 8px}}p{{color:#68737d;font-size:14px;line-height:1.6;margin:0 0 22px}}label{{display:block;font-size:13px;font-weight:600;margin:14px 0 6px}}input{{width:100%;box-sizing:border-box;border:1px solid #c8cfd6;border-radius:6px;padding:11px 12px;font:inherit}}button{{width:100%;margin-top:22px;border:0;border-radius:6px;padding:11px;background:#2f7d5c;color:#fff;font:inherit;font-weight:600;cursor:pointer}}.error{{background:#fff1f0;color:#b42318;border:1px solid #f2b8b5;border-radius:6px;padding:10px;font-size:13px}}</style></head><body><main><h1>PaykiTodo 管理后台</h1><p>请输入管理员账号和密码。这里是独立的 PaykiTodo 管理入口，不是 1Panel。</p>{error_html}<form method='post' action='/admin/login'><label for='username'>管理员账号</label><input id='username' name='username' autocomplete='username' required><label for='password'>管理员密码</label><input id='password' name='password' type='password' autocomplete='current-password' required><button type='submit'>登录管理后台</button></form></main></body></html>"
        self.send_bytes(status, body.encode("utf-8"), "text/html; charset=utf-8")

    def admin_login(self) -> None:
        if not ADMIN_USER or not ADMIN_PASSWORD:
            raise ApiError(HTTPStatus.SERVICE_UNAVAILABLE, "admin_not_configured", "管理员后台尚未配置")
        if not RATE_LIMITER.allow(f"admin-login:{self.remote_ip}", 10, 10 * 60):
            self.send_admin_login_page("登录尝试过于频繁，请稍后再试。", HTTPStatus.TOO_MANY_REQUESTS)
            return
        values = parse_qs(self.read_body(4 * 1024).decode("utf-8", errors="replace"))
        username = values.get("username", [""])[0].strip()
        password = values.get("password", [""])[0]
        if not hmac.compare_digest(username, ADMIN_USER) or not hmac.compare_digest(password, ADMIN_PASSWORD):
            self.send_admin_login_page("管理员账号或密码错误。", HTTPStatus.UNAUTHORIZED)
            return
        token, _ = self.create_admin_session()
        with DB.session() as connection:
            self.audit(connection, None, "admin_login", "web administrator login")
        self.redirect("/admin", {"Set-Cookie": f"paykitodo_admin_session={token}; Path=/admin; Max-Age={ADMIN_WEB_SESSION_SECONDS}; HttpOnly; Secure; SameSite=Strict"})

    def admin_logout(self) -> None:
        self.remove_admin_session()
        self.redirect("/admin", {"Set-Cookie": "paykitodo_admin_session=; Path=/admin; Max-Age=0; HttpOnly; Secure; SameSite=Strict"})

    def admin_csrf_token(self) -> str:
        session = self.admin_session()
        if session is not None:
            return session[1]
        message = time.strftime("%Y-%m-%d", time.gmtime()).encode("ascii")
        return hmac.new(ADMIN_PASSWORD.encode("utf-8"), message, hashlib.sha256).hexdigest()

    def require_admin_csrf(self) -> None:
        body = self.read_body(4 * 1024).decode("utf-8", errors="replace")
        values = parse_qs(body)
        provided = values.get("csrf", [""])[0]
        if not hmac.compare_digest(provided, self.admin_csrf_token()):
            raise ApiError(HTTPStatus.FORBIDDEN, "invalid_csrf", "管理员操作校验失败，请刷新页面后重试")

    def admin_page(self) -> None:
        if not self.require_admin():
            self.send_admin_login_page()
            return
        with DB.session() as connection:
            stats = connection.execute("SELECT (SELECT count(*) FROM users) users,(SELECT count(*) FROM devices WHERE revoked_at IS NULL) devices,(SELECT count(*) FROM snapshots) snapshots,(SELECT COALESCE(sum(length(payload)),0) FROM snapshots) bytes").fetchone()
            users = connection.execute("SELECT u.*,s.revision,s.updated_at,(SELECT count(*) FROM devices d WHERE d.user_id=u.id AND d.revoked_at IS NULL) device_count FROM users u LEFT JOIN snapshots s ON s.user_id=u.id ORDER BY u.created_at DESC LIMIT 500").fetchall()
            devices = connection.execute("SELECT d.*,u.username FROM devices d JOIN users u ON u.id=d.user_id ORDER BY d.last_seen_at DESC LIMIT 500").fetchall()
            audits = connection.execute("SELECT a.*,u.username FROM audit_log a LEFT JOIN users u ON u.id=a.user_id ORDER BY a.created_at DESC LIMIT 80").fetchall()
        body = ["<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width'><title>PaykiTodo 管理后台</title><style>",
                "body{font-family:Segoe UI,Microsoft YaHei,sans-serif;margin:0;background:#f4f6f8;color:#1f2933}header{background:#fff;border-bottom:1px solid #dfe3e8;padding:18px 28px;display:flex;align-items:center;justify-content:space-between;gap:16px}header strong{font-size:19px}header span{color:#68737d;font-size:13px}main{padding:24px 28px;max-width:1400px;margin:auto}.stats{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.card,section{background:#fff;border:1px solid #dfe3e8;border-radius:8px;padding:16px}.card b{display:block;font-size:28px;margin-top:6px}section{margin-top:18px;overflow:auto}table{border-collapse:collapse;width:100%;min-width:820px}th,td{text-align:left;padding:10px;border-bottom:1px solid #edf0f2;font-size:13px}th{color:#5b6570}button{border:1px solid #c8cfd6;background:#fff;border-radius:6px;padding:6px 10px;cursor:pointer}.danger{color:#b42318}.ok{color:#137333}.logout{color:#4b5563;background:#fff}@media(max-width:720px){.stats{grid-template-columns:1fr 1fr}main{padding:16px}header{padding:16px;align-items:flex-start}header span{display:none}}",
                "</style></head><body><header><div><strong>PaykiTodo 管理后台</strong><br><span>用户、设备、同步状态和审计信息</span></div><form method='post' action='/admin/logout'><button class='logout'>退出管理后台</button></form></header><main><div class='stats'>"]
        for label, value in (("注册用户", stats["users"]), ("有效设备", stats["devices"]), ("云端快照", stats["snapshots"]), ("存储占用", f'{stats["bytes"] / 1024 / 1024:.1f} MB')):
            body.append(f"<div class='card'>{html.escape(str(label))}<b>{html.escape(str(value))}</b></div>")
        body.append("</div><section><h2>用户</h2><table><thead><tr><th>账号</th><th>显示名</th><th>状态</th><th>设备</th><th>快照版本</th><th>注册时间</th><th>操作</th></tr></thead><tbody>")
        csrf = self.admin_csrf_token()
        for row in users:
            status = "已停用" if row["disabled"] else "正常"
            action = "启用" if row["disabled"] else "停用"
            body.append(f"<tr><td>{html.escape(row['username'])}</td><td>{html.escape(row['display_name'])}</td><td class={'danger' if row['disabled'] else 'ok'}>{status}</td><td>{row['device_count']}</td><td>{row['revision'] or 0}</td><td>{self.format_time(row['created_at'])}</td><td><form method='post' action='/admin/users/{row['id']}/toggle'><input type='hidden' name='csrf' value='{csrf}'><button>{action}</button></form></td></tr>")
        body.append("</tbody></table></section><section><h2>设备</h2><table><thead><tr><th>账号</th><th>设备名</th><th>平台</th><th>最后活跃</th><th>状态</th><th>操作</th></tr></thead><tbody>")
        for row in devices:
            revoked = row["revoked_at"] is not None
            action = "-" if revoked else f"<form method='post' action='/admin/devices/{html.escape(row['id'])}/revoke'><input type='hidden' name='csrf' value='{csrf}'><button class='danger'>撤销</button></form>"
            body.append(f"<tr><td>{html.escape(row['username'])}</td><td>{html.escape(row['name'])}</td><td>{html.escape(row['platform'])}</td><td>{self.format_time(row['last_seen_at'])}</td><td>{'已撤销' if revoked else '有效'}</td><td>{action}</td></tr>")
        body.append("</tbody></table></section><section><h2>最近操作</h2><table><thead><tr><th>时间</th><th>账号</th><th>动作</th><th>来源</th><th>详情</th></tr></thead><tbody>")
        for row in audits:
            body.append(f"<tr><td>{self.format_time(row['created_at'])}</td><td>{html.escape(row['username'] or '-')}</td><td>{html.escape(row['action'])}</td><td>{html.escape(row['remote_ip'])}</td><td>{html.escape(row['detail'])}</td></tr>")
        body.append("</tbody></table></section></main></body></html>")
        self.send_bytes(HTTPStatus.OK, "".join(body).encode("utf-8"), "text/html; charset=utf-8")

    def admin_toggle_user(self, raw_id: str) -> None:
        if not self.require_admin():
            self.redirect("/admin")
            return
        self.require_admin_csrf()
        try:
            user_id = int(raw_id)
        except ValueError as error:
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_user", "用户 ID 无效") from error
        with DB.session() as connection:
            connection.execute("BEGIN IMMEDIATE")
            row = connection.execute("SELECT disabled FROM users WHERE id=?", (user_id,)).fetchone()
            if row is None:
                raise ApiError(HTTPStatus.NOT_FOUND, "user_not_found", "用户不存在")
            disabled = 0 if row["disabled"] else 1
            connection.execute("UPDATE users SET disabled=? WHERE id=?", (disabled, user_id))
            if disabled:
                connection.execute("DELETE FROM sessions WHERE user_id=?", (user_id,))
            self.audit(connection, user_id, "admin_enable" if not disabled else "admin_disable", "administrator action")
            connection.commit()
        self.redirect("/admin")

    def admin_revoke_device(self, raw_id: str) -> None:
        if not self.require_admin():
            self.redirect("/admin")
            return
        self.require_admin_csrf()
        if not DEVICE_ID_PATTERN.fullmatch(raw_id):
            raise ApiError(HTTPStatus.BAD_REQUEST, "invalid_device", "设备 ID 无效")
        with DB.session() as connection:
            connection.execute("UPDATE devices SET revoked_at=? WHERE id=?", (now_seconds(), raw_id))
            connection.execute("DELETE FROM sessions WHERE device_id=?", (raw_id,))
        self.redirect("/admin")

    def redirect(self, location: str, extra_headers: dict[str, str] | None = None) -> None:
        self.send_response(HTTPStatus.SEE_OTHER)
        self.send_header("Location", location)
        self.send_header("Content-Length", "0")
        for name, value in (extra_headers or {}).items():
            self.send_header(name, value)
        self.end_headers()

    @staticmethod
    def format_time(timestamp: int | None) -> str:
        return time.strftime("%Y-%m-%d %H:%M", time.localtime(timestamp)) if timestamp else "-"

    def send_json(self, status: HTTPStatus, value: dict[str, object]) -> None:
        self.send_bytes(status, json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode("utf-8"), "application/json; charset=utf-8")

    def send_bytes(self, status: HTTPStatus, body: bytes, content_type: str, extra_headers: dict[str, str] | None = None) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        self.send_header("Referrer-Policy", "no-referrer")
        for name, value in (extra_headers or {}).items():
            self.send_header(name, value)
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args: object) -> None:
        super().log_message("%s", format % args)


def main() -> None:
    DB.initialize()
    server = ThreadingHTTPServer((LISTEN_HOST, LISTEN_PORT), PaykiTodoCloudHandler)
    server.daemon_threads = True
    server.serve_forever()


if __name__ == "__main__":
    main()
