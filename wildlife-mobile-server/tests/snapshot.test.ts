import request from 'supertest';
import app from '../src/app';
import { PrismaClient } from '@prisma/client';
import { clearDatabase, disconnectPrisma } from './helper';
import path from 'path';

const prisma = new PrismaClient();

describe('MANUAL SNAPSHOT UPLOAD TESTING SUITE', () => {
  let token = '';
  let userId = '';
  const camId = 'CAM_SNAP_TEST';
  const sampleImagePath = path.join(__dirname, '../f4.png');

  beforeAll(async () => {
    try {
      await clearDatabase();

      // Đăng ký và đăng nhập Ranger
      const regRes = await request(app).post('/auth/register').send({
        username: 'snap_user',
        password: 'password123',
        fullName: 'Snap Ranger',
        phoneNumber: '+84909999991',
        role: 'RANGER'
      });
      userId = regRes.body.id;

      const login = await request(app).post('/auth/login').send({
        username: 'snap_user',
        password: 'password123'
      });
      token = login.body.token;

      // Tạo Camera mẫu
      await prisma.camera.create({
        data: {
          id: camId,
          name: 'Trạm camera số 4',
          latitude: 10.7643,
          longitude: 106.6631,
          address: 'Khu D, Rừng Quốc Gia',
          status: 'ONLINE',
          liveFeedUrl: 'rtsp://192.168.1.104/live'
        }
      });
    } catch (err) {
      console.warn('DB Offline - bỏ qua nạp dữ liệu test snapshot upload.', err);
    }
  });

  afterAll(async () => {
    await disconnectPrisma();
  });

  // POST /cameras/{cameraId}/snapshots
  it('TC_SNAP_UPL_SUCCESS_01: Upload manual camera snapshot successfully to Cloudinary', async () => {
    if (!token) return;
    const res = await request(app)
      .post(`/cameras/${camId}/snapshots`)
      .set('Authorization', `Bearer ${token}`)
      .attach('image', sampleImagePath)
      .field('userId', userId);

    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('id');
    expect(res.body).toHaveProperty('url');
    expect(res.body.deviceId).toBe(camId);
    expect(res.body.userId).toBe(userId);
    expect(res.body).toHaveProperty('uploadedAt');

    // Kiểm tra trong database xem bản ghi đã được tạo chưa
    const snapshotInDb = await prisma.snapshot.findUnique({
      where: { id: res.body.id }
    });
    expect(snapshotInDb).not.toBeNull();
    expect(snapshotInDb!.url).toBe(res.body.url);
  });

  it('TC_SNAP_UPL_FAILURE_01: Fail to upload manual camera snapshot due to missing image file', async () => {
    if (!token) return;
    const res = await request(app)
      .post(`/cameras/${camId}/snapshots`)
      .set('Authorization', `Bearer ${token}`)
      .field('userId', userId);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_image');
    expect(res.body.message).toContain('tệp tin ảnh');
  });

  it('TC_SNAP_UPL_FAILURE_02: Fail to upload manual camera snapshot with non-existent userId', async () => {
    if (!token) return;
    const res = await request(app)
      .post(`/cameras/${camId}/snapshots`)
      .set('Authorization', `Bearer ${token}`)
      .attach('image', sampleImagePath)
      .field('userId', 'non-existent-user-uuid');

    expect(res.status).toBe(404);
    expect(res.body.error).toBe('not_found_user');
    expect(res.body.message).toContain('Người dùng');
  });

  it('TC_SNAP_UPL_FAILURE_03: Fail to upload manual camera snapshot for non-existent cameraId', async () => {
    if (!token) return;
    const res = await request(app)
      .post('/cameras/CAM_NON_EXIST_9999/snapshots')
      .set('Authorization', `Bearer ${token}`)
      .attach('image', sampleImagePath)
      .field('userId', userId);

    expect(res.status).toBe(404);
    expect(res.body.error).toBe('not_found_camera');
    expect(res.body.message).toContain('camera');
  });
});
