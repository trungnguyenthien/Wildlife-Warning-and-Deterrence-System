# Đặc tả màn hình chức năng — Android App

**Dự án:** Ứng dụng hệ thống cảnh báo và xua đuổi động vật hoang dã

**Nền tảng:** Android (Mobile App)

**Hướng hiển thị:** Vertical (Portrait) only — khóa cứng xoay dọc để tối ưu thao tác một tay ngoài thực địa.

**Ngôn ngữ giao diện:** Tiếng Việt (mặc định)

---

## Mục lục màn hình

1. `[LOGIN_SCREEN]` — Màn hình đăng nhập
2. `[REGISTER_SCREEN]` — Đăng ký tài khoản
3. `[MAIN_SCREEN]` — Trung tâm điều khiển với 3 tab `[CAMERA_LIST_TAB]` / `[STATISTICS_TAB]` / `[SETTING_TAB]`. Cấu hình thiết bị ứng phó thực hiện qua tab `[SETTING_TAB]` → `[SPECIES_CONFIG_LIST_SCREEN]` → `[SPECIES_CONFIG_DETAIL_SCREEN]`.
4. `[CAMERA_VIEW_SCREEN]` — Chi tiết một Camera *(gồm: ảnh hiện tại, đổi tên camera, danh sách log lịch sử)*
5. `[SPECIES_CONFIG_LIST_SCREEN]` — Danh sách loại thú cần thiết lập
6. `[SPECIES_CONFIG_DETAIL_SCREEN]` — Thiết lập hành vi phòng vệ theo loài *(luôn áp dụng cho tất cả camera)*

> ℹ️ **Bố cục tab của `[MAIN_SCREEN]`:**
> - Tab `[CAMERA_LIST_TAB]` *(mặc định)*: danh sách camera card.
> - Tab `[STATISTICS_TAB]`: thống kê tổng hợp toàn hệ thống.
> - Tab `[SETTING_TAB]`: cài đặt chung (ngôn ngữ, theme, đăng xuất) + nút điều hướng sang `[SPECIES_CONFIG_LIST_SCREEN]` để cấu hình thiết bị ứng phó.

---

## 1. `[LOGIN_SCREEN]` — Màn hình đăng nhập

Màn hình khởi đầu khi người dùng mở ứng dụng lần đầu (chưa có session hợp lệ). Form **đơn giản 2 ô**: Username + Password — phù hợp với phạm vi đề tài nghiên cứu của học sinh.

| Thành phần | Kiểu | Mô tả |
|---|---|---|
| Logo ứng dụng | Image | Logo dự án canh giữa phía trên cùng. |
| Tiêu đề `Đăng nhập` | Text | Tiêu đề màn hình. |
| Ô `Username` | TextField | Bắt buộc. Validate format ngay khi rời ô. |
| Ô `Mật khẩu` | TextField (Password) | Bắt buộc. Có icon con mắt hiện/ẩn (mặc định ẩn). |
| Nút `Đăng nhập` | Button (Primary, full-width) | Validate cả 2 ô → gọi API xác thực. Enable khi cả 2 ô không rỗng. |
| Nút `Đăng ký` | Text link | Mở `[REGISTER_SCREEN]`. |

**Luồng chính:**
- `Đăng nhập` thành công → `[MAIN_SCREEN]` (tab `[CAMERA_LIST_TAB]` mặc định).
- `Đăng nhập` thất bại → hiển thị Snackbar lỗi `Sai tên đăng nhập hoặc mật khẩu` (thông báo dạng chung, không phân biệt username/mật khẩu sai để tránh lộ thông tin).

### Quy tắc validation (chỉ kiểm tra format — không qua server)

| Trường | Quy tắc | Thông báo lỗi |
|---|---|---|
| Username | Bắt buộc; 4–20 ký tự; chỉ gồm chữ cái (a–z, A–Z), số (0–9), dấu gạch dưới (`_`); không được bắt đầu bằng số; không được toàn số. | `Tên đăng nhập 4–20 ký tự, gồm chữ, số và gạch dưới, không bắt đầu bằng số`. |
| Mật khẩu | Bắt buộc; 6–30 ký tự; không được chỉ toàn khoảng trắng. | `Mật khẩu 6–30 ký tự`. |

> Lưu ý: validation form chỉ chặn lỗi **định dạng** rỗng/sai trước khi bấm Đăng nhập. Sai thông tin thật (sai username/mật khẩu) do server phản hồi, hiển thị qua Snackbar ở `Hành vi`.

---

## 2. `[REGISTER_SCREEN]` — Đăng ký tài khoản

Được mở khi user nhấn **Đăng ký** ở `[LOGIN_SCREEN]`. Form đăng ký gồm **Username + Email + Password** — đơn giản hơn phiên bản OTP nhưng có đầy đủ validation để đảm bảo tài khoản hợp lệ. Sau khi tạo thành công, user quay về `[LOGIN_SCREEN]` để đăng nhập.

### Giao diện (Vertical)

| Vị trí | Thành phần | Kiểu | Mô tả |
|---|---|---|---|
| Top bar | Nút `Back` ← `[LOGIN_SCREEN]` · Tiêu đề `Đăng ký tài khoản` | — | Back là Android Stack mặc định. |
| Trên cùng | Tiêu đề phụ | Text | `Tạo tài khoản để quản lý hệ thống cảnh báo`. |
| Form | Ô `Username` | TextField | Bắt buộc. Cùng quy tắc format với `[LOGIN_SCREEN]`. |
| Form | Ô `Email` | TextField | Bắt buộc. Dùng để khôi phục tài khoản sau này. Hiển thị `✓` khi đúng định dạng. |
| Form | Ô `Mật khẩu` | TextField (Password) | Bắt buộc. Có icon con mắt hiện/ẩn. Khi user gõ, hiển thị **thanh đo độ mật `Yếu / Trung bình / Mạnh`** ngay phía dưới (tính theo độ dài + có cả chữ lẫn số). |
| Form | Ô `Xác nhận mật khẩu` | TextField (Password) | Bắt buộc. So sánh với ô trên ngay khi gõ. Hiển thị lỗi `Mật khẩu không khớp` nếu lệch. |
| Form | Nút `Đăng ký` | Button (Primary, full-width) | Validate toàn bộ form khi bấm → gọi API tạo tài khoản. Enable khi cả 4 ô hợp lệ và mật khẩu khớp. |
| Cuối màn | Text link `Đã có tài khoản? Đăng nhập` | Text link | Mở `[LOGIN_SCREEN]` (tương đương nút Back). |

### Quy tắc validation

| Trường | Quy tắc | Thông báo lỗi |
|---|---|---|
| Username | Bắt buộc; 4–20 ký tự; chỉ gồm chữ cái (a–z, A–Z), số (0–9), dấu gạch dưới (`_`); không bắt đầu bằng số; không toàn số. | `Tên đăng nhập 4–20 ký tự, gồm chữ, số và gạch dưới, không bắt đầu bằng số`. |
| Username đã tồn tại | Server trả 409 khi bấm `Đăng ký`. | `Tên đăng nhập đã được sử dụng — chọn tên khác`. |
| Email | Bắt buộc; theo cú pháp `local@domain.tld` — `local` gồm chữ cái/số/`._%+-`; `domain` và `tld` chỉ gồm chữ cái/số/`-`; phải có `.` ngăn `domain` và `tld`; `tld` tối thiểu 2 ký tự. | `Email không hợp lệ`. |
| Email đã tồn tại | Server trả 409 khi bấm `Đăng ký`. | `Email đã được sử dụng — đăng nhập ngay?`. |
| Mật khẩu | Bắt buộc; 8–30 ký tự; phải chứa cả chữ cái và chữ số; không được chỉ toàn khoảng trắng. | Lỗi riêng theo điều kiện: `Tối thiểu 8 ký tự` / `Phải có cả chữ và số` / `Tối đa 30 ký tự`. |
| Xác nhận mật khẩu | Phải khớp với ô Mật khẩu. | `Mật khẩu không khớp`. |

**Quy tắc độ mật** (chỉ hiển thị thanh `Yếu/Trung bình/Mạnh`, không chặn):
- `Yếu`: < 8 ký tự hoặc chỉ chữ hoặc chỉ số.
- `Trung bình`: ≥ 8 ký tự và có cả chữ lẫn số.
- `Mạnh`: ≥ 12 ký tự có cả chữ và số (có thể bổ sung chữ hoa và ký tự đặc biệt để đạt Mạnh, không bắt buộc).

### Hành vi

- Hiển thị lỗi validation **dạng inline** ngay dưới ô tương ứng khi user rời ô (on unfocus) hoặc khi bấm `Đăng ký`.
- Khi bấm `Đăng ký` mà form không hợp lệ: focus vào ô lỗi đầu tiên + Snackbar `Vui lòng kiểm tra các ô bị lỗi`.
- Trong khi chờ API (loading) → disable nút `Đăng ký`, hiển thị spinner nhỏ phía trước nhãn.
- Khi API trả 409 (username/email đã tồn tại) → hiện lỗi inline ngay dưới ô tương ứng + focus ô đó, **không load lại form**.
- Khi thành công → chuyển về `[LOGIN_SCREEN]`, Snackbar `Đăng ký thành công — vui lòng đăng nhập`. Username đã nhập **tự fill** vào ô Username của `[LOGIN_SCREEN]` (giúp đăng nhập luôn, không phải gõ lại).

### Điều hướng

- `Back` (Android Stack) → `[LOGIN_SCREEN]`.
- Thành công → `[LOGIN_SCREEN]` + auto fill Username.
- Text link `Đã có tài khoản? Đăng nhập` tương đương nút Back.

---

## 3. `[MAIN_SCREEN]` — Trung tâm điều khiển (3 tab)

Màn hình chính sau khi đăng nhập. **Mỗi tab có layout nội dung hoàn toàn khác nhau** — chuyển tab là chuyển hẳng sang "trang" mới, không phải lướt ngang:

- **Dọc (Vertical — mặc định):** Thanh tab nằm ở **dưới cùng** màn hình (Bottom Tab Bar), nội dung tab chiếm phần còn lại phía trên.
- **Ngang (Horizontal — chỉ preview thiết kế):** Thanh tab nằm **bên phải** (Right Tab Bar), nội dung tab chiếm phần còn lại bên trái.

| Tab | Mặc định | Mô tả ngắn |
|---|---|---|
| `[CAMERA_LIST_TAB]` | ✅ Hiển thị đầu tiên | Danh sách camera (mỗi card có timestamp + background chớp theo mức nguy hiểm + badge cảnh báo). |
| `[STATISTICS_TAB]` | | Biểu đồ thống kê + heatmap tổng quan. |
| `[SETTING_TAB]` | | Cài đặt chung (ngôn ngữ, giao diện, đăng xuất). |

> ❓ **Quan trọng:**
>
> **Danh sách camera chỉ hiển thị ở tab `[CAMERA_LIST_TAB]`.** Hai tab `[STATISTICS_TAB]` và `[SETTING_TAB]` không có danh sách camera.
>
> Thiết bị ứng phó **phụ thuộc vào loài phát hiện** (Silent Alert cho thú dữ, Active Deterrent cho thú vừa…), nên cấu hình chúng đi theo luồng **[SPECIES_CONFIG_LIST_SCREEN]** → chọn loài → **[SPECIES_CONFIG_DETAIL_SCREEN]** với scope `Áp dụng cho tất cả` camera. Nút điều hướng nằm trong tab `[SETTING_TAB]` ở mục 3.3.

---

### 3.1. `[CAMERA_LIST_TAB]` — Tab danh sách camera *(mặc định)*

Tab này vừa quản lý camera, vừa hiển thị cảnh báo khẩn cấp.

> ℹ️ **Lưu ý về vị trí các Icon / Nút trên từng Camera Card:**
> - **Không có** bất kỳ icon/nút Cài đặt hay Lịch sử riêng nào ở thanh header/top bar của màn `[MAIN_SCREEN]` — kể cả FAB floating button.
> - Mọi thao tác đặc thù cho 1 camera (xem ảnh, đổi tên, xem danh sách log) đều thực hiện trong **[CAMERA_VIEW_SCREEN]** — user **nhấn vào card** sẽ nhảy thẳng vào màn đó của camera tương ứng, **không qua màn chọn camera trung gian**.

#### b) Banner cảnh báo nhấp nháy *(tuỳ chọn UI; có thể bỏ nếu badge trên card đã đủ)*

| Thành phần | Mô tả |
|---|---|
| Banner | Có animation nhấp nháy đỏ/vàng. Nội dung: `Tên camera · Phát hiện [LOÀI] · [giờ:phút]`. Ví dụ: `Cam 1 · Phát hiện VOI · 9:04`. |
| Phân tích AI bên dưới banner | Loài, Số lượng cá thể, Mức độ nguy hiểm, Độ tin cậy AI (%). |

**Hành vi:** Banner tự động xuất hiện khi server gửi sự kiện FCM tới thiết bị. Nhấn vào banner → chuyển sang `[CAMERA_VIEW_SCREEN]` của camera tương ứng. Vì mỗi Camera Card đã có badge cảnh báo nhấp nháy riêng (phụ thuộc mức nguy hiểm), banner sticky trên đầu tab có thể *bỏ qua* nếu thấy dư thừa.

#### a) Danh sách thẻ camera

> Số lượng camera **không cố định 4** — render động theo số camera thực tế trong hệ thống (1 hoặc nhiều hơn).

Tab `[CAMERA_LIST_TAB]` gồm **một khối duy nhất**: lưới các Camera Card. Mọi thông tin, bộ lọc, điều khiển cụ thể cho từng camera được đặt ngay trong **card của camera đó** (xem Chi tiết một Camera Card phía dưới).

#### a1) Chi tiết một Camera Card

Mỗi card là **đơn vị nhỏ nhất** của danh sách, đại diện cho 1 camera. Mô tả theo **thông tin hiển thị** + **điều khiển khả dụng**.

##### Thông tin hiển thị

| # | Thông tin | Kiểu | Mô tả |
|---|---|---|---|
| 1 | **Trạng thái kết nối** | Status indicator | `🟢 Online` (xanh lá) · `⚪ Offline` (xám). Khi offline ≥ 30s sẽ hiển thị rõ đi kèm icon offline. |
| 2 | **Tên camera** | Text (Bold) | `Cam 1`, `Cam 2`… (đánh số tự động); có thể đổi sang tên tuỳ chỉnh trong `[CAMERA_VIEW_SCREEN]` (vd: `Cam Khu A`) — nhấn nút `✏️ Đổi tên` trên màn đó. |
| 3 | **Khu vực lắp đặt** | Text (caption) | Mô tả ngắn vị trí: `Rìa rừng phía B`, `Trạm 2 · Đồi cao`… Cắt bớt nếu dài. |
| 4 | **Ảnh thumbnail** | Image (16:9) | Ảnh snapshot gần nhất có **độ tin cậy AI ≥ 50%**. Nếu chưa có → placeholder icon camera + nền xám. Nếu offline → overlay icon `⚪ Offline` + tối màu 50%. Khi đang tải → shimmer effect. |
| 5 | **Badge cảnh báo trên ảnh** *(tuỳ trạng thái)* | Animated badge | Chỉ hiện khi camera có sự kiện AI mới trong 30 phút chưa xem. Nội dung: `⚠️ [LOÀI] · [%]` (vd: `⚠️ VOI · 92%`). Animation nhấp nháy đỏ-vàng nếu mức nguy hiểm cao. Tắt nhấp nháy khi user đã mở `[CAMERA_VIEW_SCREEN]` của camera đó (giữ nguyên badge để vẫn biết có sự kiện). |
| 6 | **Thời gian ghi nhận hình ảnh** *(timestamp snapshot)* | TextOverlay | Mốc thời gian server **chụp ảnh snapshot**, không phải live. Định dạng `HH:mm · dd/MM` (vd: `9:04 · 16/07`); tooltip dài hơn `HH:mm:ss · dd/MM/yyyy`. Hiển thị overlay góc dưới-trái của ảnh thumbnail, nền đen mờ 60% chữ trắng. Nếu ảnh > 1 giờ: thêm nhãn "cũ" hoặc icon `⏰` cảnh báo (vd: `9:04 · 16/07 · ⏰ cũ`); > 24 giờ: hiển thị cả ngày `16/07` rõ. Nếu chưa có ảnh → text gạch chân `—`. Khi user vào `[CAMERA_VIEW_SCREEN]` → xem thêm "Cách đây X phút" (relative time). |

##### Điều khiển (Controls)

| # | Điều khiển | Vị trí trong card | Hành vi |
|---|---|---|---|
| C1 | **Nhấn vào thân card** *(bất kỳ vị trí nào trên card)* | Toàn bộ card | Mở `[CAMERA_VIEW_SCREEN]` của camera đó — tại đây có hình ảnh hiện tại, nút đổi tên camera, và danh sách log lịch sử. |

> 💡 Camera Card chỉ có **một điều khiển duy nhất: nhấn thân card**. Mọi thao tác khác (đổi tên, xem lịch sử) đều thực hiện bên trong `[CAMERA_VIEW_SCREEN]`.

##### Trạng thái của card (Card state)

Trạng thái card gồm **hai chiều độc lập**, kết hợp để quyết định render:

> **Chiều 1 — Background chớp theo mức cảnh báo** *(do AI quyết định, dựa trên mức độ nguy hiểm của loài phát hiện gần nhất)*:
>
> | Mức cảnh báo | Loài đại diện | Background card | Tần suất chớp |
> |---|---|---|---|
> | **Cao** | Voi, Hổ, Báo, Tê giác, Rắn, Cá sấu, Người lạ | Nền **đỏ** (đỏ chói #D32F2F) phủ 12-18% | Nhấp nháy **nhanh**: 1 nhịp / 1s, alpha dao động 12%-18%, kèm glow đỏ xung quanh border |
> | **Trung bình** | Nai lớn, Khỉ đàn, Heo rừng | Nền **vàng / hổ phách** (#F9A825) phủ 10-14% | Nhấp nháy **vừa**: 1 nhịp / 1.5s, alpha dao động 10%-14%, không glow |
> | **Thấp** | Sóc, chim, các loài ít nguy hại | Nền **xanh nhạt** (#43A047) phủ 6-8% (chỉ tint, không chớp) | Không chớp — chỉ tint nhẹ ổn định |
> | **Không có sự kiện** | — | Nền trắng/kem bình thường | Không chớp |
>
> Quy tắc: chỉ áp dụng khi **ảnh snapshot có độ tin cậy ≥ 50%** và trong vòng **30 phút** gần đây. Quá 30 phút → tự động chuyển về "Thấp / không có sự kiện".

> **Chiều 2 — Đã xem / Chưa xem** *(do User quyết định)*:
>
> | Trạng thái | Điều kiện | Render |
> |---|---|---|
> | **Chưa xem** | User chưa mở `[CAMERA_VIEW_SCREEN]` của camera này. | Badge cảnh báo **có animation chớp** + border sáng. |
> | **Đã xem** | User đã mở `[CAMERA_VIEW_SCREEN]`. | Badge vẫn hiện (giữ thông tin) nhưng **tắt animation chớp**, border giảm sáng xuống mức "đã xem". |

**Kết hợp 2 chiều** *(ma trận render cuối cùng của card)*:

| Trạng thái tổng hợp | Background | Badge | Border |
|---|---|---|---|
| Có thú nguy hiểm cao + Chưa xem | Đỏ chớp nhanh + glow | `⚠️ [LOÀI] · [%]` chớp | Đỏ sáng |
| Có thú nguy hiểm cao + Đã xem | Đỏ chớp nhanh (giữ để cảnh báo liên tục 30 phút) | `⚠️ [LOÀI] · [%]` tĩnh | Đỏ vừa |
| Có thú TB + Chưa xem | Vàng chớp vừa | `⚠️ [LOÀI] · [%]` chớp | Vàng |
| Có thú TB + Đã xem | Vàng chớp vừa | `⚠️ [LOÀI] · [%]` tĩnh | Vàng nhạt |
| Có thú thấp / không có | Xanh nhạt (tĩnh) hoặc nền trắng | Không có | Xám nhạt |
| **Offline** | Nền xám đậm 50% | Ảnh tối + overlay `⚪ Offline` | Xám đậm |

##### Quy tắc render

- Grid 2 cột trên tablet/screen lớn; grid 2 cột trên điện thoại ≥ 360dp; rơi về 1 cột nếu < 320dp.
- Aspect ratio ảnh thumbnail: **16:9** (khoảng 65% chiều cao card).
- Border radius card: 12dp. Elevation: 2dp (shadow nhẹ).
- Thumbnail đang tải: shimmer effect.

---

### 3.2. `[STATISTICS_TAB]` — Tab thống kê

Tab này **không có danh sách camera**. Chỉ hiển thị thống kê tổng hợp toàn hệ thống:

| Thành phần | Mô tả |
|---|---|
| Khối `Phát hiện trong tuần` | Danh sách các sự kiện: `Camera · Ngày giờ · Loài`. |
| Khối `Phân tích theo từng camera` | Số lần xuất hiện, xu hướng (Chart line), khu vực di chuyển (sơ đồ/heatmap rừng). |
| Bộ lọc | Theo khoảng thời gian (7 ngày / 30 ngày / tuỳ chỉnh) · theo loài · theo camera cụ thể. |

> 💡 *Lưu ý:* Muốn xem **lịch sử chi tiết từng camera** (danh sách log theo thời gian), nhấn vào Camera Card tương ứng ở tab `[CAMERA_LIST_TAB]` → `[CAMERA_VIEW_SCREEN]` — phần "Danh sách log" nằm cuối màn đó. Tab `[STATISTICS_TAB]` chỉ cung cấp cái nhìn tổng quan.

---

### 3.3. `[SETTING_TAB]` — Tab cài đặt

Nơi duy nhất để user chỉnh cài đặt cá nhân và quản trị tài khoản.

| Thành phần | Kiểu | Mô tả |
|---|---|---|
| `Ngôn ngữ` | Dropdown | `Tiếng Việt` (mặc định) · `English`. |
| `Giao diện sáng/tối` | Toggle | `Sáng` / `Tối` (theo system hoặc thủ công). |
| `Thông báo SMS` | Toggle | Bật/tắt chuông điện thoại khi nhận SMS cảnh báo. |
| `Thiết lập hành vi ứng phó mặc định cho tất cả camera` | Button | Mở `[SPECIES_CONFIG_LIST_SCREEN]` để user **chọn loài** cần cấu hình → mở `[SPECIES_CONFIG_DETAIL_SCREEN]` với **scope = `Áp dụng cho tất cả`** camera. Cấu hình theo từng loài áp dụng cho mọi camera trong hệ thống. |
| `Đăng xuất` | Button | Xoá session → về `[LOGIN_SCREEN]`. |

> 💡 **Không có** toggle thiết bị ứng phó (SMS / Loa / Âm thanh / LED / Hàng rào / Kiểm lâm) ngay trong tab `[SETTING_TAB]`. Cấu hình các thiết bị này thuộc về **`[SPECIES_CONFIG_DETAIL_SCREEN]`** và phải đi qua `[SPECIES_CONFIG_LIST_SCREEN]` để chọn loài — vì thiết bị ứng phó phụ thuộc vào loài phát hiện (Silent Alert cho thú dữ, Active Deterrent cho thú vừa…), không thể cấu hình tách rời khỏi ngữ cảnh loài.

---

## 4. `[CAMERA_VIEW_SCREEN]` — Chi tiết một Camera

Được mở khi user **nhấn vào một thẻ camera** từ `[MAIN_SCREEN]` (tab `[CAMERA_LIST_TAB]`). Tại đây user xem ảnh hiện tại, đổi tên camera, và xem danh sách log.

> ℹ️ **Màn này KHÔNG hiển thị live video streaming.** Nó hiển thị **ảnh snapshot** gần nhất từ camera. Theo thuật toán ở [de-tai-nghien-cuu-canh-bao-dong-vat.md:131-170](outputs/de-tai-nghien-cuu-canh-bao-dong-vat.md#L131-L170), server AI chỉ ghi nhận snapshot mỗi 2 giây/lần khi có chuyển động đáng kể, do đó ảnh trong màn này có thể đã cũ vài giây đến vài phút tuỳ mức độ hoạt động của thú.

**Bố cục màn hình (Vertical):**

| Vị trí | Thành phần | Mô tả |
|---|---|---|
| Top bar | Nút `Back` ← `[MAIN_SCREEN]` · Tên Camera · Trạng thái online/offline · Icon `↻` Refresh. |
| Ngay dưới top bar | **Nút `✏️ Đổi tên`** *(Button text/icon)* | Mở dialog `Đổi tên camera` cho phép sửa tên hiển thị (vd: `Cam 1` → `Cam Khu A`). Có nút `Lưu` / `Huỷ`. Thay đổi lưu xuống server. |
| Giữa màn | **Khung ảnh Snapshot** + overlay timestamp `HH:mm:ss · dd/MM/yyyy` góc dưới-trái ảnh, và dòng "Cách đây X phút/giây" ở góc dưới-phải (relative time tự cập nhật mỗi 10s). | |
| Ngay dưới ảnh | **Thông báo độ mới của ảnh:** nếu > 5 phút → chip `⏰ Ảnh cách đây X phút — có thể đã cũ`; nếu > 30 phút → chip đỏ `⚠️ Ảnh cũ — kiểm tra camera`. | |
| Dưới chip | **Bảng thông tin AI:** Loài · Số lượng · Mức độ nguy hiểm · Độ tin cậy (%). | |
| Cuối màn | **Danh sách log** *(List view)* — tiêu đề `Lịch sử ghi nhận`. Mỗi dòng log gồm: ảnh thumbnail nhỏ · `giờ:phút:giây · Thứ, dd/MM/yyyy` · Độ tin cậy (%) · Loài · Số lượng. | Sắp xếp **mới nhất trên đầu**, lazy load khi cuộn. |

**Hành vi:**
- Nhấn `✏️ Đổi tên` → mở dialog cho phép sửa tên → `Lưu` cập nhật server và cập nhật lại Top bar.
- Ảnh snapshot tự động refresh mỗi ~2 giây khi AI phát hiện chuyển động đáng kể. Khi không có chuyển động → ảnh giữ nguyên cho đến khi có snapshot mới.
- Pull-to-refresh → yêu cầu lấy snapshot mới nhất từ server.
- Relative time tự cập nhật định kỳ (mỗi 10s) nhưng timestamp tuyệt đối trên ảnh chỉ thay đổi khi nhận snapshot mới.
- Banner cảnh báo có thể hiện phía trên khung ảnh khi có sự kiện mới: `Cam 1 · Phát hiện VOI · 9:04`.
- Nhấn vào 1 dòng log → mở dialog/lightbox xem ảnh lớn + metadata đầy đủ.
- Nút `Back` → `[MAIN_SCREEN]` (Android Stack mặc định).

> 🚫 Màn này **không có** nút bật/tắt stream camera, không có toggle thiết bị ngoại vi (SMS / Loa / LED / Hàng rào / Kiểm lâm). Cấu hình thiết bị ứng phó (kể cả bật/tắt thiết bị cho camera này) thuộc về `[SPECIES_CONFIG_DETAIL_SCREEN]` — mở từ nút `Thiết lập hành vi ứng phó mặc định cho tất cả camera` ở tab `[SETTING_TAB]` của `[MAIN_SCREEN]` (mục 2.3).

---

## 5. `[SPECIES_CONFIG_LIST_SCREEN]` — Danh sách loại thú cần thiết lập

Được mở khi user nhấn nút `Thiết lập hành vi ứng phó mặc định cho tất cả camera` ở tab `[SETTING_TAB]` của `[MAIN_SCREEN]` (mục 2.3).

| Thành phần | Mô tả |
|---|---|
| Tiêu đề | `Thiết lập phòng vệ theo loài` |
| Danh sách loài đã biết | Voi, Cọp, Nai, Khỉ, Heo rừng… |

**Mỗi Card loài gồm:**

| Trường | Mô tả |
|---|---|
| Tên loài | `VOI`, `CỌP`, `NAI`… |
| Chỉ số hung dữ | `0/10` - `10/10` (thang đo nguy hiểm). |
| Tập tính | Di chuyển theo bầy, hoạt động về đêm… |
| Cách phòng vệ | Mô tả ngắn kịch bản mặc định: Silent Alert / Active Deterrent. |

**Hành vi:**
- Nhấn vào 1 loài → highlight + mở `[SPECIES_CONFIG_DETAIL_SCREEN]`.

---

## 6. `[SPECIES_CONFIG_DETAIL_SCREEN]` — Thiết lập hành vi phòng vệ theo loài

**Màn này chỉ được mở qua `[SPECIES_CONFIG_LIST_SCREEN]`** (bắt buộc phải chọn loài trước). Luồng điều hướng duy nhất:

```
[MAIN_SCREEN] ── tab [SETTING_TAB] ──► nút "Thiết lập hành vi ứng phó mặc định cho tất cả camera"
                          └─► [SPECIES_CONFIG_LIST_SCREEN] ── chọn loài ──► [SPECIES_CONFIG_DETAIL_SCREEN]
                                                                         (scope cố định = ALL)
```

Mọi thông số ứng phó được thiết lập **áp dụng chung cho tất cả camera** trong hệ thống — chỉ có 1 scope duy nhất.

> 🚫 Tuyệt đối không có điều hướng nào nhảy thẳng sang `[SPECIES_CONFIG_DETAIL_SCREEN]` mà không qua `[SPECIES_CONFIG_LIST_SCREEN]` — vì cấu hình ứng phó **luôn gắn với 1 loài cụ thể**.

### 6.1. Loài đang cấu hình *(chỉ đọc)*

| Thành phần | Mô tả |
|---|---|
| Label loài | Hiển thị tên loài vừa chọn ở `[SPECIES_CONFIG_LIST_SCREEN]` (vd: `VOI`, `CỌP`, `NAI`…) — không cho chỉnh tại đây. |

### 6.2. Các nhóm cài đặt chi tiết (Defense Parameter Configurations)

**Âm thanh xua đuổi:**
- Loại âm thanh: `Tiếng súng`, `Tiếng gầm`, `Tiếng chó sủa lớn`, `Tiếng nổ giả lập`, `Tần số siêu âm`.
- Thanh trượt cường độ: `1 - 100`.
- Nút `Nghe thử (Test Audio)`.

**Đèn LED nhấp nháy:**
- Tần suất: `2 lần/giây`, `4 lần/giây`, `Nhấp nháy ngẫu nhiên`.
- Màu sắc: `Đỏ`, `Trắng`, `Đỏ xen kẽ Trắng`.
- Thời lượng (giây).

**Hàng rào điện:**
- Mức dòng điện sinh học: `Thấp`, `Trung bình`, `Mạnh`.
- Đèn cảnh báo đi kèm: Toggle bật/tắt đèn vàng/đỏ nhấp nháy.
- Cơ chế thông báo: Toggle tự động gửi SMS/Push khi hàng rào hoạt động.
- Tự ngắt: Sau **2 phút** không phát hiện thú → tự động ngắt.

**Phát cảnh báo qua loa:**
- Mẫu nội dung: `Mẫu 1 (Voi hoang dã)`, `Mẫu 2 (Thú dữ xâm lấn)`, `Mẫu 3 (Di tản lánh nạn)`.
- Giới tính giọng nói: `Nam` / `Nữ`.

**Thông báo:**
- Toggle `Gửi SMS`.
- Toggle `Gửi Push Notification`.

### 6.3. Tự thiết lập hành vi nhanh (Preset Scenarios)

| Nút preset | Hành vi |
|---|---|
| `Người lạ đột nhập` | LED đỏ-trắng nhấp nháy + âm thanh báo động + push/SMS cho cơ quan chức năng. |
| `Thú vừa` | LED nhấp nháy + âm siêu âm/chó sủa + dòng điện nhẹ (Nai, Khỉ, Hươu cao cổ). |
| `Thú cực kỳ nguy hiểm` | **Silent Alert** — không loa/đèn tại chỗ; chỉ gửi Push/SMS cho người dân di tản. |
| `Tùy chỉnh` | Mở khoá các nhóm cài đặt chi tiết phía trên để chỉnh tay. |

### 6.4. Lưu

| Thành phần | Mô tả |
|---|---|
| Nút `Lưu` | Ghi các thông số xuống server. |
| Nút `Đặt lại` | Trả về giá trị mặc định. |

---

## Phụ lục — Sơ đồ luồng chuyển màn hình

```mermaid
flowchart TD
    Login["[LOGIN_SCREEN]"] -->|"Đăng nhập OK"| Main["[MAIN_SCREEN]<br/>tab [CAMERA_LIST_TAB] mặc định"]
    Login -->|"Đăng ký"| Reg["[REGISTER_SCREEN]"]

    %% Trong [MAIN_SCREEN] chỉ tab [CAMERA_LIST_TAB] mới có danh sách camera
    subgraph MainTabs["[MAIN_SCREEN] có 3 tab"]
        TabCL["Tab [CAMERA_LIST_TAB]<br/>(có danh sách camera card)"]
        TabS["Tab [STATISTICS_TAB]<br/>(KHÔNG có camera card)"]
        TabSet["Tab [SETTING_TAB]<br/>(cài đặt cá nhân)"]
    end

    %% Nhấn thân card → mở CAMERA_VIEW_SCREEN
    TabCL -->|"Nhấn thân card"| CamView["[CAMERA_VIEW_SCREEN]"]

    %% Cấu hình ứng phó: đi từ tab SETTING → SPECIES_CONFIG_LIST → SPECIES_CONFIG_DETAIL
    SpList["[SPECIES_CONFIG_LIST_SCREEN]"]
    SpDetail["[SPECIES_CONFIG_DETAIL_SCREEN]<br/>(scope cố định = ALL)"]
    TabSet -->|"Thiết lập mặc định cho mọi camera"| SpList
    SpList -->|"Chọn loài"| SpDetail

    %% Đăng xuất thẳng về LOGIN
    TabSet -->|"Đăng xuất"| Login
```

---

> **Ghi chú tác giả:**
> - File này là đặc tả *màn hình* (UI/UX), không bao gồm API/DB chi tiết — xem thêm tài liệu kỹ thuật trong `docs/`.
> - Mọi giá trị nhấp nháy/thời lượng/tần suất có thể chỉnh trong `[SPECIES_CONFIG_DETAIL_SCREEN]`.
