# Kế hoạch Tích hợp AI Server với Mobile Server (ai_server_plan.md)

Tài liệu này tóm tắt các hành động và đặc tả giao tiếp API cần thiết để tích hợp phân hệ **AI Server** (đã xây dựng sẵn) với **Mobile Server** dựa trên tài liệu thiết kế hệ thống.

---

## 1. Bản đồ luồng tương tác hệ thống (System Interaction Flow)

Mọi luồng giao tiếp giữa AI Server và các phân hệ khác tuân thủ nghiêm ngặt theo đặc tả trong [04-sequence-diagram.md](./04-sequence-diagram.md). AI Server tham gia vào 2 luồng xử lý chính dưới đây:

### 1.1. Luồng tải lên kết quả nhận diện (Uplink / Webhook)
*   **Tham chiếu:** [04-sequence-diagram.md: Action 1.1 (AI Server sends detection snapshot)](./04-sequence-diagram.md#11-action-ai-server-sends-detection-snapshot-ai_server)
*   **Quy trình xử lý:**
    1.  Camera phát hiện chuyển động $\rightarrow$ gửi hình ảnh thô về AI Server.
    2.  AI Server phân tích hình ảnh bằng mô hình YOLOv8 để nhận dạng danh sách loài và độ tin cậy.
    3.  AI Server gửi yêu cầu Webhook lên Mobile Server thông qua API đăng ký sự kiện.
    4.  AI Server nhận phản hồi chứa cấu hình hành vi phòng vệ (`responseAction` dạng `@DefendAction`) và ra lệnh điều khiển vật lý cho thiết bị xua đuổi tại Camera.

### 1.2. Luồng thử nghiệm thiết bị thời gian thực (Downlink / Ably Pub/Sub)
*   **Tham chiếu:** [04-sequence-diagram.md: Action 6.3 (Test speaker sound at camera station)](./04-sequence-diagram.md#63-action-test-speaker-sound-at-camera-station-ai_server)
*   **Quy trình xử lý:**
    1.  AI Server lấy Token xác thực từ API và thiết lập kết nối an toàn (WebSocket) đến máy chủ Ably Cloud.
    2.  AI Server đăng ký (Subscribe) kênh lắng nghe điều khiển `user:control:{userId}` với tên sự kiện `DEVICE_COMMAND`.
    3.  Khi Kiểm lâm bấm "Test" thiết bị trên ứng dụng di động, Mobile Server sẽ gọi REST API của Ably để publish thông điệp điều khiển.
    4.  AI Server nhận được thông điệp thời gian thực từ Ably Cloud và thực thi kích hoạt vật lý tại Camera (ví dụ: phát âm thanh thử nghiệm).
    5.  Sau khi thực thi xong, AI Server gửi (Publish) phản hồi xác nhận trạng thái thực thi thành công/thất bại (`COMMAND_ACK`) lên kênh Ably `user:ack:{userId}` để Mobile Server nhận kết quả.

---

## 2. Chi tiết Đặc tả API Tích hợp

> [!NOTE]
> **Production Server URL (Địa chỉ máy chủ chạy thực tế):**
> *   REST API Base URL: `https://wildlife-warning-and-deterrence-sys.vercel.app/api/v1`
> 
> **Real-time Broker (Hệ thống điều khiển thời gian thực):**
> *   Sử dụng **Ably Cloud Broker**. AI Server kết nối trực tiếp đến Ably Cloud bằng SDK chính thức qua cơ chế xác thực Token hoặc API Key.
> 
> AI Server khi tích hợp cần cấu hình REST API để gọi webhook và nạp khóa/token để kết nối Ably.
> 
> Dữ liệu giao tiếp được định nghĩa chi tiết tại Nhóm 12 của tài liệu [03-mobile_api.md](./03-mobile_api.md#13a-nhom-12--tich-hop-thiet-bi--ai-server).

### 2.1. Đăng ký sự kiện phát hiện loài (`POST /cameras/{cameraId}/detections`)
*   **Tham chiếu URL thực tế (Production API URL):** `POST https://wildlife-warning-and-deterrence-sys.vercel.app/api/v1/cameras/{cameraId}/detections`
*   **Tham chiếu đặc tả:** [03-mobile_api.md: POST /cameras/{cameraId}/detections](./03-mobile_api.md#13a1-post-camerascameraiddetections)
*   **Phương thức:** `POST`
*   **Content-Type:** `multipart/form-data`
*   **Tham số gửi đi (Form-data):**
    *   `image`: File ảnh chụp dạng nhị phân (Binary).
    *   `userId`: ID người quản lý trạm camera (mã hex 4 ký tự, ví dụ: `9f3a`).
    *   `detections`: Chuỗi JSON danh sách con vật phát hiện, định dạng: `[{"speciesId":"elephant","confidence":0.92}]`.
*   **Kết quả phản hồi (201 Created JSON):**
    ```json
    {
      "eventId": "evt-456",
      "cameraId": "cam-001",
      "detections": [
        { "speciesId": "elephant", "confidence": 0.92 }
      ],
      "imageUrl": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-12.jpg",
      "detectedAt": "2026-07-19T04:55:00+07:00",
      "responseAction": {
        "ledFlash": true,
        "ledColor": "red",
        "ledIntensity": 20,
        "ledFlashRate": "4_per_sec",
        "speakerWarn": true,
        "audioSampleId": "A_gunshot",
        "audioIntensity": 80,
        "speakerSampleId": "monkey",
        "silentAlert": false
      }
    }
    ```

#### 2.1.1. Sample code — AI Server gửi kết quả nhận diện (Python)

> **Tham chiếu:** [03-mobile_api.md: POST /cameras/{cameraId}/detections](./03-mobile_api.md#13a1-post-camerascameraiddetections) · [04-sequence-diagram.md: Action 1.1](./04-sequence-diagram.md#11-action-ai-server-sends-detection-snapshot-ai_server) · [upload-image-service.md](./upload-image-service.md) · bản đồ `@DefendAction` ở [mục 3](#3-bản-đồ-ánh-xạ-tham-số-phòng-vệ-defendaction).

```python
import os
import json
import requests

# --- Cấu hình (khớp NOTE ở đầu mục 2) ---
MOBILE_SERVER_BASE = "https://wildlife-warning-and-deterrence-sys.vercel.app/api/v1"
CAMERA_ID = os.environ.get("CAMERA_ID", "cam-001")
USER_ID = os.environ.get("USER_ID", "9f3a")          # mã hex 4 ký tự (03)
CAPTURE_PATH = "/tmp/latest_snapshot.jpg"            # ảnh snapshot mới nhất

def run_yolov8(image_path: str) -> list:
    """Chạy mô hình YOLOv8: trả về danh sách loài + độ tin cậy.
    (Placeholder — thay bằng lời gọi mô hình thật.)"""
    # TODO: gọi YOLOv8 -> [{"speciesId": "elephant", "confidence": 0.92}]
    return [{"speciesId": "elephant", "confidence": 0.92}]

def send_detection(image_path: str, detections: list) -> dict:
    """Uplink: POST /cameras/{cameraId}/detections (multipart/form-data)."""
    url = f"{MOBILE_SERVER_BASE}/cameras/{CAMERA_ID}/detections"
    with open(image_path, "rb") as img:
        files = {"image": (os.path.basename(image_path), img, "image/jpeg")}
        data = {
            "userId": USER_ID,
            "detections": json.dumps(detections),     # chuỗi JSON (03:13a.1)
        }
        resp = requests.post(url, files=files, data=data, timeout=30)
        resp.raise_for_status()                       # kỳ vọng 201 Created
        return resp.json()
    # -> { eventId, cameraId, detections, imageUrl, detectedAt, responseAction }

def execute_defend_action(action: dict) -> None:
    """Ánh xạ @DefendAction (mục 3) sang lệnh phần cứng tại trạm."""
    if action.get("silentAlert"):
        # Silent Alert: KHÔNG kích hoạt thiết bị xua đuổi nào tại chỗ (03/04)
        print("[Hardware] silentAlert=True -> tắt mọi thiết bị (SMS/Push do Mobile Server lo)")
        return

    if action.get("ledFlash"):
        print(f"[Hardware] LED BẬT: color={action.get('ledColor')}, "
              f"rate={action.get('ledFlashRate')}, "
              f"duration={action.get('ledIntensity')}s")   # ledIntensity = giây (03)
    if action.get("speakerWarn"):
        print(f"[Hardware] LOA BẬT: audioSampleId={action.get('audioSampleId')}, "
              f"speakerSampleId={action.get('speakerSampleId')}, "
              f"audioIntensity={action.get('audioIntensity')}%")  # 0-100 (mục 3.2)

if __name__ == "__main__":
    detections = run_yolov8(CAPTURE_PATH)
    result = send_detection(CAPTURE_PATH, detections)
    print("eventId:", result["eventId"])
    execute_defend_action(result["responseAction"])
```

### 2.2. Kênh truyền tin thời gian thực qua Ably (Pub/Sub Channels)
*   **Tham chiếu tài liệu hướng dẫn Ably:** [huong-dan-ably.md](./huong-dan-ably.md)
*   **Cơ chế hoạt động:** 
    *   AI Server (trạm camera) kết nối với Ably Cloud, lắng nghe sự kiện trên kênh điều khiển của riêng trạm đó.
    *   Tất cả các bản tin truyền tải dưới dạng JSON qua giao thức WebSocket của Ably.
*   **Kênh nhận lệnh điều khiển (Control Channel):**
    *   *Tên kênh:* `user:control:{userId}`
    *   *Sự kiện (Event Name):* `DEVICE_COMMAND`
    *   *Payload:*
        ```json
        {
          "event": "DEVICE_COMMAND",
          "payload": {
            "commandId": "cmd-123",
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
*   **Kênh phản hồi xác nhận lệnh (ACK Channel):**
    *   *Tên kênh:* `user:ack:{userId}`
    *   *Sự kiện (Event Name):* `COMMAND_ACK`
    *   *Payload:*
        ```json
        {
          "event": "COMMAND_ACK",
          "payload": {
            "commandId": "cmd-123",
            "cameraId": "cam-001",
            "status": "SUCCESS | FAILED",
            "error": null // chuỗi ký tự mô tả lỗi nếu status là FAILED
          }
        }
        ```

#### 2.2.1. Sample code — AI Server lắng nghe lệnh thiết bị qua Ably (Python)

> **Tham chiếu:** [huong-dan-ably.md: ClientPY](./huong-dan-ably.md) · [ai-server-ably.md: mục 5](./ai-server-ably.md#5-ví-dụ-mã-nguồn-python-hoàn-chỉnh-cho-ai-server-camera-client) · [04-sequence-diagram.md: Action 6.3](./04-sequence-diagram.md#63-action-test-speaker-sound-at-camera-station-ai_server)
>
> **Ghi chú chi phí (huong-dan-ably.md mục 6):** dùng **2 kênh riêng** — AI Server chỉ *subscribe* `user:control:{userId}` và chỉ *publish* lên `user:ack:{userId}` → tối ưu **4 message/vòng request-response** (1 publish + 1 deliver mỗi chiều).

```python
import os
import asyncio
from ably import AblyRealtime    # pip install ably  (ClientPY)

ABLY_AI_SERVER_API_KEY = os.environ["ABLY_AI_SERVER_API_KEY"]
USER_ID = os.environ.get("ABLY_USER_ID", "user_01")

CONTROL_CHANNEL = f"user:control:{USER_ID}"   # Subscribe: nhận DEVICE_COMMAND
ACK_CHANNEL     = f"user:ack:{USER_ID}"       # Publish:    gửi COMMAND_ACK

async def execute_hardware_test(device_key: str, params: dict) -> None:
    """Giả lập điều khiển phần cứng (GPIO) tại trạm."""
    duration = params.get("durationSeconds", 3)
    if device_key == "speaker":
        print(f"[HW] LOA TEST: {params.get('audioSampleId')}, "
              f"volume={params.get('intensity')}% trong {duration}s")
    elif device_key == "led":
        print(f"[HW] LED TEST: cường độ {params.get('intensity')}% trong {duration}s")
    elif device_key == "fence":
        print(f"[HW] HÀNG RÀO TEST: trong {duration}s")
    await asyncio.sleep(duration)

async def main() -> None:
    # Pattern ClientPY (huong-dan-ably.md): async with tự quản lý vòng đời kết nối
    async with AblyRealtime(ABLY_AI_SERVER_API_KEY, client_id=f"client-{USER_ID}") as realtime:
        await realtime.connection.once_async("connected")
        control_chan = realtime.channels.get(CONTROL_CHANNEL)
        ack_chan     = realtime.channels.get(ACK_CHANNEL)

        async def on_device_command(message):
            payload = message.data.get("payload", {})
            try:
                if message.data.get("event") != "DEVICE_COMMAND":
                    return
                await execute_hardware_test(payload["deviceKey"], payload.get("params", {}))

                # Gửi ACK SUCCESS lên kênh user:ack (04 Action 6.3)
                await ack_chan.publish("message", {
                    "event": "COMMAND_ACK",
                    "payload": {
                        "commandId": payload["commandId"],
                        "cameraId": payload["cameraId"],
                        "status": "SUCCESS",
                        "error": None,
                    },
                })
            except Exception as err:
                # Gửi ACK FAILED thay vì để im (tránh Mobile Server timeout 5s)
                await ack_chan.publish("message", {
                    "event": "COMMAND_ACK",
                    "payload": {
                        "commandId": payload.get("commandId", "unknown"),
                        "cameraId": payload.get("cameraId", "unknown"),
                        "status": "FAILED",
                        "error": str(err),
                    },
                })

        await control_chan.subscribe("message", on_device_command)
        print(f"Đang lắng nghe {CONTROL_CHANNEL} ...")
        await asyncio.Event().wait()   # giữ tiến trình sống

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("Đóng kết nối Ably...")
```

---

## 3. Bản đồ Ánh xạ Tham số Phòng vệ (`@DefendAction`)

Khi nhận được đối tượng cấu hình phòng vệ từ Mobile Server (qua phản hồi API Webhook nhận dạng hoặc qua kênh điều khiển Ably), AI Server chịu trách nhiệm phân dịch các thuộc tính này để điều khiển thiết bị phần cứng tại chỗ.

Dữ liệu cấu hình được chia làm hai mức độ chi tiết tùy thuộc vào kênh truyền và cấu hình hiện tại:

### 3.1. Bản tin phản hồi Trigger nhanh (Mặc định trả về từ Webhook)
Đối với bản tin phản hồi nhanh từ API Webhook nhận dạng động vật, máy chủ trả về trạng thái kích hoạt bật/tắt (Boolean) của các phân hệ tại thực địa:

*   `ledFlash`: Kích hoạt/Vô hiệu hóa hệ thống đèn LED chớp sáng.
    *   *Giá trị:* `true` (kích hoạt chớp sáng), `false` (tắt đèn).
*   `speakerWarn`: Kích hoạt/Vô hiệu hóa hệ thống loa phát thanh cảnh báo.
    *   *Giá trị:* `true` (kích hoạt phát loa xua đuổi), `false` (tắt loa).
*   `silentAlert`: Chế độ cảnh báo im lặng.
    *   *Giá trị:* 
        *   `true`: Hệ thống chỉ ghi nhận nhật ký sự kiện, gửi SMS/Push cảnh báo về máy chủ/người dân, tuyệt đối **không kích hoạt** bất kỳ thiết bị xua đuổi vật lý nào tại chỗ (LED, Loa đều tắt).
        *   `false`: Kích hoạt các thiết bị xua đuổi vật lý tại chỗ tương ứng theo cấu hình.

---

### 3.2. Thông số điều khiển chi tiết (Lấy từ Presets / Custom Config hoặc Ably Command)
Khi thực hiện cấu hình sâu cho Camera hoặc nhận thông điệp điều khiển chi tiết qua Ably, các trường thông số chi tiết dưới đây sẽ được áp dụng:

#### 1. Hệ thống Đèn LED chớp sáng
*   `ledColor`: Màu sắc phát sáng của đèn LED chớp.
    *   *Tùy chọn giá trị hợp lệ (khớp đặc tả 03 & schema 05):*
        *   `red`: Ánh sáng đỏ cảnh báo nguy hiểm mạnh.
        *   `white`: Ánh sáng trắng công suất cao xua đuổi.
        *   `red_white_alt`: Đỏ và trắng chớp nháy xen kẽ.
        *   `null`: Không chỉ định màu.
*   `ledFlashRate`: Tần suất nhấp nháy của LED.
    *   *Tùy chọn giá trị hợp lệ:*
        *   `2_per_sec`: Nháy 2 lần mỗi giây (tiêu chuẩn).
        *   `4_per_sec`: Nháy nhanh 4 lần mỗi giây (gây kích động thị giác thú mạnh).
        *   `random`: Nháy ngẫu nhiên để động vật không làm quen được tần số.
        *   `null`: Không nhấp nháy.
*   `ledIntensity`: **Thời lượng chớp LED (đơn vị giây)** — theo đặc tả 03, trường này tương đương cột `led_duration_seconds` trong bảng `response_configs` của tài liệu 05.
    *   *Tùy chọn giá trị hợp lệ:* Số nguyên `>= 0` biểu thị tổng số giây duy trì chu kỳ chớp LED (ví dụ `20` = 20 giây).

#### 2. Hệ thống Loa phát thanh xua đuổi
*   `audioSampleId` (hoặc `speakerSampleId` đối với loa cảnh báo dân cư vùng lân cận): ID của tệp tin âm thanh mẫu được nạp sẵn trong thẻ nhớ local của trạm Camera.
    *   *Tùy chọn giá trị hợp lệ cho âm thanh xua đuổi động vật (bắt đầu bằng tiền tố `A_`):*
        *   `A_gunshot`: Tiếng súng nổ đanh (hiệu quả cao với thú lớn nguy hiểm).
        *   `A_growl`: Tiếng gầm rú của mãnh thú (hổ, báo).
        *   `A_dog_bark`: Tiếng đàn chó săn sủa dữ dội.
        *   `A_explosion`: Tiếng nổ lớn phá hủy.
    *   *Tùy chọn giá trị hợp lệ cho âm thanh cảnh báo cư dân (khớp danh mục `GET /alertSounds` / `citizenAlertSounds` của `/audio-samples`, nguồn `hard-config/alert-sound.yaml`):*
        *   `tiger`: Tiếng Hổ (âm thanh cảnh báo qua loa).
        *   `monkey`: Tiếng Khỉ (âm thanh cảnh báo qua loa).
        *   `deer` / `elephant` / `wild_boar`: các âm thanh cảnh báo khác trong danh mục.
*   `audioIntensity` (hoặc `volume`): Cường độ âm lượng phát ra loa.
    *   *Tùy chọn giá trị hợp lệ:* Số nguyên từ `0` đến `100` (đại diện cho tỷ lệ phần trăm % công suất tối đa của âm ly).


