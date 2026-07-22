import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import fs from 'fs';
import { uploadImage } from '../config/cloudinary';

const prisma = new PrismaClient();

// POST /cameras/:cameraId/snapshots
export async function uploadSnapshot(req: Request, res: Response) {
  const { cameraId } = req.params;
  const { userId } = req.body;

  // 1. Validation: Kiểm tra file
  if (!req.file) {
    return res.status(400).json({
      error: 'missed_image',
      message: 'Thiếu thông tin bắt buộc: tệp tin ảnh snapshot (image).'
    });
  }

  const file = req.file;

  // Kiểm tra định dạng (chỉ cho phép JPEG/PNG)
  const allowedMimeTypes = ['image/jpeg', 'image/png', 'image/jpg'];
  if (!allowedMimeTypes.includes(file.mimetype)) {
    // Dọn dẹp tệp tạm
    if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
    return res.status(400).json({
      error: 'invalid_image_format',
      message: 'Định dạng hình ảnh không hợp lệ. Chỉ chấp nhận JPG, JPEG hoặc PNG.'
    });
  }

  // Kiểm tra dung lượng (tối đa 5MB = 5 * 1024 * 1024 bytes)
  if (file.size > 5 * 1024 * 1024) {
    if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
    return res.status(400).json({
      error: 'image_too_large',
      message: 'Dung lượng hình ảnh quá lớn. Giới hạn tối đa là 5MB.'
    });
  }

  // 2. Validation: Kiểm tra trường userId
  if (!userId) {
    if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
    return res.status(400).json({
      error: 'missed_user_id',
      message: 'Thiếu thông tin bắt buộc: userId.'
    });
  }

  try {
    // 3. Kiểm tra cameraId tồn tại
    const camera = await prisma.camera.findUnique({
      where: { id: cameraId }
    });
    if (!camera) {
      if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
      return res.status(404).json({
        error: 'not_found_camera',
        message: 'Không tìm thấy trạm camera yêu cầu.'
      });
    }

    // 4. Kiểm tra userId tồn tại
    const user = await prisma.user.findUnique({
      where: { id: userId }
    });
    if (!user) {
      if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
      return res.status(404).json({
        error: 'not_found_user',
        message: 'Người dùng thực hiện tải ảnh không tồn tại.'
      });
    }

    // 5. Upload lên Cloudinary
    const secureUrl = await uploadImage(file.path, 'manual_snapshots');

    // 6. Dọn dẹp tệp tạm trên local storage
    if (fs.existsSync(file.path)) {
      fs.unlinkSync(file.path);
    }

    // 7. Lưu vào Database
    const newSnapshot = await prisma.snapshot.create({
      data: {
        cameraId,
        userId,
        url: secureUrl
      }
    });

    return res.status(201).json({
      id: newSnapshot.id,
      url: newSnapshot.url,
      deviceId: newSnapshot.cameraId,
      userId: newSnapshot.userId,
      uploadedAt: newSnapshot.uploadedAt.toISOString()
    });

  } catch (error: any) {
    // Đảm bảo dọn dẹp file tạm khi có lỗi bất kỳ xảy ra
    if (file && fs.existsSync(file.path)) {
      fs.unlinkSync(file.path);
    }
    console.error('Lỗi khi tải lên snapshot:', error);
    return res.status(500).json({
      error: 'server_error',
      message: `Lỗi máy chủ nội bộ trong quá trình tải ảnh snapshot: ${error?.message || String(error)}`
    });
  }
}
