# Mobile_API — Đặc tả API cho Ứng dụng Android

> **Phạm vi tài liệu:** Đặc tả HTTP API mà **ứng dụng Android (Mobile App)** sử dụng để tương tác với **Mobile Server**. Tài liệu này dùng cho đội phát triển Backend & Mobile để thống nhất contract; không lặp lại các quy tắc nghiệp vụ đã nêu trong [01-de-tai-nghien-cuu-canh-bao-dong-vat.md](./01-de-tai-nghien-cuu-canh-bao-dong-vat.md).

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
| `204 No Content` | Thành công, không có body | DELETE không cần response |
| `400 Bad Request` | Sai format / thiếu field | Validation client fail |
| `401 Unauthorized` | Thiếu token / token hết hạn / sai token | Chuyển về màn hình LOGIN |
| `403 Forbidden` | Token hợp lệ nhưng không có quyền | Vai trò không đủ (vd USER xem camera quân sự) |
| `404 Not Found` | Không tìm thấy resource | Sai ID / quyền bị thu hồi |
| `409 Conflict` | Xung đột dữ liệu | SĐT trùng / config preset đã đổi trước user |
| `422 Unprocessable Entity` | Validate nghiệp vụ fail | Sai logic (vd fence level không thuộc enum) |
| `429 Too Many Requests` | Rate limit | Spam đăng nhập / spam test thiết bị |
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

### 2.3. DeviceKey (Enum thiết bị ngoại vi)

```
sms                  — Gửi SMS cảnh báo cho người dân lân cận
speaker              — Loa phát thanh AI tại trạm
deterrent_audio      — Âm thanh xua đuổi (tần số thấp / siêu âm)
led_flash            — Đèn LED flash nhiều màu
electric_fence       — Hàng rào điện sinh học
ranger_alert         — Gửi báo động về trạm Kiểm lâm
```

### 2.4. DefendAction (Schema cấu hình phòng vệ)

Cấu hình phòng vệ tiêu chuẩn được áp dụng khi phát hiện động vật.

```json
{
  "audio": {
    "type": "gunshot",                   // A_gunshot / A_growl / A_dog_bark / A_explosion / A_ultrasonic
    "intensity": 75,                     // Cường độ âm thanh (1 - 100)
    "sampleId": "A_gunshot"              // ID âm thanh xua đuổi thú (bắt đầu bằng prefix 'A_')
  },
  "led": {
    "flashRate": "4_per_sec",            // 2_per_sec / 4_per_sec / random
    "color": "red_white_alt",            // red / white / red_white_alt
    "durationSeconds": 60                // Thời lượng LED chớp (giây)
  },
  "fence": {
    "level": "medium",                   // low / medium / high (mức dòng điện)
    "warningLight": true,                // Có bật đèn cảnh báo màu hổ phách/đỏ tại hiện trường
    "autoNotify": true,                  // Tự động SMS/Push khi kích hoạt hàng rào
    "autoOffEnabled": true,              // Tự động tắt hàng rào sau 2 phút không có thú
    "autoOffMinutes": 2                  // Thời gian tự ngắt (BẮT BUỘC >= 2)
  },
  "speaker": {
    "sampleId": "N_warning_thu"          // ID âm thanh cảnh báo người dân (bắt đầu bằng prefix 'N_')
  },
  "silentAlert": false                   // true => Cảnh báo âm thầm (không còi/đèn tại chỗ)
}
```

---

## 3. Nhóm 1 — Xác thực & Tài khoản (`[LOGIN_SCREEN]`, `[REGISTER_SCREEN]`)

### 3.1. `POST /auth/register`

Đăng ký tài khoản mới. Mapping: màn hình `[REGISTER_SCREEN]` (mục 7.4.2 của [01-de-tai-nghien-cuu-canh-bao-dong-vat.md](./01-de-tai-nghien-cuu-canh-bao-dong-vat.md)).

**Request body**
```json
{
  "username": "nguyenvana",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0901234567",
  "email": "nguyenvana@example.com", // Tùy chọn (Optional)
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
    "username": "nguyenvana",
    "fullName": "Nguyễn Văn A",
    "phoneNumber": "+84901234567",
    "email": "nguyenvana@example.com", // null nếu không nhập
    "role": "CITIZEN",
    "createdAt": "2026-07-16T08:30:00+07:00"
  },
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "rt_8s7d6f...",
  "expiresIn": 3600
}
```

**Validation format (client-side, fail → 400)**
- `username`: Bắt buộc, 4-20 ký tự, gồm chữ cái, số và dấu gạch dưới, không bắt đầu bằng số.
- `phoneNumber`: regex `^0[0-9]{9}$` (SĐT Việt Nam 10 số).
- `email`: Không bắt buộc. Nếu có nhập, phải đúng định dạng email tiêu chuẩn (`local@domain.tld`).
- `password`: ≥ 8 ký tự, có chữ hoa, chữ thường, số, ký tự đặc biệt.
- `role`: 1 trong 4 enum trên — UI là Dropdown.

**Side effects**
- Hash mật khẩu (bcrypt/argon2).
- Tạo JWT access (1h) + refresh (30 ngày).
- Tự động thực hiện đăng nhập → chuyển sang `MAIN`.

**Lỗi hay gặp**
- `409 ERR_USERNAME_ALREADY_USED` → hiện lỗi inline tại field Username.
- `409 ERR_PHONE_ALREADY_USED` → hiện lỗi inline tại field SĐT.
- `409 ERR_EMAIL_ALREADY_USED` → hiện lỗi inline tại field Email (nếu email được gửi).

---

### 3.2. `POST /auth/login`

Mapping: màn hình `[LOGIN_SCREEN]` (mục 7.4.2 của [01-de-tai-nghien-cuu-canh-bao-dong-vat.md](./01-de-tai-nghien-cuu-canh-bao-dong-vat.md)).

**Request body**
```json
{
  "username": "nguyenvana",
  "password": "P@ssw0rd!"
}
```

**Response 200**
```json
{
  "user": { 
    "id": "9f3a", 
    "username": "nguyenvana", 
    "fullName": "Nguyễn Văn A", 
    "phoneNumber": "+84901234567", 
    "email": "nguyenvana@example.com",
    "role": "CITIZEN" 
  },
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 3600
}
```

**Lỗi hay gặp**
- `401 ERR_INVALID_CREDENTIALS` → banner đỏ: "Tên đăng nhập hoặc mật khẩu không đúng." Sau 5 lần sai liên tiếp → `429` (rate limit 1 phút).
- `404 ERR_USERNAME_NOT_REGISTERED` → "Tên đăng nhập chưa đăng ký" + link "Đăng ký ngay".

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

> ⚠️ **GHI CHÚ:** ĐÂY LÀ API DÙNG ĐỂ TEST (DEVELOP)

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
        "eventId": "evt-456",
        "detections": [
          { "speciesId": "elephant", "speciesName": "Voi", "confidence": 0.92, "dangerLevel": "CRITICAL" }
        ],
        "resolvedDangerLevel": "CRITICAL",
        "firstDetectedAt": "2026-07-16T09:04:12+07:00"
      },
      "snapshotUrl": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-12.jpg"
    }
  ]
}
```

**Side effects**
- Khi có sự kiện mới, App nhận thông báo qua FCM và tự động làm mới danh sách.

---

### 5.2. `GET /cameras/{cameraId}`

Chi tiết 1 camera bao gồm thông tin camera, ảnh snapshot gần nhất và phán đoán hình ảnh hiện tại. Mapping: màn hình `[CAMERA_VIEW_SCREEN]` (màn hình chi tiết camera).

**Response 200**
```json
{
  "id": "cam-001",
  "name": "Cam 1 · Rìa Rừng Cổng Bắc",
  "location": {
    "lat": 11.4523,
    "lng": 107.4231,
    "address": "Vườn Quốc gia Cát Tiên, Đồng Nai"
  },
  "status": "ONLINE",
  "liveFeedUrl": "https://cdn.example.com/hls/cam001/index.m3u8",
  "snapshot": {
    "url": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-12.jpg",
    "capturedAt": "2026-07-16T09:04:12+07:00"
  },
  "currentDetection": {
    "eventId": "evt-456",
    "detections": [
      { "speciesId": "elephant", "speciesName": "Voi", "confidence": 0.92, "dangerLevel": "CRITICAL" }
    ],
    "resolvedDangerLevel": "CRITICAL",
    "firstDetectedAt": "2026-07-16T09:04:12+07:00"
  },
  "updatedAt": "2026-07-19T04:40:00+07:00"
}
```

---

### 5.3.  `PATCH /cameras/{cameraId}`

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

---

### 5.4. `GET /cameras/stream`

Kết nối Server-Sent Events (SSE) để nhận các thông báo cập nhật thời gian thực từ phía server về tình trạng các trạm camera. Khi có thông báo này, client Android sẽ tự động gọi lại các API REST tương ứng để tải lại dữ liệu mới nhất (Mô hình **Ping-to-Fetch**).

**Headers**
- `Accept: text/event-stream`
- `Authorization: Bearer <accessToken>`

**Event Stream Structure**

*   **Tên sự kiện (event name):** `camera-update`
*   **Dữ liệu (data):** Chuỗi JSON chứa mã camera và loại cập nhật.

**Ví dụ dữ liệu nhận được:**

```json
{
  "cameraId": "cam-001",
  "updateType": "DETECTION",
  "timestamp": "2026-07-19T04:55:00+07:00"
}
```

*Trong đó:*
- `updateType` có các giá trị:
  - `STATUS`: Có sự thay đổi về trạng thái ONLINE/OFFLINE của trạm.
  - `DETECTION`: Phát hiện loài động vật mới (có cảnh báo).
  - `SNAPSHOT`: Ảnh chụp snapshot vừa được cập nhật mới.

---




## 6. Nhóm 4 — Kiểm thử thiết bị ngoại vi

### 6.1. `POST /cameras/{cameraId}/devices/{deviceKey}/test`

Test audio / LED / loa. Mapping: nút "Nghe thử (Test Audio)" tại màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]`.

**Ví dụ:**
```
POST /cameras/cam-001/devices/deterrent_audio/test
```

**Request body**
```json
{
  "sampleId": "A_gunshot",
  "durationSeconds": 3
}
```

**Response 202** (Accepted — lệnh đã gửi xuống field, kết quả qua push event `DEVICE_TEST_COMPLETED`)
```json
{ "testId": "t-991", "queuedAt": "..." }
```

---

## 7. Nhóm 5 — Preset & Thiết lập chung

### 7.1. `GET /control/presets`

Danh sách preset mẫu. Mapping: màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]` (nhóm nút chọn kịch bản phòng vệ mẫu).

**Response 200**
```json
{
  "items": [
    {
      "id": "intruder",
      "displayName": "Người lạ đột nhập",
      "description": "Bật đèn LED nhấp nháy đỏ-trắng + phát còi báo động tiếng súng + báo Biên phòng/Kiểm lâm.",
      "config": "@DefendAction"
    },
    {
      "id": "medium_danger",
      "displayName": "Thú vừa",
      "description": "Bật âm thanh xua đuổi tần số siêu âm/chó sủa + đèn LED nhấp nháy + hàng rào điện nhẹ (áp dụng cho loài ít nguy hiểm).",
      "config": "@DefendAction"
    },
    {
      "id": "critical_danger",
      "displayName": "Thú cực kỳ nguy hiểm",
      "description": "Silent Alert - Không phát còi/đèn tại chỗ; chỉ gửi Push/SMS khẩn cấp cho người dân di tản và báo Kiểm lâm.",
      "config": "@DefendAction"
    }
  ]
}
```

---

### 7.2. `GET /audio-samples`

Lấy danh sách các file âm thanh xua đuổi và các mẫu phát loa thông báo được nạp sẵn trong phần cứng thiết bị. Mapping: dropdown `sound_type_dropdown` và `speaker_message_dropdown` tại màn hình `[SPECIES_CONFIG_DETAIL_SCREEN]`.

**Response 200**
```json
{
  "animalDeterrentSounds": [
    { "id": "A_gunshot",     "displayName": "Tiếng súng" },
    { "id": "A_growl",       "displayName": "Tiếng gầm" },
    { "id": "A_dog_bark",    "displayName": "Tiếng chó sủa lớn" },
    { "id": "A_explosion",   "displayName": "Tiếng nổ giả lập" },
    { "id": "A_ultrasonic",  "displayName": "Tần số siêu âm", "frequencyHz": 22000 }
  ],
  "citizenAlertSounds": [
    { "id": "N_warning_voi",  "displayName": "Mẫu 1 - Cảnh báo voi hoang dã" },
    { "id": "N_warning_thu",  "displayName": "Mẫu 2 - Phát hiện thú dữ xâm lấn" },
    { "id": "N_warning_tian", "displayName": "Mẫu 3 - Chỉ dẫn di tản lánh nạn" }
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
    {
      "id": "crocodile",
      "displayName": "Cá sấu",
      "dangerLevel": "HIGH",
      "aggressionLevel": 8,
      "htmlDescription": "Loài bò sát lớn ăn thịt hoang dã, hoạt động chủ yếu ở <b>vùng nước ngọt lợ ven rừng</b>.<br/>Có tập tính săn mồi phục kích âm thầm ban đêm, lực cắn cực mạnh nguy hiểm cho con người và gia súc sát bờ nước.",
      "defense": {
        "recommend": "Kịch bản ngăn chặn bằng hàng rào điện sinh học mức trung bình kết hợp đèn LED chớp nháy màu đỏ-trắng.",
        "appSetting": "@DefendAction"
      }
    },
    {
      "id": "deer",
      "displayName": "Nai",
      "dangerLevel": "LOW",
      "aggressionLevel": 2,
      "htmlDescription": "Thú ăn cỏ lành tính, nhút nhát, thường đi kiếm ăn theo bầy nhỏ vào lúc <i>bình minh</i> hoặc <i>hoàng hôn</i>.<br/>Thích ăn chồi non, dễ bị giật mình bởi tiếng động lạ và có xu hướng bỏ chạy nhanh.",
      "defense": {
        "recommend": "Kịch bản xua đuổi thân thiện bằng sóng âm tần số thấp / siêu âm xua đuổi nhẹ nhàng, không gây hại hàng rào.",
        "appSetting": "@DefendAction"
      }
    },
    {
      "id": "elephant",
      "displayName": "Voi",
      "dangerLevel": "CRITICAL",
      "aggressionLevel": 9,
      "htmlDescription": "Thú lớn di chuyển theo đàn gia đình (5-15 cá thể), sức tàn phá hoa màu <u>rất cao</u> khi xung đột đất đai.<br/>Có khứu giác và thính giác cực nhạy, dễ nổi giận tấn công nếu bị kích động bởi ánh sáng chớp mạnh hoặc tiếng ồn lớn.",
      "defense": {
        "recommend": "Kịch bản cảnh báo âm thầm (Silent Alert) gửi SMS/Push tức thì cho người dân và kiểm lâm, tuyệt đối không dùng loa/đèn chớp tại hiện trường tránh kích động voi dữ phá hoại.",
        "appSetting": "@DefendAction"
      }
    },
    {
      "id": "giraffe",
      "displayName": "Hươu cao cổ",
      "dangerLevel": "LOW",
      "aggressionLevel": 1,
      "htmlDescription": "Động vật ăn lá cây trên cao, hiền lành, di chuyển chậm rãi và hầu như không gây ra xung đột hay phá hoại hoa màu.<br/>Hoạt động ban ngày, không có xu hướng tiếp cận khu dân cư.",
      "defense": {
        "recommend": "Loài lành tính, chỉ cần ghi nhận log và bật đèn cảnh báo nhẹ nếu di chuyển sát hàng rào.",
        "appSetting": "@DefendAction"
      }
    },
    {
      "id": "leopard",
      "displayName": "Báo",
      "dangerLevel": "CRITICAL",
      "aggressionLevel": 9,
      "htmlDescription": "Mèo lớn ăn thịt nguy hiểm, kỹ năng <b>leo trèo và ngụy trang</b> bậc thầy.<br/>Thường đi săn đơn độc về đêm, cực kỳ nhạy bén và có thể tấn công bất ngờ từ trên cao nếu cảm thấy bị đe dọa.",
      "defense": {
        "recommend": "Cảnh báo âm thầm: Gửi thông báo đẩy khẩn cấp cho kiểm lâm và người dân lân cận di tản, không kích hoạt loa còi tại chỗ.",
        "appSetting": "@DefendAction"
      }
    },
    {
      "id": "monkey",
      "displayName": "Khỉ",
      "dangerLevel": "LOW",
      "aggressionLevel": 4,
      "htmlDescription": "Động vật linh trưởng thông minh, sống theo đàn lớn.<br/>Rất nghịch ngợm, thường xuyên phá hoại nông sản, không sợ người và có khả năng leo trèo vượt qua các loại hàng rào thô sơ.",
      "defense": {
        "recommend": "Kịch bản xua đuổi chủ động: Sử dụng đèn LED chớp nhấp nháy ngẫu nhiên kết hợp tiếng chó sủa giả lập tần suất cao.",
        "appSetting": "@DefendAction"
      }
    },
    {
      "id": "rhino",
      "displayName": "Tê giác",
      "dangerLevel": "CRITICAL",
      "aggressionLevel": 8,
      "htmlDescription": "Động vật ăn cỏ cỡ lớn cực kỳ quý hiếm đang bị đe dọa tuyệt chủng.<br/>Thị lực <i>rất kém</i> nhưng thính giác và khứu giác nhạy bén; có thể lao vào tấn công điên cuồng nếu giật mình phát hiện vật lạ gần.",
      "defense": {
        "recommend": "Cảnh báo bảo vệ: Gửi tin nhắn khẩn cho Hạt Kiểm lâm triển khai bảo vệ tê giác, hạn chế các xung kích âm thanh đèn báo tại chỗ.",
        "appSetting": "@DefendAction"
      }
    },
    {
      "id": "snake",
      "displayName": "Rắn",
      "dangerLevel": "HIGH",
      "aggressionLevel": 7,
      "htmlDescription": "Bao gồm các loài rắn độc nguy hiểm bò sát sát đất. Ngụy trang tốt trong bụi rậm hoặc đống lá khô.<br/>Thường ẩn nấp gần chuồng trại kiếm mồi và sẽ cắn tự vệ nếu con người vô tình giẫm phải.",
      "defense": {
        "recommend": "Cảnh báo đề phòng: Tự động gửi SMS cảnh báo đề phòng có độc cho các hộ dân lân cận xung quanh khu vực phát hiện.",
        "appSetting": "@DefendAction"
      }
    },
    {
      "id": "tiger",
      "displayName": "Hổ",
      "dangerLevel": "CRITICAL",
      "aggressionLevel": 10,
      "htmlDescription": "Thú ăn thịt đầu bảng, chúa tể rừng xanh cực kỳ hung dữ và nguy hiểm.<br/>Có tập tính lãnh thổ cao, đi săn đơn độc vào <u>ban đêm và rạng sáng</u>, có thể tấn công trực diện con người nếu phát hiện xâm nhập sâu.",
      "defense": {
        "recommend": "Nguy cấp - Cảnh báo âm thầm: Gửi thông báo đẩy và SMS khẩn cấp tức khắc cho kiểm lâm và toàn bộ người dân lân cận di tản lánh nạn, cấm tuyệt đối phát còi báo động tại trạm.",
        "appSetting": "@DefendAction"
      }
    }
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
"@DefendAction"
```

**Response 200**
```json
{
  "cameraId": "cam-001",
  "speciesId": "elephant",
  "config": "@DefendAction",
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

**Response 200**

```json
"@DefendAction"
```

---

### 8.6. `GET /response-configs/{cameraId}` *(helper)*

Lấy tất cả cấu hình của 1 camera (cho các loài). Mapping: màn hình `[SPECIES_CONFIG_LIST_SCREEN]` (hiển thị danh sách các cấu hình của từng loài).

**Response 200**
```json
{
  "cameraId": "cam-001",
  "configs": [
    { "speciesId": "elephant", "config": "@DefendAction", "updatedAt":"..." },
    { "speciesId": "tiger",    "config": "@DefendAction", "updatedAt":"..." }
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
  "role": "CITIZEN"
}
```

---

### 9.2. `PATCH /users/me`

> ⚠️ **GHI CHÚ:** ĐÂY LÀ API DÙNG ĐỂ TEST (DEVELOP)

Cập nhật thông tin tài khoản.

**Request body** (partial)
```json
{
  "fullName": "Nguyễn Văn A"
}
```

**Response 200** → trả về user mới.




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
      "detections": [
        { "speciesId": "elephant", "speciesName": "Voi", "confidence": 0.92, "dangerLevel": "CRITICAL" }
      ],
      "resolvedDangerLevel": "CRITICAL",
      "firstDetectedAt": "2026-07-16T09:04:12+07:00",
      "resolvedAt": "2026-07-16T09:18:45+07:00",
      "responseMode": "SILENT_ALERT",            // SILENT_ALERT / ACTIVE_DETERRENCE
      "snapshotThumbnails": ["..."]
    }
  ],
  "pagination": { "page": 0, "size": 20, "total": 312 }
}
```

### 10.2. `GET /stats/summary`

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

### 11.1. `GET /alerts/feed`

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

**Validate nghiệp vụ**
- Kiểm tra số lượng người nhận SMS hiện tại của user đăng nhập. Nếu số lượng đã đạt tối đa 3 số điện thoại, hệ thống chặn lại và trả về lỗi `400 ERR_MAX_RECIPIENTS_REACHED`.

**Lỗi hay gặp**
- `400 ERR_MAX_RECIPIENTS_REACHED` → "Đã đạt giới hạn đăng ký tối đa 3 số điện thoại nhận cảnh báo."
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

> ⚠️ **GHI CHÚ:** ĐÂY LÀ API DÙNG ĐỂ TEST (DEVELOP)

**Response 200**
```json
{ "status":"UP", "uptimeSeconds":98765, "aiServerReachable":true }
```

### 13.2. `GET /reference-data/danger-levels`

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
| `detections` | string (JSON) | Có | Chuỗi JSON chứa danh sách con vật phát hiện (ví dụ: `[{"speciesId":"elephant","confidence":0.92},{"speciesId":"elephant","confidence":0.70}]`) |

**Response 201 (Created)**

Trả về cấu hình phòng vệ cụ thể cần được thực hiện tại hiện trường.

```json
{
  "eventId": "evt-456",
  "cameraId": "cam-001",
  "detections": [
    { "speciesId": "elephant", "confidence": 0.92 },
    { "speciesId": "elephant", "confidence": 0.70 }
  ],
  "resolvedDangerLevel": "CRITICAL",
  "imageUrl": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-12.jpg",
  "detectedAt": "2026-07-19T04:55:00+07:00",
  "responseAction": "@DefendAction"
}
```

**Side effects**
- Lưu trữ ảnh snapshot vào CDN / Storage của Mobile Server.
- Ghi nhận nhật ký sự kiện (`event`) vào DB PostgreSQL.
- Bắn Push Notification và SMS khẩn cấp đến danh sách SĐT đăng ký nhận cảnh báo nếu cấu hình yêu cầu (`smsNotify` / `pushNotify`).
- Bắn thông báo đẩy (FCM) đến các client Android để cập nhật giao diện.

**Lỗi hay gặp**
- `400 ERR_VALIDATION_FAILED` -> File ảnh không hợp lệ hoặc thiếu tham số.
- `404 ERR_CAMERA_NOT_FOUND` -> `cameraId` không tồn tại.
- `404 ERR_USER_NOT_FOUND` -> `userId` không hợp lệ.

---

### 13a.2. `WS /ws/cameras`

Kết nối WebSocket song công (duplex connection) từ AI Server / Camera hiện trường lên Mobile Server nhằm duy trì kênh đẩy lệnh điều khiển thời gian thực (Realtime Command Downlink).

**Query Parameters**

| Field | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `cameraId` | string | Có | ID của trạm camera kết nối (ví dụ: `cam-001`) |
| `token` | string | Có | Access token dùng để xác thực thiết bị trạm |

**Downlink Messages (Server gửi cho Client)**

Khi người dùng từ Mobile App kích hoạt kiểm thử loa/còi (`test`), Mobile Server sẽ gửi chủ động bản tin JSON thông báo qua kết nối WebSocket này đến trạm để thực thi tại chỗ:

*   **Bản tin điều khiển thiết bị (`DEVICE_COMMAND`):**
```json
{
  "event": "DEVICE_COMMAND",
  "payload": {
    "commandId": "cmd-123",
    "deviceKey": "deterrent_audio | speaker", 
    "action": "TEST", // Chỉ hỗ trợ hành động TEST thử nghiệm thiết bị
    "params": {
      "volume": 80,
      "sampleId": "A_gunshot" // ID âm thanh bắt đầu bằng prefix A_ hoặc N_
    }
  }
}
```

**Uplink Messages (Client phản hồi cho Server)**

Sau khi thiết bị trạm AI Server thực thi xong lệnh vật lý tại hiện trường, nó phải gửi bản tin xác nhận trạng thái (Acknowledgment) trở lại Server:

*   **Bản tin phản hồi (`COMMAND_ACK`):**
```json
{
  "event": "COMMAND_ACK",
  "payload": {
    "commandId": "cmd-123",
    "status": "SUCCESS | FAILED",
    "error": null
  }
}
```

---

## 14. Phụ lục — Bảng tổng hợp (Phân loại theo Màn hình Mobile)

### 14.1. Màn hình đăng nhập (`[LOGIN_SCREEN]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 3.2 | POST | `/auth/login` | Đăng nhập tài khoản |

### 14.2. Màn hình đăng ký (`[REGISTER_SCREEN]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 3.1 | POST | `/auth/register` | Đăng ký tài khoản mới |

### 14.3. Tab danh sách camera (`[CAMERA_LIST_TAB]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 5.1 | GET | `/cameras` | Lấy danh sách trạm camera |
| 5.4 | GET | `/cameras/stream` | Nhận thông báo cập nhật thời gian thực qua kết nối SSE để tự động reload |

### 14.4. Tab thống kê (`[STATISTICS_TAB]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 10.5 | GET | `/stats/summary` | Tải dữ liệu tổng hợp biểu đồ và bản đồ nhiệt (heatmap) |
| 11.2 | GET | `/alerts/feed` | Lấy luồng tin tức cảnh báo liên ngành được phân luồng |

### 14.5. Tab cài đặt (`[SETTING_TAB]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 3.3 | POST | `/auth/logout` | Đăng xuất tài khoản, hủy session |
| 9.1 | GET | `/users/me` | Lấy thông tin cá nhân và thiết lập của user |
| 9.2 | PATCH | `/users/me` | Cập nhật thông tin cá nhân (CHỈ DÙNG ĐỂ TEST) |

### 14.6. Màn hình chi tiết camera (`[CAMERA_VIEW_SCREEN]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 5.2 | GET | `/cameras/{cameraId}` | Lấy chi tiết camera, ảnh snapshot gần nhất và phán đoán nhận dạng AI |
| 5.3 | PATCH | `/cameras/{cameraId}` | Đổi tên hiển thị camera (`rename_camera_dialog`) |
| 5.4 | GET | `/cameras/stream` | Nhận thông báo cập nhật thời gian thực qua kết nối SSE để tự động reload |
| 10.1 | GET | `/events` | Lấy nhật ký sự kiện lịch sử ghi nhận của camera (`camera_log_list`) |

### 14.7. Màn hình danh sách cấu hình loài (`[SPECIES_CONFIG_LIST_SCREEN]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 8.1 | GET | `/species` | Tải danh sách loài kèm chỉ số hung dữ và đặc tính loài |
| 8.6 | GET | `/response-configs/{cam}` | Tải toàn bộ danh sách cấu hình đang áp dụng trên camera |

### 14.8. Màn hình thiết lập phòng vệ theo loài (`[SPECIES_CONFIG_DETAIL_SCREEN]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 6.1 | POST | `/cameras/{cameraId}/devices/{deviceKey}/test` | Gửi lệnh test thiết bị còi/LED tại hiện trường (`sound_test_button`) |
| 7.1 | GET | `/control/presets` | Lấy danh sách 3 kịch bản phòng vệ mẫu mặc định |
| 7.2 | GET | `/audio-samples` | Tải danh sách file âm thanh xua đuổi & mẫu phát loa (dropdowns) |
| 8.1 | GET | `/species` | Hiển thị thông tin tên loài và đặc tính đang cấu hình |
| 8.2 | PUT | `/response-configs/{cam}/{species}` | Lưu cấu hình ứng phó tự chọn (`save_config_button`) |
| 8.3 | GET | `/response-configs?cameraId=&speciesId=` | Tải cấu hình hiện có của loài để đổ lên form |
| 8.4 | DELETE | `/response-configs/{cam}/{species}` | Xóa cấu hình tự chọn của loài để quay về mặc định (`reset_config_button`) |
| 8.5 | POST | `/response-configs/{cam}/{species}/apply-preset/{id}` | Áp nhanh preset mẫu vào cấu hình loài |

### 14.9. Màn hình quản lý SĐT nhận cảnh báo (`[SMS_CONFIG_SCREEN]`)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 12.1 | GET | `/users/me/sms-recipients` | Lấy danh sách số điện thoại nhận tin nhắn cảnh báo bổ sung |
| 12.2 | POST | `/users/me/sms-recipients` | Thêm số điện thoại nhận tin nhắn mới (`add_recipient_dialog`) |
| 12.3 | DELETE | `/users/me/sms-recipients/{id}` | Xóa số điện thoại khỏi danh sách nhận tin nhắn |

### 14.10. API hỗ trợ khác (Chạy ngầm / Hệ thống / Tích hợp)

| # | Method | Endpoint | Mô tả chức năng |
|---|---|---|---|
| 4.1 | POST | `/devices/push-token` | Đăng ký token FCM nhận push notification khi đăng nhập |
| 4.2 | DELETE | `/devices/push-token` | Hủy đăng ký token FCM khi đăng xuất |
| 4.3 | GET | `/notifications/inbox` | Lấy danh sách thông báo đẩy nhận được trong app (CHỈ DÙNG ĐỂ TEST) |
| 13.1 | GET | `/health` | Kiểm tra trạng thái hoạt động của hệ thống và AI server (CHỈ DÙNG ĐỂ TEST) |
| 13.2 | GET | `/reference-data/danger-levels` | Lấy danh mục mức độ nguy hại để hỗ trợ hiển thị UI |
| 13a.1 | POST | `/cameras/{cameraId}/detections` | API tích hợp: Thiết bị hiện trường / AI Server gửi snapshot và phán đoán nhận dạng |
| 13a.2 | WS | `/ws/cameras` | Kết nối WebSocket song công để nhận lệnh điều khiển thời gian thực |

**Tổng cộng: 37 API di động + 2 API tích hợp thiết bị (gồm 1 Webhook nhận diện và 1 WebSocket điều khiển; đã bỏ các chức năng ngôn ngữ, đổi mật khẩu, khôi phục mật khẩu, stream nghe thử âm thanh, và ghi đè trạng thái thiết bị thủ công).**

---

## 15. Ghi chú triển khai

- **Versioning:** mọi breaking change phải bump `/api/v2/` — không xoá/sửa ở v1.
- **Pagination:** tất cả GET list hỗ trợ `page` + `size` với response chuẩn (items + pagination).
- **Rate limit:** 100 req / phút / IP / user (tăng lên cho endpoint test audio).
- **Concurrency:** tất cả PUT dùng `If-Match: <lastModifiedAt>` để chống ghi đè lẫn nhau giữa các thiết bị.
- **i18n:** mọi message trả về nên có key (vd `errors.auth.invalid_credentials`) — client map sang tiếng Việt. Hiện tại tài liệu Vietnamese-first.
- **Security:** SĐT cần lưu ở dạng E.164 (`+84...`). Mật khẩu cần hash ≥ bcrypt cost 12 / argon2id.
- **File này cũng** chứa **đặc tả hành vi observable** của Mobile Server: trạng thái nào cho mỗi thiết bị, payload JSON mỗi endpoint, status code, side effects. **Không** chứa đặc tả chi tiết schema DB (xem thêm `docs/Mobile_API_DB.md` ở giai đoạn sau) hay script CI/CD.
