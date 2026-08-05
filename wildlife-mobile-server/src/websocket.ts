import Ably from 'ably';

/**
 * WebSocket setup function - No-op since we migrated to Ably Pub/Sub Cloud.
 */
export function setupWebSocket(_server?: unknown) {
  console.log('[Ably] Raw WebSockets disabled. Using Ably Cloud Broker for real-time messaging.');
}

/**
 * Gửi lệnh điều khiển thiết bị xuống AI Server qua Ably Pub/Sub và đợi phản hồi COMMAND_ACK trên kênh ack
 */
export function sendDeviceCommand(
  _userId: string,
  commandId: string,
  cameraId: string,
  deviceKey: string,
  action: string,
  params: Record<string, unknown>
): Promise<unknown> {
  return new Promise((resolve, reject) => {
    const key = process.env.ABLY_MOBILE_SERVER_API_KEY;
    if (!key) {
      return reject(new Error('Chưa thiết lập cấu hình khóa ABLY_MOBILE_SERVER_API_KEY trên server.'));
    }

    let realtime: Ably.Realtime | null = null;
    let timeoutId: NodeJS.Timeout | null = null;

    try {
      // Khởi tạo Realtime client kết nối thời gian thực wss://
      realtime = new Ably.Realtime({ key });

      // Lấy các kênh truyền tương ứng
      const controlChannel = realtime.channels.get(`camera:control:${cameraId}`);
      const ackChannel = realtime.channels.get(`camera:ack:${cameraId}`);

      // Thiết lập timeout 5 giây chờ phản hồi
      timeoutId = setTimeout(() => {
        if (realtime) {
          realtime.close();
        }
        reject(new Error('Quá thời gian phản hồi từ AI Server (Timeout 5s).'));
      }, 5000);

      // Chuẩn bị payload lệnh gửi đi
      const commandPayload = {
        event: 'DEVICE_COMMAND',
        payload: {
          commandId,
          cameraId,
          deviceKey,
          action,
          params
        }
      };

      // Đăng ký nhận tin nhắn phản hồi từ ACK Channel TRƯỚC,
      // rồi mới publish lệnh để tránh race condition bỏ lỡ ACK
      ackChannel.subscribe('message', (message) => {
        try {
          const data = typeof message.data === 'string' ? JSON.parse(message.data) : message.data;
          
          if (data && data.event === 'COMMAND_ACK' && data.payload) {
            const { commandId: ackCmdId, status, error } = data.payload;
            
            if (ackCmdId === commandId) {
              if (timeoutId) {
                clearTimeout(timeoutId);
              }
              if (realtime) {
                realtime.close();
              }

              if (status === 'SUCCESS') {
                resolve(data.payload);
              } else {
                reject(new Error(error || 'AI Server phản hồi thất bại.'));
              }
            }
          }
        } catch (err) {
          console.error('[Ably] Lỗi xử lý tin nhắn phản hồi ACK:', err);
        }
      }).then(() => {
        // Subscription đã được xác nhận active → bây giờ mới gửi lệnh đi
        controlChannel.publish('message', commandPayload).catch((err) => {
          if (timeoutId) {
            clearTimeout(timeoutId);
          }
          if (realtime) {
            realtime.close();
          }
          reject(err);
        });
      }).catch((err) => {
        if (timeoutId) {
          clearTimeout(timeoutId);
        }
        if (realtime) {
          realtime.close();
        }
        reject(err);
      });

    } catch (error: unknown) {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
      if (realtime) {
        realtime.close();
      }
      const err = error as Error;
      reject(new Error(err.message || 'Lỗi khi gửi lệnh qua Ably.'));
    }
  });
}
