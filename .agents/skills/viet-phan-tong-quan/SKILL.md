---
name: viet-phan-tong-quan
description: Hướng dẫn tổng quát biên soạn phần diễn giải tổng quan phi kỹ thuật cho BẤT KỲ tính năng/luồng nghiệp vụ nào dành cho Giám khảo KHKT (VISEF, Intel ISEF, HĐKH) hoặc người đọc không thuộc chuyên ngành CNTT. Kích hoạt khi người dùng yêu cầu "viết phần tổng quan" cho một tính năng bất kỳ.
---

# Skill: Viết Phần Tổng Quan (Áp dụng Tổng quát cho Mọi Tính Năng)

Skill này cung cấp phương pháp luận và công thức tổng quát giúp chuyển đổi **bất kỳ chức năng, sơ đồ trình tự (Sequence Diagram), hoặc luồng API kỹ thuật nào** thành đoạn diễn giải **ngôn ngữ tự nhiên, trực quan và dễ hiểu**, phục vụ các kỳ thi Nghiên cứu Khoa học Kỹ thuật (VISEF, Intel ISEF) hoặc báo cáo dành cho Ban Giám khảo / Nhà quản lý mỗi khi người dùng yêu cầu **"viết phần tổng quan"**.

---

## 1. Công Thức Diễn Giải Tổng Quát (3-4 Bước Mạch Lạc)

Khi được yêu cầu viết phần tổng quan cho một tính năng bất kỳ, hãy phân tích và diễn giải theo **công thức 3 đến 4 bước tự nhiên**:

1. **Bước 1: Khởi phát tác vụ (Người dùng / Thiết bị làm gì?)**  
   * Trình bày hành động khởi đầu dưới góc nhìn thực tế (Ví dụ: *Người dùng bấm nút Lưu; Camera chụp ảnh tại rừng; Kiểm lâm nhập SĐT mới...*).
2. **Bước 2: Xử lý & Ra quyết định tại Máy chủ Trung tâm**  
   * Giải thích cách hệ thống tiếp nhận thông tin, kiểm tra tính hợp lệ và ra quyết định xử lý mà không cần nhắc đến code, database hay thuật toán chi tiết.
   * **Nhấn mạnh bài toán định danh nếu có:** Làm rõ cách định danh đối tượng xử lý (ví dụ: *Định danh trạm camera theo mã `cameraId`; Định danh thiết bị nhận thông báo theo `fcmToken` hoặc SĐT*).
3. **Bước 3: Thực thi phản hồi & Cảnh báo thời gian thực**  
   * Mô tả kết quả phản hồi đến thiết bị (Ví dụ: *Phát loa xua đuổi/chớp LED tại chỗ; Gửi Push Notification sang ứng dụng di động; Bắn tin nhắn SMS cảnh báo...*).
4. **Bước 4: Lưu trữ nhật ký & Cập nhật giao diện**  
   * Mô tả việc ghi nhận thông tin vào hệ thống và cách giao diện ứng dụng tự động làm mới để người dùng theo dõi.

---

## 2. Nguyên Tắc "Phi Kỹ Thuật Hóa" (De-technicalization Rules)

Áp dụng bảng chuyển đổi ngôn ngữ cho **bất kỳ tài liệu/chức năng nào**:

| Thuật ngữ Kỹ thuật | Từ ngữ Diễn giải cho Giám khảo / Người đọc tổng quan |
|---|---|
| API Endpoint (`POST`, `GET`, `PUT`, `DELETE`) | Yêu cầu giao tiếp / Chức năng gửi dữ liệu về máy chủ trung tâm |
| HTTP Status (`200 OK`, `201 Created`, `400 Bad Request`) | Kết quả xử lý (Thành công, Thêm mới hoàn tất, Cảnh báo nhập sai dữ liệu) |
| Database / Query / SQL / Prisma | Cơ sở dữ liệu lưu trữ trung tâm |
| Payload / Body / JSON Object | Nội dung bản tin / Thông tin yêu cầu |
| `fcmToken` | Mã định danh thiết bị di động duy nhất do Google Firebase cấp |
| `cameraId` | Mã định danh riêng biệt của từng trạm camera tại hiện trường |
| `ownerId` / `userId` | Tài khoản người quản lý / chủ sở hữu thiết bị |
| Cooldown / Logic 30s | Bộ lọc thông báo thông minh (tránh gửi thông báo lặp lại gây phiền) |

---

## 3. Các Ví Dụ Minh Họa Áp Dụng Cho Nhiều Tính Năng

### Ví dụ A: Cho Tính năng Tự động Xua đuổi & Cảnh báo (Action 1.1)
> 1. **Bước 1: Chụp ảnh & Nhận dạng AI (Tại thực địa):** Camera chụp ảnh và truyền về máy chủ AI để mô hình thị giác máy tính nhận biết loài động vật.
> 2. **Bước 2: Ra quyết định Phản ứng & Xua đuổi Tức thì:** Máy chủ xác định đúng mã trạm (`cameraId`) và chủ sở hữu trạm (`ownerId`) để gửi lại kịch bản âm thanh/đèn LED xua đuổi chính xác cho trạm đó.
> 3. **Bước 3: Kích hoạt Cảnh báo Khẩn cấp Đa kênh:** Máy chủ gửi Push Notification tới thiết bị di động (qua mã định danh `fcmToken`) và gửi tin nhắn SMS tới danh sách SĐT đăng ký.
> 4. **Bước 4: Cập nhật Nhật ký & Hiển thị Trực quan:** Lưu nhật ký và làm mới giao diện ứng dụng.

### Ví dụ B: Cho Tính năng Tùy chỉnh Cấu hình Phòng vệ Theo Loài (Action 6.2)
> 1. **Bước 1: Tùy chỉnh Thông số trên Ứng dụng:** Người dùng/Kiểm lâm chọn loài động vật và thiết lập mẫu âm thanh, màu đèn LED nháy, độ âm lượng mong muốn rồi nhấn "Lưu".
> 2. **Bước 2: Kiểm tra & Lưu trữ Kịch bản tại Máy chủ:** Máy chủ tiếp nhận kịch bản phòng vệ mới, kiểm tra tính hợp lệ và ghi nhận vào hồ sơ cấu hình riêng của tài khoản người dùng đó.
> 3. **Bước 3: Áp dụng Tức thì cho Hệ thống:** Kịch bản mới lập tức có hiệu lực. Khi loài động vật này xuất hiện ở các trạm camera do người dùng quản lý, máy chủ sẽ kích hoạt đúng kịch bản vừa cài đặt.

### Ví dụ C: Cho Tính năng Quản lý Số Điện Thoại SMS Cảnh báo (Action 7.2)
> 1. **Bước 1: Nhập Số Điện Thoại Đăng ký:** Người dùng nhập số điện thoại của bản thân hoặc người dân sống lân cận vùng ranh giới rừng.
> 2. **Bước 2: Kiểm tra Giới hạn & Lưu trữ:** Máy chủ kiểm tra số lượng SĐT đã đăng ký (tối đa 3 số phụ/tài khoản) để đảm bảo không vượt quá định mức, sau đó lưu vào danh sách nhận tin.
> 3. **Bước 3: Kích hoạt Sẵn sàng Cảnh báo:** Danh sách SĐT sẵn sàng nhận tin nhắn SMS tự động mỗi khi có sự kiện động vật nguy hiểm xuất hiện.

---

## 4. Quy Trình Thực Hiện Khi Nhận Yêu Cầu "Viết phần tổng quan"

1. **Xác định Tính năng:** Xác định tính năng/mục tài liệu mà người dùng yêu cầu viết tổng quan.
2. **Áp dụng Công thức 4 Bước:** Soạn thảo đoạn diễn giải theo khối `> [!NOTE]` bằng ngôn ngữ trực quan, bình dị.
3. **Rà soát Thuật ngữ:** Đảm bảo không còn các từ ngữ thuần code/kỹ thuật gây khó hiểu cho Giám khảo.
