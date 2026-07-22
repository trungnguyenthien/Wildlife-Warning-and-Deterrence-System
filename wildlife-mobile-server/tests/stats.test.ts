import request from 'supertest';
import app from '../src/app';
import { PrismaClient } from '@prisma/client';
import { clearDatabase, disconnectPrisma } from './helper';

const prisma = new PrismaClient();

describe('STATISTICS TESTING SUITE', () => {
  let token = '';
  const camId = 'CAM_STAT_01';

  beforeAll(async () => {
    try {
      await clearDatabase();

      await request(app).post('/auth/register').send({
        username: 'stat_user',
        password: 'password123',
        fullName: 'Stats Admin',
        phoneNumber: '+84905555551',
        role: 'RANGER'
      });
      const login = await request(app).post('/auth/login').send({
        username: 'stat_user',
        password: 'password123'
      });
      token = login.body.token;

      await prisma.camera.create({
        data: {
          id: camId,
          name: 'Camera Thống Kê',
          latitude: 10.5,
          longitude: 106.3,
          address: 'Khu C',
          status: 'ONLINE',
          liveFeedUrl: 'rtsp://192.168.1.102/live'
        }
      });
    } catch (err) {
      console.warn('DB Offline - bỏ qua nạp dữ liệu test stats.', err);
    }
  });

  afterAll(async () => {
    await disconnectPrisma();
  });

  // GET /stats/summary
  it('TC_STAT_SUM_SUCCESS_01: Retrieve statistics summary successfully with valid dates', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/stats/summary?startDate=2026-01-01&endDate=2026-12-31')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('trendData');
    expect(res.body).toHaveProperty('speciesBreakdown');
    expect(res.body).toHaveProperty('heatmapData');
    expect(Array.isArray(res.body.trendData)).toBe(true);
    expect(Array.isArray(res.body.speciesBreakdown)).toBe(true);
    expect(Array.isArray(res.body.heatmapData)).toBe(true);
  });

  it('TC_STAT_SUM_FAILURE_01: Fail to retrieve statistics due to missing startDate', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/stats/summary?endDate=2026-12-31')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_start_date');
    expect(res.body.message).toContain('startDate');
  });

  it('TC_STAT_SUM_FAILURE_02: Fail to retrieve statistics due to missing endDate', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/stats/summary?startDate=2026-01-01')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_end_date');
    expect(res.body.message).toContain('endDate');
  });

  it('TC_STAT_SUM_FAILURE_03: Fail to retrieve statistics with invalid format startDate', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/stats/summary?startDate=22-07-2026&endDate=2026-12-31')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_date_format');
    expect(res.body.message).toContain('YYYY-MM-DD');
  });

  it('TC_STAT_SUM_FAILURE_04: Fail to retrieve statistics with invalid format endDate', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/stats/summary?startDate=2026-01-01&endDate=31-12-2026')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_date_format');
    expect(res.body.message).toContain('YYYY-MM-DD');
  });

  it('TC_STAT_SUM_FAILURE_05: Fail to retrieve statistics when startDate is after endDate', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/stats/summary?startDate=2026-12-31&endDate=2026-01-01')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_date_range');
    expect(res.body.message).toContain('startDate');
  });
});
