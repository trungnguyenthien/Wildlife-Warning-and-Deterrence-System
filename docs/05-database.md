# Thiết kế Cơ sở dữ liệu (Database Schema Design)

Tài liệu này mô tả chi tiết thiết kế cơ sở dữ liệu quan hệ sử dụng hệ quản trị cơ sở dữ liệu **PostgreSQL** cho dự án Hệ thống Cảnh báo và Xua đuổi Động vật hoang dã. Thiết kế này hỗ trợ đầy đủ các tính năng và API được mô tả trong [03-mobile_api.md](./03-mobile_api.md).

---

## 1. Sơ đồ thực thể mối quan hệ (ERD - Entity Relationship Diagram)

Dưới đây là sơ đồ mối quan hệ giữa các bảng dữ liệu chính trong hệ thống:

```mermaid
erDiagram
    users {
        varchar id PK
        varchar username UK
        varchar password_hash
        varchar full_name
        varchar phone_number UK
        varchar email
        varchar role
        timestamp created_at
        timestamp updated_at
    }
    sms_recipients {
        varchar id PK
        varchar user_id FK
        varchar full_name
        varchar phone_number UK
        varchar relation
        timestamp created_at
    }
    push_tokens {
        varchar id PK
        varchar user_id FK
        varchar token UK
        varchar platform
        timestamp created_at
        timestamp updated_at
    }
    cameras {
        varchar id PK
        varchar name
        double latitude
        double longitude
        varchar address
        varchar status
        varchar live_feed_url
        timestamp created_at
        timestamp updated_at
    }
    species {
        varchar id PK
        varchar display_name
        varchar danger_level
        boolean is_human
        text html_description
        integer aggression_level
        text recommend_action
        timestamp created_at
    }
    response_configs {
        varchar id PK
        varchar camera_id FK
        varchar species_id FK
        varchar last_modified_by FK
        varchar audio_sample_id
        integer audio_intensity
        varchar led_flash_rate
        varchar led_color
        integer led_duration_seconds
        varchar fence_level
        boolean fence_warning_light
        boolean fence_auto_notify
        boolean fence_auto_off_enabled
        integer fence_auto_off_minutes
        varchar speaker_sample_id
        boolean silent_alert
        timestamp updated_at
    }
    events {
        varchar id PK
        varchar camera_id FK
        varchar resolved_danger_level
        timestamp first_detected_at
        timestamp resolved_at
        varchar response_mode
        varchar snapshot_url
    }
    event_detections {
        varchar id PK
        varchar event_id FK
        varchar species_id FK
        double confidence
        timestamp detected_at
    }
    device_logs {
        varchar id PK
        varchar event_id FK
        varchar device_key
        varchar action
        timestamp action_at
        timestamp auto_off_at
    }
    alerts {
        varchar id PK
        varchar type
        varchar title
        varchar danger_level
        varchar camera_id FK
        varchar event_id FK
        timestamp created_at
    }
    alert_reads {
        varchar id PK
        varchar user_id FK
        varchar alert_id FK
        timestamp read_at
    }

    users ||--o{ sms_recipients : "quản lý"
    users ||--o{ push_tokens : "sở hữu"
    users ||--o{ alert_reads : "đã đọc"
    users ||--o{ response_configs : "cập nhật"
    cameras ||--o{ response_configs : "thiết lập tại"
    cameras ||--o{ events : "ghi nhận"
    cameras ||--o{ alerts : "kích hoạt từ"
    species ||--o{ response_configs : "áp dụng cho"
    species ||--o{ event_detections : "nhận dạng"
    events ||--o{ event_detections : "chứa"
    events ||--o{ device_logs : "lịch sử thiết bị"
    events ||--o{ alerts : "sinh ra"
    alerts ||--o{ alert_reads : "được đọc"
```

---

## 2. Chi tiết các bảng dữ liệu

### 2.1. Bảng `users` (Tài khoản người dùng)
Lưu trữ thông tin tài khoản đăng nhập của người dân, kiểm lâm, biên phòng và ban quản lý.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID người dùng (mã hex/uuid, vd: `9f3a`) |
| `username` | `VARCHAR(50)` | `NOT NULL, UNIQUE` | Tên đăng nhập |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | Mật khẩu băm (bcrypt/argon2) |
| `full_name` | `VARCHAR(100)` | `NOT NULL` | Họ và tên hiển thị |
| `phone_number` | `VARCHAR(20)` | `NOT NULL, UNIQUE` | Số điện thoại di động (E.164, vd: `+84901234567`) |
| `email` | `VARCHAR(100)` | `NULL` | Thư điện tử (tùy chọn) |
| `role` | `VARCHAR(20)` | `NOT NULL` | Enum: `CITIZEN`, `RANGER`, `BORDER_GUARD`, `HIGHWAY_ADMIN` |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Thời gian tạo tài khoản |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Thời gian cập nhật gần nhất |

*   **Các API liên quan:**
    *   [POST /auth/register](./03-mobile_api.md#31-post-authregister)
    *   [POST /auth/login](./03-mobile_api.md#32-post-authlogin)
    *   [POST /auth/logout](./03-mobile_api.md#33-post-authlogout)
    *   [GET /users/me](./03-mobile_api.md#91-get-usersme)
    *   [PATCH /users/me](./03-mobile_api.md#92-patch-usersme)

---

### 2.2. Bảng `sms_recipients` (Số điện thoại nhận SMS phụ)
Lưu danh sách SĐT nhận tin nhắn cảnh báo bổ sung do tài khoản người dùng đăng ký thêm (tối đa 3 số mỗi tài khoản).

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID bản ghi |
| `user_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `users.id` | Tài khoản sở hữu liên kết |
| `full_name` | `VARCHAR(100)` | `NOT NULL` | Tên người nhận tin nhắn |
| `phone_number` | `VARCHAR(20)` | `NOT NULL, UNIQUE` | SĐT nhận SMS (E.164) |
| `relation` | `VARCHAR(20)` | `NOT NULL` | Mối quan hệ (`self`, `family`, `neighbor`, `other`) |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm thêm số điện thoại |

*   *Ràng buộc đặc biệt:* Đảm bảo tối đa 3 bản ghi cho mỗi `user_id` (kiểm tra ở tầng Backend hoặc qua DB trigger).

*   **Các API liên quan:**
    *   [GET /users/me/sms-recipients](./03-mobile_api.md#121-get-usersmesms-recipients)
    *   [POST /users/me/sms-recipients](./03-mobile_api.md#122-post-usersmesms-recipients)
    *   [DELETE /users/me/sms-recipients/{recipientId}](./03-mobile_api.md#123-delete-usersmesms-recipientsrecipientid)

---

### 2.3. Bảng `push_tokens` (Token FCM nhận thông báo đẩy)
Lưu thông tin token FCM của thiết bị Android nhằm gửi thông báo đẩy thời gian thực.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID bản ghi |
| `user_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `users.id` | Tài khoản đăng nhập trên thiết bị |
| `token` | `VARCHAR(255)` | `NOT NULL, UNIQUE` | FCM push token từ Google Services |
| `platform` | `VARCHAR(20)` | `NOT NULL` | Hệ điều hành thiết bị (`android`) |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm đăng ký token |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm làm mới token |

*   **Các API liên quan:**
    *   [POST /devices/push-token](./03-mobile_api.md#41-post-devicespush-token)
    *   [DELETE /devices/push-token](./03-mobile_api.md#42-delete-devicespush-token)

---

### 2.4. Bảng `cameras` (Trạm camera thực địa)
Quản lý các trạm camera chụp ảnh và tích hợp thiết bị ngoại vi vật lý tại hiện trường.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID trạm camera (vd: `cam-001`) |
| `name` | `VARCHAR(100)` | `NOT NULL` | Tên gọi của trạm camera |
| `latitude` | `DOUBLE PRECISION`| `NOT NULL` | Vĩ độ tọa độ GPS |
| `longitude` | `DOUBLE PRECISION`| `NOT NULL` | Kinh độ tọa độ GPS |
| `address` | `VARCHAR(255)` | `NOT NULL` | Mô tả địa chỉ vị trí đặt trạm |
| `status` | `VARCHAR(20)` | `NOT NULL` | Enum: `ONLINE`, `OFFLINE`, `MAINTENANCE` |
| `live_feed_url` | `VARCHAR(255)` | `NOT NULL` | Link phát trực tiếp HLS (.m3u8) của camera |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm trạm được đưa vào vận hành |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm cập nhật trạng thái gần nhất |

*   **Các API liên quan:**
    *   [GET /cameras](./03-mobile_api.md#51-get-cameras)
    *   [GET /cameras/{cameraId}](./03-mobile_api.md#52-get-camerascameraid)
    *   [PATCH /cameras/{cameraId}](./03-mobile_api.md#53-patch-camerascameraid)
    *   [GET /cameras/stream](./03-mobile_api.md#54-get-camerasstream)
    *   [POST /cameras/{cameraId}/detections](./03-mobile_api.md#13a1-post-camerascameraiddetections)
    *   [WS /ws/cameras](./03-mobile_api.md#13a2-ws-wscameras)

---

### 2.5. Bảng `species` (Danh mục loài động vật)
Danh mục phân loại động vật hoang dã được thiết lập sẵn trong hệ thống để mô hình AI đối chiếu.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID loài (mã định danh tiếng Anh, vd: `elephant`) |
| `display_name` | `VARCHAR(100)` | `NOT NULL` | Tên tiếng Việt hiển thị trên UI (vd: `Voi`) |
| `danger_level` | `VARCHAR(20)` | `NOT NULL` | Enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `is_human` | `BOOLEAN` | `NOT NULL` | Đánh dấu thực thể là con người hay không |
| `html_description`| `TEXT` | `NOT NULL` | Đặc tính loài định dạng HTML hiển thị chi tiết |
| `aggression_level`| `INTEGER` | `NOT NULL` | Chỉ số hung dữ từ 1 đến 5 |
| `recommend_action`| `TEXT` | `NOT NULL` | Hướng dẫn ứng phó khuyên dùng cho người dân |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm thêm danh mục |

*   **Các API liên quan:**
    *   [GET /species](./03-mobile_api.md#81-get-species)

---

### 2.6. Bảng `response_configs` (Thiết lập kịch bản phòng vệ tùy chỉnh)
Lưu cấu hình ứng phó tự chọn cho từng cặp Camera + Loài động vật (thuộc đối tượng `DefendAction`).

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID cấu hình |
| `camera_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `cameras.id` | Áp dụng tại trạm camera nào |
| `species_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `species.id` | Áp dụng cho loài động vật nào |
| `last_modified_by`| `VARCHAR(50)` | `FOREIGN KEY` -> `users.id` | Người dùng kiểm lâm cập nhật gần nhất |
| `audio_sample_id` | `VARCHAR(50)` | `NULL` | ID âm thanh xua đuổi (`A_gunshot`, `A_growl`, ...) |
| `audio_intensity` | `INTEGER` | `NULL` | Cường độ âm thanh xua đuổi (1 - 100) |
| `led_flash_rate` | `VARCHAR(20)` | `NULL` | Tốc độ chớp led (`2_per_sec`, `4_per_sec`, `random`) |
| `led_color` | `VARCHAR(20)` | `NULL` | Màu đèn led (`red`, `white`, `red_white_alt`) |
| `led_duration_seconds`| `INTEGER` | `NULL` | Thời gian chớp led (giây) |
| `fence_level` | `VARCHAR(20)` | `NULL` | Mức dòng điện hàng rào (`low`, `medium`, `high`) |
| `fence_warning_light`| `BOOLEAN` | `NULL` | Bật đèn cảnh báo hổ phách/đỏ tại hàng rào |
| `fence_auto_notify` | `BOOLEAN` | `NULL` | Tự động bắn thông báo khi hàng rào kích hoạt |
| `fence_auto_off_enabled`| `BOOLEAN`| `NULL` | Tự động tắt hàng rào điện sinh học |
| `fence_auto_off_minutes`| `INTEGER`| `NULL` | Thời gian tự ngắt hàng rào điện (bắt buộc ≥ 2) |
| `speaker_sample_id`| `VARCHAR(50)` | `NULL` | ID mẫu phát loa cảnh báo người dân (`N_warning_voi`, ...) |
| `silent_alert` | `BOOLEAN` | `NOT NULL` | `true` => Cảnh báo âm thầm (chỉ bắn SMS/FCM, không còi/đèn) |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm cập nhật cấu hình |

*   *Khớp nối Unique:* Cặp `(camera_id, species_id)` phải là duy nhất (`UNIQUE(camera_id, species_id)`).

*   **Các API liên quan:**
    *   [GET /response-configs/{cameraId} *(helper)*](./03-mobile_api.md#86-get-response-configscameraid-helper)
    *   [GET /response-configs?cameraId=&speciesId=](./03-mobile_api.md#83-get-response-configscameraidspeciesid)
    *   [PUT /response-configs/{cameraId}/{speciesId}](./03-mobile_api.md#82-put-response-configscameraidspeciesid)
    *   [DELETE /response-configs/{cameraId}/{speciesId}](./03-mobile_api.md#84-delete-response-configscameraidspeciesid)
    *   [POST /response-configs/{cameraId}/{speciesId}/apply-preset/{presetId}](./03-mobile_api.md#85-post-response-configscameraidspeciesidapply-presetpresetid)

---

### 2.7. Bảng `events` (Nhật ký phiên sự kiện động vật xuất hiện)
Lưu thông tin phiên (session) động vật xuất hiện tại thực địa. Phiên kết thúc khi không còn ghi nhận chuyển động trong một khoảng thời gian.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID sự kiện (vd: `evt-456`) |
| `camera_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `cameras.id` | Xảy ra tại trạm camera nào |
| `resolved_danger_level`| `VARCHAR(20)`| `NOT NULL` | Cấp độ nguy hiểm tổng hợp cao nhất của sự kiện |
| `first_detected_at`| `TIMESTAMP` | `NOT NULL` | Thời điểm bắt đầu ghi nhận sự kiện |
| `resolved_at` | `TIMESTAMP` | `NULL` | Thời điểm kết thúc sự kiện (an toàn) |
| `response_mode` | `VARCHAR(20)` | `NOT NULL` | Trạng thái phòng thủ kích hoạt (`SILENT_ALERT` / `ACTIVE_DETERRENCE`) |
| `snapshot_url` | `VARCHAR(255)` | `NOT NULL` | Đường dẫn ảnh snapshot chính ghi lại loài thú |

*   **Các API liên quan:**
    *   [GET /events](./03-mobile_api.md#101-get-events)
    *   [GET /events/{eventId}/snapshots](./03-mobile_api.md#102-get-eventseventidsnapshots)
    *   [GET /stats/summary](./03-mobile_api.md#103-get-statssummary)
    *   [POST /cameras/{cameraId}/detections](./03-mobile_api.md#13a1-post-camerascameraiddetections)

---

### 2.8. Bảng `event_detections` (Chi tiết khung hình nhận diện trong phiên)
Lưu chi tiết các lần phát hiện con vật đơn lẻ từ AI Server gửi lên trong suốt quá trình phiên `events` đang diễn ra.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID bản ghi |
| `event_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `events.id` | Thuộc sự kiện phiên nào |
| `species_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `species.id` | Loài động vật nhận dạng được |
| `confidence` | `DOUBLE PRECISION`| `NOT NULL` | Độ tin cậy nhận diện của mô hình YOLOv8 (0.00 - 1.00) |
| `detected_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm ghi nhận khung hình cụ thể |

*   **Các API liên quan:**
    *   [POST /cameras/{cameraId}/detections](./03-mobile_api.md#13a1-post-camerascameraiddetections)

---

### 2.9. Bảng `device_logs` (Nhật ký kích hoạt thiết bị ngoại vi vật lý)
Lưu lịch sử bật/tắt thiết bị ngoại vi vật lý tại trạm camera trong quá trình ứng phó sự kiện.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID bản ghi |
| `event_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `events.id` | Thuộc sự kiện ứng phó nào |
| `device_key` | `VARCHAR(20)` | `NOT NULL` | Loại thiết bị vật lý ngoại vi (`DeviceKey`) |
| `action` | `VARCHAR(20)` | `NOT NULL` | Hành động kích hoạt (`ON`, `OFF`) |
| `action_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm thực hiện hành động |
| `auto_off_at` | `TIMESTAMP` | `NULL` | Thời điểm tự động tắt dự kiến (chỉ dành cho hàng rào điện) |

*   **Các API liên quan:**
    *   [POST /cameras/{cameraId}/devices/{deviceKey}/test](./03-mobile_api.md#61-post-camerascameraiddevicesdevicekeytest)
    *   [WS /ws/cameras](./03-mobile_api.md#13a2-ws-wscameras)

---

### 2.10. Bảng `alerts` (Luồng tin tức cảnh báo phân luồng)
Chứa các tin tức báo động được phân phối đến từng đối tượng dựa trên vai trò của họ.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID cảnh báo tin tức (vd: `alt-991`) |
| `type` | `VARCHAR(20)` | `NOT NULL` | Phân loại tin (`ANIMAL_RARE`, `HIGHWAY_NEARBY`, `HUMAN_BORDER`, `INTRUDER`) |
| `title` | `VARCHAR(255)` | `NOT NULL` | Tiêu đề hiển thị (vd: `Phát hiện HỔ tại Cam 2`) |
| `danger_level` | `VARCHAR(20)` | `NOT NULL` | Độ nguy cấp cảnh báo |
| `camera_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `cameras.id` | Camera ghi nhận tin tức |
| `event_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `events.id` | Liên kết đến sự kiện chi tiết gốc |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm phát tin tức |

*   **Các API liên quan:**
    *   [GET /alerts/feed](./03-mobile_api.md#111-get-alertsfeed)
    *   [POST /alerts/{alertId}/forward](./03-mobile_api.md#112-post-alertsalertidforward)

---

### 2.11. Bảng `alert_reads` (Trạng thái đã đọc tin cảnh báo)
Quản lý việc đọc tin tức độc lập của từng tài khoản người dùng, hỗ trợ tính năng làm mờ/đánh dấu đã đọc trên UI app.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `VARCHAR(50)` | `PRIMARY KEY` | ID bản ghi |
| `user_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `users.id` | Người dùng đã đọc tin |
| `alert_id` | `VARCHAR(50)` | `FOREIGN KEY` -> `alerts.id` | Tin tức đã được xem |
| `read_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm người dùng nhấn vào xem tin |

*   **Các API liên quan:**
    *   [GET /alerts/feed](./03-mobile_api.md#111-get-alertsfeed)

---

## 3. Các cơ chế nghiệp vụ đặc thù

### 3.1. Cơ chế Fallback cấu hình phòng vệ (DefendAction Fallback)
Khi trạm AI Server gửi dữ liệu nhận dạng về thông qua API `POST /cameras/{cameraId}/detections`, Mobile Server thực thi truy vấn để lấy cấu hình phòng vệ:
1.  **Bước 1 (Truy vấn Cấu hình Tùy chỉnh):** Tìm bản ghi trong bảng `response_configs` có `camera_id = :current_camera_id` and `species_id = :detected_species_id`.
2.  **Bước 2 (Fallback về cấu hình mặc định):** Nếu không tìm thấy cấu hình riêng biệt ở Bước 1, Mobile Server sẽ dựa trên trường `danger_level` của loài động vật đó trong bảng `species` để map sang kịch bản phòng vệ mẫu mặc định của trạm:
    *   `CRITICAL` -> Áp dụng cấu hình mặc định của Preset `critical_danger` (Cảnh báo âm thầm).
    *   `HIGH` / `MEDIUM` -> Áp dụng cấu hình mặc định của Preset `medium_danger` (Xua đuổi nhẹ).
    *   `LOW` -> Áp dụng cấu hình mặc định của Preset `intruder` hoặc cấu hình an toàn tối thiểu.

### 3.2. Giới hạn 3 số điện thoại nhận SMS
Mỗi tài khoản `users` chỉ được phép quản lý tối đa 3 số điện thoại nhận SMS trong bảng `sms_recipients`. 
*   **Giải pháp DB Trigger (PostgreSQL PL/pgSQL):**
    ```sql
    CREATE OR REPLACE FUNCTION check_max_sms_recipients()
    RETURNS TRIGGER AS $$
    BEGIN
        IF (SELECT COUNT(*) FROM sms_recipients WHERE user_id = NEW.user_id) >= 3 THEN
            RAISE EXCEPTION 'ERR_MAX_RECIPIENTS_REACHED' USING DETAIL = 'Đã đạt giới hạn đăng ký tối đa 3 số điện thoại nhận cảnh báo.';
        END IF;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;

    CREATE TRIGGER trigger_limit_sms_recipients
    BEFORE INSERT ON sms_recipients
    FOR EACH ROW EXECUTE FUNCTION check_max_sms_recipients();
    ```
