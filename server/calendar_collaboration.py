"""Access-controlled shared calendars, separate from encrypted personal snapshots."""

from __future__ import annotations

from contextlib import contextmanager
from http import HTTPStatus
import json
import math
import re
import sqlite3
import uuid


MAX_CALENDARS = 128
MAX_MEMBERS = 128
MAX_EVENTS = 2000
MAX_EVENT_BYTES = 64 * 1024
MAX_CALENDAR_JSON_BYTES = 2 * 1024 * 1024
EVENT_ID_PATTERN = re.compile(r"^[A-Za-z0-9_.:-]{1,128}$")
USERNAME_PATTERN = re.compile(r"^[A-Za-z0-9_.-]{3,64}$")
COLOR_PATTERN = re.compile(r"^#[0-9A-Fa-f]{6}$")
TIMEZONE_PATTERN = re.compile(r"^[A-Za-z0-9_+.-]+(?:/[A-Za-z0-9_+.-]+)*$")
ROLES = frozenset({"owner", "editor", "viewer"})
RESPONSES = frozenset({"NEEDS_ACTION", "ACCEPTED", "DECLINED", "TENTATIVE"})
MAX_SAFE_INTEGER = 2**53 - 1


class CalendarError(Exception):
    def __init__(self, status: HTTPStatus, code: str, message: str, **details):
        super().__init__(message)
        self.status = status
        self.code = code
        self.message = message
        self.details = details


def initialize_schema(connection: sqlite3.Connection) -> None:
    connection.executescript(
        """
        CREATE TABLE IF NOT EXISTS shared_calendars (
            id TEXT PRIMARY KEY,
            owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            name TEXT NOT NULL,
            color_hex TEXT NOT NULL,
            description TEXT NOT NULL DEFAULT '',
            revision INTEGER NOT NULL DEFAULT 0,
            permission_version INTEGER NOT NULL DEFAULT 1,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            deleted_at INTEGER
        );
        CREATE INDEX IF NOT EXISTS idx_shared_calendars_owner
            ON shared_calendars(owner_id, deleted_at);
        CREATE TABLE IF NOT EXISTS shared_calendar_members (
            calendar_id TEXT NOT NULL REFERENCES shared_calendars(id) ON DELETE CASCADE,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            role TEXT NOT NULL CHECK(role IN ('owner','editor','viewer')),
            joined_at INTEGER NOT NULL,
            revoked_at INTEGER,
            PRIMARY KEY(calendar_id, user_id)
        );
        CREATE INDEX IF NOT EXISTS idx_shared_members_user
            ON shared_calendar_members(user_id, revoked_at);
        CREATE TABLE IF NOT EXISTS shared_calendar_events (
            calendar_id TEXT NOT NULL REFERENCES shared_calendars(id) ON DELETE CASCADE,
            id TEXT NOT NULL,
            payload_json TEXT NOT NULL,
            updated_at INTEGER NOT NULL,
            PRIMARY KEY(calendar_id, id)
        );
        CREATE TABLE IF NOT EXISTS shared_calendar_invitations (
            id TEXT PRIMARY KEY,
            calendar_id TEXT NOT NULL REFERENCES shared_calendars(id) ON DELETE CASCADE,
            inviter_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            invitee_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            role TEXT NOT NULL CHECK(role IN ('editor','viewer')),
            status TEXT NOT NULL CHECK(status IN ('PENDING','ACCEPTED','DECLINED','REVOKED')),
            created_at INTEGER NOT NULL,
            responded_at INTEGER
        );
        CREATE UNIQUE INDEX IF NOT EXISTS idx_shared_invite_pending
            ON shared_calendar_invitations(calendar_id, invitee_id) WHERE status='PENDING';
        CREATE INDEX IF NOT EXISTS idx_shared_invites_user
            ON shared_calendar_invitations(invitee_id, status, created_at);
        """
    )


def _invalid(message: str = "共享日历数据无效") -> CalendarError:
    return CalendarError(HTTPStatus.BAD_REQUEST, "invalid_calendar_data", message)


def _integer(value, name: str, minimum: int = 0, maximum: int = MAX_SAFE_INTEGER) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or not minimum <= value <= maximum:
        raise _invalid(f"{name}必须是范围内的整数")
    return value


def _text(value, name: str, maximum: int, allow_empty: bool = False) -> str:
    if not isinstance(value, str):
        raise _invalid(f"{name}必须是文字")
    result = value.strip()
    if len(result) > maximum or (not allow_empty and not result):
        raise _invalid(f"{name}长度无效")
    return result


def _uuid(value: str) -> str:
    try:
        parsed = uuid.UUID(value)
    except (ValueError, AttributeError) as error:
        raise _invalid("日历或邀请标识无效") from error
    if str(parsed) != value.lower():
        raise _invalid("日历或邀请标识无效")
    return str(parsed)


def _event_id(value) -> str:
    if not isinstance(value, str) or not EVENT_ID_PATTERN.fullmatch(value):
        raise _invalid("日程标识无效")
    return value


def _validate_json(value, depth: int = 0) -> None:
    if depth > 16:
        raise _invalid("日程数据嵌套过深")
    if isinstance(value, dict):
        if len(value) > 256:
            raise _invalid("日程字段过多")
        for key, item in value.items():
            if not isinstance(key, str) or len(key) > 128:
                raise _invalid("日程字段名无效")
            _validate_json(item, depth + 1)
    elif isinstance(value, list):
        if len(value) > 4096:
            raise _invalid("日程数据列表过长")
        for item in value:
            _validate_json(item, depth + 1)
    elif isinstance(value, float):
        if not math.isfinite(value):
            raise _invalid("日程数据包含非有限数值")
    elif isinstance(value, int) and not isinstance(value, bool):
        if not -MAX_SAFE_INTEGER <= value <= MAX_SAFE_INTEGER:
            raise _invalid("日程数值超出范围")


def _participants(payload: dict, existing: dict | None, active_members: set[str]) -> list[dict]:
    requested = payload.get("participants")
    if requested is None:
        encoded = payload.get("eventParticipantsJson", "[]")
        if not isinstance(encoded, str):
            raise _invalid("参与者数据无效")
        try:
            requested = json.loads(encoded)
        except json.JSONDecodeError as error:
            raise _invalid("参与者数据无效") from error
    if not isinstance(requested, list) or len(requested) > MAX_MEMBERS:
        raise _invalid("参与者数量或格式无效")
    previous = {item["username"]: item.get("response", "NEEDS_ACTION")
                for item in (existing or {}).get("participants", [])}
    result = []
    seen = set()
    for item in requested:
        if isinstance(item, str):
            username = item.strip().lower()
        elif isinstance(item, dict):
            username = _text(item.get("username"), "参与者账号", 64).lower()
            response = item.get("response", "NEEDS_ACTION")
            if not isinstance(response, str) or response not in RESPONSES:
                raise _invalid("参与者回应无效")
        else:
            raise _invalid("参与者数据无效")
        if username not in active_members and username in previous:
            continue
        if not USERNAME_PATTERN.fullmatch(username) or username not in active_members:
            raise _invalid("参与者须先成为当前共享日历成员")
        if username not in seen:
            seen.add(username)
            result.append({"username": username, "response": previous.get(username, "NEEDS_ACTION")})
    return result


def _normalize_event(value, existing: dict | None, owner_username: str, active_members: set[str]) -> dict:
    if not isinstance(value, dict):
        raise _invalid("日程必须是对象")
    _validate_json(value)
    payload = dict(value)
    payload["id"] = _event_id(payload.get("id"))
    payload["title"] = _text(payload.get("title"), "日程标题", 1000)
    start = _integer(payload.get("startAtMillis"), "开始时间", -MAX_SAFE_INTEGER)
    end = _integer(payload.get("endAtMillis"), "结束时间", -MAX_SAFE_INTEGER)
    if end <= start:
        raise _invalid("结束时间必须晚于开始时间")
    if "allDay" in payload and not isinstance(payload["allDay"], bool):
        raise _invalid("全天状态无效")
    payload["allDay"] = payload.get("allDay", False)
    for status_field in ("canceled", "completed"):
        if status_field in payload and not isinstance(payload[status_field], bool):
            raise _invalid("日程状态无效")
    payload["isEvent"] = True
    timezone = _text(payload.get("eventTimeZoneId", ""), "日程时区", 128, True)
    if timezone and (not TIMEZONE_PATTERN.fullmatch(timezone) or ".." in timezone):
        raise _invalid("日程时区无效")
    payload["eventTimeZoneId"] = timezone
    payload["eventBusyStatus"] = payload.get("eventBusyStatus", "BUSY")
    payload["eventVisibility"] = payload.get("eventVisibility", "DEFAULT")
    if not isinstance(payload["eventBusyStatus"], str) or not isinstance(payload["eventVisibility"], str) or payload["eventBusyStatus"] not in {"BUSY", "FREE"} or payload["eventVisibility"] not in {"DEFAULT", "PRIVATE", "PUBLIC"}:
        raise _invalid("忙闲或可见范围无效")
    all_day_rules = payload.get("allDayReminderRulesJson", "")
    if not isinstance(all_day_rules, str):
        raise _invalid("全天提醒数据无效")
    if all_day_rules:
        try:
            rules = json.loads(all_day_rules)
        except json.JSONDecodeError as error:
            raise _invalid("全天提醒数据无效") from error
        if not isinstance(rules, list) or len(rules) > 64:
            raise _invalid("全天提醒数量或格式无效")
        for rule in rules:
            if not isinstance(rule, dict):
                raise _invalid("全天提醒格式无效")
            _integer(rule.get("daysBefore"), "全天提醒提前天数", 0, 3650)
            _integer(rule.get("minuteOfDay"), "全天提醒钟点", 0, 1439)
    if "template" in payload and not isinstance(payload["template"], dict):
        raise _invalid("循环模板必须是对象")
    payload["participants"] = _participants(payload, existing, active_members)
    payload["eventParticipantsJson"] = json.dumps(payload["participants"], ensure_ascii=False, separators=(",", ":"))
    payload["organizerUsername"] = owner_username
    if len(json.dumps(payload, ensure_ascii=False, separators=(",", ":"), allow_nan=False).encode("utf-8")) > MAX_EVENT_BYTES:
        raise CalendarError(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, "event_too_large", "单条日程内容过大")
    return payload


def _project_event(payload: dict, role: str, username: str = "") -> dict:
    if role != "viewer" or payload.get("eventVisibility") != "PRIVATE":
        return payload
    allowed = {key: payload[key] for key in (
        "id", "isEvent", "startAtMillis", "endAtMillis", "allDay", "eventTimeZoneId",
        "eventBusyStatus", "eventVisibility", "canceled", "completed",
    ) if key in payload}
    allowed.update({"redacted": True, "title": "忙碌" if payload["eventBusyStatus"] == "BUSY" else "空闲"})
    own_response = [{"username": item["username"], "response": item["response"]}
                    for item in payload.get("participants", []) if item["username"] == username]
    allowed["participants"] = own_response
    allowed["eventParticipantsJson"] = json.dumps(own_response, ensure_ascii=False, separators=(",", ":"))
    return allowed


@contextmanager
def _transaction(database, write: bool):
    with database.session() as connection:
        connection.execute("BEGIN IMMEDIATE" if write else "BEGIN")
        try:
            yield connection
            connection.commit()
        except BaseException:
            connection.rollback()
            raise


def _require_actor(connection, handler, actor, timestamp: int, hash_token) -> None:
    row = connection.execute(
        "SELECT s.user_id FROM sessions s JOIN users u ON u.id=s.user_id "
        "JOIN devices d ON d.id=s.device_id WHERE s.access_hash=? AND s.user_id=? "
        "AND s.device_id=? AND s.access_expires_at>? AND u.disabled=0 AND d.revoked_at IS NULL",
        (hash_token(handler.bearer_token()), actor.user_id, actor.device_id, timestamp),
    ).fetchone()
    if row is None:
        raise CalendarError(HTTPStatus.UNAUTHORIZED, "invalid_token", "登录已过期，请重新登录")


def _member(connection, calendar_id: str, actor, manage: bool = False, edit: bool = False):
    calendar = connection.execute(
        "SELECT c.*,u.username AS owner_username FROM shared_calendars c "
        "JOIN users u ON u.id=c.owner_id WHERE c.id=? AND c.deleted_at IS NULL", (calendar_id,),
    ).fetchone()
    if calendar is None:
        raise CalendarError(HTTPStatus.NOT_FOUND, "calendar_not_found", "共享日历不存在")
    membership = connection.execute(
        "SELECT role FROM shared_calendar_members WHERE calendar_id=? AND user_id=? AND revoked_at IS NULL",
        (calendar_id, actor.user_id),
    ).fetchone()
    if membership is None:
        raise CalendarError(HTTPStatus.FORBIDDEN, "calendar_access_denied", "已无权访问该共享日历")
    role = membership["role"]
    if manage and role != "owner" or edit and role not in {"owner", "editor"}:
        raise CalendarError(HTTPStatus.FORBIDDEN, "calendar_write_denied", "当前角色不能执行此操作")
    return calendar, role


def _public_calendar(connection, calendar, role: str) -> dict:
    members = connection.execute(
        "SELECT u.username,u.display_name,m.role FROM shared_calendar_members m "
        "JOIN users u ON u.id=m.user_id WHERE m.calendar_id=? AND m.revoked_at IS NULL AND u.disabled=0 "
        "ORDER BY CASE m.role WHEN 'owner' THEN 0 WHEN 'editor' THEN 1 ELSE 2 END,u.username",
        (calendar["id"],),
    ).fetchall()
    return {"id": calendar["id"], "name": calendar["name"], "colorHex": calendar["color_hex"],
            "description": calendar["description"], "ownerUsername": calendar["owner_username"],
            "role": role, "revision": calendar["revision"], "permissionVersion": calendar["permission_version"],
            "canEdit": role in {"owner", "editor"}, "canManage": role == "owner", "shared": True,
            "encryption": "server-managed", "createdAt": calendar["created_at"], "updatedAt": calendar["updated_at"],
            "members": [{"username": row["username"], "displayName": row["display_name"], "role": row["role"]} for row in members]}


def _revision(body: dict, calendar) -> None:
    expected = _integer(body.get("expectedRevision"), "expectedRevision")
    if expected != calendar["revision"]:
        raise CalendarError(HTTPStatus.CONFLICT, "revision_conflict", "共享日历已在其他客户端修改，请刷新后重试",
                            currentRevision=calendar["revision"])


def _invitation(connection, invitation_id: str):
    return connection.execute(
        "SELECT i.*,c.name AS calendar_name,c.deleted_at,u.username AS owner_username, "
        "target.username AS invitee_username FROM shared_calendar_invitations i "
        "JOIN shared_calendars c ON c.id=i.calendar_id JOIN users u ON u.id=c.owner_id "
        "JOIN users target ON target.id=i.invitee_id WHERE i.id=?", (invitation_id,),
    ).fetchone()


def _public_invitation(row) -> dict:
    return {"id": row["id"], "calendarId": row["calendar_id"], "calendarName": row["calendar_name"],
            "ownerUsername": row["owner_username"], "username": row["invitee_username"], "role": row["role"],
            "status": row["status"], "createdAt": row["created_at"], "respondedAt": row["responded_at"]}


def handle_request(handler, method: str, path: str, database, limiter, clock, hash_token) -> None:
    actor = handler.require_session()
    write = method != "GET"
    if not limiter.allow(f"shared-calendar:{actor.user_id}:{'write' if write else 'read'}", 60 if write else 240, 60):
        raise CalendarError(HTTPStatus.TOO_MANY_REQUESTS, "rate_limited", "共享日历请求过于频繁")
    parts = path.strip("/").split("/")
    try:
        body = handler.read_json(MAX_CALENDAR_JSON_BYTES) if method in {"POST", "PUT", "PATCH"} else {}
    except RecursionError as error:
        raise _invalid("共享日历数据嵌套过深") from error

    with _transaction(database, write) as connection:
        timestamp = clock()
        _require_actor(connection, handler, actor, timestamp, hash_token)
        status = HTTPStatus.OK
        result = None

        if parts == ["v3", "calendars"]:
            if method == "GET":
                calendars = connection.execute(
                    "SELECT c.*,u.username AS owner_username,m.role FROM shared_calendars c "
                    "JOIN shared_calendar_members m ON m.calendar_id=c.id JOIN users u ON u.id=c.owner_id "
                    "WHERE m.user_id=? AND m.revoked_at IS NULL AND c.deleted_at IS NULL "
                    "ORDER BY c.created_at,c.id", (actor.user_id,),
                ).fetchall()
                result = {"calendars": [_public_calendar(connection, row, row["role"]) for row in calendars]}
            elif method == "POST":
                count = connection.execute(
                    "SELECT count(*) FROM shared_calendars WHERE owner_id=? AND deleted_at IS NULL", (actor.user_id,),
                ).fetchone()[0]
                if count >= MAX_CALENDARS:
                    raise CalendarError(HTTPStatus.CONFLICT, "calendar_limit", "共享日历数量已达上限")
                name = _text(body.get("name"), "日历名称", 120)
                color = body.get("colorHex", "#3370FF")
                description = _text(body.get("description", ""), "日历说明", 2000, True)
                if not isinstance(color, str) or not COLOR_PATTERN.fullmatch(color):
                    raise _invalid("日历颜色无效")
                calendar_id = str(uuid.uuid4())
                connection.execute(
                    "INSERT INTO shared_calendars(id,owner_id,name,color_hex,description,created_at,updated_at) VALUES(?,?,?,?,?,?,?)",
                    (calendar_id, actor.user_id, name, color.upper(), description, timestamp, timestamp),
                )
                connection.execute("INSERT INTO shared_calendar_members(calendar_id,user_id,role,joined_at) VALUES(?,?,'owner',?)",
                                   (calendar_id, actor.user_id, timestamp))
                calendar, role = _member(connection, calendar_id, actor)
                result, status = {"calendar": _public_calendar(connection, calendar, role)}, HTTPStatus.CREATED

        elif parts == ["v3", "calendar-invitations"] and method == "GET":
            rows = connection.execute(
                "SELECT i.id FROM shared_calendar_invitations i JOIN shared_calendars c ON c.id=i.calendar_id "
                "WHERE i.invitee_id=? AND i.status='PENDING' AND c.deleted_at IS NULL ORDER BY i.created_at DESC,i.id LIMIT 128",
                (actor.user_id,),
            ).fetchall()
            result = {"invitations": [_public_invitation(_invitation(connection, row["id"])) for row in rows]}

        elif len(parts) == 4 and parts[:2] == ["v3", "calendar-invitations"] and parts[3] == "respond" and method == "POST":
            invitation_id = _uuid(parts[2])
            invitation = _invitation(connection, invitation_id)
            if invitation is None or invitation["invitee_id"] != actor.user_id:
                raise CalendarError(HTTPStatus.NOT_FOUND, "invitation_not_found", "邀请不存在")
            if invitation["deleted_at"] is not None or invitation["status"] == "REVOKED":
                raise CalendarError(HTTPStatus.GONE, "invitation_revoked", "邀请已失效")
            if invitation["status"] != "PENDING":
                raise CalendarError(HTTPStatus.CONFLICT, "invitation_already_responded", "邀请已经处理")
            if not isinstance(body.get("accept"), bool):
                raise _invalid("请选择接受或拒绝邀请")
            response = "ACCEPTED" if body["accept"] else "DECLINED"
            if body["accept"]:
                total = connection.execute("SELECT count(*) FROM shared_calendar_members WHERE calendar_id=? AND revoked_at IS NULL",
                                           (invitation["calendar_id"],)).fetchone()[0]
                if total >= MAX_MEMBERS:
                    raise CalendarError(HTTPStatus.CONFLICT, "member_limit", "日历成员数量已达上限")
                calendar_count = connection.execute(
                    "SELECT count(*) FROM shared_calendar_members m JOIN shared_calendars c ON c.id=m.calendar_id "
                    "WHERE m.user_id=? AND m.revoked_at IS NULL AND c.deleted_at IS NULL", (actor.user_id,),
                ).fetchone()[0]
                if calendar_count >= MAX_CALENDARS:
                    raise CalendarError(HTTPStatus.CONFLICT, "calendar_limit", "已加入的共享日历数量已达上限")
                connection.execute(
                    "INSERT INTO shared_calendar_members(calendar_id,user_id,role,joined_at,revoked_at) VALUES(?,?,?,?,NULL) "
                    "ON CONFLICT(calendar_id,user_id) DO UPDATE SET role=excluded.role,joined_at=excluded.joined_at,revoked_at=NULL",
                    (invitation["calendar_id"], actor.user_id, invitation["role"], timestamp),
                )
                connection.execute("UPDATE shared_calendars SET permission_version=permission_version+1,updated_at=? WHERE id=?",
                                   (timestamp, invitation["calendar_id"]))
            connection.execute("UPDATE shared_calendar_invitations SET status=?,responded_at=? WHERE id=?",
                               (response, timestamp, invitation_id))
            result = {"invitation": _public_invitation(_invitation(connection, invitation_id)), "status": response}
            if body["accept"]:
                calendar, role = _member(connection, invitation["calendar_id"], actor)
                result["calendar"] = _public_calendar(connection, calendar, role)

        elif len(parts) >= 3 and parts[:2] == ["v3", "calendars"]:
            calendar_id = _uuid(parts[2])
            if len(parts) == 3:
                calendar, role = _member(connection, calendar_id, actor, manage=method in {"PATCH", "DELETE"})
                if method == "GET":
                    result = {"calendar": _public_calendar(connection, calendar, role)}
                elif method == "PATCH":
                    _revision(body, calendar)
                    name = _text(body.get("name", calendar["name"]), "日历名称", 120)
                    color = body.get("colorHex", calendar["color_hex"])
                    description = _text(body.get("description", calendar["description"]), "日历说明", 2000, True)
                    if not isinstance(color, str) or not COLOR_PATTERN.fullmatch(color):
                        raise _invalid("日历颜色无效")
                    connection.execute("UPDATE shared_calendars SET name=?,color_hex=?,description=?,revision=revision+1,updated_at=? WHERE id=?",
                                       (name, color.upper(), description, timestamp, calendar_id))
                    calendar, role = _member(connection, calendar_id, actor)
                    result = {"calendar": _public_calendar(connection, calendar, role)}
                elif method == "DELETE":
                    connection.execute("UPDATE shared_calendars SET deleted_at=?,updated_at=?,revision=revision+1,permission_version=permission_version+1 WHERE id=?",
                                       (timestamp, timestamp, calendar_id))
                    connection.execute("UPDATE shared_calendar_invitations SET status='REVOKED',responded_at=? WHERE calendar_id=? AND status='PENDING'",
                                       (timestamp, calendar_id))
                    result = {"calendarId": calendar_id, "deleted": True}

            elif len(parts) == 4 and parts[3] == "events":
                calendar, role = _member(connection, calendar_id, actor, edit=method == "PUT")
                if method == "GET":
                    events = [json.loads(row["payload_json"]) for row in connection.execute(
                        "SELECT payload_json FROM shared_calendar_events WHERE calendar_id=? ORDER BY id", (calendar_id,),
                    )]
                    result = {"calendarId": calendar_id, "revision": calendar["revision"], "permissionVersion": calendar["permission_version"],
                              "role": role, "canEdit": role in {"owner", "editor"}, "events": [_project_event(item, role, actor.username) for item in events]}
                elif method == "PUT":
                    _revision(body, calendar)
                    raw_events = body.get("events")
                    if not isinstance(raw_events, list) or len(raw_events) > MAX_EVENTS:
                        raise _invalid(f"日程须为不超过{MAX_EVENTS}条的列表")
                    old_events = {row["id"]: json.loads(row["payload_json"]) for row in connection.execute(
                        "SELECT id,payload_json FROM shared_calendar_events WHERE calendar_id=?", (calendar_id,),
                    )}
                    usernames = {row["username"] for row in connection.execute(
                        "SELECT u.username FROM shared_calendar_members m JOIN users u ON u.id=m.user_id "
                        "WHERE m.calendar_id=? AND m.revoked_at IS NULL AND u.disabled=0", (calendar_id,),
                    )}
                    events = []
                    for item in raw_events:
                        event_id = _event_id(item.get("id")) if isinstance(item, dict) else None
                        events.append(_normalize_event(item, old_events.get(event_id), calendar["owner_username"], usernames))
                    if len({item["id"] for item in events}) != len(events):
                        raise _invalid("日程标识重复")
                    connection.execute("DELETE FROM shared_calendar_events WHERE calendar_id=?", (calendar_id,))
                    connection.executemany("INSERT INTO shared_calendar_events(calendar_id,id,payload_json,updated_at) VALUES(?,?,?,?)",
                                           [(calendar_id, item["id"], json.dumps(item, ensure_ascii=False, separators=(",", ":"), allow_nan=False), timestamp) for item in events])
                    connection.execute("UPDATE shared_calendars SET revision=revision+1,updated_at=? WHERE id=?", (timestamp, calendar_id))
                    result = {"calendarId": calendar_id, "revision": calendar["revision"] + 1, "eventCount": len(events)}

            elif len(parts) == 4 and parts[3] == "invitations" and method == "POST":
                calendar, _ = _member(connection, calendar_id, actor, manage=True)
                username = _text(body.get("username"), "邀请账号", 64).lower()
                role = body.get("role", "viewer")
                if not USERNAME_PATTERN.fullmatch(username) or not isinstance(role, str) or role not in {"editor", "viewer"}:
                    raise _invalid("邀请账号或角色无效")
                invitee = connection.execute("SELECT id FROM users WHERE username=? AND disabled=0", (username,)).fetchone()
                if invitee is None:
                    raise CalendarError(HTTPStatus.NOT_FOUND, "invitee_not_found", "可邀请账号不存在")
                current = connection.execute("SELECT role FROM shared_calendar_members WHERE calendar_id=? AND user_id=? AND revoked_at IS NULL",
                                             (calendar_id, invitee["id"])).fetchone()
                if current:
                    raise CalendarError(HTTPStatus.CONFLICT, "already_member", "该账号已经是日历成员")
                pending_count = connection.execute("SELECT count(*) FROM shared_calendar_invitations WHERE calendar_id=? AND status='PENDING'", (calendar_id,)).fetchone()[0]
                if pending_count >= MAX_MEMBERS:
                    raise CalendarError(HTTPStatus.CONFLICT, "invitation_limit", "待处理邀请数量已达上限")
                invitation_id = str(uuid.uuid4())
                try:
                    connection.execute("INSERT INTO shared_calendar_invitations(id,calendar_id,inviter_id,invitee_id,role,status,created_at) VALUES(?,?,?,?,?,'PENDING',?)",
                                       (invitation_id, calendar_id, actor.user_id, invitee["id"], role, timestamp))
                except sqlite3.IntegrityError as error:
                    raise CalendarError(HTTPStatus.CONFLICT, "invitation_pending", "该账号已有待处理邀请") from error
                result, status = {"invitation": _public_invitation(_invitation(connection, invitation_id))}, HTTPStatus.CREATED

            elif len(parts) == 5 and parts[3] == "members" and method == "DELETE":
                calendar, _ = _member(connection, calendar_id, actor, manage=True)
                username = parts[4].strip().lower()
                if not USERNAME_PATTERN.fullmatch(username):
                    raise _invalid("成员账号无效")
                member = connection.execute(
                    "SELECT m.user_id,m.role FROM shared_calendar_members m JOIN users u ON u.id=m.user_id "
                    "WHERE m.calendar_id=? AND u.username=? AND m.revoked_at IS NULL", (calendar_id, username),
                ).fetchone()
                if member is None:
                    raise CalendarError(HTTPStatus.NOT_FOUND, "member_not_found", "成员不存在")
                if member["role"] == "owner":
                    raise CalendarError(HTTPStatus.CONFLICT, "owner_required", "不能移除日历所有者")
                connection.execute("UPDATE shared_calendar_members SET revoked_at=? WHERE calendar_id=? AND user_id=?", (timestamp, calendar_id, member["user_id"]))
                connection.execute("UPDATE shared_calendars SET permission_version=permission_version+1,updated_at=? WHERE id=?", (timestamp, calendar_id))
                connection.execute("UPDATE shared_calendar_invitations SET status='REVOKED',responded_at=? WHERE calendar_id=? AND invitee_id=? AND status='PENDING'",
                                   (timestamp, calendar_id, member["user_id"]))
                result = {"calendarId": calendar_id, "username": username, "revoked": True, "permissionVersion": calendar["permission_version"] + 1}

            elif len(parts) == 6 and parts[3] == "events" and parts[5] == "rsvp" and method == "POST":
                calendar, _ = _member(connection, calendar_id, actor)
                event_id = _event_id(parts[4])
                response = body.get("response")
                if not isinstance(response, str) or response not in RESPONSES - {"NEEDS_ACTION"}:
                    raise _invalid("日程回应无效")
                row = connection.execute("SELECT payload_json FROM shared_calendar_events WHERE calendar_id=? AND id=?", (calendar_id, event_id)).fetchone()
                if row is None:
                    raise CalendarError(HTTPStatus.NOT_FOUND, "event_not_found", "日程不存在")
                event = json.loads(row["payload_json"])
                usernames = {member["username"] for member in connection.execute(
                    "SELECT u.username FROM shared_calendar_members m JOIN users u ON u.id=m.user_id "
                    "WHERE m.calendar_id=? AND m.revoked_at IS NULL AND u.disabled=0", (calendar_id,),
                )}
                participants = [dict(item) for item in event.get("participants", []) if item["username"] in usernames]
                previous = next((item for item in participants if item["username"] == actor.username), None)
                changed = previous is None or previous["response"] != response
                if previous:
                    previous["response"] = response
                else:
                    participants.append({"username": actor.username, "response": response})
                event["participants"] = participants
                event["eventParticipantsJson"] = json.dumps(participants, ensure_ascii=False, separators=(",", ":"))
                encoded = json.dumps(event, ensure_ascii=False, separators=(",", ":"))
                if len(encoded.encode("utf-8")) > MAX_EVENT_BYTES:
                    raise CalendarError(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, "event_too_large", "单条日程内容过大")
                if changed:
                    connection.execute("UPDATE shared_calendar_events SET payload_json=?,updated_at=? WHERE calendar_id=? AND id=?",
                                       (encoded, timestamp, calendar_id, event_id))
                    connection.execute("UPDATE shared_calendars SET revision=revision+1,updated_at=? WHERE id=?", (timestamp, calendar_id))
                result = {"calendarId": calendar_id, "eventId": event_id, "revision": calendar["revision"] + (1 if changed else 0), "response": response}

        if result is None:
            raise CalendarError(HTTPStatus.METHOD_NOT_ALLOWED, "method_not_allowed", "共享日历接口或请求方法无效")
        if write:
            handler.audit(connection, actor.user_id, f"calendar_{method.lower()}", "/".join(parts[:4]))

    handler.send_json(status, result)
