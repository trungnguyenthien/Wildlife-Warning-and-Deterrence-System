# 📚 Ngân Hàng Câu Hỏi Nghiên Cứu Phản Biện Theo Phiên Học (25 - 35 Phút / Phiên)
### *Cấu trúc Phân nhóm Theo Action & Tiến Trình Xử Lý Dữ Liệu Tài Liệu 04*

> [!NOTE]
> ### 💡 Diễn giải Luồng vận hành (Dành cho Giám khảo / Người đọc tổng quan)
> Tài liệu này được thiết kế thành **11 Phiên học độc lập** (mỗi phiên từ **25 đến 35 phút**, gồm 5 - 7 câu hỏi), bám sát toàn bộ các Action trong tài liệu đặc tả [04-sequence-diagram.md](./04-sequence-diagram.md).
> - **Định mức:** 5 phút / câu hỏi. Học sinh tra cứu tài liệu 04 để tự trả lời.
> - **Mục tiêu:** Giúp học sinh làm chủ 100% bản chất hệ thống, luồng dữ liệu API, cơ chế bảo mật và kiến trúc máy chủ để tự tin phản biện trước Hội đồng Giám khảo.

---

# 🎯 LỘ TRÌNH 11 PHIÊN HỌC TỔNG QUAN HỆ THỐNG

---

## ⏱️ PHIÊN HỌC 1: ĐĂNG KÝ TÀI KHOẢN (ACTION 1.1)
*Thời lượng: **25 phút** (5 câu hỏi) | Nguồn tài liệu: [Mục 1.1 - 04-sequence-diagram.md](./04-sequence-diagram.md#11-action-register)*

### 📖 Khái niệm & Định nghĩa:
1. **Kiểm tra dữ liệu đầu vào (Validation)** là gì? Tại sao hệ thống phải thực hiện validation ở cả phía App (Client) lẫn Máy chủ (Server)?
2. **Mã phản hồi HTTP Status Code** (ví dụ: `201 Created`, `400 Bad Request`, `409 Conflict`) có ý nghĩa như thế nào trong giao tiếp Web API?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
3. Khi người dùng nhấn nút "Đăng ký" trên giao diện Android App, gói tin gửi lên Máy chủ Backend chứa những trường thông tin cụ thể nào?
4. Trình tự các bước kiểm tra dữ liệu diễn ra tại Máy chủ Backend như thế nào? Trong trường hợp tên đăng nhập hoặc số điện thoại đã tồn tại, máy chủ trả về mã lỗi HTTP và thông điệp gì?
5. Khi tất cả thông tin hợp lệ, Máy chủ Backend thực hiện thao tác gì với Cơ sở dữ liệu và phản hồi lại kết quả ra sao để App điều hướng người dùng về màn hình Đăng nhập?

---

## ⏱️ PHIÊN HỌC 2: ĐĂNG NHẬP & ĐĂNG KÝ PUSH TOKEN (ACTION 2.1)
*Thời lượng: **35 phút** (7 câu hỏi) | Nguồn tài liệu: [Mục 2.1 - 04-sequence-diagram.md](./04-sequence-diagram.md#21-action-login--register-push-token)*

### 📖 Khái niệm & Định nghĩa:
6. **Băm mật khẩu một chiều (Password Hashing / Bcrypt)** là gì? Tại sao mật khẩu tuyệt đối không lưu dưới dạng chữ thô (plain text) trong Database? Hãy giải thích qua hình ảnh minh họa *"Trộn màu sơn một chiều"*.
7. **Hiện tượng đụng độ mã băm (Hash Collision)** là gì? Tại sao thuật toán Bcrypt lại đảm bảo an toàn cao trước hiện tượng này?
8. **Access Token** và **Refresh Token** là gì? Hãy so sánh vai trò, mục đích sử dụng và thời hạn tồn tại của 2 loại token này.
9. **Push Notification** và **FCM Token (Google Firebase Cloud Messaging)** là gì?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
10. Trong **Bước 1 (Đăng nhập)**: Luồng kiểm tra mật khẩu và xác thực tại Máy chủ Backend diễn ra theo trình tự nào? Khi đăng nhập thành công, máy chủ phát trả về cho App những chìa khóa xác thực và thông tin gì?
11. Trong **Bước 2 (Lấy mã thiết bị)**: Ngay sau khi nhận được Access Token, Android App thực hiện giao tiếp với Google Firebase SDK như thế nào để nhận về `fcmToken`?
12. Trong **Bước 3 (Liên kết thiết bị)**: Android App gửi gói tin chứa dữ liệu gì lên Máy chủ Backend để liên kết `fcmToken` với tài khoản người dùng? Máy chủ lưu thông tin này vào bảng DB nào và nhằm mục đích gì cho luồng cảnh báo sau này?

---

## ⏱️ PHIÊN HỌC 3: TẢI DANH SÁCH CAMERA & AUTO-POLLING HEARTBEAT (ACTION 3.1.1 & 3.1.2 PHẦN 1)
*Thời lượng: **30 phút** (6 câu hỏi) | Nguồn tài liệu: [Mục 3.1.1 & 3.1.2 - 04-sequence-diagram.md](./04-sequence-diagram.md#311-action-load-cameras-list)*

### 📖 Khái niệm & Định nghĩa:
13. **Bearer Token** trong tiêu đề HTTP Header (`Authorization: Bearer <token>`) là gì? Tại sao cần truyền Bearer Token trong mọi request yêu cầu bảo mật?
14. Khái niệm **Polling** là gì? Cơ chế **Smart Polling (Heartbeat Check)** khác gì so với Polling truyền thống?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
15. Khi người dùng chuyển sang màn hình tab Camera, Android App gửi yêu cầu API gì lên Máy chủ Backend và đính kèm tiêu đề xác thực nào?
16. Máy chủ Backend xử lý truy vấn Cơ sở dữ liệu ra sao để lọc đúng các trạm camera thuộc quyền sở hữu của tài khoản đăng nhập? Gói dữ liệu phản hồi trả về danh sách gồm những thông tin thực địa nào của camera?
17. Sau khi nhận được phản hồi từ máy chủ, Android App thực hiện các bước xử lý nào để hiển thị danh sách trạm camera và tải hình ảnh snapshot lên màn hình?
18. Trình tự tiến trình kiểm tra Heartbeat định kỳ (mỗi 5 giây) được Android App khởi chạy và gửi lên máy chủ như thế nào?

---

## ⏱️ PHIÊN HỌC 4: SMART POLLING, HEATMAP & HỒ SƠ CÁ NHÂN (ACTION 3.1.2 PHẦN 2, 3.2.3 & 3.3.1 PHẦN 1)
*Thời lượng: **30 phút** (6 câu hỏi) | Nguồn tài liệu: [Mục 3.1.2, 3.2.3 & 3.3.1 - 04-sequence-diagram.md](./04-sequence-diagram.md#312-action-auto-polling-smart-polling-cập-nhật-danh-sách-camera)*

### 📖 Khái niệm & Định nghĩa:
19. **Biểu đồ Xu hướng (Line Chart)** và **Sơ đồ Nhiệt di chuyển (Heatmap)** là gì? Ý nghĩa của 2 dạng biểu đồ này đối với công tác giám sát của kiểm lâm?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
20. Gói tin phản hồi Heartbeat từ Máy chủ Backend chứa những cờ dữ liệu nào? Android App căn cứ vào cờ dữ liệu đó để quyết định xử lý ra sao trong 2 trường hợp (có thay đổi vs không có thay đổi)?
21. Tại sao giải pháp chia làm 2 bước (kiểm tra cờ Heartbeat nhẹ trước rồi mới gọi API tải full danh sách) lại giúp tối ưu tài nguyên thiết bị và giảm tải cho máy chủ?
22. Hãy mô tả trình tự Máy chủ Backend tổng hợp dữ liệu xuất hiện của động vật theo chuỗi thời gian và cấu trúc dữ liệu trả về cho App để dựng Biểu đồ xu hướng?
23. Gói dữ liệu tọa độ/tần suất di chuyển được Máy chủ Backend xử lý và trả về dưới định dạng nào để Android App vẽ được Sơ đồ nhiệt di chuyển (Heatmap)?
24. Khi truy cập màn hình Cấu hình cá nhân, trình tự gửi yêu cầu lấy thông tin người dùng diễn ra như thế nào? Máy chủ Backend phản hồi lại những thông tin hồ sơ nào?

---

## ⏱️ PHIÊN HỌC 5: CẬP NHẬT HỒ SƠ & ĐĂNG XUẤT AN TOÀN (ACTION 3.3.1 PHẦN 2 & ACTION 3.3.2)
*Thời lượng: **25 phút** (5 câu hỏi) | Nguồn tài liệu: [Mục 3.3.1 & 3.3.2 - 04-sequence-diagram.md](./04-sequence-diagram.md#332-action-logout)*

### 📖 Khái niệm & Định nghĩa:
25. Khái niệm **Hủy đăng ký mã Push Token (Unregister Token)** là gì? Tại sao phải xóa liên kết token thiết bị trên máy chủ ngay khi người dùng đăng xuất?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
26. Khi người dùng chỉnh sửa tên hiển thị hoặc số điện thoại và bấm "Lưu", Android App gửi gói tin gì lên Máy chủ Backend? Trình tự máy chủ kiểm tra, cập nhật Database và phản hồi kết quả về App diễn ra ra sao?
27. Khi người dùng nhấn nút "Đăng xuất", Android App thực hiện bước gửi gói tin hủy `fcmToken` lên Máy chủ Backend như thế nào? Việc này giúp ngăn ngừa sự cố an ninh/thông báo nào?
28. Trình tự gửi yêu cầu hủy phiên đăng nhập (Invalidate Token) lên Máy chủ Backend diễn ra ra sao và máy chủ phản hồi lại kết quả thế nào?
29. Phía Android App thực hiện chuỗi thao tác dọn dẹp bộ nhớ cục bộ (xóa chìa khóa xác thực trong EncryptedSharedPreferences) và điều hướng giao diện người dùng theo thứ tự nào?

---

## ⏱️ PHIÊN HỌC 6: CHI TIẾT CAMERA, ĐỔI TÊN TRẠM & DANH SÁCH LOÀI (ACTION 4.1, 4.2 & 5.1 PHẦN 1)
*Thời lượng: **35 phút** (7 câu hỏi) | Nguồn tài liệu: [Mục 4.1, 4.2 & 5.1 - 04-sequence-diagram.md](./04-sequence-diagram.md#41-action-view-camera-detail--history-logs)*

### 📖 Khái niệm & Định nghĩa:
30. Việc cho phép người dùng thay đổi tên gợi nhớ của trạm camera mang lại giá trị quản lý thực tế gì cho kiểm lâm tại địa phương?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
31. Khi người dùng nhấn vào một trạm camera cụ thể, Android App gửi yêu cầu API mang tham số định danh nào lên Máy chủ Backend?
32. Máy chủ Backend tổng hợp và phản hồi về cho App một khối dữ liệu đầy đủ gồm những thông tin thành phần nào (thông tin trạm, ảnh snapshot, kết quả phân tích AI, danh sách lịch sử sự kiện)?
33. Android App tiếp nhận khối dữ liệu tổng hợp này và phân bổ hiển thị lên các khu vực trên màn hình chi tiết như thế nào?
34. Hãy mô tả trình tự từ lúc người dùng mở dialog đổi tên, nhập tên mới và bấm "Xác nhận": Android App gửi gói dữ liệu gồm những trường nào lên Máy chủ Backend?
35. Máy chủ Backend thực hiện cập nhật tên mới vào Database ra sao và phản hồi kết quả thế nào để Android App cập nhật ngay lập tức tiêu đề trên thanh điều hướng Topbar?
36. Khi người dùng mở màn hình Cấu hình Phòng vệ, Android App phát yêu cầu lấy danh sách toàn bộ các loài động vật từ Máy chủ Backend theo trình tự nào?

---

## ⏱️ PHIÊN HỌC 7: CẤU HÌNH PHÒNG VỆ LOÀI & ÁP DỤNG PRESETS (ACTION 5.1 PHẦN 2, 6.1 & 6.2 PHẦN 1)
*Thời lượng: **35 phút** (7 câu hỏi) | Nguồn tài liệu: [Mục 5.1, 6.1 & 6.2 - 04-sequence-diagram.md](./04-sequence-diagram.md#61-action-view--edit-response-config-for-a-species)*

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
37. Máy chủ Backend truy vấn Database và phản hồi danh sách các loài kèm theo cờ trạng thái cấu hình của từng loài ra sao (phân biệt giữa loài đã có cấu hình tùy chỉnh riêng và loài đang dùng cấu hình mặc định)?
38. Android App nhận danh sách và xử lý giao diện như thế nào để kiểm lâm dễ dàng nhận biết trạng thái cấu hình phòng vệ của từng loài động vật?
39. Khi người dùng bấm chọn một loài động vật cụ thể để chỉnh sửa cấu hình, Android App gửi mã định danh `speciesId` lên Máy chủ Backend như thế nào?
40. Máy chủ Backend tổng hợp và trả về khối dữ liệu trợ giúp gồm 4 nhóm thông tin gì (cấu hình hiện tại, danh sách preset, danh sách mẫu âm thanh xua đuổi, danh sách âm thanh cảnh báo dân cư)?
41. Android App tiếp nhận dữ liệu và thực hiện liên kết (binding) dữ liệu lên các thành phần giao diện chọn (Dropdown/Sliders) ra sao?
42. Khi người dùng điều chỉnh các tham số phòng vệ và bấm "Lưu", Android App đóng gói và gửi lên Máy chủ Backend một payload chứa những thông số cấu hình thực thi nào?
43. Khi người dùng chọn áp dụng một mẫu **Preset Phòng vệ Nhanh**, Android App gửi thông điệp gì lên máy chủ? Máy chủ xử lý ghi đè các tham số từ Preset vào cấu hình loài trong Database theo các bước nào?

---

## ⏱️ PHIÊN HỌC 8: RESET DEFAULT, WEBSOCKET & ABLY BROKER (ACTION 6.2 PHẦN 2 & 6.3 PHẦN 1)
*Thời lượng: **30 phút** (6 câu hỏi) | Nguồn tài liệu: [Mục 6.2 & 6.3 - 04-sequence-diagram.md](./04-sequence-diagram.md#63-action-test-speaker-sound-at-camera-station-ai_server)*

### 📖 Khái niệm & Định nghĩa:
44. Giao thức **WebSocket** là gì? So sánh cơ chế giao tiếp 2 chiều của WebSocket với mô hình Request/Response truyền thống của HTTP.
45. **Cloud Message Broker (Ably)** giữ vai trò trung gian như thế nào trong việc kết nối giữa Máy chủ Cloud (Vercel) và Máy chủ thực địa (`AI_Server`)?
46. Đặc tính **Serverless & Stateless** của máy chủ Vercel ảnh hưởng thế nào đến việc duy trì kết nối WebSocket trực tiếp, từ đó dẫn đến quyết định thiết kế sử dụng Ably Broker?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
47. Trình tự xử lý khi người dùng chọn "Khôi phục mặc định": App gửi yêu cầu gì, Máy chủ xóa bản ghi tùy chỉnh trong Database ra sao và phản hồi kết quả để App nạp lại cấu hình mặc định như thế nào?
48. **Bước 1 (App gửi lệnh):** Khi kiểm lâm bấm nút *"Phát thử âm thanh từ trạm (5s)"*, Android App gửi gói tin chứa những tham số điều khiển thiết bị cụ thể nào lên `Mobile_Server`?
49. **Bước 2 (Chuyển tiếp qua Ably):** `Mobile_Server` đóng gói bản tin lệnh `DEVICE_COMMAND` và xuất bản (Publish) lên kênh (Channel) nào của Ably Broker? Ably chuyển tiếp bản tin qua kết nối WebSocket xuống `AI_Server` tại trạm thực địa ra sao?

---

## ⏱️ PHIÊN HỌC 9: LUỒNG ACK/TIMEOUT & QUẢN LÝ SĐT SMS (ACTION 6.3 PHẦN 2 & ACTION 7.1-7.2)
*Thời lượng: **35 phút** (7 câu hỏi) | Nguồn tài liệu: [Mục 6.3 & 7.1-7.2 - 04-sequence-diagram.md](./04-sequence-diagram.md#71-action-view-sms-recipients-list)*

### 📖 Khái niệm & Định nghĩa:
50. Bản tin xác nhận **ACK (Acknowledge)** và cơ chế **Timeout (Thời gian chờ tối đa)** có vai trò gì trong việc đảm bảo tính tin cậy của lệnh điều khiển thiết bị?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
51. **Bước 3 (Thực thi phần cứng):** Khi nhận được lệnh từ kết nối WebSocket, `AI_Server` xử lý và điều khiển Loa công suất lớn tại trạm camera phát âm thanh theo các tham số nhận được như thế nào?
52. **Bước 4 (Phản hồi ACK):** Sau khi hoàn tất 5 giây phát âm thanh, `AI_Server` tạo bản tin `COMMAND_ACK` gửi ngược về `Mobile_Server` qua Ably như thế nào để máy chủ ghi log và phản hồi thành công về Android App?
53. **Xử lý lỗi Timeout:** Nếu trạm camera bị mất mạng hoặc mất điện (không có ACK gửi về sau 9 giây), `Mobile_Server` xử lý bắt lỗi Timeout và trả về mã lỗi/thông điệp gì để thông báo cho kiểm lâm trên App?
54. Trình tự Android App gửi yêu cầu tải danh sách số điện thoại nhận SMS cảnh báo khẩn cấp diễn ra như thế nào? Máy chủ Backend phản hồi lại cấu trúc dữ liệu ra sao?
55. Khi người dùng nhập số điện thoại mới và bấm "Thêm", Android App gửi dữ liệu gì lên máy chủ? Máy chủ Backend thực hiện các bước kiểm tra ràng buộc gì (định dạng SĐT, giới hạn tối đa 3 số/tài khoản) trước khi lưu vào Database?
56. Khi nhấn xóa một số điện thoại trong danh sách, Android App gửi mã định danh bản ghi (`recipientId`) lên máy chủ ra sao? Trình tự máy chủ xóa bản ghi trong Database và phản hồi danh sách cập nhật về cho App diễn ra thế nào?

---

## ⏱️ PHIÊN HỌC 10: AI EDGE DETECTION, COOLDOWN 30S & UPLOAD CDN (ACTION 1.1 AI & 1.2 AI PHẦN 1)
*Thời lượng: **35 phút** (7 câu hỏi) | Nguồn tài liệu: [Khối II - 04-sequence-diagram.md](./04-sequence-diagram.md#11-action-ai-server-sends-detection-snapshot-ai_server)*

### 📖 Khái niệm & Định nghĩa:
57. Khái niệm **Cooldown (Thời gian chờ lọc chống spam cảnh báo - 30s)** là gì? Tại sao cần cơ chế Cooldown này tại trạm giám sát động vật hoang dã?
58. **Dịch vụ Lưu trữ Đám mây CDN (Cloudinary Image Storage)** là gì? Tại sao hình ảnh snapshot thực địa lại được upload lên CDN thay vì lưu trực tiếp tệp nhị phân trong Database?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
59. **Bước 1 (Phát hiện tại thực địa):** Hãy mô tả trình tự từ lúc Camera chụp ảnh chuyển động $\rightarrow$ truyền sang `AI_Server` chạy mô hình AI YOLO $\rightarrow$ `AI_Server` đóng gói dữ liệu kết quả nhận dạng và gửi request API lên `Mobile_Server`?
60. **Bước 2 (Tra cứu kịch bản phòng vệ):** Khi nhận được dữ liệu phát hiện từ `AI_Server`, `Mobile_Server` xử lý truy vấn Database tìm kịch bản phòng vệ phẳng `@DefendAction` cài đặt riêng cho loài đó tại trạm đó ra sao? Gói kịch bản phản hồi lập tức về cho `AI_Server` gồm những thông số kích hoạt thiết bị tại chỗ nào?
61. **Bước 3 (Phát cảnh báo khẩn & Lọc Cooldown 30s):** `Mobile_Server` thực hiện thuật toán kiểm tra mốc thời gian phát hiện gần nhất trong Database như thế nào? 
    * Khi `isNewEvent = true` (> 30s): Trình tự máy chủ tạo bản ghi Alert, giải mã mã Firebase Key trong RAM để gửi Push Notification qua FCM và kích hoạt gửi SMS khẩn cấp diễn ra ra sao?
    * Khi `isNewEvent = false` ($\le$ 30s): Máy chủ xử lý bỏ qua bước nào để chống spam thông báo cho kiểm lâm?
62. **Bước 4 (Cập nhật dữ liệu thực địa):** Trình tự lưu bản ghi Snapshot vào Database và cơ chế cập nhật hình ảnh thực địa mới nhất lên ứng dụng Android App của các kiểm lâm diễn ra như thế nào?
63. Khi các công cụ kiểm thử (cURL / Test Script) gửi tệp ảnh thử nghiệm kèm mã trạm camera `cameraId` lên endpoint API của `Mobile_Server`, gói tin HTTP multipart request được đóng gói ra sao?

---

## ⏱️ PHIÊN HỌC 11: ĐÁNH GIÁ 4 HẠN CHẾ & KIẾN TRÚC DIGITALOCEAN CẢI TIẾN (ACTION 1.2 AI PHẦN 2 & ACTION 3.1 PHẦN III)
*Thời lượng: **35 phút** (7 câu hỏi) | Nguồn tài liệu: [Phần III - 04-sequence-diagram.md](./04-sequence-diagram.md#iii-đánh-giá-hạn-chế-kiến-trúc-hiện-tại--đề-xuất-hướng-cải-tiến-tối-ưu-architecture-assessment--future-redesign)*

### 📖 Khái niệm & Định nghĩa:
64. Sự khác biệt bản chất giữa **Hạ tầng Serverless (Vercel)** và **Dedicated Cloud VPS (DigitalOcean)** là gì? Tại sao Dedicated VPS lại cho phép duy trì kết nối WebSocket bền vững 24/7 mà không cần qua dịch vụ trung gian trung chuyển tin nhắn?

### 🔄 Trình tự Xử lý & Dữ liệu Trao đổi:
65. Hãy trình bày thứ tự các bước xử lý nội bộ tại `Mobile_Server`: Kiểm tra định dạng/kích thước ảnh $\rightarrow$ Tải ảnh lên dịch vụ Cloudinary để lấy URL $\rightarrow$ Ghi bản ghi vào Database $\rightarrow$ Trả phản hồi HTTP Status Code `201 Created` kèm URL cho công cụ kiểm thử?
66. Hãy phân tích chi tiết **4 hạn chế nguyên nhân cốt lõi** (Kinh phí Vercel/Laptop cá nhân, Thời gian phát triển, Kinh nghiệm kiến trúc lớn, Năng lực phát triển Backend của nhóm AI) đã buộc nhóm dự án phải chấp nhận giải pháp trung gian Ably Broker trong phiên bản hiện tại?
67. Hãy mô tả trình tự luồng dữ liệu **Nhận diện & Thực thi xua đuổi** hoàn chỉnh trong Kiến trúc Cải tiến Đề xuất (DigitalOcean Cloud Environment & Safe Area Edge Station):
    * Luồng xử lý từ Camera $\rightarrow$ Raspberry Pi tại thực địa $\rightarrow$ AI Server trên DigitalOcean $\rightarrow$ Mobile Server $\rightarrow$ Database $\rightarrow$ Phản hồi kịch bản phòng vệ $\rightarrow$ Điều khiển Loa/Đèn LED tại trạm thực địa diễn ra theo thứ tự từng bước như thế nào?
    * Đồng thời, luồng đẩy thông báo cảnh báo qua Cloudinary, Google FCM và hiển thị trên Android App của kiểm lâm kết nối ra sao?
68. Mô tả trình tự luồng **Phát thử âm thanh xua đuổi từ xa** trong Kiến trúc Cải tiến DigitalOcean: Từ lúc kiểm lâm thao tác trên Android App $\rightarrow$ Mobile Server $\rightarrow$ AI Server (DigitalOcean) $\rightarrow$ Raspberry Pi tại Safe Area kích hoạt Loa 5s diễn ra như thế nào khi bỏ qua Ably Broker?
69. Tại sao trong mô hình kiến trúc cải tiến đề xuất, thiết bị nhúng Raspberry Pi đặt tại thực địa (Safe Area) **không cần có địa chỉ IP công khai (Public IP)** và **không cần mở Port mạng** mà vẫn có thể duy trì kết nối an toàn với Cloud Server?
70. Phương án **"Đồng nhất Nền tảng Công nghệ" (Unified Python Stack bằng FastAPI + PyTorch/YOLO)** giúp khắc phục triệt để chi phí giao tiếp giữa 2 nhóm phát triển và tối ưu độ trễ xử lý dữ liệu qua bộ nhớ RAM (In-memory execution) như thế nào?

---

## 📝 BẢNG QUẢN LÝ TIẾN ĐỘ THEO 11 PHIÊN HỌC (25 - 35 PHÚT / PHIÊN)

| STT Phiên | Nội dung chính của Phiên học | Số câu hỏi | Thời lượng học | Đã hoàn thành? (X) |
| :---: | :--- | :---: | :---: | :---: |
| **Phiên 1** | Đăng ký tài khoản (Action 1.1) | 5 câu | **25 phút** | [ ] |
| **Phiên 2** | Đăng nhập & Register FCM Push Token (Action 2.1) | 7 câu | **35 phút** | [ ] |
| **Phiên 3** | Tải danh sách Camera & Auto-Polling (Action 3.1.1 & 3.1.2) | 6 câu | **30 phút** | [ ] |
| **Phiên 4** | Smart Polling, Biểu đồ Heatmap & Profile (Action 3.1.2, 3.2.3 & 3.3.1) | 6 câu | **30 phút** | [ ] |
| **Phiên 5** | Cập nhật Hồ sơ & Đăng xuất an toàn (Action 3.3.1 & 3.3.2) | 5 câu | **25 phút** | [ ] |
| **Phiên 6** | Chi tiết Camera, Đổi tên trạm & Danh sách loài (Action 4.1, 4.2 & 5.1) | 7 câu | **35 phút** | [ ] |
| **Phiên 7** | Cấu hình phòng vệ loài & Áp dụng Presets (Action 5.1, 6.1 & 6.2) | 7 câu | **35 phút** | [ ] |
| **Phiên 8** | Reset Default, WebSocket & Ably Broker (Action 6.2 & 6.3) | 6 câu | **30 phút** | [ ] |
| **Phiên 9** | Luồng ACK/Timeout & Quản lý SĐT SMS (Action 6.3 & 7.1-7.2) | 7 câu | **35 phút** | [ ] |
| **Phiên 10**| AI Edge Detection, Cooldown 30s & Upload CDN (Action 1.1 & 1.2 AI) | 7 câu | **35 phút** | [ ] |
| **Phiên 11**| 4 Hạn chế cốt lõi & Sơ đồ DigitalOcean (Action 1.2 AI & Sec III) | 7 câu | **35 phút** | [ ] |
| **TỔNG** | **TOÀN BỘ LỘ TRÌNH 11 PHIÊN HỌC** | **70 câu** | **350 phút (~5.8 giờ)** | |
