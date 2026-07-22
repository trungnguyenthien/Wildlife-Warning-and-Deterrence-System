import request from 'supertest';
import app from '../src/app';
import { clearDatabase, disconnectPrisma } from './helper';

beforeAll(async () => {
  await clearDatabase();
});

afterAll(async () => {
  await disconnectPrisma();
});

describe('GET /health', () => {
  it('Trả về trạng thái OK và thông tin thời gian hợp lệ', async () => {
    const res = await request(app).get('/health');

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('status');
    expect(res.body.status).toBe('OK');
    expect(res.body).toHaveProperty('timestamp');
  });
});
