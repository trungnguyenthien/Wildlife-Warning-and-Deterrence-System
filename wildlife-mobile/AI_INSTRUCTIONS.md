# Chỉ thị dành cho AI Coding Assistant (Android App System Instructions)

Bạn là trợ lý lập trình AI được giao nhiệm vụ phát triển ứng dụng di động Android (Native Kotlin) sử dụng Jetpack Compose cho dự án "Hệ thống Cảnh báo và Phòng ngừa Động vật hoang dã dã ngoại". Hãy tuân thủ nghiêm ngặt các quy định kiến trúc và quy trình dưới đây.

---

## 1a. Yêu cầu đọc tài liệu nghiệp vụ bên ngoài

Trước khi viết bất kỳ mã nguồn Kotlin/Compose nào, bạn BẮT BUỘC phải đọc toàn bộ nội dung của các tài liệu đặc tả nằm tại thư mục tài liệu chung của dự án (nằm ở thư mục cha `../docs/` tương đối):
*   **Đặc tả giao diện ứng dụng:** [02-dac-ta-man-hinh-android-app.md](../docs/02-dac-ta-man-hinh-android-app.md) (Chứa thông tin cấu trúc màn hình, luồng chuyển đổi UI và giao diện mẫu).
*   **Đặc tả API di động:** [03-mobile_api.md](../docs/03-mobile_api.md) (Chứa chi tiết danh sách API di động để cấu hình Retrofit/Ktor client).
*   **Sơ đồ tương tác Sequence:** [04-sequence-diagram.md](../docs/04-sequence-diagram.md) (Chứa luồng tương tác nghiệp vụ di động và cơ chế luồng tin SSE/WebSocket).
*   **Hình ảnh giao diện thiết kế (Mockup Images):** Các file ảnh thiết kế trực quan dạng `.png` được lưu trữ tại thư mục [../docs/design-screen](../docs/design-screen).

---

## 1b. Quy trình lập kế hoạch xây dựng màn hình (Screen Construction Planning)

Trước khi tiến hành code bất kỳ màn hình nào, AI Coding Assistant bắt buộc phải lập kế hoạch triển khai chi tiết:
1.  **Phối hợp tài liệu:** Phân tích ảnh thiết kế trực quan tương ứng trong thư mục [../docs/design-screen](../docs/design-screen), kết hợp với đặc tả UI chi tiết tại [02-dac-ta-man-hinh-android-app.md](../docs/02-dac-ta-man-hinh-android-app.md), cách gọi API ở [03-mobile_api.md](../docs/03-mobile_api.md), và luồng xử lý/chuyển trạng thái ở [04-sequence-diagram.md](../docs/04-sequence-diagram.md) để lên phương án xây dựng màn hình.
2.  **Khởi tạo Kế hoạch (Screen Plan):** Tạo tệp kế hoạch thiết kế màn hình dưới định dạng Markdown bên trong thư mục [wildlife-mobile/screen-plans](./screen-plans/) (nếu chưa có thư mục thì tự động tạo mới). Bản kế hoạch bắt buộc phải tuân thủ cấu trúc mẫu sau:
    *   `## 0. Thiết kế Giao diện Mockup (UI Design)`: Liên kết tương đối đến ảnh mockups dạng `.png` trong thư mục `../docs/design-screen/...` và hiển thị ảnh dạng xem trước (preview: `![caption](path)`).
    *   `## 1. Thành phần Giao diện (UI Components)`: Mô tả chi tiết cách chia các cấu phần UI (chia rõ Screen Composable chính và các SubViews tái sử dụng đặt ở thư mục `components`).
    *   `## 2. API Tương tác & Luồng Dữ liệu (Retrofit API Integration)`: Định nghĩa các endpoint API sẽ gọi, cấu trúc body và vẽ biểu đồ Mermaid `sequenceDiagram` mô tả luồng gọi dữ liệu.
    *   `## 3. Cấu trúc Trạng thái UI (UI State) & Event/Action`: Khai báo cấu trúc lớp dữ liệu UI State và danh sách các sự kiện tương tác giữa View và ViewModel.
    *   `## 4. Các Quy tắc Kiểm tra Định dạng (Client-side Validation Rules)`: Quy định rõ các quy tắc xác thực dữ liệu đầu vào phía Client.
    *   `## 5. Kế hoạch Kiểm thử (Verification Plan)`: Chỉ rõ phương án kiểm thử tự động (Unit Test ViewModel) và các bước kiểm thử thủ công để nghiệm thu giao diện.
3.  **Yêu cầu Phê duyệt (User Review Required):** Dừng lại và chờ người dùng xem xét, phê duyệt kế hoạch này trước khi bắt đầu tạo các file source code Kotlin chính thức.

---

## 2. Kiến trúc & Công nghệ Stack ứng dụng

Ứng dụng được thiết kế tối giản cho học sinh dễ đọc, dễ học nhưng vẫn tuân thủ các chuẩn mực phát triển hiện đại:

*   **Ngôn ngữ:** Kotlin 1.9+.
*   **Giao diện:** Jetpack Compose (Material Design 3).
*   **Mô hình thiết kế:** MVVM (Model-View-ViewModel) chia làm 3 tầng chính:
    1.  **Tầng Data:** Chứa các Model (Data classes), Retrofit API Interface, và Repository để xử lý tương tác mạng và lưu trữ.
    2.  **Tầng ViewModel:** Kế thừa từ `androidx.lifecycle.ViewModel`, quản lý các trạng thái UI thông qua `StateFlow`/`MutableStateFlow` và xử lý logic nghiệp vụ.
    3.  **Tầng View:** Các hàm Composable để vẽ giao diện dựa trên UI State nhận được từ ViewModel.
*   **Thư viện mạng:** Retrofit + OkHttp để gọi các API từ Vercel Backend Server.
*   **Bảo mật cơ bản:**
    *   Lưu trữ khóa đăng nhập JWT Token bằng **EncryptedSharedPreferences** của Jetpack Security để đảm bảo an toàn, tránh bị dịch ngược và đánh cắp token từ bộ nhớ máy.

---

## 3. Quy định cấu trúc thư mục phẳng (Simple & Flat Structure)

Hạn chế việc chia nhỏ thư mục sâu nhiều cấp. Cấu trúc thư mục của dự án Android phải tuân thủ dạng phẳng dưới đây:

```
wildlife-mobile/
├── app/
│   └── src/main/java/com/wildlife/deterrence/
│       ├── data/             # Retrofit client, Models, Repositories
│       ├── viewmodel/        # ViewModels của các màn hình chính
│       └── ui/               # Tầng hiển thị giao diện Compose
│           ├── components/   # Các SubViews/Widgets tái sử dụng (Button, AlertCard, Badge, v.v.)
│           ├── screens/      # Các màn hình chính (LoginScreen, MainScreen, CameraDetailScreen, v.v.)
│           └── theme/        # Cấu hình Màu sắc, Typography và Shapes của Material Theme
```

---

## 4. Quy định viết mã nguồn UI & Tái sử dụng SubView

*   **Không viết code View quá dài:** Các màn hình lớn tại `ui/screens/` phải được phân tách rõ ràng.
*   **Tách biệt SubViews:** Nếu một thành phần giao diện xuất hiện trên 2 màn hình trở lên (ví dụ: Card hiển thị cảnh báo, thanh hiển thị trạng thái kết nối camera, nút bấm styled đặc thù), bắt buộc phải tách ra thành một hàm Composable độc lập nằm ở thư mục `ui/components/`.
*   **Một Composable một trách nhiệm:** Tránh lồng ghép quá nhiều logic tính toán hay xử lý mạng trong tầng View. View chỉ nhận State và bắn Event lên ViewModel.

---

## 5. Scripts khởi động nhanh trên Emulator

Để thuận tiện cho việc phát triển và chạy thử ứng dụng không cần mở Android Studio, bạn phải duy trì và viết các script tự động tại thư mục root `scripts/` (ví dụ: `scripts/run-emulator.sh` hoặc `scripts/run-app.sh`):

1.  **`scripts/run-emulator.sh`**:
    *   Tìm kiếm danh sách thiết bị ảo (AVD) đã cài đặt trên máy bằng lệnh: `emulator -list-avds`.
    *   Tự động khởi chạy Emulator đầu tiên tìm thấy bằng lệnh: `emulator -avd <tên_avd> &`.
    *   Đợi thiết bị ảo boot hoàn thành bằng cách kiểm tra trạng thái qua `adb shell getprop sys.boot_completed`.
2.  **`scripts/run-app.sh`**:
    *   Thực hiện biên dịch và cài đặt ứng dụng debug lên thiết bị ảo bằng lệnh: `./gradlew installDebug`.
    *   Tự động kích hoạt khởi chạy MainActivity của App trên thiết bị ảo thông qua lệnh adb:
        `adb shell am start -n com.wildlife.deterrence/.MainActivity`
    *   Tự động theo dõi log từ app thông qua filter: `adb logcat | grep com.wildlife.deterrence`.

Mọi thay đổi tính năng cần được kiểm thử cục bộ trước khi bàn giao.
