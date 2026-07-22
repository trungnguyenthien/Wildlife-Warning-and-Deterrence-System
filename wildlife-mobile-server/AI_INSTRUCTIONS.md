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

## 3. Quy trình tự động đồng bộ tài liệu, chạy test và sửa lỗi (Self-Healing Loop)
Khi phát triển, cập nhật tính năng hoặc API endpoint, bạn BẮT BUỘC phải thực hiện đúng theo trình tự 4 bước sau đây:

1.  **Đồng bộ tài liệu kiểm thử (Sync Test Spec):** Sao chép đè nội dung từ tệp đặc tả kiểm thử bên ngoài dự án `../docs/06-test-mobile-api.md` vào tệp cục bộ `test-mobile-api.md` nằm ở thư mục gốc của server (nếu chưa có thì tự động tạo mới).
2.  **Kiểm tra thay đổi (Check Git Diff):** Sử dụng các lệnh Git (`git diff`) hoặc so sánh nội dung của tệp `test-mobile-api.md` hiện tại với commit trước đó để xác định những ca kiểm thử nào mới được thêm vào, cập nhật, hoặc bị gắn thẻ `[DELETED]`.
3.  **Cập nhật mã nguồn Testcase (Update Test Code):**
    *   Tiến hành cập nhật hoặc viết thêm các file test Jest + Supertest tương ứng trong thư mục `tests/` dựa trên kết quả diff vừa phân tích.
    *   Nếu một testcase trong tài liệu bị gắn thẻ `[DELETED]`, hãy gỡ bỏ testcase tương ứng đó ra khỏi mã nguồn test Jest.
4.  **Chạy kiểm thử và tự động sửa lỗi (Self-Healing Fix Loop):**
    *   Thực thi lệnh `npm run test` (chạy trên database PostgreSQL cục bộ độc lập `wildlife_test`).
    *   Nếu có testcase thất bại (`FAIL`), bạn phải tự đọc log lỗi chi tiết từ terminal đầu ra.
    *   Xác định nguyên nhân (sai cấu trúc response, thiếu validate đầu vào, sai logic DB, v.v.) và sửa trực tiếp tại các file controller liên quan.
    *   Lặp lại việc chạy test (`npm run test`) cho đến khi toàn bộ test suite báo màu xanh (`PASS 100%`).
5.  **Xác thực tổng thể & Xuất Test Report JSON:** 
    *   Thực thi lệnh `npm run test:report` để chạy bộ test tích hợp và xuất kết quả ra tệp `test-report.json` tại thư mục gốc của server.
    *   Chạy lệnh `npm run validate` để đảm bảo code sạch lỗi cú pháp, compile thành công, pass toàn bộ test và xuất file report JSON đầy đủ trước khi báo cáo hoàn thành cho người dùng.

## 6. Môi trường Production (Vercel)

*   **Production URL:** `https://wildlife-warning-and-deterrence-sys.vercel.app`
*   **Health Check:** `GET https://wildlife-warning-and-deterrence-sys.vercel.app/health` → trả về `{ "status": "OK" }`.
*   **Platform:** Vercel Serverless (Node.js), kết nối PostgreSQL qua `DATABASE_URL` trong Vercel Environment Variables.

### Lưu ý khi deploy lên Vercel
*   `prisma generate` được tự động chạy qua script `postinstall` trong `package.json` — **không cần** cấu hình thêm.
*   Mọi thay đổi schema phải được push lên database production thủ công bằng `DATABASE_URL=<prod_url> npx prisma db push` trước khi deploy.

### Quy trình kiểm tra trên Production
Sau khi code được deploy thành công lên Vercel, bạn PHẢI thực hiện smoke-test tối thiểu bằng các lệnh `curl` sau để xác nhận server production hoạt động đúng:

```bash
BASE="https://wildlife-warning-and-deterrence-sys.vercel.app"

# 1. Health check
curl -s "$BASE/health" | jq .

# 2. Đăng ký tài khoản test
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"smoke_ranger","password":"password123","fullName":"Smoke Test","phoneNumber":"+84900000001","role":"RANGER"}' | jq .

# 3. Đăng nhập lấy token
TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"smoke_ranger","password":"password123"}' | jq -r '.token')
echo "Token: $TOKEN"

# 4. Lấy danh sách camera (yêu cầu auth)
curl -s "$BASE/cameras" -H "Authorization: Bearer $TOKEN" | jq .
```

Nếu bất kỳ bước nào trả về lỗi HTTP 5xx, bạn phải kiểm tra Vercel Function Logs và sửa lỗi ở controller tương ứng rồi deploy lại.
