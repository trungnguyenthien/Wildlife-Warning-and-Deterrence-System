# Giao thức WebSocket giữa Camera (RPi) và AI_Server

> Tài liệu này mô tả giao thức WebSocket chuẩn cho kết nối giữa **Camera** (Raspberry Pi - client) và **AI_Server** (server Python).
>
> Cả hai module đều viết bằng Python. Yêu cầu thư viện: `websockets >= 13.0`.

---

## 1. Tổng quan

```
Camera (RPi)  ── WS ──▶  AI_Server
     ↑                     │
     └───────── WS ◀───────┘
```

| Hướng | Giao thức | Bên chủ động |
|-------|-----------|--------------|
| Camera → AI_Server | Gửi ảnh, heartbeat, trạng thái thiết bị | **Camera** kết nối WebSocket đến AI_Server |
| AI_Server → Camera | Gửi lệnh phòng vệ, lệnh test, cấu hình | **AI_Server** gửi qua kết nối có sẵn |

---

## 2. Kết nối

```
ws://<AI_SERVER_HOST>:5000/camera/ws?cameraId={cameraId}&token={apiToken}
```

- **Camera** là bên chủ động mở kết nối WebSocket
- **cameraId**: định danh trạm (vd: `cam-001`)
- **token**: API token để xác thực thiết bị
- Auto-reconnect với exponential backoff nếu mất kết nối

---

## 3. Định dạng bản tin

Tất cả bản tin đều là **JSON** với cấu trúc:

```json
{
  "event": "TÊN_EVENT",
  "payload": { ... }
}
```

---

## 4. Bản tin Uplink (Camera → AI_Server)

### 4.1. Gửi ảnh khi phát hiện chuyển động

```json
{
  "event": "IMAGE_DATA",
  "payload": {
    "imageBase64": "<base64 của ảnh JPEG>",
    "timestamp": "2026-07-25T14:30:00Z",
    "motionScore": 0.85
  }
}
```

### 4.2. Heartbeat (mỗi 30 giây)

```json
{
  "event": "DEVICE_HEARTBEAT",
  "payload": {
    "cameraId": "cam-001",
    "cpuTemp": 52.3,
    "uptime": 3600
  }
}
```

### 4.3. Báo cáo trạng thái thiết bị vật lý

```json
{
  "event": "DEVICE_STATUS",
  "payload": {
    "deviceKey": "speaker",
    "status": "ON",
    "errorMsg": null
  }
}
```

---

## 5. Bản tin Downlink (AI_Server → Camera)

### 5.1. Lệnh phòng vệ (sau khi phân tích ảnh)

```json
{
  "event": "DEFEND_ACTION",
  "payload": {
    "actionId": "act-123",
    "ledFlash": true,
    "ledColor": "STROBE",
    "speakerWarn": true,
    "audioSampleId": "A_gunshot",
    "audioIntensity": 80,
    "electricFence": true,
    "electricFenceDuration": 15
  }
}
```

### 5.2. Lệnh test thiết bị

```json
{
  "event": "DEVICE_COMMAND",
  "payload": {
    "commandId": "cmd-456",
    "deviceKey": "speaker",
    "action": "TEST",
    "params": {
      "durationSeconds": 5,
      "sampleId": "A_gunshot"
    }
  }
}
```

### 5.3. Cập nhật cấu hình

```json
{
  "event": "CONFIG_UPDATE",
  "payload": {
    "ledColor": "RED",
    "audioSampleId": "A_growl",
    "fenceLevel": "medium"
  }
}
```

---

## 6. Code mẫu Python

### 6.1. AI_Server (server)

Dùng `asyncio` + `websockets` (≥ 13.0):

```python
import asyncio
import base64
import json
from urllib.parse import urlsplit, parse_qs

from websockets.asyncio.server import serve

CAMERA_TOKENS = {"cam-001": "token-abc-123"}


async def handle_camera(websocket):
    # websocket.request.path chứa cả path + query string, vd:
    # "/camera/ws?cameraId=cam-001&token=token-abc-123"
    query = urlsplit(websocket.request.path).query
    params = parse_qs(query)
    camera_id = params.get("cameraId", [None])[0]
    token = params.get("token", [None])[0]

    if not camera_id or CAMERA_TOKENS.get(camera_id) != token:
        await websocket.close(code=4001, reason="Unauthorized")
        return

    print(f"[AI_Server] Camera {camera_id} đã kết nối")

    async for message in websocket:
        try:
            data = json.loads(message)
        except json.JSONDecodeError:
            print(f"[AI_Server] Message không phải JSON hợp lệ từ {camera_id}, bỏ qua")
            continue

        event = data.get("event")

        if event == "IMAGE_DATA":
            # Giải mã base64 -> bytes ảnh JPEG thật
            img_bytes = base64.b64decode(data["payload"]["imageBase64"])
            print(
                f"[AI_Server] Nhận ảnh từ {camera_id} "
                f"({len(img_bytes)} bytes), timestamp={data['payload']['timestamp']}"
            )

            # TODO: Chạy YOLOv8 inference ở đây
            # result = run_yolo_inference(img_bytes)
            # Gửi kết quả phân tích lên Mobile_Server qua HTTP POST
            # Nhận @DefendAction từ Mobile_Server

            # Gửi lệnh phòng vệ xuống Camera
            await websocket.send(json.dumps({
                "event": "DEFEND_ACTION",
                "payload": {
                    "actionId": "act-123",
                    "ledFlash": True,
                    "speakerWarn": True,
                    "electricFence": False
                }
            }))

        elif event == "DEVICE_HEARTBEAT":
            print(f"[AI_Server] Heartbeat từ {camera_id}: CPU {data['payload']['cpuTemp']}°C")

        elif event == "DEVICE_STATUS":
            print(f"[AI_Server] Trạng thái {data['payload']['deviceKey']} = {data['payload']['status']}")


async def main():
    async with serve(handle_camera, "0.0.0.0", 5000):
        print("[AI_Server] WebSocket server đang chạy trên ws://0.0.0.0:5000")
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
```

### 6.2. Camera (Raspberry Pi - client)

Dùng `asyncio` + `websockets` (≥ 13.0) + tự động reconnect:

```python
import asyncio
import base64
import json
from datetime import datetime, timezone
from urllib.parse import urlencode

from websockets.asyncio.client import connect
from websockets.exceptions import ConnectionClosed

CAMERA_ID = "cam-001"
TOKEN = "token-abc-123"
AI_SERVER_HOST = "192.168.1.100"

CAPTURE_INTERVAL_SEC = 2      # chụp ảnh khi có chuyển động, tối đa mỗi 2s/lần
HEARTBEAT_INTERVAL_SEC = 30   # heartbeat mỗi 30s

_query = urlencode({"cameraId": CAMERA_ID, "token": TOKEN})
WS_URL = f"ws://{AI_SERVER_HOST}:5000/camera/ws?{_query}"


async def capture_image() -> str:
    """Giả lập chụp ảnh từ camera, trả về base64 của ảnh JPEG."""
    # TODO: Dùng picamera2 / opencv để chụp ảnh thực tế
    fake_image_bytes = b"fake-jpeg-bytes"
    return base64.b64encode(fake_image_bytes).decode("utf-8")


async def detect_motion() -> bool:
    """Giả lập phát hiện chuyển động."""
    # TODO: Dùng PIR sensor hoặc frame diff
    return True


async def control_gpio(action: dict):
    """Điều khiển thiết bị vật lý qua GPIO."""
    print(f"[Camera] GPIO: {json.dumps(action, indent=2)}")
    # TODO: Bật/tắt LED (GPIO), phát loa, điều khiển hàng rào điện


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


async def uplink_loop(ws):
    """Vòng lặp chụp ảnh (throttle theo CAPTURE_INTERVAL_SEC) + heartbeat."""
    elapsed_since_capture = CAPTURE_INTERVAL_SEC  # cho phép chụp ngay lần đầu
    elapsed_since_heartbeat = 0

    while True:
        if await detect_motion() and elapsed_since_capture >= CAPTURE_INTERVAL_SEC:
            image_b64 = await capture_image()
            await ws.send(json.dumps({
                "event": "IMAGE_DATA",
                "payload": {
                    "imageBase64": image_b64,
                    "timestamp": now_iso(),
                    "motionScore": 0.85
                }
            }))
            print("[Camera] Đã gửi ảnh lên AI_Server")
            elapsed_since_capture = 0

        if elapsed_since_heartbeat >= HEARTBEAT_INTERVAL_SEC:
            await ws.send(json.dumps({
                "event": "DEVICE_HEARTBEAT",
                "payload": {
                    "cameraId": CAMERA_ID,
                    "cpuTemp": 52.3,
                    "uptime": 3600
                }
            }))
            elapsed_since_heartbeat = 0

        await asyncio.sleep(1)
        elapsed_since_capture += 1
        elapsed_since_heartbeat += 1


async def downlink_loop(ws):
    """Lắng nghe lệnh từ AI_Server."""
    async for message in ws:
        try:
            data = json.loads(message)
        except json.JSONDecodeError:
            print("[Camera] Message không phải JSON hợp lệ, bỏ qua")
            continue

        event = data.get("event")

        if event == "DEFEND_ACTION":
            await control_gpio(data["payload"])

        elif event == "DEVICE_COMMAND":
            print(f"[Camera] Nhận lệnh: {data['payload']['action']}")
            await control_gpio(data["payload"])


async def connect_and_serve():
    backoff = 3
    while True:
        try:
            async with connect(WS_URL) as ws:
                print(f"[Camera] Đã kết nối AI_Server tại {WS_URL}")
                backoff = 3  # reset backoff sau khi kết nối thành công

                async with asyncio.TaskGroup() as tg:
                    tg.create_task(uplink_loop(ws))
                    tg.create_task(downlink_loop(ws))
                    # Nếu 1 trong 2 task lỗi, TaskGroup tự hủy task còn lại

        except* (ConnectionClosed, OSError) as eg:
            print(f"[Camera] Mất kết nối: {eg.exceptions}. Reconnect sau {backoff} giây...")
            await asyncio.sleep(backoff)
            backoff = min(backoff * 2, 60)  # exponential backoff, tối đa 60s


if __name__ == "__main__":
    asyncio.run(connect_and_serve())
```

> **Ghi chú tương thích:** `asyncio.TaskGroup` và cú pháp `except*` yêu cầu **Python 3.11+**. Nếu chạy trên Raspberry Pi với Python cũ hơn, thay bằng `asyncio.gather(uplink_loop(ws), downlink_loop(ws))` kết hợp tự `cancel()` task còn lại trong khối `except`, và dùng `except (ConnectionClosed, OSError) as e:` thông thường.

---

## 7. Flow tổng thể (Camera → AI_Server → Mobile_Server)

```mermaid
sequenceDiagram
    participant Camera as Camera (RPi)
    participant AI_Server as AI_Server
    participant Mobile_Server as Mobile_Server

    Note over Camera, AI_Server: WebSocket luôn duy trì
    Camera->>AI_Server: WS: IMAGE_DATA {image, timestamp}
    activate AI_Server
    AI_Server->>AI_Server: YOLOv8 inference
    AI_Server->>Mobile_Server: POST /cameras/{id}/detections
    activate Mobile_Server
    Mobile_Server-->>AI_Server: 201 {eventId, @DefendAction}
    deactivate Mobile_Server
    AI_Server-->>Camera: WS: DEFEND_ACTION {@DefendAction}
    deactivate AI_Server
    Camera->>Camera: GPIO: bật LED, loa, hàng rào
```

---

## 8. Ghi chú

- Port mặc định: **5000** (có thể cấu hình qua biến môi trường `WS_PORT`)
- Camera chụp ảnh tần suất **2 giây/lần** (khi có chuyển động) — được throttle đúng trong `uplink_loop`
- Heartbeat gửi mỗi **30 giây**
- Timeout chờ response: **5 giây**
- Auto-reconnect: **3 giây** (tăng dần theo exponential backoff: 3s → 6s → 12s → max 60s)
- Yêu cầu `websockets >= 13.0` (do dùng `websocket.request.path` và module `websockets.asyncio.server` / `websockets.asyncio.client`); nếu dùng bản cũ hơn cần thay bằng `websocket.path`
- Tham khảo thêm: [ai_server_plan.md](./ai_server_plan.md), [04-sequence-diagram.md](./04-sequence-diagram.md)