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

### 1.2. Luồng thử nghiệm thiết bị thời gian thực (Downlink / WebSocket)
*   **Tham chiếu:** [04-sequence-diagram.md: Action 6.3 (Test speaker sound at camera station)](./04-sequence-diagram.md#63-action-test-speaker-sound-at-camera-station-ai_server)
*   **Quy trình xử lý:**
    1.  AI Server duy trì một kết nối WebSocket song công liên tục đến Mobile Server.
    2.  Khi Kiểm lâm bấm "Test" thiết bị trên ứng dụng di động, Mobile Server đẩy thông điệp điều khiển xuống qua WebSocket.
    3.  AI Server thực thi kích hoạt vật lý tại Camera (ví dụ: phát âm thanh thử nghiệm).
    4.  AI Server gửi phản hồi xác nhận trạng thái thực thi thành công/thất bại lên Mobile Server qua WebSocket.

---

## 2. Chi tiết Đặc tả API Tích hợp

> [!NOTE]
> **Production Server URL (Địa chỉ máy chủ chạy thực tế):**
> *   REST API Base URL: `https://wildlife-warning-and-deterrence-sys.vercel.app/api/v1`
> *   WebSocket Base URL: `wss://wildlife-warning-and-deterrence-sys.vercel.app`
> 
> AI Server khi tích hợp cần cấu hình hai địa chỉ base trên để gọi webhook và duy trì kết nối.

Dữ liệu giao tiếp được định nghĩa chi tiết tại Nhóm 12 của tài liệu [03-mobile_api.md](./03-mobile_api.md#13a-nhom-12--tich-hop-thiet-bi--ai-server).

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
        "electricFence": true,
        "electricFenceDuration": 15,
        "silentAlert": false
      }
    }
    ```

### 2.2. Kênh kết nối WebSocket song công (`WS /ws`)
*   **Tham chiếu URL thực tế (Production WS URL):** `wss://wildlife-warning-and-deterrence-sys.vercel.app/ws?userId={userId}`
*   **Tham chiếu đặc tả:** [03-mobile_api.md: WS /ws](./03-mobile_api.md#13a2-ws-ws)
*   **Endpoint:** `/ws?userId={userId}`
*   **Định dạng gói tin:** JSON.
*   **Bản tin nhận lệnh Downlink (`DEVICE_COMMAND`):**
    ```json
    {
      "event": "DEVICE_COMMAND",
      "payload": {
        "commandId": "cmd-123",
        "cameraId": "cam-001",
        "deviceKey": "deterrent_audio | speaker",
        "action": "TEST",
        "params": { "volume": 80, "sampleId": "A_gunshot" }
      }
    }
    ```
*   **Bản tin phản hồi Uplink (`COMMAND_ACK`):**
    ```json
    {
      "event": "COMMAND_ACK",
      "payload": {
        "commandId": "cmd-123",
        "cameraId": "cam-001",
        "status": "SUCCESS",
        "error": null
      }
    }
    ```

---

## 3. Bản đồ Ánh xạ Tham số Phòng vệ (`@DefendAction`)

Khi nhận được đối tượng cấu hình phòng vệ từ Mobile Server (qua phản hồi API Webhook nhận dạng hoặc qua lệnh điều khiển WebSocket), AI Server chịu trách nhiệm phân dịch các thuộc tính này để điều khiển thiết bị phần cứng tại chỗ.

Dữ liệu cấu hình được chia làm hai mức độ chi tiết tùy thuộc vào kênh truyền và cấu hình hiện tại:

### 3.1. Bản tin phản hồi Trigger nhanh (Mặc định trả về từ Webhook)
Đối với bản tin phản hồi nhanh từ API Webhook nhận dạng động vật, máy chủ trả về trạng thái kích hoạt bật/tắt (Boolean) của các phân hệ tại thực địa:

*   `ledFlash`: Kích hoạt/Vô hiệu hóa hệ thống đèn LED chớp sáng.
    *   *Giá trị:* `true` (kích hoạt chớp sáng), `false` (tắt đèn).
*   `speakerWarn`: Kích hoạt/Vô hiệu hóa hệ thống loa phát thanh cảnh báo.
    *   *Giá trị:* `true` (kích hoạt phát loa xua đuổi), `false` (tắt loa).
*   `electricFence`: Kích hoạt/Vô hiệu hóa xung dòng điện của hàng rào sinh học.
    *   *Giá trị:* `true` (kích hoạt phát xung điện), `false` (ngắt xung điện).
*   `silentAlert`: Chế độ cảnh báo im lặng.
    *   *Giá trị:* 
        *   `true`: Hệ thống chỉ ghi nhận nhật ký sự kiện, gửi SMS/Push cảnh báo về máy chủ/người dân, tuyệt đối **không kích hoạt** bất kỳ thiết bị xua đuổi vật lý nào tại chỗ (LED, Loa, Hàng rào đều tắt).
        *   `false`: Kích hoạt các thiết bị xua đuổi vật lý tại chỗ tương ứng theo cấu hình.

---

### 3.2. Thông số điều khiển chi tiết (Lấy từ Presets / Custom Config hoặc WebSocket Command)
Khi thực hiện cấu hình sâu cho Camera hoặc nhận thông điệp điều khiển chi tiết qua WebSocket, các trường thông số chi tiết dưới đây sẽ được áp dụng:

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

#### 3. Hệ thống Hàng rào điện sinh học
*   `fenceLevel`: Mức điện áp xung kích của hàng rào điện sinh học.
    *   *Tùy chọn giá trị hợp lệ:*
        *   `low`: Xung điện áp nhẹ, gây tê nhẹ (dành cho động vật nhỏ hoặc cảnh cáo).
        *   `medium`: Xung điện áp vừa, gây co cơ tức thời (dành cho thú vừa).
        *   `high`: Xung điện áp mạnh, đẩy lùi ngay lập tức (dành cho thú lớn nguy hiểm cực độ).
*   `electricFenceDuration`: Thời gian phát xung điện liên tục từ lúc kích hoạt.
    *   *Tùy chọn giá trị hợp lệ:* Số nguyên tính bằng giây, từ `5` đến `60` giây.
*   `fenceAutoOffMinutes`: Thời gian tự động ngắt hàng rào điện hoàn toàn nếu camera không phát hiện thêm chuyển động của động vật.
    *   *Tùy chọn giá trị hợp lệ:* Số nguyên tính bằng phút, bắt buộc tối thiểu là `2` phút để đảm bảo an toàn sinh mạng cho người dân và vật nuôi trong vùng.
*   `fenceWarningLight`: Bật/Tắt đèn báo động màu hổ phách chạy dọc theo hàng rào khi đang có điện.
    *   *Giá trị:* `true` (bật đèn chớp hàng rào), `false` (tắt đèn chớp hàng rào).
*   `fenceAutoNotify`: Tự động kích hoạt gửi tin nhắn SMS khẩn cấp khi hàng rào điện phát sinh dòng chạm (động vật chạm vào hàng rào).
    *   *Giá trị:* `true` (gửi SMS khẩn cấp), `false` (chỉ ghi nhận nhật ký).
