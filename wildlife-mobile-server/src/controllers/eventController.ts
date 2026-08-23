import { Request, Response } from 'express';
import { PrismaClient, AlertType, DangerLevel, Role, Prisma } from '@prisma/client';
import { AuthenticatedRequest } from '../middlewares/auth';
import { notifySSE } from './cameraController';
import { sendPushToAllDevices } from '../config/firebase';
import { getRecommendedPresetForSpecies } from '../config/presets';
import fs from 'fs';
import { uploadImage } from '../config/cloudinary';

const prisma = new PrismaClient();

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
        },
        event: {
          include: {
            eventDetections: {
              include: {
                species: true
              }
            }
          }
        }
      }
    });

    const result = alerts.map((alt) => {
      // Tìm detection tương ứng với tên loài xuất hiện trong tiêu đề Alert
      const mainDetection = alt.event?.eventDetections?.find((det) => 
        alt.title.includes(det.species.displayName)
      ) || alt.event?.eventDetections?.[0];

      return {
        id: alt.id,
        type: alt.type,
        title: alt.title,
        dangerLevel: alt.dangerLevel,
        cameraId: alt.cameraId,
        cameraName: alt.camera.name,
        eventId: alt.eventId,
        speciesId: mainDetection ? mainDetection.speciesId : null,
        confidence: mainDetection ? mainDetection.confidence : null,
        createdAt: alt.createdAt.toISOString(),
        isRead: alt.alertReads.length > 0
      };
    });

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
  
  console.log(`[Detection-Webhook] Nhận request: cameraId=${cameraId}, hasFile=${req.file ? 'YES' : 'NO'}, body=${JSON.stringify(req.body)}`);

  // 1. Parse fields (handles JSON or Multipart Form Data)
  interface DetectionItem {
    speciesId: string;
    confidence: number;
  }
  const rawDetections = req.body.detections;
  let parsedDetections: DetectionItem[] = [];
  let imageUrl = req.body.imageUrl;
  let detectedAt = req.body.detectedAt;
  const userId = req.body.userId;

  // Handle file validation and upload if file exists
  if (req.file) {
    const file = req.file;
    const allowedMimeTypes = ['image/jpeg', 'image/png', 'image/jpg'];
    if (!allowedMimeTypes.includes(file.mimetype)) {
      if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
      return res.status(400).json({
        error: 'invalid_image_format',
        message: 'Định dạng hình ảnh không hợp lệ. Chỉ chấp nhận JPG, JPEG hoặc PNG.'
      });
    }

    if (file.size > 5 * 1024 * 1024) {
      if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
      return res.status(400).json({
        error: 'image_too_large',
        message: 'Dung lượng hình ảnh quá lớn. Giới hạn tối đa là 5MB.'
      });
    }

    // Spec: userId is mandatory for multipart
    if (!userId) {
      if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
      return res.status(400).json({
        error: 'missed_user_id',
        message: 'Thiếu thông tin bắt buộc: userId.'
      });
    }

    // Spec: Validate user exists
    const user = await prisma.user.findUnique({
      where: { id: userId }
    });
    if (!user) {
      if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
      return res.status(404).json({
        error: 'not_found_user',
        message: 'Người dùng sở hữu trạm camera không tồn tại.'
      });
    }

    // Upload to Cloudinary
    try {
      imageUrl = await uploadImage(file.path, 'detections');
    } catch (err) {
      console.error('Lỗi tải ảnh lên Cloudinary:', err);
      if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
      return res.status(500).json({
        error: 'upload_failed',
        message: 'Không thể tải ảnh snapshot lên Cloudinary.'
      });
    } finally {
      if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
    }

    // Default detectedAt if not provided in multipart mode
    if (!detectedAt) {
      detectedAt = new Date().toISOString();
    }
  }

  // Parse detections if it is a JSON string (Multipart mode)
  if (typeof rawDetections === 'string') {
    try {
      parsedDetections = JSON.parse(rawDetections);
    } catch (e) {
      return res.status(400).json({
        error: 'invalid_detections_json',
        message: 'Trường detections không đúng định dạng JSON.'
      });
    }
  } else {
    parsedDetections = rawDetections;
  }

  // Nếu không truyền detectedAt, tự động sinh thời gian hiện tại
  if (!detectedAt) {
    detectedAt = new Date().toISOString();
  }

  // 3. Validation: Thiếu trường bắt buộc
  if (!parsedDetections) {
    return res.status(400).json({ error: 'missed_detections', message: 'Thiếu thông tin bắt buộc: detections.' });
  }
  if (!imageUrl) {
    return res.status(400).json({ error: 'missed_image_url', message: 'Thiếu thông tin bắt buộc: imageUrl.' });
  }

  // Validation: Mảng detections trống rỗng
  if (!Array.isArray(parsedDetections) || parsedDetections.length === 0) {
    return res.status(400).json({ error: 'invalid_detections', message: 'Dữ liệu nhận dạng (detections) phải là mảng và không được để trống.' });
  }

  // Validation: Sai khoảng độ tin cậy confidence
  for (const det of parsedDetections) {
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

    // Ràng buộc nghiệp vụ: Gom nhóm sự kiện nếu khoảng cách dưới 30 giây
    // Ngoại lệ: bỏ qua giới hạn nếu request đến từ công cụ simulate (header X-Bypass-Cooldown)
    const bypassCooldown = false;
    if (!bypassCooldown && latestEvent && Math.abs(detectionTime.getTime() - latestEvent.detectedAt.getTime()) < 30 * 1000) {
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
      // Khoảng cách trên 30 giây: Tạo mới hoàn toàn phiên sự kiện
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
      parsedDetections.map(async (det) => {
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

    // Tạo bản tin cảnh báo gửi lên feed khẩn cấp và gửi Push Notification (bất kể là sự kiện mới hay gộp)
    let alertType = AlertType.INTRUDER as AlertType;
    if (mainSpecies.isHuman) {
      alertType = AlertType.HUMAN_BORDER as AlertType;
    } else {
      alertType = AlertType.ANIMAL_RARE as AlertType;
    }

    console.log(`[Detection-Workflow] Đang tạo Alert trong DB: type=${alertType}, title=Phát hiện ${mainSpecies.displayName}, dangerLevel=${maxDangerLevel}`);
    const newAlert = await prisma.alert.create({
      data: {
        id: `alt-${Date.now()}`,
        type: alertType,
        title: `Cảnh báo: Phát hiện ${mainSpecies.displayName} tại khu vực ${camera.name}`,
        dangerLevel: maxDangerLevel,
        cameraId,
        eventId
      }
    });
    console.log(`[Detection-Workflow] Đã tạo Alert thành công! Alert ID: ${newAlert.id}`);

    // Bắn Push Notification: Luôn phát thông báo bất kể là con người hay độ nguy hại thấp
    const isPushConditionMet = true;
    console.log(`[Detection-Workflow] Kiểm tra điều kiện gửi Push Notification: Luôn gửi thông báo -> Điều kiện thỏa mãn: ${isPushConditionMet}`);
    
    if (isPushConditionMet) {
      const isEscalated = maxDangerLevel === DangerLevel.CRITICAL;
      console.log(`[Detection-Workflow] Tiến hành gọi sendPushToAllDevices: isEscalated=${isEscalated}`);
      await sendPushToAllDevices(
        isEscalated ? 'Cảnh báo nguy khẩn: Phát hiện động vật nguy cấp' : 'Cảnh báo: Phát hiện động vật hoang dã nguy hiểm',
        `Phát hiện ${mainSpecies.displayName} tại khu vực ${camera.name} (Độ nguy hiểm: ${maxDangerLevel})`,
        { 
          eventId, 
          cameraId, 
          speciesId: mainSpecies.id,
          type: isEscalated ? 'animal.escalated' : 'animal.detected',
          dangerLevel: maxDangerLevel
        }
      );
    }

    // Tra cứu userId sở hữu camera từ Snapshot gần nhất để định dạng cấu hình phòng vệ theo chủ sở hữu
    const latestSnapshot = await prisma.snapshot.findFirst({
      where: { cameraId },
      orderBy: { uploadedAt: 'desc' }
    });
    const ownerId = latestSnapshot?.userId || 'u_rg';

    // Tra cứu hành động phòng vệ (Custom Config hoặc Preset mặc định)
    const customConfig = await prisma.responseConfig.findUnique({
      where: {
        userId_speciesId: {
          userId: ownerId,
          speciesId: mainSpecies.id
        }
      }
    });

    const defaultPreset = getRecommendedPresetForSpecies(mainSpecies.id, maxDangerLevel);
    let actionResponse = {
      ledFlash: defaultPreset.ledFlash,
      ledFlashRate: defaultPreset.ledFlashRate || (defaultPreset.ledFlash ? 'FAST' : null),
      speakerWarn: defaultPreset.speakerWarn,
      silentAlert: defaultPreset.silentAlert
    };
    if (customConfig) {
      actionResponse = {
        ledFlash: customConfig.ledFlashRate ? true : false,
        ledFlashRate: customConfig.ledFlashRate,
        speakerWarn: customConfig.speakerSampleId ? true : false,
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

// 5. GET /alerts/:alertId - Lấy chi tiết cảnh báo phát hiện
export async function getAlertDetail(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'unauthorized_session', message: 'Truy cập bị từ chối.' });
  }

  const { alertId } = req.params;

  try {
    const alert = await prisma.alert.findUnique({
      where: { id: alertId },
      include: {
        camera: true,
        event: {
          include: {
            eventDetections: {
              include: {
                species: true
              }
            }
          }
        }
      }
    });

    if (!alert) {
      return res.status(404).json({ error: 'not_found_alert', message: 'Không tìm thấy cảnh báo.' });
    }

    // Xác định species chính dựa vào dangerLevel hoặc là phần tử đầu tiên
    const detections = alert.event.eventDetections;
    let mainSpecies = detections[0]?.species || null;
    let maxConfidence = detections[0]?.confidence || 0;

    // Tìm loài có confidence cao nhất
    detections.forEach((d) => {
      if (d.confidence > maxConfidence) {
        maxConfidence = d.confidence;
        mainSpecies = d.species;
      }
    });

    const isIntrusion = alert.type === AlertType.HUMAN_BORDER || alert.type === AlertType.INTRUDER || (mainSpecies?.isHuman ?? false);

    // Tên hiển thị lấy từ DB (displayName là nguồn duy nhất, không hardcode)
    const speciesNameEn = mainSpecies ? mainSpecies.displayName : null;

    // Định dạng ngày recordedAt: "HH:mm:ss · dd/MM/yyyy"
    const date = alert.createdAt;
    const pad = (n: number) => n.toString().padStart(2, '0');
    const recordedAt = `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} · ${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;

    const gpsCoordinate = alert.camera ? `${alert.camera.latitude}, ${alert.camera.longitude}` : null;

    const result = {
      alertId: alert.id,
      title: alert.title,
      alertType: isIntrusion ? 'intrusion' : 'animal',
      imageUrl: alert.event.snapshotUrl,
      speciesName: mainSpecies ? mainSpecies.displayName : null,
      speciesNameEn: speciesNameEn,
      cameraCode: alert.camera.id,
      cameraName: alert.camera.name,
      dangerLevel: alert.dangerLevel,
      confidencePercent: Math.round(maxConfidence * 100),
      estimatedCount: detections.length,
      recordedAt: recordedAt,
      gpsCoordinate: gpsCoordinate
    };

    return res.status(200).json(result);
  } catch (error) {
    console.error('Lỗi khi tải chi tiết cảnh báo:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 5. GET /notifications/inbox - Lấy danh sách thông báo đẩy nhận được trong app (chỉ dùng để test)
export async function listNotificationsInbox(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'unauthorized', message: 'Truy cập bị từ chối.' });
  }

  const { page, size, unreadOnly } = req.query;
  const pageNum = parseInt(page as string) || 0; // page 0-indexed theo spec
  const sizeNum = parseInt(size as string) || 20;
  const skip = pageNum * sizeNum;
  const unreadOnlyBool = unreadOnly === 'true';

  if (page !== undefined && (isNaN(pageNum) || pageNum < 0)) {
    return res.status(400).json({ error: 'invalid_page', message: 'Tham số page không hợp lệ.' });
  }
  if (size !== undefined && (isNaN(sizeNum) || sizeNum <= 0)) {
    return res.status(400).json({ error: 'invalid_size', message: 'Tham số size không hợp lệ.' });
  }

  try {
    const alertFilter: Prisma.AlertWhereInput = {};
    
    // Phân quyền vai trò: CITIZEN không được xem HUMAN_BORDER
    if (req.user.role === Role.CITIZEN) {
      alertFilter.type = {
        not: AlertType.HUMAN_BORDER
      };
    }

    if (unreadOnlyBool) {
      // Chỉ lấy tin chưa đọc (chưa có trong AlertRead của user này)
      alertFilter.alertReads = {
        none: {
          userId: req.user.id
        }
      };
    }

    const total = await prisma.alert.count({ where: alertFilter });

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

    const items = alerts.map((alt) => {
      // Map type của alert sang type của notification
      let notifType = 'system.alert';
      if (alt.type === AlertType.ANIMAL_RARE) {
        notifType = alt.dangerLevel === DangerLevel.CRITICAL ? 'animal.escalated' : 'animal.detected';
      } else if (alt.type === AlertType.HUMAN_BORDER) {
        notifType = 'animal.escalated';
      } else if (alt.type === AlertType.INTRUDER) {
        notifType = 'animal.detected';
      }

      return {
        id: alt.id,
        type: notifType,
        title: alt.title,
        body: `Mức độ nguy hiểm: ${alt.dangerLevel} tại ${alt.camera.name}`,
        cameraId: alt.cameraId,
        eventId: alt.eventId,
        isRead: alt.alertReads.length > 0,
        createdAt: alt.createdAt.toISOString()
      };
    });

    return res.status(200).json({
      items,
      pagination: {
        page: pageNum,
        size: sizeNum,
        total
      }
    });
  } catch (error) {
    console.error('Lỗi khi tải inbox thông báo:', error);
    return res.status(500).json({ error: 'internal_server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}
