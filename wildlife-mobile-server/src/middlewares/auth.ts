import { Request, Response, NextFunction } from 'express';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

export interface AuthenticatedRequest extends Request {
  user?: {
    id: string;
    username: string;
    role: string;
  };
}

export async function authenticateToken(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer <token>

  if (!token) {
    return res.status(401).json({ error: 'Truy cập bị từ chối: Thiếu token xác thực.' });
  }

  try {
    // Tìm PushToken tương ứng để lấy thông tin user
    const pushTokenRecord = await prisma.pushToken.findUnique({
      where: { token },
      include: { user: true },
    });

    if (!pushTokenRecord || !pushTokenRecord.user) {
      return res.status(403).json({ error: 'Token không hợp lệ hoặc đã hết hạn.' });
    }

    req.user = {
      id: pushTokenRecord.user.id,
      username: pushTokenRecord.user.username,
      role: pushTokenRecord.user.role,
    };

    return next();
  } catch (error) {
    console.error('Lỗi xác thực Token:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ trong quá trình xác thực.' });
  }
}
