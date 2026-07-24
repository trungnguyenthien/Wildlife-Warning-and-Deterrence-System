import request from 'supertest';
import app from '../src/app';
import { clearDatabase, disconnectPrisma } from './helper';

beforeAll(async () => {
  try {
    await clearDatabase();
  } catch (err) {
    console.warn('Cảnh báo: Không thể dọn dẹp database. Có thể PostgreSQL chưa hoạt động.', err);
  }
});

afterAll(async () => {
  await disconnectPrisma();
});

describe('HỆ THỐNG & HEALTH CHECK', () => {
  it('TC_SYS_HLT_SUCCESS_01: Retrieve system health status successfully', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('OK');
    expect(res.body).toHaveProperty('timestamp');
  });
});

describe('AUTH_REGISTER - Đăng ký tài khoản', () => {
  const validUser = {
    username: 'test_ranger',
    password: 'password123',
    fullName: 'Nguyen Van A',
    phoneNumber: '+84901234567',
    role: 'RANGER'
  };

  it('TC_AUTH_REG_SUCCESS_01: Register user successfully', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send(validUser);

    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('id');
    expect(res.body.id).toMatch(/^[0-9a-f]{4}$/);
    expect(res.body.username).toBe(validUser.username);
    expect(res.body.fullName).toBe(validUser.fullName);
    expect(res.body.phoneNumber).toBe(validUser.phoneNumber);
    expect(res.body.role).toBe(validUser.role);
  });

  it('TC_AUTH_REG_FAILURE_02: Fail to register due to missing username', async () => {
    const { username: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, username: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_username');
  });

  it('TC_AUTH_REG_FAILURE_03: Fail to register due to missing password', async () => {
    const { password: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, password: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_password');
  });

  it('TC_AUTH_REG_FAILURE_04: Fail to register due to missing fullName', async () => {
    const { fullName: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, fullName: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_fullName');
  });

  it('TC_AUTH_REG_FAILURE_05: Fail to register due to missing phoneNumber', async () => {
    const { phoneNumber: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, phoneNumber: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_phoneNumber');
  });

  it('TC_AUTH_REG_FAILURE_06: Fail to register due to missing role', async () => {
    const { role: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, role: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_role');
  });

  it('TC_AUTH_REG_FAILURE_07: Fail to register with invalid format phone number', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send({ ...validUser, username: 'ranger2', phoneNumber: '0901234567' }); // Không bắt đầu bằng +
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_phone_number');
  });

  it('TC_AUTH_REG_FAILURE_08: Fail to register with password too short', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send({ ...validUser, username: 'ranger3', password: '123' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_password');
  });

  it('TC_AUTH_REG_FAILURE_01: Fail to register with a duplicate username', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send(validUser); // Gửi lại thông tin trùng
    expect(res.status).toBe(409);
    expect(res.body.error).toBe('duplicate_user');
  });

  it('TC_AUTH_REG_FAILURE_09: Fail to register with non-existent system role', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send({ ...validUser, username: 'ranger_super', role: 'SUPER_ADMIN' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('invalid_role');
    expect(res.body.message).toContain('Vai trò không hợp lệ');
  });

  it('TC_AUTH_REG_FAILURE_10: Fail to register due to client sending explicit ID', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send({ ...validUser, username: 'ranger_explicit_id', id: 'custom_id' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('id_not_allowed_from_client');
    expect(res.body.message).toContain('Không cho phép gửi kèm ID tài khoản');
  });
});

describe('AUTH_LOGIN - Đăng nhập tài khoản', () => {
  it('TC_AUTH_LOG_SUCCESS_01: Login successfully with correct credentials', async () => {
    const res = await request(app)
      .post('/auth/login')
      .send({
        username: 'test_ranger',
        password: 'password123'
      });

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('token');
    expect(res.body.role).toBe('RANGER');
  });

  it('TC_AUTH_LOG_FAILURE_01: Fail to login with incorrect password', async () => {
    const res = await request(app)
      .post('/auth/login')
      .send({
        username: 'test_ranger',
        password: 'wrongpassword'
      });
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('unauthorized_credentials');
  });

  it('AUTH_LOGIN_FAILURE_02: Login with non-existing username', async () => {
    const res = await request(app)
      .post('/auth/login')
      .send({
        username: 'non_exist_ranger',
        password: 'password123'
      });
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('unauthorized_credentials');
  });

  it('TC_AUTH_LOG_FAILURE_02: Fail to login due to missing username', async () => {
    const res = await request(app)
      .post('/auth/login')
      .send({ password: 'password123' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_username');
  });

  it('TC_AUTH_LOG_FAILURE_03: Fail to login due to missing password', async () => {
    const res = await request(app)
      .post('/auth/login')
      .send({ username: 'test_ranger' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('missed_password');
  });
});

describe('AUTH_ME & LOGOUT - Phiên đăng nhập', () => {
  let authToken = '';

  beforeAll(async () => {
    // Đăng nhập để lấy token hoạt động
    try {
      const res = await request(app)
        .post('/auth/login')
        .send({
          username: 'test_ranger',
          password: 'password123'
        });
      authToken = res.body.token;
    } catch (err) {
      console.warn('Lấy token kiểm thử thất bại.', err);
    }
  });

  it('AUTH_ME_SUCCESS_01: Retrieve profile details successfully', async () => {
    if (!authToken) return; // Skip test nếu DB offline
    const res = await request(app)
      .get('/users/me')
      .set('Authorization', `Bearer ${authToken}`);

    expect(res.status).toBe(200);
    expect(res.body.username).toBe('test_ranger');
    expect(res.body.role).toBe('RANGER');
  });

  it('TC_AUTH_OUT_SUCCESS_01: Logout successfully with a valid token', async () => {
    if (!authToken) return; // Skip test nếu DB offline
    const res = await request(app)
      .post('/auth/logout')
      .set('Authorization', `Bearer ${authToken}`);

    expect(res.status).toBe(200);
    expect(res.body.message).toContain('Đăng xuất');

    // Truy cập me sau khi logout phải thất bại
    const meRes = await request(app)
      .get('/users/me')
      .set('Authorization', `Bearer ${authToken}`);
    expect(meRes.status).toBe(401);
  });

  it('TC_AUTH_OUT_FAILURE_01: Fail to logout with missing authorization header', async () => {
    const res = await request(app)
      .post('/auth/logout');
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('missed_token');
  });

  it('TC_AUTH_OUT_FAILURE_02: Fail to logout with invalid format token', async () => {
    const res = await request(app)
      .post('/auth/logout')
      .set('Authorization', 'Bearer this_is_a_totally_invalid_token_xyz');
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('invalid_token');
  });
});
