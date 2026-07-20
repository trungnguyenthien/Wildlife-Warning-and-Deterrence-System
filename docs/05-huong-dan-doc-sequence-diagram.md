# Hướng dẫn đọc Sơ đồ trình tự (Sequence Diagram) cho Học sinh

Tài liệu này được viết nhằm giúp các bạn học sinh tham gia đề tài Nghiên cứu Khoa học Kỹ thuật (KHKT) hiểu rõ cách đọc, vẽ và giải thích các sơ đồ trình tự (Sequence Diagram) có trong dự án của mình (cụ thể là tệp [04-sequence-diagram.md](./04-sequence-diagram.md)).

---

## 1. Sơ đồ trình tự (Sequence Diagram) là gì?

Hãy tưởng tượng sơ đồ trình tự giống như **kịch bản của một cuộc hội thoại** giữa các nhân vật (các thành phần phần mềm và phần cứng) theo chiều thời gian trôi từ trên xuống dưới. 

Nó trả lời cho các câu hỏi:
*   Ai gửi thông điệp cho ai?
*   Gửi cái gì (dữ liệu gì)?
*   Nhận lại kết quả gì và khi nào?

---

## 2. Các thành phần tham gia (Participants / Lifelines)

Ở đỉnh sơ đồ, bạn sẽ thấy các ô hình chữ nhật đại diện cho các thành phần trong hệ thống của chúng ta:

*   `Mobile`: Ứng dụng Android trên điện thoại của người dân/kiểm lâm.
*   `Mobile_Server`: Máy chủ trung tâm (Backend) nhận lệnh, lưu cơ sở dữ liệu và điều phối hệ thống.
*   `Database`: Cơ sở dữ liệu lưu trữ thông tin tài khoản, nhật ký sự kiện, cấu hình.
*   `AI_Server`: Máy chủ trí tuệ nhân tạo nhận ảnh từ Camera, chạy YOLOv8 để nhận diện loài thú.
*   `Camera`: Thiết bị camera chụp ảnh thực địa và các thiết bị ngoại vi vật lý (loa, đèn LED, hàng rào điện).

> 📊 **Lifeline (Đường đời):** Là đường đứt nét đi thẳng từ trên xuống dưới dưới mỗi đối tượng. Thời gian trôi dần từ trên xuống dưới theo đường này.

---

## 3. Các loại mũi tên và Ký hiệu thông dụng

Trong sơ đồ trình tự Mermaid ở tệp đặc tả của chúng ta, các ký hiệu mũi tên mang ý nghĩa kỹ thuật rất cụ thể:

### a) Gửi yêu cầu (Request / Call)
*   **Ký hiệu:** `A ->> B: Lời gọi API` (Mũi tên nét liền, đầu nhọn đặc).
*   **Ý nghĩa:** Đối tượng A chủ động gửi một yêu cầu (ví dụ: gửi một HTTP Request GET/POST) đến đối tượng B và chờ B xử lý.

### b) Phản hồi kết quả (Response / Return)
*   **Ký hiệu:** `B -->> A: Response 200 OK` (Mũi tên nét đứt, đầu nhọn hở).
*   **Ý nghĩa:** Đối tượng B đã xử lý xong và gửi trả lại kết quả (dữ liệu) về cho đối tượng A.

### c) Hộp kích hoạt (Activation Bar)
*   **Ký hiệu:** `activate B` và `deactivate B`.
*   **Hình ảnh hiển thị:** Một dải hình chữ nhật đứng màu xám/trắng đè lên đường nét đứt của đối tượng B.
*   **Ý nghĩa:** Thể hiện khoảng thời gian mà đối tượng B đang bận xử lý tác vụ (chờ tính toán hoặc truy vấn cơ sở dữ liệu) trước khi trả lại kết quả.

---

## 4. Các cấu trúc nâng cao có trong dự án của chúng ta

Trong tệp đặc tả [04-sequence-diagram.md](./04-sequence-diagram.md), có một số khối lệnh đặc biệt của Mermaid mà các bạn cần lưu ý khi thuyết trình đề tài:

### a) Khối chạy song song (`par` ... `and` ... `end`)
*   **Từ khóa trong Mermaid:** 
    ```mermaid
    par Tác vụ 1
        A->>B: Request 1
    and Tác vụ 2
        A->>B: Request 2
    end
    ```
*   **Ý nghĩa:** Thể hiện việc ứng dụng di động gửi nhiều yêu cầu cùng một lúc (song song) để tiết kiệm thời gian, thay vì phải đợi yêu cầu 1 xong mới gửi yêu cầu 2.
*   **Ví dụ thực tế trong dự án:** Khi bạn mở tab Thống kê, app gửi song song 3 yêu cầu: tải biểu đồ (`GET /stats/summary`), tải tin tức (`GET /alerts/feed`), và tải danh mục độ nguy hại (`GET /reference-data/danger-levels`).

### b) Hộp ghi chú (`Note over A, B` hoặc `Note right of A`)
*   **Ý nghĩa:** Dùng để ghi chú thích giải thích ngữ cảnh, trạng thái hệ thống, hoặc một hành động vật lý xảy ra ngoài đời thực.
*   **Ví dụ thực tế:** `Note over Camera, AI_Server: Phát hiện chuyển động vật lý tại thực địa` (Camera tự động chụp ảnh khi cảm biến hồng ngoại phát hiện có thú di chuyển).

### c) Tự động đánh số (`autonumber`)
*   Hệ thống tự động đánh số các bước `1, 2, 3...` từ trên xuống để các bạn dễ dàng gọi tên các bước khi viết báo cáo hoặc thuyết trình trước hội đồng giám khảo KHKT.

### d) Vùng đóng khung tô màu (`rect rgb(...)` ... `end`)
*   Dùng để nhóm các hành động có liên quan mật thiết lại với nhau (ví dụ: nhóm toàn bộ luồng thêm số điện thoại nhận SMS vào một khung riêng để dễ nhìn).

---

## 5. Ví dụ cụ thể: Đọc luồng Nhận diện YOLO (Action II - 1.1)

Hãy cùng phân tích một đoạn sơ đồ quan trọng nhất trong đề tài của bạn:

```
Camera ->> AI_Server: Gửi hình ảnh chụp được (File Binary)
AI_Server ->> AI_Server: Phân tích bằng mô hình YOLOv8
AI_Server ->> Mobile_Server: POST /cameras/{cameraId}/detections (image, detections)
Mobile_Server -->> AI_Server: Response 201 Created (eventId, "DefendAction")
AI_Server ->> Camera: Truyền lệnh điều khiển thiết bị vật lý ("DefendAction")
```

**Cách giải thích:**
1.  **Bước 1:** Khi có chuyển động, thiết bị **Camera** chụp ảnh và gửi dữ liệu ảnh (Binary) đến trạm xử lý **AI_Server**.
2.  **Bước 2:** **AI_Server** tự thực hiện một hành động nội bộ (tự gọi chính nó) để chạy mô hình mạng nơ-ron **YOLOv8** nhằm phát hiện loài vật và độ tin cậy.
3.  **Bước 3:** **AI_Server** gửi kết quả nhận diện lên **Mobile_Server** qua API REST `POST /detections`.
4.  **Bước 4:** **Mobile_Server** lưu sự kiện vào cơ sở dữ liệu, tính toán cấp độ nguy hiểm và trả về cấu hình phòng vệ phù hợp (`@DefendAction` - ví dụ: cần bật còi hú, chớp đèn LED).
5.  **Bước 5:** **AI_Server** nhận phản hồi và ngay lập tức truyền lệnh vật lý tương ứng xuống **Camera** để kích hoạt loa/còi hú/LED chớp tại thực địa nhằm xua đuổi thú.
