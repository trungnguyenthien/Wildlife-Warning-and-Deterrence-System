# Kế hoạch Nâng cấp Mã nguồn: Tích hợp Kiến trúc Ably Pub/Sub

Tài liệu này phác thảo kế hoạch nâng cấp mã nguồn của dự án (Mobile Server, AI Server Simulator) để chuyển đổi từ WebSocket tự chạy sang hệ thống tin nhắn thời gian thực của Ably. Kế hoạch này được xây dựng dựa trên thiết kế kiến trúc đã thống nhất trong [260805_ably_plan.md](./260805_ably_plan.md).

---

## 1. Mục tiêu & Tài liệu Tham chiếu Thiết kế

1.  **Loại bỏ Raw WebSockets:** Gỡ bỏ các module thiết lập HTTP upgrade `/ws` và quản lý socket thủ công trên Mobile Server (Tham chiếu kế hoạch: [260805_ably_plan.md#31-03-mobile_apimd](./260805_ably_plan.md#31-03-mobile_apimd)).
2.  **Tích hợp Ably SDK:** 
    *   Tích hợp `ably` vào `wildlife-mobile-server` để publish lệnh điều khiển thời gian thực và lắng nghe phản hồi ACK từ trạm camera (Tham chiếu thiết kế kênh: [260805_ably_plan.md#2-thiet-ke-kenh-truyen-tin-channel-design](./260805_ably_plan.md#2-thiet-ke-kenh-truyen-tin-channel-design)).
    *   Thêm endpoint `/api/v1/auth/ably-token` để cấp Token Request an toàn cho các client (Tham chiếu đặc tả API: [03-mobile_api.md: API 13a.3](../03-mobile_api.md#13a3-get-authably-token)).
    *   Cập nhật công cụ giả lập AI Server để kết nối trực tiếp đến Ably Cloud (Tham chiếu kịch bản kết nối: [ai-server-ably.md](../ai-server-ably.md) và hướng dẫn [huong-dan-ably.md](../huong-dan-ably.md)).

---

## 2. Các thay đổi đề xuất (Proposed Changes)

### 2.1. Mobile Server (`wildlife-mobile-server`)

#### [MODIFY] [package.json](../../wildlife-mobile-server/package.json)
*   Thêm dependency `ably` vào dự án để hỗ trợ giao tiếp với Ably Cloud.
    ```json
    "dependencies": {
      "ably": "^2.0.0",
      ...
    }
    ```

#### [MODIFY] [src/app.ts](../../wildlife-mobile-server/src/app.ts)
*   Loại bỏ import và khởi tạo WebSocket server cục bộ (Gỡ bỏ hàm `setupWebSocket` cũ).
*   Khai báo API endpoint mới: `GET /api/v1/auth/ably-token` trỏ tới hàm `getAblyToken` trong `authController` (Xem chi tiết thiết kế API tại [03-mobile_api.md: API 13a.3](../03-mobile_api.md#13a3-get-authably-token)).

#### [MODIFY] [src/controllers/authController.ts](../../wildlife-mobile-server/src/controllers/authController.ts)
*   Thêm hàm `getAblyToken` sử dụng Ably Rest SDK để sinh Token Request an toàn cho Ranger/Camera (dùng khóa của Mobile Server):
    ```typescript
    import Ably from 'ably';
    const ablyRest = new Ably.Rest({ key: process.env.ABLY_MOBILE_SERVER_API_KEY });

    export async function getAblyToken(req: AuthenticatedRequest, res: Response) {
      try {
        const clientId = (req.query.clientId as string) || req.user?.id || 'anonymous';
        const tokenRequest = await ablyRest.auth.createTokenRequest({ clientId });
        return res.status(200).json(tokenRequest);
      } catch (error) {
        console.error('Lỗi sinh Ably Token Request:', error);
        return res.status(500).json({ error: 'server_error', message: 'Không thể sinh token xác thực Ably.' });
      }
    }
    ```

#### [MODIFY] [src/websocket.ts](../../wildlife-mobile-server/src/websocket.ts)
*   Loại bỏ hoàn toàn code setup `WebSocketServer` và `wss.handleUpgrade` cũ.
*   Viết lại hàm `sendDeviceCommand` để chuyển sang giao thức Ably (Tham chiếu sơ đồ sequence: [04-sequence-diagram.md: Sơ đồ 6.3](../04-sequence-diagram.md#63-action-test-speaker-sound-at-camera-station-ai_server)):
    *   Tự động mở kết nối `Ably.Realtime` tạm thời bằng `process.env.ABLY_MOBILE_SERVER_API_KEY`.
    *   Publish lệnh `DEVICE_COMMAND` tới kênh `camera:control:{cameraId}`.
    *   Subscribe kênh `camera:ack:{cameraId}`, chờ sự kiện `COMMAND_ACK` trùng khớp `commandId` (với timeout 5 giây).
    *   Ngắt kết nối Ably (gọi `realtime.close()`) và trả về kết quả (200 OK) hoặc ném lỗi Timeout (504 Gateway Timeout).

#### [MODIFY] [.env.local](../../wildlife-mobile-server/.env.local) & [.env.test](../../wildlife-mobile-server/.env.test)
*   Bổ sung các khóa cấu hình môi trường Ably được phân quyền (Least Privilege):
    ```env
    # Khóa cho Mobile Server (Quyền publish control, subscribe ack)
    ABLY_MOBILE_SERVER_API_KEY=abc123.DEF456:ghIjKlmNoPqrSTuv_mobile
    
    # Khóa cho AI Server / Simulators (Quyền subscribe control, publish ack)
    ABLY_AI_SERVER_API_KEY=abc123.DEF456:ghIjKlmNoPqrSTuv_ai
    ```

---

### 2.2. Công cụ mô phỏng AI Server (`html_tool`)

#### [MODIFY] [simulate_ai_server.html](../../html_tool/simulate_ai_server.html)
*   Nhúng thư viện Ably JS SDK từ CDN ở đầu file HTML:
    ```html
    <script src="https://cdn.ably.com/lib/ably.min-2.js"></script>
    ```
*   Thay thế đối tượng kết nối `new WebSocket(...)` bằng `new Ably.Realtime(...)`:
    *   Hỗ trợ kết nối bằng khóa thiết bị `ABLY_AI_SERVER_API_KEY` (nhập thủ công trên giao diện tool) hoặc tự động lấy token tạm thời thông qua endpoint `/auth/ably-token` (Tham chiếu cơ chế token tại [huong-dan-ably.md](../huong-dan-ably.md)).
    *   Đăng ký nhận tin nhắn trên kênh `camera:control:{cameraId}` với event name `DEVICE_COMMAND`.
    *   Khi nhận được lệnh, cập nhật đồ họa LED/Loa/Rào điện và tự động gửi (Publish) phản hồi `COMMAND_ACK` lên kênh `camera:ack:{cameraId}` (Tham chiếu payload tại [ai-server-ably.md](../ai-server-ably.md)).

---

## 3. Kế hoạch Kiểm tra & Nghiệm thu (Verification Plan)

### 3.1. Các ca kiểm thử tự động (Unit Tests)

#### 1. Mocking thư viện Ably trong môi trường test
Do thư viện `ably` gọi kết nối mạng thật đến Ably Cloud, chúng ta cần mock nó trong môi trường kiểm thử Jest để tránh bị treo/timeout.
Chúng ta sẽ tạo hoặc cấu hình file mock tại đầu bộ test của Auth:
```typescript
jest.mock('ably', () => {
  return {
    Rest: jest.fn().mockImplementation(() => ({
      auth: {
        createTokenRequest: jest.fn().mockResolvedValue({
          keyName: 'mock.key',
          clientId: 'mock-client',
          timestamp: Date.now(),
          nonce: 'mock-nonce',
          mac: 'mock-mac'
        })
      }
    })),
    Realtime: jest.fn().mockImplementation(() => ({
      connection: {
        once_async: jest.fn().mockResolvedValue('connected')
      },
      channels: {
        get: jest.fn().mockImplementation(() => ({
          publish: jest.fn().mockResolvedValue(true),
          subscribe: jest.fn().mockImplementation((event, callback) => {
            // Giả lập callback nhận COMMAND_ACK sau 100ms
            setTimeout(() => {
              callback({
                data: {
                  event: 'COMMAND_ACK',
                  payload: {
                    commandId: 'mock-cmd-id',
                    status: 'SUCCESS',
                    error: null
                  }
                }
              });
            }, 100);
          })
        }))
      },
      close: jest.fn()
    }))
  };
});
```

#### 2. Viết các Test Cases mới cho API `/auth/ably-token`
Chúng ta sẽ bổ sung các ca kiểm thử sau vào [tests/auth.test.ts](../../wildlife-mobile-server/tests/auth.test.ts):
*   **`TC_AUTH_TOK_SUCCESS_01`**: Lấy Ably Token Request thành công khi truyền JWT Token hợp lệ của Ranger hoặc Camera.
    *   *Mã trả về mong đợi:* `200 OK`.
    *   *Cấu trúc body:* Có đủ các trường `keyName`, `timestamp`, `nonce`, `mac`.
*   **`TC_AUTH_TOK_FAILURE_01`**: Thất bại khi không truyền mã JWT trong header Authorization.
    *   *Mã trả về mong đợi:* `401 Unauthorized`.
*   **`TC_AUTH_TOK_FAILURE_02`**: Thất bại khi truyền JWT Token không hợp lệ hoặc đã hết hạn.
    *   *Mã trả về mong đợi:* `401 Unauthorized`.

#### 3. Chạy kiểm thử tự động
*   Chạy bộ test của Mobile Server để đảm bảo các thay đổi không làm vỡ các nghiệp vụ khác và 100% testcases báo xanh:
    ```bash
    npm run test
    ```

### 3.2. Kiểm thử thủ công (Manual Verification)
1.  Khởi chạy Mobile Server cục bộ (`npm run dev`).
2.  Mở trang giả lập AI Server (`simulate_ai_server.html`), điền khóa `ABLY_AI_SERVER_API_KEY` và bấm **"Kết nối Ably"**. Xác nhận trạng thái hiển thị màu xanh lá cây `CONNECTED`.
3.  Mở trang giả lập Mobile App (`simulate_mobile_app.html`), đăng nhập và bấm **"Test Device"** kích hoạt còi/đèn LED.
4.  Xác nhận:
    *   Giả lập AI Server nhận được lệnh ngay lập tức, nháy đèn LED neon, chạy sóng volume loa.
    *   Giả lập AI Server tự động đẩy phản hồi ACK ngược lại.
    *   Giao diện Mobile App hiển thị thông báo thành công và Mobile Server trả về HTTP code 200.

---

### 3.3. Phương án Kiểm thử Độ ổn định Kết nối trên Production (Production Stability Verification)

Để đảm bảo kết nối giữa các trạm Camera (AI Server) và Mobile Server hoạt động cực kỳ ổn định, không bị mất gói tin hay rò rỉ bộ nhớ dài hạn trên môi trường thực tế, chúng tôi đề xuất 3 giải pháp giám sát sau:

#### 1. Ghi nhật ký trạng thái kết nối Ably (Ably Connection State Telemetry)
*   **Triển khai ở Client (AI Server):** Sử dụng các sự kiện vòng đời kết nối của Ably SDK để tự động ghi log vào hệ thống file local/DB:
    ```python
    # Lắng nghe sự thay đổi trạng thái kết nối
    realtime.connection.on(lambda state_change: log_status(state_change))
    ```
    *   **Trạng thái cần theo dõi:** `connected`, `disconnected`, `suspended`, `failed`, `update`.
    *   **Mục tiêu:** Kiểm tra tần suất và thời gian reconnect tự động của Camera khi chạy thực địa (24/7).

#### 2. Kịch bản Kiểm tra latency tuần hoàn (Hourly Round-Trip Ping Loop)
*   **Triển khai:** Tạo một script chạy cron-job mỗi 1 tiếng:
    1.  Tự động kích hoạt API điều khiển test thiết bị: `POST /cameras/{cameraId}/devices/led/test` với `durationSeconds: 0`.
    2.  Đo tổng thời gian (Round-Trip Latency) từ lúc Mobile Server phát lệnh REST -> Ably -> AI Server -> AI Server phản hồi ACK -> Mobile Server nhận lại qua REST.
    3.  Lưu trữ tỷ lệ thành công (Success Rate) và thời gian trễ trung bình. Nếu tỷ lệ thành công thấp hơn 98% hoặc trễ > 3s, gửi cảnh báo SMS/Email về hệ thống giám sát.

#### 3. Giám sát qua Trang Quản trị của Ably (Ably Dashboard & Integration Analytics)
*   **Cấu hình:** Sử dụng bảng điều khiển của Ably để theo dõi:
    *   **Message Delivery Rate:** Tỷ lệ gửi nhận tin nhắn thành công trên các kênh `camera:control:*` và `camera:ack:*`.
    *   **Token Requests Rate:** Theo dõi số lượng yêu cầu sinh token tạm từ client để phát hiện hành vi DDoS hoặc reconnect liên tục bất thường.
    *   **Connection Lifetimes:** Thời gian duy trì kết nối trung bình của các trạm thực địa.

