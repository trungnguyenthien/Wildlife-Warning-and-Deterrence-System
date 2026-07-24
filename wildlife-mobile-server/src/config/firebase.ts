import { initializeApp, getApps, cert } from 'firebase-admin';
import { getMessaging, MulticastMessage } from 'firebase-admin/messaging';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

export function initFirebase(): void {
  if (getApps().length > 0) {
    return;
  }

  const base64Key = process.env.PUSH_SERVICE_ACCOUNT_KEY_JSON;
  if (!base64Key) {
    console.warn('[Firebase] Biến môi trường PUSH_SERVICE_ACCOUNT_KEY_JSON chưa được cấu hình.');
    return;
  }

  try {
    const decodedJson = Buffer.from(base64Key, 'base64').toString('utf8');
    const serviceAccount = JSON.parse(decodedJson);

    initializeApp({
      credential: cert(serviceAccount)
    });

    console.log('[Firebase] Khởi tạo Firebase Admin SDK thành công.');
  } catch (error) {
    console.error('[Firebase] Lỗi khởi tạo Firebase Admin SDK:', error);
  }
}

export async function sendPushToAllDevices(
  title: string,
  body: string,
  payload?: Record<string, string>
): Promise<void> {
  try {
    initFirebase();

    if (getApps().length === 0) {
      console.warn('[Firebase] Không thể gửi Push do Firebase chưa được khởi tạo.');
      return;
    }

    const devices = await prisma.deviceToken.findMany({
      select: { fcmToken: true }
    });

    const tokens = devices.map((d) => d.fcmToken).filter(Boolean);

    if (tokens.length === 0) {
      console.log('[Firebase] Không tìm thấy token thiết bị nào trong cơ sở dữ liệu để gửi.');
      return;
    }

    const message: MulticastMessage = {
      notification: { title, body },
      data: payload || {},
      tokens: tokens
    };

    const response = await getMessaging().sendEachForMulticast(message);
    console.log(`[Firebase] Gửi Push hoàn tất. Thành công: ${response.successCount}, Thất bại: ${response.failureCount}`);
  } catch (error) {
    console.error('[Firebase] Lỗi trong quá trình gửi Push Notification:', error);
  }
}
