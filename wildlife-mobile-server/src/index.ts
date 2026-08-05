import app from './app';
import dotenv from 'dotenv';
import http from 'http';
import { PrismaClient } from '@prisma/client';
import { runSeed } from './config/seed';

dotenv.config({ path: '.env.local' });
dotenv.config();

const PORT = process.env.PORT || 3000;
const prisma = new PrismaClient();

async function checkAndSeedDatabase() {
  try {
    const userCount = await prisma.user.count();
    if (userCount === 0) {
      console.log('[DB-Check] Phát hiện Cơ sở dữ liệu trống! Đang tiến hành tự động nạp dữ liệu mẫu...');
      await runSeed(prisma);
      console.log('[DB-Check] Đã tự động nạp dữ liệu mẫu thành công.');
    } else {
      console.log(`[DB-Check] Cơ sở dữ liệu hoạt động bình thường (${userCount} người dùng).`);
    }
  } catch (error) {
    console.error('[DB-Check] Lỗi kiểm tra/nạp dữ liệu tự động:', error);
  } finally {
    await prisma.$disconnect();
  }
}

const server = http.createServer(app);

server.listen(PORT, async () => {
  console.log(`Server is running locally at http://localhost:${PORT}`);
  await checkAndSeedDatabase();
});
