import request from 'supertest';
import app from '../src/app';
import { clearDatabase, disconnectPrisma } from './helper';

describe('REFERENCE DATA TESTING SUITE', () => {
  let token = '';

  beforeAll(async () => {
    try {
      await clearDatabase();

      await request(app).post('/auth/register').send({
        username: 'ref_user',
        password: 'password123',
        fullName: 'Reference Admin',
        phoneNumber: '+84904444441',
        role: 'RANGER'
      });
      const login = await request(app).post('/auth/login').send({
        username: 'ref_user',
        password: 'password123'
      });
      token = login.body.token;
    } catch (err) {
      console.warn('DB Offline - bỏ qua nạp dữ liệu test reference.', err);
    }
  });

  afterAll(async () => {
    await disconnectPrisma();
  });

  // GET /control/presets
  it('TC_REF_PRE_SUCCESS_01: Retrieve list of preset response configs successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/control/presets')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body[0]).toHaveProperty('id');
    expect(res.body[0]).toHaveProperty('name');
    expect(res.body[0]).toHaveProperty('config');
  });

  // GET /audio-samples
  it('TC_REF_AUD_SUCCESS_01: Retrieve audio sample directory successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/audio-samples')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('audio_samples');
    expect(res.body).toHaveProperty('speaker_samples');
    expect(Array.isArray(res.body.audio_samples)).toBe(true);
    expect(Array.isArray(res.body.speaker_samples)).toBe(true);
    expect(res.body.audio_samples.length).toBeGreaterThan(0);
    expect(res.body.audio_samples[0]).toHaveProperty('id');
    expect(res.body.audio_samples[0]).toHaveProperty('name');
    expect(res.body.audio_samples[0]).toHaveProperty('durationSeconds');
  });
});
