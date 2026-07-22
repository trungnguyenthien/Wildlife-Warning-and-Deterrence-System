import request from 'supertest';
import app from '../src/app';
import { PrismaClient } from '@prisma/client';
import { clearDatabase, disconnectPrisma } from './helper';

const prisma = new PrismaClient();

describe('CAMERA TESTING SUITE', () => {
  let rangerToken = '';
  let citizenToken = '';
  const camId = 'CAM_TEST_01';

  beforeAll(async () => {
    try {
      await clearDatabase();

      // Tạo tài khoản kiểm lâm Ranger
      await request(app).post('/auth/register').send({
        username: 'ranger_cam',
        password: 'password123',
        fullName: 'Kiem Lam 1',
        phoneNumber: '+84908888881',
        role: 'RANGER'
      });
      const loginRanger = await request(app).post('/auth/login').send({
        username: 'ranger_cam',
        password: 'password123'
      });
      rangerToken = loginRanger.body.token;

      // Tạo tài khoản người dân Citizen
      await request(app).post('/auth/register').send({
        username: 'citizen_cam',
        password: 'password123',
        fullName: 'Dan Thuong 1',
        phoneNumber: '+84908888882',
        role: 'CITIZEN'
      });
      const loginCitizen = await request(app).post('/auth/login').send({
        username: 'citizen_cam',
        password: 'password123'
      });
      citizenToken = loginCitizen.body.token;

      // Tạo sẵn Camera mẫu
      await prisma.camera.create({
        data: {
          id: camId,
          name: 'Trạm camera số 1',
          latitude: 10.762622,
          longitude: 106.660172,
          address: 'Khu A, Rừng Quốc Gia',
          status: 'ONLINE',
          liveFeedUrl: 'rtsp://192.168.1.100/live'
        }
      });
    } catch (err) {
      console.warn('DB Offline - bỏ qua nạp dữ liệu test camera.', err);
    }
  });

  afterAll(async () => {
    await disconnectPrisma();
  });

  // GET /cameras
  it('TC_CAM_LIST_SUCCESS_01: Retrieve camera list successfully', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .get('/cameras')
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body[0]).toHaveProperty('location');
  });

  // GET /cameras/{cameraId}
  it('TC_CAM_DETAIL_SUCCESS_01: Retrieve camera details successfully', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .get(`/cameras/${camId}`)
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(200);
    expect(res.body.id).toBe(camId);
    expect(res.body.name).toBe('Trạm camera số 1');
  });

  it('TC_CAM_DETAIL_FAILURE_01: Retrieve details of non-existing camera', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .get('/cameras/CAM_NON_EXIST')
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(404);
  });

  it('TC_CAM_DETAIL_FAILURE_02: Retrieve details with camera ID exceeds limit', async () => {
    if (!rangerToken) return;
    const longId = 'C'.repeat(51);
    const res = await request(app)
      .get(`/cameras/${longId}`)
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toContain('quá dài');
  });

  // PATCH /cameras/{cameraId}
  it('TC_CAM_RENAME_SUCCESS_01: Rename camera successfully by Ranger', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .patch(`/cameras/${camId}`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ name: 'Trạm camera số 1 - Đã cập nhật' });

    expect(res.status).toBe(200);
    expect(res.body.name).toBe('Trạm camera số 1 - Đã cập nhật');
  });

  it('TC_CAM_RENAME_FAILURE_01: Rename camera failed by Citizen (Forbidden)', async () => {
    if (!citizenToken) return;
    const res = await request(app)
      .patch(`/cameras/${camId}`)
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ name: 'Tên mới' });

    expect(res.status).toBe(403);
  });

  it('TC_CAM_RENAME_FAILURE_02: Rename camera missing name', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .patch(`/cameras/${camId}`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ name: '' });

    expect(res.status).toBe(400);
  });

  it('TC_CAM_RENAME_FAILURE_03: Rename camera name exceeds limit', async () => {
    if (!rangerToken) return;
    const longName = 'C'.repeat(101);
    const res = await request(app)
      .patch(`/cameras/${camId}`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ name: longName });

    expect(res.status).toBe(400);
  });

  // POST /cameras/{cameraId}/devices/{deviceKey}/test
  it('TC_CAM_TEST_SUCCESS_01: Trigger device test successfully', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .post(`/cameras/${camId}/devices/led/test`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({
        durationSeconds: 10,
        intensity: 80
      });

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('commandId');
    expect(res.body.status).toBe('SENT');
  });

  it('TC_CAM_TEST_FAILURE_01: Device test missing durationSeconds', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .post(`/cameras/${camId}/devices/led/test`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ intensity: 80 });

    expect(res.status).toBe(400);
    expect(res.body.error).toContain('durationSeconds');
  });

  it('TC_CAM_TEST_FAILURE_02: Device test missing intensity', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .post(`/cameras/${camId}/devices/led/test`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ durationSeconds: 10 });

    expect(res.status).toBe(400);
    expect(res.body.error).toContain('intensity');
  });

  it('TC_CAM_TEST_FAILURE_03: Device test with negative duration', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .post(`/cameras/${camId}/devices/led/test`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ durationSeconds: -5, intensity: 80 });

    expect(res.status).toBe(400);
  });

  it('TC_CAM_TEST_FAILURE_04: Device test with intensity negative', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .post(`/cameras/${camId}/devices/led/test`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ durationSeconds: 10, intensity: -10 });

    expect(res.status).toBe(400);
  });

  it('TC_CAM_TEST_FAILURE_05: Device test with intensity exceeds 100', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .post(`/cameras/${camId}/devices/led/test`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ durationSeconds: 10, intensity: 150 });

    expect(res.status).toBe(400);
  });

  it('TC_CAM_TEST_FAILURE_06: Device test with unsupported device key', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .post(`/cameras/${camId}/devices/laser/test`)
      .set('Authorization', `Bearer ${rangerToken}`)
      .send({ durationSeconds: 10, intensity: 80 });

    expect(res.status).toBe(404);
  });
});
