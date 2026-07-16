# Hướng dẫn Mermaid cho tài liệu NCKH phần mềm

Mỗi block Mermaid trong markdown viết dạng:

    ```mermaid
    <nội dung sơ đồ>
    ```

Luôn đặt một dòng chú thích `**Hình N: <tên sơ đồ>**` ngay phía trên block, đánh số N tăng dần
xuyên suốt toàn tài liệu (không reset theo từng phần).

## 1. Kiến trúc hệ thống (dùng ở Phần III)

Dùng `flowchart TB` với `subgraph` để nhóm theo layer.

```mermaid
flowchart TB
    subgraph UI["Giao dien nguoi dung"]
        A["Man hinh chinh"]
        B["Man hinh ket qua"]
    end
    subgraph Logic["Xu ly nghiep vu"]
        C["Module xu ly du lieu dau vao"]
        D["Module thuat toan chinh"]
    end
    subgraph Data["Du lieu"]
        E["Co so du lieu / API ben ngoai"]
    end
    A --> C
    C --> D
    D --> E
    D --> B
```

## 2. Flowchart thuật toán (dùng ở Phần III, phần thiết kế thuật toán)

Dùng node hình thoi `{...}` cho điều kiện rẽ nhánh.

```mermaid
flowchart TD
    Start(["Bat dau"]) --> Input["Nhan du lieu dau vao"]
    Input --> Check{"Du lieu hop le?"}
    Check -- "Khong" --> Error["Bao loi va yeu cau nhap lai"]
    Error --> Input
    Check -- "Co" --> Process["Xu ly theo thuat toan chinh"]
    Process --> Output["Xuat ket qua"]
    Output --> End(["Ket thuc"])
```

## 3. Sequence Diagram — luồng tương tác người dùng/hệ thống (dùng ở Phần III)

```mermaid
sequenceDiagram
    participant NguoiDung as Nguoi dung
    participant App as Ung dung
    participant Server as May chu

    NguoiDung->>App: Nhap yeu cau
    App->>Server: Gui request
    Server-->>App: Tra ve ket qua
    App-->>NguoiDung: Hien thi ket qua
```

## 4. ERD — mô hình dữ liệu (nếu đề tài có phần lưu trữ dữ liệu phức tạp)

```mermaid
erDiagram
    NGUOIDUNG ||--o{ PHIENSUDUNG : "tao ra"
    PHIENSUDUNG ||--|{ KETQUA : "chua"
    NGUOIDUNG {
        string id
        string ten
    }
    KETQUA {
        string id
        float diem_so
    }
```

## 5. Gantt — tiến độ nghiên cứu (dùng ở Phụ lục, nhật ký nghiên cứu)

```mermaid
gantt
    dateFormat  YYYY-MM-DD
    title Tien do nghien cuu
    section Giai doan 1
    Khao sat va dat van de       :a1, 2026-01-01, 20d
    section Giai doan 2
    Thiet ke va xay dung phan mem :a2, after a1, 30d
    section Giai doan 3
    Kiem thu va thu thap so lieu  :a3, after a2, 20d
    section Giai doan 4
    Viet bao cao                 :a4, after a3, 15d
```

## Lưu ý tránh lỗi cú pháp

- Không dùng dấu `(` `)` `:` `,` bên ngoài dấu ngoặc kép trong nhãn node — luôn bọc nhãn bằng `["..."]`.
- Ưu tiên viết tiếng Việt không dấu bên trong node để giảm rủi ro lỗi hiển thị ở một số renderer;
  nếu người dùng yêu cầu rõ cần có dấu, vẫn dùng được miễn bọc trong ngoặc kép `"..."`.
- Không vẽ biểu đồ dữ liệu định lượng (cột, đường, tròn) bằng Mermaid — Mermaid chỉ hỗ trợ `pie`
  rất cơ bản; nếu cần biểu đồ số liệu đẹp, gợi ý người dùng dùng bảng markdown hoặc tạo artifact
  biểu đồ riêng (chart) thay vì ép dùng Mermaid.
