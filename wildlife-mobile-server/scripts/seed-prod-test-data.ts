import { PrismaClient } from '@prisma/client';
import { runSeed } from '../src/config/seed';

const prisma = new PrismaClient();

runSeed(prisma)
  .then(async () => {
    await prisma.$disconnect();
  })
  .catch(async (e) => {
    console.error('Lỗi khi seed dữ liệu:', e);
    await prisma.$disconnect();
    process.exit(1);
  });
