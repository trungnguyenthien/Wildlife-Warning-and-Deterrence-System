---
name: ui-api-database-review
description: Rà soát tính khớp nối và đồng bộ giữa Đặc tả giao diện (02), Đặc tả API (03), Sơ đồ trình tự (04) và Thiết kế Database (05).
---

# Kỹ năng Rà soát UI - API - Database - Sơ đồ trình tự

Kỹ năng này giúp AI Agent thực hiện rà soát chéo giữa 4 tài liệu kỹ thuật cốt lõi của dự án để đảm bảo tính đồng bộ, nhất quán và không có tính năng/API/trường dữ liệu nào bị dư thừa hoặc thiếu sót.

## Quy trình rà soát

### 1. Rà soát UI (Tài liệu 02) vs Sơ đồ trình tự (Tài liệu 04)
*   **Mục tiêu:** Đảm bảo tất cả các màn hình, tab và các nút bấm kích hoạt hành động nghiệp vụ trong `docs/02-dac-ta-man-hinh-android-app.md` đều được vẽ sơ đồ trình tự hoặc mô tả Action tương ứng trong `docs/04-sequence-diagram.md`.
*   **Hành động:**
    *   Liệt kê danh sách màn hình từ tệp `02`.
    *   Tìm kiếm mã ID màn hình (ví dụ: `[LOGIN_SCREEN]`, `[SETTING_TAB]`) trong tệp `04`.
    *   Xác minh các nút bấm quan trọng (ví dụ: `rename_camera_button`, `logout_button`, `sound_test_button`) có Action tương ứng trong `04` không.

### 2. Rà soát API (Tài liệu 03) vs Action (Tài liệu 04)
*   **Mục tiêu:** Phát hiện các API được thiết kế trong `docs/03-mobile_api.md` nhưng không xuất hiện trong các Action của sơ đồ trình tự `docs/04-sequence-diagram.md` (có khả năng dư thừa hoặc thiếu sơ đồ).
*   **Hành động:**
    *   Trích xuất tất cả các endpoint và method từ bảng tổng hợp ở phụ lục tệp `03`.
    *   Tìm kiếm sự xuất hiện của các endpoint này trong tệp `04`.
    *   Phân loại các API chưa được sử dụng thành:
        *   API chạy ngầm/hỗ trợ kỹ thuật (ví dụ: `/health`).
        *   API có chức năng trên UI nhưng chưa được vẽ sơ đồ trình tự (cần cảnh báo).

### 3. Rà soát Database (Tài liệu 05) vs API (Tài liệu 03)
*   **Mục tiêu:** Kiểm tra xem cấu trúc bảng trong `docs/05-database.md` có dư thừa trường (vi phạm chuẩn hóa) hoặc thiếu trường so với payload Request/Response của API trong `03` không.
*   **Hành động:**
    *   Đối chiếu dữ liệu trả về của các API với các cột tương ứng trong bảng.
    *   Phát hiện các trường lưu trữ tính toán động (ví dụ: trạng thái online/offline có thể suy ra từ sự kiện) hoặc các trường dư thừa có thể join từ bảng khác (ví dụ: `danger_level` trong bảng con có thể lấy từ bảng danh mục cha).

### 4. Rà soát API (Tài liệu 03) vs UI (Tài liệu 02)
*   **Mục tiêu:** Đảm bảo mọi API nghiệp vụ trong `docs/03-mobile_api.md` (ngoại trừ các API tích hợp phần cứng/AI Server hoặc các API chạy ngầm hệ thống) đều được kích hoạt bởi một hành động người dùng hoặc hiển thị dữ liệu trên một màn hình cụ thể được mô tả trong `docs/02-dac-ta-man-hinh-android-app.md`.
*   **Hành động:**
    *   Lấy danh sách các API nghiệp vụ từ tệp `03`.
    *   Đối chiếu xem có nút bấm, danh sách, hay trường dữ liệu nào trên UI của màn hình ở tệp `02` gọi đến hoặc tiêu thụ dữ liệu của API này không.
    *   Nếu phát hiện API nghiệp vụ không liên kết với bất kỳ giao diện người dùng nào, cảnh báo và đề xuất loại bỏ hoặc bổ sung đặc tả giao diện.

