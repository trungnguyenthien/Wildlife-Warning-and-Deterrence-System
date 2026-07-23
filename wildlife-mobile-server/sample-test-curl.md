# Tài liệu Kịch bản Kiểm thử API trên Môi trường Production bằng cURL

Tài liệu này chứa danh sách các lệnh `cURL` dùng để kiểm thử khói (smoke test) toàn bộ các đầu API của ứng dụng trên môi trường Production (Vercel).

---

## 1. Thiết lập Biến Môi trường Kiểm thử

Hãy chạy các dòng lệnh dưới đây trong Terminal của bạn trước khi thực hiện các bài test để khai báo địa chỉ Host và tạo biến ngẫu nhiên cho tài khoản kiểm thử:

```bash
# Địa chỉ URL của môi trường Production Vercel
BASE="https://wildlife-warning-and-deterrence-sys.vercel.app"

# Tạo username ngẫu nhiên để tránh trùng lặp tài khoản khi test nhiều lần
RANDOM_ID=$RANDOM
RANGER_USER="ranger_smoke_$RANDOM_ID"
CITIZEN_USER="citizen_smoke_$RANDOM_ID"
PASSWORD="Password123!"
```

---

## 2. Danh sách các lệnh cURL Kiểm thử

### 2.1. Hệ thống & Health Check
```bash
# GET /health - Kiểm tra tình trạng hoạt động của máy chủ
curl -s "$BASE/health" | jq .
```

### 2.2. Xác thực & Đăng ký / Đăng nhập
```bash
# 1. Đăng ký tài khoản Ranger (Kiểm lâm)
RANGER_REG_RESP=$(curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$RANGER_USER\",\"password\":\"$PASSWORD\",\"fullName\":\"Ranger Smoke Test\",\"phoneNumber\":\"+8490511$RANDOM_ID\",\"role\":\"RANGER\"}")
echo "Ranger Reg Response: $RANGER_REG_RESP"

# Lưu lại Ranger ID từ kết quả trả về
RANGER_ID=$(echo "$RANGER_REG_RESP" | jq -r '.id')
echo "Ranger User ID: $RANGER_ID"

# 2. Đăng ký tài khoản Citizen (Người dân)
CITIZEN_REG_RESP=$(curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$CITIZEN_USER\",\"password\":\"$PASSWORD\",\"fullName\":\"Citizen Smoke Test\",\"phoneNumber\":\"+8490522$RANDOM_ID\",\"role\":\"CITIZEN\"}")
echo "Citizen Reg Response: $CITIZEN_REG_RESP"

# Lưu lại Citizen ID từ kết quả trả về
CITIZEN_ID=$(echo "$CITIZEN_REG_RESP" | jq -r '.id')
echo "Citizen User ID: $CITIZEN_ID"

# 3. Đăng nhập lấy Token xác thực (Ranger)
RANGER_LOGIN_RESP=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$RANGER_USER\",\"password\":\"$PASSWORD\"}")
RANGER_TOKEN=$(echo "$RANGER_LOGIN_RESP" | jq -r '.token')
echo "Ranger Bearer Token: $RANGER_TOKEN"

# 4. Đăng nhập lấy Token xác thực (Citizen)
CITIZEN_LOGIN_RESP=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$CITIZEN_USER\",\"password\":\"$PASSWORD\"}")
CITIZEN_TOKEN=$(echo "$CITIZEN_LOGIN_RESP" | jq -r '.token')
echo "Citizen Bearer Token: $CITIZEN_TOKEN"
```

### 2.3. Thông tin người dùng
```bash
# GET /users/me - Lấy thông tin tài khoản đang đăng nhập
curl -s "$BASE/users/me" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# PATCH /users/me - Cập nhật thông tin (Chỉ dùng để test)
curl -s -X PATCH "$BASE/users/me" \
  -H "Authorization: Bearer $RANGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Ranger Smoke Test Updated"}' | jq .
```

### 2.4. Quản lý Trạm Camera
```bash
# Khởi tạo một Camera test trên Production (nếu chưa có)
# Lưu ý: Do DB Production Neon sử dụng bảo mật, cần chạy seed camera CAM_PROD_TEST như hướng dẫn trước khi test upload/chi tiết.

# 1. GET /cameras - Lấy danh sách camera
curl -s "$BASE/cameras" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 2. GET /cameras/:cameraId - Lấy chi tiết camera
curl -s "$BASE/cameras/CAM_PROD_TEST" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 3. PATCH /cameras/:cameraId - Đổi tên hiển thị camera
curl -s -X PATCH "$BASE/cameras/CAM_PROD_TEST" \
  -H "Authorization: Bearer $RANGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Trạm Cát Tiên Số 1"}' | jq .

# 4. POST /cameras/:cameraId/snapshots - Tải lên ảnh snapshot thủ công (Cloudinary)
# Đảm bảo có tệp f4.png tại thư mục chạy lệnh
curl -s -X POST "$BASE/cameras/CAM_PROD_TEST/snapshots" \
  -H "Authorization: Bearer $RANGER_TOKEN" \
  -F "image=@f4.png" \
  -F "userId=$RANGER_ID" | jq .

# 5. POST /cameras/:cameraId/devices/:deviceKey/test - Gửi lệnh test thiết bị ngoại vi
curl -s -X POST "$BASE/cameras/CAM_PROD_TEST/devices/deterrent_audio/test" \
  -H "Authorization: Bearer $RANGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"durationSeconds":5,"intensity":80}' | jq .

# 6. GET /cameras/stream - Kết nối luồng Server-Sent Events (SSE) cập nhật trạng thái động
# Lệnh này sẽ mở kết nối liên tục, nhấn Ctrl+C để thoát
curl -N -s "$BASE/cameras/stream" -H "Authorization: Bearer $RANGER_TOKEN"
```

### 2.5. Cấu hình Phòng vệ & Loài
```bash
# 1. GET /species - Tải danh mục loài
curl -s "$BASE/species" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 2. GET /control/presets - Tải các thiết lập mặc định (Presets)
curl -s "$BASE/control/presets" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 3. GET /audio-samples - Tải danh mục âm thanh mẫu
curl -s "$BASE/audio-samples" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 4. PUT /response-configs/:cameraId/:speciesId - Lưu cấu hình tùy chỉnh
# Lưu ý: Cần thêm loài 'voi_rung' vào DB để chạy thành công (đã có ở bản test local)
curl -s -X PUT "$BASE/response-configs/CAM_PROD_TEST/voi_rung" \
  -H "Authorization: Bearer $RANGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ledFlash":true,"speakerWarn":true,"electricFence":true,"silentAlert":false,"ledColor":"RED","ledFlashRate":"FAST","ledDurationSeconds":10,"speakerSampleId":"warning_voice_1","audioIntensity":90,"fenceLevel":"HIGH","fenceWarningLight":true,"fenceAutoNotify":true,"fenceAutoOffEnabled":true,"fenceAutoOffMinutes":5}' | jq .

# 5. GET /response-configs - Lấy chi tiết cấu hình tùy chỉnh của cặp camera/loài
curl -s "$BASE/response-configs?cameraId=CAM_PROD_TEST&speciesId=voi_rung" \
  -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 6. GET /response-configs/:cameraId - Lấy toàn bộ cấu hình tùy chỉnh tại camera
curl -s "$BASE/response-configs/CAM_PROD_TEST" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 7. POST /response-configs/:cameraId/:speciesId/apply-preset/:presetId - Áp dụng Preset kịch bản mẫu
curl -s -X POST "$BASE/response-configs/CAM_PROD_TEST/voi_rung/apply-preset/CRITICAL" \
  -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 8. DELETE /response-configs/:cameraId/:speciesId - Reset cấu hình tùy chỉnh về cấu hình mặc định
curl -s -X DELETE "$BASE/response-configs/CAM_PROD_TEST/voi_rung" \
  -H "Authorization: Bearer $RANGER_TOKEN" | jq .
```

### 2.6. Quản lý Số điện thoại nhận tin khẩn cấp (SMS)
```bash
# 1. POST /users/me/sms-recipients - Thêm SĐT nhận tin khẩn cấp
SMS_ADD_RESP=$(curl -s -X POST "$BASE/users/me/sms-recipients" \
  -H "Authorization: Bearer $RANGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"fullName\":\"Nguyen Van Gia Dinh\",\"phoneNumber\":\"+84918${RANDOM_ID}\",\"relation\":\"family\"}")
echo "SMS Add Response: $SMS_ADD_RESP"

# Lấy SMS Recipient ID vừa tạo
RECIPIENT_ID=$(echo "$SMS_ADD_RESP" | jq -r '.id')
echo "SMS Recipient ID: $RECIPIENT_ID"

# 2. GET /users/me/sms-recipients - Lấy danh sách SĐT đăng ký nhận tin của user
curl -s "$BASE/users/me/sms-recipients" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 3. DELETE /users/me/sms-recipients/:recipientId - Xóa SĐT nhận tin
curl -s -X DELETE "$BASE/users/me/sms-recipients/$RECIPIENT_ID" \
  -H "Authorization: Bearer $RANGER_TOKEN" | jq .
```

### 2.7. Nhật ký Sự kiện & Báo động
```bash
# 1. GET /events - Lấy nhật ký sự kiện lịch sử của camera
curl -s "$BASE/events?cameraId=CAM_PROD_TEST" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 2. GET /alerts/feed - Luồng tin tức cảnh báo khẩn cấp (Ranger xem được cả HUMAN_BORDER)
curl -s "$BASE/alerts/feed" -H "Authorization: Bearer $RANGER_TOKEN" | jq .

# 3. GET /alerts/feed - Luồng tin tức cảnh báo khẩn cấp (Citizen không xem được các tin HUMAN_BORDER)
curl -s "$BASE/alerts/feed" -H "Authorization: Bearer $CITIZEN_TOKEN" | jq .

# 4. POST /alerts/feed/:alertId/read - Đánh dấu đã đọc cảnh báo
# Lưu ý: Cần truyền mã alertId thực tế từ danh sách feed để test
# curl -s -X POST "$BASE/alerts/feed/alt-12345/read" -H "Authorization: Bearer $RANGER_TOKEN" | jq .
```

### 2.8. Tích hợp thiết bị & AI Server (Webhook)
```bash
# POST /cameras/:cameraId/detections - AI Server gửi nhận diện thú rừng (Không cần Auth token)
curl -s -X POST "$BASE/cameras/CAM_PROD_TEST/detections" \
  -H "Content-Type: application/json" \
  -d '{"detections":[{"speciesId":"voi_rung","confidence":0.95}],"imageUrl":"https://res.cloudinary.com/dhfkqbnnx/image/upload/v1784718531/manual_snapshots/hs756foqkx1t0kjauhxm.png","detectedAt":"2026-07-23T09:00:00Z"}' | jq .
```

### 2.9. Thống kê & Báo cáo
```bash
# GET /stats/summary - Lấy tóm tắt số liệu thống kê và bản đồ nhiệt
curl -s "$BASE/stats/summary?startDate=2026-01-01&endDate=2026-12-31" \
  -H "Authorization: Bearer $RANGER_TOKEN" | jq .
```

### 2.10. Đăng xuất (Logout)
```bash
# POST /auth/logout - Hủy phiên đăng nhập (Hủy token)
curl -s -X POST "$BASE/auth/logout" -H "Authorization: Bearer $RANGER_TOKEN" | jq .
curl -s -X POST "$BASE/auth/logout" -H "Authorization: Bearer $CITIZEN_TOKEN" | jq .
```
