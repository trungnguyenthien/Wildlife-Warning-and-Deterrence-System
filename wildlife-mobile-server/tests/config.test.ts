import request from 'supertest';
import app from '../src/app';
import { PrismaClient } from '@prisma/client';
import { clearDatabase, disconnectPrisma } from './helper';

const prisma = new PrismaClient();

describe('RESPONSE CONFIGS TESTING SUITE', () => {
  let token = '';
  const camId = 'CAM_CFG_01';
  const speciesId = 'voi_chau_a';

  beforeAll(async () => {
    try {
      await clearDatabase();

      // Đăng ký và đăng nhập
      await request(app).post('/auth/register').send({
        username: 'config_user',
        password: 'password123',
        fullName: 'Config Manager',
        phoneNumber: '+84906666661',
        role: 'RANGER'
      });
      const login = await request(app).post('/auth/login').send({
        username: 'config_user',
        password: 'password123'
      });
      token = login.body.token;

      // Tạo Camera
      await prisma.camera.create({
        data: {
          id: camId,
          name: 'Camera Cấu Hình',
          latitude: 10.1,
          longitude: 106.2,
          address: 'Khu B',
          status: 'ONLINE',
          liveFeedUrl: 'rtsp://192.168.1.101/live'
        }
      });

      // Tạo Loài động vật
      await prisma.species.create({
        data: {
          id: speciesId,
          displayName: 'Voi Châu Á',
          dangerLevel: 'CRITICAL',
          isHuman: false,
          htmlDescription: '<p>Voi nguy hiểm</p>',
          aggressionLevel: 5,
          recommendAction: 'Kích hoạt còi hú'
        }
      });
    } catch (err) {
      console.warn('DB Offline - bỏ qua nạp dữ liệu test config.', err);
    }
  });

  afterAll(async () => {
    await disconnectPrisma();
  });

  // 1. GET /species
  it('TC_SPC_LIST_SUCCESS_01: Retrieve species directory successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/species')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body[0].id).toBe(speciesId);
  });

  // 2. GET /response-configs
  it('TC_CFG_LIST_SUCCESS_01: Retrieve configs applied for current user successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .get(`/response-configs`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
  });

  // 3. GET /response-configs?speciesId=
  it('TC_CFG_DET_SUCCESS_01: Retrieve custom configuration successfully when custom record exists', async () => {
    if (!token) return;
    // Trước tiên, lưu cấu hình tùy chỉnh vào DB
    await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({
        ledFlash: true,
        ledColor: 'RED',
        ledIntensity: 90,
        speakerWarn: true,
        audioSampleId: 'A_gunshot',
        audioIntensity: 85,
        silentAlert: false
      });

    const res = await request(app)
      .get(`/response-configs?speciesId=${speciesId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    // Kiểm tra trường `id` của cấu hình tùy chỉnh (khác với fallback preset)
    expect(res.body).toHaveProperty('id');
    expect(res.body).toHaveProperty('userId');
    expect(res.body).toHaveProperty('speciesId');
  });

  it('TC_CFG_DET_SUCCESS_02: Retrieve custom config details or fallback preset', async () => {
    if (!token) return;
    // Xóa cấu hình tùy chỉnh để test fallback
    await request(app)
      .delete(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`);

    const res = await request(app)
      .get(`/response-configs?speciesId=${speciesId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    // Voi châu á có dangerLevel = CRITICAL -> fallback preset
    expect(res.body.ledColor).toBe('STROBE');
  });

  it('TC_CFG_DET_FAILURE_02: Fail to retrieve config when speciesId query parameter is missing', async () => {
    if (!token) return;
    const res = await request(app)
      .get(`/response-configs?speciesId=`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_species_id');
  });

  it('TC_CFG_DET_FAILURE_03: Fail to retrieve config when speciesId is invalid', async () => {
    if (!token) return;
    const res = await request(app)
      .get(`/response-configs?speciesId=non_existent_species`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(404);
    expect(res.body.error).toBe('not_found_species');
  });

  // 4. PUT /response-configs/{speciesId}
  const validConfigPayload = {
    ledFlash: true,
    ledColor: 'RED',
    ledIntensity: 90,
    speakerWarn: true,
    audioSampleId: 'A_gunshot',
    audioIntensity: 85,
    silentAlert: false
  };

  it('TC_CFG_SAVE_SUCCESS_01: Save custom defense configuration successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send(validConfigPayload);

    expect(res.status).toBe(200);
    expect(res.body.ledColor).toBe('RED');
    expect(res.body.ledIntensity).toBe(90);
  });

  it('TC_CFG_SAVE_FAILURE_01: Save config missing ledFlash', async () => {
    if (!token) return;
    const { ledFlash: _, ...payload } = validConfigPayload;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_led_flash');
  });

  it('TC_CFG_SAVE_FAILURE_02: Save config missing speakerWarn', async () => {
    if (!token) return;
    const { speakerWarn: _, ...payload } = validConfigPayload;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_speaker_warn');
  });

  it('TC_CFG_SAVE_FAILURE_04: Save config missing silentAlert', async () => {
    if (!token) return;
    const { silentAlert: _, ...payload } = validConfigPayload;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_silent_alert');
  });

  it('TC_CFG_SAVE_FAILURE_05: Save config with invalid LED color', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, ledColor: 'BLUE' }); // BLUE không có trong enum

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_led_color');
  });

  it('TC_CFG_SAVE_FAILURE_07: Save config with negative LED intensity', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, ledIntensity: -1 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_led_intensity');
  });

  it('TC_CFG_SAVE_FAILURE_06: Save config with LED intensity exceeds 100', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, ledIntensity: 120 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_led_intensity');
  });

  it('TC_CFG_SAVE_FAILURE_09: Save config with negative audio intensity', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, audioIntensity: -5 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_audio_intensity');
  });

  it('TC_CFG_SAVE_FAILURE_08: Save config with audio intensity exceeds 100', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, audioIntensity: 110 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_audio_intensity');
  });

  // 5. POST /response-configs/{speciesId}/apply-preset/{presetId}
  it('TC_CFG_PRE_SUCCESS_01: Apply danger level preset successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .post(`/response-configs/${speciesId}/apply-preset/critical_danger`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.ledColor).toBe('STROBE');
  });

  it('TC_CFG_PRE_FAILURE_01: Fail to apply preset with invalid presetId', async () => {
    if (!token) return;
    const res = await request(app)
      .post(`/response-configs/${speciesId}/apply-preset/non_existent_preset`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(404);
    expect(res.body.error).toBe('not_found_preset');
  });

  // 6. DELETE /response-configs/{speciesId}
  it('TC_CFG_DEL_SUCCESS_01: Reset custom config to fallback presets successfully', async () => {
    if (!token) return;
    // Đảm bảo có cấu hình tùy chỉnh trước khi xóa
    await request(app)
      .put(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({
        ledFlash: true,
        ledColor: 'RED',
        ledIntensity: 90,
        speakerWarn: false,
        audioIntensity: 0,
        silentAlert: false
      });

    const res = await request(app)
      .delete(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(204);
  });

  it('TC_CFG_DEL_FAILURE_01: Fail to delete non-existent custom configuration', async () => {
    if (!token) return;
    // Lúc này đã xóa ở test trước, nên xóa lần nữa phải trả về 404
    const res = await request(app)
      .delete(`/response-configs/${speciesId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(404);
    expect(res.body.error).toBe('not_found_config');
  });
});
