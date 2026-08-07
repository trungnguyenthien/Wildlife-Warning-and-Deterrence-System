# 🦁 Hệ thống Cảnh báo & Xua đuổi Động vật Hoang dã

Hệ thống IoT tích hợp **AI phát hiện động vật hoang dã**, gửi **cảnh báo tức thời** và kích hoạt **các biện pháp xua đuổi an toàn** (đèn LED chớp, loa phát âm thanh, hàng rào điện sinh học) ngay tại trạm camera, giúp giảm thiểu xung đột giữa con người và động vật hoang dã.

> Dự án được xây dựng theo tư tưởng **Spec Driven Development (SDD)** — đặc tả là nguồn chân lý duy nhất, con người thiết kế và AI hiện thực hóa. Chi tiết xem [docs/README.md](docs/README.md).

---

## 🔗 Liên kết Web

### Công cụ Giả lập (GitHub Pages)

Các công cụ giả lập tĩnh chạy trên trình duyệt, mô phỏng đầy đủ các phân hệ để demo và kiểm thử hệ thống mà không cần phần cứng thật:

| Công cụ | Mô tả | Liên kết |
| :--- | :--- | :--- |
| **AI Server Simulator** | Mô phỏng AI Server: nhận webhook phát hiện (Uplink), lắng nghe lệnh điều khiển thiết bị qua Ably (Downlink), mô phỏng phần cứng (LED / loa / hàng rào điện) | [simulate_ai_server.html](https://trungnguyenthien.github.io/Wildlife-Warning-and-Deterrence-System/html_tool/simulate_ai_server.html) |
| **Mobile App Simulator** | Mô phỏng giao diện & hành vi của ứng dụng Android dành cho kiểm lâm | [simulate_mobile_app.html](https://trungnguyenthien.github.io/Wildlife-Warning-and-Deterrence-System/html_tool/simulate_mobile_app.html) |
| **Post Detections Simulator** | Mô phỏng gửi sự kiện phát hiện động vật (webhook) lên Mobile Server | [simulate_post_detections.html](https://trungnguyenthien.github.io/Wildlife-Warning-and-Deterrence-System/html_tool/simulate_post_detections.html) |

### API Server (Vercel)

Mobile Server (REST API) triển khai trên Vercel:

- 🌐 **Production API:** https://wildlife-warning-and-deterrence-sys.vercel.app

### Tài liệu thiết kế (GitHub Pages)

Toàn bộ tài liệu đặc tả hệ thống được publish trên GitHub Pages:

- 📚 **Danh mục tài liệu:** https://trungnguyenthien.github.io/Wildlife-Warning-and-Deterrence-System/docs/

---

## 🧱 Kiến trúc tổng quan

```
┌──────────────┐   REST API    ┌──────────────────────┐   Push lệnh   ┌──────────────┐
│ Mobile App   │ ────────────► │  Mobile Server       │ ─────────────► │  Ably Cloud  │
│ (Android)    │ ◄──────────── │  (Vercel, Serverless)│ ◄───────────── │   Broker     │
└──────────────┘  JSON/SSE/FCM └──────────┬───────────┘                └──────┬───────┘
                                          │                                  │ WS wss://
                                          ▼                                  ▼
                               ┌─────────────────────────────┐   ┌──────────────────────┐
                               │  Cơ sở dữ liệu + FCM / SMS  │   │  AI Server / Camera  │
                               └─────────────────────────────┘   │  (LED, loa, hàng rào) │
                                                                 └──────────────────────┘
```

Các phân hệ chính:

- **Mobile App (Android):** Giao diện kiểm lâm — theo dõi camera, nhận cảnh báo, điều khiển kiểm thử thiết bị từ xa.
- **Mobile Server (Vercel):** REST API stateless (không trạng thái) — quản lý camera, sự kiện phát hiện, đẩy lệnh điều khiển thiết bị qua **Ably Pub/Sub**, gửi thông báo FCM/SMS.
- **AI Server / Camera:** Phát hiện động vật bằng AI, chạy phần cứng xua đuổi (đèn LED chớp, loa âm thanh, hàng rào điện), nhận lệnh điều khiển từ xa.
- **Ably Cloud Broker:** Hạ tầng thời gian thực thay thế WebSocket tự vận hành — kênh `user:control:{userId}` / `user:ack:{userId}`.

---

## 📂 Cấu trúc Repository

| Thư mục | Nội dung |
| :--- | :--- |
| [`docs/`](docs/) | Tài liệu đặc tả hệ thống (nghiệp vụ, UI, API, sequence diagram, database, test case, tích hợp Ably) |
| [`html_tool/`](html_tool/) | Các công cụ giả lập tĩnh (HTML/JS) — chạy local hoặc deploy lên GitHub Pages |
| [`wildlife-mobile/`](wildlife-mobile/) | Ứng dụng Android (Mobile Client) |
| [`wildlife-mobile-server/`](wildlife-mobile-server/) | Mobile Server (Node.js REST API, deploy trên Vercel) |
| `.github/workflows/` | Workflow GitHub Actions deploy GitHub Pages |

---

## 🚀 Demo nhanh

1. Mở **AI Server Simulator** (link ở trên), nhập Ably API Key và bấm **Kết nối** để lắng nghe lệnh điều khiển.
2. Dùng **Post Detections Simulator** gửi một sự kiện phát hiện động vật lên API.
3. Xem **Mobile App Simulator** hiển thị cảnh báo tức thời.

---

## 📖 Hướng dẫn & Tài liệu chính

- [Đề tài nghiên cứu & cơ chế cảnh báo](docs/01-de-tai-nghien-cuu-canh-bao-dong-vat.md)
- [Đặc tả màn hình Android App](docs/02-dac-ta-man-hinh-android-app.md)
- [Đặc tả REST API](docs/03-mobile_api.md)
- [Sơ đồ trình tự hệ thống](docs/04-sequence-diagram.md)
- [Thiết kế cơ sở dữ liệu](docs/05-database.md)
- [Kịch bản kiểm thử API](docs/06-test-mobile-api.md)
- [Giao tiếp thời gian thực qua Ably](docs/ai-server-ably.md)
