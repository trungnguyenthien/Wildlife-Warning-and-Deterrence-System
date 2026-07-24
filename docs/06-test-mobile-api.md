# Đặc tả Kịch bản Kiểm thử API Di động (Mobile API Test Specification)

Tài liệu này định nghĩa chi tiết danh sách các ca kiểm thử (testcases) tự động cho toàn bộ 37 API di động và 2 API tích hợp thiết bị được mô tả trong [03-mobile_api.md](03-mobile_api.md). Tài liệu giúp học sinh và AI Agent có căn cứ thiết lập Jest + Supertest tự động chạy lặp để đảm bảo tính đúng đắn của Backend.

---

## 1. Nguyên tắc Thiết kế và Viết Ca kiểm thử (Test Design Principles)

Để đảm bảo tính độc lập, bao phủ tốt và tự động hóa dễ dàng bằng Jest + Supertest, toàn bộ các ca kiểm thử trong tài liệu này tuân thủ các nguyên tắc thiết kế sau:

*   **Mã định danh duy nhất (Unique Case ID):** Mỗi testcase được đặt mã ID chuẩn hóa dạng `TC_[NHÓM]_[SUCCESS/FAILURE]_[SỐ_THỨ_TỰ]` giúp dễ dàng phân loại và theo dõi (ví dụ: `TC_AUTH_REG_SUCCESS_01`, `TC_AUTH_REG_FAILURE_01`).
*   **Tiêu đề Tiếng Anh & Nội dung Tiếng Việt:** Tiêu đề của testcase được viết bằng **tiếng Anh** (thuận tiện cho việc đặt tên hàm/khối `it()` hoặc `test()` trong code Jest). Tất cả thông tin mô tả chi tiết, payload dữ liệu gửi đi, kết quả mong đợi và bước kiểm tra DB đều được viết bằng **tiếng Việt** giúp học sinh dễ đọc hiểu.
*   **Tách biệt hoàn toàn các ca kiểm thử lỗi (Isolated Invalidation):**
    *   *Không gom chung lỗi thiếu thông tin bắt buộc (Missing required fields):* Nếu một request yêu cầu nhiều trường bắt buộc, việc kiểm thử thiếu trường phải được phân tách thành từng ca kiểm thử độc lập cho mỗi trường cụ thể.
    *   *Không gom chung lỗi định dạng sai chuẩn (Invalid formats):* Lỗi sai định dạng số điện thoại, ngày tháng, hay sai kiểu dữ liệu của mỗi trường phải được test riêng biệt.
    *   *Không gom chung lỗi vi phạm ràng buộc nghiệp vụ (Business logic/Bounds validation):* Lỗi logic (như ngày bắt đầu lớn hơn ngày kết thúc, giá trị vượt quá khoảng giới hạn 0-100%, hoặc đăng ký vượt hạn mức DB) được kiểm thử trong các ca riêng biệt.
*   **Một testcase chỉ kiểm tra một lỗi duy nhất (Single Responsibility Test):** Tránh thiết kế các ca kiểm thử "phức hợp" kiểm tra nhiều lỗi đồng thời trên cùng một request payload để đảm bảo khi test báo đỏ, lập trình viên/AI xác định ngay lập tức nguyên nhân lỗi cụ thể.
*   **Quản lý Vòng đời Testcase bằng thẻ `[DELETED]` (Soft Delete):** Khi một kịch bản kiểm thử không còn phù hợp và cần được loại bỏ, **không được xóa bỏ hoàn toàn dòng/mã của testcase đó** khỏi tài liệu. Hãy thêm tiền tố `[DELETED]` vào ngay trước mã định danh ID (ví dụ: `[DELETED] TC_AUTH_REG_SUCCESS_02`). Việc này giúp giữ nguyên hệ thống đánh số ID và giúp AI Agent nhận biết chính xác để xóa bỏ testcase tương ứng trong code hoặc cập nhật nhanh chóng.
*   **Phản hồi Lỗi Chuẩn hóa (Standardized Error Responses):** Tất cả các kết quả mong đợi của ca kiểm thử lỗi (Validation Failures, Bad Requests) phải có cấu trúc phản hồi dạng: `{ error: string, message: string }`. Trường `error` chứa mã lỗi snake_case cụ thể mô tả rõ bản chất lỗi (ví dụ: `missed_username`, `missed_password`, `invalid_phone_number`, `invalid_led_color`, `limit_reached`, `not_found`, `forbidden`) chứ không chỉ trả về mã HTTP và thông báo chung chung.
*   **Không Assert Nội dung Thông báo Lỗi (No Message Content Assertion):** Trong các ca kiểm thử thất bại, mã kiểm thử Jest chỉ cần xác minh mã lỗi `error` (`expect(res.body.error).toBe(...)`) và mã trạng thái HTTP, tuyệt đối **không** kiểm tra/so khớp nội dung chi tiết của chuỗi thông báo lỗi `message` (ví dụ không sử dụng `toContain` hay `toBe` với `res.body.message`). Điều này nhằm tránh việc kiểm thử bị lỗi/đỏ không đáng có khi có thay đổi nhỏ về câu chữ hoặc bản dịch của thông báo lỗi.

---

## 2. Cơ chế Kiểm thử chung (Global Test Context)

*   **Xác thực (Authentication):** Trừ các API Auth (`/auth/login`, `/auth/register`) và API Health (`/health`), tất cả các API còn lại đều yêu cầu header:
    ```http
    Authorization: Bearer <token>
    ```
    *Nếu thiếu hoặc token không khớp với bất kỳ bản ghi nào trong bảng `push_tokens`, API phải trả về mã lỗi tương ứng (`401 Unauthorized` hoặc `403 Forbidden`).*
*   **Database Isolation (Cô lập dữ liệu):** Dữ liệu trong cơ sở dữ liệu (`wildlife_test`) sẽ được làm sạch (truncate) tự động trước và sau khi thực thi mỗi bộ test suite để tránh làm bẩn dữ liệu chéo.
*   **Thời gian thực:** Các trường định dạng thời gian (`capturedAt`, `detectedAt`, `createdAt`, `updatedAt`) phải được định dạng theo chuẩn ISO 8601 (vd: `2026-07-22T10:45:00+07:00`).

---

## 3. Chi tiết các Ca kiểm thử (Test Cases)

### 2.1. Nhóm Auth & Đăng nhập (Màn hình `[LOGIN_SCREEN]` / `[REGISTER_SCREEN]`)

#### 1. `POST /auth/register` (Đăng ký tài khoản)
*   **TC_AUTH_REG_SUCCESS_01: Register successfully with valid payload**
    *   **Mô tả:** Đăng ký người dùng mới thành công với các thông tin hợp lệ.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen", "password": "password123", "fullName": "Nguyen Ranger", "phoneNumber": "+84901234567", "role": "RANGER" }`
    *   **Kết quả mong đợi (Expected Response):** `201 Created` trả về đối tượng người dùng (không bao gồm mật khẩu), chứa các trường `id` và `createdAt`.
    *   **Xác thực Database (DB Verification):** Bản ghi người dùng mới tồn tại trong bảng `users`, mật khẩu được mã hóa an toàn bằng bcrypt.
*   **TC_AUTH_REG_FAILURE_01: Fail to register with a duplicate username**
    *   **Mô tả:** Lỗi đăng ký khi username đã tồn tại trong hệ thống.
    *   **Điều kiện trước:** Tài khoản `ranger_tuyen` đã tồn tại.
    *   **Dữ liệu gửi đi (Request Body):** Đăng ký lại với username `ranger_tuyen`.
    *   **Kết quả mong đợi (Expected Response):** `409 Conflict` kèm thông báo lỗi trùng tài khoản.
*   **TC_AUTH_REG_FAILURE_02: Fail to register due to missing username**
    *   **Mô tả:** Lỗi đăng ký khi thiếu trường username.
    *   **Dữ liệu gửi đi (Request Body):** `{ "password": "password123", "fullName": "Nguyen Ranger", "phoneNumber": "+84901234567", "role": "RANGER" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm chi tiết lỗi validation.
*   **TC_AUTH_REG_FAILURE_03: Fail to register due to missing password**
    *   **Mô tả:** Lỗi đăng ký khi thiếu trường password.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen2", "fullName": "Nguyen Ranger", "phoneNumber": "+84901234567", "role": "RANGER" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm chi tiết lỗi validation.
*   **TC_AUTH_REG_FAILURE_04: Fail to register due to missing fullName**
    *   **Mô tả:** Lỗi đăng ký khi thiếu trường fullName.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen2", "password": "password123", "phoneNumber": "+84901234567", "role": "RANGER" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm chi tiết lỗi validation.
*   **TC_AUTH_REG_FAILURE_05: Fail to register due to missing phoneNumber**
    *   **Mô tả:** Lỗi đăng ký khi thiếu trường phoneNumber.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen2", "password": "password123", "fullName": "Nguyen Ranger", "role": "RANGER" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm chi tiết lỗi validation.
*   **TC_AUTH_REG_FAILURE_06: Fail to register due to missing role**
    *   **Mô tả:** Lỗi đăng ký khi thiếu trường role.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen2", "password": "password123", "fullName": "Nguyen Ranger", "phoneNumber": "+84901234567" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm chi tiết lỗi validation.
*   **TC_AUTH_REG_FAILURE_07: Fail to register with invalid format phone number**
    *   **Mô tả:** Số điện thoại truyền vào sai cấu trúc định dạng E.164.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen2", "password": "password123", "fullName": "Nguyen Ranger", "phoneNumber": "09012345", "role": "RANGER" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm thông báo định dạng SĐT không hợp lệ (phải bắt đầu bằng dấu + và mã quốc gia).
*   **TC_AUTH_REG_FAILURE_08: Fail to register with password too short**
    *   **Mô tả:** Mật khẩu truyền vào quá ngắn, vi phạm ràng buộc bảo mật.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen2", "password": "123", "fullName": "Nguyen Ranger", "phoneNumber": "+84901234567", "role": "RANGER" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm thông báo mật khẩu phải chứa ít nhất 6 ký tự.
*   **TC_AUTH_REG_FAILURE_09: Fail to register with non-existent system role**
    *   **Mô tả:** Đăng ký thất bại khi truyền vai trò (role) không được hệ thống hỗ trợ.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen2", "password": "password123", "fullName": "Nguyen Ranger", "phoneNumber": "+84901234567", "role": "SUPER_ADMIN" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` (Vai trò phải là RANGER, CITIZEN, BORDER_GUARD hoặc HIGHWAY_ADMIN).

#### 2. `POST /auth/login` (Đăng nhập tài khoản)
*   **TC_AUTH_LOG_SUCCESS_01: Login successfully with correct credentials**
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen", "password": "password123" }`
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về `token` và vai trò `role` của tài khoản.
    *   **Xác thực Database (DB Verification):** Một token mới được tạo ra và lưu trữ thành công trong bảng `push_tokens` liên kết với người dùng.
*   **TC_AUTH_LOG_FAILURE_01: Fail to login with incorrect password**
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen", "password": "wrongpassword" }`
    *   **Kết quả mong đợi (Expected Response):** `401 Unauthorized` kèm thông báo sai thông tin tài khoản hoặc mật khẩu.
*   **TC_AUTH_LOG_FAILURE_02: Fail to login due to missing username**
    *   **Mô tả:** Lỗi đăng nhập khi không truyền username.
    *   **Dữ liệu gửi đi (Request Body):** `{ "password": "password123" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm lỗi validation.
*   **TC_AUTH_LOG_FAILURE_03: Fail to login due to missing password**
    *   **Mô tả:** Lỗi đăng nhập khi không truyền password.
    *   **Dữ liệu gửi đi (Request Body):** `{ "username": "ranger_tuyen" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm lỗi validation.

#### 3. `POST /auth/logout` (Đăng xuất tài khoản)
*   **TC_AUTH_OUT_SUCCESS_01: Logout successfully with a valid token**
    *   **Header:** `Authorization: Bearer <valid_token>`
    *   **Kết quả mong đợi (Expected Response):** `200 OK` kèm thông báo đăng xuất thành công.
    *   **Xác thực Database (DB Verification):** Token tương ứng bị xóa sạch khỏi bảng `push_tokens`.
*   **TC_AUTH_OUT_FAILURE_01: Fail to logout with missing authorization header**
    *   **Mô tả:** Truy cập bị chặn do thiếu token xác thực.
    *   **Header:** Trống.
    *   **Kết quả mong đợi (Expected Response):** `401 Unauthorized`.
*   **TC_AUTH_OUT_FAILURE_02: Fail to logout with invalid format token**
    *   **Mô tả:** Truy cập bị từ chối do token sai cấu trúc.
    *   **Header:** `Authorization: Bearer short_tok`
    *   **Kết quả mong đợi (Expected Response):** `403 Forbidden` hoặc `401 Unauthorized`.

---

### 2.2. Nhóm Tab danh sách Camera (`[CAMERA_LIST_TAB]` / `[CAMERA_VIEW_SCREEN]`)

#### 4. `GET /cameras` (Lấy danh sách trạm camera)
*   **TC_CAM_LIST_SUCCESS_01: Retrieve all camera stations successfully**
    *   **Điều kiện trước:** Có các trạm camera đang hoạt động trong DB.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về mảng danh sách camera. Mỗi camera phải có đầy đủ các trường: `id`, `name`, `location` (`lat`, `lng`, `address`), `status`, `liveFeedUrl`, `snapshot` (`url`, `capturedAt`).
*   **TC_CAM_LIST_SUCCESS_02: Retrieve empty list when no camera stations exist**
    *   **Điều kiện trước:** Không có camera nào trong cơ sở dữ liệu.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về mảng trống `[]`.
*   **TC_CAM_LIST_FAILURE_01: Fail to list cameras due to invalid token**
    *   **Mô tả:** Truy cập bị từ chối do sử dụng token không tồn tại.
    *   **Header:** `Authorization: Bearer token_not_in_db`
    *   **Kết quả mong đợi (Expected Response):** `403 Forbidden`.

#### 5. `GET /cameras/{cameraId}` (Lấy chi tiết 1 camera)
*   **TC_CAM_DET_SUCCESS_01: Retrieve camera station details successfully**
    *   **Điều kiện trước:** Trạm camera `cam-001` tồn tại.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` chứa chi tiết camera và trường lồng `currentDetection` (chứa thông tin sự kiện nhận dạng AI gần nhất bao gồm `eventId`, `detections`, `detectedAt`).
*   **TC_CAM_DET_FAILURE_01: Fail to retrieve non-existent camera station**
    *   **Mô tả:** Không tìm thấy dữ liệu khi truyền mã camera không tồn tại.
    *   **Đường dẫn gọi:** `/cameras/cam-999`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found`.
*   **TC_CAM_DET_FAILURE_02: Fail to retrieve details with invalid format cameraId**
    *   **Mô tả:** Mã camera truyền quá dài hoặc chứa ký tự đặc biệt không cho phép (Vượt giới hạn 50 ký tự trong schema).
    *   **Đường dẫn gọi:** `/cameras/cam-very-long-id-that-exceeds-fifty-characters-limit-12345`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` hoặc `404 Not Found`.

#### 6. `PATCH /cameras/{cameraId}` (Đổi tên hiển thị camera)
*   **TC_CAM_REN_SUCCESS_01: Rename camera station successfully**
    *   **Điều kiện trước:** Camera `cam-001` tồn tại. Người dùng có vai trò `RANGER`.
    *   **Dữ liệu gửi đi (Request Body):** `{ "name": "Rìa Rừng Phía Bắc" }`
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về thông tin camera với tên mới.
    *   **Xác thực Database (DB Verification):** Trường `name` của `cam-001` trong bảng `cameras` được cập nhật thành "Rìa Rừng Phía Bắc".
*   **TC_CAM_REN_FAILURE_01: Fail to rename with empty name payload**
    *   **Mô tả:** Không cho phép cập nhật nếu trường tên camera bị bỏ trống.
    *   **Dữ liệu gửi đi (Request Body):** `{ "name": "" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm chi tiết lỗi validation.
*   **TC_CAM_REN_FAILURE_02: Fail to rename with invalid format name length**
    *   **Mô tả:** Tên camera quá dài vượt quá giới hạn thiết kế 100 ký tự.
    *   **Dữ liệu gửi đi (Request Body):** `{ "name": "Ten camera rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai rat dai" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CAM_REN_FAILURE_03: Fail to rename camera for non-existent cameraId**
    *   **Mô tả:** Không thể đổi tên camera không tồn tại trong hệ thống.
    *   **Đường dẫn gọi:** `/cameras/cam-not-exist`
    *   **Dữ liệu gửi đi (Request Body):** `{ "name": "Cam 1 New" }`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found`.

#### 7. `GET /cameras/stream` (Nhận cập nhật qua kết nối SSE)
*   **TC_CAM_SSE_SUCCESS_01: Establish SSE stream connection successfully**
    *   **Header:** `Accept: text/event-stream`
    *   **Kết quả mong đợi (Expected Response):** `200 OK` thiết lập kết nối luồng sự kiện (Keep-Alive) với Content-Type `text/event-stream`.
*   **TC_CAM_SSE_FAILURE_01: Fail to establish SSE connection without valid token**
    *   **Mô tả:** Bị chặn khi kết nối luồng cập nhật không cung cấp token hợp lệ.
    *   **Header:** Trống hoặc token sai.
    *   **Kết quả mong đợi (Expected Response):** `401 Unauthorized` hoặc `403 Forbidden`.

#### 7b. `POST /cameras/{cameraId}/snapshots` (Tải lên hình ảnh snapshot thủ công)
*   **TC_SNAP_UPL_SUCCESS_01: Upload manual camera snapshot successfully to Cloudinary**
    *   **Mô tả:** Tải lên hình ảnh snapshot chụp thủ công lên Cloudinary thành công và tạo bản ghi lưu trữ trong DB.
    *   **Dữ liệu gửi đi (Multipart/form-data):** `image` (tệp tin f4.png), `userId` (mã uuid người dùng hợp lệ).
    *   **Kết quả mong đợi (Expected Response):** `201 Created` trả về ID ảnh, url Cloudinary, deviceId, userId, và thời điểm tải lên.
    *   **Xác thực Database (DB Verification):** Một bản ghi mới được tạo trong bảng `snapshots` chứa đúng url và deviceId/userId.
*   **TC_SNAP_UPL_FAILURE_01: Fail to upload manual camera snapshot due to missing image file**
    *   **Mô tả:** Gửi yêu cầu tải ảnh thủ công nhưng không đính kèm file ảnh.
    *   **Dữ liệu gửi đi (Multipart/form-data):** `userId` (uuid người dùng hợp lệ).
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm mã lỗi `missed_image`.
*   **TC_SNAP_UPL_FAILURE_02: Fail to upload manual camera snapshot with non-existent userId**
    *   **Mô tả:** Tải ảnh lên với mã userId không tồn tại trong hệ thống.
    *   **Dữ liệu gửi đi (Multipart/form-data):** `image` (tệp tin f4.png), `userId` = "non-existent-user-uuid".
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found` kèm mã lỗi `not_found_user`.
*   **TC_SNAP_UPL_FAILURE_03: Fail to upload manual camera snapshot for non-existent cameraId**
    *   **Mô tả:** Tải ảnh lên với mã cameraId của camera không tồn tại trong hệ thống.
    *   **Dữ liệu gửi đi (Multipart/form-data):** `image` (tệp tin f4.png), `userId` (uuid người dùng hợp lệ).
    *   **Đường dẫn gọi:** `/cameras/CAM_NON_EXIST_9999/snapshots`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found` kèm mã lỗi `not_found_camera`.

---

### 2.3. Nhóm Tab thống kê & Bản tin cảnh báo (`[STATISTICS_TAB]`)

#### 8. `GET /stats/summary` (Tải dữ liệu biểu đồ và bản đồ nhiệt)
*   **TC_STAT_SUM_SUCCESS_01: Retrieve statistics summary successfully with valid dates**
    *   **Tham số truy vấn (Query):** `startDate=2026-07-15&endDate=2026-07-22`
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về dữ liệu `trendData`, `speciesBreakdown` và `heatmapData`.
*   **TC_STAT_SUM_FAILURE_01: Fail to retrieve statistics due to missing startDate**
    *   **Mô tả:** Thiếu tham số ngày bắt đầu.
    *   **Tham số truy vấn (Query):** `endDate=2026-07-22`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_STAT_SUM_FAILURE_02: Fail to retrieve statistics due to missing endDate**
    *   **Mô tả:** Thiếu tham số ngày kết thúc.
    *   **Tham số truy vấn (Query):** `startDate=2026-07-15`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_STAT_SUM_FAILURE_03: Fail to retrieve statistics with invalid format startDate**
    *   **Mô tả:** Ngày bắt đầu truyền sai định dạng ISO 8601.
    *   **Tham số truy vấn (Query):** `startDate=15-07-2026&endDate=2026-07-22`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_STAT_SUM_FAILURE_04: Fail to retrieve statistics with invalid format endDate**
    *   **Mô tả:** Ngày kết thúc truyền sai định dạng ISO 8601.
    *   **Tham số truy vấn (Query):** `startDate=2026-07-15&endDate=22-07-2026`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_STAT_SUM_FAILURE_05: Fail to retrieve statistics when startDate is after endDate**
    *   **Mô tả:** Lỗi logic khi ngày bắt đầu thống kê lớn hơn ngày kết thúc.
    *   **Tham số truy vấn (Query):** `startDate=2026-07-22&endDate=2026-07-15`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.

#### 9. `GET /alerts/feed` (Lấy luồng tin tức cảnh báo khẩn cấp phân vai trò)
*   **TC_ALT_FEED_SUCCESS_01: Retrieve weekly alert feed for RANGER including sensitive reports**
    *   **Điều kiện trước:** Tồn tại tin tức cảnh báo biên giới `HUMAN_BORDER` nhạy cảm. Người dùng đang đăng nhập có vai trò `RANGER`.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` chứa đầy đủ danh sách cảnh báo bao gồm cả tin biên giới.
*   **TC_ALT_FEED_SUCCESS_02: Retrieve weekly alert feed for CITIZEN excluding border control alerts**
    *   **Điều kiện trước:** Tồn tại tin tức cảnh báo biên giới `HUMAN_BORDER`. Người dùng đăng nhập có vai trò `CITIZEN`.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` chỉ hiển thị các tin cảnh báo công cộng, không hiển thị tin biên giới `HUMAN_BORDER`.
*   **TC_ALT_FEED_FAILURE_01: Fail to retrieve weekly alert feed with invalid pagination page**
    *   **Mô tả:** Số trang phân trang sai định dạng (ví dụ truyền số âm).
    *   **Tham số truy vấn (Query):** `page=-5&size=10`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` hoặc server tự chuyển về mặc định page = 1.
*   **TC_ALT_FEED_FAILURE_02: Fail to retrieve weekly alert feed with invalid pagination size**
    *   **Mô tả:** Số phần tử trên mỗi trang sai định dạng (ví dụ truyền chữ).
    *   **Tham số truy vấn (Query):** `page=1&size=abc`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` hoặc server tự chuyển về mặc định size = 10.

---

### 2.4. Nhóm Cấu hình phòng vệ (`[SPECIES_CONFIG_LIST_SCREEN]` / `[SPECIES_CONFIG_DETAIL_SCREEN]`)

#### 10. `GET /species` (Tải danh sách danh mục loài)
*   **TC_SPC_LIST_SUCCESS_01: Retrieve species directory successfully**
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về mảng danh mục các loài.

#### 11. `GET /response-configs/{cameraId}` (Tải tất cả cấu hình đang áp dụng tại camera)
*   **TC_CFG_LIST_SUCCESS_01: Retrieve all active custom config list for a camera**
    *   **Điều kiện trước:** Camera `cam-001` có cấu hình tùy chỉnh cho các loài trong bảng `response_configs`.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` chứa danh sách cấu hình.
*   **TC_CFG_LIST_FAILURE_01: Fail to retrieve config list for non-existent camera**
    *   **Mô tả:** Mã camera truyền vào không tồn tại trong hệ thống.
    *   **Đường dẫn gọi:** `/response-configs/cam-not-exist`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found`.

#### 12. `GET /response-configs?cameraId=&speciesId=` (Tải cấu hình chi tiết của 1 loài tại camera)
*   **TC_CFG_DET_SUCCESS_01: Retrieve custom configuration successfully when custom record exists**
    *   **Điều kiện trước:** Tồn tại cấu hình tùy chọn cho cặp `(cam-001, elephant)`.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về các thông số tùy chỉnh đã cấu hình.
*   **TC_CFG_DET_SUCCESS_02: Fallback to danger level presets when no custom record exists**
    *   **Điều kiện trước:** Không tồn tại cấu hình tùy chọn cho `(cam-001, elephant)`.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về các thông số mặc định của hệ thống dựa trên Preset Danger Level tương ứng của loài Voi.
*   **TC_CFG_DET_FAILURE_01: Fail to retrieve config when cameraId query parameter is missing**
    *   **Mô tả:** Thiếu tham số mã camera.
    *   **Tham số truy vấn (Query):** `speciesId=elephant`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_DET_FAILURE_02: Fail to retrieve config when speciesId query parameter is missing**
    *   **Mô tả:** Thiếu tham số mã loài.
    *   **Tham số truy vấn (Query):** `cameraId=cam-001`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_DET_FAILURE_03: Fail to retrieve config when speciesId is invalid**
    *   **Mô tả:** Truyền mã loài không tồn tại trong danh mục loài.
    *   **Tham số truy vấn (Query):** `cameraId=cam-001&speciesId=dragon`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found`.

#### 13. `PUT /response-configs/{cameraId}/{speciesId}` (Lưu cấu hình phòng vệ tự chọn)
*   **TC_CFG_SAVE_SUCCESS_01: Save custom configuration successfully**
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": true, "ledColor": "RED", "ledIntensity": 90, "speakerWarn": true, "audioSampleId": "A_gunshot", "audioIntensity": 85, "electricFence": true, "electricFenceDuration": 15, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `200 OK` hoặc `201 Created` trả về cấu hình đã lưu.
    *   **Xác thực Database (DB Verification):** Bản ghi được lưu/cập nhật thành công trong bảng `response_configs` với trường `last_modified_by` tự động cập nhật khớp với ID của user thực hiện.
*   **TC_CFG_SAVE_FAILURE_01: Fail to save config due to missing ledFlash**
    *   **Mô tả:** Thiếu trường ledFlash.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledColor": "RED", "ledIntensity": 90, "speakerWarn": true, "audioSampleId": "A_gunshot", "audioIntensity": 85, "electricFence": true, "electricFenceDuration": 15, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_02: Fail to save config due to missing speakerWarn**
    *   **Mô tả:** Thiếu trường speakerWarn.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": true, "ledColor": "RED", "ledIntensity": 90, "audioSampleId": "A_gunshot", "audioIntensity": 85, "electricFence": true, "electricFenceDuration": 15, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_03: Fail to save config due to missing electricFence**
    *   **Mô tả:** Thiếu trường electricFence.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": true, "ledColor": "RED", "ledIntensity": 90, "speakerWarn": true, "audioSampleId": "A_gunshot", "audioIntensity": 85, "electricFenceDuration": 15, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_04: Fail to save config due to missing silentAlert**
    *   **Mô tả:** Thiếu trường silentAlert.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": true, "ledColor": "RED", "ledIntensity": 90, "speakerWarn": true, "audioSampleId": "A_gunshot", "audioIntensity": 85, "electricFence": true, "electricFenceDuration": 15 }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_05: Fail to save config with invalid ledColor enum**
    *   **Mô tả:** Cường độ Led Color không thuộc Enum màu LED hỗ trợ (RED, YELLOW, WHITE, STROBE).
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": true, "ledColor": "BLUE", "ledIntensity": 90, "speakerWarn": false, "audioSampleId": "A_gunshot", "audioIntensity": 0, "electricFence": false, "electricFenceDuration": 0, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_06: Fail to save config with ledIntensity too high**
    *   **Mô tả:** Cường độ LED lớn hơn 100%.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": true, "ledColor": "RED", "ledIntensity": 150, "speakerWarn": false, "audioSampleId": "A_gunshot", "audioIntensity": 0, "electricFence": false, "electricFenceDuration": 0, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_07: Fail to save config with ledIntensity negative**
    *   **Mô tả:** Cường độ LED nhỏ hơn 0%.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": true, "ledColor": "RED", "ledIntensity": -10, "speakerWarn": false, "audioSampleId": "A_gunshot", "audioIntensity": 0, "electricFence": false, "electricFenceDuration": 0, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_08: Fail to save config with audioIntensity too high**
    *   **Mô tả:** Cường độ âm thanh loa lớn hơn 100%.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": false, "ledColor": "RED", "ledIntensity": 0, "speakerWarn": true, "audioSampleId": "A_gunshot", "audioIntensity": 120, "electricFence": false, "electricFenceDuration": 0, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_09: Fail to save config with audioIntensity negative**
    *   **Mô tả:** Cường độ âm thanh loa nhỏ hơn 0%.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": false, "ledColor": "RED", "ledIntensity": 0, "speakerWarn": true, "audioSampleId": "A_gunshot", "audioIntensity": -5, "electricFence": false, "electricFenceDuration": 0, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_CFG_SAVE_FAILURE_10: Fail to save config with electricFenceDuration negative**
    *   **Mô tả:** Thời gian sốc hàng rào điện nhỏ hơn 0.
    *   **Dữ liệu gửi đi (Request Body):** `{ "ledFlash": false, "ledColor": "RED", "ledIntensity": 0, "speakerWarn": false, "audioSampleId": "A_gunshot", "audioIntensity": 0, "electricFence": true, "electricFenceDuration": -10, "silentAlert": false }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.

#### 14. `DELETE /response-configs/{cameraId}/{speciesId}` (Xóa cấu hình tự chọn để quay về mặc định)
*   **TC_CFG_DEL_SUCCESS_01: Reset custom configuration back to default**
    *   **Điều kiện trước:** Tồn tại bản ghi cấu hình tùy chỉnh cho `(cam-001, elephant)`.
    *   **Kết quả mong đợi (Expected Response):** `204 No Content` hoặc `200 OK`.
    *   **Xác thực Database (DB Verification):** Bản ghi tùy chỉnh bị xóa khỏi bảng `response_configs`.
*   **TC_CFG_DEL_FAILURE_01: Fail to delete non-existent custom configuration**
    *   **Mô tả:** Trả về lỗi khi yêu cầu xóa cấu hình tùy chỉnh của cặp camera/loài chưa từng được cấu hình.
    *   **Đường dẫn gọi:** `/response-configs/cam-001/monkey`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found`.

#### 15. `POST /response-configs/{cameraId}/{speciesId}/apply-preset/{presetId}` (Áp dụng preset mẫu)
*   **TC_CFG_PRE_SUCCESS_01: Auto-fill configuration using preset values**
    *   **Đường dẫn gọi:** `/response-configs/cam-001/elephant/apply-preset/critical_danger`
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về thông số cấu hình khớp với Preset mẫu `critical_danger`.
*   **TC_CFG_PRE_FAILURE_01: Fail to apply preset with invalid presetId**
    *   **Mô tả:** Lỗi khi mã Preset mẫu truyền vào không tồn tại trong hệ thống.
    *   **Đường dẫn gọi:** `/response-configs/cam-001/elephant/apply-preset/super_critical`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found`.

---

### 2.5. Nhóm Quản lý SĐT nhận tin nhắn cảnh báo (`[SMS_CONFIG_SCREEN]`)

#### 16. `GET /users/me/sms-recipients` (Lấy danh sách SĐT nhận tin nhắn phụ)
*   **TC_SMS_LIST_SUCCESS_01: Retrieve SMS recipients list successfully**
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về danh sách các SĐT nhận SMS phụ liên kết với tài khoản hiện tại.

#### 17. `POST /users/me/sms-recipients` (Thêm SĐT nhận tin nhắn mới)
*   **TC_SMS_ADD_SUCCESS_01: Add new SMS recipient successfully**
    *   **Điều kiện trước:** Tài khoản có 1 người nhận phụ trong DB.
    *   **Dữ liệu gửi đi (Request Body):** `{ "fullName": "Nguyen Van B", "phoneNumber": "+84908888888", "relation": "family" }`
    *   **Kết quả mong đợi (Expected Response):** `201 Created`.
    *   **Xác thực Database (DB Verification):** Một bản ghi mới được thêm thành công vào bảng `sms_recipients`.
*   **TC_SMS_ADD_FAILURE_01: Fail to add 4th recipient due to database limit constraint**
    *   **Mô tả:** Lỗi logic nghiệp vụ khi cố gắng đăng ký người nhận thứ 4 cho 1 tài khoản (Giới hạn tối đa là 3 người nhận).
    *   **Điều kiện trước:** Tài khoản đã có đủ 3 SĐT nhận SMS phụ.
    *   **Dữ liệu gửi đi (Request Body):** Đăng ký thêm SĐT thứ 4.
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request` kèm thông báo lỗi đạt giới hạn tối đa (Bị chặn bởi trigger cơ sở dữ liệu PostgreSQL).
*   **TC_SMS_ADD_FAILURE_02: Fail to add recipient due to missing fullName**
    *   **Mô tả:** Thiếu trường tên người nhận khi đăng ký.
    *   **Dữ liệu gửi đi (Request Body):** `{ "phoneNumber": "+84908888888", "relation": "family" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_SMS_ADD_FAILURE_03: Fail to add recipient due to missing phoneNumber**
    *   **Mô tả:** Thiếu trường số điện thoại khi đăng ký.
    *   **Dữ liệu gửi đi (Request Body):** `{ "fullName": "Nguyen Van B", "relation": "family" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_SMS_ADD_FAILURE_04: Fail to add recipient due to missing relation**
    *   **Mô tả:** Thiếu trường mối quan hệ khi đăng ký.
    *   **Dữ liệu gửi đi (Request Body):** `{ "fullName": "Nguyen Van B", "phoneNumber": "+84908888888" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_SMS_ADD_FAILURE_05: Fail to add recipient with invalid format phone number**
    *   **Mô tả:** Số điện thoại truyền vào sai cấu trúc E.164 (ví dụ truyền thiếu số hoặc sai ký tự).
    *   **Dữ liệu gửi đi (Request Body):** `{ "fullName": "Nguyen Van B", "phoneNumber": "090812", "relation": "family" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_SMS_ADD_FAILURE_06: Fail to add recipient with invalid relation value**
    *   **Mô tả:** Mối quan hệ truyền vào không nằm trong Enum quan hệ SMS (self, family, neighbor, other).
    *   **Dữ liệu gửi đi (Request Body):** `{ "fullName": "Nguyen Van B", "phoneNumber": "+84908888888", "relation": "friend" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.

#### 18. `DELETE /users/me/sms-recipients/{recipientId}` (Xóa SĐT khỏi danh sách nhận tin nhắn)
*   **TC_SMS_DEL_SUCCESS_01: Delete SMS recipient successfully**
    *   **Điều kiện trước:** Bản ghi người nhận `recipient-123` tồn tại và thuộc sở hữu của người dùng hiện tại.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` hoặc `204 No Content`.
    *   **Xác thực Database (DB Verification):** Bản ghi tương ứng bị xóa khỏi bảng `sms_recipients`.
*   **TC_SMS_DEL_FAILURE_01: Fail to delete non-existent recipient**
    *   **Mô tả:** Gửi yêu cầu xóa mã người nhận không tồn tại.
    *   **Đường dẫn gọi:** `/users/me/sms-recipients/recip-not-exist`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found`.

---

### 2.6. Nhóm API hệ thống & kiểm tra vật lý ngoại vi

#### 19. `POST /cameras/{cameraId}/devices/{deviceKey}/test` (Gửi lệnh test thiết bị ngoại vi tại trạm)
*   **TC_DEV_TST_SUCCESS_01: Send test command to camera peripheral successfully**
    *   **Dữ liệu gửi đi (Request Body):** `{ "durationSeconds": 5, "audioSampleId": "A_gunshot", "intensity": 80 }`
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về `commandId` xác nhận đã đẩy lệnh xuống camera thành công.
    *   **Xác thực Database (DB Verification):** Bản ghi nhật ký thiết bị mới được chèn vào bảng `device_logs` với `action` là `ON` và đúng thiết bị.
*   **TC_DEV_TST_FAILURE_01: Fail to trigger test command with missing durationSeconds**
    *   **Mô tả:** Thiếu thông số thời gian chạy test.
    *   **Dữ liệu gửi đi (Request Body):** `{ "audioSampleId": "A_gunshot", "intensity": 80 }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_DEV_TST_FAILURE_02: Fail to trigger test command with missing intensity**
    *   **Mô tả:** Thiếu thông số cường độ.
    *   **Dữ liệu gửi đi (Request Body):** `{ "durationSeconds": 5, "audioSampleId": "A_gunshot" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_DEV_TST_FAILURE_03: Fail to trigger test command with intensity too high**
    *   **Mô tả:** Cường độ phát test lớn hơn 100%.
    *   **Dữ liệu gửi đi (Request Body):** `{ "durationSeconds": 5, "audioSampleId": "A_gunshot", "intensity": 120 }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_DEV_TST_FAILURE_04: Fail to trigger test command with intensity negative**
    *   **Mô tả:** Cường độ phát test nhỏ hơn 0%.
    *   **Dữ liệu gửi đi (Request Body):** `{ "durationSeconds": 5, "audioSampleId": "A_gunshot", "intensity": -10 }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_DEV_TST_FAILURE_05: Fail to trigger test command with durationSeconds negative**
    *   **Mô tả:** Thời gian phát chạy test nhỏ hơn 0.
    *   **Dữ liệu gửi đi (Request Body):** `{ "durationSeconds": -5, "audioSampleId": "A_gunshot", "intensity": 80 }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_DEV_TST_FAILURE_06: Fail to trigger test with invalid deviceKey**
    *   **Mô tả:** Không tìm thấy thiết bị ngoại vi tại camera có mã key truyền vào (ví dụ trạm chỉ có led, speaker mà truyền key motor).
    *   **Đường dẫn gọi:** `/cameras/cam-001/devices/motor/test`
    *   **Dữ liệu gửi đi (Request Body):** `{ "durationSeconds": 5, "intensity": 50 }`
    *   **Kết quả mong đợi (Expected Response):** `404 Not Found` hoặc `400 Bad Request`.

#### 20. `GET /control/presets` (Lấy danh sách các Preset kịch bản)
*   **TC_REF_PRE_SUCCESS_01: Retrieve system danger presets successfully**
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về thông tin các preset.

#### 21. `GET /audio-samples` (Lấy danh mục âm thanh mẫu)
*   **TC_REF_AUD_SUCCESS_01: Retrieve catalog of animal sounds and speech alerts successfully**
    *   **Kết quả mong đợi (Expected Response):** `200 OK` chứa mảng danh sách âm thanh.

#### 22. `GET /health` (Kiểm tra sức khỏe hệ thống)
*   **TC_SYS_HLT_SUCCESS_01: Retrieve system health status successfully**
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về `{ "status": "OK" }`.

---

### 2.7. Nhóm Tích hợp AI Server & Nhận diện Hiện trường (Webhook API)

#### 23. `POST /cameras/{cameraId}/detections` (Thiết bị/AI Server gửi kết quả nhận dạng động vật)
*   **TC_AI_DET_SUCCESS_01: Initiate a new session event when no active event exists**
    *   **Mô tả:** Đảm bảo khi AI Server gửi kết quả nhận dạng và tại camera này không có bất kỳ sự kiện nào xảy ra trong vòng 5 phút trước đó, hệ thống sẽ tự động khởi tạo một phiên sự kiện (`events`) mới.
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": 0.92 } ], "imageUrl": "https://cdn.example.com/snap/cam001_2026-07-16T09-04-12.jpg", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `201 Created` trả về `eventId` mới tạo, danh sách `detections` và các thông số hành động phòng vệ tự động được tính toán.
    *   **Xác thực Database & Side Effects:**
        1.  Một bản ghi sự kiện mới được chèn thành công vào bảng `events`.
        2.  Bản ghi chi tiết nhận dạng được tạo trong bảng `event_detections` liên kết với sự kiện mới này.
        3.  Một bản ghi tin tức cảnh báo khẩn cấp được thêm thành công vào bảng `alerts` để đẩy lên feed tin tức.
*   **TC_AI_DET_SUCCESS_02: Append to an existing active session event within 5 minutes**
    *   **Mô tả:** Đảm bảo khi có nhận dạng mới gửi lên trong vòng dưới 5 phút kể từ thời điểm phát hiện gần nhất của sự kiện đang hoạt động tại camera này, hệ thống sẽ gom nhóm ghi nhận vào phiên sự kiện hiện có thay vì tạo mới.
    *   **Điều kiện trước:** Tại camera `cam-001` đang có sự kiện `evt-456` đang hoạt động (ghi nhận thời điểm 2 phút trước).
    *   **Dữ liệu gửi đi (Request Body):** Gửi nhận dạng mới tại `cam-001`.
    *   **Kết quả mong đợi (Expected Response):** `200 OK` trả về chính xác `eventId` = `evt-456`.
    *   **Xác thực Database (DB Verification):** Bản ghi `events` cũ được cập nhật lại trường `detected_at` và `snapshot_url` mới nhất. Bản ghi `event_detections` mới được chèn thêm và liên kết với `evt-456`. Không phát sinh bản ghi sự kiện mới trong bảng `events`.
*   **TC_AI_DET_SUCCESS_03: Start a new session event after 5 minutes of inactivity**
    *   **Mô tả:** Đảm bảo nếu nhận dạng mới gửi lên cách thời điểm phát hiện gần nhất của sự kiện trước đó tại camera này đã quá 5 phút, hệ thống sẽ coi sự kiện cũ đã hết và tạo mới hoàn toàn một phiên sự kiện mới.
    *   **Điều kiện trước:** Sự kiện gần nhất tại camera `cam-001` đã xảy ra từ 6 phút trước (đã quá giới hạn 5 phút).
    *   **Dữ liệu gửi đi (Request Body):** Gửi nhận dạng mới.
    *   **Kết quả mong đợi (Expected Response):** `201 Created` trả về một `eventId` hoàn toàn mới (ví dụ: `evt-789`).
    *   **Xác thực Database (DB Verification):** Một bản ghi sự kiện mới được tạo ra trong bảng `events`.
*   **TC_AI_DET_FAILURE_01: Fail to process detection due to missing detections array**
    *   **Mô tả:** Thiếu mảng thông tin nhận dạng.
    *   **Dữ liệu gửi đi (Request Body):** `{ "imageUrl": "https://cdn.example.com/snap/cam001.jpg", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_AI_DET_FAILURE_02: Fail to process detection due to missing imageUrl**
    *   **Mô tả:** Thiếu đường dẫn ảnh chụp hiện trường.
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": 0.92 } ], "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_AI_DET_FAILURE_03: Fail to process detection due to missing detectedAt**
    *   **Mô tả:** Thiếu trường thời điểm phát hiện.
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": 0.92 } ], "imageUrl": "https://cdn.example.com/snap/cam001.jpg" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_AI_DET_FAILURE_04: Fail to process detection with empty detections list**
    *   **Mô tả:** Mảng nhận dạng gửi lên trống rỗng không có phần tử.
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [], "imageUrl": "https://cdn.example.com/snap/cam001.jpg", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_AI_DET_FAILURE_05: Fail to process detection with confidence too high**
    *   **Mô tả:** Độ tin cậy nhận dạng lớn hơn 1 (100%).
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": 1.5 } ], "imageUrl": "https://cdn.example.com/snap/cam001.jpg", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_AI_DET_FAILURE_06: Fail to process detection with confidence negative**
    *   **Mô tả:** Độ tin cậy nhận dạng nhỏ hơn 0.
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": -0.2 } ], "imageUrl": "https://cdn.example.com/snap/cam001.jpg", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_AI_DET_FAILURE_07: Fail to process detection with invalid format imageUrl**
    *   **Mô tả:** Đường dẫn ảnh gửi lên không đúng định dạng URL.
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": 0.95 } ], "imageUrl": "not-a-url", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_AI_DET_FAILURE_08: Fail to process detection with invalid format detectedAt**
    *   **Mô tả:** Định dạng thời điểm phát hiện không đúng chuẩn ISO 8601.
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": 0.95 } ], "imageUrl": "https://cdn.example.com/snap/cam001.jpg", "detectedAt": "22-07-2026 10:00:00" }`
    *   **Kết quả mong đợi (Expected Response):** `400 Bad Request`.
*   **TC_AI_DET_SUCCESS_04: Push notification sent via Firebase when wild animal detected**
    *   **Mô tả:** Khi phát hiện thú hoang dã nguy hiểm, kiểm tra hệ thống giải mã chứng chỉ trực tiếp trong RAM từ biến môi trường `PUSH_SERVICE_ACCOUNT_KEY_JSON` và gửi thông báo qua FCM thành công đến các thiết bị di động đã đăng ký.
    *   **Điều kiện trước:** Có bản ghi token thiết bị trong bảng `device_tokens`. Loài được nhận diện là loài thú hoang dã nguy hiểm (`isHuman` = false, `dangerLevel` = HIGH/CRITICAL, ví dụ: `elephant`).
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": 0.92 } ], "imageUrl": "https://cdn.example.com/snap/cam001.jpg", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `201 Created` trả về eventId và các hành động phòng vệ.
    *   **Xác thực Side Effects:** Kiểm tra giả lập gửi tin nhắn FCM được gọi thành công, không tạo ra file tạm nào trên ổ đĩa cứng của server.
*   **TC_AI_DET_SUCCESS_05: No push notification sent when detection is non-dangerous or human**
    *   **Mô tả:** Khi phát hiện con người hoặc loài động vật không gây nguy hiểm (DangerLevel LOW), kiểm tra hệ thống không kích hoạt luồng gửi Push Notification để tránh làm phiền thiết bị người dùng.
    *   **Điều kiện trước:** Có bản ghi token thiết bị trong bảng `device_tokens`. Loài được nhận diện là `human` hoặc loài có DangerLevel `LOW` (ví dụ: `monkey`).
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "monkey", "confidence": 0.95 } ], "imageUrl": "https://cdn.example.com/snap/cam001.jpg", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `201 Created`.
    *   **Xác thực Side Effects:** Hệ thống không thực hiện bất kỳ cuộc gọi gửi tin nhắn push nào qua Firebase Cloud Messaging.
*   **TC_AI_DET_FAILURE_09: Firebase push notification fails gracefully on invalid service account key**
    *   **Mô tả:** Kiểm tra hệ thống vẫn hoạt động bình thường và ghi nhận sự kiện vào DB khi xảy ra lỗi giải mã/khởi tạo Firebase do biến môi trường `PUSH_SERVICE_ACCOUNT_KEY_JSON` không hợp lệ. Lỗi phải được xử lý mềm để tránh gián đoạn Webhook của AI Server.
    *   **Điều kiện trước:** Biến môi trường `PUSH_SERVICE_ACCOUNT_KEY_JSON` chứa chuỗi base64 không đúng định dạng của Object JSON chứng chỉ.
    *   **Dữ liệu gửi đi (Request Body):** `{ "detections": [ { "speciesId": "elephant", "confidence": 0.92 } ], "imageUrl": "https://cdn.example.com/snap/cam001.jpg", "detectedAt": "2026-07-22T10:00:00+07:00" }`
    *   **Kết quả mong đợi (Expected Response):** `201 Created` (hoặc `200 OK` tùy trường hợp gom nhóm sự kiện), sự kiện vẫn được lưu trữ vào DB.
    *   **Xác thực Side Effects:** Server in log lỗi khởi tạo/xác thực Firebase ra console, không crash ứng dụng.

