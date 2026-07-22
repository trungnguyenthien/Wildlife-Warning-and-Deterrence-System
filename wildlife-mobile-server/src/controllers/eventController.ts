import { Request, Response } from 'express';
import { PrismaClient, AlertType, DangerLevel, Role } from '@prisma/client';
import { AuthenticatedRequest } from '../middlewares/auth';
import { notifySSE } from './cameraController';

const prisma = new PrismaClient();

// Danger Level Presets mặc định để AI Webhook trả về Action ứng phó nhanh
const DANGER_LEVEL_ACTIONS: Record<DangerLevel, Record<string, unknown>> = {
  [DangerLevel.LOW]: { ledFlash: false, speakerWarn: false, electricFence: false, silentAlert: true },
  [DangerLevel.MEDIUM]: { ledFlash: true, speakerWarn: true, electricFence: false, silentAlert: false },
  [DangerLevel.HIGH]: { ledFlash: true, speakerWarn: true, electricFence: false, silentAlert: false },
  [DangerLevel.CRITICAL]: { ledFlash: true, speakerWarn: true, electricFence: true, silentAlert: false }
};

// 1. GET /events - Lấy nhật ký sự kiện lịch sử của camera
export async function listEvents(req: AuthenticatedRequest, res: Response) {
  const { cameraId, page, size } = req.query;

  const pageNum = parseInt(page as string) || 1;
  const sizeNum = parseInt(size as string) || 10;
  const skip = (pageNum - 1) * sizeNum;

  try {
    const events = await prisma.event.findMany({
      where: cameraId ? { cameraId: cameraId as string } : {},
      orderBy: { detectedAt: 'desc' },
      skip,
      take: sizeNum,
      include: {
        eventDetections: {
          include: { species: true }
        }
      }
    });

    const result = events.map((evt) => ({
      id: evt.id,
      cameraId: evt.cameraId,
      detectedAt: evt.detectedAt.toISOString(),
      snapshotUrl: evt.snapshotUrl,
      detections: evt.eventDetections.map((d) => ({
        speciesId: d.speciesId,
        displayName: d.species.displayName,
        confidence: d.confidence
      }))
    }));

    return res.status(200).json(result);
  } catch (error) {
    console.error('Lỗi khi tải lịch sử sự kiện:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}

// 2. GET /alerts/feed - Luồng tin tức cảnh báo khẩn cấp phân vai trò
export async function listAlertFeed(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'Truy cập bị từ chối.' });
  }

  const { page, size } = req.query;
  const pageNum = parseInt(page as string) || 1;
  const sizeNum = parseInt(size as string) || 10;
  const skip = (pageNum - 1) * sizeNum;

  if (page !== undefined && (isNaN(pageNum) || pageNum <= 0)) {
    return res.status(400).json({ error: 'invalid_page', message: 'Tham số page không hợp lệ.' });
  }
  if (size !== undefined && (isNaN(sizeNum) || sizeNum <= 0)) {
    return res.status(400).json({ error: 'invalid_size', message: 'Tham số size không hợp lệ.' });
  }

  try {
    // Phân quyền vai trò: CITIZEN không được xem HUMAN_BORDER
    const alertFilter: Record<string, unknown> = {};
    if (req.user.role === Role.CITIZEN) {
      alertFilter.type = {
        not: AlertType.HUMAN_BORDER
      };
    }

    const alerts = await prisma.alert.findMany({
      where: alertFilter,
      orderBy: { createdAt: 'desc' },
      skip,
      take: sizeNum,
      include: {
        camera: true,
        alertReads: {
          where: { userId: req.user.id }
        }
      }
    });

    const result = alerts.map((alt) => ({
      id: alt.id,
      type: alt.type,
      title: alt.title,
      dangerLevel: alt.dangerLevel,
      cameraId: alt.cameraId,
      cameraName: alt.camera.name,
      eventId: alt.eventId,
      createdAt: alt.createdAt.toISOString(),
      isRead: alt.alertReads.length > 0
    }));

    return res.status(200).json(result);
  } catch (error) {
    console.error('Lỗi khi tải luồng cảnh báo:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 3. POST /alerts/feed/{alertId}/read - Đánh dấu đã đọc cảnh báo
export async function readAlert(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'unauthorized_session', message: 'Truy cập bị từ chối.' });
  }

  const { alertId } = req.params;

  try {
    const alert = await prisma.alert.findUnique({
      where: { id: alertId }
    });

    if (!alert) {
      return res.status(404).json({ error: 'not_found_alert', message: 'Không tìm thấy cảnh báo.' });
    }

    // Ghi nhận trạng thái đọc tin
    const existingRead = await prisma.alertRead.findFirst({
      where: {
        userId: req.user.id,
        alertId
      }
    });

    if (existingRead) {
      await prisma.alertRead.update({
        where: { id: existingRead.id },
        data: { readAt: new Date() }
      });
    } else {
      await prisma.alertRead.create({
        data: {
          userId: req.user.id,
          alertId,
          readAt: new Date()
        }
      });
    }

    return res.status(200).json({ message: 'Đã đánh dấu đọc cảnh báo.' });
  } catch (error) {
    console.error('Lỗi khi đọc tin cảnh báo:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}

// 4. POST /cameras/{cameraId}/detections - Webhook AI Server nhận dạng hiện trường
export async function processDetection(req: Request, res: Response) {
  const { cameraId } = req.params;
  const { detections, imageUrl, detectedAt } = req.body;

  // Validation: Thiếu trường bắt buộc
  if (!detections) {
    return res.status(400).json({ error: 'missed_detections', message: 'Thiếu thông tin bắt buộc: detections.' });
  }
  if (!imageUrl) {
    return res.status(400).json({ error: 'missed_image_url', message: 'Thiếu thông tin bắt buộc: imageUrl.' });
  }
  if (!detectedAt) {
    return res.status(400).json({ error: 'missed_detected_at', message: 'Thiếu thông tin bắt buộc: detectedAt.' });
  }

  // Validation: Mảng detections trống rỗng
  if (!Array.isArray(detections) || detections.length === 0) {
    return res.status(400).json({ error: 'invalid_detections', message: 'Dữ liệu nhận dạng (detections) phải là mảng và không được để trống.' });
  }

  // Validation: Sai khoảng độ tin cậy confidence
  for (const det of detections) {
    if (det.confidence === undefined || det.confidence < 0 || det.confidence > 1) {
      return res.status(400).json({ error: 'invalid_confidence', message: 'Độ tin cậy nhận diện (confidence) phải nằm trong khoảng từ 0 đến 1.' });
    }
  }

  // Validation: Sai định dạng imageUrl
  const urlRegex = /^(https?|ftp):\/\/[^\s/$.?#].[^\s]*$/i;
  if (!urlRegex.test(imageUrl)) {
    return res.status(400).json({ error: 'invalid_image_url', message: 'Đường dẫn hình ảnh (imageUrl) không đúng định dạng URL.' });
  }

  // Validation: Sai định dạng ISO 8601 của detectedAt
  const dateVal = Date.parse(detectedAt);
  if (isNaN(dateVal)) {
    return res.status(400).json({ error: 'invalid_detected_at', message: 'Định dạng thời gian detectedAt không hợp lệ. Vui lòng dùng định dạng chuẩn ISO 8601.' });
  }

  try {
    const camera = await prisma.camera.findUnique({
      where: { id: cameraId }
    });

    if (!camera) {
      return res.status(404).json({ error: 'not_found_camera', message: 'Không tìm thấy trạm camera.' });
    }

    const detectionTime = new Date(detectedAt);

    // Tìm sự kiện gần nhất của camera này
    const latestEvent = await prisma.event.findFirst({
      where: { cameraId },
      orderBy: { detectedAt: 'desc' }
    });

    let eventId = '';
    let isNewEvent = false;

    // Ràng buộc nghiệp vụ: Gom nhóm sự kiện nếu khoảng cách dưới 5 phút
    if (latestEvent && Math.abs(detectionTime.getTime() - latestEvent.detectedAt.getTime()) < 5 * 60 * 1000) {
      eventId = latestEvent.id;
      // Cập nhật lại thời gian và ảnh snapshot mới nhất cho sự kiện đang diễn ra
      await prisma.event.update({
        where: { id: eventId },
        data: {
          detectedAt: detectionTime,
          snapshotUrl: imageUrl
        }
      });
    } else {
      // Khoảng cách trên 5 phút: Tạo mới hoàn toàn phiên sự kiện
      eventId = `evt-${Date.now()}`;
      isNewEvent = true;
      await prisma.event.create({
        data: {
          id: eventId,
          cameraId,
          detectedAt: detectionTime,
          snapshotUrl: imageUrl
        }
      });
    }

    // Ghi nhận chi tiết nhận dạng AI vào bảng event_detections
    const savedDetections = await Promise.all(
      detections.map(async (det) => {
        // Kiểm tra xem loài có tồn tại không
        const species = await prisma.species.findUnique({
          where: { id: det.speciesId }
        });

        if (!species) {
          throw new Error(`Loài '${det.speciesId}' không tồn tại trong danh mục.`);
        }

        return prisma.eventDetection.create({
          data: {
            eventId,
            speciesId: det.speciesId,
            confidence: det.confidence,
            detectedAt: detectionTime
          },
          include: { species: true }
        });
      })
    );

    // Tính toán độ nguy hiểm cao nhất trong danh sách loài phát hiện để kích hoạt Action
    let maxDangerLevel = DangerLevel.LOW as DangerLevel;
    let mainSpecies = savedDetections[0].species;

    savedDetections.forEach((d) => {
      const currentLevel = d.species.dangerLevel;
      const order: Record<DangerLevel, number> = { LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4 };
      if (order[currentLevel] > order[maxDangerLevel]) {
        maxDangerLevel = currentLevel;
        mainSpecies = d.species;
      }
    });

    // Tạo bản tin cảnh báo gửi lên feed khẩn cấp nếu có sự kiện mới
    if (isNewEvent) {
      let alertType = AlertType.INTRUDER as AlertType;
      if (mainSpecies.isHuman) {
        alertType = AlertType.HUMAN_BORDER as AlertType;
      } else if (maxDangerLevel === DangerLevel.CRITICAL) {
        alertType = AlertType.ANIMAL_RARE as AlertType;
      }

      await prisma.alert.create({
        data: {
          id: `alt-${Date.now()}`,
          type: alertType,
          title: `Cảnh báo: Phát hiện ${mainSpecies.displayName} tại khu vực ${camera.name}`,
          dangerLevel: maxDangerLevel,
          cameraId,
          eventId
        }
      });
    }

    // Tra cứu hành động phòng vệ (Custom Config hoặc Preset mặc định)
    const customConfig = await prisma.responseConfig.findUnique({
      where: {
        cameraId_speciesId: {
          cameraId,
          speciesId: mainSpecies.id
        }
      }
    });

    let actionResponse = DANGER_LEVEL_ACTIONS[maxDangerLevel];
    if (customConfig) {
      actionResponse = {
        ledFlash: customConfig.ledFlashRate ? true : false,
        speakerWarn: customConfig.speakerSampleId ? true : false,
        electricFence: customConfig.fenceLevel ? true : false,
        silentAlert: customConfig.silentAlert
      };
    }

    // Bắn tin SSE để Mobile Client tự động load dữ liệu thời gian thực
    notifySSE({
      event: 'DETECTION_ALERT',
      data: {
        eventId,
        cameraId,
        cameraName: camera.name,
        detections: savedDetections.map((d) => ({
          speciesId: d.speciesId,
          displayName: d.species.displayName,
          confidence: d.confidence
        })),
        imageUrl
      }
    });

    // Trả về kết quả
    return res.status(isNewEvent ? 201 : 200).json({
      eventId,
      detections: savedDetections.map((d) => ({
        speciesId: d.speciesId,
        confidence: d.confidence
      })),
      responseAction: actionResponse
    });
  } catch (error) {
    console.error('Lỗi Webhook xử lý nhận dạng AI:', error);
    const err = error as Error;
    if (err.message && err.message.includes('không tồn tại')) {
      return res.status(404).json({ error: err.message });
    }
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}
