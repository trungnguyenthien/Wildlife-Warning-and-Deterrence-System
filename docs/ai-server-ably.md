# Giao tiếp Thời gian thực qua Ably giữa Camera (AI Server) và Mobile Server

Tài liệu này mô tả kiến trúc giao tiếp thời gian thực (Real-time Downlink) giữa **AI Server / Camera** (Python Client) và **Mobile Server** thông qua hạ tầng đám mây của **Ably Pub/Sub**, thay thế hoàn toàn cho giải pháp WebSocket song công tự vận hành trước đây.

---

## 1. Tổng quan & Cơ chế hoạt động của Ably

Trong kiến trúc mới, thay vì duy trì các socket kết nối trực tiếp gây quá tải cho bộ nhớ server và không thể triển khai trên môi trường Serverless (như Vercel), chúng ta ủy thác (outsource) việc quản lý socket thời gian thực cho hạ tầng đám mây Ably.

```
                  ┌──────────────────────┐
                  │  Ably Cloud Broker   │
                  └──────────┬───────────┘
                 ▲ (REST)    │          ▲ (WS wss://)
      Publish    │           │          │   Subscribe
      Downlink   │           │ Deliver  │   ACK
      DEVICE_CMD │           ▼          │   COMMAND_ACK
  ┌──────────────┴─┐   ┌─────────┐   ┌──┴────────────┐
  │ Mobile Server  │   │ Camera  │   │   AI Server   │
  │   (Vercel)     │   │ (Field) │   │ (Python Client)│
  └────────────────┘   └─────────┘   └───────────────┘
```

1.  **Thiết lập lắng nghe:** AI Server dùng SDK Ably kết nối (qua WebSocket an toàn `wss://`) đến máy chủ Ably và Đăng ký (Subscribe) lắng nghe trên kênh điều khiển `camera:control:{cameraId}` với sự kiện `DEVICE_COMMAND`.
2.  **Đẩy lệnh điều khiển:** Khi Mobile App gửi yêu cầu kiểm thử, Mobile Server (chạy stateless trên Vercel) chỉ cần thực hiện 1 request HTTP POST REST nhanh gọn gửi lệnh `DEVICE_COMMAND` lên Ably Cloud rồi kết thúc tiến trình.
3.  **Phát tin:** Ably Cloud tự động điều phối, đẩy lệnh qua kết nối WebSocket thời gian thực sẵn có xuống cho AI Server.
4.  **Xác nhận lệnh:** AI Server thi hành lệnh vật lý ở trạm, sau đó gửi phản hồi `COMMAND_ACK` qua WebSocket lên kênh `camera:ack:{cameraId}` để Ably chuyển về cho Mobile Server hoàn tất request HTTP.

---

## 2. Vì sao nên dùng Ably Pub/Sub thay thế cho Tự quản lý WebSocket?

### 2.1. So sánh chi tiết

| Đặc tính | Tự quản lý WebSocket Server | Sử dụng Ably Pub/Sub Cloud |
| :--- | :--- | :--- |
| **Khả năng tương thích Serverless** | **Không hỗ trợ.** Vercel Function sẽ tự động ngắt kết nối sau tối đa 10-15 giây. | **Tương thích tuyệt đối.** Mobile Server chỉ cần gọi HTTP POST REST ngắn hạn (0.1 giây) để gửi lệnh. |
| **Quản lý kết nối (Stateful Connection)** | Server phải tự duy trì danh sách Socket đang kết nối trong RAM, tốn nhiều tài nguyên khi số trạm tăng lên. | Kết nối được lưu trữ và quản lý hoàn toàn trên đám mây của Ably. Server hoàn toàn stateless (không trạng thái). |
| **Độ tin cậy & Tự phục hồi** | Phải tự code logic Ping-Pong, Heartbeat để phát hiện đứt mạng và tự viết mã Reconnect phức tạp ở Client. | SDK của Ably tích hợp sẵn cơ chế Auto-reconnect, quản lý Heartbeat ngầm và đệm tin nhắn (Buffering) khi đứt mạng. |
| **Bảo mật API Key** | Cấu hình thủ công, dễ lộ nếu không quản lý tốt xác thực. | Hỗ trợ cơ chế **Token Authentication** cấp khóa tạm thời (TTL 1 giờ) qua endpoint `/auth/ably-token` cực kỳ an toàn. |
| **Khả năng chịu tải (Scaling)** | Giới hạn bởi RAM/Băng thông của VPS. Đòi hỏi cấu hình clustering phức tạp khi scale. | Hệ thống phân tán toàn cầu tự động scale hàng triệu kết nối đồng thời mà không cần cấu hình thêm. |

---

## 3. Thiết kế Kênh truyền tin (Channel Design)

Để tối ưu chi phí tin nhắn, mỗi trạm camera sẽ giao tiếp qua hai kênh độc lập:

1.  **Kênh điều khiển (Control Channel):** `camera:control:{cameraId}`
    *   *Event:* `DEVICE_COMMAND`
    *   *Hướng đi:* Mobile Server (Publish REST) -> Ably -> AI Server (Subscribe WebSocket).
2.  **Kênh phản hồi ACK (ACK Channel):** `camera:ack:{cameraId}`
    *   *Event:* `COMMAND_ACK`
    *   *Hướng đi:* AI Server (Publish WebSocket) -> Ably -> Mobile Server (Subscribe REST/Short-live).

---

## 4. Đặc tả định dạng bản tin (JSON Payload)

Định dạng gói tin JSON được giữ nguyên tương thích với logic xử lý phần cứng hiện có:

### 4.1. Lệnh điều khiển vật lý (`DEVICE_COMMAND`)
```json
{
  "event": "DEVICE_COMMAND",
  "payload": {
    "commandId": "cmd-123-abc",
    "cameraId": "cam-001",
    "deviceKey": "led | speaker | fence",
    "action": "TEST",
    "params": {
      "intensity": 80,
      "durationSeconds": 5,
      "audioSampleId": "A_gunshot"
    }
  }
}
```

### 4.2. Bản tin phản hồi xác nhận (`COMMAND_ACK`)
```json
{
  "event": "COMMAND_ACK",
  "payload": {
    "commandId": "cmd-123-abc",
    "cameraId": "cam-001",
    "status": "SUCCESS | FAILED",
    "error": null
  }
}
```

---

## 5. Ví dụ mã nguồn Python hoàn chỉnh cho AI Server (Camera Client)

Yêu cầu cài đặt thư viện Ably chính thức cho Python:
```bash
pip install ably
```

Dưới đây là file script Python hoàn chỉnh chạy trên Raspberry Pi hoặc AI Server giả lập:

```python
import os
import asyncio
import time
from ably import AblyRealtime
from ably.util.exceptions import AblyException

# Cấu hình kiểm thử trạm camera
ABLY_API_KEY = os.environ.get("ABLY_API_KEY", "abc123.DEF456:ghIjKlmNoPqrSTuv")
CAMERA_ID = "camera_01"

CONTROL_CHANNEL = f"camera:control:{CAMERA_ID}"
ACK_CHANNEL = f"camera:ack:{CAMERA_ID}"

async def execute_hardware_test(device_key: str, params: dict):
    """
    Giả lập điều khiển thiết bị phần cứng tại trạm (GPIO).
    """
    print(f"\n[Hardware] >>> BẮT ĐẦU thực thi thử nghiệm thiết bị: {device_key.upper()}")
    duration = params.get("durationSeconds", 3)
    intensity = params.get("intensity", 100)
    
    if device_key == "speaker":
        print(f"[Hardware] LOA PHÁT: File: {params.get('audioSampleId')}, Cường độ âm lượng: {intensity}%")
    elif device_key == "led":
        print(f"[Hardware] ĐÈN LED CHỚP: Kích hoạt nháy LED với độ sáng: {intensity}%")
    elif device_key == "fence":
        print(f"[Hardware] RÀO ĐIỆN: Bật hàng rào cảnh báo trong: {duration} giây")
        
    # Giả lập thời gian chạy thử nghiệm
    await asyncio.sleep(duration)
    print(f"[Hardware] <<< HOÀN THÀNH thực thi thiết bị: {device_key.upper()}\n")

async def main():
    if not ABLY_API_KEY:
        print("[AI Server] Lỗi: Chưa thiết lập biến môi trường ABLY_API_KEY")
        return

    print(f"[AI Server] Khởi tạo kết nối tới Ably với Client ID: client-{CAMERA_ID}")
    
    try:
        # Khởi tạo realtime client kết nối an toàn wss://
        async with AblyRealtime(ABLY_API_KEY, client_id=f"client-{CAMERA_ID}") as realtime:
            await realtime.connection.once_async("connected")
            print("[AI Server] Đã kết nối thành công tới Ably Cloud Broker!")

            # Lấy các đối tượng channel tương ứng
            control_chan = realtime.channels.get(CONTROL_CHANNEL)
            ack_chan = realtime.channels.get(ACK_CHANNEL)

            # Hàm callback xử lý tin nhắn nhận được
            async def on_device_command(message):
                try:
                    data = message.data
                    event = data.get("event")
                    payload = data.get("payload", {})
                    
                    if event != "DEVICE_COMMAND":
                        return
                        
                    command_id = payload.get("commandId")
                    device_key = payload.get("deviceKey")
                    params = payload.get("params", {})

                    print(f"[AI Server] Nhận lệnh test [{command_id}] cho thiết bị: {device_key}")
                    
                    # 1. Thực thi phần cứng
                    await execute_hardware_test(device_key, params)
                    
                    # 2. Publish phản hồi ACK lên kênh Ack Channel
                    ack_payload = {
                        "event": "COMMAND_ACK",
                        "payload": {
                            "commandId": command_id,
                            "cameraId": CAMERA_ID,
                            "status": "SUCCESS",
                            "error": None
                        }
                    }
                    await ack_chan.publish("message", ack_payload)
                    print(f"[AI Server] Đã gửi xác nhận COMMAND_ACK cho lệnh: {command_id}")
                    
                except Exception as err:
                    print(f"[AI Server] Lỗi xử lý lệnh: {err}")
                    # Gửi phản hồi ACK thất bại
                    try:
                        failed_payload = {
                            "event": "COMMAND_ACK",
                            "payload": {
                                "commandId": payload.get("commandId", "unknown"),
                                "cameraId": CAMERA_ID,
                                "status": "FAILED",
                                "error": str(err)
                            }
                        }
                        await ack_chan.publish("message", failed_payload)
                    except Exception:
                        pass

            # Đăng ký lắng nghe sự kiện trên kênh điều khiển
            await control_chan.subscribe("message", on_device_command)
            print(f"[AI Server] Đang lắng nghe lệnh điều khiển tại kênh: {CONTROL_CHANNEL}")

            # Giữ tiến trình chạy vô hạn để lắng nghe
            await asyncio.Event().wait()

    except AblyException as e:
        print(f"[AI Server] Lỗi kết nối Ably: {e}")
    except KeyboardInterrupt:
        print("[AI Server] Đang đóng kết nối và thoát...")

if __name__ == "__main__":
    asyncio.run(main())
```

---

## 6. Tài liệu tham khảo chéo
*   [03-mobile_api.md](./03-mobile_api.md) — Đặc tả API REST và Token Endpoint.
*   [04-sequence-diagram.md](./04-sequence-diagram.md) — Sơ đồ luồng gửi lệnh Downlink.
*   [huong-dan-ably.md](./huong-dan-ably.md) — Hướng dẫn toàn diện và tối ưu chi phí tin nhắn Ably.
