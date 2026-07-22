import { Request, Response } from 'express';
import { PrismaClient, Role } from '@prisma/client';
import bcrypt from 'bcrypt';
import crypto from 'crypto';
import { AuthenticatedRequest } from '../middlewares/auth';

const prisma = new PrismaClient();

// Đăng ký tài khoản mới
export async function register(req: Request, res: Response) {
  const { username, password, fullName, phoneNumber, role } = req.body;

  // Validation: Thiếu các trường bắt buộc
  if (!username) {
    return res.status(400).json({ error: 'missed_username', message: 'Thiếu thông tin bắt buộc: username.' });
  }
  if (!password) {
    return res.status(400).json({ error: 'missed_password', message: 'Thiếu thông tin bắt buộc: password.' });
  }
  if (!fullName) {
    return res.status(400).json({ error: 'missed_fullName', message: 'Thiếu thông tin bắt buộc: fullName.' });
  }
  if (!phoneNumber) {
    return res.status(400).json({ error: 'missed_phoneNumber', message: 'Thiếu thông tin bắt buộc: phoneNumber.' });
  }
  if (!role) {
    return res.status(400).json({ error: 'missed_role', message: 'Thiếu thông tin bắt buộc: role.' });
  }

  // Validation: Số điện thoại phải đúng chuẩn E.164
  const e164Regex = /^\+[1-9]\d{1,14}$/;
  if (!e164Regex.test(phoneNumber)) {
    return res.status(400).json({ error: 'invalid_phone_number', message: 'Định dạng số điện thoại không hợp lệ. Phải đúng chuẩn E.164 (ví dụ: +84901234567).' });
  }

  // Validation: Mật khẩu tối thiểu 6 ký tự
  if (password.length < 6) {
    return res.status(400).json({ error: 'invalid_password', message: 'Mật khẩu phải chứa ít nhất 6 ký tự.' });
  }

  // Validation: Vai trò (role) không hợp lệ
  const validRoles = Object.values(Role);
  if (!validRoles.includes(role as Role)) {
    return res.status(400).json({ error: 'invalid_role', message: `Vai trò không hợp lệ. Phải thuộc: ${validRoles.join(', ')}.` });
  }

  try {
    // Kiểm tra trùng username hoặc phoneNumber
    const existingUser = await prisma.user.findFirst({
      where: {
        OR: [
          { username },
          { phoneNumber }
        ]
      }
    });

    if (existingUser) {
      return res.status(409).json({ error: 'duplicate_user', message: 'Username hoặc số điện thoại đã tồn tại trong hệ thống.' });
    }

    // Mã hóa mật khẩu
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // Lưu User vào Database
    const newUser = await prisma.user.create({
      data: {
        username,
        passwordHash,
        fullName,
        phoneNumber,
        role: role as Role
      }
    });

    return res.status(201).json({
      id: newUser.id,
      username: newUser.username,
      fullName: newUser.fullName,
      phoneNumber: newUser.phoneNumber,
      role: newUser.role,
      createdAt: newUser.createdAt.toISOString()
    });
  } catch (error) {
    console.error('Lỗi khi đăng ký tài khoản:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ trong quá trình đăng ký.' });
  }
}

// Đăng nhập tài khoản
export async function login(req: Request, res: Response) {
  const { username, password } = req.body;

  // Validation: Thiếu các trường bắt buộc
  if (!username) {
    return res.status(400).json({ error: 'missed_username', message: 'Thiếu thông tin bắt buộc: username.' });
  }
  if (!password) {
    return res.status(400).json({ error: 'missed_password', message: 'Thiếu thông tin bắt buộc: password.' });
  }

  try {
    // Tìm người dùng
    const user = await prisma.user.findUnique({
      where: { username }
    });

    if (!user) {
      return res.status(401).json({ error: 'unauthorized_credentials', message: 'Sai tài khoản hoặc mật khẩu.' });
    }

    // Kiểm tra mật khẩu
    const isPasswordMatch = await bcrypt.compare(password, user.passwordHash);
    if (!isPasswordMatch) {
      return res.status(401).json({ error: 'unauthorized_credentials', message: 'Sai tài khoản hoặc mật khẩu.' });
    }

    // Sinh token ngẫu nhiên (sử dụng uuid hoặc một token ngẫu nhiên an toàn)
    const token = crypto.randomBytes(32).toString('hex');

    // Lưu token vào DB (Giả lập token xác thực tĩnh)
    await prisma.pushToken.create({
      data: {
        userId: user.id,
        token,
        platform: 'mobile'
      }
    });

    return res.status(200).json({
      token,
      role: user.role
    });
  } catch (error) {
    console.error('Lỗi khi đăng nhập tài khoản:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ trong quá trình đăng nhập.' });
  }
}

// Đăng xuất tài khoản
export async function logout(req: AuthenticatedRequest, res: Response) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'missed_token', message: 'Truy cập bị từ chối: Thiếu token xác thực.' });
  }

  try {
    // Hủy đăng ký push token tương ứng
    await prisma.pushToken.deleteMany({
      where: { token }
    });

    return res.status(200).json({ message: 'Đăng xuất và hủy token thành công.' });
  } catch (error) {
    console.error('Lỗi khi đăng xuất:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ trong quá trình đăng xuất.' });
  }
}

// Lấy thông tin tài khoản hiện tại
export async function me(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'unauthorized_session', message: 'Truy cập bị từ chối: Không tìm thấy thông tin phiên.' });
  }

  try {
    const user = await prisma.user.findUnique({
      where: { id: req.user.id },
      select: {
        id: true,
        username: true,
        fullName: true,
        phoneNumber: true,
        role: true,
        createdAt: true
      }
    });

    if (!user) {
      return res.status(404).json({ error: 'not_found_user', message: 'Người dùng không tồn tại.' });
    }

    return res.status(200).json({
      id: user.id,
      username: user.username,
      fullName: user.fullName,
      phoneNumber: user.phoneNumber,
      role: user.role,
      createdAt: user.createdAt.toISOString()
    });
  } catch (error) {
    console.error('Lỗi khi lấy thông tin cá nhân:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// Cập nhật thông tin cá nhân (Chỉ dùng để kiểm thử)
export async function updateMe(req: AuthenticatedRequest, res: Response) {
  if (!req.user) {
    return res.status(401).json({ error: 'unauthorized_session', message: 'Truy cập bị từ chối.' });
  }

  const { fullName, phoneNumber } = req.body;

  try {
    // Kiểm tra định dạng số điện thoại nếu được cập nhật
    if (phoneNumber) {
      const e164Regex = /^\+[1-9]\d{1,14}$/;
      if (!e164Regex.test(phoneNumber)) {
        return res.status(400).json({ error: 'invalid_phone_number', message: 'Số điện thoại không hợp lệ.' });
      }

      // Kiểm tra trùng SĐT
      const existingUser = await prisma.user.findFirst({
        where: {
          phoneNumber,
          NOT: { id: req.user.id }
        }
      });
      if (existingUser) {
        return res.status(409).json({ error: 'duplicate_phone_number', message: 'Số điện thoại đã được đăng ký bởi tài khoản khác.' });
      }
    }

    const updatedUser = await prisma.user.update({
      where: { id: req.user.id },
      data: {
        fullName: fullName || undefined,
        phoneNumber: phoneNumber || undefined
      }
    });

    return res.status(200).json({
      id: updatedUser.id,
      username: updatedUser.username,
      fullName: updatedUser.fullName,
      phoneNumber: updatedUser.phoneNumber,
      role: updatedUser.role
    });
  } catch (error) {
    console.error('Lỗi khi cập nhật tài khoản:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}
