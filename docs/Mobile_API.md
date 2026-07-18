# Mobile_API — Đặc tả API cho Ứng dụng Android

> **Phạm vi tài liệu:** Đặc tả HTTP API mà **ứng dụng Android (Mobile App)** sử dụng để tương tác với **Mobile Server**. Tài liệu này dùng cho đội phát triển Backend & Mobile để thống nhất contract; không lặp lại các quy tắc nghiệp vụ đã nêu trong [de-tai-nghien-cuu-canh-bao-dong-vat.md](../outputs/de-tai-nghien-cuu-canh-bao-dong-vat.md).

---

## 1. Quy ước chung

### 1.1. Base URL & Versioning

```
Base URL:  https://<mobile-server-host>/api/v1
```

Mọi endpoint dưới đây đều bắt đầu bằng `/api/v1` (đã lược bỏ trong bảng để gọn).

### 1.2. Cấu trúc endpoint

```
{METHOD} /{resource}/{id?}       (RESTful, danh từ số nhiều)
```

### 1.3. Headers bắt buộc

| Header | Bắt buộc | Mô tả |
|---|---|---|
| `Authorization` | Có (trừ Auth) | `Bearer <jwt_access_token>` |
| `Content-Type` | Có (POST/PUT/PATCH) | `application/json; charset=utf-8` |
| `Accept` | Có | `application/json` |
| `X-Client-Platform` | Khuyến nghị | `android` |
| `X-App-Version` | Khuyến nghị | `1.0.0` — phục vụ compatibility check |
| `X-Device-Id` | Khuyến nghị | UUID thiết bị — phục vụ rate limit / fraud |

### 1.4. Mã lỗi chuẩn (HTTP Status Code)

| Code | Ý nghĩa | Khi nào dùng |
|---|---|---|
| `200 OK` | Thành công | Trả về resource / kết quả |
| `201 Created` | Tạo mới thành công | Có resource mới trong DB |
| `204 No Content` | Thành công, không có body | DELETE / override không cần response |
| `400 Bad Request` | Sai format / thiếu field | Validation client fail |
| `401 Unauthorized` | Thiếu token / token hết hạn / sai token | Chuyển về màn hình LOGIN |
| `403 Forbidden` | Token hợp lệ nhưng không có quyền | Vai trò không đủ (vd USER xem camera quân sự) |
| `404 Not Found` | Không tìm thấy resource | Sai ID / quyền bị thu hồi |
| `409 Conflict` | Xung đột dữ liệu | SĐT trùng / config preset đã đổi trước user |
| `422 Unprocessable Entity` | Validate nghiệp vụ fail | Sai logic (vd fence level không thuộc enum) |
| `429 Too Many Requests` | Rate limit | Spam đăng nhập / spam override |
| `500 Internal Server Error` | Lỗi server | Hiển thị banner "Đã có lỗi xảy ra. Thử lại sau." |
| `503 Service Unavailable` | Mobile Server mất kết nối tới AI / Field Device | Hiển thị banner offline / fallback |

### 1.5. Cấu trúc response lỗi

Tất cả response lỗi đều dùng JSON:

```json
{
  "error": {
    "code": "ERR_VALIDATION_FAILED",
    "message": "Số điện thoại không đúng định dạng Việt Nam.",
    "details": {
      "field": "phoneNumber",
      "rule": "regex_vn_phone"
    },
    "traceId": "abc123def456"
  }
}
```

Một số mã lỗi nghiệp vụ hay dùng:

| Error Code | Ý nghĩa |
|---|---|
| `ERR_INVALID_CREDENTIALS` | SĐT hoặc mật khẩu sai |
| `ERR_PHONE_NOT_REGISTERED` | SĐT chưa đăng ký tài khoản |
| `ERR_PHONE_ALREADY_USED` | SĐT đã tồn tại (đăng ký trùng) |
| `ERR_PRESET_NOT_FOUND` | Preset không tồn tại hoặc đã bị xoá |
| `ERR_CAMERA_OFFLINE` | Trạm camera mất kết nối |
| `ERR_FENCE_AUTO_OFF_TOO_SHORT` | Giá trị `autoOffMinutes` phải ≥ 2 |
| `ERR_CONCURRENT_CONFIG_CHANGE` | Config đã bị thay đổi ở thiết bị khác → client refresh |
| `ERR_PUSH_TOKEN_REVOKED` | Token FCM đã thu hồi / hết hạn |

---

## 2. Model dữ liệu chung (Schemas tái sử dụng)

### 2.1. UserRole (Enum)

```
CITIZEN            — Người dân địa phương
RANGER             — Kiểm lâm khu vực
BORDER_GUARD       — Bộ đội Biên phòng
HIGHWAY_ADMIN      — Ban QL đường cao tốc
```

### 2.2. DangerLevel (Enum)

```
LOW         — Ít nguy hiểm (Khỉ, Nai, Hươu)
MEDIUM      — Nguy hiểm trung bình
HIGH        — Nguy hiểm cao (Rắn lớn, Cá sấu)
CRITICAL    — Cực kỳ nguy hiểm (Voi, Hổ, Báo, Tê giác)
```

### 2.3. DeviceKey (Enum cho override ngoại vi)

```
sms                  — Gửi SMS cảnh báo cho người dân lân cận
speaker              — Loa phát thanh AI tại trạm
deterrent_audio      — Âm thanh xua đuổi (tần số thấp / siêu âm)
led_flash            — Đèn LED flash nhiều màu
electric_fence       — Hàng rào điện sinh học
ranger_alert         — Gửi báo động về trạm Kiểm lâm
```

### 2.4. Animal (Schema)

```json
{
  "id": "elephant",
  "displayName": "Voi",
  "dangerLevel": "CRITICAL",
  "isHuman": false
}
```

### 2.5. Camera (Schema)

```json
{
  "id": "cam-001",
  "name": "Trạm Rìa Rừng Cát Tiên - Cổng Bắc",
  "location": {
    "lat": 11.4523,
    "lng": 107.4231,
    "address": "Vườn Quốc gia Cát Tiên, Đồng Nai"
  },
  "status": "ONLINE",
  "hasCurrentDetection": true,
  "currentDetection": { /* AnimalDetection hoặc null */ }
}
```

Enum `Camera.status`: `ONLINE`, `OFFLINE`, `MAINTENANCE`.

### 2.6. AnimalDetection (schema realtime)

```json
{
  "speciesId": "elephant",
  "speciesName": "Voi",
  "count": 1,
  "dangerLevel": "CRITICAL",
  "confidence": 0.92,
  "firstDetectedAt": "2026-07-16T09:04:12+07:00",
  "lastDetectedAt": "2026-07-16T09:04:30+07:00",
  "cumulativeSeconds": 12,
  "snapshotUrl": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-12.jpg"
}
```

---

## 3. Nhóm 1 — Xác thực & Tài khoản (`[LOGIN_SCREEN]`, `[REGISTER_SCREEN]`)

### 3.1. `POST /auth/register`

Đăng ký tài khoản mới. Mapping: màn hình `[REGISTER_SCREEN]` (mục 7.4.2 của [de-tai-nghien-cuu-canh-bao-dong-vat.md](../outputs/de-tai-nghien-cuu-canh-bao-dong-vat.md)).

**Request body**
```json
{
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0901234567",
  "role": "CITIZEN",
  "password": "P@ssw0rd!",
  "confirmPassword": "P@ssw0rd!"
}
```

**Response 201**
```json
{
  "user": {
    "id": "9f3a",
    "fullName": "Nguyễn Văn A",
    "phoneNumber": "+84901234567",
    "role": "CITIZEN",
    "createdAt": "2026-07-16T08:30:00+07:00"
  },
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "rt_8s7d6f...",
  "expiresIn": 3600
}
```

**Validation format (client-side, fail → 400)**
- `phoneNumber`: regex `^0[0-9]{9}$` (SĐT Việt Nam 10 số).
- `password`: ≥ 8 ký tự, có chữ hoa, chữ thường, số, ký tự đặc biệt.
- `role`: 1 trong 4 enum trên — UI là Dropdown.

**Side effects**
- Hash mật khẩu (bcrypt/argon2).
- Tạo JWT access (1h) + refresh (30 ngày).
- Tự động thực hiện đăng nhập → chuyển sang `MAIN`.

**Lỗi hay gặp**
- `409 ERR_PHONE_ALREADY_USED` → hiện lỗi inline tại field SĐT.

---

### 3.2. `POST /auth/login`

Mapping: màn hình `[LOGIN_SCREEN]` (mục 7.4.2 của [de-tai-nghien-cuu-canh-bao-dong-vat.md](../outputs/de-tai-nghien-cuu-canh-bao-dong-vat.md)).

**Request body**
```json
{
  "phoneNumber": "0901234567",
  "password": "P@ssw0rd!"
}
```

**Response 200**
```json
{
  "user": { "id":"...", "fullName":"...", "phoneNumber":"...", "role":"CITIZEN" },
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 3600
}
```

**Lỗi hay gặp**
- `401 ERR_INVALID_CREDENTIALS` → banner đỏ: "Số điện thoại hoặc mật khẩu không đúng." Sau 5 lần sai liên tiếp → `429` (rate limit 1 phút).
- `404 ERR_PHONE_NOT_REGISTERED` → "Số điện thoại chưa đăng ký" + link "Đăng ký ngay".

**Side effects**
- Reset rate-limit counter khi đăng nhập thành công.
- Trả `refreshToken` để dùng cho API 3.4.

---

### 3.3. `POST /auth/logout`

Thu hồi session hiện tại. Mapping: từ tab `[SETTING_TAB]` → Đăng xuất.

**Request header**: chỉ Authorization.
**Response 204** (no content).

**Side effects**
- Thu hồi refresh token.
- Tự động thực hiện `DELETE /devices/push-token` để ngừng nhận push.

---

### 3.4. `POST /auth/refresh-token`

App tự gọi khi access token còn < 5 phút.

**Request body**
```json
{ "refreshToken": "rt_8s7d6f..." }
```

**Response 200**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "rt_5g7h8j...",   // rotate
  "expiresIn": 3600
}
```

**Lỗi hay gặp**
- `401 ERR_INVALID_CREDENTIALS` → chuyển về `[LOGIN_SCREEN]`.

---

### 3.5. `POST /auth/forgot-password` *(đề xuất thêm ngoài spec UI)*

**Request body**
```json
{ "phoneNumber": "0901234567" }
```

**Response 200** (luôn trả 200 để tránh leak SĐT có tồn tại hay không)
```json
{ "message": "Nếu SĐT tồn tại, OTP đã được gửi qua SMS." }
```

---

### 3.6. `POST /auth/reset-password` *()

**Request body**
```json
{
  "phoneNumber": "0901234567",
  "otp": "123456",
  "newPassword": "NewP@ss1!"
}
```

**Response 200** `{ "message": "Đặt lại mật khẩu thành công." }`

---

## 4. Nhóm 2 — Push Token & Thông báo

### 4.1. `POST /devices/push-token`

Đăng ký FCM token. Mapping: tự động chạy ngầm sau khi đăng nhập (`[LOGIN_SCREEN]`) hoặc đăng ký (`[REGISTER_SCREEN]`) thành công.

**Request body**
```json
{
  "fcmToken": "fK3...",
  "deviceModel": "Xiaomi Redmi Note 12",
  "osVersion": "Android 14"
}
```

**Response 201**
```json
{ "registered": true, "registeredAt": "2026-07-16T09:00:00+07:00" }
```

**Side effects**
- Lưu token vào DB mapping theo `userId`.
- Khi có sự kiện → server bắn push qua FCM.

---

### 4.2. `DELETE /devices/push-token`

Hủy đăng ký khi logout / user tắt thông báo.

**Response 204**.

---

### 4.3. `GET /notifications/inbox`

Lấy danh sách thông báo trong app (phân trang).

**Query params**

| Param | Kiểu | Mặc định |
|---|---|---|
| `page` | int | 0 |
| `size` | int | 20 |
| `unreadOnly` | bool | false |

**Response 200**
```json
{
  "items": [
    {
      "id": "n-987",
      "type": "animal.detected",
      "title": "Phát hiện VOI tại Cam 1",
      "body": "Độ tin cậy 92% · 9:04",
      "cameraId": "cam-001",
      "eventId": "evt-456",
      "isRead": false,
      "createdAt": "2026-07-16T09:04:30+07:00"
    }
  ],
  "pagination": { "page": 0, "size": 20, "total": 145 }
}
```

Enum `type`: `animal.detected`, `animal.escalated`, `fence.activated`, `fence.deactivated`, `system.alert`, `device.offline`.

---

## 5. Nhóm 3 — Camera & Live feed

### 5.1. `GET /cameras`

Danh sách camera user được phép xem. Mapping: tab `[CAMERA_LIST_TAB]` (phần hiển thị danh sách thẻ camera).

**Query params**
- `hasDetection` (bool, optional): lọc camera đang có phát hiện.
- `status` (optional): `ONLINE`/`OFFLINE`.

**Response 200**
```json
{
  "items": [
    {
      "id": "cam-001",
      "name": "Cam 1 · Rìa Rừng Cổng Bắc",
      "status": "ONLINE",
      "hasCurrentDetection": true,
      "currentDetection": {
        "speciesId": "elephant",
        "speciesName": "Voi",
        "dangerLevel": "CRITICAL",
        "count": 1,
        "confidence": 0.92,
        "firstDetectedAt": "2026-07-16T09:04:12+07:00"
      },
      "snapshotUrl": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-12.jpg"
    }
  ]
}
```

**Side effects**
- App refresh snapshot mỗi 2 giây theo spec, bằng cách gọi API 5.3.

---

### 5.2. `GET /cameras/{cameraId}`

Chi tiết 1 camera. Mapping: màn hình `[CAMERA_VIEW_SCREEN]` (màn hình chi tiết camera).

**Response 200**
```json
{
  "id": "cam-001",
  "name": "Cam 1 · Rìa Rừng Cổng Bắc",
  "location": { "lat": 11.4523, "lng": 107.4231, "address": "..." },
  "status": "ONLINE",
  "liveFeedUrl": "https://cdn.example.com/hls/cam001/index.m3u8",
  "currentDetection": { ... },
  "deviceStates": {                               // mapping API 6.1
    "sms": true,
    "speaker": false,
    "deterrent_audio": false,
    "led_flash": false,
    "electric_fence": false,
    "ranger_alert": true
  },
  "appliedPreset": "critical_danger"             // null nếu user chưa config
}
```

---

### 5.3. `GET /cameras/{cameraId}/snapshot`

Ảnh snapshot mới nhất. Mapping: màn hình `[CAMERA_VIEW_SCREEN]` (ảnh lớn nửa trên) và tab `[CAMERA_LIST_TAB]` (ảnh thumbnail trên thẻ).

**Response 200**
```json
{
  "url": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-30.jpg",
  "capturedAt": "2026-07-16T09:04:30+07:00",
  "nextRefreshIn": 2                              // gợi ý client poll sau 2s
}
```

**Lỗi hay gặp**
- `503 ERR_CAMERA_OFFLINE` → hiển thị placeholder "Camera offline".

---

### 5.4. `POST /cameras/{cameraId}/snapshot/refresh`

Nút "Làm mới" thủ công. Mapping: màn hình `[CAMERA_VIEW_SCREEN]` (kéo xuống hoặc bấm nút refresh).

**Response 200**
```json
{
  "url": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-36.jpg",
  "capturedAt": "2026-07-16T09:04:36+07:00"
}
```

**Side effects**
- Trigger yêu cầu chụp snapshot mới tới AI Server.

---

### 5.5. `GET /cameras/{cameraId}/detections/current`

Phân tích AI hiện tại dạng JSON (không qua WebSocket). Mapping: màn hình `[CAMERA_VIEW_SCREEN]` (bảng hiển thị phân tích AI).

**Response 200**
```json
{
  "speciesId": "elephant",
  "speciesName": "Voi",
  "count": 1,
  "dangerLevel": "CRITICAL",
  "confidence": 0.92,
  "firstDetectedAt": "...",
  "cumulativeSeconds": 12,
  "snapshotUrl": "..."
}
```

---

### 5.6. `GET /cameras/{cameraId}/detections/stream` *(optional, WebSocket/SSE)*

Stream realtime detection thay vì poll.

**Flow (SSE)**
```
data: { "type": "DETECTION_UPDATE", "payload": { ...AnimalDetection } }

data: { "type": "DETECTION_CLEARED" }

data: { "type": "DANGER_ESCALATION", "payload": { "from":"HIGH","to":"CRITICAL" } }
```

**Side effects**
- App reconnect mỗi 5 phút hoặc khi kết nối rớt.

---

### 5.7. `PATCH /cameras/{cameraId}`

Cập nhật thông tin chi tiết của camera (ví dụ: đổi tên hiển thị). Mapping: màn hình `[CAMERA_VIEW_SCREEN]` (nút `rename_camera_button` và hộp thoại `rename_camera_dialog`).

**Request body**
```json
{
  "name": "Cam Khu A - Bờ Sông"
}
```

**Response 200**
```json
{
  "id": "cam-001",
  "name": "Cam Khu A - Bờ Sông",
  "location": {
    "lat": 11.4523,
    "lng": 107.4231,
    "address": "Vườn Quốc gia Cát Tiên, Đồng Nai"
  },
  "status": "ONLINE",
  "hasCurrentDetection": true,
  "currentDetection": null,
  "updatedAt": "2026-07-19T04:40:00+07:00"
}
```

**Lỗi hay gặp**
- `400 ERR_VALIDATION_FAILED` -> Tên camera trống hoặc không hợp lệ.
- `409 ERR_CONCURRENT_CONFIG_CHANGE` -> Trùng tên với camera khác trong hệ thống.

---

## 6. Nhóm 4 — Override thiết bị ngoại vi (`[CAMERA_VIEW_SCREEN]` action bar)

### 6.1. `GET /cameras/{cameraId}/devices/state`

Lấy trạng thái hiện tại 6 thiết bị ngoại vi của 1 camera.

**Response 200**
```json
{
  "cameraId": "cam-001",
  "states": {
    "sms": { "enabled": true,  "source": "AUTO_PRESET" },
    "speaker": { "enabled": false, "source": "AUTO_PRESET" },
    "deterrent_audio": { "enabled": false, "source": "USER_OVERRIDE", "overrideUntil": "..." },
    "led_flash": { "enabled": false, "source": "AUTO_PRESET" },
    "electric_fence": { "enabled": true, "source": "AUTO_PRESET", "autoOffAt": "..." },
    "ranger_alert": { "enabled": true, "source": "AUTO_PRESET" }
  }
}
```

Enum `source`:
- `AUTO_PRESET` — server đang áp dụng theo preset (animal detected).
- `USER_OVERRIDE` — user vừa gạt tay.
- `SYSTEM` — server ép (vd auto-off fence).

---

### 6.2. `POST /cameras/{cameraId}/devices/{deviceKey}/override`

Bật/tắt nhanh 1 thiết bị. Mapping: màn hình `[CAMERA_VIEW_SCREEN]` (hàng nút toggle bật/tắt thiết bị ngoại vi).

**URL params**
- `deviceKey`: 1 trong 6 enum (mục 2.3).

**Request body**
```json
{ "enabled": true }
```

**Response 200**
```json
{
  "deviceKey": "led_flash",
  "enabled": true,
  "overrideUntil": "2026-07-16T11:30:00+07:00",   // nếu là override tạm
  "appliedAt": "2026-07-16T09:30:12+07:00"
}
```

**Side effects**
- Server ghi nhận bật/tắt → forward lệnh tới **Field Devices** qua gateway.
- Nếu `electric_fence = true` → mặc định `overrideUntil = now + 2 phút` rồi auto-off (đúng spec).

---

### 6.3. `POST /cameras/{cameraId}/devices/override-all`

Bật/tắt đồng loạt (vd khi user gạt "Cảnh báo khẩn cấp").

**Request body**
```json
{
  "states": {
    "sms": true,
    "speaker": true,
    "deterent_audio": true,
    "led_flash": true,
    "electric_fence": true,
    "ranger_alert": true
  }
}
```

**Response 200**
```json
{ "cameraId": "cam-001", "states": { ... đối chiếu lại với payload trên … } }
```

---

### 6.4. `POST /cameras/{cameraId}/devices/{deviceKey}/test`

Test audio / LED / loa. Mapping: nút "Nghe thử (Test Audio)" tại màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]`.

**Ví dụ:**
```
POST /cameras/cam-001/devices/deterrent_audio/test
```

**Request body**
```json
{
  "sampleId": "gunshot",
  "durationSeconds": 3
}
```

**Response 202** (Accepted — lệnh đã gửi xuống field, kết quả qua push event `DEVICE_TEST_COMPLETED`)
```json
{ "testId": "t-991", "queuedAt": "..." }
```

---

## 7. Nhóm 5 — Preset & Thiết lập chung

### 7.1. `GET /control/system/status`

Toàn bộ trạng thái điều khiển của user trên mọi camera.

**Response 200**
```json
{
  "global": {
    "smsNotificationEnabled": true,            // user-level toggle ở tab `[SETTING_TAB]` hoặc cài đặt chung
    "smsRingtoneEnabled": true                 // cài đặt âm thanh thông báo ở tab `[SETTING_TAB]`
  },
  "cameras": [ {
    "cameraId": "cam-001",
    "appliedPresetId": "critical_danger",
    "deviceStates": { /* tương tự API 6.1 */ }
  } ]
}
```

---

### 7.2. `POST /control/system/sms-notification/toggle`

Mapping: Không có nút điều khiển trực tiếp trên UI mới, chạy ngầm hoặc tích hợp tại tab `[SETTING_TAB]`.

**Request body**
```json
{ "enabled": true }
```

**Response 200**
```json
{ "enabled": true, "appliedAt": "..." }
```

---

### 7.3. `GET /control/presets`

Danh sách preset mẫu. Mapping: màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (nhóm nút chọn kịch bản phòng vệ mẫu).

**Response 200**
```json
{
  "items": [
    {
      "id": "intruder",
      "displayName": "Người lạ đột nhập",
      "forSpecies": "HUMAN",
      "config": { /* ResponseConfigSchema xem mục 8.2 */ }
    },
    {
      "id": "medium_danger",
      "displayName": "Thú vừa",
      "description": "Áp dụng cho loài ít nguy hiểm (Nai, Khỉ, Hươu)"
    },
    {
      "id": "critical_danger",
      "displayName": "Thú cực kỳ nguy hiểm",
      "description": "Silent Alert — không phát loa/LED tại chỗ",
      "silentAlert": true
    }
  ]
}
```

---

### 7.4. `POST /control/presets/{presetId}/apply`

Áp dụng preset cho 1 camera hoặc tất cả.

**Request body**
```json
{
  "cameraId": "cam-001 | ALL",
  "presetId": "critical_danger"
}
```

**Response 200**
```json
{
  "appliedPresetId": "critical_danger",
  "affectedCameraIds": ["cam-001", "cam-002"],
  "effectiveAt": "..."
}
```

---

### 7.5. `GET /audio-samples`

Mapping: dropdown `sound_type_dropdown` chọn loại âm thanh xua đuổi tại màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]`.

**Response 200**
```json
{
  "items": [
    { "id": "gunshot",     "displayName": "Tiếng súng",     "previewUrl": "..." },
    { "id": "growl",       "displayName": "Tiếng gầm",      "previewUrl": "..." },
    { "id": "dog_bark",    "displayName": "Tiếng chó sủa lớn", "previewUrl": "..." },
    { "id": "explosion",   "displayName": "Tiếng nổ giả lập",  "previewUrl": "..." },
    { "id": "ultrasonic",  "displayName": "Tần số siêu âm",    "previewUrl": "...", "frequencyHz": 22000 }
  ]
}
```

---

### 7.6. `GET /audio-samples/{sampleId}/stream`

URL streaming file MP3/WAV để player "Nghe thử" âm thanh tại màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]`.

**Response 200** → trả về binary stream (Content-Type `audio/mpeg`).

---

### 7.7. `GET /speaker-templates`

Mapping: dropdown chọn mẫu nội dung cảnh báo qua loa tại màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]`.

**Response 200**
```json
{
  "items": [
    { "id": "tpl-01", "displayName": "Mẫu 1 - Cảnh báo voi hoang dã xuất hiện", "previewUrl": "..." },
    { "id": "tpl-02", "displayName": "Mẫu 2 - Phát hiện thú dữ xâm lấn nguy cấp", "previewUrl": "..." },
    { "id": "tpl-03", "displayName": "Mẫu 3 - Thông báo di tản & chỉ dẫn lánh nạn", "previewUrl": "..." }
  ]
}
```

---

## 8. Nhóm 6 — Cấu hình ứng phó theo loài (`[SPECIES_CONFIG_DETAIL_SCREEN]`)

### 8.1. `GET /species`

Mapping: danh sách các loài dạng chip chọn tại màn hình `[SPECIES_CONFIG_LIST_SCREEN]`.

**Response 200**
```json
{
  "items": [
    { "id": "crocodile", "displayName": "Cá sấu", "dangerLevel":"HIGH" },
    { "id": "deer",      "displayName": "Nai",   "dangerLevel":"LOW" },
    { "id": "elephant",  "displayName": "Voi",   "dangerLevel":"CRITICAL" },
    { "id": "giraffe",   "displayName": "Hươu cao cổ", "dangerLevel":"LOW" },
    { "id": "leopard",   "displayName": "Báo",   "dangerLevel":"CRITICAL" },
    { "id": "monkey",    "displayName": "Khỉ",  "dangerLevel":"LOW" },
    { "id": "rhino",     "displayName": "Tê giác", "dangerLevel":"CRITICAL" },
    { "id": "snake",     "displayName": "Rắn", "dangerLevel":"HIGH" },
    { "id": "tiger",     "displayName": "Hổ",  "dangerLevel":"CRITICAL" }
  ]
}
```

---

### 8.2. `PUT /response-configs/{cameraId}/{speciesId}`

Tạo / cập nhật cấu hình ứng phó cho cặp **(camera × loài)**. Mapping: màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (bấm nút Lưu `save_config_button`).

**URL params**
- `cameraId`: `cam-...` hoặc `ALL` (áp dụng cho tất cả).
- `speciesId`: id từ API 8.1.

**Request body** (full schema)
```json
{
  "audio": {
    "type": "gunshot",
    "intensity": 75,
    "sampleId": "gunshot"
  },
  "led": {
    "flashRate": "4_per_sec",
    "color": "red_white_alt",
    "durationSeconds": 60
  },
  "fence": {
    "level": "medium",
    "warningLight": true,
    "autoNotify": true,
    "autoOffEnabled": true,
    "autoOffMinutes": 2                 // BẮT BUỘC >= 2 theo spec
  },
  "speaker": {
    "templateId": "tpl-02"
  },
  "silentAlert": false                  // true => chỉ push notification
}
```

**Response 200**
```json
{
  "cameraId": "cam-001",
  "speciesId": "elephant",
  "config": { /* echo body */ },
  "updatedAt": "2026-07-16T09:30:00+07:00",
  "updatedBy": "9f3a"
}
```

**Validate nghiệp vụ**
- `intensity` ∈ [1,100].
- `fence.autoOffMinutes` ≥ 2.
- Nếu `silentAlert = true` thì `led`,`speaker` có thể null.

**Side effects**
- Lưu DB, set làm default cho cặp (camera, loài).
- Push `RESPONSE_CONFIG_UPDATED` cho các thiết bị đang online của user (nếu user đang mở app trên thiết bị khác).

**Lỗi hay gặp**
- `409 ERR_CONCURRENT_CONFIG_CHANGE` → UI hiện dialog "Cấu hình đã thay đổi, tải lại?" + button Refresh.
- `422 ERR_FENCE_AUTO_OFF_TOO_SHORT` → UI đỏ field "autoOffMinutes".

---

### 8.3. `GET /response-configs?cameraId=&speciesId=`

Lấy config của 1 cặp. Mapping: màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (khi load cấu hình hiện tại của loài).

**Response 200** (hoặc 404 nếu chưa config)
```json
{
  "cameraId": "cam-001",
  "speciesId": "elephant",
  "config": { /* ResponseConfigSchema */ },
  "fallbackPresetId": "critical_danger",    // server dùng preset này nếu user xoá config
  "updatedAt": "..."
}
```

**Concurrency model**
- Client lưu `lastModifiedAt` (lấy từ `updatedAt`).
- Khi PUT, gửi header `If-Match: <lastModifiedAt>`.
- Server trả `409 ERR_CONCURRENT_CONFIG_CHANGE` nếu `updatedAt` mới hơn.

---

### 8.4. `DELETE /response-configs/{cameraId}/{speciesId}`

Xoá config — server quay về `fallbackPresetId` (mặc định theo `dangerLevel`).

**Response 204**.

---

### 8.5. `POST /response-configs/{cameraId}/{speciesId}/apply-preset/{presetId}`

Áp dụng preset vào 1 cặp — UI có nút Preset gần chip loài (mục 7.4.7).

**Request body** (optional override)
```json
{
  "intensity": 80,
  "fence": { "level": "high" }
}
```

**Response 200** → trả về config đã merge preset + override.

---

### 8.6. `GET /response-configs/{cameraId}` *(helper)*

Lấy tất cả cấu hình của 1 camera (cho các loài). Mapping: màn hình `[SPECIES_CONFIG_LIST_SCREEN]` (hiển thị danh sách các cấu hình của từng loài).

**Response 200**
```json
{
  "cameraId": "cam-001",
  "configs": [
    { "speciesId": "elephant", "config": {...}, "updatedAt":"..." },
    { "speciesId": "tiger",    "config": {...}, "updatedAt":"..." }
  ]
}
```

---

## 9. Nhóm 7 — Cài đặt chung (`[SETTING_TAB]`)

### 9.1. `GET /users/me`

**Response 200**
```json
{
  "id": "9f3a",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "+84901234567",
  "role": "CITIZEN",
  "preferences": {
    "smsRingtoneEnabled": true,
    "pushNotificationEnabled": true
  }
}
```

---

### 9.2. `PATCH /users/me`

Cập nhật thông tin + preferences.

**Request body** (partial)
```json
{
  "fullName": "Nguyễn Văn A",
  "preferences": {
    "smsRingtoneEnabled": false
  }
}
```

**Response 200** → trả về user mới.

---

### 9.3. `POST /users/me/change-password`

**Request body**
```json
{
  "oldPassword": "OldP@ss1",
  "newPassword": "NewP@ss2!"
}
```

**Response 200** `{ "message": "Đổi mật khẩu thành công." }`

**Side effects**
- Vô hiệu hoá refresh tokens cũ → user phải login lại trên các thiết bị khác.

---

### 9.5. `POST /users/me/push-notifications/toggle` *(khuyến nghị tách)*

Mapping: cài đặt tắt/bật push notification trong tab `[SETTING_TAB]`.

**Request body**
```json
{
  "enabled": false,
  "scope": ["animal.detected", "fence.activated"]   // optional, mặc định all
}
```

---

## 10. Nhóm 8 — Lịch sử & Thống kê (`[STATISTICS_TAB]`, `[CAMERA_VIEW_SCREEN]`)

### 10.1. `GET /events`

Phân trang + lọc.

**Query params**

| Param | Kiểu | Ghi chú |
|---|---|---|
| `from` | ISO datetime | mặc định = now - 7 ngày |
| `to` | ISO datetime | mặc định = now |
| `cameraId` | string | optional |
| `speciesId` | string | optional |
| `dangerLevel` | enum | optional |
| `page` | int | mặc định 0 |
| `size` | int | mặc định 20 |

**Response 200**
```json
{
  "items": [
    {
      "id": "evt-456",
      "cameraId": "cam-001",
      "cameraName": "Cam 1 · Rìa Rừng Cổng Bắc",
      "speciesId": "elephant",
      "speciesName": "Voi",
      "count": 1,
      "dangerLevel": "CRITICAL",
      "firstDetectedAt": "2026-07-16T09:04:12+07:00",
      "resolvedAt": "2026-07-16T09:18:45+07:00",
      "responseMode": "SILENT_ALERT",            // SILENT_ALERT / ACTIVE_DETERRENCE
      "snapshotThumbnails": ["..."]
    }
  ],
  "pagination": { "page": 0, "size": 20, "total": 312 }
}
```

---

### 10.2. `GET /events/{eventId}`

Chi tiết sự kiện.

**Response 200**
```json
{
  "id": "evt-456",
  "cameraId": "cam-001",
  "species": { "id":"elephant", "displayName":"Voi", "dangerLevel":"CRITICAL" },
  "timeline": [
    { "at":"09:04:12", "type":"DETECTED", "confidence":0.65 },
    { "at":"09:04:30", "type":"AI_CONFIRMED", "confidence":0.92 },
    { "at":"09:04:42", "type":"CUMULATIVE_THRESHOLD_REACHED", "seconds":10 },
    { "at":"09:04:43", "type":"RESPONSE_TRIGGERED", "presetId":"critical_danger" },
    { "at":"09:18:45", "type":"RESOLVED" }
  ],
  "appliedConfigSnapshot": { /* ResponseConfigSchema */ },
  "deviceLogs": [
    { "deviceKey":"sms",            "action":"ON",  "at":"09:04:43" },
    { "deviceKey":"speaker",        "action":"OFF", "at":"09:04:43" },
    { "deviceKey":"electric_fence", "action":"ON",  "at":"09:04:43", "autoOffAt":"09:06:43" }
  ]
}
```

---

### 10.3. `GET /events/{eventId}/snapshots`

**Response 200**
```json
{
  "items": [
    { "url":"...", "capturedAt":"09:04:12" },
    { "url":"...", "capturedAt":"09:04:30" },
    { "url":"...", "capturedAt":"09:04:43" }
  ]
}
```

---

### 10.4. `POST /events/{eventId}/ack`

User xác nhận đã đọc.

**Response 204**.

---

### 10.5. `GET /stats/summary`

**Query params**
- `from`, `to`: ISO datetime.

**Response 200**
```json
{
  "totalEvents": 312,
  "byDangerLevel": { "LOW":210, "MEDIUM":0, "HIGH":32, "CRITICAL":70 },
  "bySpecies": [
    { "speciesId":"monkey",  "count":87 },
    { "speciesId":"deer",    "count":65 },
    { "speciesId":"elephant","count":12 }
  ],
  "hotHours": [ { "hour": 4, "count": 48 }, { "hour": 19, "count": 35 } ],
  "byCamera": [ { "cameraId":"cam-001", "count": 156 } ]
}
```

---

## 11. Nhóm 9 — Phân vai & Liên ngành

### 11.1. `GET /users/me/role`

**Response 200**
```json
{
  "role": "RANGER",
  "permissions": {
    "canViewAllCameras": true,
    "canEditResponseConfig": true,
    "canReceive": ["ANIMAL_RARE", "INTRUDER", "FENCE_ACTIVATED"]
  }
}
```

---

### 11.2. `GET /alerts/feed`

Cảnh báo đã phân luồng theo vai trò (mapping hiển thị tại tab `[STATISTICS_TAB]` và thông báo đẩy).

**Query params**
- `severity`, `from`, `to`, `page`, `size`.

**Response 200**
```json
{
  "items": [
    {
      "id": "alt-991",
      "type": "ANIMAL_RARE",
      "title": "Phát hiện HỔ tại Cam 2",
      "dangerLevel": "CRITICAL",
      "cameraId": "cam-002",
      "eventId": "evt-991",
      "createdAt": "...",
      "isRead": false
    }
  ]
}
```

Enum `type`: `ANIMAL_RARE` (Kiểm lâm), `HIGHWAY_NEARBY` (Ban QL cao tốc), `HUMAN_BORDER` (Biên phòng), `INTRUDER` (Kiểm lâm / Biên phòng).

---

### 11.3. `POST /alerts/{alertId}/forward`

Cấp cao hơn forward alert (vd Kiểm lâm chi viện gửi Biên phòng).

**Request body**
```json
{
  "toRole": "BORDER_GUARD",
  "note": "Yêu cầu phối hợp kiểm tra vùng biên."
}
```

**Response 200** `{ "alertId":"alt-991", "forwardedTo":"BORDER_GUARD", "forwardedAt":"..." }`

---

## 12. Nhóm 10 — Quản lý SĐT nhận SMS (`[SMS_CONFIG_SCREEN]`)

### 12.1. `GET /users/me/sms-recipients`

**Response 200**
```json
{
  "items": [
    { "id":"r-1", "fullName":"Nguyễn Văn A (Tôi)", "phoneNumber":"+84901234567", "relation":"self" },
    { "id":"r-2", "fullName":"Nguyễn Thị B",        "phoneNumber":"+84987654321", "relation":"family" }
  ]
}
```

---

### 12.2. `POST /users/me/sms-recipients`

**Request body**
```json
{ "fullName":"Nguyễn Thị C", "phoneNumber":"0987654322", "relation":"neighbor" }
```

**Response 201**
```json
{ "id":"r-3", "fullName":"Nguyễn Thị C", "phoneNumber":"+84987654322", "relation":"neighbor" }
```

**Lỗi hay gặp**
- `409 ERR_PHONE_ALREADY_USED` → "SĐT này đã có người dân khác đăng ký gửi cùng khu vực."

**Side effects**
- Server sẽ tự động gửi SMS mẫu xác nhận (welcome message "Bạn đã đăng ký nhận cảnh báo xua đuổi động vật...").

---

### 12.3. `DELETE /users/me/sms-recipients/{recipientId}`

**Response 204**.

**Side effects**
- Ngừng gửi SMS cho số này trong các lần phát hiện sau.

---

## 13. Nhóm 11 — Hỗ trợ & Meta

### 13.1. `GET /health`

**Response 200**
```json
{ "status":"UP", "uptimeSeconds":98765, "aiServerReachable":true }
```

---

### 13.2. `GET /app/version`

**Response 200**
```json
{
  "minSupportedVersion": "1.0.0",
  "latestVersion": "1.4.0",
  "forceUpdate": false,
  "updateUrl": "https://play.google.com/store/apps/details?id=..."
}
```

---

### 13.3. `GET /reference-data/danger-levels`

**Response 200**
```json
{
  "items": [
    { "level":"LOW",      "displayName":"Ít nguy hiểm",   "colorHex":"#4CAF50" },
    { "level":"MEDIUM",   "displayName":"Nguy hiểm TB",   "colorHex":"#FFC107" },
    { "level":"HIGH",     "displayName":"Nguy hiểm cao",  "colorHex":"#FF5722" },
    { "level":"CRITICAL", "displayName":"CỰC KỲ nguy hiểm","colorHex":"#D32F2F" }
  ],
  "speciesByLevel": {
    "LOW":      ["deer","giraffe","monkey"],
    "CRITICAL": ["elephant","tiger","leopard","rhino"]
  }
}
```

---

## 13a. Nhóm 12 — Tích hợp thiết bị & AI Server

### 13a.1. `POST /cameras/{cameraId}/detections`

Gửi dữ liệu hình ảnh chụp được, thông tin người dùng và kết quả phán đoán từ AI Server / thiết bị Camera lên Mobile Server. Server sẽ lưu trữ hình ảnh và phản hồi cấu hình hành vi ứng phó tương ứng cho loài động vật được phát hiện để thiết bị thực thi tại hiện trường.

**Content-Type:** `multipart/form-data`

**Request Body (Form data)**

| Field | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `image` | File (Binary) | Có | Ảnh snapshot chụp từ camera tại hiện trường |
| `userId` | string | Có | ID của người dùng cấu hình / sở hữu trạm camera (mã hex 4 ký tự, ví dụ: `9f3a`) |
| `speciesId` | string | Có | ID của loài phát hiện (ví dụ: `elephant`, `tiger`, `monkey`) |
| `count` | int | Có | Số lượng cá thể phát hiện |
| `confidence` | float | Có | Độ tin cậy nhận diện của mô hình AI (từ 0.0 đến 1.0) |

**Response 201 (Created)**

Trả về cấu hình phòng vệ cụ thể cần được thực hiện tại hiện trường.

```json
{
  "eventId": "evt-456",
  "cameraId": "cam-001",
  "speciesId": "elephant",
  "dangerLevel": "CRITICAL",
  "imageUrl": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-12.jpg",
  "detectedAt": "2026-07-19T04:55:00+07:00",
  "responseAction": {
    "silentAlert": false,
    "audio": {
      "enabled": true,
      "type": "gunshot",
      "intensity": 75,
      "sampleId": "gunshot"
    },
    "led": {
      "enabled": true,
      "flashRate": "4_per_sec",
      "color": "red_white_alt",
      "durationSeconds": 60
    },
    "fence": {
      "enabled": true,
      "level": "medium",
      "warningLight": true,
      "autoNotify": true,
      "autoOffEnabled": true,
      "autoOffMinutes": 2
    },
    "speaker": {
      "enabled": false,
      "templateId": "tpl-02"
    },
    "smsNotify": true,
    "pushNotify": true
  }
}
```

**Side effects**
- Lưu trữ ảnh snapshot vào CDN / Storage của Mobile Server.
- Ghi nhận nhật ký sự kiện (`event`) vào DB PostgreSQL.
- Bắn Push Notification và SMS khẩn cấp đến danh sách SĐT đăng ký nhận cảnh báo nếu cấu hình yêu cầu (`smsNotify` / `pushNotify`).
- Đẩy dữ liệu realtime qua kết nối WebSocket/SSE đến các client Android đang mở app.

**Lỗi hay gặp**
- `400 ERR_VALIDATION_FAILED` -> File ảnh không hợp lệ hoặc thiếu tham số.
- `404 ERR_CAMERA_NOT_FOUND` -> `cameraId` không tồn tại.
- `404 ERR_USER_NOT_FOUND` -> `userId` không hợp lệ.

---

## 14. Phụ lục — Bảng tổng hợp

| # | Group | Method | Endpoint | Màn hình / Luồng (Hệ thống mới) |
|---|---|---|---|---|
| 3.1 | Auth | POST | `/auth/register` | `[REGISTER_SCREEN]` |
| 3.2 | Auth | POST | `/auth/login` | `[LOGIN_SCREEN]` |
| 3.3 | Auth | POST | `/auth/logout` | Tab `[SETTING_TAB]` |
| 3.4 | Auth | POST | `/auth/refresh-token` | Tự động chạy ngầm |
| 3.5 | Auth | POST | `/auth/forgot-password` | Màn hình ngoài spec (Login flow) |
| 3.6 | Auth | POST | `/auth/reset-password` | Màn hình ngoài spec (Forgot Password flow) |
| 4.1 | Push | POST | `/devices/push-token` | Chạy ngầm sau đăng nhập/đăng ký |
| 4.2 | Push | DELETE | `/devices/push-token` | Chạy ngầm sau đăng xuất |
| 4.3 | Push | GET | `/notifications/inbox` | Tải danh sách thông báo đẩy (không có màn hình trực tiếp) |
| 5.1 | Camera | GET | `/cameras` | Tab `[CAMERA_LIST_TAB]` |
| 5.2 | Camera | GET | `/cameras/{id}` | Màn hình `[CAMERA_VIEW_SCREEN]` |
| 5.3 | Camera | GET | `/cameras/{id}/snapshot` | Tab `[CAMERA_LIST_TAB]` (thumbnail), Màn hình `[CAMERA_VIEW_SCREEN]` |
| 5.4 | Camera | POST | `/cameras/{id}/snapshot/refresh` | Màn hình `[CAMERA_VIEW_SCREEN]` |
| 5.5 | Camera | GET | `/cameras/{id}/detections/current` | Màn hình `[CAMERA_VIEW_SCREEN]` (bảng AI) |
| 5.6 | Camera | GET | `/cameras/{id}/detections/stream` | Màn hình `[CAMERA_VIEW_SCREEN]` (realtime) |
| 5.7 | Camera | PATCH | `/cameras/{id}` | Màn hình `[CAMERA_VIEW_SCREEN]` (đổi tên camera) |
| 6.1 | Override | GET | `/cameras/{id}/devices/state` | Màn hình `[CAMERA_VIEW_SCREEN]` / `[SPECIES_CONFIG_DETAIL_SCREEN]` |
| 6.2 | Override | POST | `/cameras/{id}/devices/{key}/override` | Màn hình `[CAMERA_VIEW_SCREEN]` (toggle thiết bị ngoại vi) |
| 6.3 | Override | POST | `/cameras/{id}/devices/override-all` | Màn hình `[CAMERA_VIEW_SCREEN]` (phát báo động khẩn cấp) |
| 6.4 | Override | POST | `/cameras/{id}/devices/{key}/test` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (nút nghe thử) |
| 7.1 | Control | GET | `/control/system/status` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` / Chạy ngầm |
| 7.2 | Control | POST | `/control/system/sms-notification/toggle` | Tích hợp tab `[SETTING_TAB]` (Chạy ngầm) |
| 7.3 | Control | GET | `/control/presets` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (tải presets) |
| 7.4 | Control | POST | `/control/presets/{id}/apply` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (chọn kịch bản nhanh) |
| 7.5 | Control | GET | `/audio-samples` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (dropdown âm thanh) |
| 7.6 | Control | GET | `/audio-samples/{id}/stream` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (nghe thử audio) |
| 7.7 | Control | GET | `/speaker-templates` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (dropdown mẫu loa) |
| 8.1 | Config | GET | `/species` | Màn hình `[SPECIES_CONFIG_LIST_SCREEN]`, `[SPECIES_CONFIG_DETAIL_SCREEN]` |
| 8.2 | Config | PUT | `/response-configs/{cam}/{species}` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (Lưu) |
| 8.3 | Config | GET | `/response-configs?cameraId=&speciesId=` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (Load config) |
| 8.4 | Config | DELETE | `/response-configs/{cam}/{species}` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (Đặt lại) |
| 8.5 | Config | POST | `/response-configs/{cam}/{species}/apply-preset/{id}` | Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (áp preset) |
| 8.6 | Config | GET | `/response-configs/{cam}` | Màn hình `[SPECIES_CONFIG_LIST_SCREEN]` (danh sách config các loài) |
| 9.1 | Settings | GET | `/users/me` | Tab `[SETTING_TAB]` |
| 9.2 | Settings | PATCH | `/users/me` | Tab `[SETTING_TAB]` |
| 9.3 | Settings | POST | `/users/me/change-password` | Tab `[SETTING_TAB]` |
| 9.5 | Settings | POST | `/users/me/push-notifications/toggle` | Tab `[SETTING_TAB]` |
| 10.1 | Stats | GET | `/events` | Tab `[STATISTICS_TAB]`, Màn hình `[CAMERA_VIEW_SCREEN]` (timeline log) |
| 10.2 | Stats | GET | `/events/{id}` | Màn hình `[CAMERA_VIEW_SCREEN]` (log lightbox) |
| 10.3 | Stats | GET | `/events/{id}/snapshots` | Màn hình `[CAMERA_VIEW_SCREEN]` (lightbox snapshots) |
| 10.4 | Stats | POST | `/events/{id}/ack` | Màn hình `[CAMERA_VIEW_SCREEN]` (đọc log) |
| 10.5 | Stats | GET | `/stats/summary` | Tab `[STATISTICS_TAB]` (biểu đồ thống kê) |
| 11.1 | Role | GET | `/users/me/role` | Phân quyền (chạy ngầm sau đăng nhập) |
| 11.2 | Role | GET | `/alerts/feed` | Tab `[STATISTICS_TAB]` |
| 11.3 | Role | POST | `/alerts/{id}/forward` | Luồng liên ngành (không có UI trực tiếp) |
| 12.1 | SMS | GET | `/users/me/sms-recipients` | Màn hình `[SMS_CONFIG_SCREEN]` |
| 12.2 | SMS | POST | `/users/me/sms-recipients` | Màn hình `[SMS_CONFIG_SCREEN]` |
| 12.3 | SMS | DELETE | `/users/me/sms-recipients/{id}` | Màn hình `[SMS_CONFIG_SCREEN]` |
| 13.1 | Meta | GET | `/health` | Chạy ngầm / Monitor |
| 13.2 | Meta | GET | `/app/version` | Khởi động (bắt buộc update) |
| 13.3 | Meta | GET | `/reference-data/danger-levels` | Tab `[STATISTICS_TAB]`, Màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` |
| 13a.1 | Device | POST | `/cameras/{id}/detections` | Tích hợp thiết bị Camera & AI Server |

**Tổng: 52 API (Đã bổ sung API đổi tên camera và API tích hợp camera, bỏ API ngôn ngữ).**

---

## 15. Ghi chú triển khai

- **Versioning:** mọi breaking change phải bump `/api/v2/` — không xoá/sửa ở v1.
- **Pagination:** tất cả GET list hỗ trợ `page` + `size` với response chuẩn (items + pagination).
- **Rate limit:** 100 req / phút / IP / user (tăng lên cho endpoint test audio).
- **Concurrency:** tất cả PUT dùng `If-Match: <lastModifiedAt>` để chống ghi đè lẫn nhau giữa các thiết bị.
- **i18n:** mọi message trả về nên có key (vd `errors.auth.invalid_credentials`) — client map sang tiếng Việt. Hiện tại tài liệu Vietnamese-first.
- **Security:** SĐT cần lưu ở dạng E.164 (`+84...`). Mật khẩu cần hash ≥ bcrypt cost 12 / argon2id.
- **File này cũng** chứa **đặc tả hành vi observable** của Mobile Server: trạng thái nào cho mỗi thiết bị, payload JSON mỗi endpoint, status code, side effects. **Không** chứa đặc tả chi tiết schema DB (xem thêm `docs/Mobile_API_DB.md` ở giai đoạn sau) hay script CI/CD.
