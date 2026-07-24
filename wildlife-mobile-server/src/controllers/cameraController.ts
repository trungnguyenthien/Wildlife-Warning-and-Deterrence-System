import { Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middlewares/auth';
import { sendDeviceCommand } from '../websocket';

const prisma = new PrismaClient();

// SSE Emitter List để phục vụ cập nhật realtime
export const sseClients: Response[] = [];

// 1. GET /cameras - Lấy danh sách camera
export async function listCameras(_req: AuthenticatedRequest, res: Response) {
  try {
    const cameras = await prisma.camera.findMany({
      orderBy: { createdAt: 'desc' }
    });

    // Gom dữ liệu trả về kèm theo snapshot gần nhất
    const result = await Promise.all(
      cameras.map(async (cam) => {
        // Tìm event gần nhất của camera này
        const latestEvent = await prisma.event.findFirst({
          where: { cameraId: cam.id },
          orderBy: { detectedAt: 'desc' }
        });

        return {
          id: cam.id,
          name: cam.name,
          location: {
            lat: cam.latitude,
            lng: cam.longitude,
            address: cam.address
          },
          status: cam.status,
          liveFeedUrl: cam.liveFeedUrl,
          snapshot: latestEvent
            ? {
                url: latestEvent.snapshotUrl,
                capturedAt: latestEvent.detectedAt.toISOString()
              }
            : null
        };
      })
    );

    return res.status(200).json(result);
  } catch (error) {
    console.error('Lỗi khi tải danh sách camera:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}

// 2. GET /cameras/{cameraId} - Lấy chi tiết camera
export async function getCamera(req: AuthenticatedRequest, res: Response) {
  const { cameraId } = req.params;

  if (cameraId.length > 50) {
    return res.status(400).json({ error: 'invalid_camera_id', message: 'Mã camera quá dài. Giới hạn tối đa 50 ký tự.' });
  }

  try {
    const camera = await prisma.camera.findUnique({
      where: { id: cameraId }
    });

    if (!camera) {
      return res.status(404).json({ error: 'not_found_camera', message: 'Không tìm thấy trạm camera yêu cầu.' });
    }

    // Tìm sự kiện hoạt động gần nhất trong vòng 5 phút trước
    const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000);
    const activeEvent = await prisma.event.findFirst({
      where: {
        cameraId: camera.id,
        detectedAt: { gte: fiveMinutesAgo }
      },
      orderBy: { detectedAt: 'desc' },
      include: {
        eventDetections: {
          include: { species: true }
        }
      }
    });

    let currentDetection = null;
    if (activeEvent) {
      currentDetection = {
        eventId: activeEvent.id,
        detectedAt: activeEvent.detectedAt.toISOString(),
        detections: activeEvent.eventDetections.map((d) => ({
          speciesId: d.speciesId,
          displayName: d.species.displayName,
          confidence: d.confidence
        }))
      };
    }

    return res.status(200).json({
      id: camera.id,
      name: camera.name,
      location: {
        lat: camera.latitude,
        lng: camera.longitude,
        address: camera.address
      },
      status: camera.status,
      liveFeedUrl: camera.liveFeedUrl,
      currentDetection
    });
  } catch (error) {
    console.error('Lỗi khi lấy chi tiết camera:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 3. PATCH /cameras/{cameraId} - Đổi tên camera
export async function renameCamera(req: AuthenticatedRequest, res: Response) {
  const { cameraId } = req.params;
  const { name } = req.body;

  // Lấy vai trò user
  if (req.user?.role !== 'RANGER') {
    return res.status(403).json({ error: 'forbidden_ranger_only', message: 'Quyền truy cập bị từ chối: Chỉ kiểm lâm mới có thể đổi tên camera.' });
  }

  if (name === undefined || name === '') {
    return res.status(400).json({ error: 'missed_name', message: 'Thiếu thông tin bắt buộc: name không được để trống.' });
  }

  if (name.length > 100) {
    return res.status(400).json({ error: 'invalid_camera_name', message: 'Tên camera vượt quá giới hạn tối đa 100 ký tự.' });
  }

  try {
    const camera = await prisma.camera.findUnique({
      where: { id: cameraId }
    });

    if (!camera) {
      return res.status(404).json({ error: 'not_found_camera', message: 'Không tìm thấy trạm camera yêu cầu.' });
    }

    const updatedCamera = await prisma.camera.update({
      where: { id: cameraId },
      data: { name }
    });

    // Phát tín hiệu SSE cập nhật thời gian thực
    notifySSE({ event: 'CAMERA_UPDATE', data: updatedCamera });

    return res.status(200).json({
      id: updatedCamera.id,
      name: updatedCamera.name,
      location: {
        lat: updatedCamera.latitude,
        lng: updatedCamera.longitude,
        address: updatedCamera.address
      },
      status: updatedCamera.status,
      liveFeedUrl: updatedCamera.liveFeedUrl
    });
  } catch (error) {
    console.error('Lỗi khi đổi tên camera:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}

// 4. GET /cameras/stream - Kênh SSE phát sự kiện thời gian thực
export async function streamCameras(req: AuthenticatedRequest, res: Response) {
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');

  // Gửi heartbeat ban đầu
  res.write(': keepalive\n\n');

  sseClients.push(res);

  req.on('close', () => {
    const index = sseClients.indexOf(res);
    if (index !== -1) {
      sseClients.splice(index, 1);
    }
  });
}

// Helper thông báo qua SSE
export function notifySSE(message: { event: string; data: unknown }) {
  const payload = `event: ${message.event}\ndata: ${JSON.stringify(message.data)}\n\n`;
  sseClients.forEach((client) => {
    client.write(payload);
  });
}

// 5. POST /cameras/{cameraId}/devices/{deviceKey}/test - Gửi lệnh kiểm tra thiết bị ngoại vi
export async function testDevice(req: AuthenticatedRequest, res: Response) {
  const { cameraId, deviceKey } = req.params;
  const { durationSeconds, intensity } = req.body;

  // Validation: Thiếu các trường bắt buộc
  if (durationSeconds === undefined) {
    return res.status(400).json({ error: 'missed_duration_seconds', message: 'Thiếu thông tin bắt buộc: durationSeconds.' });
  }
  if (intensity === undefined) {
    return res.status(400).json({ error: 'missed_intensity', message: 'Thiếu thông tin bắt buộc: intensity.' });
  }

  // Validation: Sai định dạng khoảng giá trị
  if (intensity < 0 || intensity > 100) {
    return res.status(400).json({ error: 'invalid_intensity', message: 'Cường độ thiết bị (intensity) phải nằm trong khoảng từ 0 đến 100.' });
  }
  if (durationSeconds < 0) {
    return res.status(400).json({ error: 'invalid_duration_seconds', message: 'Thời gian chạy thử (durationSeconds) phải lớn hơn hoặc bằng 0.' });
  }

  // Validation: Thiết bị ngoại vi không được hỗ trợ
  const validKeys = ['led', 'speaker', 'fence'];
  if (!validKeys.includes(deviceKey)) {
    return res.status(404).json({ error: 'not_found_device', message: 'Thiết bị ngoại vi yêu cầu không tồn tại tại trạm camera này.' });
  }

  try {
    const camera = await prisma.camera.findUnique({
      where: { id: cameraId }
    });

    if (!camera) {
      return res.status(404).json({ error: 'not_found_camera', message: 'Không tìm thấy trạm camera yêu cầu.' });
    }

    // Tìm sự kiện gần nhất của camera này để liên kết log.
    // Nếu chưa từng có sự kiện nào, ta tự động tạo một sự kiện hệ thống giả định để tránh lỗi khóa ngoại.
    let latestEvent = await prisma.event.findFirst({
      where: { cameraId },
      orderBy: { detectedAt: 'desc' }
    });

    if (!latestEvent) {
      latestEvent = await prisma.event.create({
        data: {
          id: `sys-test-${Date.now()}`,
          cameraId,
          detectedAt: new Date(),
          snapshotUrl: 'https://cdn.example.com/default-test.jpg'
        }
      });
    }

    // Ghi nhật ký kích hoạt thiết bị
    const now = new Date();
    const autoOffAt = new Date(now.getTime() + durationSeconds * 1000);

    const deviceLog = await prisma.deviceLog.create({
      data: {
        eventId: latestEvent.id,
        deviceKey,
        action: 'ON',
        actionAt: now,
        autoOffAt
      }
    });

    // Gửi lệnh điều khiển qua WebSocket và chờ phản hồi COMMAND_ACK từ AI Server
    const user = req.user!;
    try {
      await sendDeviceCommand(
        user.id,
        deviceLog.id,
        cameraId,
        deviceKey,
        'TEST',
        { durationSeconds, intensity }
      );
      
      return res.status(200).json({
        commandId: deviceLog.id,
        status: 'SUCCESS',
        message: `Đã đẩy lệnh test thiết bị ${deviceKey} xuống camera thành công.`
      });
    } catch (error: unknown) {
      const wsError = error as Error;
      console.error('[WS] Lỗi gửi lệnh test:', wsError.message);
      
      const isOffline = wsError.message?.includes('ngoại tuyến') || wsError.message?.includes('Offline');
      const isTimeout = wsError.message?.includes('Quá thời gian') || wsError.message?.includes('Timeout');
      
      if (isOffline) {
        return res.status(400).json({
          error: 'camera_offline',
          message: wsError.message
        });
      }
      
      if (isTimeout) {
        return res.status(504).json({
          error: 'gateway_timeout',
          message: wsError.message
        });
      }
      
      return res.status(400).json({
        error: 'command_failed',
        message: wsError.message
      });
    }
  } catch (error) {
    console.error('Lỗi khi gửi lệnh test thiết bị:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}
