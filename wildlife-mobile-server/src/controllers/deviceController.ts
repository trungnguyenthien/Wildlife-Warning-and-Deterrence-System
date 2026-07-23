import { Response } from 'express';
import { AuthenticatedRequest } from '../middlewares/auth';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

// 1. POST /devices/push-token - Đăng ký FCM token của thiết bị
export async function registerPushToken(req: AuthenticatedRequest, res: Response) {
  const { fcmToken, deviceModel, osVersion } = req.body;
  const userId = req.user?.id;

  if (!userId) {
    return res.status(401).json({ error: 'unauthorized', message: 'Truy cập bị từ chối: Người dùng chưa đăng nhập.' });
  }

  if (!fcmToken) {
    return res.status(400).json({ error: 'missed_fcm_token', message: 'Thiếu thông tin bắt buộc: fcmToken.' });
  }

  try {
    const modelStr = deviceModel || 'Unknown';
    const osStr = osVersion || 'Unknown';

    await prisma.deviceToken.upsert({
      where: { fcmToken },
      update: {
        userId,
        deviceModel: modelStr,
        osVersion: osStr,
      },
      create: {
        userId,
        fcmToken,
        deviceModel: modelStr,
        osVersion: osStr,
      },
    });

    return res.status(201).json({
      registered: true,
      registeredAt: new Date().toISOString(),
    });
  } catch (error) {
    console.error('Lỗi khi đăng ký Device Push Token:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ trong quá trình đăng ký thiết bị.' });
  }
}

// 2. DELETE /devices/push-token - Hủy đăng ký FCM token của thiết bị
export async function deletePushToken(req: AuthenticatedRequest, res: Response) {
  const userId = req.user?.id;
  const fcmToken = req.body?.fcmToken || req.query?.fcmToken;

  if (!userId) {
    return res.status(401).json({ error: 'unauthorized', message: 'Truy cập bị từ chối: Người dùng chưa đăng nhập.' });
  }

  try {
    if (fcmToken) {
      await prisma.deviceToken.deleteMany({
        where: {
          fcmToken: String(fcmToken),
        },
      });
    } else {
      // Hủy toàn bộ token thiết bị của user này
      await prisma.deviceToken.deleteMany({
        where: { userId },
      });
    }

    return res.status(204).send();
  } catch (error) {
    console.error('Lỗi khi hủy đăng ký Device Push Token:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ trong quá trình hủy đăng ký thiết bị.' });
  }
}
