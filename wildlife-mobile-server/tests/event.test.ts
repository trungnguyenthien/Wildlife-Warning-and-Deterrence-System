import request from 'supertest';
import app from '../src/app';
import { PrismaClient } from '@prisma/client';
import { clearDatabase, disconnectPrisma } from './helper';

const prisma = new PrismaClient();

describe('EVENTS & ALERTS INTEGRATION SUITE', () => {
  let rangerToken = '';
  let citizenToken = '';
  const camId = 'CAM_EVT_01';
  const speciesId = 'voi_rung';
  const humanId = 'human_border_intruder';

  beforeAll(async () => {
    try {
      await clearDatabase();

      // Đăng ký Ranger
      await request(app).post('/auth/register').send({
        username: 'ranger_evt',
        password: 'password123',
        fullName: 'Ranger Evt',
        phoneNumber: '+84905555551',
        role: 'RANGER'
      });
      const loginRanger = await request(app).post('/auth/login').send({
        username: 'ranger_evt',
        password: 'password123'
      });
      rangerToken = loginRanger.body.token;

      // Đăng ký Citizen
      await request(app).post('/auth/register').send({
        username: 'citizen_evt',
        password: 'password123',
        fullName: 'Citizen Evt',
        phoneNumber: '+84905555552',
        role: 'CITIZEN'
      });
      const loginCitizen = await request(app).post('/auth/login').send({
        username: 'citizen_evt',
        password: 'password123'
      });
      citizenToken = loginCitizen.body.token;

      // Tạo Camera
      await prisma.camera.create({
        data: {
          id: camId,
          name: 'Camera Trạm Đông',
          latitude: 12.34,
          longitude: 108.9,
          address: 'Trạm kiểm lâm số 3',
          status: 'ONLINE',
          liveFeedUrl: 'rtsp://192.168.1.102/live'
        }
      });

      // Tạo Loài hoang dã
      await prisma.species.create({
        data: {
          id: speciesId,
          displayName: 'Voi Rừng',
          dangerLevel: 'CRITICAL',
          isHuman: false,
          htmlDescription: '<p>Voi</p>',
          aggressionLevel: 4,
          recommendAction: 'Coi hu'
        }
      });

      // Tạo đối tượng Con người xâm nhập biên giới
      await prisma.species.create({
        data: {
          id: humanId,
          displayName: 'Xâm nhập biên giới',
          dangerLevel: 'MEDIUM',
          isHuman: true,
          htmlDescription: '<p>Xâm nhập</p>',
          aggressionLevel: 1,
          recommendAction: 'Cảnh báo biên phòng'
        }
      });
    } catch (err) {
      console.warn('DB Offline - bỏ qua nạp dữ liệu test event.', err);
    }
  });

  afterAll(async () => {
    await disconnectPrisma();
  });

  // 1. GET /events
  it('TC_EVT_LIST_SUCCESS_01: Retrieve event history logs successfully', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .get(`/events?cameraId=${camId}`)
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
  });

  // 2. Webhook AI Server: POST /cameras/{cameraId}/detections
  const validAiPayload = {
    detections: [
      { speciesId: 'voi_rung', confidence: 0.95 }
    ],
    imageUrl: 'https://cdn.example.com/voi-1.jpg',
    detectedAt: '2026-07-22T10:00:00Z'
  };

  it('TC_EVT_DETECTION_SUCCESS_01: Create new event and alert from AI webhook', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send(validAiPayload);

    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('eventId');
    expect(res.body.detections.length).toBe(1);
    expect(res.body.responseAction.electricFence).toBe(true); // Voi -> CRITICAL -> Fence true
  });

  it('TC_EVT_DETECTION_SUCCESS_02: Append to active event if duration is under 5 minutes', async () => {
    if (!rangerToken) return;
    // Gửi tiếp nhận diện lúc 10h02 (cách 2 phút)
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send({
        ...validAiPayload,
        detectedAt: '2026-07-22T10:02:00Z',
        imageUrl: 'https://cdn.example.com/voi-2.jpg'
      });

    expect(res.status).toBe(200); // 200 vì gom nhóm vào sự kiện đang diễn ra
    expect(res.body.detections.length).toBe(1);
  });

  it('TC_EVT_DETECTION_SUCCESS_03: Split into new event if duration exceeds 5 minutes', async () => {
    if (!rangerToken) return;
    // Gửi tiếp nhận diện lúc 10h10 (cách 8 phút)
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send({
        ...validAiPayload,
        detectedAt: '2026-07-22T10:10:00Z',
        imageUrl: 'https://cdn.example.com/voi-3.jpg'
      });

    expect(res.status).toBe(201); // Tạo sự kiện mới cách biệt
  });

  it('TC_EVT_DETECTION_FAILURE_01: AI Webhook missing detections', async () => {
    const { detections: _, ...payload } = validAiPayload;
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_detections');
  });

  it('TC_EVT_DETECTION_FAILURE_02: AI Webhook missing imageUrl', async () => {
    const { imageUrl: _, ...payload } = validAiPayload;
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_image_url');
  });

  it('TC_EVT_DETECTION_FAILURE_03: AI Webhook missing detectedAt', async () => {
    const { detectedAt: _, ...payload } = validAiPayload;
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_detected_at');
  });

  it('TC_EVT_DETECTION_FAILURE_04: AI Webhook with empty detections array', async () => {
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send({ ...validAiPayload, detections: [] });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_detections');
  });

  it('TC_EVT_DETECTION_FAILURE_05: AI Webhook with negative confidence', async () => {
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send({
        ...validAiPayload,
        detections: [{ speciesId: speciesId, confidence: -0.1 }]
      });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_confidence');
  });

  it('TC_EVT_DETECTION_FAILURE_06: AI Webhook with confidence exceeds 1', async () => {
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send({
        ...validAiPayload,
        detections: [{ speciesId: speciesId, confidence: 1.5 }]
      });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_confidence');
  });

  it('TC_EVT_DETECTION_FAILURE_07: AI Webhook with invalid imageUrl URL format', async () => {
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send({ ...validAiPayload, imageUrl: 'ftp-not-valid-url' });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_image_url');
  });

  it('TC_EVT_DETECTION_FAILURE_08: AI Webhook with invalid detectedAt format', async () => {
    const res = await request(app)
      .post(`/cameras/${camId}/detections`)
      .send({ ...validAiPayload, detectedAt: '22-07-2026' }); // Sai ISO 8601

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_detected_at');
  });

  // 3. GET /alerts/feed & GET /alerts/feed/{alertId}/read
  it('TC_EVT_FEED_SUCCESS_01: Ranger reads alert feed including security alerts', async () => {
    if (!rangerToken) return;

    // Gửi cảnh báo người xâm nhập biên phòng từ AI
    await request(app)
      .post(`/cameras/${camId}/detections`)
      .send({
        detections: [{ speciesId: 'human_border_intruder', confidence: 0.99 }],
        imageUrl: 'https://cdn.example.com/human-1.jpg',
        detectedAt: '2026-07-22T10:15:00Z'
      });

    const res = await request(app)
      .get('/alerts/feed')
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(200);
    // Ranger phải xem được tin HUMAN_BORDER vừa bắn
    const humanAlert = res.body.find((a: any) => a.type === 'HUMAN_BORDER');
    expect(humanAlert).toBeDefined();
  });

  it('TC_EVT_FEED_SUCCESS_02: Citizen reads alert feed without security alerts', async () => {
    if (!citizenToken) return;
    const res = await request(app)
      .get('/alerts/feed')
      .set('Authorization', `Bearer ${citizenToken}`);

    expect(res.status).toBe(200);
    // Citizen không được xem tin HUMAN_BORDER
    const humanAlert = res.body.find((a: any) => a.type === 'HUMAN_BORDER');
    expect(humanAlert).toBeUndefined();
  });

  it('TC_EVT_FEED_FAILURE_01: Retrieve feed with negative page number', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .get('/alerts/feed?page=-1')
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_page');
  });

  it('TC_EVT_FEED_FAILURE_02: Retrieve feed with negative size number', async () => {
    if (!rangerToken) return;
    const res = await request(app)
      .get('/alerts/feed?size=-5')
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_size');
  });

  it('TC_EVT_READ_SUCCESS_01: Mark alert notification as read successfully', async () => {
    if (!rangerToken) return;
    // Lấy tin cảnh báo đầu tiên
    const feed = await request(app)
      .get('/alerts/feed')
      .set('Authorization', `Bearer ${rangerToken}`);
    const alertId = feed.body[0].id;

    const res = await request(app)
      .post(`/alerts/feed/${alertId}/read`)
      .set('Authorization', `Bearer ${rangerToken}`);

    expect(res.status).toBe(200);

    // Lấy lại feed kiểm tra trạng thái đọc
    const refetched = await request(app)
      .get('/alerts/feed')
      .set('Authorization', `Bearer ${rangerToken}`);
    expect(refetched.body[0].isRead).toBe(true);
  });
});
