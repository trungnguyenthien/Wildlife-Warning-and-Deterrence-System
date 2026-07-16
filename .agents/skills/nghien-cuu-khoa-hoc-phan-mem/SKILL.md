---
name: nghien-cuu-khoa-hoc-phan-mem
description: Soạn thảo tài liệu đề tài nghiên cứu khoa học kỹ thuật (KHKT) cấp học sinh trong lĩnh vực phần mềm/tin học, theo cấu trúc chuẩn kiểu VISEF/Intel ISEF, xuất ra file Markdown (.md) có sơ đồ Mermaid. Dùng skill này bất cứ khi nào người dùng nhắc đến "đề tài nghiên cứu khoa học", "báo cáo NCKH", "dự thi KHKT", "VISEF", "ISEF", hoặc muốn viết/tạo/chỉnh sửa tài liệu nghiên cứu cho một dự án phần mềm của học sinh — kể cả khi họ chỉ mô tả ý tưởng phần mềm và muốn "viết thành báo cáo nghiên cứu" hoặc "làm tài liệu dự thi". Cũng dùng khi người dùng muốn thêm/chỉnh sơ đồ Mermaid (kiến trúc hệ thống, flowchart thuật toán, sequence diagram) vào một tài liệu nghiên cứu đã có.
---

# Soạn tài liệu Nghiên cứu Khoa học Kỹ thuật — Lĩnh vực Phần mềm

## Mục đích

Tạo file Markdown hoàn chỉnh cho đề tài nghiên cứu khoa học kỹ thuật (KHKT) cấp học sinh,
lĩnh vực phần mềm/tin học, theo cấu trúc chuẩn dùng trong các cuộc thi như VISEF
(Vietnam Science and Engineering Fair) và Intel ISEF. Output là **một file `.md`**,
có thể chứa các sơ đồ **Mermaid** (kiến trúc hệ thống, flowchart thuật toán, sequence diagram,
ERD...) hiển thị trực tiếp khi mở bằng công cụ hỗ trợ Mermaid (GitHub, Obsidian, VSCode, Notion...).

Không dùng công cụ tạo `docx` cho tác vụ này trừ khi người dùng đổi ý và yêu cầu rõ file Word.

## Quy trình

### Bước 1 — Thu thập thông tin

Trước khi viết, cần nắm được (hỏi người dùng nếu thiếu, nhưng đừng hỏi dồn dập — ưu tiên suy luận
từ những gì họ đã cung cấp và hỏi gọn 1-2 câu còn thiếu quan trọng nhất):

1. Tên đề tài / ý tưởng phần mềm là gì, giải quyết vấn đề gì
2. Đối tượng người dùng mục tiêu
3. Công nghệ/nền tảng dự kiến (mobile, web, AI/ML, IoT...)
4. Đã có phần nào viết sẵn chưa (ý tưởng thô, code, số liệu khảo sát...) hay cần Antigravity soạn từ đầu
5. Mức độ hoàn chỉnh mong muốn: khung sườn để điền dần, hay bản nháp đầy đủ nội dung mẫu

Nếu người dùng chỉ đưa ý tưởng ngắn gọn, được phép chủ động soạn nội dung hợp lý cho toàn bộ khung,
đánh dấu rõ những chỗ mang tính giả định/ví dụ bằng cách viết trong ngoặc vuông, ví dụ:
`[Điền số liệu khảo sát thực tế tại đây]`, để người dùng biết cần bổ sung.

### Bước 2 — Dựng file Markdown theo cấu trúc chuẩn

Dùng đúng cấu trúc trong `references/cau-truc-chuan.md`. Đây là khung 11 phần bắt buộc
(trang bìa, tóm tắt, lời cảm ơn, mục lục, 5 phần nội dung chính, tài liệu tham khảo, phụ lục).
Đọc file đó để lấy chi tiết từng phần và các câu hỏi gợi ý nội dung cho từng mục.

### Bước 3 — Chèn sơ đồ Mermaid đúng chỗ

Phần III (Phương pháp nghiên cứu) và Phần IV (Kết quả) là nơi bắt buộc phải có sơ đồ trực quan.
Xem `references/mermaid-huong-dan.md` để lấy template Mermaid theo từng loại nội dung
(kiến trúc hệ thống → `flowchart`/`graph`, thuật toán → `flowchart` dạng quyết định,
luồng tương tác người dùng-hệ thống → `sequenceDiagram`, mô hình dữ liệu → `erDiagram`,
tiến độ nghiên cứu → `gantt`). Luôn đặt sơ đồ ngay sau đoạn văn mô tả nó, không dồn hết
sơ đồ vào cuối tài liệu.

Nguyên tắc khi viết Mermaid:

- Dùng tiếng Việt không dấu hoặc có dấu trong ngoặc kép `["Tên node"]` để tránh lỗi cú pháp
  với ký tự đặc biệt; test nhanh bằng cách kiểm tra không có ký tự `(` `)` `:` nằm ngoài ngoặc kép trong nhãn node.
- Mỗi sơ đồ nên có tiêu đề bằng dòng text markdown ngay phía trên (`**Hình 1: Kiến trúc hệ thống**`),
  đánh số thứ tự "Hình" tăng dần xuyên suốt tài liệu vì ban giám khảo hay tham chiếu ngược lại số hình.
- Sơ đồ kiến trúc hệ thống: ưu tiên `flowchart TB` hoặc `flowchart LR` với `subgraph` để nhóm
  các layer (UI / Business Logic / Data).
- Sơ đồ thuật toán: dùng `flowchart TD` với node hình thoi `{...}` cho điều kiện rẽ nhánh.

### Bước 4 — Tạo file và giao cho người dùng

1. Tạo file `.md` tại thư mục workspace của người dùng (ví dụ: tạo trong thư mục `outputs/de-tai-nghien-cuu-<slug-ten-de-tai>.md`).
2. Dùng công cụ `write_to_file` để tạo file và thông báo đường dẫn tuyệt đối cho người dùng.
3. Trong câu trả lời kèm theo, liệt kê ngắn gọn những phần còn để trống/giả định cần người dùng
   bổ sung số liệu thật (khảo sát, benchmark, ảnh chụp màn hình sản phẩm...).

### Khi chỉnh sửa tài liệu đã có

Nếu người dùng đưa file `.md` đã có sẵn và muốn bổ sung/sửa (thêm sơ đồ, viết thêm phần,
định dạng lại theo cấu trúc chuẩn): đọc file bằng `view_file`, dùng `replace_file_content` hoặc `multi_replace_file_content` để chỉnh từng phần,
không viết lại toàn bộ file trừ khi người dùng yêu cầu tái cấu trúc hoàn toàn.

## Tài liệu tham khảo trong skill

- `references/cau-truc-chuan.md` — Chi tiết 11 phần của cấu trúc chuẩn, câu hỏi gợi ý nội dung
  từng mục, và một số lưu ý chấm điểm VISEF/ISEF cần nhấn mạnh trong văn bản (giả thuyết khoa học,
  phương pháp kiểm chứng, nhật ký nghiên cứu).
- `references/mermaid-huong-dan.md` — Template Mermaid sẵn dùng cho 5 loại sơ đồ thường gặp
  trong đề tài phần mềm, kèm ví dụ cụ thể.
