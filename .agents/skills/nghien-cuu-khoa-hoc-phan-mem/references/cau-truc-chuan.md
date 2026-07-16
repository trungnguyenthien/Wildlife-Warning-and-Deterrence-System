# Cấu trúc chuẩn — Đề tài NCKH lĩnh vực Phần mềm (cấp học sinh)

Áp dụng khi soạn file Markdown. Mỗi phần dưới đây tương ứng một hoặc nhiều heading (`##`/`###`)
trong file output. Giữ đúng thứ tự.

## 1. Trang bìa

Heading `#` tên đề tài, sau đó vài dòng thông tin:

- Lĩnh vực dự thi (ví dụ: Phần mềm hệ thống / Khoa học máy tính / Robotics và phần mềm thông minh)
- Tên tác giả, lớp, trường
- Giáo viên hướng dẫn
- Năm học

## 2. Tóm tắt (Abstract, ~250 từ)

Trả lời gọn trong 1 đoạn: vấn đề → phương pháp → kết quả chính → ý nghĩa/ứng dụng.
Không dùng thuật ngữ chưa giải thích, đây là phần giám khảo đọc đầu tiên.

## 3. Lời cảm ơn

Ngắn, không tính vào nội dung chấm điểm.

## 4. Mục lục

Có thể để dạng danh sách liên kết nội bộ markdown (`- [Phần I...](#phan-i...)`) nếu công cụ hiển thị hỗ trợ.

## 5. Phần I: Giới thiệu / Đặt vấn đề

- **Lý do chọn đề tài**: hiện trạng, số liệu/dẫn chứng cho thấy vấn đề có thật
- **Mục tiêu nghiên cứu**: cụ thể, đo lường được (tránh mục tiêu mơ hồ như "giúp ích cho xã hội")
- **Đối tượng & phạm vi nghiên cứu**: dùng cho ai, giới hạn kỹ thuật (nền tảng, ngôn ngữ, quy mô test)
- **Câu hỏi nghiên cứu / Giả thuyết khoa học**: PHẢI phát biểu rõ ràng và kiểm chứng được bằng số liệu.
  Đây là phần giám khảo VISEF/ISEF chú ý nhất. Ví dụ dạng câu hỏi tốt:
  "Liệu thuật toán X có giảm thời gian xử lý Y ít nhất Z% so với phương pháp hiện tại hay không?"

## 6. Phần II: Tổng quan tài liệu (Literature Review)

- Liệt kê giải pháp/phần mềm tương tự đã có (trong nước & quốc tế), có thể dùng bảng so sánh markdown
- Khoảng trống (gap) mà đề tài lấp vào — nêu rõ điểm khác biệt/cải tiến
- Cơ sở lý thuyết liên quan (thuật toán, mô hình, công nghệ nền tảng) — trích dẫn nguồn

## 7. Phần III: Phương pháp nghiên cứu

- Quy trình phát triển phần mềm áp dụng (Agile/Scrum, Waterfall, hoặc mô tả riêng)
- **Kiến trúc hệ thống**: chèn sơ đồ Mermaid (xem `mermaid-huong-dan.md`)
- Công nghệ/công cụ sử dụng và lý do chọn (nên trình bày dạng bảng: Công nghệ | Vai trò | Lý do chọn)
- **Thiết kế thuật toán** (nếu có AI/ML/xử lý dữ liệu): sơ đồ Mermaid flowchart hoặc pseudocode dạng code block
- Phương pháp kiểm thử: loại test (unit/integration/user test), số lượng người dùng thử, thời gian thử nghiệm
- Phương pháp đánh giá kết quả: tiêu chí đo lường cụ thể (độ chính xác %, thời gian phản hồi ms,
  điểm hài lòng theo thang Likert...) — nêu rõ công cụ thống kê nếu có

## 8. Phần IV: Kết quả và Thảo luận

- Sản phẩm đạt được: mô tả tính năng, có thể dùng bảng liệt kê tính năng đã hoàn thành
- Kết quả thử nghiệm/khảo sát: bảng số liệu, có thể mô tả biểu đồ bằng markdown table
  (nếu người dùng cần biểu đồ trực quan dạng chart thật sự, gợi ý dùng Visualizer/artifact riêng,
  không cố vẽ chart bằng Mermaid vì Mermaid không mạnh cho biểu đồ dữ liệu định lượng phức tạp)
- So sánh với mục tiêu ban đầu và với giải pháp đã có ở Phần II
- Thảo luận: hạn chế, nguyên nhân, bài học kinh nghiệm

## 9. Phần V: Kết luận và Hướng phát triển

- Trả lời lại trực tiếp câu hỏi nghiên cứu/giả thuyết đã nêu ở Phần I
- Đóng góp/ý nghĩa của đề tài
- Hướng phát triển tiếp theo

## 10. Tài liệu tham khảo

Theo chuẩn IEEE (phổ biến cho CS) hoặc APA nếu trường yêu cầu. Đánh số `[1]`, `[2]`...
và trích dẫn trong bài bằng `[1]`.

## 11. Phụ lục

- Mã nguồn chính (rút gọn, dạng code block, không dán toàn bộ repo)
- Sơ đồ chi tiết bổ sung (Mermaid)
- Phiếu khảo sát / form đồng ý tham gia (informed consent) nếu có thử nghiệm với người dùng thật
- Log kiểm thử / nhật ký nghiên cứu (research log) — ghi mốc thời gian các bước đã làm

---

## Lưu ý chấm điểm cần thể hiện xuyên suốt văn bản

- Không chỉ mô tả "sản phẩm chạy được" — phải chứng minh bằng số liệu nó tốt hơn giải pháp cũ.
- Nhật ký nghiên cứu (research log) nên xuất hiện rõ trong Phụ lục, ghi lại quá trình chứ không chỉ kết quả cuối.
- Nếu có khảo sát/thử nghiệm với người dùng thật, cần đính kèm phiếu đồng ý tham gia.
