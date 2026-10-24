from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
import importlib.util
import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from unittest.mock import patch


class CalendarCollaborationIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory()
        cls.environment = patch.dict(os.environ, {
            "PAYKITODO_CLOUD_DB": str(Path(cls.temporary.name) / "shared.sqlite3"),
            "PAYKITODO_PASSWORD_PEPPER": "calendar-test-only-pepper",
            "PAYKITODO_ADMIN_USER": "", "PAYKITODO_ADMIN_PASSWORD": "",
            "PAYKITODO_TURNSTILE_SITE_KEY": "", "PAYKITODO_TURNSTILE_SECRET_KEY": "",
            "PAYKITODO_TURNSTILE_ALLOWED_HOSTNAMES": "",
        })
        cls.environment.start()
        spec = importlib.util.spec_from_file_location("paykitodo_shared_calendar_test", Path(__file__).with_name("paykitodo_cloud_server.py"))
        cls.cloud = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = cls.cloud
        spec.loader.exec_module(cls.cloud)

        class QuietHandler(cls.cloud.PaykiTodoCloudHandler):
            def log_message(self, *args):
                pass

        cls.server = cls.cloud.ThreadingHTTPServer(("127.0.0.1", 0), QuietHandler)
        cls.server.daemon_threads = True
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.base = f"http://127.0.0.1:{cls.server.server_port}"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=3)
        cls.environment.stop()
        sys.modules.pop(cls.cloud.__name__, None)
        cls.temporary.cleanup()

    def setUp(self):
        self.cloud.RATE_LIMITER = self.cloud.RateLimiter()
        self.sequence = self._testMethodName.replace("test_", "")[:40]

    def request(self, path, method="GET", body=None, account=None):
        headers = {"Authorization": f"Bearer {account['accessToken']}"} if account else {}
        payload = json.dumps(body).encode("utf-8") if body is not None else None
        if payload is not None:
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(self.base + path, method=method, data=payload, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=8) as response:
                return response.status, json.loads(response.read())
        except urllib.error.HTTPError as error:
            return error.code, json.loads(error.read())

    def account(self, name):
        username = f"{name}_{self.sequence}"
        status, result = self.request("/v2/auth/register", "POST", {
            "username": username, "password": "calendar-test-only-password", "displayName": name,
            "deviceId": f"device_{username}", "deviceName": "synthetic", "platform": "test",
        })
        self.assertEqual(201, status, result)
        return result

    def calendar(self, owner):
        status, result = self.request("/v3/calendars", "POST", {"name": "测试共享日历", "colorHex": "#1234ab", "description": "synthetic"}, owner)
        self.assertEqual(201, status, result)
        calendar = result["calendar"]
        self.assertEqual("owner", calendar["role"])
        self.assertEqual("server-managed", calendar["encryption"])
        return calendar

    def join(self, owner, calendar, target, role="viewer", accept=True):
        status, result = self.request(f"/v3/calendars/{calendar['id']}/invitations", "POST", {"username": target["user"]["username"].upper(), "role": role}, owner)
        self.assertEqual(201, status, result)
        invitation = result["invitation"]
        status, result = self.request(f"/v3/calendar-invitations/{invitation['id']}/respond", "POST", {"accept": accept}, target)
        self.assertEqual(200, status, result)
        return invitation, result

    def event(self, identifier="event-1", **values):
        return {"id": identifier, "title": "Shared title", "startAtMillis": 1793000000000, "endAtMillis": 1793003600000,
                "allDay": False, "eventTimeZoneId": "Asia/Shanghai", "eventBusyStatus": "BUSY", "eventVisibility": "DEFAULT", **values}

    def put_events(self, owner, calendar, revision, events):
        return self.request(f"/v3/calendars/{calendar['id']}/events", "PUT", {"expectedRevision": revision, "events": events}, owner)

    def test_membership_accept_decline_and_owner_controls(self):
        owner, viewer, declined = self.account("owner"), self.account("viewer"), self.account("declined")
        calendar = self.calendar(owner)
        self.assertEqual(403, self.request(f"/v3/calendars/{calendar['id']}", account=viewer)[0])
        invite, accepted = self.join(owner, calendar, viewer)
        self.assertEqual("ACCEPTED", accepted["status"])
        self.assertEqual("viewer", accepted["calendar"]["role"])
        self.assertFalse(accepted["calendar"]["canEdit"])
        self.assertEqual(2, accepted["calendar"]["permissionVersion"])
        self.assertEqual([], self.request("/v3/calendar-invitations", account=viewer)[1]["invitations"])
        self.assertEqual(409, self.request(f"/v3/calendar-invitations/{invite['id']}/respond", "POST", {"accept": True}, viewer)[0])
        _, rejected = self.join(owner, calendar, declined, accept=False)
        self.assertEqual("DECLINED", rejected["status"])
        self.assertEqual([], self.request("/v3/calendars", account=declined)[1]["calendars"])
        self.assertEqual(403, self.put_events(viewer, calendar, 0, [self.event()])[0])
        self.assertEqual(403, self.request(f"/v3/calendars/{calendar['id']}", "PATCH", {"expectedRevision": 0, "name": "unauthorized"}, viewer)[0])
        self.assertEqual(403, self.request(f"/v3/calendars/{calendar['id']}", "DELETE", account=viewer)[0])
        self.assertEqual(403, self.request(f"/v3/calendars/{calendar['id']}/invitations", "POST", {"username": declined["user"]["username"]}, viewer)[0])
        self.assertEqual(409, self.request(f"/v3/calendars/{calendar['id']}/members/{owner['user']['username']}", "DELETE", account=owner)[0])

    def test_event_cas_concurrency_metadata_and_unknown_fields(self):
        owner, editor = self.account("owner"), self.account("editor")
        calendar = self.calendar(owner)
        self.join(owner, calendar, editor, "editor")
        template = {"id": 42, "recurrenceType": "WEEKLY", "recurrenceInterval": 2, "recurrenceEndEpochDay": None}
        first = self.event("first", template=template, customExtension={"v": 2})
        second = self.event("second")
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(lambda value: self.put_events(editor, calendar, 0, [value]), (first, second)))
        self.assertEqual([200, 409], sorted(status for status, _ in results), results)
        conflict = next(body for status, body in results if status == 409)
        self.assertEqual(1, conflict["currentRevision"])
        current = self.request(f"/v3/calendars/{calendar['id']}/events", account=owner)[1]
        self.assertEqual(1, current["revision"])
        self.assertEqual(1, len(current["events"]))
        self.assertEqual(200, self.put_events(editor, calendar, 1, [first])[0])
        current = self.request(f"/v3/calendars/{calendar['id']}/events", account=owner)[1]
        self.assertEqual(template, current["events"][0]["template"])
        self.assertEqual({"v": 2}, current["events"][0]["customExtension"])
        self.assertEqual(owner["user"]["username"], current["events"][0]["organizerUsername"])
        self.assertEqual(409, self.request(f"/v3/calendars/{calendar['id']}", "PATCH", {"expectedRevision": 1, "name": "stale"}, owner)[0])
        status, renamed = self.request(f"/v3/calendars/{calendar['id']}", "PATCH", {"expectedRevision": 2, "name": "Changed"}, owner)
        self.assertEqual(200, status, renamed)
        self.assertEqual(3, renamed["calendar"]["revision"])
        self.assertEqual(409, self.put_events(editor, calendar, 2, [second])[0])

    def test_private_projection_rsvp_integrity_and_revocation(self):
        owner, viewer, editor = self.account("owner"), self.account("viewer"), self.account("editor")
        calendar = self.calendar(owner)
        self.join(owner, calendar, viewer)
        self.join(owner, calendar, editor, "editor")
        secret_event = self.event(eventVisibility="PRIVATE", notes="PRIVATE_MARKER", location="PRIVATE_MARKER",
                                  arbitrary={"secret": "PRIVATE_MARKER"}, template={"notes": "PRIVATE_MARKER"},
                                  participants=[{"username": viewer["user"]["username"], "response": "ACCEPTED"}])
        self.assertEqual(200, self.put_events(owner, calendar, 0, [secret_event])[0])
        owner_event = self.request(f"/v3/calendars/{calendar['id']}/events", account=owner)[1]["events"][0]
        self.assertEqual("NEEDS_ACTION", owner_event["participants"][0]["response"])
        status, visible = self.request(f"/v3/calendars/{calendar['id']}/events", account=viewer)
        self.assertEqual(200, status, visible)
        self.assertNotIn("PRIVATE_MARKER", json.dumps(visible))
        self.assertTrue(visible["events"][0]["redacted"])
        self.assertEqual("忙碌", visible["events"][0]["title"])
        self.assertEqual("PRIVATE_MARKER", self.request(f"/v3/calendars/{calendar['id']}/events", account=editor)[1]["events"][0]["notes"])
        status, response = self.request(f"/v3/calendars/{calendar['id']}/events/event-1/rsvp", "POST", {"response": "DECLINED", "username": editor["user"]["username"]}, viewer)
        self.assertEqual(200, status, response)
        self.assertEqual(2, response["revision"])
        visible_response = self.request(f"/v3/calendars/{calendar['id']}/events", account=viewer)[1]["events"][0]
        self.assertEqual([{"username": viewer["user"]["username"], "response": "DECLINED"}], visible_response["participants"])
        self.assertNotIn("PRIVATE_MARKER", json.dumps(visible_response))
        self.assertEqual(2, self.request(f"/v3/calendars/{calendar['id']}/events/event-1/rsvp", "POST", {"response": "DECLINED"}, viewer)[1]["revision"])
        secret_event["participants"][0]["response"] = "ACCEPTED"
        self.assertEqual(200, self.put_events(editor, calendar, 2, [secret_event])[0])
        actual = self.request(f"/v3/calendars/{calendar['id']}/events", account=owner)[1]["events"][0]
        self.assertEqual([{"username": viewer["user"]["username"], "response": "DECLINED"}], actual["participants"])
        self.assertEqual(actual["participants"], json.loads(actual["eventParticipantsJson"]))
        self.assertEqual(200, self.request(f"/v3/calendars/{calendar['id']}/members/{viewer['user']['username']}", "DELETE", account=owner)[0])
        self.assertEqual([], self.request("/v3/calendars", account=viewer)[1]["calendars"])
        for suffix, method, body in (("", "GET", None), ("/events", "GET", None), ("/events/event-1/rsvp", "POST", {"response": "ACCEPTED"})):
            self.assertEqual(403, self.request(f"/v3/calendars/{calendar['id']}{suffix}", method, body, viewer)[0])
        self.assertEqual(200, self.put_events(owner, calendar, 3, [secret_event])[0])
        self.assertEqual([], self.request(f"/v3/calendars/{calendar['id']}/events", account=owner)[1]["events"][0]["participants"])

    def test_private_canceled_projection_never_becomes_an_active_busy_block(self):
        owner, viewer = self.account("owner"), self.account("viewer")
        calendar = self.calendar(owner)
        self.join(owner, calendar, viewer)
        private = self.event(eventVisibility="PRIVATE", canceled=True, completed=False,
                             notes="PRIVATE_MARKER", location="PRIVATE_MARKER",
                             participants=[{"username": viewer["user"]["username"], "response": "ACCEPTED"},
                                           {"username": owner["user"]["username"], "response": "ACCEPTED"}])
        self.assertEqual(200, self.put_events(owner, calendar, 0, [private])[0])
        visible = self.request(f"/v3/calendars/{calendar['id']}/events", account=viewer)[1]["events"][0]
        self.assertTrue(visible["canceled"])
        self.assertFalse(visible["completed"])
        self.assertTrue(visible["redacted"])
        self.assertNotIn("PRIVATE_MARKER", json.dumps(visible))
        self.assertEqual([{"username": viewer["user"]["username"], "response": "NEEDS_ACTION"}], visible["participants"])
        self.assertEqual(400, self.put_events(owner, calendar, 1, [self.event(canceled={"secret": "PRIVATE_MARKER"})])[0])
        self.assertEqual(1, self.request(f"/v3/calendars/{calendar['id']}/events", account=owner)[1]["revision"])

    def test_authentication_rechecked_and_personal_snapshots_unchanged(self):
        owner = self.account("owner")
        calendar = self.calendar(owner)
        self.assertEqual(401, self.request("/v3/calendars")[0])
        self.assertEqual(0, self.request("/v2/sync/status", account=owner)[1]["revision"])
        self.assertEqual(200, self.put_events(owner, calendar, 0, [self.event()])[0])
        self.assertEqual(0, self.request("/v2/sync/status", account=owner)[1]["revision"])
        with self.cloud.DB.session() as connection:
            self.assertEqual(0, connection.execute("SELECT count(*) FROM snapshots").fetchone()[0])
            actor = self.cloud.AuthenticatedSession(owner["user"]["id"], owner["user"]["username"], f"device_{owner['user']['username']}")
            connection.execute("UPDATE devices SET revoked_at=1 WHERE id=?", (actor.device_id,))
        with patch.object(self.cloud.PaykiTodoCloudHandler, "require_session", return_value=actor):
            self.assertEqual(401, self.put_events(owner, calendar, 1, [self.event("blocked")])[0])
        with self.cloud.DB.session() as connection:
            self.assertEqual(1, connection.execute("SELECT revision FROM shared_calendars WHERE id=?", (calendar["id"],)).fetchone()[0])

    def test_validation_is_atomic_and_limits_payloads(self):
        owner = self.account("owner")
        calendar = self.calendar(owner)
        invalid = [self.event(id=[]), self.event(endAtMillis=1), self.event(eventBusyStatus={}),
                   self.event(eventVisibility=[]), self.event(allDay=1), self.event(eventTimeZoneId="../UTC"),
                   self.event(allDayReminderRulesJson='[{"daysBefore":0,"minuteOfDay":1440}]'),
                   self.event(participants=[{"username": "outsider", "response": []}]), self.event(template=[]),
                   self.event(customExtension=float("nan"))]
        for event in invalid:
            with self.subTest(event=event):
                self.assertEqual(400, self.put_events(owner, calendar, 0, [self.event("valid"), event])[0])
        self.assertEqual(400, self.put_events(owner, calendar, True, [self.event()])[0])
        self.assertEqual(400, self.put_events(owner, calendar, 0, [self.event(), self.event()])[0])
        self.assertEqual(400, self.put_events(owner, calendar, 0, [self.event(str(index)) for index in range(2001)])[0])
        self.assertEqual(413, self.put_events(owner, calendar, 0, [self.event(notes="x" * 70000)])[0])
        self.assertEqual(413, self.put_events(owner, calendar, 0, [self.event(notes="x" * (2 * 1024 * 1024))])[0])
        actual = self.request(f"/v3/calendars/{calendar['id']}/events", account=owner)[1]
        self.assertEqual(0, actual["revision"])
        self.assertEqual([], actual["events"])

    def test_invitation_identity_duplicates_and_deleted_source(self):
        owner, target, outsider = self.account("owner"), self.account("target"), self.account("outsider")
        calendar = self.calendar(owner)
        path = f"/v3/calendars/{calendar['id']}/invitations"
        self.assertEqual(400, self.request(path, "POST", {"username": target["user"]["username"], "role": "owner"}, owner)[0])
        self.assertEqual(400, self.request(path, "POST", {"username": target["user"]["username"], "role": []}, owner)[0])
        status, result = self.request(path, "POST", {"username": target["user"]["username"]}, owner)
        self.assertEqual(201, status, result)
        invitation = result["invitation"]
        self.assertEqual(409, self.request(path, "POST", {"username": target["user"]["username"]}, owner)[0])
        response_path = f"/v3/calendar-invitations/{invitation['id']}/respond"
        self.assertEqual(404, self.request(response_path, "POST", {"accept": True}, outsider)[0])
        self.assertEqual(400, self.request(response_path, "POST", {"accept": "true"}, target)[0])
        self.assertEqual(1, len(self.request("/v3/calendar-invitations", account=target)[1]["invitations"]))
        self.assertEqual(200, self.request(f"/v3/calendars/{calendar['id']}", "DELETE", account=owner)[0])
        self.assertEqual(410, self.request(response_path, "POST", {"accept": True}, target)[0])
        self.assertEqual([], self.request("/v3/calendar-invitations", account=target)[1]["invitations"])
        self.assertEqual(404, self.request(f"/v3/calendars/{calendar['id']}/events", account=owner)[0])

    def test_member_identity_is_calendar_scoped_and_reinvite_works(self):
        owner, member = self.account("owner"), self.account("member")
        first, second = self.calendar(owner), self.calendar(owner)
        self.join(owner, first, member, "editor")
        self.assertEqual(403, self.put_events(member, second, 0, [self.event()])[0])
        self.assertEqual(403, self.request(f"/v3/calendars/{second['id']}/events", account=member)[0])
        self.assertEqual(200, self.request(f"/v3/calendars/{first['id']}/members/{member['user']['username']}", "DELETE", account=owner)[0])
        self.join(owner, first, member, "viewer")
        self.assertEqual("viewer", self.request(f"/v3/calendars/{first['id']}", account=member)[1]["calendar"]["role"])
        self.assertEqual(403, self.put_events(member, first, 0, [self.event()])[0])

    def test_rate_limit_and_disabled_account_cannot_change_shared_source(self):
        owner = self.account("owner")
        calendar = self.calendar(owner)
        with patch.object(self.cloud.RATE_LIMITER, "allow", return_value=False):
            status, body = self.put_events(owner, calendar, 0, [self.event()])
        self.assertEqual(429, status, body)
        self.assertEqual("rate_limited", body["error"])
        with self.cloud.DB.session() as connection:
            actor = self.cloud.AuthenticatedSession(owner["user"]["id"], owner["user"]["username"], f"device_{owner['user']['username']}")
            connection.execute("UPDATE users SET disabled=1 WHERE id=?", (actor.user_id,))
        self.assertEqual(401, self.request("/v3/calendars", account=owner)[0])
        with patch.object(self.cloud.PaykiTodoCloudHandler, "require_session", return_value=actor):
            self.assertEqual(401, self.put_events(owner, calendar, 0, [self.event()])[0])
        with self.cloud.DB.session() as connection:
            self.assertEqual(0, connection.execute("SELECT revision FROM shared_calendars WHERE id=?", (calendar["id"],)).fetchone()[0])


if __name__ == "__main__":
    unittest.main()
