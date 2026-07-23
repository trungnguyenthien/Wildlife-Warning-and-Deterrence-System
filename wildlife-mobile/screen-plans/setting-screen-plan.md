# Kế hoạch Triển khai: SETTING_SCREEN (Màn hình Cài đặt)

Bản kế hoạch này mô tả thiết kế và kiến trúc triển khai cho màn hình Cài đặt (`[SETTING_SCREEN]`), được hiển thị trực tiếp trong Tab thứ 4 của màn hình chính ứng dụng `wildlife-mobile`, tuân thủ các tài liệu đặc tả nghiệp vụ (02), đặc tả API (03) và hướng dẫn viết mã nguồn tại `AI_INSTRUCTIONS.md`.

---

## 0. Thiết kế Giao diện Mockup (UI Design)

*   **Hình ảnh Thiết kế Mockup:** [screen.png](../../docs/design-screen/SETTING_TAB/screen.png)
*   **Bản xem trước trực quan (Preview):**
    ![Thiết kế SETTING_TAB](../../docs/design-screen/SETTING_TAB/screen.png)

---

## 1. Thành phần Giao diện (UI Components)

Màn hình được nhúng vào Tab thứ 4 của `ui/main/MainScreen.kt` dưới dạng Composable `SettingsTabContent` và cấu trúc giao diện theo các quy tắc sau:

*   **Bố cục InfoPanel tinh chỉnh gọn gàng:**
    *   Sử dụng cấu trúc dòng nằm ngang (`Row`) thay vì cột đứng (`Column`) để tiết kiệm diện tích và tăng tính cân đối.
    *   **Trái:** Ảnh Avatar dạng tròn (`CircleShape`) thu gọn kích thước (`64.dp`).
    *   **Phải:** Cột thông tin (`Column`) gồm: Họ tên (cỡ chữ lớn, in đậm), Dòng phụ chứa Tên đăng nhập và Vai trò (cỡ chữ nhỏ `12.sp`), Khối hiển thị ID người dùng dạng nhãn nổi bo góc (`Surface`) đè nền mờ (`White.copy(alpha = 0.15f)`) giúp nổi bật ID hex 4 ký tự.
*   **Chuyển đổi màu sắc đa giao diện (Theme Color Mapping):**
    *   **Chế độ sáng (Light Mode):** Sử dụng tông xanh lá chủ đạo: Nền thẻ cá nhân xanh lục rừng thẫm (`Color(0xFF2C4C2C)`), Nền Avatar xanh nhạt (`Color(0xFFEFF7EF)`), Tiêu đề màn hình xanh lục (`Color(0xFF2C4C2C)`).
    *   **Chế độ tối (Dark Mode):** Toàn bộ tông xanh lá chuyển sang sắc vàng/mustard ấm:
        *   Nền thẻ cá nhân chuyển sang vàng mù tạt đậm (`Color(0xFF6E5906)`) đảm bảo tương phản cao với chữ trắng.
        *   Nền Avatar chuyển sang vàng nhạt (`Color(0xFFFEF9E7)`), biểu tượng bên trong đổi sang sắc vàng đậm (`Color(0xFF6E5906)`).
        *   Hộp chọn giao diện `H1ChoiceButtonGroup` chuyển màu nền capsule sang vàng mù tạt đậm (`Color(0xFF6E5906)`) và container mờ (`Color(0xFF3E350E)`).
        *   Đường viền của các ô nhập liệu `ValidatedTextField` và thẻ chọn `V1ChoiceButtonGroup` tự động chuyển sang sắc vàng mù tạt khi được kích hoạt.
*   **`AppCard` (Tái sử dụng từ [AppCard](../UI_COMPONENTS.md#9-appcard-the-container-chuan)):** Khung chứa chuẩn hóa dùng để bọc nhóm thông tin tài khoản và cấu hình giao diện.
*   **`H1ChoiceButtonGroup` (Tái sử dụng từ [H1ChoiceButtonGroup](../UI_COMPONENTS.md#7-h1choicebuttongroup-hang-nut-chon-mot---ngang-cao-cap)):** Hàng nút chọn capsule ngang cao cấp thay thế cho nhóm RadioButton mặc định.
*   **`logout_button` (FilledButton):** Nút đăng xuất màu đỏ sử dụng tông màu lỗi `MaterialTheme.colorScheme.error`, kèm icon `Icons.Default.ExitToApp`.
*   **`logout_confirm_dialog` (AlertDialog):** Hộp thoại xác nhận đăng xuất.
*   **`sms_config_button` / `configure_defense_default_button` (Tái sử dụng từ [AppButton](../UI_COMPONENTS.md#11-appbutton-nut-bam-da-nang-cua-he-thong)):** Các dòng lệnh điều hướng cài đặt nâng cao.

---

## 2. API Tương tác & Luồng Dữ liệu (Retrofit API Integration)

Màn hình sẽ tương tác với API Backend thông qua:
*   **API Endpoint:** `GET /users/me` (Định nghĩa trong `data/AuthApi.kt`).
*   **Header:** `Authorization: Bearer <token>`
*   **Response Body (Thành công):**
    ```json
    {
      "id": "user-uuid",
      "username": "ranger_john",
      "fullName": "John Doe",
      "phoneNumber": "+84901234567",
      "role": "RANGER",
      "email": "john.doe@example.com"
    }
    ```

### Luồng xử lý chính:
```mermaid
sequenceDiagram
  autonumber
  actor User as "Bệ Hạ / Người dùng"
  participant View as "MainScreen (Tab 4)"
  participant VM as "MainViewModel"
  participant TS as "ThemeSettings"
  participant TM as "TokenManager"
  participant API as "AuthApi"

  Note over View: Khi người dùng chuyển sang Tab 4 (Settings)
  View->>VM: fetchUserProfile()
  VM->>API: GET /users/me
  API-->>VM: Trả về 200 OK (UserProfileResponse)
  VM-->>View: Cập nhật UI State (Hiển thị User ID)

  Note over View: Khi thay đổi cấu hình giao diện
  User->>View: Chọn Chế độ tối (Dark Mode) trên H1ChoiceButtonGroup
  View->>TS: setThemeMode("dark")
  TS-->>View: Phát dòng dữ liệu themeMode (StateFlow)
  Note over View: Giao diện toàn app chuyển sang tối lập tức

  Note over View: Khi nhấn Đăng xuất
  User->>View: Nhấn [Đăng xuất] (logout_button)
  View->>User: Hiển thị Dialog xác nhận
  User->>View: Nhấn Đồng ý đăng xuất
  View->>TM: deleteToken()
  View->>User: Điều hướng quay lại [LOGIN_SCREEN]
```

---

## 3. Cấu trúc Trạng thái UI (UI State) & Event/Action

### Cập nhật MainViewModel.kt:
*   `userProfile: StateFlow<UserProfileResponse?>`: Thông tin hồ sơ người dùng tải từ máy chủ.
*   `isLoadingProfile: StateFlow<Boolean>`: Trạng thái xoay tròn loading khi đang tải profile.
*   `profileError: StateFlow<String?>`: Nội dung lỗi hiển thị nếu không thể kết nối hoặc lỗi token.

### Events / Actions:
*   `fetchUserProfile()`: Thực hiện gọi API `GET /users/me` sử dụng Token lấy từ `TokenManager`.
*   `selectTab(index: Int)`: Hàm thay đổi tab hiện hành.

---

## 4. Các Quy tắc Luồng nghiệp vụ (Business Rules)

*   **Tự động tải dữ liệu:** Khi người dùng chuyển sang Tab 4, nếu `userProfile` trong ViewModel đang trống (`null`), hệ thống tự động phát lệnh `fetchUserProfile()`.
*   **Lưu trữ Theme bền vững:** Lựa chọn theme của người dùng phải được lưu giữ xuống `SharedPreferences` cục bộ để duy trì trạng thái khi người dùng mở lại app lần sau.
*   **Ngăn chặn nút Back sau khi Logout:** Khi nhấn Đăng xuất thành công, ứng dụng phải xóa sạch Backstack định tuyến trước khi điều hướng sang màn hình Login, ngăn ngừa việc người dùng nhấn nút Back quay lại màn hình chính khi không còn phiên làm việc.

---

## 5. Kế hoạch Kiểm thử (Verification Plan)

### Automated Tests (Unit Tests)
*   **`MainViewModelTest.kt`**:
    *   `testSelectTab`: Chuyển đổi qua lại giữa các Tab -> UI State cập nhật chỉ số tab chính xác.
    *   `testFetchUserProfileSuccess`: Tải profile thành công -> Lưu dữ liệu vào `userProfile` và tắt loading.
    *   `testFetchUserProfileFailure`: Máy chủ trả lỗi (ví dụ: 401 hoặc 500) -> Đặt `profileError` và tắt loading.

### Manual Verification
1.  Đăng nhập, truy cập màn hình chính, nhấn chuyển sang Tab "Cài đặt".
2.  Xác nhận hiển thị đúng mã định danh User ID tương ứng với tài khoản đã đăng nhập.
3.  Thay đổi theme sang "Tối", "Sáng", "Hệ thống" trên `H1ChoiceButtonGroup` để kiểm tra màu sắc giao diện thay đổi tức thì. Khởi động lại ứng dụng để xác thực theme đã chọn được duy trì bền vững.
4.  Nhấn nút "Đăng xuất", chọn "Hủy" kiểm tra hộp thoại biến mất. Nhấn chọn "Đăng xuất" lần nữa, bấm xác nhận, kiểm định ứng dụng đã chuyển hướng về màn hình Login và không thể nhấn Back để quay lại.
