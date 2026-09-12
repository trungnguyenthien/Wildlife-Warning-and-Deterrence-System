# 📚 Tổng Hợp Sơ Đồ Trình Tự (Sequence Diagrams)

---

## Action 1.1: Register a new account (`POST /auth/register`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server

    Note over Mobile, Server: Tiến trình Đăng ký tài khoản mới
    Mobile->>Server: POST /auth/register (username, fullName, phoneNumber, password, role, email?)
    activate Server

    rect rgb(238, 242, 255)
        Note over Server: Kiểm tra dữ liệu đầu vào
        alt Gửi kèm id hoặc userId từ Client
            Server-->>Mobile: Response 400 Bad Request (id_not_allowed_from_client)
        end
    end

    Server->>Server: Truy vấn DB kiểm tra tên đăng nhập / số điện thoại trùng lặp
    Server->>Server: Băm mật khẩu (Bcrypt/Argon2) & Sinh mã ID hex 4 ký tự ngẫu nhiên
    Server->>Server: Lưu bản ghi người dùng mới vào DB (mã hex 4 ký tự)
    Server-->>Mobile: Response 201 Created (Đăng ký thành công)
    deactivate Server
    Mobile->>Mobile: Hiển thị thông báo & chuyển về màn đăng nhập
```

---

## Action 2.1: Login & Register fcm-push-token (`POST /auth/login` & `POST /devices/push-token`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server
    participant FCM as FCM

    Note over Mobile, Server: Tiến trình Đăng nhập tài khoản
    Mobile->>Server: POST /auth/login (username, password)
    activate Server
    Server->>Server: Truy vấn DB lấy mật khẩu băm & thông tin người dùng
    Server->>Server: Xác thực mật khẩu
    Server->>Server: Tạo JWT Access Token & Refresh Token
    Server-->>Mobile: Response 200 OK (accessToken, refreshToken, expiresIn)
    deactivate Server

    Mobile->>Mobile: Lưu Access Token & Refresh Token bảo mật

    Note over Mobile, FCM: Tự động đăng ký fcm-push-token sau khi đăng nhập
    Mobile->>FCM: Gọi lấy fcm-push-token
    FCM-->>Mobile: fcm-push-token
    Mobile->>Server: POST /devices/push-token (fcm-push-token, deviceModel, osVersion)
    activate Server
    Server->>Server: Lưu/Cập nhật fcm-push-token liên kết với userId vào DB
    Server-->>Mobile: Response 201 Created
    deactivate Server
    Mobile->>Mobile: Chuyển hướng người dùng vào màn hình chính [MAIN_SCREEN]
```

---

## Action 3.1.1: Load camera list & initial snapshots (`GET /cameras`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server

    Note over Mobile, Server: Mở tab Danh sách Camera [CAMERA_LIST_TAB]
    Mobile->>Server: GET /cameras (Header: Authorization: Bearer <token>)
    activate Server
    Server->>Server: Truy vấn danh sách camera thuộc quyền quản lý của Ranger từ DB
    Server-->>Mobile: Response 200 OK (Danh sách camera + snapshot gần nhất)
    deactivate Server
    Mobile->>Mobile: Đổ dữ liệu trạm camera & hiển thị ảnh thumbnail snapshot lên danh sách
```

---

## Action 3.1.2: Auto-polling / Heartbeat (`GET /cameras/heartbeat`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server

    loop Định kỳ mỗi 5 giây (Polling Loop)
        Mobile->>Server: GET /cameras/heartbeat
        activate Server
        Server->>Server: Kiểm tra mốc thời gian cập nhật camera gần nhất
        alt Có dữ liệu thay đổi mới (hasChanges = true)
            Server-->>Mobile: Response 200 OK (hasChanges: true)
            Mobile->>Server: GET /cameras
            Server-->>Mobile: Response 200 OK (Danh sách camera mới nhất)
            Mobile->>Mobile: Cập nhật giao diện danh sách camera
        else Không có thay đổi (hasChanges = false)
            Server-->>Mobile: Response 200 OK (hasChanges: false)
            Note over Mobile: Giữ nguyên giao diện hiện tại
        end
        deactivate Server
    end
```

---

## Action 3.2.3: Load analytics summary & species heatmap (`GET /analytics/summary`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server

    Note over Mobile, Server: Mở màn hình Thống kê chuyên sâu
    Mobile->>Server: GET /analytics/summary?period=30d&speciesId={speciesId}&cameraId={cameraId}
    activate Server
    Server->>Server: Truy vấn tổng hợp dữ liệu xuất hiện động vật theo thời gian & tọa độ từ DB
    Server-->>Mobile: Response 200 OK (trendSeries, heatmapPoints)
    deactivate Server
    Mobile->>Mobile: Vẽ Biểu đồ xu hướng xuất hiện & Sơ đồ nhiệt di chuyển (Heatmap)
```

---

## Action 3.3.1: View & edit user profile (`GET /users/me` & `PATCH /users/me`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server

    Note over Mobile, Server: Mở tab Cá nhân [PROFILE_TAB]
    Mobile->>Server: GET /users/me
    activate Server
    Server->>Server: Lấy thông tin tài khoản người dùng hiện tại từ DB
    Server-->>Mobile: Response 200 OK (fullName, role, phoneNumber, email)
    deactivate Server
    Mobile->>Mobile: Đổ thông tin người dùng lên form hiển thị

    Note over Mobile, Server: Người dùng chỉnh sửa thông tin cá nhân và bấm "Lưu"
    Mobile->>Server: PATCH /users/me (fullName, phoneNumber)
    activate Server
    Server->>Server: Cập nhật thông tin người dùng trong DB
    Server-->>Mobile: Response 200 OK (Thông tin cập nhật)
    deactivate Server
    Mobile->>Mobile: Hiển thị thông báo cập nhật hồ sơ thành công
```

---

## Action 5.1: Load species & config statuses (`GET /species` & `GET /response-configs`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server

    Note over Mobile, Server: Người dùng mở màn hình Danh sách cấu hình loài
    Note over Mobile, Server: Gửi các yêu cầu tải dữ liệu song song
    par Tải danh sách loài động vật
        Mobile->>Server: GET /species
        activate Server
        Server->>Server: Lấy danh sách loài từ DB
        Server-->>Mobile: Response 200 OK (items)
        deactivate Server
    and Tải các cấu hình đang hoạt động của Ranger
        Mobile->>Server: GET /response-configs
        activate Server
        Server->>Server: Lấy các cấu hình phòng vệ hiện tại từ DB
        Server-->>Mobile: Response 200 OK (items)
        deactivate Server
    end
    Mobile->>Mobile: Hiển thị danh sách loài kèm trạng thái cấu hình (Đang hoạt động/Mặc định)
```

---

## Action 6.1: Load species configuration & sample lists (`GET /response-configs?speciesId=`, `GET /control/presets`, `GET /audio-samples`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server

    Note over Mobile, Server: Người dùng mở màn hình Thiết lập phòng vệ theo loài
    Note over Mobile, Server: Gửi các yêu cầu tải cấu hình & danh mục mẫu
    par Tải cấu hình phòng vệ hiện tại
        Mobile->>Server: GET /response-configs?speciesId={species}
        activate Server
        Server->>Server: Lấy cấu hình ứng phó từ DB
        Server-->>Mobile: Response 200 OK (payload)
        deactivate Server
    and Tải 3 preset phòng vệ mẫu
        Mobile->>Server: GET /control/presets
        activate Server
        Server->>Server: Lấy danh sách presets mẫu từ DB
        Server-->>Mobile: Response 200 OK (items)
        deactivate Server
    and Tải danh mục âm thanh & mẫu phát loa
        Mobile->>Server: GET /audio-samples (chứa animalDeterrentSounds + citizenAlertSounds)
        activate Server
        Server->>Server: Lấy danh sách âm thanh & mẫu phát loa từ DB
        Server-->>Mobile: Response 200 OK (items)
        deactivate Server
    end
    Note right of Mobile: citizenAlertSounds nạp từ GET /audio-samples (nguồn hard-config/alert-sound.yaml), app không hardcode id
    Mobile->>Mobile: Đổ dữ liệu lên các dropdown chọn preset, âm thanh và mẫu phát loa
```

---

## Action 6.2: Update species configuration (`PUT /response-configs/{speciesId}`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server

    Note over Mobile, Server: Người dùng tùy chỉnh thông số (hoặc chọn Preset) và bấm Lưu
    Mobile->>Server: PUT /response-configs/{speciesId} (cấu hình "@DefendAction")
    activate Server
    Server->>Server: Lưu/Cập nhật cấu hình phòng vệ vào DB
    Server-->>Mobile: Response 200 OK (cấu hình mới)
    deactivate Server
    Mobile->>Mobile: Hiển thị thông báo thành công & cập nhật giao diện
```

---

## Action 6.3: Test speaker sound at camera station (`POST /cameras/{cameraId}/devices/{deviceKey}/test`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Mobile as Mobile
    participant Server as Server
    participant Ably as Ably Broker (Cloud)
    participant AI_Client as AI_Client
    participant Rasp_PI as Rasp_PI

    Note over AI_Client, Ably: AI_Client kết nối và subscribe kênh user:control:{userId} (qua WebSocket)
    Note over Mobile, Rasp_PI: Người dùng bấm nút "Nghe thử" tại app
    Mobile->>Server: POST /cameras/{cameraId}/devices/{deviceKey}/test (intensity, durationSeconds, audioSampleId)
    activate Server
    Server->>Ably: REST: Publish DEVICE_COMMAND lên kênh user:control:{userId}
    Note over Server, Ably: (Đồng thời Server subscribe nhận ACK từ kênh user:ack:{userId})
    activate Ably
    Ably-->>AI_Client: Đẩy tin nhắn DEVICE_COMMAND qua WebSocket
    deactivate Ably
    activate AI_Client
    AI_Client->>Rasp_PI: Ra lệnh cho Loa/LED/Rào điện thực thi thử nghiệm
    activate Rasp_PI
    Rasp_PI-->>AI_Client: Phản hồi xác nhận thiết bị đã thực thi xong
    deactivate Rasp_PI
    AI_Client->>Ably: WebSocket: Publish phản hồi COMMAND_ACK lên kênh user:ack:{userId} (SUCCESS)
    deactivate AI_Client
    activate Ably
    Ably-->>Server: Đẩy tin nhắn phản hồi COMMAND_ACK
    deactivate Ably

    alt Nhận được ACK trong vòng 5 giây
        Server->>Server: Ghi nhật ký kích hoạt thử nghiệm thiết bị ngoại vi vào DB (device_logs)
        Server-->>Mobile: Response 200 OK (SUCCESS)
        Mobile->>Mobile: Hiển thị thông báo "Kích hoạt thiết bị kiểm thử thành công"
    else Quá 5 giây không nhận được ACK (Timeout)
        Server-->>Mobile: Response 504 Gateway Timeout (camera_offline)
        Mobile->>Mobile: Hiển thị thông báo lỗi "Không thể kết nối tới camera hiện trường"
    end
    deactivate Server
```

---

## Action 1.1 AI: AI Client sends detection snapshot (`POST /cameras/{cameraId}/detections`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Rasp_PI as Rasp_PI
    participant AI_Client as AI_Client
    participant Server as Server
    participant FCM as FCM (Push Notification)
    participant Mobile as Mobile

    Note over Rasp_PI, AI_Client: Phát hiện chuyển động vật lý tại thực địa
    Rasp_PI->>AI_Client: Gửi hình ảnh chụp được (File Binary)
    activate AI_Client
    AI_Client->>AI_Client: Phân tích hình ảnh bằng mô hình YOLOv8 (Nhận dạng danh sách loài, độ tin cậy)

    AI_Client->>Server: POST /cameras/{cameraId}/detections (image, detections)
    activate Server
    Server->>Server: Lưu trữ ảnh snapshot lên CDN / Cloud Storage
    Server->>Server: Ghi nhận sự kiện phát hiện động vật vào DB (events & event_detections)
    Server->>Server: Truy vấn cấu hình phòng vệ từ DB (response_configs: "@DefendAction")

    Note over Server: Kiểm tra cooldown 30s: Có Event nào từ cameraId này trong 30s vừa qua không?
    alt isNewEvent = true (Lần đầu / Đã quá 30s)
        Server->>Server: Giải mã PUSH_SERVICE_ACCOUNT_KEY_JSON (Base64) trong RAM → khởi tạo Firebase Admin SDK
        Server->>Server: Tạo Alert mới trong DB (type, title, dangerLevel, cameraId, eventId)
        Server->>Server: Truy vấn danh sách fcm-push-token từ DB (device_tokens)
        Server->>FCM: Gửi push alert (speciesName, cameraId, eventId, dangerLevel)
        FCM-->>Mobile: Hiển thị Push Notification khẩn cấp lên màn hình khóa
    else isNewEvent = false (Phát hiện liên tiếp ≤ 30s)
        Note over Server: Bỏ qua tạo Alert & gửi Push Notification để tránh spam. Snapshot đã được lưu để ứng dụng tự động cập nhật.
    end
    Server-->>Mobile: Cập nhật dữ liệu camera mới nhất cho điện thoại

    Server->>Server: Ghi nhật ký tự động kích hoạt thiết bị ngoại vi vào DB (device_logs)

    Server-->>AI_Client: Response 201/200 (eventId, detections, responseAction: "@DefendAction" phẳng 8 trường)
    deactivate Server

    AI_Client->>Rasp_PI: Truyền lệnh điều khiển thiết bị vật lý (phát audioSampleId, chớp LED theo ledFlashRate)
    deactivate AI_Client

    Note over Rasp_PI: Thực thi phòng vệ tại chỗ (Phát tệp âm thanh xua đuổi chọn lọc, chớp nháy LED)
```

---

## Action 1.2 AI: Manual snapshot upload via Backend API / Testing Tools (`POST /cameras/{cameraId}/image-upload`)

```mermaid
%%{init: {
  'theme': 'default',
  'sequence': {
    'rightAngles': true,
    'messageAlign': 'left',
    'messageMargin': 40,
    'actorMargin': 80
  },
  'themeVariables': {
    'primaryColor': '#EEF2FF',
    'primaryTextColor': '#1E1B4B',
    'primaryBorderColor': '#6366F1',
    'lineColor': '#4F46E5',
    'secondaryColor': '#F0FDFA',
    'actorBkg': '#EEF2FF',
    'actorBorder': '#4F46E5',
    'actorTextColor': '#1E1B4B',
    'signalColor': '#4F46E5',
    'signalTextColor': '#1E1B4B',
    'labelBoxBkgColor': '#F8FAFC',
    'labelBoxBorderColor': '#818CF8',
    'labelTextColor': '#0F172A',
    'loopTextColor': '#4F46E5',
    'noteBkgColor': '#FEF3C7',
    'noteTextColor': '#78350F',
    'noteBorderColor': '#F59E0B',
    'activationBkgColor': '#C7D2FE',
    'sequenceNumberColor': '#FFFFFF'
  }
}}%%
sequenceDiagram
    autonumber
    participant Client_Test as External Client (cURL / Test Script)
    participant Server as Server
    participant Cloudinary as Cloudinary / Cloud Storage

    Note over Client_Test, Server: Gửi tệp ảnh snapshot qua công cụ kiểm thử / cURL
    Client_Test->>Server: POST /cameras/{cameraId}/image-upload (form-data: image, userId)
    activate Server
    Server->>Server: Validation định dạng (JPG/PNG, size ≤ 5MB) & kiểm tra cameraId, userId
    Server->>Cloudinary: Upload tệp ảnh snapshot thực địa
    Cloudinary-->>Server: Trả về URL ảnh (secureUrl)
    Server->>Server: Lưu bản ghi snapshot mới vào DB (cameraId, userId, url, uploadedAt)
    Server-->>Client_Test: Response 201 Created (id, url, deviceId, userId, uploadedAt)
    deactivate Server
```
