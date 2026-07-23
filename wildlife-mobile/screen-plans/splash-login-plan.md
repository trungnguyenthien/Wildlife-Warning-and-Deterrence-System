# Kế hoạch Triển khai: SplashScreen và LoginScreen Boilerplate

Tài liệu này phác thảo kế hoạch xây dựng giao diện khởi động (`SplashScreen`) và giao diện đăng nhập (`LoginScreen`) dạng khung sườn (boilerplate).

## 1. Phân tích tài liệu thiết kế & API liên quan

*   **Tài liệu UI (02):** `[LOGIN_SCREEN]` chứa `username_input`, `password_input`, `login_button`, `register_linkbutton`.
*   **Tài liệu API (03):** Đăng nhập gọi `POST /auth/login` nhận về `token` và `role`.
*   **Bảo mật:** Lưu token vào `EncryptedSharedPreferences`.
*   **SplashScreen Logic:**
    1. Khi mở app, hiển thị màn hình Splash với logo và tên ứng dụng trong 2 giây.
    2. Đồng thời kiểm tra xem `EncryptedSharedPreferences` đã lưu trữ token đăng nhập hợp lệ chưa:
       * Nếu **Có token:** Chuyển tiếp tới màn hình chính `[MAIN_SCREEN]`.
       * Nếu **Không có token:** Chuyển tiếp tới màn hình đăng nhập `[LOGIN_SCREEN]`.

---

## 2. Cấu trúc mã nguồn đề xuất

Chúng ta sẽ tạo các lớp Kotlin trong gói `com.wildlife.deterrence`:

### Tầng Data (`data/`)
*   **`TokenManager.kt`**: Quản lý lưu trữ/đọc/xóa JWT Token sử dụng `EncryptedSharedPreferences`.
*   **`NetworkClient.kt`**: Khởi tạo cấu hình Retrofit + OkHttp.

### Tầng ViewModel (`viewmodel/`)
*   **`SplashViewModel.kt`**: Kiểm tra trạng thái token và kích hoạt sự kiện điều hướng.
*   **`LoginViewModel.kt`**: Quản lý trạng thái nhập liệu (username, password) và logic đăng nhập (hiện tại để trống cấu trúc sơ khai).

### Tầng View & UI Components (`ui/`)
*   **`ui/screens/SplashScreen.kt`**: Vẽ logo ứng dụng và tên hệ thống ở giữa màn hình bằng Compose.
*   **`ui/screens/LoginScreen.kt`**: Màn hình đăng nhập trống (chỉ chứa một text hiển thị placeholder như yêu cầu).
*   **`ui/components/AppLogo.kt`**: SubView vẽ Logo chung (được dùng ở Splash và có thể cả Login/Register sau này).

---

## 3. Quy trình thực hiện cụ thể

1.  Cài đặt các thư viện phụ thuộc (Hỗ trợ Navigation Compose, Coroutines, Retrofit, Jetpack Security) trong `build.gradle.kts`.
2.  Tạo tệp `TokenManager.kt` để quản lý token an toàn.
3.  Tạo `SplashViewModel.kt` điều phối luồng chuyển tiếp.
4.  Tạo các Composable `SplashScreen`, `LoginScreen` và `MainActivity` cấu hình Jetpack Navigation Graph để điều hướng.

---

## 4. Câu hỏi thảo luận / Review của người dùng

*   Bạn có muốn sử dụng logo mặc định dạng Icon Vector có sẵn của Material Design (ví dụ: `Icons.Default.Warning` hoặc hình ảnh lá chắn/động vật đại diện) cho SplashScreen và AppLogo không?
*   Đường dẫn API Backend mặc định cho Retrofit cục bộ sẽ trỏ về `http://10.0.2.2:3000` (địa chỉ localhost của máy tính nhìn từ Android Emulator). Bạn có đồng ý với thiết lập này không?
