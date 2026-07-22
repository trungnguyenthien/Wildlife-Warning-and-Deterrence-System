import { Response } from 'express';
import { PrismaClient, SmsRelation } from '@prisma/client';
import { AuthenticatedRequest } from '../middlewares/auth';

const prisma = new PrismaClient();

// 1. GET /users/me/sms-recipients - Danh sách người nhận SMS phụ
export async function listSmsRecipients(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'Truy cập bị từ chối.' });
  }

  try {
    const recipients = await prisma.smsRecipient.findMany({
      where: { userId: req.user.id }
    });

    return res.status(200).json(recipients);
  } catch (error) {
    console.error('Lỗi khi tải danh sách người nhận SMS:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}

// 2. POST /users/me/sms-recipients - Thêm người nhận SMS phụ mới
export async function addSmsRecipient(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'Truy cập bị từ chối.' });
  }

  const { fullName, phoneNumber, relation } = req.body;

  // Validation: Thiếu trường required
  if (!fullName) {
    return res.status(400).json({ error: 'Thiếu thông tin bắt buộc: fullName.' });
  }
  if (!phoneNumber) {
    return res.status(400).json({ error: 'Thiếu thông tin bắt buộc: phoneNumber.' });
  }
  if (!relation) {
    return res.status(400).json({ error: 'Thiếu thông tin bắt buộc: relation.' });
  }

  // Validation: Sai định dạng số điện thoại E.164
  const e164Regex = /^\+[1-9]\d{1,14}$/;
  if (!e164Regex.test(phoneNumber)) {
    return res.status(400).json({ error: 'Định dạng số điện thoại không hợp lệ. Phải đúng chuẩn E.164 (ví dụ: +84908888888).' });
  }

  // Validation: Mối quan hệ sai enum (self, family, neighbor, other)
  const validRelations = Object.values(SmsRelation);
  if (!validRelations.includes(relation as SmsRelation)) {
    return res.status(400).json({ error: `Mối quan hệ không hợp lệ. Phải thuộc: ${validRelations.join(', ')}.` });
  }

  try {
    // Ràng buộc nghiệp vụ: Tối đa 3 SĐT phụ cho mỗi người dùng
    const count = await prisma.smsRecipient.count({
      where: { userId: req.user.id }
    });

    if (count >= 3) {
      return res.status(400).json({ error: 'Đã đạt giới hạn đăng ký tối đa 3 số điện thoại nhận cảnh báo.' });
    }

    // Đề phòng số điện thoại trùng lặp
    const existing = await prisma.smsRecipient.findFirst({
      where: { phoneNumber }
    });
    if (existing) {
      return res.status(409).json({ error: 'Số điện thoại này đã được đăng ký nhận SMS cảnh báo.' });
    }

    const newRecipient = await prisma.smsRecipient.create({
      data: {
        userId: req.user.id,
        fullName,
        phoneNumber,
        relation: relation as SmsRelation
      }
    });

    return res.status(201).json(newRecipient);
  } catch (error) {
    console.error('Lỗi khi thêm người nhận SMS:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}

// 3. DELETE /users/me/sms-recipients/{recipientId} - Xóa số điện thoại
export async function deleteSmsRecipient(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'Truy cập bị từ chối.' });
  }

  const { recipientId } = req.params;

  try {
    const recipient = await prisma.smsRecipient.findFirst({
      where: {
        id: recipientId,
        userId: req.user.id
      }
    });

    if (!recipient) {
      return res.status(404).json({ error: 'Không tìm thấy thông tin người nhận SMS hoặc bạn không có quyền xóa.' });
    }

    await prisma.smsRecipient.delete({
      where: { id: recipientId }
    });

    return res.status(204).send();
  } catch (error) {
    console.error('Lỗi khi xóa người nhận SMS:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}
