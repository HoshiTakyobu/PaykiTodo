from __future__ import annotations

import base64
import importlib.util
import json
import os
import re
import sys
import tempfile
import threading
import unittest
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


class CloudServerIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.temp_dir = tempfile.TemporaryDirectory()
        os.environ["PAYKITODO_CLOUD_DB"] = str(Path(cls.temp_dir.name) / "cloud.sqlite3")
        os.environ["PAYKITODO_ADMIN_USER"] = "admin"
        os.environ["PAYKITODO_ADMIN_PASSWORD"] = "test-admin-password"
        os.environ["PAYKITODO_PASSWORD_PEPPER"] = "test-only-pepper"
        module_path = Path(__file__).with_name("paykitodo_cloud_server.py")
        spec = importlib.util.spec_from_file_location("paykitodo_cloud_server_test", module_path)
        assert spec and spec.loader
        cls.cloud = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = cls.cloud
        spec.loader.exec_module(cls.cloud)
        cls.server = cls.cloud.ThreadingHTTPServer(("127.0.0.1", 0), cls.cloud.PaykiTodoCloudHandler)
        cls.server.daemon_threads = True
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.base_url = f"http://127.0.0.1:{cls.server.server_port}"

    @classmethod
    def tearDownClass(cls) -> None:
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=3)
        sys.modules.pop(cls.cloud.__name__, None)
        cls.temp_dir.cleanup()

    def request(self, path: str, method: str = "GET", body: bytes | dict | None = None, headers: dict[str, str] | None = None):
        actual_headers = dict(headers or {})
        payload = body
        if isinstance(body, dict):
            payload = json.dumps(body).encode("utf-8")
            actual_headers["Content-Type"] = "application/json"
        request = urllib.request.Request(self.base_url + path, data=payload, method=method, headers=actual_headers)
        try:
            with urllib.request.urlopen(request, timeout=5) as response:
                return response.status, dict(response.headers), response.read()
        except urllib.error.HTTPError as error:
            return error.code, dict(error.headers), error.read()

    def request_form_without_redirect(self, path: str, body: dict[str, str], headers: dict[str, str]):
        payload = urllib.parse.urlencode(body).encode("utf-8")
        request = urllib.request.Request(
            self.base_url + path,
            data=payload,
            method="POST",
            headers={**headers, "Content-Type": "application/x-www-form-urlencoded"},
        )
        opener = urllib.request.build_opener(
            type("NoRedirectHandler", (urllib.request.HTTPRedirectHandler,), {"redirect_request": lambda *_args: None})
        )
        try:
            with opener.open(request, timeout=5) as response:
                return response.status, dict(response.headers), response.read()
        except urllib.error.HTTPError as error:
            return error.code, dict(error.headers), error.read()

    def create_account(self, suffix: str):
        status, _, raw = self.request(
            "/v2/auth/register",
            "POST",
            {
                "username": f"tester_{suffix}",
                "displayName": "测试用户",
                "password": "correct horse battery staple",
                "deviceId": f"android_device_{suffix}",
                "deviceName": "Android 测试机",
                "platform": "android",
            },
        )
        self.assertEqual(201, status, raw)
        return json.loads(raw)

    def test_register_login_tokens_and_device_list(self):
        account = self.create_account("auth")
        self.assertTrue(account["accessToken"])
        self.assertEqual(0, account["snapshotRevision"])

        status, _, _ = self.request(
            "/v2/auth/login",
            "POST",
            {"username": "tester_auth", "password": "wrong", "deviceId": "windows_device_auth", "deviceName": "PC", "platform": "windows"},
        )
        self.assertEqual(401, status)

        status, _, raw = self.request(
            "/v2/auth/login",
            "POST",
            {"username": "TESTER_AUTH", "password": "correct horse battery staple", "deviceId": "windows_device_auth", "deviceName": "PC", "platform": "windows"},
        )
        self.assertEqual(200, status, raw)
        login = json.loads(raw)
        status, _, raw = self.request("/v2/devices", headers={"Authorization": f"Bearer {login['accessToken']}"})
        self.assertEqual(200, status, raw)
        self.assertEqual(2, len(json.loads(raw)["devices"]))

        status, _, raw = self.request("/v2/auth/refresh", "POST", {"refreshToken": login["refreshToken"]})
        self.assertEqual(200, status, raw)
        self.assertNotEqual(login["accessToken"], json.loads(raw)["accessToken"])

    def test_versioned_snapshot_idempotency_and_conflict(self):
        account = self.create_account("sync")
        auth = {"Authorization": f"Bearer {account['accessToken']}"}
        status, _, raw = self.request("/v2/sync/status", headers=auth)
        self.assertEqual(200, status, raw)
        self.assertEqual(0, json.loads(raw)["revision"])

        payload = b"PKSYNC1" + bytes(range(64))
        upload_headers = {**auth, "Content-Type": "application/octet-stream", "If-Match": '"0"', "X-Payki-Operation-Id": "operation-sync-0001"}
        status, _, raw = self.request("/v2/sync/snapshot", "PUT", payload, upload_headers)
        self.assertEqual(200, status, raw)
        self.assertEqual(1, json.loads(raw)["revision"])

        status, _, raw = self.request("/v2/sync/snapshot", "PUT", payload, upload_headers)
        self.assertEqual(200, status, raw)
        self.assertTrue(json.loads(raw)["idempotent"])

        conflict_headers = {**auth, "Content-Type": "application/octet-stream", "If-Match": '"0"', "X-Payki-Operation-Id": "operation-sync-0002"}
        status, _, raw = self.request("/v2/sync/snapshot", "PUT", payload + b"x", conflict_headers)
        self.assertEqual(409, status, raw)
        self.assertEqual(1, json.loads(raw)["currentRevision"])

        status, response_headers, downloaded = self.request("/v2/sync/snapshot", headers=auth)
        self.assertEqual(200, status)
        self.assertEqual('"1"', response_headers["ETag"])
        self.assertEqual(payload, downloaded)

    def test_admin_requires_basic_auth_and_renders_users(self):
        self.create_account("admin")
        status, response_headers, _ = self.request("/admin")
        self.assertEqual(401, status)
        self.assertIn("Basic", response_headers["WWW-Authenticate"])

        credential = base64.b64encode(b"admin:test-admin-password").decode("ascii")
        status, _, raw = self.request("/admin", headers={"Authorization": f"Basic {credential}"})
        self.assertEqual(200, status, raw)
        page = raw.decode("utf-8")
        self.assertIn("PaykiTodo 管理后台", page)
        self.assertIn("tester_admin", page)

    def test_admin_state_changes_require_csrf_and_revoke_device(self):
        account = self.create_account("admin_actions")
        credential = base64.b64encode(b"admin:test-admin-password").decode("ascii")
        admin_headers = {"Authorization": f"Basic {credential}"}

        status, _, raw = self.request("/admin", headers=admin_headers)
        self.assertEqual(200, status, raw)
        page = raw.decode("utf-8")
        csrf = re.search(r"name='csrf' value='([^']+)'", page)
        user_id = re.search(r"<tr><td>tester_admin_actions</td>.*?/admin/users/(\d+)/toggle", page, re.S)
        self.assertIsNotNone(csrf)
        self.assertIsNotNone(user_id)

        status, _, _ = self.request_form_without_redirect(
            f"/admin/users/{user_id.group(1)}/toggle",
            {"csrf": "wrong-token"},
            admin_headers,
        )
        self.assertEqual(403, status)

        status, headers, _ = self.request_form_without_redirect(
            f"/admin/users/{user_id.group(1)}/toggle",
            {"csrf": csrf.group(1)},
            admin_headers,
        )
        self.assertEqual(303, status)
        self.assertEqual("/admin", headers["Location"])

        status, _, raw = self.request("/v2/sync/status", headers={"Authorization": f"Bearer {account['accessToken']}"})
        self.assertEqual(401, status)

        status, _, raw = self.request("/admin", headers=admin_headers)
        self.assertEqual(200, status, raw)
        page = raw.decode("utf-8")
        csrf = re.search(r"name='csrf' value='([^']+)'", page)
        self.assertIsNotNone(csrf)
        status, _, _ = self.request_form_without_redirect(
            f"/admin/users/{user_id.group(1)}/toggle",
            {"csrf": csrf.group(1)},
            admin_headers,
        )
        self.assertEqual(303, status)

        status, _, raw = self.request(
            "/v2/auth/login",
            "POST",
            {
                "username": "TESTER_ADMIN_ACTIONS",
                "password": "correct horse battery staple",
                "deviceId": "windows_device_admin_actions",
                "deviceName": "PC",
                "platform": "windows",
            },
        )
        self.assertEqual(200, status, raw)
        login = json.loads(raw)

        status, _, raw = self.request("/admin", headers=admin_headers)
        page = raw.decode("utf-8")
        csrf = re.search(r"name='csrf' value='([^']+)'", page)
        self.assertIsNotNone(csrf)
        device_id = login["deviceId"]
        status, headers, _ = self.request_form_without_redirect(
            f"/admin/devices/{device_id}/revoke",
            {"csrf": csrf.group(1)},
            admin_headers,
        )
        self.assertEqual(303, status)
        self.assertEqual("/admin", headers["Location"])
        status, _, _ = self.request("/v2/sync/status", headers={"Authorization": f"Bearer {login['accessToken']}"})
        self.assertEqual(401, status)


if __name__ == "__main__":
    unittest.main()
