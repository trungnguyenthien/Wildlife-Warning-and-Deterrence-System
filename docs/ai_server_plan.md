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
    2.  AI Server đăng ký (Subscribe) kênh lắng nghe điều khiển `camera:control:{cameraId}` với tên sự kiện `DEVICE_COMMAND`.
    3.  Khi Kiểm lâm bấm "Test" thiết bị trên ứng dụng di động, Mobile Server sẽ gọi REST API của Ably để publish thông điệp điều khiển.
    4.  AI Server nhận được thông điệp thời gian thực từ Ably Cloud và thực thi kích hoạt vật lý tại Camera (ví dụ: phát âm thanh thử nghiệm).
    5.  Sau khi thực thi xong, AI Server gửi (Publish) phản hồi xác nhận trạng thái thực thi thành công/thất bại (`COMMAND_ACK`) lên kênh Ably `camera:ack:{cameraId}` để Mobile Server nhận kết quả.

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
*   **Tham chiếu đặc tả:** [03-mobile_api.md: POST /cameras/{cameraId}/detections](./03-mobile_api.md#13a1-post-camerascameraphonedetections)
*   **Phương thức:** `POST`
*   **Content-Type:** `multipart/form-data`
*   **Tham số gửi đi (Form-data):**
    *   `image`: File ảnh chụp dạng nhị phân (Binary).
    *   `userId`: ID người quản lý trạm camera (mã hex 4 ký tự, ví dụ: `u_rg`).
    *   `detections`: Chuỗi JSON danh sách con vật phát hiện, định dạng: `[{"speciesId":"elephant","confidence":0.92}]`.
*   **Kết quả phản hồi (201 Created JSON):**
    ```json
    {
      "eventId": "evt-456",
      "responseAction": {
        "ledFlash": true,
        "ledColor": "STROBE",
        "ledIntensity": 100,
        "speakerWarn": true,
        "audioSampleId": "A_gunshot",
        "audioIntensity": 80,
        "silentAlert": false
      }
    }
    ```

### 2.2. Kênh truyền tin thời gian thực qua Ably (Pub/Sub Channels)
*   **Tham chiếu tài liệu hướng dẫn Ably:** [huong-dan-ably.md](./huong-dan-ably.md)
*   **Cơ chế hoạt động:** 
    *   AI Server (trạm camera) kết nối với Ably Cloud, lắng nghe sự kiện trên kênh điều khiển của riêng trạm đó.
    *   Tất cả các bản tin truyền tải dưới dạng JSON qua giao thức WebSocket của Ably.
*   **Kênh nhận lệnh điều khiển (Control Channel):**
    *   *Tên kênh:* `camera:control:{cameraId}`
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
    *   *Tên kênh:* `camera:ack:{cameraId}`
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
    *   *Tùy chọn giá trị hợp lệ (Option Values):*
        *   `STROBE`: Nhấp nháy liên tục cường độ cao (mặc định cho mức CRITICAL).
        *   `RED`: Ánh sáng đỏ cảnh báo nguy hiểm mạnh (mức HIGH).
        *   `YELLOW`: Ánh sáng vàng cảnh báo (mức MEDIUM).
        *   `WHITE`: Ánh sáng trắng công suất cao xua đuổi.
        *   `red_white_alt`: Đỏ và trắng chớp nháy xen kẽ (Custom configuration).
        *   `null`: Không chỉ định màu.
*   `ledFlashRate`: Tần suất nhấp nháy của LED.
    *   *Tùy chọn giá trị hợp lệ:*
        *   `2_per_sec`: Nháy 2 lần mỗi giây (tiêu chuẩn).
        *   `4_per_sec`: Nháy nhanh 4 lần mỗi giây (gây kích động thị giác thú mạnh).
        *   `random`: Nháy ngẫu nhiên để động vật không làm quen được tần số.
*   `ledIntensity`: Cường độ sáng của đèn LED.
    *   *Tùy chọn giá trị hợp lệ:* Số nguyên từ `0` đến `100` (đại diện cho tỷ lệ phần trăm % công suất bóng LED).
*   `ledDurationSeconds`: Thời gian duy trì chu kỳ chớp LED.
    *   *Tùy chọn giá trị hợp lệ:* Số nguyên đại diện cho số giây, giới hạn từ `10` đến `120` giây.

#### 2. Hệ thống Loa phát thanh xua đuổi
*   `audioSampleId` (hoặc `speakerSampleId` đối với loa cảnh báo dân cư vùng lân cận): ID của tệp tin âm thanh mẫu được nạp sẵn trong thẻ nhớ local của trạm Camera.
    *   *Tùy chọn giá trị hợp lệ cho âm thanh xua đuổi động vật (bắt đầu bằng tiền tố `A_`):*
        *   `A_gunshot`: Tiếng súng nổ đanh (hiệu quả cao với thú lớn nguy hiểm).
        *   `A_growl`: Tiếng gầm rú của mãnh thú (hổ, báo).
        *   `A_dog_bark`: Tiếng đàn chó săn sủa dữ dội.
        *   `A_explosion`: Tiếng nổ lớn phá hủy.
        *   `A_ultrasonic`: Phát sóng siêu âm tần số cao (khiến màng nhĩ thú khó chịu nhưng tai người không nghe thấy).
    *   *Tùy chọn giá trị hợp lệ cho âm thanh cảnh báo cư dân (bắt đầu bằng tiền tố `N_`):*
        *   `N_warning_thu`: Loa phát thanh giọng nói cảnh báo phát hiện động vật hoang dã xâm nhập.
        *   `N_siren`: Tiếng còi hú cảnh sát/cứu hỏa khẩn cấp.
*   `audioIntensity` (hoặc `volume`): Cường độ âm lượng phát ra loa.
    *   *Tùy chọn giá trị hợp lệ:* Số nguyên từ `0` đến `100` (đại diện cho tỷ lệ phần trăm % công suất tối đa của âm ly).


