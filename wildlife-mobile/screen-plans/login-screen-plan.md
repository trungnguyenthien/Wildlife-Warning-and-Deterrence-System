# Kế hoạch Triển khai: LOGIN_SCREEN (Màn hình Đăng nhập)

Bản kế hoạch này mô tả thiết kế và kiến trúc triển khai cho màn hình `[LOGIN_SCREEN]`, tuân thủ các tài liệu đặc tả nghiệp vụ (02), đặc tả API (03) và sơ đồ sequence (04).

## 0. Thiết kế Giao diện Mockup (UI Design)

*   **Hình ảnh Thiết kế Mockup:** [screen.png](../../docs/design-screen/LOGIN_SCREEN/screen.png)
*   **Bản xem trước trực quan (Preview):**
    ![Thiết kế LOGIN_SCREEN](../../docs/design-screen/LOGIN_SCREEN/screen.png)

---

## 1. Thành phần Giao diện (UI Components)

Màn hình được đặt tại `ui/screens/LoginScreen.kt` và chia nhỏ các SubViews như sau:

*   **`AppLogo` (Tái sử dụng từ `ui/components/AppLogo.kt`):** Hiển thị logo cảnh báo và tên dự án ở vị trí trên cùng.
*   **`login_title_text` (Text):** Hiển thị nhãn `Đăng nhập` với kiểu chữ H4 Bold.
*   **`username_input` (TextField):** Ô nhập tên đăng nhập với nhãn `Username`. Có nút xóa nhanh dữ liệu, hỗ trợ validate khi thay đổi tiêu điểm (focus).
*   **`password_input` (TextField - Password):** Ô nhập mật khẩu với nhãn `Mật khẩu`. Hỗ trợ ẩn/hiện mật khẩu bằng biểu tượng mắt (mặc định ẩn).
*   **`login_button` (Button):** Nút đăng nhập chính. Chỉ kích hoạt (enable) khi cả hai trường nhập liệu không trống.
*   **`register_linkbutton` (TextButton):** Nút liên kết `Đăng ký tài khoản` đưa người dùng sang màn hình đăng ký `[REGISTER_SCREEN]`.
*   **`login_error_snackbar` (Snackbar):** Hiển thị thông báo lỗi xác thực từ máy chủ hoặc lỗi mạng.

---

## 2. API Tương tác & Luồng Dữ liệu (Retrofit API Integration)

Màn hình sẽ tương tác với API Backend thông qua:
*   **API Endpoint:** `POST /auth/login` (Định nghĩa trong `data/AuthApi.kt`).
*   **Request Body:** `{ "username": "...", "password": "..." }`
*   **Response Body (Thành công):** `{ "token": "...", "role": "..." }`

### Luồng xử lý chính:
```mermaid
sequenceDiagram
  autonumber
  actor User as "Bệ Hạ / Người dùng"
  participant View as "LoginScreen"
  participant VM as "LoginViewModel"
  participant TM as "TokenManager"
  participant API as "AuthApi"

  User->>View: Nhập Username & Password
  User->>View: Nhấn [Đăng nhập] (login_button)
  View->>VM: login(username, password)
  VM->>API: POST /auth/login
  API-->>VM: Trả về 200 OK (token, role)
  VM->>TM: saveToken(token)
  VM-->>View: Emit trạng thái Success
  View->>User: Điều hướng sang [MAIN_SCREEN]
```

---

## 3. Cấu trúc Trạng thái UI (UI State) & Event/Action

### UI State:
```kotlin
data class LoginUiState(
    val usernameText: String = "",
    val usernameError: String? = null,
    val passwordText: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val loginSuccess: Boolean = false
)
```

### Events / Actions:
*   `onUsernameChanged(text: String)`: Cập nhật text và thực hiện validate định dạng.
*   `onPasswordChanged(text: String)`: Cập nhật text và thực hiện validate định dạng.
*   `onLoginClick()`: Kích hoạt gọi API xác thực, đổi trạng thái sang `isLoading = true`.
*   `clearErrors()`: Reset lại các thông báo lỗi hiển thị trên snackbar.

---

## 4. Các Quy tắc Kiểm tra Định dạng (Client-side Validation Rules)

*   **Username (`username_input`):**
    *   Bắt buộc phải nhập.
    *   Độ dài từ 4 đến 20 ký tự.
    *   Chỉ gồm các chữ cái (a-z, A-Z), chữ số (0-9) và dấu gạch dưới (`_`).
    *   Không được phép bắt đầu bằng một chữ số.
    *   Không được phép chứa toàn bộ là chữ số.
    *   *Thông báo lỗi:* `Tên đăng nhập 4–20 ký tự, gồm chữ, số và gạch dưới, không bắt đầu bằng số`.
*   **Password (`password_input`):**
    *   Bắt buộc phải nhập.
    *   Độ dài từ 6 đến 30 ký tự.
    *   Không được chỉ chứa khoảng trắng.
    *   *Thông báo lỗi:* `Mật khẩu 6–30 ký tự`.

---

## 5. Kế hoạch Kiểm thử (Verification Plan)

### Automated Tests (Unit Tests)
*   **`LoginViewModelTest.kt`**:
    *   `TC_UI_VAL_USER_01`: Validate username rỗng, quá ngắn, hoặc bắt đầu bằng số -> báo lỗi định dạng tương ứng.
    *   `TC_UI_VAL_PASS_01`: Validate mật khẩu quá ngắn -> báo lỗi định dạng.
    *   `TC_UI_AUTH_SUCCESS`: Đăng nhập thành công -> lưu token và báo trạng thái thành công.
    *   `TC_UI_AUTH_FAILURE`: Đăng nhập thất bại (401) -> hiển thị thông báo lỗi thích hợp lên UI State.

### Manual Verification
1.  Nhập sai định dạng username/password để xem cảnh báo tức thời dưới mỗi ô nhập liệu.
2.  Để trống một trong hai ô xem nút Đăng nhập có bị vô hiệu hóa hay không.
3.  Nhập sai tài khoản thực tế để kiểm tra sự hiển thị của thông báo lỗi trên Snackbar: `Sai tên đăng nhập hoặc mật khẩu`.
4.  Nhập đúng tài khoản đã đăng ký xem ứng dụng có điều hướng sang màn hình chính hay không.
