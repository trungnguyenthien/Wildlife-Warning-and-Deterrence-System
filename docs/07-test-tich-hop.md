# Kịch bản Kiểm thử Tích hợp Hệ thống (Mobile App & Trạm Camera)

Tài liệu này định nghĩa chi tiết các kịch bản kiểm thử tích hợp (Integration Test Scenarios) giữa **Ứng dụng Di động (Mobile App)** và **Thiết bị tại Trạm Camera (Camera Station)** thông qua hệ thống máy chủ trung gian. Các kịch bản được thiết kế tập trung vào kết quả trực quan mà người dùng thực địa (Kiểm lâm/Người dân) có thể trực tiếp quan sát, đánh giá và xác nhận hiệu quả hoạt động của hệ thống.

---

## 1. Thành phần tham gia kiểm thử

*   **Mobile App:** Ứng dụng Android chạy trên thiết bị di động thực tế hoặc tệp giả lập giao diện di động ([simulate_mobile_app.html](../html_tool/simulate_mobile_app.html)).
*   **Camera Station:** Hệ thống thiết bị tại trạm thực địa (bao gồm Camera chụp ảnh, Loa cảnh báo, Đèn LED chớp, Hàng rào điện) hoặc bộ giả lập thiết bị ([simulate_ai_server.html](../html_tool/simulate_ai_server.html)).
*   **Mobile Server:** Máy chủ trung tâm điều phối cơ sở dữ liệu, quản lý session và xử lý kịch bản ứng phó.
*   **Ably Cloud Broker:** Kênh truyền tin thời gian thực truyền lệnh điều khiển và phản hồi ACK giữa Mobile Server và Camera Station.

---

## I. CÁC TÍNH NĂNG CHÍNH (MAIN FEATURES)

### Kịch bản 1.1: Phát hiện Động vật & Cảnh báo đẩy thời gian thực
*   **Mục đích:** Xác minh khi camera phát hiện động vật hoang dã xâm nhập, thông tin cảnh báo lập tức được đẩy về điện thoại di động và hiển thị trực quan.
*   **Các bước thực hiện:**
    1.  Khởi động ứng dụng di động, đăng nhập bằng tài khoản kiểm lâm (`ranger_demo`). Đảm bảo điện thoại có kết nối mạng và app đang hiển thị màn hình danh sách camera (`[CAMERA_LIST_TAB]`).
    2.  Tại Trạm Camera (hoặc bộ giả lập AI Server), thực hiện gửi một gói tin phát hiện động vật nguy hiểm:
        *   Loài: **elephant** (Voi)
        *   Độ tin cậy: **92%**
        *   Hình ảnh snapshot: Chọn một ảnh chụp voi rõ nét.
    3.  Quan sát phản hồi trên điện thoại và trạm.
*   **Kết quả trực quan mong đợi:**
    *   **Trên thiết bị di động:**
        *   Nếu app đang đóng hoặc chạy nền: Xuất hiện thông báo đẩy (Push Notification) tức thời với tiêu đề: *"Phát hiện Voi"* kèm nội dung khuyến nghị di tản.
        *   Nếu app đang mở: Thẻ camera tương ứng lập tức nhấp nháy đèn viền đỏ khẩn cấp (mức cảnh báo **Cao**), hiển thị Badge cảnh báo: `⚠️ Voi · 92%`, và ảnh thumbnail được cập nhật sang ảnh voi vừa chụp.
    *   **Tại Trạm Camera:** Thiết bị ngoại vi phản ứng đúng theo cấu hình mặc định hoặc tùy chỉnh dành cho Voi (đèn LED chớp trắng mạnh `STROBE`, loa phát thanh cảnh báo).
*   **Liên kết thiết kế:**
    *   Màn hình danh sách: [02-dac-ta-man-hinh-android-app.md](./02-dac-ta-man-hinh-android-app.md#31-camera_list_tab--tab-danh-sach-camera)
    *   Luồng sự kiện: [04-sequence-diagram.md](./04-sequence-diagram.md#3-action-process-detection-webhook-from-ai-server)

---

### Kịch bản 1.2: Phản ứng Phòng vệ tự động theo cấu hình loài tại trạm
*   **Mục đích:** Xác minh khi thay đổi cấu hình phòng vệ cho một loài cụ thể trên Mobile App, Trạm Camera sẽ thực thi đúng hành vi tương ứng khi phát hiện loài đó.
*   **Các bước thực hiện:**
    1.  Trên Mobile App, truy cập **Cài đặt** -> **Thiết lập hành vi ứng phó**.
    2.  Chọn loài **deer** (Nai) để chỉnh sửa.
    3.  Chọn kịch bản **Tùy chỉnh (Custom)** và thiết lập các thông số:
        *   Màu sắc LED: **YELLOW** (Vàng)
        *   Loại âm thanh xua đuổi: **A_dog_bark** (Tiếng chó sủa)
        *   Gửi SMS: **Bật** (`silentAlert = true`)
    4.  Bấm **Lưu** cấu hình.
    5.  Tại Trạm Camera, thực hiện gửi gói tin phát hiện loài **deer** (Nai) với độ tin cậy **85%**.
*   **Kết quả trực quan mong đợi:**
    *   **Tại Trạm Camera:**
        *   Đèn LED tại chỗ chớp nháy màu **Vàng**.
        *   Loa phát thanh phát tiếng **chó sủa dữ dội**.
        *   Không kích hoạt xung điện hàng rào.
    *   **Trên thiết bị di động:**
        *   Điện thoại của kiểm lâm và người dân đã đăng ký nhận được tin nhắn SMS cảnh báo có Nai xâm hại rẫy.
        *   Trên thẻ camera hiển thị Badge màu vàng nhạt (mức cảnh báo **Trung bình**).
*   **Liên kết thiết kế:**
    *   Màn hình cấu hình chi tiết: [02-dac-ta-man-hinh-android-app.md](./02-dac-ta-man-hinh-android-app.md#6-species_config_detail_screen--thiet-lap-hanh-vi-phong-ve-theo-loai)
    *   Đặc tả API lưu cấu hình: [03-mobile_api.md](./03-mobile_api.md#84-put-response-configscameraidspeciesid)

---

### Kịch bản 1.3: Thử nghiệm Thiết bị ngoại vi từ xa (Manual Remote Testing)
*   **Mục đích:** Xác minh kiểm lâm có thể chủ động kích hoạt các thiết bị xua đuổi tại chỗ từ xa thông qua ứng dụng di động để ứng phó khẩn cấp.
*   **Các bước thực hiện:**
    1.  Trên Mobile App, nhấn vào thẻ camera bất kỳ để vào màn hình chi tiết camera (`[CAMERA_VIEW_SCREEN]`).
    2.  Bấm vào biểu tượng điều khiển thiết bị ngoại vi (hoặc màn hình cấu hình chi tiết loài, phần thông số tùy chỉnh nâng cao).
    3.  Tại phần cài đặt thử nghiệm, cấu hình:
        *   Thiết bị: **speaker** (Loa)
        *   Âm thanh nghe thử: **A_explosion** (Tiếng nổ giả lập)
        *   Cường độ (Cường độ âm lượng): **90%**
        *   Thời gian chạy thử: **5 giây**
    4.  Nhấn nút **Nghe thử (Test Audio)**.
    5.  Quan sát hành vi thực tế của loa tại trạm camera và trạng thái nút bấm trên điện thoại.
*   **Kết quả trực quan mong đợi:**
    *   **Tại Trạm Camera:** Loa phát thanh lập tức hú còi khẩn cấp với âm lượng rất lớn, tự động ngắt sau đúng **5 giây**.
    *   **Trên thiết bị di động:**
        *   Khi bấm nút: Nút chuyển sang trạng thái xoay vòng (loading/chờ).
        *   Khi trạm phản hồi thành công: App xuất hiện thông báo ngắn (Toast/Snackbar): *"Thử nghiệm thiết bị LOA thành công"*.
*   **Liên kết thiết kế:**
    *   Nút thử nghiệm: [02-dac-ta-man-hinh-android-app.md](./02-dac-ta-man-hinh-android-app.md#sound_test_button)
    *   Giao tiếp lệnh điều khiển qua Ably: [04-sequence-diagram.md](./04-sequence-diagram.md#2-action-trigger-manual-device-test-led-speaker-fence)

---

## II. CÁC TÍNH NĂNG PHỤ (SECONDARY FEATURES)

### Kịch bản 2.1: Giám sát Trạng thái Trạm thời gian thực
*   **Mục đích:** Xác minh trạng thái kết nối Online/Offline của trạm được cập nhật liên tục và hiển thị chính xác trên ứng dụng di động.
*   **Các bước thực hiện:**
    1.  Mở màn hình danh sách camera trên Mobile App. Xác nhận camera `cam-01` đang hiển thị trạng thái `🟢 Online`.
    2.  Ngắt kết nối mạng của Trạm Camera `cam-01` (hoặc tắt tệp giả lập AI Server).
    3.  Đợi khoảng **≤5 giây** (chu kỳ Auto-Polling) và quan sát giao diện.
    4.  Bật lại mạng hoặc khởi động lại trạm camera.
*   **Kết quả trực quan mong đợi:**
    *   **Khi mất kết nối:** Trạng thái trên card đổi sang màu xám `⚪ Offline`, ảnh thumbnail của camera bị làm mờ kèm biểu tượng mất kết nối.
    *   **Khi kết nối lại:** Trạng thái tự động chuyển lại sang `🟢 Online` màu xanh lá cây rực rỡ và khôi phục hiển thị ảnh snapshot sắc nét mà không cần người dùng kéo thả để refresh trang thủ công.
*   **Liên kết thiết kế:**
    *   Card trạng thái: [02-dac-ta-man-hinh-android-app.md](./02-dac-ta-man-hinh-android-app.md#camera_status_indicator)

---

### Kịch bản 2.2: Thay đổi tên hiển thị Trạm Camera
*   **Mục đích:** Xác minh kiểm lâm có thể cá nhân hóa tên gọi của trạm camera để dễ quản lý theo địa bàn hoạt động.
*   **Các bước thực hiện:**
    1.  Trên Mobile App, nhấn vào thẻ camera có tên mặc định là `camera_01` để mở `[CAMERA_VIEW_SCREEN]`.
    2.  Nhấn nút **Đổi tên (rename_camera_button)**.
    3.  Trong hộp thoại mở ra, nhập tên mới: *"Trạm Bờ Sông Đăk Bla"* rồi nhấn **Lưu**.
    4.  Quay lại màn hình danh sách camera chính.
*   **Kết quả trực quan mong đợi:**
    *   Tiêu đề màn hình chi tiết và tên hiển thị trên thẻ camera lập tức đổi sang *"Trạm Bờ Sông Đăk Bla"*.
    *   Khi có thông báo đẩy hoặc tin nhắn SMS gửi về máy điện thoại trong tương lai, tên hiển thị mới này sẽ được sử dụng thay thế cho mã ID thô ban đầu.
*   **Liên kết thiết kế:**
    *   Hộp thoại sửa tên: [02-dac-ta-man-hinh-android-app.md](./02-dac-ta-man-hinh-android-app.md#rename_camera_dialog)

---

### Kịch bản 2.3: Đăng ký & Nhận cảnh báo SMS khẩn cấp
*   **Mục đích:** Xác minh người dân vùng đệm ven rừng nhận được tin nhắn SMS cảnh báo khẩn cấp tức thời khi trạm camera phát hiện thú dữ nguy hiểm.
*   **Các bước thực hiện:**
    1.  Trên Mobile App, vào **Cài đặt** -> **Quản lý SĐT nhận cảnh báo**.
    2.  Nhấn thêm mới số điện thoại phụ của người dân: nhập tên *"Nguyễn Văn Hàng Xóm"* và số điện thoại thực tế đang kiểm thử. Nhấn **Lưu**.
    3.  Tại Trạm Camera, gửi gói tin nhận dạng loài **tiger** (Hổ) với độ tin cậy **95%**.
    4.  Đợi 5 giây và kiểm tra hộp thư đến trên điện thoại của *"Nguyễn Văn Hàng Xóm"*.
    5.  Quay lại app di động, xóa số điện thoại của *"Nguyễn Văn Hàng Xóm"* khỏi danh sách.
    6.  Gửi lại tin phát hiện Hổ lần hai và kiểm tra xem có nhận được SMS không.
*   **Kết quả trực quan mong đợi:**
    *   **Lần phát hiện 1:** Điện thoại của *"Nguyễn Văn Hàng Xóm"* nhận được tin nhắn SMS với nội dung dạng: *"[CANH BAO] Phat hien Ho tai Cua Rung Quoc Gia Yok Don luc 11:30. De nghi nguoi dan dong cua chuong trai va de phong nguy hiem."*
    *   **Lần phát hiện 2 (sau khi xóa số):** Điện thoại hoàn toàn im lặng, không nhận được thêm bất kỳ tin nhắn SMS nào.
*   **Liên kết thiết kế:**
    *   Màn SMS: [02-dac-ta-man-hinh-android-app.md](./02-dac-ta-man-hinh-android-app.md#7-sms_config_screen--quan-ly-sdt-nhan-canh-bao-sms)
    *   API SMS: [03-mobile_api.md](./03-mobile_api.md#12-nhom-10--quan-ly-so-dien-thoai-nhan-tin-khan-cap-sms)

---

### Kịch bản 2.4: Đồng bộ trạng thái đa thiết bị thời gian thực
*   **Mục đích:** Đảm bảo dữ liệu cấu hình phòng vệ và sự kiện luôn đồng nhất tuyệt đối khi nhiều kiểm lâm cùng đăng nhập và giám sát trên nhiều điện thoại khác nhau.
*   **Các bước thực hiện:**
    1.  Mở hệ thống trên 2 thiết bị khác nhau sử dụng cùng một tài khoản kiểm lâm (hoặc mở 1 điện thoại thực tế và 1 trình giả lập trên máy tính).
    2.  Trên thiết bị 1, đổi cấu hình phòng vệ của loài **monkey** (Khỉ): Đổi LED Color sang **WHITE** (Trắng). Nhấn **Lưu**.
    3.  Quan sát màn hình của thiết bị 2.
*   **Kết quả trực quan mong đợi:**
    *   Thiết bị 2 lập tức tự cập nhật màn hình cấu hình của Khỉ sang màu LED **Trắng** mà không cần người dùng thao tác quay ra ngoài hoặc reload thủ công.
*   **Liên kết thiết kế:**
    *   Cơ chế cập nhật Auto-Polling: [giai-thich-ket-noi-sse.md](./giai-thich-ket-noi-sse.md)
