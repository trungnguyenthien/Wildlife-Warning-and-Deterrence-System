# Chỉ thị dành cho AI Coding Assistant (System Instructions)

Bạn là một trợ lý lập trình AI được giao nhiệm vụ phát triển và kiểm thử tự động cho Mobile Server của dự án này. Hãy đọc kỹ các yêu cầu và quy trình dưới đây trước khi thực hiện bất kỳ hành động nào.

## 1. Yêu cầu đọc tài liệu nghiệp vụ bên ngoài
Trước khi viết mã nguồn hoặc viết testcase, bạn BẮT BUỘC phải đọc toàn bộ nội dung của các tài liệu đặc tả nằm tại thư mục tài liệu chung của dự án (nằm ở thư mục cha `../docs/` tương đối):
*   **Đặc tả API di động:** [03-mobile_api.md](../docs/03-mobile_api.md) (Chứa danh sách 37 API và cấu hình Request/Response mẫu).
*   **Sơ đồ tương tác Sequence:** [04-sequence-diagram.md](../docs/04-sequence-diagram.md) (Chứa luồng tương tác nghiệp vụ chi tiết của 18 Actions).
*   **Thiết kế Database:** [05-database.md](../docs/05-database.md) (Chứa sơ đồ ERD và cấu trúc chi tiết 11 bảng dữ liệu).
*   **Kế hoạch Kiến trúc:** [mobile_server_plan.md](../docs/mobile_server_plan.md) (Chứa kiến trúc tổng quan, thiết lập môi trường và cấu trúc thư mục).

## 2. Quy định cấu trúc viết code (TypeScript)
*   **Không chia nhỏ code:** Tuân thủ cấu trúc thư mục phẳng tại `src/controllers/` và `src/routes/`. Cấm tạo các thư mục con sâu hơn làm phức tạp luồng đọc của học sinh.
*   **Database Schema:** Mọi thay đổi về cấu trúc bảng phải được cập nhật trước vào `prisma/schema.prisma` dựa theo Đặc tả Database và chạy lệnh `npx prisma db push`.
*   **Kiểm tra cú pháp tĩnh:** Sau mỗi lần sinh code, bạn phải chạy lệnh `npm run build` để kiểm tra lỗi cú pháp TypeScript, không được để code lỗi biên dịch.

## 3. Quy trình tự động chạy test và sửa lỗi (Self-Healing Loop)
Khi phát triển một tính năng hoặc API endpoint, bạn phải tuân thủ quy trình vòng lặp tự động sau:
1.  **Viết Testcase:** Tạo tệp test tương ứng trong thư mục `tests/` sử dụng Jest + Supertest giả lập các case thành công và thất bại của API theo đúng đặc tả tài liệu 03.
2.  **Chạy Test:** Thực thi lệnh `npm run test` (chạy trên database PostgreSQL cục bộ độc lập `wildlife_test`).
3.  **Tự sửa lỗi (Fix Loop):**
    *   Nếu có testcase thất bại (`FAIL`), bạn phải tự đọc log lỗi từ terminal đầu ra.
    *   Xác định nguyên nhân (sai cấu trúc response, thiếu validate đầu vào, sai logic DB, v.v.).
    *   Tiến hành sửa mã nguồn trực tiếp tại các file controller liên quan.
    *   Lặp lại việc chạy test (`npm run test`) cho đến khi toàn bộ test suite báo màu xanh (`PASS 100%`).
4.  **Xác thực tổng thể:** Chạy lệnh `npm run validate` để đảm bảo code sạch lỗi cú pháp, compile thành công và pass toàn bộ test trước khi báo cáo hoàn thành cho người dùng.
