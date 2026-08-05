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
  console.log(`[FCM-Sender] Yêu cầu gửi Push: Title="${title}", Body="${body}", Payload=${JSON.stringify(payload)}`);
  try {
    initFirebase();

    if (getApps().length === 0) {
      console.warn('[FCM-Sender] Không thể gửi Push do Firebase Admin SDK chưa được khởi tạo thành công.');
      return;
    }

    console.log('[FCM-Sender] Đang truy vấn danh sách token thiết bị (DeviceToken) từ cơ sở dữ liệu...');
    const devices = await prisma.deviceToken.findMany({
      select: { fcmToken: true, userId: true, deviceModel: true }
    });

    const tokens = devices.map((d) => d.fcmToken).filter(Boolean);
    console.log(`[FCM-Sender] Tìm thấy ${devices.length} bản ghi token trong DB. Tổng số token hợp lệ lọc ra: ${tokens.length}`);
    devices.forEach((d, idx) => {
      console.log(`- Token [${idx + 1}]: UserID=${d.userId}, Model=${d.deviceModel}, Token=${d.fcmToken ? (d.fcmToken.substring(0, 15) + '...') : 'NULL'}`);
    });

    if (tokens.length === 0) {
      console.log('[FCM-Sender] Không tìm thấy token thiết bị nào trong cơ sở dữ liệu để gửi.');
      return;
    }

    const message: MulticastMessage = {
      notification: { title, body },
      data: payload || {},
      tokens: tokens
    };

    console.log(`[FCM-Sender] Đang gọi Firebase Cloud Messaging (sendEachForMulticast) gửi tới ${tokens.length} thiết bị...`);
    const response = await getMessaging().sendEachForMulticast(message);
    console.log(`[FCM-Sender] Kết quả gửi từ FCM: Thành công: ${response.successCount}, Thất bại: ${response.failureCount}`);
    
    response.responses.forEach((res, idx) => {
      if (res.success) {
        console.log(`- Thiết bị [${idx + 1}] gửi thành công. MessageId: ${res.messageId}`);
      } else {
        console.error(`- Thiết bị [${idx + 1}] gửi thất bại. Lỗi:`, res.error);
      }
    });

  } catch (error) {
    console.error('[FCM-Sender] Lỗi nghiêm trọng trong quá trình gửi Push Notification:', error);
  }
}
