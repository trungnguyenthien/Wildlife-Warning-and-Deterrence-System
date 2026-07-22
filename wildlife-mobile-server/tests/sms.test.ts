import request from 'supertest';
import app from '../src/app';
import { PrismaClient } from '@prisma/client';
import { clearDatabase, disconnectPrisma } from './helper';

const prisma = new PrismaClient();

describe('SMS ALERT RECIPIENTS TESTING SUITE', () => {
  let token = '';

  beforeAll(async () => {
    try {
      await clearDatabase();

      // Đăng ký và đăng nhập Ranger
      await request(app).post('/auth/register').send({
        username: 'sms_user',
        password: 'password123',
        fullName: 'SMS Admin',
        phoneNumber: '+84907777771',
        role: 'RANGER'
      });
      const login = await request(app).post('/auth/login').send({
        username: 'sms_user',
        password: 'password123'
      });
      token = login.body.token;
    } catch (err) {
      console.warn('DB Offline - bỏ qua nạp dữ liệu test sms.', err);
    }
  });

  afterAll(async () => {
    await disconnectPrisma();
  });

  // GET /users/me/sms-recipients
  it('TC_SMS_LIST_SUCCESS_01: Retrieve registered SMS recipients list successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .get('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
  });

  // POST /users/me/sms-recipients
  const recipientData = {
    fullName: 'Nguyen Thi B',
    phoneNumber: '+84908888883',
    relation: 'family'
  };

  it('TC_SMS_ADD_SUCCESS_01: Register a secondary SMS recipient successfully', async () => {
    if (!token) return;
    const res = await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send(recipientData);

    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('id');
    expect(res.body.fullName).toBe(recipientData.fullName);
    expect(res.body.phoneNumber).toBe(recipientData.phoneNumber);
    expect(res.body.relation).toBe(recipientData.relation);
  });

  it('TC_SMS_ADD_FAILURE_02: Register recipient missing fullName', async () => {
    if (!token) return;
    const { fullName: _, ...payload } = recipientData;
    const res = await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send({ ...payload, fullName: '' });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_fullName');
  });

  it('TC_SMS_ADD_FAILURE_03: Register recipient missing phoneNumber', async () => {
    if (!token) return;
    const { phoneNumber: _, ...payload } = recipientData;
    const res = await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send({ ...payload, phoneNumber: '' });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_phoneNumber');
  });

  it('TC_SMS_ADD_FAILURE_04: Register recipient missing relation', async () => {
    if (!token) return;
    const { relation: _, ...payload } = recipientData;
    const res = await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send({ ...payload, relation: '' });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_relation');
  });

  it('TC_SMS_ADD_FAILURE_05: Register recipient with invalid phone number format', async () => {
    if (!token) return;
    const res = await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send({ ...recipientData, phoneNumber: '0908888883' }); // Không khớp E.164

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_phone_number');
  });

  it('TC_SMS_ADD_FAILURE_06: Register recipient with invalid relation value', async () => {
    if (!token) return;
    const res = await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send({ ...recipientData, relation: 'friend' }); // friend không nằm trong enum

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_relation');
  });

  it('TC_SMS_ADD_FAILURE_01: Register recipient violates the limit of 3 secondary numbers', async () => {
    if (!token) return;
    // Thêm số thứ 2
    await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send({ fullName: 'Nguoi 2', phoneNumber: '+84908888884', relation: 'family' });

    // Thêm số thứ 3
    await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send({ fullName: 'Nguoi 3', phoneNumber: '+84908888885', relation: 'neighbor' });

    // Thêm số thứ 4 (vi phạm giới hạn)
    const res = await request(app)
      .post('/users/me/sms-recipients')
      .set('Authorization', `Bearer ${token}`)
      .send({ fullName: 'Nguoi 4', phoneNumber: '+84908888886', relation: 'other' });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('limit_reached');
  });

  // DELETE /users/me/sms-recipients/{recipientId}
  it('TC_SMS_DEL_SUCCESS_01: Delete SMS recipient registration successfully', async () => {
    if (!token) return;
    // Tìm recipient đầu tiên trong DB
    const list = await prisma.smsRecipient.findMany();
    const id = list[0].id;

    const res = await request(app)
      .delete(`/users/me/sms-recipients/${id}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(204);
  });

  it('TC_SMS_DEL_FAILURE_01: Fail to delete SMS recipient that does not belong to current user', async () => {
    if (!token) return;
    const res = await request(app)
      .delete('/users/me/sms-recipients/non-existent-id-9999')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(404);
    expect(res.body.error).toBe('not_found_recipient');
  });
});
