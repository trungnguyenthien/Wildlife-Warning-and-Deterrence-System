import { IncomingMessage, Server } from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import parse from 'url';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

// Quản lý các kết nối WebSocket đang hoạt động theo userId
const userSockets = new Map<string, WebSocket>();

const pendingCommands = new Map<string, {
  resolve: (value: unknown) => void;
  reject: (reason: Error) => void;
  timeoutId: NodeJS.Timeout;
}>();

export function setupWebSocket(server: Server) {
  const wss = new WebSocketServer({ noServer: true });

  // Xử lý nâng cấp kết nối từ HTTP Server
  server.on('upgrade', async (request: IncomingMessage, socket, head) => {
    const { pathname, query } = parse.parse(request.url || '', true);

    if (pathname !== '/ws') {
      return;
    }

    const userId = query.userId as string;
    if (!userId) {
      socket.write('HTTP/1.1 400 Bad Request\r\n\r\n');
      socket.destroy();
      return;
    }

    try {
      // Xác thực userId tồn tại trong DB
      const user = await prisma.user.findUnique({
        where: { id: userId }
      });

      if (!user) {
        socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
        socket.destroy();
        return;
      }

      // Thực hiện nâng cấp kết nối
      wss.handleUpgrade(request, socket, head, (ws) => {
        wss.emit('connection', ws, request, userId);
      });
    } catch (error) {
      console.error('Lỗi khi nâng cấp kết nối WS:', error);
      socket.write('HTTP/1.1 500 Internal Server Error\r\n\r\n');
      socket.destroy();
    }
  });

  // Khi kết nối WebSocket được thiết lập thành công
  wss.on('connection', (ws: WebSocket, _request: IncomingMessage, userId: string) => {
    console.log(`[WS] User ${userId} đã kết nối.`);
    
    // Lưu socket vào Map (ghi đè nếu có kết nối cũ của cùng một user)
    const oldSocket = userSockets.get(userId);
    if (oldSocket) {
      oldSocket.close();
    }
    userSockets.set(userId, ws);

    ws.on('message', (message: string) => {
      try {
        const data = JSON.parse(message);
        
        // Xử lý bản tin phản hồi COMMAND_ACK từ AI Server
        if (data.event === 'COMMAND_ACK' && data.payload) {
          const { commandId, status, error } = data.payload;
          const pending = pendingCommands.get(commandId);
          
          if (pending) {
            clearTimeout(pending.timeoutId);
            pendingCommands.delete(commandId);

            if (status === 'SUCCESS') {
              pending.resolve(data.payload);
            } else {
              pending.reject(new Error(error || 'AI Server phản hồi thất bại.'));
            }
          }
        }
      } catch (err) {
        console.error('[WS] Lỗi xử lý tin nhắn nhận được:', err);
      }
    });

    ws.on('close', () => {
      console.log(`[WS] User ${userId} đã ngắt kết nối.`);
      if (userSockets.get(userId) === ws) {
        userSockets.delete(userId);
      }
    });

    ws.on('error', (err) => {
      console.error(`[WS] Lỗi kết nối của User ${userId}:`, err);
      if (userSockets.get(userId) === ws) {
        userSockets.delete(userId);
      }
    });
  });
}

/**
 * Gửi lệnh điều khiển thiết bị xuống AI Server qua WebSocket và đợi phản hồi COMMAND_ACK
 */
export function sendDeviceCommand(
  userId: string,
  commandId: string,
  cameraId: string,
  deviceKey: string,
  action: string,
  params: Record<string, unknown>
): Promise<unknown> {
  return new Promise((resolve, reject) => {
    const ws = userSockets.get(userId);
    
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      return reject(new Error('AI Server của người dùng hiện đang ngoại tuyến (Offline).'));
    }

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

    // Gửi lệnh qua WebSocket
    ws.send(JSON.stringify(commandPayload));

    // Thiết lập timeout 5 giây chờ phản hồi
    const timeoutId = setTimeout(() => {
      pendingCommands.delete(commandId);
      reject(new Error('Quá thời gian phản hồi từ AI Server (Timeout 5s).'));
    }, 5000);

    // Lưu vào danh sách chờ
    pendingCommands.set(commandId, { resolve, reject, timeoutId });
  });
}
