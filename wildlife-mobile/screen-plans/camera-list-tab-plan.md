# Kế hoạch Triển khai: CAMERA_LIST_TAB (Tab Danh sách Camera)

Bản kế hoạch này mô tả thiết kế và kiến trúc triển khai cho tab `[CAMERA_LIST_TAB]` (thuộc màn hình chính `[MAIN_SCREEN]`), tuân thủ các tài liệu đặc tả nghiệp vụ (02), đặc tả API (03) và sơ đồ sequence (04).

---

## 0. Thiết kế Giao diện Mockup (UI Design)

*   **Hình ảnh Thiết kế Mockup:** [screen.png](../../docs/design-screen/CAMERA_LIST_TAB/screen.png)
*   **Bản xem trước trực quan (Preview):**
    ![Thiết kế CAMERA_LIST_TAB](../../docs/design-screen/CAMERA_LIST_TAB/screen.png)

---

## 1. Thành phần Giao diện (UI Components)

Màn hình được đặt tại `ui/screens/CameraListTab.kt` và cấu thành từ các thành phần UI sau:

*   **`screen_header`**: Khung tiêu đề gồm nút back (←) và tiêu đề màn hình, đồng bộ style với MAIN_SCREEN header.
*   **`emergency_banner_container` (Khung cảnh báo khẩn cấp):** Banner hiển thị sticky ở phía trên cùng của tab khi có sự kiện cảnh báo động vật hoang dã nguy hiểm mới.
    *   `emergency_banner_analysis_text`: Text mô tả chi tiết con vật (Tên loài, độ tin cậy AI, mức độ nguy hiểm).
*   **`camera_grid_list` (Danh sách lưới camera):** Hiển thị danh sách các camera card dưới dạng Grid (1 cột).
*   **`StationCard` (Thẻ trạm camera):** Đại diện cho một trạm camera cụ thể, bao gồm:
    *   `camera_thumbnail_image`: Ảnh snapshot mới nhất chụp từ trạm có độ tin cậy AI ≥ 50%, hiển thị dạng rộng đầy đủ theo tỉ lệ camera 16:9.
    *   `offline_placeholder_state`: Trạng thái khi trạm Offline, hiển thị ảnh mờ/xám + icon camera gạch chéo ở giữa.
    *   `warning_badge_overlay`: Badge cảnh báo đỏ đè lên góc trên-trái ảnh khi có phát hiện động vật nguy hiểm (tương tự `⚠ PHÁT HIỆN VOI · 92%`).
    *   `station_status_badge`: Badge trạng thái `🟢 Online` / `🔴 Offline` đè lên góc trên-phải ảnh thumbnail (overlay trực tiếp trên ảnh).
    *   `station_name_text`: Tên của trạm camera (ví dụ: `Trạm Số 1`), hiển thị đầu tiên bên dưới ảnh (không còn chấm tròn trạng thái).
    *   `station_timestamp_text`: Hiển thị dưới tên trạm, kèm icon đồng hồ 🕐 (ví dụ: `🕐 14:30 · 27/07`). Khi Offline, đổi thành "Mất kết nối X giờ trước".
    *   `camera_location_text`: Địa chỉ/mô tả khu vực lắp đặt (ví dụ: `Rìa rừng Phía Nam`), hiển thị dưới thời gian (màu chữ xám nhạt, cỡ nhỏ hơn tên trạm).
    *   `station_action_button`: Nút hành động dạng icon tròn (ví dụ chuông) ở góc phải card, chỉ hiển thị khi `isOnline = true`.
    *   `camera_card_clickable_container`: Vùng clickable bao trùm toàn bộ card để mở màn hình chi tiết `[CAMERA_VIEW_SCREEN]`.
*   **`bottom_nav_bar`**: Thanh điều hướng 3 tab (icon + label) đồng bộ với bottom_nav_bar của CAMERA_LIST_TAB, đổi label cho phù hợp chức năng màn hình trạm.

---

## 2. API Tương tác & Luồng Dữ liệu (Retrofit & SSE Integration)

Màn hình tương tác với Mobile Server qua hai kênh chính:

1.  **REST API (Retrofit):** `GET /cameras` (Định nghĩa trong `data/CameraApi.kt`) để lấy danh sách trạm camera.
2.  **Server-Sent Events (SSE):** `GET /cameras/stream` (Định nghĩa trong `data/SseClient.kt`) để nhận ping cập nhật thời gian thực.

### Luồng xử lý thời gian thực "Ping-to-Fetch":
```mermaid
sequenceDiagram
  autonumber
  participant Mobile as Android Client (Foreground)
  participant SSE as SseClient
  participant Server as Mobile Server
  participant VM as CameraListViewModel

  Note over Mobile, Server: 1. Duy trì kết nối SSE dài hạn
  Mobile->>SSE: Khởi tạo kết nối GET /cameras/stream
  SSE->>Server: Thiết lập EventStream
  Server-->>SSE: EventStream Established (Keep-Alive)
  
  Note over Server, VM: 2. Khi phát hiện sự kiện động vật mới
  Server-->>SSE: Gửi event: camera-update {"cameraId": "cam-01", "updateType": "DETECTION"}
  SSE->>VM: Trigger callback onSseEventReceived
  
  Note over Mobile, VM: 3. Tự động tải lại danh sách chạy ngầm (Fetch)
  VM->>Server: GET /cameras (Tải lại thông tin mới nhất)
  Server-->>VM: Trả về danh sách camera (đã cập nhật status/badge/thumbnail)
  VM-->>Mobile: Cập nhật UI State (Card nhấp nháy đỏ cảnh báo)
```

---

## 3. Cấu trúc Trạng thái UI (UI State) & Event/Action

### UI State:
```kotlin
data class CameraListUiState(
    val stations: List<StationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long = 0L
)

data class StationUiModel(
    val id: String,
    val name: String,
    val address: String,
    val isOnline: Boolean,
    val thumbnailUrl: String?,
    val hasUnreadAlert: Boolean,
    val alertSpecies: String?,
    val alertConfidence: Int?,
    val timestampText: String,
    val offlineDurationText: String?
)
```

### Events / Actions:
*   `loadCameras(isSilent: Boolean = false)`: Nạp danh sách camera từ server.
*   `refreshCameras()`: Pull-to-refresh làm mới danh sách thủ công.
*   `startSseListening()`: Đăng ký lắng nghe sự kiện SSE khi Tab được hiển thị ở Foreground.
*   `stopSseListening()`: Hủy kết nối SSE khi ứng dụng chuyển xuống Background hoặc chuyển Tab.

---

## 4. Ma trận logic trạng thái thẻ (Card State Logic)

*   **Logic Cảnh báo:** Trạng thái nhấp nháy cảnh báo chỉ kích hoạt khi ảnh snapshot gần nhất có độ tin cậy AI ≥ 50% và thời gian xảy ra sự kiện dưới 30 phút. Quá 30 phút, thẻ tự động tắt cảnh báo nhấp nháy.
*   **Logic Đã xem / Chưa xem:** 
    *   *Chưa xem:* Thẻ hiển thị `warning_badge_overlay` và nhấp nháy nền đỏ (cảnh báo cao/trung bình).
    *   *Đã xem:* Ngay sau khi người dùng nhấn vào thẻ mở `[CAMERA_VIEW_SCREEN]`, trạng thái đổi sang Đã xem, thẻ sẽ tắt nhấp nháy nền nhưng giữ `warning_badge_overlay` để bảo toàn thông tin.

---

## 5. Kế hoạch Kiểm thử (Verification Plan)

### Automated Tests (Unit Tests)
*   **`CameraListViewModelTest.kt`**:
    *   `TC_UI_CAM_LOAD_SUCCESS`: Nạp danh sách camera thành công, kiểm tra map đúng định dạng dữ liệu (Online/Offline, text thời gian).
    *   `TC_UI_CAM_LOAD_FAILURE`: Lỗi kết nối API -> hiển thị thông báo lỗi trên UI State.
    *   `TC_UI_CAM_SSE_TRIGGER`: Giả lập nhận event SSE `camera-update` -> kiểm tra hàm `loadCameras` được gọi tự động chạy ngầm để kéo dữ liệu mới.
    *   `TC_UI_CAM_OFFLINE_30S`: Giả lập camera offline trên 30s -> kiểm tra UI cập nhật đúng badge xám offline.

### Manual Verification
1.  Bật/tắt kết nối mạng của thiết bị di động để kiểm tra trạng thái hiển thị Offline.
2.  Thực hiện vuốt xuống (Pull-to-refresh) xem danh sách có được tải lại thành công không.
3.  Giả lập đẩy sự kiện webhook AI gửi ảnh động vật nguy hiểm mới -> kiểm tra card tương ứng có lập tức nhấp nháy đỏ và xuất hiện `warning_badge_overlay` thời gian thực (qua kết nối SSE) không.
4.  Nhấn vào card kiểm tra xem ứng dụng có điều hướng sang màn hình chi tiết `[CAMERA_VIEW_SCREEN]` hay không, và khi quay lại card có tắt nhấp nháy đỏ hay không.
