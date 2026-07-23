# Đặc tả Thành phần Giao diện Tái sử dụng (Reusable UI Components Specification)

Tài liệu này định nghĩa danh sách các cấu phần giao diện (UI Components) tùy chỉnh, được chia nhỏ để tái sử dụng trên nhiều màn hình khác nhau của ứng dụng Android Native (Jetpack Compose). 

Để tránh làm phân rã mã nguồn và tăng độ phức tạp không đáng có cho học sinh, các component được thiết kế ở mức độ phức hợp vừa phải (không chia nhỏ đến mức chỉ chứa 1 nút bấm đơn lẻ).

---

## Danh sách các UI Components Tái sử dụng

### 1. `AppLogo` (Logo Hệ thống)
*   **Mô tả:** Khối thương hiệu của ứng dụng, hiển thị biểu tượng cảnh báo động vật (Warning Icon), tiêu đề lớn "Wildlife Warning" và phụ đề "Deterrence System".
*   **Vị trí tái sử dụng:** 
    *   `[SPLASH_SCREEN]` (canh giữa toàn màn hình)
    *   `[LOGIN_SCREEN]` (canh giữa phía trên biểu mẫu đăng nhập)
    *   `[REGISTER_SCREEN]` (canh giữa phía trên biểu mẫu đăng ký)
*   **Cấu trúc Compose đề xuất:**
    *   `Column` chứa:
        *   `Icon(Icons.Default.Warning)` với kích thước `80.dp` và màu đỏ cảnh báo.
        *   `Text` tiêu đề Bold `24.sp`.
        *   `Text` phụ đề `14.sp` màu xám mờ.

---

### 2. `CameraStatusBadge` (Nhãn Trạng thái Camera)
*   **Mô tả:** Nhãn hiển thị trạng thái kết nối trực tuyến của camera, tự động thay đổi màu nền và icon tương ứng với dữ liệu trạng thái.
*   **Vị trí tái sử dụng:**
    *   Camera Card trong `[CAMERA_LIST_TAB]` (góc trên cùng bên phải của thẻ)
    *   Thanh tiêu đề trong màn hình chi tiết `[CAMERA_VIEW_SCREEN]`
*   **Tham số đầu vào (Inputs):** 
    *   `isOnline: Boolean`
    *   `offlineDurationSeconds: Long` (để hiển thị thời gian offline nếu quá 30 giây)
*   **Cấu trúc Compose đề xuất:**
    *   Một `Surface` hoặc `Row` bo góc tròn (`RoundedCornerShape(8.dp)`), màu nền xanh lục nhạt khi Online, xám/đỏ nhạt khi Offline.
    *   Hiển thị icon chấm tròn kèm dòng chữ `Online` hoặc `Offline (X giây)`.

---

### 3. `ValidatedTextField` (Trường Nhập liệu kèm Xác thực - Cao Cấp)
*   **Mô tả:** Khối trường nhập liệu mở rộng từ `OutlinedTextField` mặc định. Hỗ trợ hiển thị nhãn lỗi màu đỏ bên dưới khi dữ liệu nhập vào không hợp lệ hoặc khi mất focus (focus lost), nút xóa nhanh nội dung (clear button). Khi được cấu hình làm trường mật khẩu (`isPassword = true`), ô nhập liệu sẽ tự động hiển thị nút ẩn/hiện mật khẩu (biểu tượng con mắt) ở góc phải để người dùng bật/tắt hiển thị mật khẩu dưới dạng văn bản thô (raw text).
*   **Vị trí tái sử dụng:**
    *   `[LOGIN_SCREEN]` (`username_input` kèm icon Person, `password_input` kèm icon Lock)
    *   `[REGISTER_SCREEN]` (`username_input` kèm icon Person, `email_input` kèm icon Email, `password_input` kèm icon Lock, nhập lại mật khẩu kèm icon Reset/Lock)
    *   Hộp thoại đổi tên camera `rename_camera_dialog` trong `[CAMERA_VIEW_SCREEN]` (Dạng tối giản, không kèm icon)
*   **Phong cách Thiết kế UI (Aesthetics):**
    *   **Bo góc tròn hiện đại:** Sử dụng `shape = RoundedCornerShape(12.dp)` cho toàn bộ ô nhập liệu.
    *   **Màu sắc đường viền (Border Colors):**
        *   Trạng thái bình thường: Màu viền xám hơi ánh lục nhẹ (ví dụ: `Color(0xFF8A9A8A)`).
        *   Trạng thái được chọn (Focused): Viền chuyển màu lục đậm thương hiệu (`Color(0xFF2C4C2C)`).
        *   Trạng thái lỗi (Error): Viền tự động chuyển đỏ (`MaterialTheme.colorScheme.error`).
    *   **Đồng bộ màu sắc Icon:** Các biểu tượng bổ trợ (như icon Person, Lock, Email và icon Con mắt) đều sử dụng tông màu lục đậm sẫm màu (`Color(0xFF2C3E2C)`) để tăng tính nhất quán của hệ thống.
*   **Tham số đầu vào (Inputs):**
    *   `value: String`, `onValueChange: (String) -> Unit`
    *   `label: String`, `placeholder: String`
    *   `leadingIcon: ImageVector? = null` (Icon hiển thị trước ô nhập liệu, ví dụ: icon Person cho tài khoản, Lock cho mật khẩu, v.v.)
    *   `isPassword: Boolean = false` (Nếu true, tự động hiển thị nút biểu tượng con mắt ở góc phải để chuyển đổi giữa hiển thị dấu hoa thị ẩn mật khẩu và hiển thị mật khẩu dạng văn bản thô)
    *   `errorText: String?` (nếu null thì ẩn nhãn lỗi)
    *   `onFocusChanged: ((Boolean) -> Unit)? = null` (Callback thông báo trạng thái focus thay đổi, giúp ViewModel kích hoạt validation khi người dùng rời ô nhập liệu - focus lost)
*   **Cơ chế Validation (Kiến trúc MVVM):**
    *   Để thuận tiện cho học sinh viết Unit Test và giữ View không có trạng thái phức tạp, các quy tắc kiểm tra định dạng (validate rules) sẽ **không viết trực tiếp trong Composable**.
    *   Logic validate được đặt trong **ViewModel**. Khi người dùng thay đổi dữ liệu hoặc rời ô nhập liệu (thông qua `onFocusChanged`), ViewModel sẽ kiểm tra dữ liệu và cập nhật `errorText` vào UI State. Composable chỉ cần hiển thị chuỗi lỗi được truyền xuống.
*   **Cấu trúc Compose đề xuất:**
    *   `Column` bao gồm `OutlinedTextField` chính và một `Text` báo lỗi (màu `MaterialTheme.colorScheme.error`, size `12.sp`) ở ngay phía dưới.
    *   Sử dụng `Modifier.onFocusChanged { focusState -> onFocusChanged?.invoke(focusState.isFocused) }` trên `OutlinedTextField` để bắt sự kiện thay đổi tiêu điểm.
    *   Cấu hình `colors` trong `OutlinedTextFieldDefaults.colors(...)` để đồng bộ màu viền lục đậm khi focus, màu xám khi bình thường, và màu đỏ khi có lỗi.
    *   `OutlinedTextField` sẽ quản lý một biến trạng thái ẩn/hiện (`var passwordVisible by remember { mutableStateOf(false) }`) để áp dụng `visualTransformation` tương ứng: `PasswordVisualTransformation()` nếu ẩn, hoặc `VisualTransformation.None` nếu hiện thô.

---

### 4. `EventLogListItem` (Dòng Nhật ký Cảnh báo)
*   **Mô tả:** Một dòng hiển thị thông tin tóm tắt sự kiện phát hiện động vật. Bao gồm ảnh nhỏ (thumbnail) con vật bị phát hiện, tên loài, mức độ tin cậy AI (Confidence), và thời gian ghi nhận.
*   **Vị trí tái sử dụng:**
    *   Thành phần `camera_log_list` trong màn hình chi tiết `[CAMERA_VIEW_SCREEN]`
    *   Danh sách cảnh báo nhanh `weekly_detections_section` ở tab `[STATISTICS_TAB]`
*   **Tham số đầu vào (Inputs):**
    *   `imageUrl: String` (link ảnh snapshot từ Cloudinary)
    *   `speciesName: String`
    *   `confidence: Float` (từ 0.0 đến 1.0)
    *   `timestamp: String`
*   **Cấu trúc Compose đề xuất:**
    *   Một `Card` dạng nằm ngang (`Row`):
        *   Bên trái: `AsyncImage` tải ảnh thumbnail nhỏ bo góc.
        *   Giữa: Cột chứa Tên loài (Bold) và thời gian ghi nhận (Caption).
        *   Phải: Nhãn hiển thị độ tin cậy (ví dụ: `92%`) kèm mức độ cảnh báo (màu đỏ cho voi/hổ, màu cam cho khỉ/heo).

---

### 5. `EmergencyWarningBanner` (Banner Cảnh báo Khẩn cấp)
*   **Mô tả:** Khung cảnh báo sticky nổi bật ở đầu màn hình, hiển thị thông tin tức thời khi hệ thống phát hiện thú dữ nguy hiểm cấp độ CAO (Voi, Hổ, Báo...) ở bất kỳ trạm camera nào. Hỗ trợ animation nhấp nháy đỏ-vàng và bấm vào để nhảy đến Camera tương ứng.
*   **Vị trí tái sử dụng:**
    *   Đầu màn hình `[CAMERA_LIST_TAB]`
    *   Đầu màn hình `[CAMERA_VIEW_SCREEN]` (nếu có phát hiện nguy cấp khi đang xem camera khác)
*   **Tham số đầu vào (Inputs):**
    *   `cameraName: String`
    *   `detectedSpecies: String`
    *   `timeString: String`
    *   `onBannerClick: () -> Unit`
*   **Cấu trúc Compose đề xuất:**
    *   Một `Row` nổi trên giao diện với màu nền đỏ/cam đậm, bo góc, có bóng đổ (`CardDefaults.cardElevation`).
    *   Icon cảnh báo rung/nhấp nháy (`AnimatedVisibility`), nội dung text cảnh báo khẩn và nút chuyển nhanh.

---

### 6. `SmsRecipientCard` (Thẻ Người nhận Cảnh báo)
*   **Mô tả:** Thẻ hiển thị thông tin liên hệ của người nhận tin cảnh báo khẩn cấp bằng SMS. Cho biết tên người nhận, số điện thoại, mối quan hệ và một nút xóa (Delete Icon).
*   **Vị trí tái sử dụng:**
    *   Danh sách chính trong `[SMS_CONFIG_SCREEN]`
    *   Hộp thoại xác nhận thao tác hoặc màn hình tổng hợp thông tin liên lạc.
*   **Tham số đầu vào (Inputs):**
    *   `fullName: String`
    *   `phoneNumber: String`
    *   `relation: String` (family / ranger / local_authority...)
    *   `onDeleteClick: () -> Unit`
*   **Cấu trúc Compose đề xuất:**
    *   Một `Card` chứa `Row` phân cấp rõ: thông tin liên hệ bên trái và nút `IconButton(Icons.Default.Delete)` màu đỏ ở bên phải để kích hoạt sự kiện xóa.

---

### 7. `H1ChoiceButtonGroup` (Hàng Nút Chọn Một - Ngang Cao Cấp)
*   **Mô tả:** Thành phần thanh chọn ngang cao cấp (Segmented Control). Cấu tạo gồm một thanh nền màu xám nhạt bo tròn dạng capsule. Các tùy chọn bên trong được phân bổ đều không gian. Tùy chọn đang được chọn sẽ nổi bật hẳn lên giống như một thẻ card màu trắng bo góc nằm chồng lên thanh nền, tạo cảm giác trượt và phản hồi trực quan mượt mà cho người dùng.
*   **Vị trí tái sử dụng:**
    *   Thanh lọc khoảng thời gian trong tab `[STATISTICS_TAB]` (7 ngày / 30 ngày / tùy chỉnh).
    *   Lựa chọn tần suất chớp LED (ví dụ: 2 lần/s, 4 lần/s) hoặc chế độ còi báo trong màn hình thiết lập `[SPECIES_CONFIG_DETAIL_SCREEN]`.
*   **Tham số đầu vào (Inputs):**
    *   `options: List<String>` (Danh sách tên các lựa chọn)
    *   `selectedOption: String` (Tùy chọn hiện tại đang được chọn)
    *   `onOptionSelected: (String) -> Unit` (Sự kiện khi người dùng nhấn chọn)
*   **Cấu trúc Compose đề xuất:**
    *   Một `Row` làm container nền chính:
        *   Màu nền: Xám nhạt (ví dụ: `Color(0xFFF0F0F0)`).
        *   Bo góc: `shape = RoundedCornerShape(12.dp)`.
        *   Khoảng đệm trong (Padding): `4.dp` bao quanh để tạo khoảng cách giữa viền nền và thẻ card lựa chọn bên trong.
        *   `verticalAlignment = Alignment.CenterVertically`.
    *   Các item bên trong được chia đều bằng `Modifier.weight(1f)`.
    *   Mỗi item là một `Box` hoặc `Surface` có sự kiện click bo tròn:
        *   Nếu được chọn: Màu nền Trắng tinh (`Color.White`), bo góc `shape = RoundedCornerShape(8.dp)`, có đổ bóng nhẹ (`CardDefaults.cardElevation(defaultElevation = 2.dp)`). Chữ bên trong hiển thị màu sẫm đậm (`FontWeight.Medium`).
        *   Nếu không được chọn: Màu nền trong suốt, chữ hiển thị màu xám mờ (`color = MaterialTheme.colorScheme.onSurfaceVariant`).

---

### 8. `V1ChoiceButtonGroup` (Nhóm Nút Chọn Một - Dọc Cao Cấp)
*   **Mô tả:** Nhóm nút chọn một được hiển thị theo chiều dọc. Thay vì sử dụng danh sách `RadioButton` mặc định đơn giản, thành phần này được nâng cấp thành dạng danh sách thẻ card bo góc xếp chồng. Mỗi lựa chọn nằm trên một thẻ có viền xám nhạt độc lập. Khi được chọn, thẻ sẽ kích hoạt trạng thái nổi bật: đổi viền sang màu lục đậm, đổi nền sang màu lục nhạt, chữ chuyển sang dạng Bold và có thể hiển thị thêm icon chỉ thị cảnh báo ở bên phải.
*   **Vị trí tái sử dụng:**
    *   Lựa chọn nhanh kịch bản mẫu Preset trong màn hình thiết lập `[SPECIES_CONFIG_DETAIL_SCREEN]` (Người lạ đột nhập, Thú vừa, Thú cực kỳ nguy hiểm, Tùy chỉnh).
*   **Tham số đầu vào (Inputs):**
    *   `options: List<String>`
    *   `selectedOption: String`
    *   `onOptionSelected: (String) -> Unit`
    *   `optionRightIcons: Map<String, ImageVector>? = null` (Bản đồ chứa các icon bổ sung hiển thị ở góc phải của từng lựa chọn, ví dụ: nhãn "Thú cực kỳ nguy hiểm" đi kèm icon Warning tam giác)
*   **Cấu trúc Compose đề xuất:**
    *   Sử dụng một `Column` xếp dọc với `verticalArrangement = Arrangement.spacedBy(10.dp)`.
    *   Mỗi lựa chọn là một `Surface` hoặc `OutlinedCard`:
        *   Bo góc: `shape = RoundedCornerShape(12.dp)`.
        *   Màu nền: Nếu được chọn sẽ là màu lục nhạt (ví dụ: `Color(0xFFEFF7EF)`), nếu không chọn là màu trắng hoặc màu Surface.
        *   Đường viền: Nếu được chọn sẽ là viền lục đậm dày `1.5.dp`, nếu không chọn là viền xám nhạt `BorderStroke(1.dp, Color.LightGray)`.
        *   Sử dụng `Row` bên trong thẻ với `verticalAlignment = Alignment.CenterVertically`, padding `16.dp`:
            *   **Bên trái (Radio Indicator):** Một icon vòng tròn tùy chỉnh (vẽ bằng `Box` + `border` bo tròn và chấm tròn ở giữa cho selected) thể hiện trạng thái chọn.
            *   **Ở giữa (Label):** `Text` nhãn hiển thị, tự động đổi `fontWeight = FontWeight.SemiBold` và màu sẫm khi được chọn.
            *   **Bên phải (Optional Icon):** `Icon` phụ trợ (nếu có định nghĩa trong `optionRightIcons`) hiển thị ở cuối hàng (ví dụ: Warning Icon màu xanh lục sẫm).

---

### 9. `AppCard` (Thẻ Container Chuẩn)
*   **Mô tả:** Khung chứa (Container) chuẩn hóa dùng để nhóm các thành phần giao diện liên quan lại với nhau (ví dụ: các ô cài đặt, phần tử danh sách). Giúp tạo chiều sâu (elevation), bo góc đồng bộ, viền nhẹ hiện đại và tự động tương thích với cả giao diện sáng (Light Theme) và tối (Dark Theme).
*   **Vị trí tái sử dụng:**
    *   Làm khung bao quanh từng Camera Card trong `[CAMERA_LIST_TAB]`.
    *   Bao bọc bảng cấu hình thiết bị ứng phó trong `[SPECIES_CONFIG_DETAIL_SCREEN]`.
    *   Bao bọc phần phân tích AI `ai_analysis_section` ở `[CAMERA_VIEW_SCREEN]`.
    *   Các nhóm tùy chọn cài đặt ở tab `[SETTING_TAB]`.
*   **Tham số đầu vào (Inputs):**
    *   `modifier: Modifier = Modifier`
    *   `onClick: (() -> Unit)? = null` (Nếu truyền vào, thẻ sẽ tự động có hiệu ứng click gợn sóng và phản hồi tương tác)
    *   `isAlert: Boolean = false` (Nếu true, thẻ sẽ tự động chuyển màu viền sang tông đỏ cảnh báo để biểu thị sự kiện khẩn cấp)
    *   `content: @Composable ColumnScope.() -> Unit` (Slot chứa nội dung con linh hoạt bên trong thẻ)
*   **Cấu trúc Compose đề xuất:**
    *   Sử dụng `Card` hoặc `OutlinedCard` của Material 3.
    *   Bo góc: `shape = RoundedCornerShape(16.dp)` (Phong cách bo góc bo viền hiện đại, tạo cảm giác mềm mại cao cấp).
    *   Màu nền: `containerColor = if (isAlert) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface`.
    *   Đường viền: `border = BorderStroke(1.dp, if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))` giúp phân định rõ ranh giới thẻ trên các nền khác nhau.
    *   Đổ bóng: `elevation = CardDefaults.cardElevation(defaultElevation = if (onClick != null) 4.dp else 2.dp)`.
    *   Nếu có `onClick`, sử dụng constructor `Card(onClick = ...)` hoặc `Modifier.clickable` để tự động tích hợp hiệu ứng gợn sóng (Material Ripple).

---

### 10. `AppText Components` (Các Composable View Văn bản Tự định nghĩa)
*   **Mô tả:** Thay vì sử dụng Composable `Text` mặc định của Jetpack Compose và tự định nghĩa kiểu dáng thủ công ở mỗi màn hình, dự án xây dựng 4 Composable View tùy chỉnh độc lập (4 hàm Kotlin riêng biệt). Các View này đóng vai trò là các View hiển thị văn bản chuyên dụng, được cấu hình sẵn kích thước, độ dày và màu sắc đồng bộ:
    *   `AppTitleText` (Composable View hiển thị tiêu đề chính của màn hình)
    *   `AppSectionTitleText` (Composable View hiển thị tiêu đề phân đoạn/khối nội dung)
    *   `AppSubTitleText` (Composable View hiển thị phụ đề, nhãn phụ hoặc caption)
    *   `AppBodyText` (Composable View hiển thị nội dung văn bản thông thường)
*   **Vị trí tái sử dụng:** Sử dụng trực tiếp làm các UI View văn bản ở tất cả các màn hình hiển thị để đảm bảo tính nhất quán của hệ thống.
*   **Tham số đầu vào (Inputs):**
    *   `text: String`
    *   `modifier: Modifier = Modifier`
    *   `color: Color = Color.Unspecified` (Nếu truyền vào sẽ ưu tiên sử dụng, nếu không sẽ lấy màu mặc định theo cấu hình hệ thống)
    *   `leadingIcon: ImageVector? = null` (Biểu tượng tùy chọn hiển thị bên trái văn bản, tự động áp dụng cùng tông màu tint với chữ)
    *   `textAlign: TextAlign? = null`
    *   `maxLines: Int = Int.MAX_VALUE`
*   **Cấu trúc Compose đề xuất (Định nghĩa hàm Kotlin độc lập):**
    *   Cả 4 Composable View đều có chung cơ chế bọc icon: Nếu `leadingIcon != null`, toàn bộ nội dung được bọc trong một `Row` với `verticalAlignment = Alignment.CenterVertically` và khoảng cách `Spacer(modifier = Modifier.width(6.dp))`. `Icon` hiển thị sẽ được truyền `tint` trùng khớp với màu chữ được phân giải.
    *   **`AppTitleText`:**
        *   Sử dụng: `Text(text = text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = color.takeOrElse { MaterialTheme.colorScheme.onBackground }, ...)`
    *   **`AppSectionTitleText`:**
        *   Sử dụng: `Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = color.takeOrElse { MaterialTheme.colorScheme.onSurface }, ...)`
    *   **`AppSubTitleText`:**
        *   Sử dụng: `Text(text = text, style = MaterialTheme.typography.bodySmall, color = color.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) }, ...)`
    *   **`AppBodyText`:**
        *   Sử dụng: `Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color.takeOrElse { MaterialTheme.colorScheme.onSurface }, ...)`

---

### 11. `AppButton` (Nút bấm Đa năng của Hệ thống)
*   **Mô tả:** Nút bấm chuẩn hóa toàn ứng dụng, hỗ trợ linh hoạt các kiểu dáng nút từ màu nền đặc (Filled), nút viền rỗng (Outlined), các kiểu bo góc (Hình chữ nhật bo tròn hoặc Hình con nhộng/Capsule), và hỗ trợ chèn thêm biểu tượng (icon) tùy chọn trước văn bản.
*   **Vị trí tái sử dụng:**
    *   Nút "Đăng nhập" ở `[LOGIN_SCREEN]` (Filled, Wide, Rectangular).
    *   Nút "Đăng ký" ở `[REGISTER_SCREEN]` (Filled, Wide, Rectangular).
    *   Nút "Nghe thử" trong `[SPECIES_CONFIG_DETAIL_SCREEN]` (Outlined, Rectangular kèm Sound Icon).
    *   Nút "Lưu" trong `[SPECIES_CONFIG_DETAIL_SCREEN]` (Filled, Rectangular kèm Save Icon).
    *   Nút "Thêm mới" / "Thêm SĐT" ở `[SMS_CONFIG_SCREEN]` (Filled, Capsule/Pill shape).
*   **Tham số đầu vào (Inputs):**
    *   `text: String`
    *   `onClick: () -> Unit`
    *   `modifier: Modifier = Modifier`
    *   `variant: AppButtonVariant = AppButtonVariant.Filled` (Gồm `Filled` - nền đặc màu lục, và `Outlined` - nền trong suốt viền lục)
    *   `shape: AppButtonShape = AppButtonShape.Rounded` (Gồm `Rounded` - bo góc `12.dp` hình chữ nhật mềm mại, và `Capsule` - bo tròn tối đa dạng viên thuốc `CircleShape`)
    *   `icon: ImageVector? = null` (Biểu tượng tùy chọn hiển thị trước nhãn văn bản)
    *   `enabled: Boolean = true`
*   **Cấu trúc Compose đề xuất:**
    *   Định nghĩa các kiểu nút qua Enum:
        ```kotlin
        enum class AppButtonVariant { Filled, Outlined }
        enum class AppButtonShape { Rounded, Capsule }
        ```
    *   Sử dụng nút bấm Material 3 (`Button` hoặc `OutlinedButton`):
        *   Nếu `variant == Filled`: Sử dụng `Button` với màu chủ đạo dự án (Lục đậm: `Color(0xFF006400)`), màu chữ trắng.
        *   Nếu `variant == Outlined`: Sử dụng `OutlinedButton` với `BorderStroke(1.dp, Color(0xFF006400))` và màu chữ là màu lục đậm.
    *   Thiết lập hình dạng (`shape`):
        *   Nếu `shape == Capsule`: Gán `shape = CircleShape`.
        *   Nếu `shape == Rounded`: Gán `shape = RoundedCornerShape(12.dp)`.
    *   Bên trong `RowScope` của nút bấm:
        *   Nếu `icon != null`, hiển thị `Icon(imageVector = icon, contentDescription = null, tint = color)` đi kèm `Spacer(Modifier.width(8.dp))`.
        *   Hiển thị `Text(text = text, fontWeight = FontWeight.SemiBold)`.
