# Kế hoạch Cập nhật Kiến trúc: Tích hợp Ably thay thế Raw WebSocket

Tài liệu này phác thảo kế hoạch thay đổi kiến trúc hệ thống để loại bỏ kết nối WebSocket trực tiếp (Raw WebSocket) giữa AI Server, Mobile Server và Mobile App, thay thế bằng dịch vụ Pub/Sub đám mây chuyên dụng của **Ably**. Kế hoạch này tập trung vào việc cập nhật các tài liệu thiết kế trong thư mục `docs/`.

---

## 1. Mục tiêu và Lý do thay đổi

1.  **Tính tương thích Serverless:** Mobile Server chạy trên Vercel không hỗ trợ duy trì kết nối WebSocket liên tục. Sử dụng Ably giúp máy chủ serverless chỉ cần gọi API HTTPS POST ngắn hạn để đẩy lệnh, khắc phục hoàn toàn hạn chế này.
2.  **Độ tin cậy và Khả năng mở rộng:** Ably tự động xử lý kết nối lại (Auto-reconnect), đệm tin nhắn khi mất mạng tạm thời (Buffer), và sẵn sàng phục vụ quy mô lớn mà không làm tăng tải CPU/RAM của Mobile Server.
3.  **Tách biệt mối quan tâm (Decoupling):** Loại bỏ việc Mobile Server phải quản lý trạng thái kết nối socket của từng thiết bị trong bộ nhớ.

---

## 2. Thiết kế Kênh truyền tin (Channel Design)

Để tối ưu hóa số lượng tin nhắn tính phí (tránh lãng phí do cơ chế fan-out của Ably), hệ thống sẽ sử dụng mô hình kênh truyền tin riêng biệt cho từng trạm camera:

*   **Kênh điều khiển (Control Channel):** 
    *   *Tên kênh:* `camera:control:{cameraId}`
    *   *Người gửi (Publisher):* Mobile Server (được kích hoạt khi Ranger gửi yêu cầu test thiết bị).
    *   *Người nhận (Subscriber):* AI Server (trạm camera cụ thể có mã `cameraId`).
    *   *Sự kiện (Event Name):* `DEVICE_COMMAND`
*   **Kênh xác nhận (ACK Channel):**
    *   *Tên kênh:* `camera:ack:{cameraId}`
    *   *Người gửi (Publisher):* AI Server (trạm camera phản hồi sau khi thi hành lệnh).
    *   *Người nhận (Subscriber):* Mobile Server (lắng nghe để trả kết quả về cho Ranger).
    *   *Sự kiện (Event Name):* `COMMAND_ACK`

---

## 3. Kế hoạch Cập nhật các Tài liệu trong `docs/`

Nô Tài sẽ cập nhật nội dung thiết kế kỹ thuật trong 4 tài liệu cốt lõi sau:

### 3.1. [03-mobile_api.md](../03-mobile_api.md)
*   **Xóa bỏ:** API Tích hợp 13a.2 kết nối WebSocket (`WS /ws`) của camera.
*   **Thêm mới:** API Tích hợp 13a.3 cấp quyền Ably (`GET /auth/ably-token` - Token Authentication):
    *   *Xác thực:* Bắt buộc dùng JWT Bearer Token của Ranger/Camera.
    *   *Nội dung phản hồi:* Trả về JSON chứa Ably Token Request (mã token tạm thời có hạn dùng để client kết nối an toàn mà không bị lộ API Key gốc).
*   **Cập nhật:** API Điều khiển 6.1 test thiết bị (`POST /cameras/{cameraId}/devices/{deviceKey}/test`):
    *   *Mô tả luồng xử lý:* Thay vì tìm socket trong bộ nhớ, Mobile Server sẽ khởi tạo client Ably tạm thời, publish tin nhắn vào kênh `camera:control:{cameraId}`, đồng thời subscribe vào kênh `camera:ack:{cameraId}` để chờ nhận ACK (với thời gian chờ timeout là 5 giây) trước khi trả phản hồi HTTP 200/504 về cho Mobile App.

### 3.2. [04-sequence-diagram.md](../04-sequence-diagram.md)
*   **Cập nhật cấu trúc Thiết kế Hệ thống tổng quan (System Design / Overview Architecture):**
    *   Bổ sung thành phần trung gian **Ably Cloud Broker** vào danh sách các bên tham gia (Standardized Participants).
    *   Cập nhật sơ đồ kiến trúc tổng quan Mermaid để thể hiện đường truyền kết nối của Ably thay thế cho kết nối socket song công trực tiếp.
*   **Cập nhật Mục 6.3 (Action: Test speaker sound at camera station (AI_SERVER)):**
    *   *Mô tả kết nối:* Thay thế ghi chú về việc duy trì kết nối Raw WebSocket trực tiếp thành kết nối an toàn đến hệ thống Ably Cloud bằng Token Authentication.
    *   *Trình tự gửi và phản hồi Downlink:* Cập nhật các bước gửi nhận bản tin `DEVICE_COMMAND` và `COMMAND_ACK` thông qua cơ chế Pub/Sub của các kênh Ably tương ứng (`camera:control:{cameraId}` và `camera:ack:{cameraId}`).

### 3.3. [ai_server_plan.md](../ai_server_plan.md)
*   **Thay đổi Phần 4 (Giao tiếp Downlink):**
    *   Cập nhật đặc tả thư viện sử dụng: Khuyên dùng thư viện `ably` chính thức của Python thay vì thư viện `websockets` thuần.
    *   Thay đổi kịch bản kết nối: Mô tả vòng lặp kết nối bằng khối `async with AblyRealtime` và cơ chế lắng nghe trên kênh `camera:control:{cameraId}`.

### 3.4. [ai-server-websocket.md](../ai-server-websocket.md)
*   **Đổi tên tài liệu:** Thành `docs/ai-server-ably.md` để phù hợp với công nghệ mới.
*   **Cập nhật tài liệu:**
    *   **Bổ sung phần giải thích cơ chế của Ably & Nguyên nhân sử dụng:** Phân tích lý do hạ tầng Serverless (như Vercel) không thể duy trì Stateful socket dài hạn, giải thích cơ chế Pub/Sub trung gian giúp Serverless gọi API gửi lệnh tức thì.
    *   **Bảng so sánh giá trị sử dụng:** So sánh chi tiết lợi ích giữa việc dùng Ably (tự động xử lý Reconnect/Heartbeat, đệm tin nhắn khi ngoại tuyến, khả năng scale không giới hạn) so với việc tự xây dựng và quản lý WebSocket Server thủ công (tốn tài nguyên RAM/CPU duy trì pool kết nối, phải tự viết code xử lý ping-pong và auto-reconnect phức tạp).
    *   **Nội dung đặc tả kỹ thuật:**
        *   Mô tả cấu trúc tin nhắn JSON truyền tải qua Ably (giữ nguyên định dạng payload của `DEVICE_COMMAND` và `COMMAND_ACK`).
        *   Hướng dẫn cấu hình biến môi trường và cung cấp ví dụ mã nguồn Python hoàn chỉnh sử dụng thư viện `ably`.

---

## 4. Kế hoạch Triển khai tiếp theo (Next Steps)

Sau khi Bệ Hạ duyệt qua kế hoạch cập nhật tài liệu này, Nô Tài sẽ tiến hành các bước:
1.  Cập nhật trực tiếp nội dung 4 tệp tài liệu thiết kế trên nhánh `ably`.
2.  Sau khi tài liệu được cập nhật hoàn chỉnh, chúng ta sẽ lên kế hoạch (Implementation Plan) tiếp theo để thực hiện sửa đổi mã nguồn thực tế (mã nguồn Server, mã nguồn mô phỏng, và mã nguồn Android App) theo thiết kế mới.
