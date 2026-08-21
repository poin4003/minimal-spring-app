# /// script
# requires-python = ">=3.10"
# dependencies = [
#     "beautifulsoup4>=4.13,<5",
#     "httpx>=0.28,<1",
# ]
# ///

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import mimetypes
import re
import shutil
import subprocess
import tempfile
import time
import zipfile
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Any, Iterable
from urllib.parse import unquote, urljoin

import httpx
from bs4 import BeautifulSoup


# Run with: uv run scripts/import_telegram_conversation.py
# Preview classification only: uv run scripts/import_telegram_conversation.py --dry-run

# -----------------------------------------------------------------------------
# Required configuration
# -----------------------------------------------------------------------------

BASE_URL = "https://archlinux.tail582cf7.ts.net/"

POSTER_EMAIL = "thuylinh008@user.com"
POSTER_PASSWORD = "12345678"

ADMIN_EMAIL = "system@admin.com"
ADMIN_PASSWORD = "12345678"

TELEGRAM_EXPORT_ZIP = Path(__file__).with_name("ChatExport_2026-08-21.zip")
CHECKPOINT_PATH = Path(__file__).with_name(".telegram-import-checkpoint.json")

# Set to False only for a trusted development server with a self-signed certificate.
VERIFY_TLS = True

# -----------------------------------------------------------------------------
# Import behavior
# -----------------------------------------------------------------------------

AUTO_PUBLISH = True
DEFAULT_LIMIT: int | None = None

DIRECT_UPLOAD_THRESHOLD_BYTES = 16 * 1024 * 1024
MEDIA_READY_TIMEOUT_SECONDS = 30 * 60
MEDIA_READY_POLL_SECONDS = 5
UPLOAD_DELAY_SECONDS = 2.1
HTTP_TIMEOUT_SECONDS = 120

# Keep these values aligned with app.post.short-post in application.yaml.
SHORT_MAX_DURATION_SECONDS = 3 * 60
SHORT_MIN_ASPECT_RATIO = 0.5
SHORT_MAX_ASPECT_RATIO = 1.0
SHORT_MIN_SHORT_EDGE = 720

FFPROBE_EXECUTABLE = "ffprobe"

# Change these paths only when the matching application.yaml paths are customized.
AUTH_LOGIN_PATH = "/api/v1/auth/login"
AUTH_REFRESH_PATH = "/api/v1/auth/refresh"
MEDIA_UPLOAD_PATH = "/api/v1/media"
MEDIA_CHUNK_UPLOAD_PATH = "/api/v1/media/uploads"
STANDARD_CREATE_PATH = "/my/posts/create"
SHORT_CREATE_PATH = "/my/shorts/create"
VIDEO_CREATE_PATH = "/my/videos/create"
MODERATION_PATH = "/admin/posts/moderation"

ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN"
REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN"
CSRF_COOKIE = "XSRF-TOKEN"
CSRF_HEADER = "X-XSRF-TOKEN"

POSTER_REQUIRED_PERMISSIONS = {
    "POST:CREATE",
    "POST:UPDATE:OWN",
}
ADMIN_REQUIRED_PERMISSIONS = {"POST:MODERATE"}

UUID_PATTERN = re.compile(
    r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}"
    r"-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
)


class ImportFailure(RuntimeError):
    pass


class AppRequestFailure(ImportFailure):
    def __init__(
        self,
        message: str,
        *,
        status_code: int = 0,
        error: str | None = None,
    ) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.error = error


@dataclass(frozen=True)
class TelegramMediaItem:
    key: str
    message_id: str
    archive_path: str
    media_type: str
    caption: str
    sender: str
    sent_at: str
    duration_hint_seconds: float | None


@dataclass(frozen=True)
class VideoProbe:
    duration_seconds: float
    width: int
    height: int


@dataclass(frozen=True)
class ClassifiedMedia:
    post_type: str
    probe: VideoProbe | None


class CheckpointStore:
    def __init__(self, path: Path, source_zip: Path) -> None:
        self.path = path
        stat = source_zip.stat()
        self.source = {
            "name": source_zip.name,
            "size": stat.st_size,
            "modifiedNs": stat.st_mtime_ns,
        }
        self.data = self._load()

    def _load(self) -> dict[str, Any]:
        if not self.path.exists():
            return {"source": self.source, "items": {}}

        try:
            data = json.loads(self.path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise ImportFailure(
                f"Cannot read checkpoint {self.path}: {exc}"
            ) from exc

        if data.get("source") != self.source:
            raise ImportFailure(
                "The Telegram ZIP changed after the checkpoint was created. "
                "Run again with --reset-checkpoint to start a new import."
            )
        data.setdefault("items", {})
        return data

    def item(self, key: str) -> dict[str, Any]:
        return self.data["items"].setdefault(key, {})

    def save(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        temporary_path = self.path.with_suffix(self.path.suffix + ".tmp")
        temporary_path.write_text(
            json.dumps(self.data, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        temporary_path.replace(self.path)


class VibeClient:
    def __init__(self, email: str, password: str) -> None:
        self.email = email
        self.password = password
        self.access_token: str | None = None
        self.refresh_token: str | None = None
        self.access_token_expires_at = 0.0
        self.client = httpx.Client(
            base_url=BASE_URL.rstrip("/"),
            verify=VERIFY_TLS,
            follow_redirects=False,
            timeout=httpx.Timeout(HTTP_TIMEOUT_SECONDS),
            headers={"Accept-Language": "en"},
        )

    def close(self) -> None:
        self.client.close()

    def login(self) -> None:
        response = self.client.post(
            AUTH_LOGIN_PATH,
            json={"email": self.email, "password": self.password},
        )
        result = self._require_api_result(response)
        self._apply_tokens(result)

    def ensure_access_token(self) -> None:
        if self.access_token is None:
            self.login()
            return
        if time.time() < self.access_token_expires_at - 60:
            return
        self.refresh()

    def refresh(self) -> None:
        if not self.refresh_token:
            self.login()
            return

        response = self.client.post(
            AUTH_REFRESH_PATH,
            json={"refreshToken": self.refresh_token},
        )
        if response.status_code >= 400:
            self.login()
            return
        self._apply_tokens(self._require_api_result(response))

    def api_request(self, method: str, path: str, **kwargs: Any) -> Any:
        self.ensure_access_token()
        request_headers = dict(kwargs.pop("headers", {}))
        for attempt in range(3):
            headers = dict(request_headers)
            headers["Authorization"] = f"Bearer {self.access_token}"
            response = self.client.request(
                method,
                path,
                headers=headers,
                **kwargs,
            )

            if response.status_code == 401 and attempt == 0:
                self.refresh()
                continue
            if response.status_code == 429 or response.status_code in {
                502,
                503,
                504,
            }:
                if attempt < 2:
                    delay = self._retry_delay(response, attempt)
                    log(f"Request throttled/unavailable; retrying in {delay:.1f}s")
                    time.sleep(delay)
                    continue
            return self._require_api_result(response)

        raise ImportFailure(f"Request failed after retries: {method} {path}")

    def seed_csrf(self, path: str) -> None:
        self.ensure_access_token()
        response = self.client.get(path)
        if self._is_login_redirect(response):
            self.login()
            response = self.client.get(path)
        if response.status_code >= 400:
            raise AppRequestFailure(
                f"Cannot initialize CSRF from {path}: "
                f"HTTP {response.status_code} {summarize_html(response.text)}",
                status_code=response.status_code,
            )
        if not self._cookie_value(CSRF_COOKIE):
            raise ImportFailure(f"Server did not issue {CSRF_COOKIE} for {path}")

    def web_post(
        self,
        path: str,
        *,
        data: dict[str, Any] | list[tuple[str, str]] | None = None,
        csrf_seed_path: str,
    ) -> httpx.Response:
        self.ensure_access_token()
        if not self._cookie_value(CSRF_COOKIE):
            self.seed_csrf(csrf_seed_path)

        csrf_token = unquote(self._cookie_value(CSRF_COOKIE) or "")
        response = self.client.post(
            path,
            data=data or {},
            headers={CSRF_HEADER: csrf_token, "Accept": "text/html"},
        )
        if self._is_login_redirect(response):
            self.login()
            self.seed_csrf(csrf_seed_path)
            csrf_token = unquote(self._cookie_value(CSRF_COOKIE) or "")
            response = self.client.post(
                path,
                data=data or {},
                headers={CSRF_HEADER: csrf_token, "Accept": "text/html"},
            )

        if response.status_code >= 400:
            raise AppRequestFailure(
                f"Web action failed: POST {path} -> HTTP {response.status_code}: "
                f"{summarize_html(response.text)}",
                status_code=response.status_code,
            )
        return response

    def _apply_tokens(self, result: dict[str, Any]) -> None:
        access_token = result.get("accessToken")
        refresh_token = result.get("refreshToken")
        if not access_token or not refresh_token:
            raise ImportFailure("Login response does not contain JWT tokens")

        self.access_token = str(access_token)
        self.refresh_token = str(refresh_token)
        self.access_token_expires_at = jwt_expiration(self.access_token)
        self.client.cookies.set(ACCESS_TOKEN_COOKIE, self.access_token, path="/")
        self.client.cookies.set(REFRESH_TOKEN_COOKIE, self.refresh_token, path="/")

    def _require_api_result(self, response: httpx.Response) -> Any:
        try:
            payload = response.json()
        except json.JSONDecodeError as exc:
            raise AppRequestFailure(
                f"Expected JSON from {response.request.url}, got HTTP "
                f"{response.status_code}: {response.text[:500]}",
                status_code=response.status_code,
            ) from exc

        if response.status_code >= 400 or payload.get("success") is not True:
            raise AppRequestFailure(
                payload.get("message") or f"HTTP {response.status_code}",
                status_code=response.status_code,
                error=payload.get("error"),
            )
        return payload.get("result")

    def _cookie_value(self, name: str) -> str | None:
        values = [cookie.value for cookie in self.client.cookies.jar if cookie.name == name]
        return values[-1] if values else None

    @staticmethod
    def _retry_delay(response: httpx.Response, attempt: int) -> float:
        retry_after = response.headers.get("Retry-After")
        if retry_after:
            try:
                return max(1.0, float(retry_after))
            except ValueError:
                pass
        return float(2 ** (attempt + 1))

    @staticmethod
    def _is_login_redirect(response: httpx.Response) -> bool:
        if response.status_code not in {301, 302, 303, 307, 308}:
            return False
        return "/login" in response.headers.get("Location", "")


class TelegramImporter:
    def __init__(
        self,
        source_zip: Path,
        checkpoint: CheckpointStore,
        poster: VibeClient,
        admin: VibeClient,
    ) -> None:
        self.source_zip = source_zip
        self.checkpoint = checkpoint
        self.poster = poster
        self.admin = admin

    def run(self, items: list[TelegramMediaItem], limit: int | None) -> None:
        selected = items[:limit] if limit is not None else items
        published = 0
        skipped = 0
        failed = 0

        with zipfile.ZipFile(self.source_zip) as archive:
            with tempfile.TemporaryDirectory(prefix="vibe-telegram-import-") as temp:
                temp_directory = Path(temp)
                for index, item in enumerate(selected, start=1):
                    state = self.checkpoint.item(item.key)
                    if state.get("published") is True:
                        skipped += 1
                        log(f"[{index}/{len(selected)}] Skip published {item.archive_path}")
                        continue

                    try:
                        self._process_item(
                            archive,
                            temp_directory,
                            item,
                            state,
                            index,
                            len(selected),
                        )
                        published += 1
                    except Exception as exc:
                        failed += 1
                        state["lastError"] = str(exc)
                        state["lastErrorAt"] = int(time.time())
                        self.checkpoint.save()
                        log(f"ERROR {item.key}: {exc}")

        log(
            f"Import finished: published={published}, skipped={skipped}, "
            f"failed={failed}"
        )
        if failed:
            raise ImportFailure(
                "Some items failed. Fix the reported issue and rerun; the checkpoint "
                "will continue from incomplete items."
            )

    def _process_item(
        self,
        archive: zipfile.ZipFile,
        temp_directory: Path,
        item: TelegramMediaItem,
        state: dict[str, Any],
        index: int,
        total: int,
    ) -> None:
        destination = temp_directory / safe_temp_name(item)
        extract_archive_entry(archive, item.archive_path, destination)
        try:
            classified = classify_media(item, destination)
            probe_label = format_probe(classified.probe)
            log(
                f"[{index}/{total}] {item.archive_path} -> "
                f"{classified.post_type}{probe_label}"
            )

            if not state.get("mediaId"):
                media = self._upload_media(
                    destination,
                    PurePosixPath(item.archive_path).name,
                    state,
                )
                state.update(
                    {
                        "archivePath": item.archive_path,
                        "postType": classified.post_type,
                        "mediaId": media["id"],
                        "mediaPublicKey": media.get("publicKey"),
                        "mediaContentUrl": media.get("contentUrl"),
                        "mediaProcessingStatus": media.get("processingStatus"),
                    }
                )
                state.pop("lastError", None)
                self.checkpoint.save()
                time.sleep(UPLOAD_DELAY_SECONDS)

            self._wait_until_ready(state, classified.post_type)

            if not state.get("postId"):
                post_id = self._create_post(item, classified, state["mediaId"])
                state["postId"] = post_id
                self.checkpoint.save()

            if not state.get("submitted"):
                self._submit_post(classified.post_type, state["postId"])
                state["submitted"] = True
                self.checkpoint.save()

            if AUTO_PUBLISH and not state.get("published"):
                self._publish_post(state["postId"])
                state["published"] = True
                state["publishedAt"] = int(time.time())
                state.pop("lastError", None)
                self.checkpoint.save()
        finally:
            destination.unlink(missing_ok=True)

    def _upload_media(
        self,
        file_path: Path,
        original_name: str,
        state: dict[str, Any],
    ) -> dict[str, Any]:
        if file_path.stat().st_size <= DIRECT_UPLOAD_THRESHOLD_BYTES:
            log(
                f"  Uploading {original_name} directly "
                f"({format_bytes(file_path.stat().st_size)})"
            )
            mime_type = mimetypes.guess_type(file_path.name)[0] or "application/octet-stream"
            media = self.poster.api_request(
                "POST",
                MEDIA_UPLOAD_PATH,
                files={
                    "file": (
                        original_name,
                        file_path.read_bytes(),
                        mime_type,
                    )
                },
            )
            return require_mapping(media, "direct media upload")

        log(
            f"  Uploading {original_name} in chunks "
            f"({format_bytes(file_path.stat().st_size)})"
        )
        return self._upload_chunked(file_path, original_name, state)

    def _upload_chunked(
        self,
        file_path: Path,
        original_name: str,
        state: dict[str, Any],
    ) -> dict[str, Any]:
        upload_id = state.get("uploadSessionId")
        session: dict[str, Any] | None = None

        if upload_id:
            try:
                session = require_mapping(
                    self.poster.api_request(
                        "GET",
                        f"{MEDIA_CHUNK_UPLOAD_PATH}/{upload_id}",
                    ),
                    "media upload session",
                )
            except AppRequestFailure as exc:
                if exc.error not in {"RESOURCE_NOT_FOUND", "INVALID_PARAM"}:
                    raise
                upload_id = None

        if not upload_id:
            session = require_mapping(
                self.poster.api_request(
                    "POST",
                    MEDIA_CHUNK_UPLOAD_PATH,
                    json={
                        "originalName": original_name,
                        "fileSize": file_path.stat().st_size,
                    },
                ),
                "new media upload session",
            )
            upload_id = str(session["id"])
            state["uploadSessionId"] = upload_id
            self.checkpoint.save()

        if session is None:
            raise ImportFailure("Chunk upload session was not initialized")
        if session.get("status") == "COMPLETED":
            completed = require_mapping(
                session.get("completedMedia"),
                "completed chunk media",
            )
            state.pop("uploadSessionId", None)
            return completed
        if session.get("status") != "UPLOADING":
            raise ImportFailure(
                f"Upload session {upload_id} is not resumable: {session.get('status')}"
            )

        chunk_size = int(session["chunkSize"])
        total_chunks = int(session["totalChunks"])
        uploaded_chunks = {int(value) for value in session.get("uploadedChunks", [])}

        with file_path.open("rb") as source:
            for chunk_index in range(total_chunks):
                if chunk_index in uploaded_chunks:
                    continue
                source.seek(chunk_index * chunk_size)
                chunk = source.read(chunk_size)
                self.poster.api_request(
                    "PUT",
                    f"{MEDIA_CHUNK_UPLOAD_PATH}/{upload_id}/chunks/{chunk_index}",
                    content=chunk,
                    headers={
                        "Content-Type": "application/octet-stream",
                        "Content-Length": str(len(chunk)),
                        "X-Chunk-SHA256": hashlib.sha256(chunk).hexdigest(),
                    },
                )

        media = require_mapping(
            self.poster.api_request(
                "POST",
                f"{MEDIA_CHUNK_UPLOAD_PATH}/{upload_id}/complete",
            ),
            "completed media upload",
        )
        state.pop("uploadSessionId", None)
        return media

    def _wait_until_ready(self, state: dict[str, Any], post_type: str) -> None:
        if state.get("mediaReady") is True:
            return
        if state.get("mediaProcessingStatus") == "FAILED":
            raise ImportFailure(f"Media {state['mediaId']} processing failed")
        if post_type == "STANDARD" and state.get("mediaProcessingStatus") == "READY":
            state["mediaReady"] = True
            self.checkpoint.save()
            return

        content_url = state.get("mediaContentUrl")
        if not content_url:
            public_key = state.get("mediaPublicKey")
            if not public_key:
                raise ImportFailure("Uploaded media has no public key or content URL")
            suffix = "/hls/index.m3u8" if post_type in {"SHORT", "VIDEO"} else ""
            content_url = f"/api/v1/public/media/{public_key}{suffix}"

        deadline = time.monotonic() + MEDIA_READY_TIMEOUT_SECONDS
        absolute_url = urljoin(BASE_URL.rstrip("/") + "/", str(content_url).lstrip("/"))
        while time.monotonic() < deadline:
            response = self.poster.client.get(absolute_url)
            if response.status_code == 200:
                state["mediaReady"] = True
                state["mediaProcessingStatus"] = "READY"
                self.checkpoint.save()
                return
            if response.status_code not in {404, 409, 423, 425}:
                raise AppRequestFailure(
                    f"Unexpected media readiness response: HTTP {response.status_code}",
                    status_code=response.status_code,
                )
            time.sleep(MEDIA_READY_POLL_SECONDS)

        raise ImportFailure(
            f"Media {state['mediaId']} did not become READY within "
            f"{MEDIA_READY_TIMEOUT_SECONDS}s"
        )

    def _create_post(
        self,
        item: TelegramMediaItem,
        classified: ClassifiedMedia,
        media_id: str,
    ) -> str:
        if classified.post_type == "STANDARD":
            response = self.poster.web_post(
                STANDARD_CREATE_PATH,
                data={"content": item.caption, "mediaIds": media_id},
                csrf_seed_path=STANDARD_CREATE_PATH,
            )
        elif classified.post_type == "SHORT":
            response = self.poster.web_post(
                SHORT_CREATE_PATH,
                data={"caption": truncate(item.caption, 1000), "mediaId": media_id},
                csrf_seed_path=SHORT_CREATE_PATH,
            )
        else:
            title, description = video_text(item)
            response = self.poster.web_post(
                VIDEO_CREATE_PATH,
                data={
                    "title": title,
                    "description": description,
                    "sourceMediaId": media_id,
                },
                csrf_seed_path=VIDEO_CREATE_PATH,
            )
        return require_redirect_uuid(response, "create post")

    def _submit_post(self, post_type: str, post_id: str) -> None:
        owner_base_path = {
            "STANDARD": "/my/posts",
            "SHORT": "/my/shorts",
            "VIDEO": "/my/videos",
        }[post_type]
        response = self.poster.web_post(
            f"{owner_base_path}/{post_id}/submit",
            csrf_seed_path=f"{owner_base_path}/{post_id}",
        )
        require_redirect(response, "submit post")

    def _publish_post(self, post_id: str) -> None:
        response = self.admin.web_post(
            f"{MODERATION_PATH}/{post_id}/publish",
            csrf_seed_path=MODERATION_PATH,
        )
        require_redirect(response, "publish post")


def parse_telegram_export(source_zip: Path) -> list[TelegramMediaItem]:
    with zipfile.ZipFile(source_zip) as archive:
        html_entries = [
            entry for entry in archive.namelist() if entry.endswith("/messages.html")
        ]
        if len(html_entries) != 1:
            raise ImportFailure(
                f"Expected one messages.html in {source_zip}, found {len(html_entries)}"
            )

        messages_entry = html_entries[0]
        export_root = PurePosixPath(messages_entry).parent
        html = archive.read(messages_entry).decode("utf-8")
        available_entries = set(archive.namelist())

    soup = BeautifulSoup(html, "html.parser")
    items: list[TelegramMediaItem] = []
    current_sender = ""

    for message in soup.select("div.message.default"):
        body = message.find("div", class_="body", recursive=False)
        if body is None:
            continue

        sender_element = body.find("div", class_="from_name", recursive=False)
        if sender_element is not None:
            current_sender = sender_element.get_text(" ", strip=True)

        anchor = body.select_one("a.photo_wrap[href], a.video_file_wrap[href]")
        if anchor is None:
            continue

        href = str(anchor.get("href", "")).strip()
        relative_path = PurePosixPath(href)
        if relative_path.is_absolute() or ".." in relative_path.parts:
            raise ImportFailure(f"Unsafe Telegram media path: {href}")
        archive_path = str(export_root / relative_path)
        if archive_path not in available_entries:
            raise ImportFailure(f"Telegram media file is missing from ZIP: {archive_path}")

        message_id = str(message.get("id", "")).removeprefix("message") or str(
            len(items) + 1
        )
        caption_element = body.find("div", class_="text", recursive=False)
        date_element = body.select_one(".date.details")
        duration_element = body.select_one(".video_duration")
        media_type = "PHOTO" if "photo_wrap" in anchor.get("class", []) else "VIDEO"
        key = f"{message_id}:{archive_path}"

        items.append(
            TelegramMediaItem(
                key=key,
                message_id=message_id,
                archive_path=archive_path,
                media_type=media_type,
                caption=normalize_text(
                    caption_element.get_text("\n", strip=True)
                    if caption_element is not None
                    else ""
                ),
                sender=current_sender,
                sent_at=(
                    str(date_element.get("title", ""))
                    if date_element is not None
                    else ""
                ),
                duration_hint_seconds=(
                    parse_duration(duration_element.get_text(strip=True))
                    if duration_element is not None
                    else None
                ),
            )
        )

    return items


def classify_media(item: TelegramMediaItem, file_path: Path) -> ClassifiedMedia:
    if item.media_type == "PHOTO":
        return ClassifiedMedia(post_type="STANDARD", probe=None)

    probe = probe_video(file_path, item.duration_hint_seconds)
    aspect_ratio = probe.width / probe.height
    short_edge = min(probe.width, probe.height)
    is_short = (
        probe.duration_seconds <= SHORT_MAX_DURATION_SECONDS
        and SHORT_MIN_ASPECT_RATIO <= aspect_ratio <= SHORT_MAX_ASPECT_RATIO
        and short_edge >= SHORT_MIN_SHORT_EDGE
    )
    return ClassifiedMedia(post_type="SHORT" if is_short else "VIDEO", probe=probe)


def probe_video(file_path: Path, duration_hint: float | None) -> VideoProbe:
    command = [
        FFPROBE_EXECUTABLE,
        "-v",
        "error",
        "-select_streams",
        "v:0",
        "-show_entries",
        "stream=width,height,duration:stream_tags=rotate:stream_side_data=rotation:format=duration",
        "-of",
        "json",
        str(file_path),
    ]
    try:
        result = subprocess.run(
            command,
            check=True,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
    except (OSError, subprocess.CalledProcessError) as exc:
        details = exc.stderr.strip() if isinstance(exc, subprocess.CalledProcessError) else str(exc)
        raise ImportFailure(f"ffprobe failed for {file_path.name}: {details}") from exc

    try:
        payload = json.loads(result.stdout)
        stream = payload["streams"][0]
        width = int(stream["width"])
        height = int(stream["height"])
    except (KeyError, IndexError, TypeError, ValueError, json.JSONDecodeError) as exc:
        raise ImportFailure(f"ffprobe returned no usable video stream for {file_path.name}") from exc

    rotation = first_number(
        stream.get("tags", {}).get("rotate"),
        *(entry.get("rotation") for entry in stream.get("side_data_list", [])),
    )
    if rotation is not None and int(abs(rotation)) % 180 == 90:
        width, height = height, width

    duration = first_number(
        stream.get("duration"),
        payload.get("format", {}).get("duration"),
        duration_hint,
    )
    if duration is None or duration <= 0 or width <= 0 or height <= 0:
        raise ImportFailure(f"Invalid video metadata for {file_path.name}")
    return VideoProbe(duration_seconds=duration, width=width, height=height)


def extract_archive_entry(
    archive: zipfile.ZipFile,
    archive_path: str,
    destination: Path,
) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    with archive.open(archive_path) as source, destination.open("wb") as target:
        shutil.copyfileobj(source, target, length=1024 * 1024)


def safe_temp_name(item: TelegramMediaItem) -> str:
    suffix = PurePosixPath(item.archive_path).suffix.lower()
    return f"telegram-{re.sub(r'[^a-zA-Z0-9_-]', '_', item.message_id)}{suffix}"


def video_text(item: TelegramMediaItem) -> tuple[str, str]:
    lines = [line.strip() for line in item.caption.splitlines() if line.strip()]
    if lines:
        title = truncate(lines[0], 255)
        description = truncate("\n".join(lines[1:]), 10_000)
        return title, description

    fallback = PurePosixPath(item.archive_path).stem.strip()
    return truncate(fallback or f"Telegram video {item.message_id}", 255), ""


def require_redirect_uuid(response: httpx.Response, action: str) -> str:
    location = require_redirect(response, action)
    match = UUID_PATTERN.search(location)
    if not match:
        raise ImportFailure(f"Cannot find post UUID after {action}: {location}")
    return match.group(0)


def require_redirect(response: httpx.Response, action: str) -> str:
    location = response.headers.get("Location") or response.headers.get("HX-Redirect")
    if response.status_code in {301, 302, 303, 307, 308} and location:
        if "/login" in location:
            raise ImportFailure(f"Authentication was lost while attempting to {action}")
        return location
    raise ImportFailure(
        f"Expected redirect after {action}, got HTTP {response.status_code}: "
        f"{summarize_html(response.text)}"
    )


def require_mapping(value: Any, context: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ImportFailure(f"Expected an object for {context}")
    return value


def jwt_expiration(token: str) -> float:
    payload = jwt_payload(token)
    try:
        return float(payload["exp"])
    except (KeyError, TypeError, ValueError):
        return time.time() + 5 * 60


def jwt_payload(token: str) -> dict[str, Any]:
    try:
        payload_part = token.split(".")[1]
        payload_part += "=" * (-len(payload_part) % 4)
        payload = json.loads(base64.urlsafe_b64decode(payload_part))
    except (IndexError, TypeError, ValueError, json.JSONDecodeError) as exc:
        raise ImportFailure("Cannot decode JWT claims") from exc
    if not isinstance(payload, dict):
        raise ImportFailure("JWT claims are not an object")
    return payload


def require_permissions(
    client: VibeClient,
    required_permissions: set[str],
    account_label: str,
) -> None:
    if client.access_token is None:
        raise ImportFailure(f"{account_label} is not authenticated")

    payload = jwt_payload(client.access_token)
    permissions = {
        str(permission)
        for permission in payload.get("permissions", [])
        if permission is not None
    }
    missing = sorted(required_permissions - permissions)
    if not missing:
        return

    role_hint = "USER" if account_label == "Poster account" else "SUPER_ADMIN"
    raise ImportFailure(
        f"{account_label} {client.email} is missing permissions: "
        f"{', '.join(missing)}. Assign the {role_hint} role in "
        "Admin > Users > Manage Roles, log in again, then rerun this script."
    )


def format_bytes(value: int) -> str:
    size = float(value)
    for unit in ("B", "KB", "MB", "GB"):
        if size < 1024 or unit == "GB":
            return f"{size:.1f} {unit}"
        size /= 1024
    return f"{size:.1f} GB"


def summarize_html(html: str) -> str:
    if not html:
        return "empty response"
    soup = BeautifulSoup(html, "html.parser")
    candidates = soup.select(
        ".alert-danger, .invalid-feedback, [data-field-error], h1, h2, title"
    )
    messages = [normalize_text(node.get_text(" ", strip=True)) for node in candidates]
    messages = [message for message in messages if message]
    return " | ".join(dict.fromkeys(messages))[:1000] or normalize_text(
        soup.get_text(" ", strip=True)
    )[:1000]


def normalize_text(value: str) -> str:
    lines = [re.sub(r"\s+", " ", line).strip() for line in value.splitlines()]
    return "\n".join(line for line in lines if line)


def parse_duration(value: str) -> float:
    parts = [float(part) for part in value.split(":")]
    total = 0.0
    for part in parts:
        total = total * 60 + part
    return total


def first_number(*values: Any) -> float | None:
    for value in values:
        if value is None:
            continue
        try:
            number = float(value)
        except (TypeError, ValueError):
            continue
        if number == number:
            return number
    return None


def truncate(value: str, max_length: int) -> str:
    return value if len(value) <= max_length else value[:max_length]


def format_probe(probe: VideoProbe | None) -> str:
    if probe is None:
        return ""
    return (
        f" ({probe.duration_seconds:.1f}s, "
        f"{probe.width}x{probe.height})"
    )


def validate_configuration(dry_run: bool) -> None:
    if not TELEGRAM_EXPORT_ZIP.is_file():
        raise ImportFailure(f"Telegram export ZIP not found: {TELEGRAM_EXPORT_ZIP}")
    if shutil.which(FFPROBE_EXECUTABLE) is None:
        raise ImportFailure(f"ffprobe executable not found: {FFPROBE_EXECUTABLE}")
    if dry_run:
        return

    values = {
        "POSTER_EMAIL": POSTER_EMAIL,
        "POSTER_PASSWORD": POSTER_PASSWORD,
        "ADMIN_EMAIL": ADMIN_EMAIL,
        "ADMIN_PASSWORD": ADMIN_PASSWORD,
    }
    missing = [name for name, value in values.items() if not value or "CHANGE_ME" in value]
    if missing:
        raise ImportFailure(
            "Configure these values at the top of the script: " + ", ".join(missing)
        )


def run_dry(items: Iterable[TelegramMediaItem], limit: int | None) -> None:
    selected = list(items)
    if limit is not None:
        selected = selected[:limit]
    counts = {"STANDARD": 0, "SHORT": 0, "VIDEO": 0}

    with zipfile.ZipFile(TELEGRAM_EXPORT_ZIP) as archive:
        with tempfile.TemporaryDirectory(prefix="vibe-telegram-dry-run-") as temp:
            temp_directory = Path(temp)
            for index, item in enumerate(selected, start=1):
                destination = temp_directory / safe_temp_name(item)
                extract_archive_entry(archive, item.archive_path, destination)
                try:
                    classified = classify_media(item, destination)
                    counts[classified.post_type] += 1
                    log(
                        f"[{index}/{len(selected)}] {item.archive_path} -> "
                        f"{classified.post_type}{format_probe(classified.probe)}"
                    )
                finally:
                    destination.unlink(missing_ok=True)

    log(
        "Classification: "
        + ", ".join(f"{post_type}={count}" for post_type, count in counts.items())
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Import a Telegram HTML conversation export into Vibe."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Only parse and classify media; do not call Vibe.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=DEFAULT_LIMIT,
        help="Process only the first N media items.",
    )
    parser.add_argument(
        "--reset-checkpoint",
        action="store_true",
        help="Delete the existing checkpoint before importing.",
    )
    return parser.parse_args()


def log(message: str) -> None:
    print(message, flush=True)


def main() -> int:
    args = parse_args()
    try:
        validate_configuration(args.dry_run)
        if args.limit is not None and args.limit <= 0:
            raise ImportFailure("--limit must be greater than zero")
        if args.reset_checkpoint:
            CHECKPOINT_PATH.unlink(missing_ok=True)

        items = parse_telegram_export(TELEGRAM_EXPORT_ZIP)
        log(f"Found {len(items)} media items in {TELEGRAM_EXPORT_ZIP.name}")
        if args.dry_run:
            run_dry(items, args.limit)
            return 0

        checkpoint = CheckpointStore(CHECKPOINT_PATH, TELEGRAM_EXPORT_ZIP)
        poster = VibeClient(POSTER_EMAIL, POSTER_PASSWORD)
        admin = VibeClient(ADMIN_EMAIL, ADMIN_PASSWORD)
        try:
            poster.login()
            if AUTO_PUBLISH:
                admin.login()
            require_permissions(
                poster,
                POSTER_REQUIRED_PERMISSIONS,
                "Poster account",
            )
            if AUTO_PUBLISH:
                require_permissions(
                    admin,
                    ADMIN_REQUIRED_PERMISSIONS,
                    "Admin account",
                )
            importer = TelegramImporter(
                TELEGRAM_EXPORT_ZIP,
                checkpoint,
                poster,
                admin,
            )
            importer.run(items, args.limit)
        finally:
            poster.close()
            admin.close()
        return 0
    except KeyboardInterrupt:
        log(
            "Import interrupted. Run the same command again to continue from "
            "the checkpoint."
        )
        return 130
    except (ImportFailure, httpx.HTTPError, zipfile.BadZipFile) as exc:
        log(f"Import stopped: {exc}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
