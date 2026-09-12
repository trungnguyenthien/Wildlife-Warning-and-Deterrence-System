# 📚 Tổng Hợp Sơ Đồ Trình Tự & Giải Thích Khái Niệm Cốt Lõi

> [!NOTE]
>
> ### 💡 Diễn giải Khái niệm & Luồng vận hành (Dành cho Giám khảo / Người đọc tổng quan)
>
> Tài liệu này bao gồm **Bảng giải thích thuật ngữ cốt lõi** và các **Sơ đồ Trình tự (Sequence Diagram)** chuẩn của hệ thống.
> Người đọc có thể dễ dàng tra cứu ý nghĩa bản chất của từng công nghệ trước khi theo dõi luồng tương tác giữa các thành phần.

---

## Các Thành phần Hệ thống (Standardized Participants)

- **Mobile:** Ứng dụng di động (Android Client) cài đặt trên điện thoại người dùng và kiểm lâm để tương tác với hệ thống.
- **Server:** Máy chủ trung tâm lưu trữ dữ liệu, xử lý logic, quản lý phiên làm việc, lưu cấu hình ứng phó và giao tiếp với `Mobile` (qua REST / SSE) và `Ably` (qua REST).
- **Ably:** Dịch vụ đám mây Pub/Sub trung gian (Cloud Broker) phân phối tin nhắn thời gian thực giữa `Server` và `AI_Client` thay thế cho WebSocket trực tiếp.
- **AI_Client:** Ứng dụng trí tuệ nhân tạo nhận diện (YOLOv8) chạy tại trạm thực địa, nhận hình ảnh từ `Rasp_PI` để phân tích, gửi kết quả nhận diện lên `Server` (qua REST) và kết nối với `Ably` (qua WebSocket) để nhận lệnh điều khiển.
- **Rasp_PI:** Thiết bị trạm thực địa (Raspberry Pi) điều khiển camera chụp ảnh (chỉ gửi ảnh về `AI_Client` khi phát hiện chuyển động) và các thiết bị xua đuổi vật lý (Loa phát thanh, Đèn LED chớp, còi hú báo động).
- **FCM (Firebase Cloud Messaging):** Dịch vụ trung gian gửi thông báo đẩy (Push notification) thời gian thực đến `Mobile`. Nhận lệnh từ Máy chủ Backend và đánh thức ứng dụng di động hiển thị **Push Notification** rực sáng trên màn hình điện thoại (kể cả khi tắt/khóa màn hình).

## 📖 BẢNG GIẢI THÍCH KHÁI NIỆM & THUẬT NGỮ CỐT LÕI

| Thuật ngữ / Khái niệm                                             | Giải thích ngắn gọn dễ hiểu                                                                                                                                                                                                   | Vai trò & Giá trị trong Hệ thống                                                                                                                                                                                                                       |
| :---------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 🔑 **FCM Push Token**                                             | Nó là token do Google Firebase cấp. Dùng để định danh một **Ứng dụng di động (App) cài trên một Thiết bị (Device)** cụ thể. Thời hạn sử dụng dài hạn (đến khi gỡ app/xóa bộ nhớ).                                             | Chức năng: Đóng vai trò như _"Địa chỉ hòm thư nhận tin độc nhất"_, giúp máy chủ Backend phát thông báo đẩy khẩn cấp (Push Notification) đến đúng thiết bị di động.                                                                                     |
| 🛡️ **Access Token**                                               | Nó là token chìa khóa thông hành điện tử (mã hóa JWT) do Backend cấp khi đăng nhập. Dùng để định danh **Người dùng (`userId`), Thiết bị (`deviceId`) và Ứng dụng (`appId`)**. Thời hạn sử dụng ngắn hạn (vài giờ đến 1 ngày). | Chức năng: Đính kèm vào HTTP Header (`Authorization: Bearer <token>`) ở mọi API để xác thực quyền truy cập dữ liệu an toàn mà không cần gửi lại mật khẩu.                                                                                              |
| 🔄 **Refresh Token**                                              | Nó là token gia hạn phiên làm việc do Backend cấp song song với Access Token. Dùng để định danh **Phiên làm việc hợp lệ (User Session)** của tài khoản trên thiết bị. Thời hạn sử dụng dài hạn (vài tuần đến vài tháng).      | Chức năng: Dùng để xin Máy chủ Backend cấp lại một `Access Token` mới một cách tự động khi `Access Token` cũ hết hạn, giúp duy trì trạng thái đăng nhập liền mạch cho người dùng.                                                                      |
| 💓 **Heartbeat (Heartbeat Check)**                                | Cơ chế _"Bắt mạch"_ kiểm tra định kỳ (mỗi 5 giây) bằng gói tin siêu nhẹ.                                                                                                                                                      | Máy chủ trả về mốc thời gian cập nhật mới nhất (`lastUpdatedAt`), Mobile Client tự so sánh với mốc thời gian lưu cục bộ để quyết định có gọi lại API `GET /cameras` tải dữ liệu mới hay không $\rightarrow$ tiết kiệm pin và giảm 90% tải cho máy chủ. |
| ⏱️ **Cooldown (Thời gian chờ 30s)**                               | Cơ chế lọc thời gian thông minh giữa 2 lần phát cảnh báo liên tiếp tại cùng 1 trạm.                                                                                                                                           | Khi động vật đứng yên trước camera nhiều phút, ảnh snapshot vẫn được lưu liên tục nhưng thông báo đẩy khẩn cấp chỉ gửi **1 lần mỗi 30 giây** $\rightarrow$ chống spam thông báo cho kiểm lâm.                                                          |
| 🔒 **Băm Mật Khẩu (Password Hashing / Bcrypt)**                   | Hàm toán học một chiều biến đổi mật khẩu thành một chuỗi mã băm cố định độc nhất (_nguyên lý "Trộn màu sơn một chiều"_).                                                                                                      | Máy chủ tuyệt đối không lưu mật khẩu thô trong DB. Kẻ xấu dù đánh cắp được mã băm trong DB cũng không thể giải mã ngược lại mật khẩu gốc.                                                                                                              |
| 🌐 **WebSocket & Ably Broker**                                    | Đường truyền dữ liệu 2 chiều liên tục (như kết nối điện thoại trực tiếp) qua kênh đám mây Ably.                                                                                                                               | Giúp Máy chủ Cloud (Vercel Serverless) có thể bắn lệnh điều khiển trực tiếp xuống thiết bị thực địa (Loa/LED) tại rừng chỉ trong vài mili-giây mà không bị ngắt kết nối.                                                                               |
| 🌐 **HTTP Method (Phương thức HTTP)**                             | Các _"Động từ hành động"_ (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`) trong giao thức truyền tải Web API.                                                                                                                       | Định nghĩa rõ mục đích thao tác của Client đối với tài nguyên dữ liệu trên Máy chủ (Ví dụ: `GET` để Đọc, `POST` để Tạo mới/Gửi lệnh, `PUT`/`PATCH` để Cập nhật, `DELETE` để Xóa).                                                                      |
| 📑 **HTTP Header (Tiêu đề HTTP)**                                 | Các cặp thông tin ngữ cảnh (`Key: Value`) được đính kèm ở phần đầu gói tin trao đổi giữa Client và Server.                                                                                                                    | Truyền tải thông tin phụ quan trọng như chìa khóa xác thực (`Authorization: Bearer <token>`), định dạng dữ liệu gửi lên/nhận về (`Content-Type: application/json`), thông tin thiết bị (`User-Agent`).                                                 |
| 🚥 **HTTP Status Code (Mã trạng thái HTTP)**                      | Mã số 3 chữ số do Máy chủ trả về cho Client để báo cáo kết quả xử lý yêu cầu.                                                                                                                                                 | Giúp App di động nhận biết trạng thái kết quả: `200 OK` (Thành công), `201 Created` (Tạo mới thành công), `400 Bad Request` (Dữ liệu lỗi), `401 Unauthorized` (Sai MK / Chưa đăng nhập), `504 Gateway Timeout` (Quá giờ chờ kết nối trạm).             |
| 🔑 **Push Service Account Key (`PUSH_SERVICE_ACCOUNT_KEY_JSON`)** | Chứng chỉ dịch vụ riêng tư (`serviceAccountKey.json`) do Google Firebase phát cho Máy chủ Backend.                                                                                                                            | Cho phép Backend Server xác thực với Google Firebase API để gửi Push Notification. Để bảo mật và phù hợp Serverless, file JSON được mã hóa sang **Base64**, lưu trong biến môi trường và giải mã trực tiếp trong RAM khi dùng (không lưu file ra đĩa). |

---

### 🔑 Cấu Trúc Định Nghĩa Thống Nhất Các Loại Token Trong Hệ Thống

1. **🔑 FCM Push Token:**
   - **Bản chất:** Nó là token định danh thiết bị do dịch vụ hạ tầng Google Firebase Cloud Messaging (FCM) khởi tạo và cấp cho ứng dụng di động.
   - **Định danh:** Token này dùng để định danh duy nhất một **Ứng dụng di động (App) cài đặt trên một Thiết bị di động (Device)** cụ thể.
   - **Thời hạn sử dụng:** Thời hạn sử dụng token dài hạn (tồn tại liên tục cho đến khi gỡ ứng dụng, xóa bộ nhớ app hoặc Firebase chủ động cấp lại).
   - **Chức năng:** Chức năng của token này làm _"Địa chỉ hòm thư nhận tin độc nhất"_, giúp Máy chủ Backend gửi thông báo đẩy cảnh báo khẩn cấp (Push Notification) đến đúng thiết bị di động của người dùng.

2. **🛡️ Access Token:**
   - **Bản chất:** Nó là token chìa khóa thông hành điện tử (chuỗi mã hóa JWT) do Máy chủ Backend tạo ra và phát cho App sau khi xác thực đăng nhập thành công.
   - **Định danh:** Token này dùng để định danh đồng thời **Tài khoản người dùng (`userId`)**, **Thiết bị (`deviceId`)** và **Ứng dụng (`appId`)** đang thực hiện các yêu cầu.
   - **Thời hạn sử dụng:** Thời hạn sử dụng token ngắn hạn (vài giờ đến 1 ngày) nhằm tối ưu an toàn bảo mật và giảm thiểu rủi ro khi bị rò rỉ.
   - **Chức năng:** Chức năng của token này đính kèm vào tiêu đề HTTP Header (`Authorization: Bearer <token>`) ở mọi yêu cầu API tiếp theo để máy chủ xác thực quyền truy cập dữ liệu an toàn mà không bắt người dùng gửi lại tên đăng nhập và mật khẩu.

3. **🔄 Refresh Token:**
   - **Bản chất:** Nó là token gia hạn phiên làm việc do Máy chủ Backend cấp song song với Access Token khi đăng nhập.
   - **Định danh:** Token này dùng để định danh **Phiên làm việc hợp lệ (User Session)** của tài khoản trên thiết bị di động đã được xác thực trước đó.
   - **Thời hạn sử dụng:** Thời hạn sử dụng token dài hạn (vài tuần đến vài tháng).
   - **Chức năng:** Chức năng của token này dùng để xin Máy chủ Backend cấp lại một `Access Token` mới một cách hoàn toàn tự động ngay khi `Access Token` cũ hết hạn, giúp duy trì trạng thái đăng nhập liền mạch mà người dùng không phải gõ lại mật khẩu nhiều lần.

---

## Action 1.1: Register a new account (`POST /auth/register`)

- **Mô tả:** Người dùng nhập các thông tin đăng ký (tên đăng nhập, họ tên, số điện thoại, mật khẩu, vai trò, và email tùy chọn) để tạo tài khoản mới trong hệ thống.

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

- **Mô tả kỹ thuật:** Người dùng đăng nhập bằng tên đăng nhập và mật khẩu. Sau khi nhận accessToken từ server, Android Client lấy fcm-push-token từ FCM và tự động gửi lên server để liên kết thiết bị.

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

- **Mô tả:** Khi mở tab hoặc vào màn hình chính, app tự động gọi API lấy danh sách các trạm camera trực thuộc quyền quản lý kèm theo trạng thái hoạt động và ảnh snapshot thumbnail gần nhất để hiển thị.

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

    Note over Mobile, Server: Khởi động Mobile / Vào tab Danh sách Camera
    Mobile->>Server: GET /cameras
    activate Server
    Server->>Server: Lấy danh sách các trạm camera từ DB
    Server-->>Mobile: Response 200 OK (items)
    deactivate Server
    Mobile->>Mobile: Hiển thị danh sách trạm & ảnh thumbnail snapshot
```

---

## Action 3.1.2: Cập nhật tự động danh sách camera (`GET /cameras/heartbeat` & `GET /cameras`)

- **Mô tả:** Khi người dùng mở màn hình danh sách camera, ứng dụng thực hiện kiểm tra định kỳ trạng thái dữ liệu mới qua API `GET /cameras/heartbeat`. Nếu hệ thống có dữ liệu cập nhật mới, ứng dụng tự động gọi `GET /cameras` để tải danh sách camera mới nhất và cập nhật giao diện.

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

    Note over Mobile, Server: Người dùng đang mở màn hình Camera (Danh sách / Chi tiết)
    loop Định kỳ kiểm tra (Mỗi 5 giây)
        Mobile->>Server: GET /cameras/heartbeat
        Server-->>Mobile: Trả về lastUpdatedAt (Thời điểm cập nhật mới nhất)
        alt Có dữ liệu cập nhật mới
            Mobile->>Server: GET /cameras
            Server-->>Mobile: Trả về danh sách camera mới nhất (thumbnail, status, currentDetection)
            Mobile->>Mobile: Cập nhật giao diện và ảnh snapshot mới nhất
        else Không có thay đổi
            Note over Mobile: Giữ nguyên giao diện hiện tại
        end
    end
```

---

## Action 3.2.3: Load trend chart & movement heatmap (`GET /stats/summary`)

- **Mô tả:** Tải dữ liệu phân tích thống kê tổng hợp (tổng số lần xuất hiện, tọa độ di chuyển) để vẽ biểu đồ đường xu hướng và sơ đồ nhiệt (heatmap) phân bố động vật.

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

    Note over Mobile, Server: Tải dữ liệu tổng hợp phân tích theo trạm
    Mobile->>Server: GET /stats/summary
    activate Server
    Server->>Server: Truy vấn số lần xuất hiện, tọa độ di chuyển từ DB
    Server-->>Mobile: Response 200 OK (summary data)
    deactivate Server
    Mobile->>Mobile: Vẽ biểu đồ xu hướng (Line Chart) và sơ đồ nhiệt di chuyển (Heatmap)
```

---

## Action 3.3.1: Load & Update user profile (`GET /users/me` & `PATCH /users/me`)

- **Mô tả:** Tải thông tin tài khoản hiện tại (họ tên, vai trò, số điện thoại đăng nhập) để hiển thị lên form cài đặt chung bằng `GET /users/me`. Khi người dùng chỉnh sửa họ tên/số điện thoại, ứng dụng gửi yêu cầu `PATCH /users/me` để lưu cập nhật.

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

    Note over Mobile, Server: Chuyển sang tab Cài đặt
    Mobile->>Server: GET /users/me
    activate Server
    Server->>Server: Truy vấn hồ sơ cá nhân người dùng từ DB
    Server-->>Mobile: Response 200 OK
    deactivate Server
    Mobile->>Mobile: Đổ thông tin lên giao diện cài đặt cá nhân

    opt Người dùng chỉnh sửa thông tin cá nhân (Họ tên, SĐT)
        Mobile->>Server: PATCH /users/me (fullName, phoneNumber)
        activate Server
        Server->>Server: Cập nhật thông tin tài khoản vào DB
        Server-->>Mobile: Response 200 OK (hồ sơ mới)
        deactivate Server
        Mobile->>Mobile: Cập nhật thông tin hiển thị trên UI
    end
```

---

## Action 5.1: Load species & config statuses (`GET /species` & `GET /response-configs`)

- **Mô tả:** Tải danh sách loài động vật và trạng thái cấu hình tương ứng khi mở màn hình quản lý cấu hình loài.

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

- **Mô tả:** Khi chọn một loài để cấu hình chi tiết, app tải cấu hình phòng vệ hiện tại đang lưu trên DB, đồng thời tải danh sách 3 preset phòng vệ mẫu và danh sách âm thanh mẫu (bao gồm cả âm thanh xua đuổi `animalDeterrentSounds` và âm thanh cảnh báo qua loa `citizenAlertSounds` lấy qua `GET /audio-samples`) để phục vụ dropdown lựa chọn của người dùng. Các id âm thanh hoàn toàn lấy từ API, không hardcode trong app.

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

- **Mô tả:** Người dùng tùy biến các tham số (âm thanh, đèn LED nháy, còi báo động, mẫu phát loa, chế độ silent) hoặc chọn Preset phòng vệ mẫu (áp dụng local trên ViewModel), sau đó nhấn **Lưu cấu hình** để gửi yêu cầu cập nhật (`PUT /response-configs/{speciesId}`) lên máy chủ.

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

- **Mô tả:** Người dùng chọn loại âm thanh còi báo và nhấn "Nghe thử" để phát thử nghiệm trực tiếp tại hiện trường nhằm căn chỉnh âm lượng.

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

- **Mô tả kỹ thuật backend:**
  - `Server` nhận payload từ `AI_Client` tại `POST /cameras/{cameraId}/detections`.
  - Giải mã `PUSH_SERVICE_ACCOUNT_KEY_JSON` (Base64) trong RAM để khởi tạo Firebase Admin SDK (nếu chưa được khởi tạo).
  - `Server` truy vấn danh sách `fcm-push-token` từ bảng `device_tokens` rồi gửi Push Notification thông qua Firebase Cloud Messaging.

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

- **Mô tả kỹ thuật backend:** Công cụ kiểm thử (cURL / Postman / Integration Test Script) gửi tệp ảnh snapshot thực địa lên trạm camera qua API `POST /cameras/{cameraId}/image-upload` (truyền multipart/form-data chứa tệp ảnh JPEG/PNG ≤ 5MB và `userId`). Máy chủ tải ảnh lên Cloud Storage/Cloudinary và lưu bản ghi vào cơ sở dữ liệu.

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
