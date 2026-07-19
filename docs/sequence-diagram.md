# Tài liệu Sequence Diagrams — Hệ thống Cảnh báo & Xua đuổi Động vật Hoang dã

Tài liệu này mô tả chi tiết luồng tương tác giữa các thành phần trong hệ thống (Android App, Mobile Server, Database, FCM, SMS Gateway, và Trạm Camera) được phân chia theo cấu trúc 3 cấp: **Application** -> **Screen** -> **Action**.

---

## Các Thành phần Hệ thống (Actors)

*   **Android App (Client):** Ứng dụng trên điện thoại di động dành cho Kiểm lâm và Người dân để vận hành hệ thống.
*   **Mobile Server (Backend):** Máy chủ trung tâm xử lý dữ liệu, xác thực, điều phối thông tin và quản lý cấu hình.
*   **Camera (Field Device & AI Server):** Trạm camera chụp ảnh thực tế tại hiện trường tích hợp mô hình nhận diện AI (YOLO) và các thiết bị xua đuổi vật lý.
*   **Database (DB):** Cơ sở dữ liệu PostgreSQL lưu trữ trạng thái, cấu hình và nhật ký sự kiện.
*   **FCM (Firebase Cloud Messaging):** Dịch vụ trung gian gửi thông báo đẩy (Push notification) thời gian thực đến điện thoại di động.
*   **SMS Gateway:** Hệ thống gửi tin nhắn SMS cảnh báo khẩn cấp đến các số điện thoại đã đăng ký.

---

# I. Ứng dụng Android (Android Application)

## 1. Màn hình Đăng ký (`[REGISTER_SCREEN]`)

*(Không có action load dữ liệu ban đầu)*

### 1.1. Action: Đăng ký tài khoản mới (`POST /auth/register`)
*   **Mô tả:** Người dùng nhập các thông tin đăng ký (họ tên, số điện thoại, mật khẩu, vai trò) để tạo tài khoản mới trong hệ thống.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

---

## 2. Màn hình Đăng nhập (`[LOGIN_SCREEN]`)

*(Không có action load dữ liệu ban đầu)*

### 2.1. Action: Đăng nhập hệ thống & Đăng ký Push Token (`POST /auth/login` + `POST /devices/push-token`)
*   **Mô tả:** Người dùng đăng nhập bằng số điện thoại và mật khẩu. Sau khi nhận accessToken từ server, Android Client lấy FCM Token từ Firebase SDK và tự động gửi lên server để liên kết thiết bị.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

---

## 3. Màn hình chính (`[MAIN_SCREEN]`)

### 3.1. Tab Danh sách Camera (`[CAMERA_LIST_TAB]`)

#### 3.1.1. Action: Load danh sách trạm camera & snapshot ban đầu (`GET /cameras`)
*   **Mô tả:** Khi mở tab hoặc vào màn hình chính, app tự động gọi API lấy danh sách các trạm camera trực thuộc quyền quản lý kèm theo trạng thái hoạt động và ảnh snapshot thumbnail gần nhất để hiển thị.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

#### 3.1.2. Action: Đẩy cảnh báo khẩn cấp thời gian thực qua Server-Sent Events (SSE)
*   **Mô tả:** SSE stream nhận diện realtime chạy ngầm liên tục đẩy tin tức cảnh báo khẩn cấp để app hiển thị `emergency_banner_container` nổi lên ở đầu danh sách camera.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 3.2. Tab Thống kê (`[STATISTICS_TAB]`)

#### 3.2.1. Action: Load dữ liệu biểu đồ thống kê & feed tin tức (`GET /stats/summary` + `GET /alerts/feed` + `GET /reference-data/danger-levels`)
*   **Mô tả:** Khi chuyển sang tab Thống kê, app thực hiện tải dữ liệu để vẽ biểu đồ tần suất phát hiện, sơ đồ nhiệt (heatmap) phân bố động vật và danh sách tin tức cảnh báo của các cơ quan kiểm lâm khu vực.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 3.3. Tab Cài đặt (`[SETTING_TAB]`)

#### 3.3.1. Action: Load thông tin cá nhân của người dùng (`GET /users/me`)
*   **Mô tả:** Tải thông tin tài khoản hiện tại (họ tên, vai trò, số điện thoại đăng nhập) và các thiết lập preferences chuông báo để hiển thị lên form cài đặt chung.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

#### 3.3.2. Action: Đăng xuất tài khoản (`POST /auth/logout` + `DELETE /devices/push-token`)
*   **Mô tả:** Người dùng nhấn nút Đăng xuất, app gửi yêu cầu hủy session trên server, đồng thời hủy FCM Push Token trên thiết bị để ngưng nhận thông báo và đưa người dùng trở lại màn hình đăng nhập.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

---

## 4. Màn hình Chi tiết Camera (`[CAMERA_VIEW_SCREEN]`)

### 4.1. Action: Load chi tiết trạm camera, snapshot & lịch sử nhật ký (`GET /cameras/{id}` + `GET /cameras/{id}/snapshot` + `GET /cameras/{id}/detections/current` + `GET /events`)
*   **Mô tả:** Khi người dùng chọn một camera, app sẽ đồng thời tải: thông tin chi tiết trạm (liveFeedUrl, deviceStates), ảnh snapshot chất lượng cao ở nửa trên màn hình, thông tin nhận diện động vật hiện tại của trạm và danh sách lịch sử log ghi nhận của camera đó.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 4.2. Action: Yêu cầu "Làm mới" Snapshot thủ công (`POST /cameras/{cameraId}/snapshot/refresh`)
*   **Mô tả:** Người dùng thực hiện vuốt kéo hoặc nhấn nút làm mới để yêu cầu camera hiện trường chụp ảnh ngay lập tức và trả về điện thoại.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 4.3. Action: Điều khiển ghi đè thủ công thiết bị ngoại vi (`POST /cameras/{cameraId}/devices/{key}/override` hoặc `override-all`)
*   **Mô tả:** Người dùng bật/tắt thủ công nhanh còi, LED, hàng rào điện sinh học hoặc kích hoạt báo động khẩn cấp toàn trạm.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 4.4. Action: Thay đổi tên hiển thị của trạm camera (`PATCH /cameras/{cameraId}`)
*   **Mô tả:** Người dùng bấm nút sửa tên hiển thị trên màn hình chi tiết, nhập tên mới và lưu lại để đồng bộ lên DB.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 4.5. Action: Tải lên hình ảnh snapshot thủ công (`POST /cameras/{cameraId}/snapshots`)
*   **Mô tả:** Người dùng chọn tải lên một hình ảnh hiện trường tự chụp bằng điện thoại kèm các thông tin chi tiết (url, deviceId, userId, thời gian).
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

---

## 5. Màn hình Danh sách Cấu hình Loài (`[SPECIES_CONFIG_LIST_SCREEN]`)

### 5.1. Action: Load danh sách loài & tổng quan cấu hình đang áp dụng (`GET /species` + `GET /response-configs/{cam}`)
*   **Mô tả:** Khi người dùng mở màn hình thiết lập ứng phó mặc định, app tải danh sách toàn bộ các loài động vật trong hệ thống (kèm chỉ số hung dữ, đặc tính htmlDescription) và tình trạng cấu hình tương ứng đang được áp dụng tại trạm camera đã chọn.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

---

## 6. Màn hình Thiết lập Phòng vệ theo Loài (`[SPECIES_CONFIG_DETAIL_SCREEN]`)

### 6.1. Action: Load cấu hình hiện tại của loài & danh mục dữ liệu mẫu (`GET /response-configs?cameraId=&speciesId=` + `GET /control/presets` + `GET /audio-samples` + `GET /speaker-templates`)
*   **Mô tả:** Khi chọn một loài để cấu hình chi tiết, app tải cấu hình phòng vệ hiện tại đang lưu trên DB, đồng thời tải danh sách 3 preset phòng vệ mẫu, danh sách âm thanh mẫu và mẫu nội dung phát loa để phục vụ dropdown lựa chọn của người dùng.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 6.2. Action: Lưu cấu hình ứng phó tự chỉnh của loài (`PUT /response-configs/{cam}/{species}` hoặc `DELETE ...`)
*   **Mô tả:** Người dùng tùy biến các tham số (âm thanh, đèn LED nháy, cấp độ hàng rào điện, mẫu phát loa, chế độ silent) cho một loài động vật cụ thể và nhấn Lưu cấu hình hoặc Đặt lại về mặc định.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 6.3. Action: Phát âm thanh test thử loa tại trạm hiện trường (`POST /cameras/{cameraId}/devices/{key}/test`)
*   **Mô tả:** Người dùng chọn loại âm thanh còi báo và nhấn "Nghe thử" để phát thử nghiệm trực tiếp tại hiện trường nhằm căn chỉnh âm lượng.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

---

## 7. Màn hình Quản lý SĐT Nhận Cảnh Báo (`[SMS_CONFIG_SCREEN]`)

### 7.1. Action: Load danh sách số điện thoại nhận SMS bổ sung (`GET /users/me/sms-recipients`)
*   **Mô tả:** Tải danh sách tối đa 3 số điện thoại đăng ký nhận cảnh báo bổ sung khi mở màn hình quản lý SMS.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

### 7.2. Action: Thêm / Xóa số điện thoại nhận tin nhắn SMS (`POST /users/me/sms-recipients` hoặc `DELETE ...`)
*   **Mô tả:** Người dùng thực hiện thêm số điện thoại mới (qua dialog) hoặc nhấn xóa một số điện thoại khỏi danh sách nhận cảnh báo.
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*

---

# II. Thiết bị Camera & AI Server (Server AI / Field Device)

## 1. Không phân chia màn hình (Thực thi ngầm & Tích hợp)

### 1.1. Action: Gửi hình ảnh snapshot và phán đoán nhận dạng của AI Server (`POST /cameras/{cameraId}/detections`)
*   **Mô tả:** Khi phát hiện có động vật hoặc chuyển động bất thường, Camera/AI Server tải hình ảnh lên Mobile Server, nhận cấu hình phòng vệ "@DefendAction" phản hồi để thực thi loa/LED/hàng rào tại chỗ, đồng thời kích hoạt cảnh báo đa kênh đến người dân (SMS/Push/SSE).
*   **Trạng thái nháp:** *(Đang chờ cùng người dùng xây dựng)*
