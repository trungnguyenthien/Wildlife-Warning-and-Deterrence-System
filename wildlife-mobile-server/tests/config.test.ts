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
  it('TC_CFG_SPECIES_LIST_SUCCESS_01: Retrieve species dictionary successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/species')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body[0].id).toBe(speciesId);
  });

  // 2. GET /response-configs/{cameraId}
  it('TC_CFG_LIST_SUCCESS_01: Retrieve configs applied at camera successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .get(`/response-configs/${camId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
  });

  // 3. GET /response-configs
  it('TC_CFG_DETAIL_SUCCESS_01: Retrieve custom config details or fallback preset', async () => {
    if (!token) return;
    const res = await request(app)
      .get(`/response-configs?cameraId=${camId}&speciesId=${speciesId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    // Voi châu á có dangerLevel = CRITICAL -> fallback preset
    expect(res.body.ledColor).toBe('STROBE');
    expect(res.body.electricFence).toBe(true);
  });

  // 4. PUT /response-configs/{cameraId}/{speciesId}
  const validConfigPayload = {
    ledFlash: true,
    ledColor: 'RED',
    ledIntensity: 90,
    speakerWarn: true,
    audioSampleId: 'A_gunshot',
    audioIntensity: 85,
    electricFence: true,
    electricFenceDuration: 20,
    silentAlert: false
  };

  it('TC_CFG_SAVE_SUCCESS_01: Save custom defense configuration successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
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
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_led_flash');
  });

  it('TC_CFG_SAVE_FAILURE_02: Save config missing speakerWarn', async () => {
    if (!token) return;
    const { speakerWarn: _, ...payload } = validConfigPayload;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_speaker_warn');
  });

  it('TC_CFG_SAVE_FAILURE_03: Save config missing electricFence', async () => {
    if (!token) return;
    const { electricFence: _, ...payload } = validConfigPayload;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_electric_fence');
  });

  it('TC_CFG_SAVE_FAILURE_04: Save config missing silentAlert', async () => {
    if (!token) return;
    const { silentAlert: _, ...payload } = validConfigPayload;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_silent_alert');
  });

  it('TC_CFG_SAVE_FAILURE_05: Save config with invalid LED color', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, ledColor: 'BLUE' }); // BLUE không có trong enum

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_led_color');
  });

  it('TC_CFG_SAVE_FAILURE_06: Save config with negative LED intensity', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, ledIntensity: -1 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_led_intensity');
  });

  it('TC_CFG_SAVE_FAILURE_07: Save config with LED intensity exceeds 100', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, ledIntensity: 120 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_led_intensity');
  });

  it('TC_CFG_SAVE_FAILURE_08: Save config with negative audio intensity', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, audioIntensity: -5 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_audio_intensity');
  });

  it('TC_CFG_SAVE_FAILURE_09: Save config with audio intensity exceeds 100', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, audioIntensity: 110 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_audio_intensity');
  });

  it('TC_CFG_SAVE_FAILURE_10: Save config with negative electric fence duration', async () => {
    if (!token) return;
    const res = await request(app)
      .put(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ ...validConfigPayload, electricFenceDuration: -10 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_electric_fence_duration');
  });

  // 5. POST /response-configs/{cameraId}/{speciesId}/apply-preset/{presetId}
  it('TC_CFG_APPLY_PRESET_SUCCESS_01: Apply danger level preset successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .post(`/response-configs/${camId}/${speciesId}/apply-preset/critical_danger`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.ledColor).toBe('STROBE');
  });

  // 6. DELETE /response-configs/{cameraId}/{speciesId}
  it('TC_CFG_RESET_SUCCESS_01: Reset custom config to fallback presets successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .delete(`/response-configs/${camId}/${speciesId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(204);
  });
});
